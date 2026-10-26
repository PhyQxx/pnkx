package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alibaba.fastjson.*;
import com.pnkx.blog.domain.vo.PxArticleVo;
import com.pnkx.blog.service.IPxArticleService;
import com.pnkx.life.domain.po.PxBook;
import com.pnkx.life.domain.po.PxDiary;
import com.pnkx.life.domain.po.PxNote;
import com.pnkx.life.service.IPxBookService;
import com.pnkx.life.service.IPxDiaryService;
import com.pnkx.life.service.IPxNoteService;
import com.pnkx.web.service.operation.ContentPublishTool;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.util.ReflectionTestUtils;

@EnabledIfEnvironmentVariable(
    named = "PNKX_TEST_MYSQL_URL",
    matches = "jdbc:mysql://127\\.0\\.0\\.1:.*?/pnkx_test.*")
class ContentPublishPlanIntegrationTest {
  OperationPlanIntegrationTest f;
  ContentPublishTool tool;
  boolean failArticle;

  @BeforeEach
  void setup() throws Exception {
    f = new OperationPlanIntegrationTest();
    f.setup();
    for (String table :
        List.of("px_note", "px_diary", "px_book", "px_article"))
      f.db.execute("drop table if exists " + table);
    f.db.execute(
        "create table px_note(id bigint auto_increment primary key,title"
            + " varchar(255),content text,create_by varchar(64),del_flag int default 0,client_uuid"
            + " varchar(100) unique)");
    f.db.execute(
        "create table px_diary(id bigint auto_increment primary key,title"
            + " varchar(255),content text,create_by varchar(64),del_flag int default 0)");
    f.db.execute(
        "create table px_book(id bigint auto_increment primary key,title"
            + " varchar(255),author varchar(128),description text,last_read_chapter_name"
            + " varchar(255),create_by varchar(64),del_flag char(1) default '0')");
    f.db.execute(
        "create table px_article(id bigint auto_increment primary key,title"
            + " varchar(255),content longtext,rich_text longtext,tag varchar(255),state"
            + " varchar(2),open varchar(2),create_by varchar(64))");
    f.db.update("insert into px_note(title,content,create_by) values('周报','本周完成开发','1')");
    f.db.update(
        "insert into px_diary(title,content,create_by) values('假期','去了海边','1')");
    f.db.update(
        "insert into px_book(title,author,description,last_read_chapter_name,create_by)"
            + " values('思考快与慢','卡尼曼','认知偏见','第3章','1')");
    f.db.update("insert into px_note(title,content,create_by) values('他人的笔记','x','2')");

    var notes = mock(IPxNoteService.class);
    doAnswer(
            inv -> {
              var rows =
                  f.db.queryForList(
                      "select * from px_note where id=?", inv.getArgument(0, Long.class));
              if (rows.isEmpty()) return null;
              var row = rows.get(0);
              var note = new PxNote();
              note.setId(((Number) row.get("id")).longValue());
              note.setTitle((String) row.get("title"));
              note.setContent((String) row.get("content"));
              note.setCreateBy((String) row.get("create_by"));
              return note;
            })
        .when(notes)
        .selectPxNoteById(anyLong());
    when(notes.insertPxNote(any()))
        .thenAnswer(
            inv -> {
              PxNote n = inv.getArgument(0);
              f.db.update(
                  "insert into px_note(title,content,create_by,del_flag,client_uuid)"
                      + " values(?,?,?,?,?)",
                  n.getTitle(),
                  n.getContent(),
                  n.getCreateBy(),
                  0,
                  n.getClientUuid());
              n.setId(
                  f.db.queryForObject(
                      "select id from px_note where client_uuid=?",
                      Long.class,
                      n.getClientUuid()));
              return n;
            });
    var diaries = mock(IPxDiaryService.class);
    when(diaries.selectPxDiaryById(anyLong()))
        .thenAnswer(
            inv -> {
              var rows =
                  f.db.queryForList(
                      "select * from px_diary where id=?", inv.getArgument(0, Long.class));
              if (rows.isEmpty()) return null;
              var row = rows.get(0);
              var diary = new PxDiary();
              diary.setId(((Number) row.get("id")).longValue());
              diary.setTitle((String) row.get("title"));
              diary.setContent((String) row.get("content"));
              diary.setCreateBy((String) row.get("create_by"));
              return diary;
            });
    var books = mock(IPxBookService.class);
    when(books.selectBookById(anyLong(), anyString()))
        .thenAnswer(
            inv -> {
              var rows =
                  f.db.queryForList(
                      "select * from px_book where id=? and create_by=? and del_flag='0'",
                      inv.getArgument(0, Long.class),
                      inv.getArgument(1, String.class));
              if (rows.isEmpty()) return null;
              var row = rows.get(0);
              var book = new PxBook();
              book.setId(((Number) row.get("id")).longValue());
              book.setTitle((String) row.get("title"));
              book.setAuthor((String) row.get("author"));
              book.setDescription((String) row.get("description"));
              book.setLastReadChapterName((String) row.get("last_read_chapter_name"));
              book.setCreateBy((String) row.get("create_by"));
              return book;
            });
    var articles = mock(IPxArticleService.class);
    when(articles.insertPxArticle(any()))
        .thenAnswer(
            inv -> {
              if (failArticle) throw new IllegalStateException("temporary failure");
              PxArticleVo a = inv.getArgument(0);
              f.db.update(
                  "insert into px_article(title,content,rich_text,tag,state,open,create_by)"
                      + " values(?,?,?,?,?,?,?)",
                  a.getTitle(),
                  a.getContent(),
                  a.getRichText(),
                  a.getTag(),
                  a.getState(),
                  a.getOpen(),
                  "1");
              return f.db.queryForObject("select max(id) from px_article", Integer.class);
            });
    tool = new ContentPublishTool(f.db, books, notes, diaries, articles);
    ReflectionTestUtils.setField(f.service, "contentPublishes", tool);
  }

