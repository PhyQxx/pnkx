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
  PxSubscription sub;
  PxSubscriptionServiceImpl subscriptions;
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
    db.execute("drop table if exists px_subscription");
    db.execute("drop table if exists px_bookkeeping_recurring");
    db.execute(
        "create table px_subscription(id bigint,name varchar(255),create_by varchar(64),enabled"
            + " boolean,del_flag char(1),amount decimal(18,2))");
    db.execute(
        "create table px_bookkeeping_recurring(id bigint,name varchar(255),create_by"
            + " varchar(64),enabled boolean,del_flag char(1),money decimal(18,2))");
    sub = new PxSubscription();
    sub.setId(1L);
    sub.setName("会员");
    sub.setCreateBy("1");
    sub.setAmount(new BigDecimal("30.00"));
    sub.setCycle("monthly");
    sub.setCycleInterval(1);
    sub.setAnchorDay(31);
    sub.setNextPaymentDate(java.sql.Date.valueOf("2026-01-31"));
    sub.setEnabled(false);
    sub.setLifecycle("paused");
    var mapper = mock(PxSubscriptionMapper.class);
    when(mapper.selectPxSubscriptionById(1L)).thenReturn(sub);
    when(mapper.selectForCharge(1L)).thenReturn(sub);
    when(mapper.updatePxSubscription(any())).thenReturn(1);
    subscriptions = mock(PxSubscriptionServiceImpl.class);
    when(subscriptions.updatePxSubscription(any())).thenReturn(1);
    service =
        new ChargeRuleChangeService(
            db,
            mapper,
            mock(PxBookkeepingRecurringMapper.class),
            subscriptions,
            mock(PxBookkeepingRecurringService.class));
    instances = mock(ChargeInstanceService.class);
    ReflectionTestUtils.setField(service, "instances", instances);
  }

  JSONObject input() {
    return JSON.parseObject(
        "{\"sourceType\":\"subscription\",\"sourceId\":1,\"changes\":{\"amount\":25}}");
  }

  @Test
  void changedSourceInvalidatesPreview() {
    var p = service.preview(input(), "1");
    sub.setName("他端修改");
    assertThrows(
        Exception.class, () -> tx.execute(t -> service.commit(p.getString("previewId"), "1")));
    verify(subscriptions, never()).updatePxSubscription(any());
  }

  @Test
  void commitRetryDoesNotApplyRuleTwice() {
    var p = service.preview(input(), "1");
    tx.execute(t -> service.commit(p.getString("previewId"), "1"));
    tx.execute(t -> service.commit(p.getString("previewId"), "1"));
    verify(subscriptions, times(1)).updatePxSubscription(any());
  }

  @Test
  void pausedBackfillIsExplicitAndAlwaysPending() {
    var p = service.backfillPreview(input(), "1");
    assertTrue(p.getJSONArray("periods").contains("2026-02-28"));
    assertTrue(p.getJSONArray("periods").contains("2026-03-31"));
    tx.execute(t -> service.commit(p.getString("previewId"), "1"));
    verify(instances, atLeastOnce())
        .generate(
            eq("1"),
            eq("subscription"),
            eq(1L),
            eq("会员"),
            any(LocalDate.class),
            eq(new BigDecimal("30.00")),
            isNull(),
            isNull(),
            isNull(),
            eq("manual_confirm"),
            anyString());
    assertFalse(sub.getEnabled());
  }

  @Test
  void wrongOwnerAndActiveBackfillAreRejected() {
    assertThrows(Exception.class, () -> service.preview(input(), "2"));
    sub.setEnabled(true);
    assertThrows(Exception.class, () -> service.backfillPreview(input(), "1"));
  }
}
