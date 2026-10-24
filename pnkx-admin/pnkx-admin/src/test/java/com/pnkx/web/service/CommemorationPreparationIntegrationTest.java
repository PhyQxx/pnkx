package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.life.domain.po.*;
import com.pnkx.life.service.IPxToDoService;
import com.pnkx.life.service.impl.SubscriptionSchedule;
import com.pnkx.web.service.operation.CommemorationPreparationService;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;

@EnabledIfEnvironmentVariable(
    named = "PNKX_TEST_MYSQL_URL",
    matches = "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/pnkx_test(?:\\?.*)?")
class CommemorationPreparationIntegrationTest {
  JdbcTemplate db;
  TransactionTemplate tx;
  CommemorationPreparationService service;
  IPxToDoService todos;
  DriverManagerDataSource ds;
  LocalDate today = LocalDate.now(SubscriptionSchedule.ZONE), occurrence = today.plusDays(10);

  @BeforeEach
  void setup() throws Exception {
    ds =
        new DriverManagerDataSource(
            System.getenv("PNKX_TEST_MYSQL_URL"), "root", "pnkx_local_test_only");
    db = new JdbcTemplate(ds);
    tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
    for (String table :
        List.of(
            "px_commemoration_preparation",
            "px_commemoration_day",
            "px_to_do",
            "px_data_group",
            "px_data_group_member")) db.execute("drop table if exists " + table);
    db.execute(
        Files.readString(
            Path.of("src/main/resources/db/migration/V1.8.1__commemoration_preparations.sql")));
    db.execute(
        "create table px_commemoration_day(id bigint primary key,name varchar(50),date"
            + " datetime,`repeat` bit,create_by varchar(64),del_flag bit,icon varchar(30),order_num"
            + " bigint,version varchar(50),client_uuid varchar(80),create_time datetime,update_by"
            + " varchar(64),update_time datetime,remark varchar(255))");
    db.execute(
        "create table px_to_do(id bigint auto_increment primary key,content varchar(2000),status"
            + " bit,create_by varchar(64),client_uuid varchar(100) unique)");
    db.execute(
        "create table px_data_group(id bigint primary key,status char(1),visibility_json json)");
    db.execute(
        "create table px_data_group_member(group_id bigint,user_id bigint,role varchar(20),status"
            + " varchar(20))");
    db.update(
        "insert into px_commemoration_day(id,name,date,`repeat`,create_by,del_flag)"
            + " values(1,'纪念日',?,0,'1',0),(2,'他人纪念日',?,0,'9',0)",
        occurrence.toString(),
        occurrence.toString());
    db.update("insert into px_data_group values(1,'0','[\"commemoration\"]')");
    db.update(
        "insert into px_data_group_member"
            + " values(1,1,'owner','active'),(1,2,'member','active'),(1,3,'viewer','active')");
    todos = mock(IPxToDoService.class);
    when(todos.insertPxToDo(any()))
        .thenAnswer(
            inv -> {
              PxToDo t = inv.getArgument(0);
              db.update(
                  "insert into px_to_do(content,status,create_by,client_uuid) values(?,0,?,?)",
                  t.getContent(),
                  t.getCreateBy(),
                  t.getClientUuid());
              t.setId(
                  db.queryForObject(
                      "select id from px_to_do where client_uuid=?",
                      Long.class,
                      t.getClientUuid()));
              return 1;
            });
    service = new CommemorationPreparationService(db, todos);
  }

  JSONObject input() {
    var b = new JSONObject();
    b.put("content", "提前确认安排");
    b.put("dueDate", today.plusDays(3).toString());
    b.put("occurrenceDate", occurrence.toString());
    return b;
  }

  String preview(String actor) {
    return tx.execute(t -> service.preview(1L, actor, input())).getString("previewId");
  }

  @Test
  void concurrentConfirmationCreatesOneTodoAndShowsCompletion() throws Exception {
    var token = preview("1");
    var pool = Executors.newFixedThreadPool(2);
    try {
      var a = pool.submit(() -> tx.execute(t -> service.commit(token, "1")));
      var b = pool.submit(() -> tx.execute(t -> service.commit(token, "1")));
      assertEquals(a.get().getLong("todoId"), b.get().getLong("todoId"));
    } finally {
      pool.shutdownNow();
    }
    assertEquals(1, db.queryForObject("select count(*) from px_to_do", Integer.class));
    db.update("update px_to_do set status=1");
    assertEquals(
        "completed", service.upcoming(List.of("1"), "1", today).get(0).get("preparationStatus"));
  }

