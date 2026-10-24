package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alibaba.fastjson.*;
import com.pnkx.life.domain.po.PxNote;
import com.pnkx.life.service.*;
import com.pnkx.web.service.operation.LocalCreateTools;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.util.ReflectionTestUtils;

@EnabledIfEnvironmentVariable(
    named = "PNKX_TEST_MYSQL_URL",
    matches = "jdbc:mysql://127\\.0\\.0\\.1:.*?/pnkx_test.*")
class LocalCreatePlanIntegrationTest {
  OperationPlanIntegrationTest f;
  LocalCreateTools local;

  @BeforeEach
  void setup() throws Exception {
    f = new OperationPlanIntegrationTest();
    f.setup();
    f.db.execute("drop table if exists px_note");
    f.db.execute(
        "create table px_note(id bigint auto_increment primary key,create_by varchar(64),del_flag"
            + " int,content text,client_uuid varchar(100) unique)");
    f.db.execute("drop table if exists px_note_folder");
    f.db.execute(
        "create table px_note_folder(id bigint primary key,create_by varchar(64),del_flag int)");
    f.db.update("insert into px_note_folder values(1,'1',0),(2,'2',0)");
    var notes = mock(IPxNoteService.class);
    when(notes.insertPxNote(any()))
        .thenAnswer(
            inv -> {
              PxNote n = inv.getArgument(0);
              f.db.update(
                  "insert into px_note(create_by,del_flag,content,client_uuid) values(?,0,?,?)",
                  n.getCreateBy(),
                  n.getContent(),
                  n.getClientUuid());
              n.setId(
                  f.db.queryForObject(
                      "select id from px_note where client_uuid=?", Long.class, n.getClientUuid()));
              return n;
            });
    when(notes.deletePxNoteById(anyLong()))
        .thenAnswer(
            inv ->
                f.db.update(
                    "update px_note set del_flag=1 where id=?", inv.getArgument(0, Long.class)));
    local =
        new LocalCreateTools(
            f.db, notes, mock(IPxDiaryService.class), mock(IPxCommemorationDayService.class));
    ReflectionTestUtils.setField(f.service, "localCreates", local);
  }

  JSONObject input(long folder) {
    return JSON.parseObject(
        "{\"steps\":[{\"toolCode\":\"note.create\",\"input\":{\"title\":\"测试\",\"content\":\"正文\",\"folderId\":"
            + folder
            + "}}]}");
  }

  @Test
  void repeatedConfirmationCreatesOnceAndCompensates() {
    String id = f.service.preview(input(1), "1").getString("id");
    assertEquals("succeeded", f.service.execute(id, "1").getString("status"));
    f.service.execute(id, "1");
    assertEquals(1, f.db.queryForObject("select count(*) from px_note", Integer.class));
    assertEquals("compensated", f.service.compensate(id, "1").getString("status"));
    assertEquals(1, f.db.queryForObject("select del_flag from px_note", Integer.class));
  }

  @Test
  void changedNoteCannotBeCompensated() {
    String id = f.service.preview(input(1), "1").getString("id");
    f.service.execute(id, "1");
    f.db.update("update px_note set content='后续编辑'");
    assertThrows(Exception.class, () -> f.service.compensate(id, "1"));
  }

  @Test
  void folderOwnershipIsRecheckedAtExecution() {
    assertThrows(Exception.class, () -> f.service.preview(input(2), "1"));
    String id = f.service.preview(input(1), "1").getString("id");
    f.db.update("update px_note_folder set del_flag=1 where id=1");
    assertEquals("failed", f.service.execute(id, "1").getString("status"));
    assertEquals(0, f.db.queryForObject("select count(*) from px_note", Integer.class));
  }

  @Test
  void invalidDatesNeverFallBackToToday() {
    for (String code : new String[] {"diary.create", "commemoration.create"})
      assertThrows(
          Exception.class,
          () ->
              local.validate(
                  code,
                  JSON.parseObject(
                      "{\"date\":\"2026-02-30\",\"content\":\"测试\",\"name\":\"生日\",\"repeat\":true}"),
                  "1",
                  false));
  }
}
