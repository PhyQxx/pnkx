package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.life.service.impl.AssignmentService;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;

@EnabledIfEnvironmentVariable(
    named = "PNKX_TEST_MYSQL_URL",
    matches = "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/pnkx_test(?:\\?.*)?")
class AssignmentIntegrationTest {
  JdbcTemplate db;
  TransactionTemplate tx;
  AssignmentService service;

  @BeforeEach
  void setup() throws Exception {
    var ds =
        new DriverManagerDataSource(
            System.getenv("PNKX_TEST_MYSQL_URL"), "root", "pnkx_local_test_only");
    db = new JdbcTemplate(ds);
    tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
    service = new AssignmentService(db);
    for (String t :
        new String[] {
          "px_assignment_audit",
          "px_to_do",
          "px_shopping_item",
          "px_shopping_list",
          "px_data_group",
          "px_data_group_member"
        }) db.execute("drop table if exists " + t);
    db.execute(
        Files.readString(Path.of("src/main/resources/db/migration/V1.7.8__assignment_audit.sql")));
    db.execute(
        "create table px_to_do(id bigint primary key,create_by varchar(64),performer"
            + " varchar(64),status bit,update_by varchar(64),update_time datetime)");
    db.execute(
        "create table px_shopping_list(id bigint primary key,create_by varchar(64),del_flag"
            + " char(1))");
    db.execute(
        "create table px_shopping_item(id bigint primary key,list_id bigint,claimed_by"
            + " varchar(64),claimed_at datetime,fulfillment_state varchar(20),batch_id"
            + " bigint,del_flag char(1),revision bigint)");
    db.execute(
        "create table px_data_group(id bigint primary key,status char(1),visibility_json json)");
    db.execute(
        "create table px_data_group_member(group_id bigint,user_id bigint,role varchar(20),status"
            + " varchar(20))");
    db.update("insert into px_to_do values(1,'1','3',0,null,null)");
    db.update("insert into px_shopping_list values(1,'1','0')");
    db.update("insert into px_shopping_item values(1,1,'3',null,'needed',null,'0',0)");
    db.update("insert into px_data_group values(1,'0','[\"todo\",\"shopping\"]')");
    db.update(
        "insert into px_data_group_member"
            + " values(1,1,'owner','active'),(1,2,'admin','active'),(1,3,'member','active'),(1,4,'viewer','active'),(1,5,'member','left')");
  }

  JSONObject input(String target) {
    var b = new JSONObject();
    b.put("assignee", target);
    b.put("expectedAssignee", "3");
    b.put("reason", "调整本周分工");
    return b;
  }

  @Test
  void adminTransferUpdatesAssigneeAndAuditTogether() {
    tx.execute(t -> service.transfer("todo", 1L, "2", input("1")));
    assertEquals("1", db.queryForObject("select performer from px_to_do", String.class));
    var detail = service.detail("todo", 1L, "4", 1);
    assertFalse(detail.getBooleanValue("canTransfer"));
    assertEquals(1, detail.getJSONArray("history").size());
    assertEquals(
        "3", detail.getJSONArray("history").getJSONObject(0).getString("previous_assignee"));
  }

  @Test
  void viewerMemberOutsiderAndInactiveTargetsCannotTransfer() {
    for (String actor : new String[] {"3", "4", "9"})
      assertThrows(
          Exception.class, () -> tx.execute(t -> service.transfer("todo", 1L, actor, input("1"))));
    for (String target : new String[] {"4", "5", "9"})
      assertThrows(
          Exception.class,
          () -> tx.execute(t -> service.transfer("shopping", 1L, "2", input(target))));
    assertThrows(Exception.class, () -> service.detail("todo", 1L, "9", 1));
    assertEquals(0, db.queryForObject("select count(*) from px_assignment_audit", Integer.class));
  }

