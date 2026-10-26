package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alibaba.fastjson.*;
import com.pnkx.life.domain.po.PxBookkeepingRecord;
import com.pnkx.life.service.IPxBookkeepingRecordService;
import com.pnkx.web.service.operation.BookkeepingCreateTool;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.util.ReflectionTestUtils;

@EnabledIfEnvironmentVariable(
    named = "PNKX_TEST_MYSQL_URL",
    matches = "jdbc:mysql://127\\.0\\.0\\.1:.*?/pnkx_test.*")
class BookkeepingCreatePlanIntegrationTest {
  OperationPlanIntegrationTest f;
  BookkeepingCreateTool tool;
  IPxBookkeepingRecordService records;

  @BeforeEach
  void setup() throws Exception {
    f = new OperationPlanIntegrationTest();
    f.setup();
    f.db.execute("drop table if exists px_bookkeeping_record");
    f.db.execute("drop table if exists px_bookkeeping_account");
    f.db.execute("drop table if exists px_bookkeeping_classification");
    f.db.execute(
        "create table px_bookkeeping_record(id bigint auto_increment primary key,money"
            + " decimal(18,2),account bigint,type bigint,remark varchar(255),pay_time"
            + " datetime,del_flag int default 0,create_by varchar(64),client_uuid"
            + " varchar(64))");
    f.db.execute(
        "create table px_bookkeeping_account(id bigint primary key,account_name"
            + " varchar(64),create_by varchar(64),del_flag int default 0)");
    f.db.execute(
        "create table px_bookkeeping_classification(id bigint primary key,type_name"
            + " varchar(64),create_by varchar(64),del_flag int default 0)");
    f.db.update("insert into px_bookkeeping_account values(10,'我的钱包','1',0)");
    f.db.update("insert into px_bookkeeping_account values(11,'他人钱包','2',0)");
    f.db.update("insert into px_bookkeeping_classification values(20,'餐饮','1',0)");
    f.db.update("insert into px_bookkeeping_classification values(21,'他人分类','2',0)");
    records = mock(IPxBookkeepingRecordService.class);
    when(records.insertPxBookkeepingRecord(any()))
        .thenAnswer(
            inv -> {
              PxBookkeepingRecord r = inv.getArgument(0);
              f.db.update(
                  "insert into px_bookkeeping_record(money,account,type,remark,pay_time,"
                      + " del_flag,create_by,client_uuid) values(?,?,?,?,?,0,?,?)",
                  r.getMoney(),
                  r.getAccount(),
                  r.getType(),
                  r.getRemark(),
                  r.getPayTime(),
                  "1",
                  r.getClientUuid());
              r.setId(
                  f.db.queryForObject(
                      "select id from px_bookkeeping_record where client_uuid=?",
                      Long.class,
                      r.getClientUuid()));
              return 1;
            });
    tool = new BookkeepingCreateTool(f.db, records);
    ReflectionTestUtils.setField(f.service, "bookkeepingCreates", tool);
  }

  JSONObject args() {
    var parsed =
        JSON.parseObject(
            "{\"money\":\"24.5\",\"type\":\"1\",\"typeId\":20,\"accountId\":10,"
                + "\"payTime\":\""
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                + "\",\"remark\":\"午饭\",\"secondaryType\":\"餐饮\",\"account\":\"我的钱包\","
                + "\"typeObject\":{\"id\":20},\"accountObject\":{\"id\":10}}");
    tool.prepare(parsed, "1");
    return parsed;
  }

  String preview(JSONObject a) {
    var step = new JSONObject();
    step.put("toolCode", "bookkeeping.create");
    step.put("input", a);
    var steps = new JSONArray();
    steps.add(step);
    var input = new JSONObject();
    input.put("steps", steps);
    return f.service.preview(input, "1").getString("id");
  }

  @Test
  void previewCleansContextAndConfirmInsertsOnce() {
    JSONObject a = args();
    assertFalse(a.containsKey("typeObject"));
    assertFalse(a.containsKey("accountObject"));
    assertEquals("我的钱包", a.getString("accountName"));
    String id = preview(a);
    assertEquals(
        0, f.db.queryForObject("select count(*) from px_bookkeeping_record", Integer.class));
    assertEquals("succeeded", f.service.execute(id, "1").getString("status"));
    f.service.execute(id, "1");
    var row =
        f.db.queryForMap(
            "select money,account,type,remark,client_uuid from px_bookkeeping_record");
    assertEquals(new java.math.BigDecimal("24.50"), ((java.math.BigDecimal) row.get("money")));
    assertEquals(10L, ((Number) row.get("account")).longValue());
    assertEquals(20L, ((Number) row.get("type")).longValue());
    assertEquals("plan:" + id + ":1", row.get("client_uuid"));
  }

  @Test
  void invalidMoneyTypeOrFuturePayTimeRejectedAtPreview() {
    JSONObject bad = args();
    bad.put("money", "十二块");
    assertThrows(Exception.class, () -> preview(bad));
    JSONObject negative = args();
    negative.put("money", -5);
    assertThrows(Exception.class, () -> preview(negative));
    JSONObject wrongType = args();
    wrongType.put("type", "2");
    assertThrows(Exception.class, () -> preview(wrongType));
    JSONObject future = args();
    future.put("payTime", LocalDateTime.now().plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    assertThrows(Exception.class, () -> preview(future));
    assertEquals(
        0, f.db.queryForObject("select count(*) from px_bookkeeping_record", Integer.class));
  }

  @Test
  void foreignAccountOrClassificationRejected() {
    JSONObject foreignAccount = args();
    foreignAccount.put("accountId", 11);
    assertThrows(Exception.class, () -> preview(foreignAccount));
    JSONObject foreignClassification = args();
    foreignClassification.put("typeId", 21);
    assertThrows(Exception.class, () -> preview(foreignClassification));
  }

  @Test
  void deletedAccountAfterPreviewFailsExecution() {
    String id = preview(args());
    f.db.update("update px_bookkeeping_account set del_flag=1 where id=10");
    assertEquals("failed", f.service.execute(id, "1").getString("status"));
    assertEquals(
        0, f.db.queryForObject("select count(*) from px_bookkeeping_record", Integer.class));
  }

  @Test
  void compensationRejectedAndReviseSupportsMoneyAndRemark() {
    String id = preview(args());
    assertEquals("succeeded", f.service.execute(id, "1").getString("status"));
    assertThrows(Exception.class, () -> f.service.compensate(id, "1"));

    JSONObject second = args();
    String draftId = preview(second);
    JSONObject patch = new JSONObject();
    patch.put("money", "12.5");
    patch.put("remark", "早饭");
    JSONObject revise = new JSONObject();
    revise.put("expectedRevision", 0);
    revise.put("fields", patch);
    JSONObject replacement = f.service.revise(draftId, revise, "1");
    assertNotEquals(draftId, replacement.getString("id"));
    assertEquals("cancelled", f.service.get(draftId, "1").getString("status"));
    JSONObject badField = new JSONObject();
    badField.put("expectedRevision", 0);
    badField.put("fields", JSON.parseObject("{\"typeId\":21}"));
    assertThrows(Exception.class, () -> f.service.revise(replacement.getString("id"), badField, "1"));
    assertEquals("succeeded", f.service.execute(replacement.getString("id"), "1").getString("status"));
    var row =
        f.db.queryForMap("select money,remark from px_bookkeeping_record where client_uuid=?",
            "plan:" + replacement.getString("id") + ":1");
    assertEquals(new java.math.BigDecimal("12.50"), ((java.math.BigDecimal) row.get("money")));
    assertEquals("早饭", row.get("remark"));
  }
}
