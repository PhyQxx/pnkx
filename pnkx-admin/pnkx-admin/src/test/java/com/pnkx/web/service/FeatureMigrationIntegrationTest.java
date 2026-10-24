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
          "auto_record",
          db.queryForObject("select posting_mode from px_subscription", String.class));
      assertEquals(31, db.queryForObject("select anchor_day from px_subscription", Integer.class));
      assertEquals(
          1, db.queryForObject("select legacy_resume_policy from px_subscription", Integer.class));
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
