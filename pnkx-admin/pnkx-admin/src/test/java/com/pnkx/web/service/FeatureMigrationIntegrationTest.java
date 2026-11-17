package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

@EnabledIfEnvironmentVariable(
    named = "PNKX_TEST_MYSQL_URL",
    matches = "jdbc:mysql://127\\.0\\.0\\.1:.*?/pnkx_test.*")
class FeatureMigrationIntegrationTest {
  @Test
  void allFeatureMigrationsPreserveLegacyMeaning() throws Exception {
    String url = System.getenv("PNKX_TEST_MYSQL_URL"),
        schema = "pnkx_test_migration_" + UUID.randomUUID().toString().replace("-", "");
    var admin = new JdbcTemplate(new DriverManagerDataSource(url, "root", "pnkx_local_test_only"));
    admin.execute("create database " + schema);
    try {
      var ds =
          new DriverManagerDataSource(
              url.replace("/pnkx_test?", "/" + schema + "?"), "root", "pnkx_local_test_only");
      var db = new JdbcTemplate(ds);
      var folder = Path.of("src/main/resources/db/migration");
      for (String file :
          List.of(
              "V1.1.1__reminder_engine.sql",
              "V1.1.8__shopping_meal_recipe.sql",
              "V1.1.9__subscription.sql",
              "V1.4.8__bookkeeping_recurring.sql")) {
        var matcher =
            Pattern.compile(
                    "CREATE TABLE IF NOT EXISTS.*?;", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)
                .matcher(Files.readString(folder.resolve(file)));
        while (matcher.find()) db.execute(matcher.group());
      }
      db.update(
          "insert into px_subscription(name,amount,cycle,next_payment_date,enabled,create_by)"
              + " values('旧订阅',30,'monthly','2026-10-31',1,'1')");
      db.update("insert into px_recipe(id,title,servings,create_by) values(1,'旧菜谱',3,'1')");
      db.update(
          "insert into px_meal_plan(plan_date,meal_type,recipe_id,title,create_by)"
              + " values('2026-10-24',3,1,'旧计划','1')");
      db.update("insert into px_shopping_list(id,name,create_by) values(1,'旧清单','1')");
      db.update(
          "insert into px_shopping_item(list_id,name,quantity,checked,create_by)"
              + " values(1,'盐','适量',1,'1')");
      // V1.8.6 会清理菜单/任务/提醒/自动化中的订阅残留，测试库需建对应最小表
      db.execute("create table sys_menu(menu_id bigint primary key, component varchar(255))");
      db.execute("create table sys_role_menu(role_id bigint, menu_id bigint)");
      db.execute("create table sys_job(job_id bigint primary key, invoke_target varchar(255))");
      db.execute(
          "create table px_automation_rule(id bigint primary key, trigger_type varchar(32),"
              + " action_type varchar(64))");
      db.update("insert into sys_menu(menu_id, component) values(9001, 'px/life/subscription/index')");
      db.update("insert into sys_role_menu(role_id, menu_id) values(1, 9001)");
      db.update("insert into sys_job(job_id, invoke_target) values(9001, 'subscriptionTask.generateDueRecords')");
      db.update(
          "insert into px_automation_rule(id, trigger_type, action_type)"
              + " values(9001, 'subscription_due', 'subscription_charge')");
      var files =
          Files.list(folder)
              .filter(p -> p.getFileName().toString().matches("V1\\.(6\\.[4-9]|[78]\\.[0-9]+)__.*"))
              .sorted()
              .toList();
      try (var connection = ds.getConnection()) {
        for (var file : files)
          ScriptUtils.executeSqlScript(connection, new FileSystemResource(file));
      }
      assertEquals(
          0,
          db.queryForObject(
              "select count(*) from information_schema.tables where table_schema=database()"
                  + " and table_name='px_subscription'",
              Integer.class));
      assertEquals(0, db.queryForObject("select count(*) from sys_menu", Integer.class));
      assertEquals(0, db.queryForObject("select count(*) from sys_job", Integer.class));
      assertEquals(0, db.queryForObject("select count(*) from px_automation_rule", Integer.class));
      assertEquals(3, db.queryForObject("select servings from px_meal_plan", Integer.class));
      assertEquals("适量", db.queryForObject("select quantity from px_shopping_item", String.class));
      assertEquals(
          "purchased",
          db.queryForObject("select fulfillment_state from px_shopping_item", String.class));
      assertEquals(0, db.queryForObject("select count(*) from px_charge_instance", Integer.class));
    } finally {
      admin.execute("drop database " + schema);
    }
  }
}
