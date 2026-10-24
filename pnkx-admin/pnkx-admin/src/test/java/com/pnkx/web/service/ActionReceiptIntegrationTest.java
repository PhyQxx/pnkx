package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;

import com.pnkx.life.service.impl.ActionReceiptService;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;

@EnabledIfEnvironmentVariable(
    named = "PNKX_TEST_MYSQL_URL",
    matches = "jdbc:mysql://127\\.0\\.0\\.1:.*?/pnkx_test.*")
class ActionReceiptIntegrationTest {
  JdbcTemplate db;
  TransactionTemplate tx;
  ActionReceiptService service;

  @BeforeEach
  void setup() throws Exception {
    var ds =
        new DriverManagerDataSource(
            System.getenv("PNKX_TEST_MYSQL_URL"), "root", "pnkx_local_test_only");
    db = new JdbcTemplate(ds);
    tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
    db.execute("drop table if exists px_action_receipt");
    db.execute(
        Files.readString(Path.of("src/main/resources/db/migration/V1.7.1__action_receipts.sql")));
    db.execute("drop table if exists test_receipt_effect");
    db.execute("create table test_receipt_effect(id bigint auto_increment primary key,value int)");
    service = new ActionReceiptService(db);
  }

  Object mutate() {
    db.update("insert into test_receipt_effect(value) values(1)");
    return Map.of("status", "completed");
  }

  @Test
  void concurrentRetriesCommitOneEffect() throws Exception {
    var pool = Executors.newFixedThreadPool(2);
    try {
      var gate = new CountDownLatch(1);
      Callable<Object> task =
          () -> {
            gate.await();
            return tx.execute(
                t -> service.execute("1", "receipt_test_0123456789", "item:1:claim", this::mutate));
          };
      var a = pool.submit(task);
      var b = pool.submit(task);
      gate.countDown();
      assertEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
      assertEquals(1, db.queryForObject("select count(*) from test_receipt_effect", Integer.class));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void rejectedMutationRollsBackReceipt() {
    assertThrows(
        Exception.class,
        () ->
            tx.execute(
                t ->
                    service.execute(
                        "1",
                        "receipt_test_0123456789",
                        "item:1:claim",
                        () -> {
                          mutate();
                          throw new IllegalStateException("rollback");
                        })));
    assertEquals(0, db.queryForObject("select count(*) from test_receipt_effect", Integer.class));
    assertEquals(0, db.queryForObject("select count(*) from px_action_receipt", Integer.class));
  }

  @Test
  void changedPayloadCannotReuseRequest() {
    tx.execute(t -> service.execute("1", "receipt_test_0123456789", "item:1:claim", this::mutate));
    assertThrows(
        Exception.class,
        () ->
            tx.execute(
                t ->
                    service.execute("1", "receipt_test_0123456789", "item:2:claim", this::mutate)));
    assertEquals(1, db.queryForObject("select count(*) from test_receipt_effect", Integer.class));
  }
}
