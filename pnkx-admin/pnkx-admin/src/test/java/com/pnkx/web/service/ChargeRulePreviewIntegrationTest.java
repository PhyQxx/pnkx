package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alibaba.fastjson.*;
import com.pnkx.life.domain.po.*;
import com.pnkx.life.mapper.*;
import com.pnkx.life.service.impl.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.regex.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

@EnabledIfEnvironmentVariable(
    named = "PNKX_TEST_MYSQL_URL",
    matches = "jdbc:mysql://127\\.0\\.0\\.1:.*?/pnkx_test.*")
class ChargeRulePreviewIntegrationTest {
  JdbcTemplate db;
  TransactionTemplate tx;
  ChargeRuleChangeService service;
  PxBookkeepingRecurring recurring;
  PxBookkeepingRecurringService recurringService;
  ChargeInstanceService instances;

  @BeforeEach
  void setup() throws Exception {
    var ds =
        new DriverManagerDataSource(
            System.getenv("PNKX_TEST_MYSQL_URL"), "root", "pnkx_local_test_only");
    db = new JdbcTemplate(ds);
    tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
    db.execute("drop table if exists px_operation_preview");
    var m =
        Pattern.compile("CREATE TABLE px_operation_preview.*?;", Pattern.DOTALL)
            .matcher(
                Files.readString(
                    Path.of("src/main/resources/db/migration/V1.6.7__shopping_workflow.sql")));
    assertTrue(m.find());
    db.execute(m.group());
    db.execute("drop table if exists px_bookkeeping_recurring");
    db.execute(
        "create table px_bookkeeping_recurring(id bigint,name varchar(255),create_by"
            + " varchar(64),enabled boolean,del_flag char(1),money decimal(18,2))");
    recurring = new PxBookkeepingRecurring();
    recurring.setId(1L);
    recurring.setName("停车费");
    recurring.setCreateBy("1");
    recurring.setMoney(new BigDecimal("30.00"));
    recurring.setFrequency("month");
    recurring.setDayNumber(15);
    recurring.setAccount(2L);
    recurring.setType(3L);
    recurring.setOtherAccount(null);
    recurring.setTypeDifference("1");
    recurring.setNextRunDate(java.sql.Date.valueOf("2026-01-15"));
    recurring.setEnabled(false);
    var mapper = mock(PxBookkeepingRecurringMapper.class);
    when(mapper.selectById(1L, "1")).thenReturn(recurring);
    when(mapper.lockForCharge(1L, "1")).thenReturn(recurring);
    when(mapper.advanceRunDate(eq(1L), any(), any())).thenReturn(1);
    recurringService = mock(PxBookkeepingRecurringService.class);
    when(recurringService.update(any())).thenReturn(1);
    service = new ChargeRuleChangeService(db, mapper, recurringService);
    instances = mock(ChargeInstanceService.class);
    ReflectionTestUtils.setField(service, "instances", instances);
  }

  JSONObject input() {
    return JSON.parseObject(
        "{\"sourceType\":\"recurring\",\"sourceId\":1,\"changes\":{\"money\":25}}");
  }

  @Test
  void changedSourceInvalidatesPreview() {
    var p = service.preview(input(), "1");
    recurring.setName("他端修改");
    assertThrows(
        Exception.class, () -> tx.execute(t -> service.commit(p.getString("previewId"), "1")));
    verify(recurringService, never()).update(any());
  }

  @Test
  void commitRetryDoesNotApplyRuleTwice() {
    var p = service.preview(input(), "1");
    tx.execute(t -> service.commit(p.getString("previewId"), "1"));
    tx.execute(t -> service.commit(p.getString("previewId"), "1"));
    verify(recurringService, times(1)).update(any());
  }

  @Test
  void pausedBackfillIsExplicitAndAlwaysPending() {
    var p = service.backfillPreview(input(), "1");
    assertTrue(p.getJSONArray("periods").contains(LocalDate.now().withDayOfMonth(15).toString()));
    tx.execute(t -> service.commit(p.getString("previewId"), "1"));
    verify(instances, atLeastOnce())
        .generate(
            eq("1"),
            eq("recurring"),
            eq(1L),
            eq("停车费"),
            any(LocalDate.class),
            eq(new BigDecimal("30.00")),
            eq(2L),
            eq(3L),
            isNull(),
            eq("manual_confirm"),
            isNull());
    assertFalse(recurring.getEnabled());
  }

  @Test
  void wrongOwnerAndActiveBackfillAreRejected() {
    assertThrows(Exception.class, () -> service.preview(input(), "2"));
    recurring.setEnabled(true);
    assertThrows(Exception.class, () -> service.backfillPreview(input(), "1"));
  }
}