  @Test
  void revisionReasonAndCompletionAreChecked() {
    var stale = input("1");
    stale.put("expectedAssignee", "2");
    assertThrows(
        Exception.class, () -> tx.execute(t -> service.transfer("shopping", 1L, "2", stale)));
    var noReason = input("1");
    noReason.put("reason", " ");
    assertThrows(
        Exception.class, () -> tx.execute(t -> service.transfer("shopping", 1L, "2", noReason)));
    db.update("update px_shopping_item set fulfillment_state='purchased'");
    assertThrows(
        Exception.class, () -> tx.execute(t -> service.transfer("shopping", 1L, "2", input("1"))));
    db.update("update px_to_do set status=1");
    assertThrows(
        Exception.class, () -> tx.execute(t -> service.transfer("todo", 1L, "2", input("1"))));
  }

  @Test
  void auditFailureRollsBackBusinessChange() {
    db.execute("drop table px_assignment_audit");
    assertThrows(
        Exception.class, () -> tx.execute(t -> service.transfer("shopping", 1L, "2", input("1"))));
    assertEquals("3", db.queryForObject("select claimed_by from px_shopping_item", String.class));
  }

  @Test
  void roleRevocationAndUnsharedModuleRemoveAuthority() {
    assertTrue(service.detail("shopping", 1L, "2", 1).getBooleanValue("canTransfer"));
    db.update("update px_data_group_member set role='viewer' where user_id=2");
    assertThrows(
        Exception.class, () -> tx.execute(t -> service.transfer("shopping", 1L, "2", input("1"))));
    db.update("update px_data_group set visibility_json='[\"todo\"]'");
    assertThrows(Exception.class, () -> service.detail("shopping", 1L, "2", 1));
  }

  @Test
  void leavingOneOfSeveralTodoAssigneesPreservesOthersAndAudits() {
    db.update("update px_to_do set performer='2,3'");
    var spaces = org.mockito.Mockito.mock(com.pnkx.system.service.ISysDataGroupService.class);
    var cleanup = new com.pnkx.life.service.impl.SpaceAssignmentCleanup(db, spaces);
    org.springframework.test.util.ReflectionTestUtils.setField(cleanup, "assignments", service);
    tx.execute(
        t -> {
          cleanup.changed(new com.pnkx.common.event.SpaceMembershipChanged(1L, 3L));
          return null;
        });
    assertEquals("2", db.queryForObject("select performer from px_to_do", String.class));
    assertNull(db.queryForObject("select claimed_by from px_shopping_item", String.class));
    assertEquals(2, db.queryForObject("select count(*) from px_assignment_audit", Integer.class));
  }

  @Test
  void legacyTodoEditCannotOverwriteAConcurrentAssignment() {
    var todos = org.mockito.Mockito.mock(com.pnkx.life.mapper.PxToDoMapper.class);
    var permissions =
        org.mockito.Mockito.mock(com.pnkx.framework.web.service.DataPermissionService.class);
    var old = new com.pnkx.life.domain.po.PxToDo();
    old.setId(1L);
    old.setCreateBy("1");
    old.setPerformer("2");
    org.mockito.Mockito.when(todos.lockById(1L)).thenReturn(old);
    org.mockito.Mockito.when(permissions.canWrite("1", "todo")).thenReturn(true);
    var service = new com.pnkx.life.service.impl.PxToDoServiceImpl();
    org.springframework.test.util.ReflectionTestUtils.setField(service, "pxToDoMapper", todos);
    org.springframework.test.util.ReflectionTestUtils.setField(
        service, "dataPermissionService", permissions);
    var edit = new com.pnkx.life.domain.po.PxToDo();
    edit.setId(1L);
    edit.setPerformer("3");
    assertThrows(Exception.class, () -> service.updatePxToDo(edit));
    org.mockito.Mockito.verify(todos, org.mockito.Mockito.never())
        .updatePxToDo(org.mockito.ArgumentMatchers.any());
  }
}
