package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;

import com.alibaba.fastjson.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(
    named = "PNKX_TEST_MYSQL_URL",
    matches = "jdbc:mysql://127\\.0\\.0\\.1:.*?/pnkx_test.*")
class PlanRevisionIntegrationTest {
  OperationPlanIntegrationTest f;

  @BeforeEach
  void setup() throws Exception {
    f = new OperationPlanIntegrationTest();
    f.setup();
  }

  JSONObject patch(String id) {
    var p = new JSONObject();
    p.put("expectedRevision", f.service.get(id, "1").getInteger("revision"));
    p.put("fields", JSON.parseObject("{\"content\":\"修改后的待办\"}"));
    return p;
  }

  @Test
  void replacesAtomicallyAndOldIdCannotExecuteNewContent() {
    String old = f.preview(false);
    var next = f.service.revise(old, patch(old), "1");
    assertNotEquals(old, next.getString("id"));
    assertEquals("cancelled", f.service.execute(old, "1").getString("status"));
    assertEquals(0, f.db.queryForObject("select count(*) from test_plan_todo", Integer.class));
    assertEquals("succeeded", f.service.execute(next.getString("id"), "1").getString("status"));
    assertEquals("修改后的待办", f.db.queryForObject("select content from test_plan_todo", String.class));
  }

  @Test
  void rejectsForeignExpiredAndAlreadyExecutedPlans() {
    String id = f.preview(false);
    var p = patch(id);
    assertThrows(Exception.class, () -> f.service.revise(id, p, "2"));
    f.db.update(
        "update px_operation_plan set expires_at=date_sub(now(),interval 1 minute) where id=?", id);
    assertThrows(Exception.class, () -> f.service.revise(id, p, "1"));
    String executed = f.preview(false);
    var ep = patch(executed);
    f.service.execute(executed, "1");
    assertThrows(Exception.class, () -> f.service.revise(executed, ep, "1"));
  }

  @Test
  void invalidFieldsDoNotCancelOriginal() {
    String id = f.preview(false);
    var p = patch(id);
    p.getJSONObject("fields").put("createBy", "2");
    assertThrows(Exception.class, () -> f.service.revise(id, p, "1"));
    p.put("fields", JSON.parseObject("{\"content\":\"\"}"));
    assertThrows(Exception.class, () -> f.service.revise(id, p, "1"));
    assertEquals("awaiting_confirmation", f.service.get(id, "1").getString("status"));
    assertEquals(1, f.db.queryForObject("select count(*) from px_operation_plan", Integer.class));
  }

  @Test
  void retryDoesNotResurrectExecutedOrReplacedDraftAndCannotLeakToForeignOwner() {
    String id = f.preview(false);
    var request = patch(id);
    var next = f.service.revise(id, request, "1");
    String nextId = next.getString("id");
    var secondRequest = patch(nextId);
    var third = f.service.revise(nextId, secondRequest, "1");
    var replay = f.service.revise(id, request, "1");
    assertEquals(nextId, replay.getString("id"));
    assertEquals("cancelled", replay.getString("status"));
    assertEquals(third.getString("id"), replay.getString("replacement_id"));
    assertThrows(Exception.class, () -> f.service.revise(id, request, "2"));
    f.service.execute(third.getString("id"), "1");
    assertEquals("succeeded", f.service.revise(nextId, secondRequest, "1").getString("status"));
    assertEquals(1, f.db.queryForObject("select count(*) from test_plan_todo", Integer.class));
    assertEquals(3, f.db.queryForObject("select count(*) from px_operation_plan", Integer.class));
  }

  @Test
  void concurrentIdenticalRetriesCreateOnlyOneReplacement() throws Exception {
    String id = f.preview(false);
    var request = patch(id);
    var start = new java.util.concurrent.CountDownLatch(1);
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<String> save =
          () -> {
            start.await();
            return f.service
                .revise(id, JSON.parseObject(request.toJSONString()), "1")
                .getString("id");
          };
      var first = pool.submit(save);
      var second = pool.submit(save);
      start.countDown();
      assertEquals(
          first.get(10, java.util.concurrent.TimeUnit.SECONDS),
          second.get(10, java.util.concurrent.TimeUnit.SECONDS));
      assertEquals(2, f.db.queryForObject("select count(*) from px_operation_plan", Integer.class));
    }
  }

  @Test
  void rejectsStaleVersionAndReplaysIdenticalReplacement() {
    String id = f.preview(false);
    var p = patch(id);
    p.put("expectedRevision", -1);
    assertThrows(Exception.class, () -> f.service.revise(id, p, "1"));
    var valid = patch(id);
    var next = f.service.revise(id, valid, "1");
    assertEquals(
        next.getString("id"),
        f.service.revise(id, JSON.parseObject(valid.toJSONString()), "1").getString("id"));
    assertEquals(next.getString("id"), f.service.get(id, "1").getString("replacement_id"));
    valid.getJSONObject("fields").put("content", "不同的请求");
    assertThrows(Exception.class, () -> f.service.revise(id, valid, "1"));
    assertEquals(2, f.db.queryForObject("select count(*) from px_operation_plan", Integer.class));
  }
}
