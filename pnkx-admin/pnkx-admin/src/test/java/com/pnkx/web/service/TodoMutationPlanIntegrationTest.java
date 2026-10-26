package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alibaba.fastjson.*;
import com.pnkx.life.domain.po.PxToDo;
import com.pnkx.web.service.operation.TodoMutationTool;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.util.ReflectionTestUtils;

@EnabledIfEnvironmentVariable(
    named = "PNKX_TEST_MYSQL_URL",
    matches = "jdbc:mysql://127\\.0\\.0\\.1:.*?/pnkx_test.*")
class TodoMutationPlanIntegrationTest {
  OperationPlanIntegrationTest f;
  TodoMutationTool tool;

  @BeforeEach
  void setup() throws Exception {
    f = new OperationPlanIntegrationTest();
    f.setup();
    f.db.execute("drop table if exists mutation_todo");
    f.db.execute(
        "create table mutation_todo(id bigint auto_increment primary key,content"
            + " varchar(2000),performer varchar(64),plan_start_time varchar(32),plan_end_time"
            + " varchar(32),status tinyint default 0,label varchar(100),kanban_status int"
            + " default 0,finish_by varchar(64),finish_time varchar(32),create_by varchar(64),"
            + " update_by varchar(64))");
    // 基础夹具已给 selectPxToDoById 打过桩，须用 doAnswer 重绑，避免 when() 触发旧桩
    doAnswer(
            inv -> {
              var rows =
                  f.db.queryForList(
                      "select * from mutation_todo where id=?", inv.getArgument(0, Long.class));
              if (rows.isEmpty()) return null;
              return todo(rows.get(0));
            })
        .when(f.todos)
        .selectPxToDoById(anyLong());
    when(f.todos.updatePxToDo(any()))
        .thenAnswer(
            inv -> {
              PxToDo t = inv.getArgument(0);
              return f.db.update(
                  "update mutation_todo set"
                      + " content=coalesce(?,content),status=coalesce(?,status),kanban_status=coalesce(?,kanban_status),"
                      + " finish_by=coalesce(?,finish_by),finish_time=coalesce(?,finish_time),"
                      + " plan_start_time=coalesce(?,plan_start_time),plan_end_time=coalesce(?,plan_end_time),"
                      + " label=coalesce(?,label),update_by=? where id=?",
                  t.getContent(),
                  t.getStatus(),
                  t.getKanbanStatus(),
                  t.getFinishBy(),
                  t.getFinishTime(),
                  t.getPlanStartTime(),
                  t.getPlanEndTime(),
                  t.getLabel(),
                  t.getUpdateBy(),
                  t.getId());
            });
    when(f.todos.deletePxToDoById(anyLong()))
        .thenAnswer(
            inv ->
                f.db.update(
                    "delete from mutation_todo where id=?", inv.getArgument(0, Long.class)));
    when(f.todos.deletePxToDoByIds(any(Long[].class)))
        .thenAnswer(
            inv -> {
              Long[] ids = inv.getArgument(0, Long[].class);
              String placeholders = String.join(",", Collections.nCopies(ids.length, "?"));
              return f.db.update(
                  "delete from mutation_todo where id in (" + placeholders + ")", (Object[]) ids);
            });
    tool = new TodoMutationTool(f.db, f.todos);
    ReflectionTestUtils.setField(f.service, "todoMutations", tool);
  }

  private PxToDo todo(Map<String, Object> row) {
    PxToDo t = new PxToDo();
    t.setId(((Number) row.get("id")).longValue());
    t.setContent((String) row.get("content"));
    t.setCreateBy((String) row.get("create_by"));
    t.setStatus(((Number) row.get("status")).intValue() != 0);
    t.setKanbanStatus(((Number) row.get("kanban_status")).intValue());
    t.setPerformer((String) row.get("performer"));
    t.setPlanStartTime((String) row.get("plan_start_time"));
    t.setPlanEndTime((String) row.get("plan_end_time"));
    t.setLabel((String) row.get("label"));
    t.setFinishBy((String) row.get("finish_by"));
    t.setFinishTime((String) row.get("finish_time"));
    return t;
  }

  Long seed(String content, String owner) {
    f.db.update(
        "insert into mutation_todo(content,create_by) values(?,?)", content, owner);
    return f.db.queryForObject("select max(id) from mutation_todo", Long.class);
  }

  PxToDo load(Long id) {
    return f.todos.selectPxToDoById(id);
  }

  String preview(String code, JSONObject draft, List<PxToDo> targets) {
    tool.prepare(code, draft, targets);
    var step = new JSONObject();
    step.put("toolCode", code);
    step.put("input", draft);
    var steps = new JSONArray();
    steps.add(step);
    var input = new JSONObject();
    input.put("steps", steps);
    return f.service.preview(input, "1").getString("id");
  }

