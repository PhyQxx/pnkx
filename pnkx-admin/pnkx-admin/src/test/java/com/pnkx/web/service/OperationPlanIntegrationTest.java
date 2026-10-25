package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alibaba.fastjson.*;
import com.pnkx.framework.web.service.DataPermissionService;
import com.pnkx.life.domain.po.PxToDo;
import com.pnkx.life.mapper.PxRecipeMapper;
import com.pnkx.life.service.*;
import com.pnkx.life.service.impl.ShoppingWorkflowService;
import com.pnkx.web.service.operation.OperationPlanService;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;

@EnabledIfEnvironmentVariable(
    named = "PNKX_TEST_MYSQL_URL",
    matches = "jdbc:mysql://127\\.0\\.0\\.1:.*?/pnkx_test.*")
class OperationPlanIntegrationTest {
  JdbcTemplate db;
  OperationPlanService service;
  ShoppingWorkflowService shopping;
  IPxToDoService todos;

  @BeforeEach
  void setup() throws Exception {
    var ds =
        new DriverManagerDataSource(
            System.getenv("PNKX_TEST_MYSQL_URL"), "root", "pnkx_local_test_only");
    db = new JdbcTemplate(ds);
    for (String table : List.of("px_operation_step", "px_operation_plan", "test_plan_todo"))
      db.execute("drop table if exists " + table);
    for (String file :
        List.of(
            "V1.6.8__operation_plans.sql",
            "V1.7.0__operation_recovery.sql",
            "V1.8.2__operation_plan_replacements.sql"))
      for (String sql :
          Files.readString(Path.of("src/main/resources/db/migration/" + file)).split(";"))
        if (!sql.isBlank()) db.execute(sql);
    db.execute(
        "create table test_plan_todo(id bigint auto_increment primary key,client_uuid varchar(100)"
            + " unique,content varchar(2000)) engine=InnoDB");
    todos = mock(IPxToDoService.class);
    shopping = mock(ShoppingWorkflowService.class);
    when(todos.insertPxToDo(any()))
        .thenAnswer(
            inv -> {
              PxToDo todo = inv.getArgument(0);
              db.update(
                  "insert into test_plan_todo(client_uuid,content) values(?,?)",
                  todo.getClientUuid(),
                  todo.getContent());
              todo.setId(
                  db.queryForObject(
                      "select id from test_plan_todo where client_uuid=?",
                      Long.class,
                      todo.getClientUuid()));
              return 1;
            });
    when(todos.selectPxToDoById(anyLong()))
        .thenAnswer(
            inv -> {
              var row =
                  db.queryForMap(
                      "select * from test_plan_todo where id=?", inv.getArgument(0, Long.class));
              var t = new PxToDo();
              t.setId(((Number) row.get("id")).longValue());
              t.setContent((String) row.get("content"));
              t.setCreateBy("1");
              return t;
            });
    service =
        new OperationPlanService(
            db,
            new DataSourceTransactionManager(ds),
            todos,
            mock(IPxMealPlanService.class),
            mock(PxRecipeMapper.class),
            shopping,
            mock(DataPermissionService.class));
  }

  String preview(boolean second) {
    var input =
        JSON.parseObject(
            "{\"title\":\"测试计划\",\"steps\":[{\"toolCode\":\"todo.create\",\"input\":{\"content\":\"做晚饭\",\"dueDate\":\"2026-10-24\"}}]}");
    if (second)
      input
          .getJSONArray("steps")
          .add(
              JSON.parseObject(
                  "{\"toolCode\":\"shopping.generate\",\"input\":{\"listId\":1,\"startDate\":\"2026-10-24\",\"endDate\":\"2026-10-24\"}}"));
    return service.preview(input, "1").getString("id");
  }

  @Test
  void partialFailureResumesWithoutDuplicatingSuccessfulStep() {
    String id = preview(true);
    when(shopping.preview(anyLong(), anyString(), anyString(), anyString()))
        .thenThrow(new IllegalStateException("offline"));
    assertEquals("partially_succeeded", service.execute(id, "1").getString("status"));
    doReturn(JSON.parseObject("{\"previewId\":\"preview\"}"))
        .when(shopping)
        .preview(anyLong(), anyString(), anyString(), anyString());
    when(shopping.commit(anyString(), anyString())).thenReturn(new JSONObject());
    assertEquals("succeeded", service.execute(id, "1").getString("status"));
    service.execute(id, "1");
    assertEquals(1, db.queryForObject("select count(*) from test_plan_todo", Integer.class));
  }

  @Test
  void expiredPreviewCannotWrite() {
    String id = preview(false);
    db.update(
        "update px_operation_plan set expires_at=date_sub(now(),interval 1 minute) where id=?", id);
    assertThrows(Exception.class, () -> service.execute(id, "1"));
    assertEquals(0, db.queryForObject("select count(*) from test_plan_todo", Integer.class));
  }

  @Test
  void runningLeaseBlocksDuplicateAndExpiredLeaseCanRecover() {
    String id = preview(false);
    db.update(
        "update px_operation_plan set"
            + " status='running',run_token='old',lease_until=date_add(now(),interval 1 minute)"
            + " where id=?",
        id);
    assertEquals("running", service.execute(id, "1").getString("status"));
    assertEquals(0, db.queryForObject("select count(*) from test_plan_todo", Integer.class));
    db.update(
        "update px_operation_plan set lease_until=date_sub(now(),interval 1 minute) where id=?",
        id);
    assertEquals("succeeded", service.execute(id, "1").getString("status"));
  }

  @Test
  void cancellationAndOwnershipPreventExecution() {
    String id = preview(false);
    assertThrows(Exception.class, () -> service.execute(id, "2"));
    service.cancel(id, "1");
    assertEquals("cancelled", service.execute(id, "1").getString("status"));
    assertEquals(0, db.queryForObject("select count(*) from test_plan_todo", Integer.class));
  }

  @Test
  void editedResultCannotBeCompensated() {
    String id = preview(false);
    service.execute(id, "1");
    db.update("update test_plan_todo set content='人工修改'");
    assertThrows(Exception.class, () -> service.compensationPreview(id, "1"));
  }
}
