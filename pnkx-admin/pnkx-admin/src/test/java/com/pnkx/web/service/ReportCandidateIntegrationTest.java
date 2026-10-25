package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alibaba.fastjson.*;
import com.pnkx.life.mapper.PxBookkeepingRecordMapper;
import com.pnkx.life.service.impl.SubscriptionSchedule;
import com.pnkx.web.service.operation.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;

@EnabledIfEnvironmentVariable(
    named = "PNKX_TEST_MYSQL_URL",
    matches = "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/pnkx_test(?:\\?.*)?")
class ReportCandidateIntegrationTest {
  JdbcTemplate db;
  ReportActionService service;
  TransactionTemplate tx;
  PxBookkeepingRecordMapper records;
  OperationPlanIntegrationTest fixture;

  @BeforeEach
  void setup() throws Exception {
    fixture = new OperationPlanIntegrationTest();
    fixture.setup();
    db = fixture.db;
    // Use the same datasource object as JdbcTemplate for transaction-bound sessions.
    tx = new TransactionTemplate(new DataSourceTransactionManager(db.getDataSource()));
    db.execute("drop table if exists px_report_insight");
    db.execute("drop table if exists px_to_do");
    db.execute(
        "create table px_to_do(id bigint primary key,content varchar(2000),status int,plan_end_time"
            + " datetime,create_by varchar(64))");
    for (String f : List.of("V1.6.9__report_actions.sql", "V1.8.0__report_candidates.sql"))
      db.execute(Files.readString(Path.of("src/main/resources/db/migration/" + f)));
    records = mock(PxBookkeepingRecordMapper.class);
    when(records.selectLifeReportCounts(anyString(), anyString(), anyString()))
        .thenReturn(Map.of("totalExpense", new BigDecimal("50.00")));
    service = new ReportActionService(db, records, fixture.service);
  }

  Long snapshot() {
    return ((Number) tx.execute(t -> service.snapshot("1", "month")).get("id")).longValue();
  }

  JSONObject action(String content) {
    var a = new JSONObject();
    a.put("content", content);
    a.put("dueDate", LocalDate.now(SubscriptionSchedule.ZONE).plusDays(3).toString());
    a.put("candidateId", "verify");
    return a;
  }

  @Test
  void candidatesStayStableWhenUnderlyingAmountsChange() {
    var id = snapshot();
    var before = service.list("1").get(0);
    assertEquals(3, ((JSONArray) before.get("candidates")).size());
    when(records.selectLifeReportCounts(anyString(), anyString(), anyString()))
        .thenReturn(Map.of("totalExpense", BigDecimal.ZERO));
    assertEquals(before.get("candidates"), service.list("1").get(0).get("candidates"));
    assertTrue(service.list("2").isEmpty());
    assertEquals(
        "rule_based", ((JSONArray) before.get("candidates")).getJSONObject(0).getString("origin"));
  }

  @Test
  void editingUnconfirmedCandidateReplacesPreviewAndAcceptanceIsIdempotent() {
    var id = snapshot();
    var first = tx.execute(t -> service.preview(id, "1", action("核对记录")));
    var second = tx.execute(t -> service.preview(id, "1", action("核对记录并补记漏项")));
    assertNotEquals(first.getString("id"), second.getString("id"));
    assertEquals("cancelled", fixture.service.get(first.getString("id"), "1").getString("status"));
    assertEquals(
        second.getString("id"),
        tx.execute(t -> service.preview(id, "1", action("核对记录并补记漏项"))).getString("id"));
    service.accept(id, "1", second.getString("id"));
    service.accept(id, "1", second.getString("id"));
    assertEquals(1, db.queryForObject("select count(*) from test_plan_todo", Integer.class));
    assertThrows(
        Exception.class, () -> tx.execute(t -> service.preview(id, "1", action("改变已执行内容"))));
  }

  @Test
  void revisedReportPlanIsTrackedAndStaleConfirmationCannotExecuteIt() {
    Long id = snapshot();
    var first = tx.execute(t -> service.preview(id, "1", action("核对记录")));
    var patch = new JSONObject();
    patch.put("expectedRevision", 0);
    patch.put("fields", JSON.parseObject("{\"content\":\"修改后的报告行动\"}"));
    var next = fixture.service.revise(first.getString("id"), patch, "1");
    var row = service.list("1").get(0);
    assertEquals(next.getString("id"), row.get("plan_id"));
    assertEquals(first.getString("id"), row.get("original_plan_id"));
    assertEquals("awaiting_confirmation", row.get("execution_status"));
    assertThrows(Exception.class, () -> service.accept(id, "1", first.getString("id")));
    assertThrows(Exception.class, () -> service.accept(id, "1", null));
    assertThrows(Exception.class, () -> service.accept(id, "2", next.getString("id")));
    assertEquals(0, db.queryForObject("select count(*) from test_plan_todo", Integer.class));
    var preview = tx.execute(t -> service.preview(id, "1", action("修改后的报告行动")));
    assertEquals(next.getString("id"), preview.getString("id"));
    assertEquals("succeeded", service.accept(id, "1", preview.getString("id")).getString("status"));
    assertThrows(Exception.class, () -> tx.execute(t -> service.preview(id, "1", action("重复生成"))));
    assertEquals(1, db.queryForObject("select count(*) from test_plan_todo", Integer.class));
    db.update(
        "insert into px_to_do(id,content,status,create_by) select id,content,0,'1' from"
            + " test_plan_todo");
    assertEquals("accepted", service.list("1").get(0).get("action_status"));
    db.update("update px_to_do set status=1");
    assertEquals("completed", service.list("1").get(0).get("action_status"));
    db.update("delete from px_to_do");
    assertEquals("cancelled", service.list("1").get(0).get("action_status"));
  }

  @Test
  void repeatedEditsAndCancellationFollowLatestPlan() {
    Long id = snapshot();
    var first = tx.execute(t -> service.preview(id, "1", action("核对记录")));
    var patch = JSON.parseObject("{\"expectedRevision\":0,\"fields\":{\"content\":\"再次核对\"}}");
    var second = fixture.service.revise(first.getString("id"), patch, "1");
    var third = fixture.service.revise(second.getString("id"), patch, "1");
    assertEquals(third.getString("id"), service.list("1").get(0).get("plan_id"));
    fixture.service.cancel(third.getString("id"), "1");
    assertEquals("cancelled", service.list("1").get(0).get("action_status"));
    var fresh = tx.execute(t -> service.preview(id, "1", action("新的行动")));
    assertNotEquals(third.getString("id"), fresh.getString("id"));
    assertEquals(fresh.getString("id"), service.list("1").get(0).get("plan_id"));
  }

  @Test
  void invalidCandidatePastDeadlineAndWrongOwnerDoNotCreatePlan() {
    var id = snapshot();
    var a = action("核对记录");
    a.put("candidateId", "forged");
    assertThrows(Exception.class, () -> tx.execute(t -> service.preview(id, "1", a)));
    a.put("candidateId", "verify");
    a.put("dueDate", LocalDate.now(SubscriptionSchedule.ZONE).minusDays(1).toString());
    assertThrows(Exception.class, () -> tx.execute(t -> service.preview(id, "1", a)));
    assertThrows(Exception.class, () -> tx.execute(t -> service.preview(id, "2", action("核对"))));
    assertEquals(0, db.queryForObject("select count(*) from px_operation_plan", Integer.class));
  }
}
