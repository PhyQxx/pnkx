package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;

import com.pnkx.web.service.sharing.SharePreviewService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@EnabledIfEnvironmentVariable(
    named = "PNKX_TEST_MYSQL_URL",
    matches = "jdbc:mysql://127\\.0\\.0\\.1:.*?/pnkx_test.*")
class SharePreviewIntegrationTest {
  JdbcTemplate db;
  SharePreviewService service;

  @BeforeEach
  void setup() {
    db =
        new JdbcTemplate(
            new DriverManagerDataSource(
                System.getenv("PNKX_TEST_MYSQL_URL"), "root", "pnkx_local_test_only"));
    db.execute("drop table if exists px_bookkeeping_budget");
    db.execute(
        "create table px_bookkeeping_budget(id bigint primary key,month varchar(20),type_id"
            + " bigint,amount decimal(10,2),remark text,del_flag int,create_by varchar(64))");
    db.execute("drop table if exists px_data_group_member");
    db.execute(
        "create table px_data_group_member(group_id bigint,user_id varchar(64),status"
            + " varchar(20))");
    db.update("insert into px_data_group_member values(1,'1','active'),(1,'2','active')");
    for (int i = 1; i <= 23; i++)
      db.update(
          "insert into px_bookkeeping_budget values(?,'2026-10',1,100,?,0,'1')",
          i,
          "<img src=\"https://example.invalid/private.png\">" + "x".repeat(2100));
    db.update(
        "insert into px_bookkeeping_budget values(90,'2026-10',1,999,'foreign"
            + " secret',0,'2'),(91,'2026-10',1,100,'deleted',1,'1')");
    service = new SharePreviewService(db);
  }

  @Test
  void onlyOwnActiveRowsArePaged() {
    var first = service.preview("1", 1L, "budget", 1);
    assertEquals(23, first.getIntValue("total"));
    assertEquals(20, first.getJSONArray("items").size());
    assertEquals(3, service.preview("1", 1L, "budget", 2).getJSONArray("items").size());
    assertFalse(first.toJSONString().contains("foreign secret"));
    assertFalse(first.toJSONString().contains("deleted"));
  }

  @Test
  void referencesAreTextAndLongFieldsAreBounded() {
    var row = service.preview("1", null, "budget", 1).getJSONArray("items").getJSONObject(0);
    assertTrue(row.getBooleanValue("truncated"));
    assertEquals(2000, row.getJSONObject("fields").getString("remark").length());
    assertEquals(
        "https://example.invalid/private.png", row.getJSONArray("references").getString(0));
  }

  @Test
  void scopeAndWhitelistAreEnforced() {
    assertThrows(Exception.class, () -> service.preview("3", 1L, "budget", 1));
    assertThrows(Exception.class, () -> service.preview("1", null, "note", 1));
    assertThrows(
        Exception.class,
        () -> service.preview("1", null, "budget;drop table px_bookkeeping_budget", 1));
    assertThrows(Exception.class, () -> service.preview("1", null, "budget", 0));
  }
}
