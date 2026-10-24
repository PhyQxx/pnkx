package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alibaba.fastjson.*;
import com.pnkx.life.domain.po.*;
import com.pnkx.life.service.*;
import com.pnkx.web.service.operation.ShoppingCreateTool;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.util.ReflectionTestUtils;

@EnabledIfEnvironmentVariable(
    named = "PNKX_TEST_MYSQL_URL",
    matches = "jdbc:mysql://127\\.0\\.0\\.1:.*?/pnkx_test.*")
class ShoppingCreatePlanIntegrationTest {
  OperationPlanIntegrationTest f;
  ShoppingCreateTool tool;
  IPxShoppingItemService items;
  boolean failSecond;

  @BeforeEach
  void setup() throws Exception {
    f = new OperationPlanIntegrationTest();
    f.setup();
    f.db.execute("drop table if exists px_shopping_item");
    f.db.execute("drop table if exists px_shopping_list");
    f.db.execute(
        "create table px_shopping_list(id bigint auto_increment primary key,name"
            + " varchar(64),create_by varchar(64),del_flag int default 0,create_time datetime"
            + " default current_timestamp,client_uuid varchar(64) unique)");
    f.db.execute(
        "create table px_shopping_item(id bigint auto_increment primary key,list_id bigint,name"
            + " varchar(128),quantity varchar(32),create_by varchar(64),client_uuid varchar(64)"
            + " unique)");
    var lists = mock(IPxShoppingListService.class);
    items = mock(IPxShoppingItemService.class);
    when(lists.insertPxShoppingList(any()))
        .thenAnswer(
            inv -> {
              PxShoppingList l = inv.getArgument(0);
              f.db.update(
                  "insert into px_shopping_list(name,create_by,client_uuid) values(?,?,?)",
                  l.getName(),
                  l.getCreateBy(),
                  l.getClientUuid());
              l.setId(
                  f.db.queryForObject(
                      "select id from px_shopping_list where client_uuid=?",
                      Long.class,
                      l.getClientUuid()));
              return 1;
            });
    when(items.insertPxShoppingItem(any()))
        .thenAnswer(
            inv -> {
              PxShoppingItem i = inv.getArgument(0);
              if (failSecond && "牛奶".equals(i.getName()))
                throw new IllegalStateException("temporary failure");
              f.db.update(
                  "insert into px_shopping_item(list_id,name,quantity,create_by,client_uuid)"
                      + " values(?,?,?,?,?)",
                  i.getListId(),
                  i.getName(),
                  i.getQuantity(),
                  i.getCreateBy(),
                  i.getClientUuid());
              i.setId(
                  f.db.queryForObject(
                      "select id from px_shopping_item where client_uuid=?",
                      Long.class,
                      i.getClientUuid()));
              return 1;
            });
    tool = new ShoppingCreateTool(f.db, lists, items);
    ReflectionTestUtils.setField(f.service, "shoppingCreates", tool);
  }

  JSONObject args() {
    return JSON.parseObject(
        "{\"items\":[{\"name\":\"鸡蛋\",\"quantity\":\"一盒\"},{\"name\":\"牛奶\",\"quantity\":\"两瓶\"}]}");
  }

  String preview(JSONObject a) {
    tool.prepare(a, "1");
    var step = new JSONObject();
    step.put("toolCode", "shopping.add");
    step.put("input", a);
    var steps = new JSONArray();
    steps.add(step);
    var input = new JSONObject();
    input.put("steps", steps);
    return f.service.preview(input, "1").getString("id");
  }

  @Test
  void previewDoesNotWriteAndRepeatedConfirmCreatesOnce() {
    String id = preview(args());
    assertEquals(0, f.db.queryForObject("select count(*) from px_shopping_list", Integer.class));
    assertEquals("succeeded", f.service.execute(id, "1").getString("status"));
    f.service.execute(id, "1");
    assertEquals(1, f.db.queryForObject("select count(*) from px_shopping_list", Integer.class));
    assertEquals(2, f.db.queryForObject("select count(*) from px_shopping_item", Integer.class));
    assertThrows(Exception.class, () -> f.service.compensate(id, "1"));
  }

  @Test
  void itemFailureRollsBackNewListAndEarlierItemsThenRetrySucceeds() {
    String id = preview(args());
    failSecond = true;
    assertEquals("failed", f.service.execute(id, "1").getString("status"));
    assertEquals(0, f.db.queryForObject("select count(*) from px_shopping_list", Integer.class));
    assertEquals(0, f.db.queryForObject("select count(*) from px_shopping_item", Integer.class));
    failSecond = false;
    assertEquals("succeeded", f.service.execute(id, "1").getString("status"));
    assertEquals(2, f.db.queryForObject("select count(*) from px_shopping_item", Integer.class));
  }

  @Test
  void targetIsBoundAtPreviewAndNotChangedToNewerList() {
    f.db.update("insert into px_shopping_list(id,name,create_by) values(1,'本周','1')");
    String id = preview(args());
    f.db.update("insert into px_shopping_list(id,name,create_by) values(2,'下周','1')");
    f.service.execute(id, "1");
    assertEquals(
        2,
        f.db.queryForObject(
            "select count(*) from px_shopping_item where list_id=1", Integer.class));
  }

  @Test
  void foreignDeletedAndRenamedTargetsFailClosed() {
    f.db.update("insert into px_shopping_list(id,name,create_by) values(1,'外人','2')");
    var a = args();
    a.put("listId", 1);
    assertThrows(Exception.class, () -> preview(a));
    f.db.update("update px_shopping_list set create_by='1'");
    String id = preview(a);
    f.db.update("update px_shopping_list set name='已改名'");
    assertEquals("failed", f.service.execute(id, "1").getString("status"));
    f.db.update("update px_shopping_list set del_flag=1");
    assertThrows(Exception.class, () -> preview(a));
    assertEquals(0, f.db.queryForObject("select count(*) from px_shopping_item", Integer.class));
  }

  @Test
  void invalidLengthsAreRejectedBeforeAnyWrite() {
    var a = args();
    a.getJSONArray("items").getJSONObject(0).put("quantity", "x".repeat(33));
    final var tooLong = a;
    assertThrows(Exception.class, () -> preview(tooLong));
    a = args();
    a.getJSONArray("items").getJSONObject(0).put("name", " ");
    final var invalid = a;
    assertThrows(Exception.class, () -> preview(invalid));
  }
}
