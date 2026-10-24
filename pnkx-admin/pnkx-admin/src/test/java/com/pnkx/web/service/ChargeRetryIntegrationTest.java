package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;

import com.pnkx.life.service.impl.ChargeExecutionService;
import java.nio.file.*;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@EnabledIfEnvironmentVariable(
    named = "PNKX_TEST_MYSQL_URL",
    matches = "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/pnkx_test(?:\\?.*)?")
class ChargeRetryIntegrationTest {
  JdbcTemplate db;
  ChargeExecutionService service;
  LocalDate due = LocalDate.of(2026, 10, 24);

  @BeforeEach
  void setup() throws Exception {
    db =
        new JdbcTemplate(
            new DriverManagerDataSource(
                System.getenv("PNKX_TEST_MYSQL_URL"), "root", "pnkx_local_test_only"));
    service = new ChargeExecutionService(db);
    db.execute("drop table if exists px_charge_execution");
    db.execute(
        Files.readString(Path.of("src/main/resources/db/migration/V1.7.7__charge_execution.sql")));
    db.execute("alter table px_charge_execution add column next_retry_at datetime null");
  }

  @Test
  void retryDelayDoublesAndCapsThenSuccessClearsIt() {
    assertTrue(service.ready("1", "subscription", 1L, due));
    for (int i = 0; i < 9; i++) {
      service.failed("1", "subscription", 1L, due);
      int seconds =
          db.queryForObject(
              "select timestampdiff(second,now(),next_retry_at) from px_charge_execution",
              Integer.class);
      int expected = Math.min(3600, 60 * (1 << i));
      assertTrue(seconds <= expected && seconds >= expected - 2, "delay=" + seconds);
      assertFalse(service.ready("1", "subscription", 1L, due));
    }
    assertTrue(service.ready("2", "subscription", 1L, due));
    service.resolved("1", "subscription", 1L, due);
    assertTrue(service.ready("1", "subscription", 1L, due));
    assertNull(
        db.queryForObject(
            "select next_retry_at from px_charge_execution", java.sql.Timestamp.class));
  }

  @Test
  void expiredDelayAllowsRetryAndDismissDoesNotBypassDelay() {
    service.failed("1", "recurring", 1L, due);
    service.dismiss(db.queryForObject("select id from px_charge_execution", Long.class), "1");
    assertFalse(service.ready("1", "recurring", 1L, due));
    db.update("update px_charge_execution set next_retry_at=date_sub(now(),interval 1 second)");
    assertTrue(service.ready("1", "recurring", 1L, due));
  }
}
