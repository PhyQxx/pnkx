package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.pnkx.life.domain.po.*;
import com.pnkx.life.mapper.*;
import com.pnkx.life.service.impl.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

/** 仅在明确指定的隔离测试库运行，不读取应用数据源配置。 */
@EnabledIfEnvironmentVariable(
    named = "PNKX_TEST_MYSQL_URL",
    matches = "jdbc:mysql://127\\.0\\.0\\.1:.*?/pnkx_test.*")
class ChargeInstanceIntegrationTest {
  private JdbcTemplate jdbc;
  private TransactionTemplate tx;
  private ChargeInstanceService service;
  private PxBookkeepingRecordMapper records;
  private PxChargeInstanceMapper instances;

  @BeforeEach
  void setup() throws Exception {
    var ds =
        new DriverManagerDataSource(
            System.getenv("PNKX_TEST_MYSQL_URL"), "root", "pnkx_local_test_only");
    jdbc = new JdbcTemplate(ds);
    tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
    jdbc.execute("drop table if exists px_charge_instance");
    jdbc.execute("drop table if exists test_records");
    jdbc.execute("drop table if exists px_action_receipt");
    jdbc.execute(
        Files.readString(Path.of("src/main/resources/db/migration/V1.7.1__action_receipts.sql")));
    String migration =
        Files.readString(Path.of("src/main/resources/db/migration/V1.6.5__charge_instances.sql"));
    jdbc.execute(migration.substring(migration.indexOf("CREATE TABLE")));
    jdbc.execute(
        "create table test_records(id bigint auto_increment primary key, client_uuid varchar(100)"
            + " unique, amount decimal(18,2)) engine=InnoDB");
    var factory = new SqlSessionFactoryBean();
    factory.setDataSource(ds);
    var config = new org.apache.ibatis.session.Configuration();
    config.addMapper(PxChargeInstanceMapper.class);
    factory.setConfiguration(config);
    instances = new SqlSessionTemplate(factory.getObject()).getMapper(PxChargeInstanceMapper.class);
    records = mock(PxBookkeepingRecordMapper.class);
    when(records.insertPxBookkeepingRecord(any()))
        .thenAnswer(
            inv -> {
              PxBookkeepingRecord r = inv.getArgument(0);
              jdbc.update(
                  "insert into test_records(client_uuid,amount) values(?,?)",
                  r.getClientUuid(),
                  r.getMoney());
              r.setId(
                  jdbc.queryForObject(
                      "select id from test_records where client_uuid=?",
                      Long.class,
                      r.getClientUuid()));
              return 1;
            });
    var accounts = mock(PxBookkeepingAccountMapper.class);
    var a = new PxBookkeepingAccount();
    a.setId(1L);
    a.setCreateBy("owner");
    when(accounts.selectPxBookkeepingAccountById(1L)).thenReturn(a);
    var categories = mock(PxBookkeepingClassificationMapper.class);
    var c = new PxBookkeepingClassification();
    c.setCreateBy("owner");
    c.setTypeDifference("1");
    when(categories.selectPxBookkeepingClassificationById(2L)).thenReturn(c);
    service = new ChargeInstanceService(instances, records, accounts, categories);
  }

  private PxChargeInstance pending() {
    return tx.execute(
        t ->
            service.generate(
                "owner",
                "subscription",
                1L,
                "会员",
                LocalDate.of(2026, 1, 31),
                new BigDecimal("30.00"),
                1L,
                2L,
                null,
                "manual_confirm",
                null));
  }

  private ChargeInstanceService.Action action() {
    var a = new ChargeInstanceService.Action();
    a.setRequestId("test_request_0123456789");
    a.setExpectedRevision(0L);
    a.setActualAmount(new BigDecimal("25.00"));
    a.setAccountId(1L);
    a.setClassificationId(2L);
    a.setPaidAt("2026-01-31");
    return a;
  }

  @Test
  void manualDoesNotAffectActualUntilConfirmedAndRetriesAreStable() {
    var row = pending();
    assertEquals(0, jdbc.queryForObject("select count(*) from test_records", Integer.class));
    var result = tx.execute(t -> service.act(row.getId(), "owner", "confirm", action()));
    assertEquals("posted", result.getStatus());
    assertEquals(new BigDecimal("25.00"), result.getActualAmount());
    tx.execute(t -> service.act(row.getId(), "owner", "confirm", action()));
    assertEquals(1, jdbc.queryForObject("select count(*) from test_records", Integer.class));
  }

