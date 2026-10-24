package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.pnkx.framework.web.service.DataPermissionService;
import com.pnkx.life.mapper.*;
import com.pnkx.life.service.impl.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;

@EnabledIfEnvironmentVariable(
    named = "PNKX_TEST_MYSQL_URL",
    matches = "jdbc:mysql://127\\.0\\.0\\.1:.*?/pnkx_test.*")
class ShoppingWorkflowIntegrationTest {
  JdbcTemplate db;
  TransactionTemplate tx;
  ShoppingWorkflowService service;
  ShoppingActionService actions;

  @BeforeEach
  void setup() throws Exception {
    var ds =
        new DriverManagerDataSource(
            System.getenv("PNKX_TEST_MYSQL_URL"), "root", "pnkx_local_test_only");
    db = new JdbcTemplate(ds);
    tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
    for (String table :
        List.of(
            "px_shopping_list",
            "px_shopping_item",
            "px_recipe",
            "px_recipe_ingredient",
            "px_meal_plan",
            "px_operation_preview",
            "px_shopping_source",
            "px_shopping_batch")) db.execute("drop table if exists " + table);
    String base =
        Files.readString(
            Path.of("src/main/resources/db/migration/V1.1.8__shopping_meal_recipe.sql"));
    var matcher = Pattern.compile("CREATE TABLE IF NOT EXISTS.*?;", Pattern.DOTALL).matcher(base);
    while (matcher.find()) db.execute(matcher.group());
    db.execute("alter table px_shopping_list add column planned_date date null");
    for (String sql :
        Files.readString(Path.of("src/main/resources/db/migration/V1.6.7__shopping_workflow.sql"))
            .split(";")) if (!sql.isBlank()) db.execute(sql);
    var f = new SqlSessionFactoryBean();
    f.setDataSource(ds);
    f.setTypeAliasesPackage("com.pnkx.life.domain.po");
    f.setMapperLocations(
        java.util.stream.Stream.of(
                "PxShoppingListMapper",
                "PxShoppingItemMapper",
                "PxRecipeMapper",
                "PxRecipeIngredientMapper",
                "PxMealPlanMapper")
            .map(
                n -> new FileSystemResource("../pnkx-life/src/main/resources/mapper/" + n + ".xml"))
            .toArray(FileSystemResource[]::new));
    var session = new SqlSessionTemplate(f.getObject());
    var permissions =
        new DataPermissionService() {
          @Override
          public List<Long> getVisibleUserIds(String module) {
            return List.of(1L);
          }

          @Override
          public boolean canWrite(String owner, String module) {
            return "1".equals(owner);
          }
        };
    service =
        new ShoppingWorkflowService(
            db,
            session.getMapper(PxShoppingListMapper.class),
            session.getMapper(PxShoppingItemMapper.class),
            session.getMapper(PxMealPlanMapper.class),
            session.getMapper(PxRecipeMapper.class),
            session.getMapper(PxRecipeIngredientMapper.class),
            permissions);
    actions =
        new ShoppingActionService(
            db,
            session.getMapper(PxShoppingItemMapper.class),
            session.getMapper(PxShoppingListMapper.class),
            permissions,
            mock(PxBookkeepingRecordMapper.class),
            mock(PxBookkeepingAccountMapper.class),
            mock(PxBookkeepingClassificationMapper.class));
    db.update("insert into px_shopping_list(id,name,create_by) values(1,'采购','1')");
    db.update(
        "insert into px_recipe(id,title,servings,create_by) values(1,'菜一',1,'1'),(2,'菜二',1,'1')");
    db.update(
        "insert into px_recipe_ingredient(id,recipe_id,name,quantity)"
            + " values(1,1,'番茄','200g'),(2,2,'番茄','0.3kg')");
    db.update(
        "insert into px_meal_plan(id,plan_date,meal_type,recipe_id,title,create_by)"
            + " values(1,'2026-10-24',3,1,'菜一','1'),(2,'2026-10-24',3,2,'菜二','1')");
  }

  private String preview() {
    return service.preview(1L, "2026-10-24", "2026-10-24", "1").getString("previewId");
  }

