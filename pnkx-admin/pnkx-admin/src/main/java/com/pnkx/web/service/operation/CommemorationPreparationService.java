package com.pnkx.web.service.operation;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.domain.po.PxToDo;
import com.pnkx.life.service.IPxToDoService;
import com.pnkx.life.service.impl.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommemorationPreparationService {
  private final JdbcTemplate db;
  private final IPxToDoService todos;

  public CommemorationPreparationService(JdbcTemplate db, IPxToDoService todos) {
    this.db = db;
    this.todos = todos;
  }

  private LocalDate today() {
    return LocalDate.now(SubscriptionSchedule.ZONE);
  }

  private boolean truth(Object v) {
    return Boolean.TRUE.equals(v) || "1".equals(String.valueOf(v));
  }

  private LocalDate date(Object v) {
    if (v instanceof java.sql.Timestamp t) return t.toLocalDateTime().toLocalDate();
    return LocalDate.parse(String.valueOf(v).substring(0, 10));
  }

  private LocalDate occurrence(Map<String, Object> source, LocalDate day) {
    return CommemorationSchedule.next(date(source.get("date")), truth(source.get("repeat")), day);
  }

  private String fingerprint(Map<String, Object> row) {
    return ShoppingWorkflowService.hash(
        row.get("id")
            + ":"
            + row.get("create_by")
            + ":"
            + row.get("name")
            + ":"
            + date(row.get("date"))
            + ":"
            + truth(row.get("repeat")));
  }

  private Map<String, Object> source(Long id, String actor) {
    var rows =
        db.queryForList(
            "select id,name,date,`repeat`,create_by from px_commemoration_day where id=? and"
                + " del_flag=0 for update",
            id);
    if (rows.isEmpty()) throw new ServiceException("SOURCE_UNAVAILABLE");
    var row = rows.get(0);
    String owner = String.valueOf(row.get("create_by"));
    if (!owner.equals(actor)) {
      // Lock active memberships too, so exit/role changes cannot race a preparation write.
      var access =
          db.queryForList(
              "select g.id from px_data_group g join px_data_group_member o on o.group_id=g.id join"
                  + " px_data_group_member a on a.group_id=g.id where g.status='0' and o.user_id=?"
                  + " and o.status='active' and a.user_id=? and a.status='active' and a.role in"
                  + " ('owner','admin','member') and"
                  + " JSON_CONTAINS(COALESCE(g.visibility_json,JSON_ARRAY()),JSON_QUOTE('commemoration'))"
                  + " for update",
              owner,
              actor);
      if (access.isEmpty()) throw new ServiceException("SOURCE_UNAVAILABLE");
    }
    return row;
  }

  public List<Map<String, Object>> upcoming(List<String> owners, String actor, LocalDate day) {
    if (owners.isEmpty()) return List.of();
    var rows =
        db.queryForList(
            "select id,name,date,`repeat`,create_by from px_commemoration_day where del_flag=0 and"
                + " date is not null and create_by in ("
                + String.join(",", Collections.nCopies(owners.size(), "?"))
                + ")",
            owners.toArray());
    var result = new ArrayList<Map<String, Object>>();
    for (var row : rows) {
      LocalDate occurrence = occurrence(row, day);
      if (occurrence == null || occurrence.isAfter(day.plusDays(30))) continue;
      row.put("occurrenceDate", occurrence.toString());
      row.put("daysUntil", ChronoUnit.DAYS.between(day, occurrence));
      row.put(
          "suggestedDueDate",
          occurrence.minusDays(7).isBefore(day)
              ? day.toString()
              : occurrence.minusDays(7).toString());
      var preparations =
          db.queryForList(
              "select p.id,p.status,p.todo_id,t.status todo_status from"
                  + " px_commemoration_preparation p left join px_to_do t on t.id=p.todo_id and"
                  + " t.create_by=p.owner_id where p.owner_id=? and p.source_id=? and"
                  + " p.occurrence_date=?",
              actor,
              row.get("id"),
              occurrence.toString());
      if (!preparations.isEmpty()) {
        var prep = preparations.get(0);
        row.put("preparationId", prep.get("id"));
        row.put(
            "preparationStatus",
            prep.get("todo_id") != null
                ? (prep.get("todo_status") == null
                    ? "removed"
                    : truth(prep.get("todo_status")) ? "completed" : "accepted")
                : prep.get("status"));
      }
      // Only expose the occurrence and title, not raw source metadata/remarks.
      row.remove("date");
      row.remove("repeat");
      row.remove("create_by");
      result.add(row);
    }
    result.sort(
        Comparator.comparing((Map<String, Object> r) -> String.valueOf(r.get("occurrenceDate")))
            .thenComparingLong(r -> ((Number) r.get("id")).longValue()));
    return result.stream().limit(30).toList();
  }

  @Transactional(rollbackFor = Exception.class)
  public JSONObject preview(Long id, String actor, JSONObject input) {
    var row = source(id, actor);
    LocalDate occurrence = occurrence(row, today());
    if (occurrence == null || occurrence.isAfter(today().plusDays(30)))
      throw new ServiceException("仅支持未来30天内的纪念日筹备");
    if (!occurrence.toString().equals(input.getString("occurrenceDate")))
      throw new ServiceException("纪念日日期已变化，请刷新");
    String content = input.getString("content");
    if (content == null || content.isBlank() || content.length() > 2000)
      throw new ServiceException("请填写1至2000字的准备事项");
    LocalDate due = LocalDate.parse(input.getString("dueDate"));
    if (due.isBefore(today()) || due.isAfter(occurrence))
      throw new ServiceException("准备日期应在今天至纪念日之间");
    var rows =
        db.queryForList(
            "select id,todo_id from px_commemoration_preparation where owner_id=? and source_id=?"
                + " and occurrence_date=? for update",
            actor,
            id,
            occurrence.toString());
    if (!rows.isEmpty() && rows.get(0).get("todo_id") != null)
      throw new ServiceException("本次已创建准备待办，请在待办中修改");
    String token = UUID.randomUUID().toString();
    db.update(
        "insert into"
            + " px_commemoration_preparation(owner_id,source_id,occurrence_date,source_hash,preview_token,content,due_date,expires_at)"
            + " values(?,?,?,?,?,?,?,date_add(now(),interval 15 minute)) on duplicate key update"
            + " source_hash=values(source_hash),preview_token=values(preview_token),content=values(content),due_date=values(due_date),expires_at=values(expires_at),status='draft',update_time=now()",
        actor,
        id,
        occurrence.toString(),
        fingerprint(row),
        token,
        content.trim(),
        due.toString());
    var result = new JSONObject();
    result.put("previewId", token);
    result.put("content", content.trim());
    result.put("dueDate", due.toString());
    result.put("occurrenceDate", occurrence.toString());
    result.put("sourceName", row.get("name"));
    return result;
  }

  @Transactional(rollbackFor = Exception.class)
  public JSONObject commit(String token, String actor) {
    var refs =
        db.queryForList(
            "select source_id from px_commemoration_preparation where preview_token=? and"
                + " owner_id=?",
            token,
            actor);
    if (refs.isEmpty()) throw new ServiceException("SOURCE_UNAVAILABLE");
    var source = source(((Number) refs.get(0).get("source_id")).longValue(), actor);
    var rows =
        db.queryForList(
            "select *,expires_at<=now() expired from px_commemoration_preparation where"
                + " preview_token=? and owner_id=? for update",
            token,
            actor);
    if (rows.isEmpty()) throw new ServiceException("预览已更新，请重新确认");
    var p = rows.get(0);
    if (p.get("todo_id") != null) return result(p.get("todo_id"));
    if (!"draft".equals(p.get("status")) || truth(p.get("expired")))
      throw new ServiceException("预览已过期或忽略，请重新预览");
    if (!fingerprint(source).equals(p.get("source_hash"))
        || !Objects.equals(occurrence(source, today()), date(p.get("occurrence_date"))))
      throw new ServiceException("纪念日已变化，请重新预览");
    if (date(p.get("due_date")).isBefore(today())) throw new ServiceException("准备日期已过，请重新预览");
    var todo = new PxToDo();
    todo.setContent(String.valueOf(p.get("content")));
    todo.setCreateBy(actor);
    todo.setPerformer(actor);
    todo.setStatus(false);
    todo.setPlanEndTime(p.get("due_date") + " 23:59:59");
    todo.setClientUuid("commemoration:" + p.get("id"));
    if (todos.insertPxToDo(todo) != 1 || todo.getId() == null)
      throw new ServiceException("准备待办创建失败");
    db.update(
        "update px_commemoration_preparation set todo_id=?,status='accepted',update_time=now()"
            + " where id=?",
        todo.getId(),
        p.get("id"));
    return result(todo.getId());
  }

  private JSONObject result(Object id) {
    var r = new JSONObject();
    r.put("todoId", id);
    r.put("status", "accepted");
    return r;
  }

  @Transactional(rollbackFor = Exception.class)
  public void dismiss(Long id, String actor, String expected) {
    var row = source(id, actor);
    LocalDate due = occurrence(row, today());
    if (due == null || !due.toString().equals(expected) || due.isAfter(today().plusDays(30)))
      throw new ServiceException("纪念日已变化，请刷新");
    var rows =
        db.queryForList(
            "select todo_id from px_commemoration_preparation where owner_id=? and source_id=? and"
                + " occurrence_date=? for update",
            actor,
            id,
            due.toString());
    if (!rows.isEmpty() && rows.get(0).get("todo_id") != null)
      throw new ServiceException("已创建准备待办，请在待办中处理");
    db.update(
        "insert into"
            + " px_commemoration_preparation(owner_id,source_id,occurrence_date,source_hash,preview_token,content,due_date,expires_at,status)"
            + " values(?,?,?,?,?,'',?,now(),'dismissed') on duplicate key update"
            + " status='dismissed',update_time=now()",
        actor,
        id,
        due.toString(),
        fingerprint(row),
        UUID.randomUUID().toString(),
        due.toString());
  }
}