  @Test
  void sourceChangesAndExpiredPreviewCannotCreateTodo() {
    String token = preview("1");
    db.update("update px_commemoration_day set name='已改名' where id=1");
    assertThrows(Exception.class, () -> tx.execute(t -> service.commit(token, "1")));
    String next = preview("1");
    db.update(
        "update px_commemoration_preparation set expires_at=date_sub(now(),interval 1 minute)");
    assertThrows(Exception.class, () -> tx.execute(t -> service.commit(next, "1")));
    assertEquals(0, db.queryForObject("select count(*) from px_to_do", Integer.class));
  }

  @Test
  void viewerOutsiderAndExitedMemberCannotWrite() {
    assertThrows(Exception.class, () -> preview("3"));
    assertThrows(Exception.class, () -> preview("9"));
    var token = preview("2");
    db.update("update px_data_group_member set status='left' where user_id=2");
    assertThrows(Exception.class, () -> tx.execute(t -> service.commit(token, "2")));
    assertThrows(Exception.class, () -> tx.execute(t -> service.commit(token, "1")));
  }

  @Test
  void dismissIsOccurrenceSpecificAndNewPreviewInvalidatesOldToken() {
    var old = preview("1");
    tx.execute(
        t -> {
          service.dismiss(1L, "1", occurrence.toString());
          return null;
        });
    assertThrows(Exception.class, () -> tx.execute(t -> service.commit(old, "1")));
    var fresh = preview("1");
    assertThrows(Exception.class, () -> tx.execute(t -> service.commit(old, "1")));
    tx.execute(t -> service.commit(fresh, "1"));
    assertThrows(
        Exception.class,
        () ->
            tx.execute(
                t -> {
                  service.dismiss(1L, "1", occurrence.toString());
                  return null;
                }));
  }

  @Test
  void todoWriteFailureRollsBackAndLeavesPreviewRetryable() {
    var token = preview("1");
    doAnswer(
            inv -> {
              throw new RuntimeException("write failed");
            })
        .when(todos)
        .insertPxToDo(any());
    assertThrows(Exception.class, () -> tx.execute(t -> service.commit(token, "1")));
    assertEquals(
        "draft",
        db.queryForObject("select status from px_commemoration_preparation", String.class));
    assertNull(db.queryForObject("select todo_id from px_commemoration_preparation", Long.class));
  }

  @Test
  void invalidDeadlineAndStaleOccurrenceAreRejected() {
    var input = input();
    input.put("dueDate", occurrence.plusDays(1).toString());
    assertThrows(Exception.class, () -> tx.execute(t -> service.preview(1L, "1", input)));
    input.put("dueDate", today.toString());
    input.put("occurrenceDate", today.toString());
    assertThrows(Exception.class, () -> tx.execute(t -> service.preview(1L, "1", input)));
    assertEquals(1, service.upcoming(List.of("1"), "1", today).size());
  }

  @Test
  void legacyRepeatBranchCannotBypassOwnerOrDeletedFilter() throws Exception {
    db.update("update px_commemoration_day set date=now(),`repeat`=1");
    db.update(
        "insert into px_commemoration_day(id,name,date,`repeat`,create_by,del_flag)"
            + " values(3,'已删除',now(),1,'1',1)");
    var factory = new org.mybatis.spring.SqlSessionFactoryBean();
    factory.setDataSource(ds);
    var config = new org.apache.ibatis.session.Configuration();
    config.getTypeAliasRegistry().registerAlias("PxCommemorationDay", PxCommemorationDay.class);
    factory.setConfiguration(config);
    factory.setMapperLocations(
        new org.springframework.core.io.FileSystemResource(
            "../pnkx-life/src/main/resources/mapper/PxCommemorationDayMapper.xml"));
    var mapper =
        new org.mybatis.spring.SqlSessionTemplate(factory.getObject())
            .getMapper(com.pnkx.life.mapper.PxCommemorationDayMapper.class);
    var q = new PxCommemorationDay();
    q.setDate(new java.util.Date());
    q.getParams().put("scopeUserIds", List.of("1"));
    q.getParams().put("scopeAll", false);
    assertEquals(
        List.of(1L),
        mapper.selectPxCommemorationDayList(q).stream().map(PxCommemorationDay::getId).toList());
    assertEquals(
        List.of(1L),
        mapper.getCommemorationDayList(q).stream().map(PxCommemorationDay::getId).toList());
  }
}