  String previewTargets(String code, Long... ids) {
    var draft = new JSONObject();
    if ("todo.update".equals(code))
      draft.put("newContent", "更新后的内容");
    List<PxToDo> targets = new ArrayList<>();
    for (Long id : ids) targets.add(load(id));
    return preview(code, draft, targets);
  }

  @Test
  void completeMarksTodoDoneOnceAndRepeatExecuteDoesNotDuplicate() {
    Long id = seed("买牛奶", "1");
    String planId = previewTargets("todo.complete", id);
    assertEquals("succeeded", f.service.execute(planId, "1").getString("status"));
    var row = f.db.queryForMap("select status,kanban_status,finish_by from mutation_todo where id=?", id);
    assertEquals(1, ((Number) row.get("status")).intValue());
    assertEquals(2, ((Number) row.get("kanban_status")).intValue());
    assertEquals("1", row.get("finish_by"));
    f.service.execute(planId, "1");
    assertEquals(1, f.db.queryForObject("select count(*) from mutation_todo", Integer.class));
    assertThrows(Exception.class, () -> f.service.compensate(planId, "1"));
  }

  @Test
  void batchCompleteAndDeleteRemoveExpectedRows() {
    Long first = seed("待办一", "1");
    Long second = seed("待办二", "1");
    Long third = seed("待办三", "1");
    String complete = previewTargets("todo.complete", first, second);
    assertEquals("succeeded", f.service.execute(complete, "1").getString("status"));
    assertEquals(
        2,
        f.db.queryForObject(
            "select count(*) from mutation_todo where status=1", Integer.class));
    String delete = previewTargets("todo.delete", first, third);
    assertEquals("succeeded", f.service.execute(delete, "1").getString("status"));
    assertEquals(
        1, f.db.queryForObject("select count(*) from mutation_todo", Integer.class));
    assertEquals(second, f.db.queryForObject("select id from mutation_todo", Long.class));
  }

  @Test
  void updateChangesContentAndDeadline() {
    Long id = seed("旧内容", "1");
    var draft = new JSONObject();
    draft.put("newContent", "新内容");
    draft.put("planEndTime", "2026-11-01");
    String planId = preview("todo.update", draft, List.of(load(id)));
    assertEquals("succeeded", f.service.execute(planId, "1").getString("status"));
    var row = f.db.queryForMap("select content,plan_end_time from mutation_todo where id=?", id);
    assertEquals("新内容", row.get("content"));
    assertEquals("2026-11-01", row.get("plan_end_time"));
  }

  @Test
  void editedTargetAfterPreviewFailsWithoutWriting() {
    Long id = seed("原始内容", "1");
    String planId = previewTargets("todo.complete", id);
    f.db.update("update mutation_todo set content='被他人修改' where id=?", id);
    assertEquals("failed", f.service.execute(planId, "1").getString("status"));
    var row = f.db.queryForMap("select status from mutation_todo where id=?", id);
    assertEquals(0, ((Number) row.get("status")).intValue());
    f.db.update("delete from mutation_todo where id=?", id);
    assertEquals("failed", f.service.execute(planId, "1").getString("status"));
  }

  @Test
  void foreignTargetRejectedAtPreview() {
    Long foreign = seed("他人的待办", "2");
    var draft = new JSONObject();
    List<PxToDo> targets = List.of(load(foreign));
    // 模拟旧客户端或伪造载荷：目标不属于本人时预览直接拒绝
    tool.prepare("todo.complete", draft, targets);
    var step = new JSONObject();
    step.put("toolCode", "todo.complete");
    step.put("input", draft);
    var steps = new JSONArray();
    steps.add(step);
    var input = new JSONObject();
    input.put("steps", steps);
    assertThrows(Exception.class, () -> f.service.preview(input, "1"));
  }

  @Test
  void oversizedBatchRejected() {
    List<PxToDo> tooMany = new ArrayList<>();
    for (int i = 0; i <= TodoMutationTool.MAX_BATCH; i++) tooMany.add(load(seed("批量" + i, "1")));
    var draft = new JSONObject();
    assertThrows(Exception.class, () -> tool.prepare("todo.complete", draft, tooMany));
  }

  @Test
  void updateWithoutFieldsFailsAtPreview() {
    Long id = seed("待改", "1");
    var draft = new JSONObject();
    List<PxToDo> targets = List.of(load(id));
    tool.prepare("todo.update", draft, targets);
    var step = new JSONObject();
    step.put("toolCode", "todo.update");
    step.put("input", draft);
    var steps = new JSONArray();
    steps.add(step);
    var input = new JSONObject();
    input.put("steps", steps);
    assertThrows(Exception.class, () -> f.service.preview(input, "1"));
  }
}
