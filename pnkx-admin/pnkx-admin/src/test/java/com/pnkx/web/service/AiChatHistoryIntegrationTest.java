package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;

import com.pnkx.web.service.operation.AiChatHistoryService;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@EnabledIfEnvironmentVariable(
    named = "PNKX_TEST_MYSQL_URL",
    matches = "jdbc:mysql://127\\.0\\.0\\.1:.*?/pnkx_test.*")
class AiChatHistoryIntegrationTest {
  JdbcTemplate db;
  AiChatHistoryService service;

  @BeforeEach
  void setup() {
    var ds =
        new DriverManagerDataSource(
            System.getenv("PNKX_TEST_MYSQL_URL"), "root", "pnkx_local_test_only");
    db = new JdbcTemplate(ds);
    db.execute("drop table if exists px_ai_chat_message");
    for (String sql : readMigration()) if (!sql.isBlank()) db.execute(sql);
    service = new AiChatHistoryService(db);
  }

  private String[] readMigration() {
    try {
      return java.nio.file.Files.readString(
              java.nio.file.Path.of("src/main/resources/db/migration/V1.8.3__ai_chat_history.sql"))
          .split(";");
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  void seed(String owner, String session, String role, String content) {
    service.append(owner, session, role, content, null);
  }

  @Test
  void appendAndPageOwnMessagesAscending() {
    service.append("1", "s1", "user", "第一条", null);
    service.append("1", "s1", "assistant", "第二条", "plan-abc");
    service.append("1", "s2", "user", "别的会话", null);
    service.append("2", "s1", "user", "他人的消息", null);
    var rows = service.messages("1", "s1", null, 50);
    assertEquals(2, rows.size());
    assertEquals("user", rows.get(0).get("role"));
    assertEquals("assistant", rows.get(1).get("role"));
    assertEquals("plan-abc", rows.get(1).get("plan_id"));
    Long secondId = ((Number) rows.get(1).get("id")).longValue();
    assertEquals(1, service.messages("1", "s1", secondId, 50).size());
    assertEquals(0, service.messages("1", "s9", null, 50).size());
    assertTrue(service.messages("2", "s1", null, 50).size() == 1);
  }

  @Test
  void sessionsDeriveTitleFromFirstUserMessageAndOrderByLatest() {
    seed("1", "old", "user", "很早以前的第一条消息内容已经超过二十个字了呀");
    seed("1", "old", "assistant", "回复");
    seed("1", "new", "user", "最新问题");
    var sessions = service.sessions("1");
    assertEquals(2, sessions.size());
    assertEquals("new", sessions.get(0).get("sessionId"));
    assertEquals("最新问题", sessions.get(0).get("title"));
    assertEquals("old", sessions.get(1).get("sessionId"));
    assertEquals(21, ((String) sessions.get(1).get("title")).length());
    assertTrue(((String) sessions.get(1).get("title")).endsWith("…"));
    seed("2", "mine", "user", "他人会话");
    assertEquals(2, service.sessions("1").size());
  }

  @Test
  void invalidInputRejectedAndContentCapped() {
    assertThrows(Exception.class, () -> service.append("1", "bad id!", "user", "x", null));
    assertThrows(Exception.class, () -> service.append("1", "s1", "system", "x", null));
    assertThrows(Exception.class, () -> service.append("1", "s1", "user", " ", null));
    assertThrows(Exception.class, () -> service.append("1", "s1", "user", "x", "bad plan!"));
    assertThrows(Exception.class, () -> service.messages("1", null, null, 50));
    service.append("1", "s1", "user", "x".repeat(60000), null);
    assertEquals(
        50000,
        ((String) service.messages("1", "s1", null, 50).get(0).get("content")).length());
  }

  @Test
  void clearRemovesOnlyOwnSession() {
    seed("1", "s1", "user", "我的");
    seed("2", "s1", "user", "他的");
    assertEquals(1, service.clear("1", "s1"));
    assertEquals(0, service.messages("1", "s1", null, 50).size());
    assertEquals(1, service.messages("2", "s1", null, 50).size());
  }
}