  @Test
  void concurrentConfirmationCreatesOneRecord() throws Exception {
    var row = pending();
    var pool = Executors.newFixedThreadPool(2);
    try {
      var gate = new CountDownLatch(1);
      Callable<PxChargeInstance> call =
          () -> {
            gate.await();
            return tx.execute(t -> service.act(row.getId(), "owner", "confirm", action()));
          };
      var a = pool.submit(call);
      var b = pool.submit(call);
      gate.countDown();
      assertEquals(
          a.get(10, TimeUnit.SECONDS).getRecordId(), b.get(10, TimeUnit.SECONDS).getRecordId());
      assertEquals(1, jdbc.queryForObject("select count(*) from test_records", Integer.class));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void rollbackRemovesRecordWhenInstanceUpdateFails() {
    var row = pending();
    jdbc.execute(
        "alter table px_charge_instance add constraint reject_posted check(status <> 'posted')");
    assertThrows(
        Exception.class,
        () -> tx.execute(t -> service.act(row.getId(), "owner", "confirm", action())));
    assertEquals(0, jdbc.queryForObject("select count(*) from test_records", Integer.class));
    assertEquals(
        "pending", jdbc.queryForObject("select status from px_charge_instance", String.class));
  }

  @Test
  void earlierRequestRemainsIdempotentAfterLaterTransition() {
    var row = pending();
    var failed = action();
    failed.setReason("余额不足");
    var first = tx.execute(t -> service.act(row.getId(), "owner", "payment-failed", failed));
    var confirmed = action();
    confirmed.setRequestId("second_request_0123456789");
    confirmed.setExpectedRevision(first.getRevision());
    tx.execute(t -> service.act(row.getId(), "owner", "confirm", confirmed));
    assertEquals(
        "payment_failed",
        tx.execute(t -> service.act(row.getId(), "owner", "payment-failed", failed)).getStatus());
    assertEquals(
        "posted", jdbc.queryForObject("select status from px_charge_instance", String.class));
    assertEquals(1, jdbc.queryForObject("select count(*) from test_records", Integer.class));
  }

  @Test
  void changedPayloadAndOtherOwnerAreRejected() {
    var row = pending();
    assertThrows(
        Exception.class,
        () -> tx.execute(t -> service.act(row.getId(), "other", "confirm", action())));
    tx.execute(t -> service.act(row.getId(), "owner", "confirm", action()));
    var changed = action();
    changed.setActualAmount(new BigDecimal("26.00"));
    assertThrows(
        Exception.class,
        () -> tx.execute(t -> service.act(row.getId(), "owner", "confirm", changed)));
  }

  @Test
  void filteredDetailReconcilesActualChangesAndKeepsReceiptsPrivate() {
    jdbc.execute("drop table if exists px_bookkeeping_record");
    jdbc.execute(
        "create table px_bookkeeping_record(id bigint primary key,money decimal(18,2),account"
            + " bigint,type bigint,del_flag int)");
    var row = pending();
    assertEquals(1, service.filtered("owner", 1, 30, "subscription", 1L, "pending").size());
    assertTrue(service.filtered("other", 1, 30, null, null, null).isEmpty());
    var posted = tx.execute(t -> service.act(row.getId(), "owner", "confirm", action()));
    jdbc.update("insert into px_bookkeeping_record values(?,40,1,2,0)", posted.getRecordId());
    var detail = tx.execute(t -> service.detail(row.getId(), "owner"));
    assertEquals(
        new BigDecimal("40.00"), ((PxChargeInstance) detail.get("instance")).getActualAmount());
    assertEquals(1, ((java.util.List<?>) detail.get("history")).size());
    assertThrows(Exception.class, () -> tx.execute(t -> service.detail(row.getId(), "other")));
    jdbc.update("update px_bookkeeping_record set del_flag=1");
    assertEquals(1, service.filtered("owner", 1, 30, null, null, "voided").size());
    assertTrue(service.filtered("owner", 1, 30, null, null, "posted").isEmpty());
  }
}
