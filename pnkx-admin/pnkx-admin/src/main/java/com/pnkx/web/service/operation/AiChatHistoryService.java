package com.pnkx.web.service.operation;

import com.pnkx.common.exception.ServiceException;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * AI 聊天历史：仅保存和返回本人消息，不参与模型上下文组装。
 * 会话由客户端生成编号；服务端按会话聚合并派生标题。
 */
@Service
public class AiChatHistoryService {
  private final JdbcTemplate db;

  public AiChatHistoryService(JdbcTemplate db) {
    this.db = db;
  }

  private void requireSession(String sessionId) {
    if (sessionId == null || !sessionId.matches("[A-Za-z0-9_-]{1,64}"))
      throw new ServiceException("会话编号无效");
  }

  public Map<String, Object> append(String owner, String sessionId, String role, String content, String planId) {
    requireSession(sessionId);
    if (!Set.of("user", "assistant").contains(role)) throw new ServiceException("消息角色无效");
    if (content == null || content.isBlank()) throw new ServiceException("消息内容不能为空");
    if (content.length() > 50000) content = content.substring(0, 50000);
    if (planId != null && !planId.matches("[A-Za-z0-9_-]{1,64}"))
      throw new ServiceException("计划编号无效");
    db.update(
        "insert into px_ai_chat_message(owner_id,session_id,role,content,plan_id)"
            + " values(?,?,?,?,?)",
        owner,
        sessionId,
        role,
        content,
        planId);
    Long id =
        db.queryForObject(
            "select max(id) from px_ai_chat_message where owner_id=? and session_id=?",
            Long.class,
            owner,
            sessionId);
    Map<String, Object> out = new HashMap<>();
    out.put("id", id);
    return out;
  }

  /** 升序返回本人会话消息；beforeId 提供时返回更早一段，用于向上翻页。 */
  public List<Map<String, Object>> messages(String owner, String sessionId, Long beforeId, int limit) {
    requireSession(sessionId);
    int size = Math.min(Math.max(limit, 1), 100);
    List<Object> args = new ArrayList<>(List.of(owner, sessionId));
    String where = "owner_id=? and session_id=?";
    if (beforeId != null) {
      where += " and id<?";
      args.add(beforeId);
    }
    args.add(size);
    var rows =
        db.queryForList(
            "select id,role,content,plan_id,create_time from px_ai_chat_message where "
                + where
                + " order by id desc limit ?",
            args.toArray());
    Collections.reverse(rows);
    return rows;
  }

  /** 最近会话列表：标题取会话内第一条用户消息前 20 字。 */
  public List<Map<String, Object>> sessions(String owner) {
    var rows =
        db.queryForList(
            "select session_id,count(*) total,max(id) last_id from px_ai_chat_message where"
                + " owner_id=? group by session_id order by max(id) desc limit 30",
            owner);
    List<Map<String, Object>> out = new ArrayList<>();
    for (var row : rows) {
      String sessionId = (String) row.get("session_id");
      String title =
          db.queryForObject(
              "select content from px_ai_chat_message where owner_id=? and session_id=? and"
                  + " role='user' order by id limit 1",
              String.class,
              owner,
              sessionId);
      Map<String, Object> item = new HashMap<>();
      item.put("sessionId", sessionId);
      item.put("title", truncate(title));
      item.put("total", row.get("total"));
      item.put("lastId", row.get("last_id"));
      out.add(item);
    }
    return out;
  }

  private String truncate(String text) {
    if (text == null || text.isBlank()) return "新对话";
    String trimmed = text.trim();
    return trimmed.length() <= 20 ? trimmed : trimmed.substring(0, 20) + "…";
  }

  public int clear(String owner, String sessionId) {
    requireSession(sessionId);
    return db.update(
        "delete from px_ai_chat_message where owner_id=? and session_id=?", owner, sessionId);
  }
}
