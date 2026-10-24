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
    service.accept(id, "1");
    service.accept(id, "1");
    assertEquals(1, db.queryForObject("select count(*) from test_plan_todo", Integer.class));
    assertThrows(
        Exception.class, () -> tx.execute(t -> service.preview(id, "1", action("改变已执行内容"))));
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