  Long noteId() {
    return f.db.queryForObject("select id from px_note where title='周报'", Long.class);
  }

  Long bookId() {
    return f.db.queryForObject("select id from px_book", Long.class);
  }

  String preview(String action, Long sourceId) {
    var draft = new JSONObject(true);
    draft.put("generatedTitle", "草稿标题");
    draft.put("generatedContent", "生成的正文，已脱敏。");
    draft.put("generatedTags", "生活,记录");
    draft.put("privacyRisks", new JSONArray());
    var source =
        tool.loadSource(
            action, sourceId, "1");
    assertNotNull(source);
    tool.prepare(action, sourceId, draft, source, "1");
    var step = new JSONObject();
    step.put("toolCode", "content.publish");
    step.put("input", draft);
    var steps = new JSONArray();
    steps.add(step);
    var input = new JSONObject();
    input.put("steps", steps);
    return f.service.preview(input, "1").getString("id");
  }

  @Test
  void noteToArticleCreatesPrivateDraftOnce() {
    String id = preview("note_to_article", noteId());
    assertEquals(
        0, f.db.queryForObject("select count(*) from px_article", Integer.class));
    assertEquals("succeeded", f.service.execute(id, "1").getString("status"));
    f.service.execute(id, "1");
    var row = f.db.queryForMap("select title,tag,state,open from px_article");
    assertEquals("草稿标题", row.get("title"));
    assertEquals("生活,记录", row.get("tag"));
    assertEquals("0", row.get("state"));
    assertEquals("0", row.get("open"));
    assertEquals(
        1, f.db.queryForObject("select count(*) from px_article", Integer.class));
  }

  @Test
  void bookToArticleWritesNoteAndDraftAtomicallyThenRetrySucceeds() {
    String id = preview("book_to_article", bookId());
    failArticle = true;
    assertEquals("failed", f.service.execute(id, "1").getString("status"));
    assertEquals(
        0, f.db.queryForObject("select count(*) from px_note where client_uuid is not null",
            Integer.class));
    assertEquals(
        0, f.db.queryForObject("select count(*) from px_article", Integer.class));
    failArticle = false;
    assertEquals("succeeded", f.service.execute(id, "1").getString("status"));
    assertEquals(
        1,
        f.db.queryForObject(
            "select count(*) from px_note where client_uuid like 'plan:%'",
            Integer.class));
    assertEquals(
        1, f.db.queryForObject("select count(*) from px_article", Integer.class));
  }

  @Test
  void editedOrDeletedSourceAfterPreviewFails() {
    String id = preview("note_to_article", noteId());
    f.db.update("update px_note set content='内容已被编辑' where title='周报'");
    assertEquals("failed", f.service.execute(id, "1").getString("status"));
    assertEquals(
        0, f.db.queryForObject("select count(*) from px_article", Integer.class));

    String second = preview("note_to_article", noteId());
    f.db.update("delete from px_note where title='周报'");
    assertEquals("failed", f.service.execute(second, "1").getString("status"));
    assertEquals(
        0, f.db.queryForObject("select count(*) from px_article", Integer.class));
  }

  @Test
  void foreignSourceRejectedAtPreview() {
    Long foreign =
        f.db.queryForObject("select id from px_note where title='他人的笔记'", Long.class);
    assertNull(tool.loadSource("note_to_article", foreign, "1"));
    var draft = new JSONObject(true);
    draft.put("action", "note_to_article");
    draft.put("sourceId", foreign);
    draft.put("title", "越权草稿");
    draft.put("content", "正文");
    var step = new JSONObject();
    step.put("toolCode", "content.publish");
    step.put("input", draft);
    var steps = new JSONArray();
    steps.add(step);
    var input = new JSONObject();
    input.put("steps", steps);
    assertThrows(Exception.class, () -> f.service.preview(input, "1"));
  }

  @Test
  void compensationRejectedAndReviseSupportsTitleContentTags() {
    String id = preview("diary_to_article", 1L);
    JSONObject patch = new JSONObject();
    patch.put("title", "新标题");
    patch.put("tags", "随笔");
    JSONObject revise = new JSONObject();
    revise.put("expectedRevision", 0);
    revise.put("fields", patch);
    JSONObject replacement = f.service.revise(id, revise, "1");
    assertEquals("cancelled", f.service.get(id, "1").getString("status"));
    assertEquals("succeeded", f.service.execute(replacement.getString("id"), "1").getString("status"));
    assertEquals(
        "新标题", f.db.queryForObject("select title from px_article", String.class));
    assertThrows(Exception.class, () -> f.service.compensate(replacement.getString("id"), "1"));
  }
}