  @Test
  void mergesUnitsAndRepeatedGenerationDoesNotMultiply() {
    String id = preview();
    tx.execute(t -> service.commit(id, "1"));
    assertEquals("500g", db.queryForObject("select quantity from px_shopping_item", String.class));
    tx.execute(t -> service.commit(id, "1"));
    String second = preview();
    tx.execute(t -> service.commit(second, "1"));
    assertEquals(1, db.queryForObject("select count(*) from px_shopping_item", Integer.class));
    assertEquals("500g", db.queryForObject("select quantity from px_shopping_item", String.class));
  }

  @Test
  void changedPlanRejectsStalePreview() {
    String id = preview();
    db.update("update px_meal_plan set servings=2 where id=1");
    assertThrows(Exception.class, () -> tx.execute(t -> service.commit(id, "1")));
    assertEquals(0, db.queryForObject("select count(*) from px_shopping_item", Integer.class));
  }

  @Test
  void deletingOnePlanPreservesOtherContribution() {
    String id = preview();
    tx.execute(t -> service.commit(id, "1"));
    db.update("update px_meal_plan set del_flag='1' where id=1");
    String next = preview();
    tx.execute(t -> service.commit(next, "1"));
    assertEquals("300g", db.queryForObject("select quantity from px_shopping_item", String.class));
  }

  @Test
  void purchasedQuantityIsNotOverwritten() {
    String id = preview();
    tx.execute(t -> service.commit(id, "1"));
    db.update(
        "update px_shopping_item set checked=1,fulfillment_state='purchased',revision=revision+1");
    db.update("update px_meal_plan set servings=2 where id=1");
    String next = preview();
    var result = tx.execute(t -> service.commit(next, "1"));
    assertFalse(result.getJSONArray("warnings").isEmpty());
    assertEquals("500g", db.queryForObject("select quantity from px_shopping_item", String.class));
  }

  @Test
  void recipeFromUnrelatedOwnerIsRejected() {
    db.update("update px_recipe set create_by='99' where id=1");
    assertThrows(Exception.class, this::preview);
  }

  @Test
  void checkoutRetriesReturnSameBatchAndCannotReuseKeyForAnotherList() {
    String id = preview();
    tx.execute(t -> service.commit(id, "1"));
    db.update("update px_shopping_item set fulfillment_state='purchased',checked=1");
    var input = new com.alibaba.fastjson.JSONObject();
    input.put("requestId", "checkout_test_0123456789");
    input.put("purchasedItemIds", db.queryForList("select id from px_shopping_item", Long.class));
    input.put("recordExpense", false);
    var first = tx.execute(t -> actions.checkout(1L, input, "1"));
    var again = tx.execute(t -> actions.checkout(1L, input, "1"));
    assertEquals(first.getLong("batchId"), again.getLong("batchId"));
    assertEquals(1, db.queryForObject("select count(*) from px_shopping_batch", Integer.class));
    db.update("insert into px_shopping_list(id,name,create_by) values(2,'其他','1')");
    assertThrows(Exception.class, () -> tx.execute(t -> actions.checkout(2L, input, "1")));
  }

  @Test
  void onlyOneConcurrentClaimWins() throws Exception {
    String id = preview();
    tx.execute(t -> service.commit(id, "1"));
    Long item = db.queryForObject("select id from px_shopping_item", Long.class);
    var input = new com.alibaba.fastjson.JSONObject();
    input.put(
        "expectedRevision", db.queryForObject("select revision from px_shopping_item", Long.class));
    var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
    try {
      var gate = new java.util.concurrent.CountDownLatch(1);
      java.util.concurrent.Callable<Boolean> task =
          () -> {
            gate.await();
            try {
              tx.execute(t -> actions.act(item, "claim", input, "1"));
              return true;
            } catch (Exception e) {
              return false;
            }
          };
      var a = pool.submit(task);
      var b = pool.submit(task);
      gate.countDown();
      assertNotEquals(
          a.get(10, java.util.concurrent.TimeUnit.SECONDS),
          b.get(10, java.util.concurrent.TimeUnit.SECONDS));
    } finally {
      pool.shutdownNow();
    }
  }
}
