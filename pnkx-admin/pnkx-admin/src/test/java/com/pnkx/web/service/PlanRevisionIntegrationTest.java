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
  void rejectsStaleVersionAndSecondReplacement() {
    String id = f.preview(false);
    var p = patch(id);
    p.put("expectedRevision", -1);
    assertThrows(Exception.class, () -> f.service.revise(id, p, "1"));
    var valid = patch(id);
    f.service.revise(id, valid, "1");
    var same = valid;
    assertThrows(Exception.class, () -> f.service.revise(id, same, "1"));
    assertEquals(2, f.db.queryForObject("select count(*) from px_operation_plan", Integer.class));
  }
}
