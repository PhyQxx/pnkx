package com.pnkx.life.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.exception.ServiceException;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentService {
  private final JdbcTemplate db;

  public AssignmentService(JdbcTemplate db) {
    this.db = db;
  }

  private String module(String type) {
    if (!Set.of("todo", "shopping").contains(type)) throw new ServiceException("不支持的分工类型");
    return type;
  }

  private Map<String, Object> source(String type, Long id, boolean lock) {
    module(type);
    String sql =
        "shopping".equals(type)
            ? "select i.id,l.create_by owner,i.claimed_by assignee,i.fulfillment_state"
                  + " state,i.batch_id batch from px_shopping_item i join px_shopping_list l on"
                  + " l.id=i.list_id where i.id=? and i.del_flag='0' and l.del_flag='0'"
            : "select id,create_by owner,performer assignee,status state from px_to_do where id=?";
    var rows = db.queryForList(sql + (lock ? " for update" : ""), id);
    if (rows.isEmpty()) throw new ServiceException("SOURCE_UNAVAILABLE");
    return rows.get(0);
  }

  private String sharedJoin() {
    return " from px_data_group g join px_data_group_member o on o.group_id=g.id join"
               + " px_data_group_member a on a.group_id=g.id where g.status='0' and"
               + " o.status='active' and a.status='active' and o.user_id=? and a.user_id=? and"
               + " JSON_CONTAINS(COALESCE(g.visibility_json,JSON_ARRAY()),JSON_QUOTE(?))";
  }

  private boolean readable(String owner, String actor, String type) {
    return owner.equals(actor)
        || db.queryForObject("select count(*)" + sharedJoin(), Integer.class, owner, actor, type)
            > 0;
  }

  private boolean manageable(String owner, String actor, String type) {
    return owner.equals(actor)
        || db.queryForObject(
                "select count(*)" + sharedJoin() + " and a.role in ('owner','admin')",
                Integer.class,
                owner,
                actor,
                type)
            > 0;
  }

  private List<Map<String, Object>> candidates(String owner, String actor, String type) {
    if (!manageable(owner, actor, type)) return List.of();
    var result = new ArrayList<Map<String, Object>>();
    result.add(Map.of("userId", owner));
    String sql =
        "select distinct t.user_id userId from px_data_group g join px_data_group_member o on"
            + " o.group_id=g.id join px_data_group_member a on a.group_id=g.id join"
            + " px_data_group_member t on t.group_id=g.id where g.status='0' and o.user_id=? and"
            + " o.status='active' and a.user_id=? and a.status='active' and (?=? or a.role in"
            + " ('owner','admin')) and t.status='active' and t.role in ('owner','admin','member')"
            + " and t.user_id<>? and"
            + " JSON_CONTAINS(COALESCE(g.visibility_json,JSON_ARRAY()),JSON_QUOTE(?))";
    result.addAll(db.queryForList(sql, owner, actor, owner, actor, owner, type));
    return result;
  }

  public JSONObject detail(String type, Long id, String actor, int page) {
    if (page < 1 || page > 10000) throw new ServiceException("分页参数无效");
    var row = source(type, id, false);
    String owner = String.valueOf(row.get("owner"));
    if (!readable(owner, actor, type)) throw new ServiceException("SOURCE_UNAVAILABLE");
    var result = new JSONObject();
    result.put("assignee", row.get("assignee"));
    result.put("canTransfer", manageable(owner, actor, type));
    result.put("candidates", candidates(owner, actor, type));
    result.put(
        "history",
        db.queryForList(
            "select id,previous_assignee,assignee,operator_id,action,reason,create_time from"
                + " px_assignment_audit where source_type=? and source_id=? order by id desc limit"
                + " 30 offset ?",
            type,
            id,
            (page - 1) * 30));
    return result;
  }

  @Transactional(rollbackFor = Exception.class)
  public JSONObject transfer(String type, Long id, String actor, JSONObject input) {
    var row = source(type, id, true);
    String owner = String.valueOf(row.get("owner"));
    db.queryForList(
        "select t.user_id from px_data_group g join px_data_group_member o on o.group_id=g.id join"
            + " px_data_group_member t on t.group_id=g.id where o.user_id=? for update",
        owner);
    if (!manageable(owner, actor, type)) throw new ServiceException("只有所有者或共享空间管理员可以转派");
    String target = input.getString("assignee"), reason = input.getString("reason");
    if (reason == null || reason.isBlank() || reason.length() > 500)
      throw new ServiceException("请填写1至500字的转派原因");
    String previous = row.get("assignee") == null ? null : String.valueOf(row.get("assignee"));
    if (!input.containsKey("expectedAssignee")
        || !Objects.equals(previous, input.getString("expectedAssignee")))
      throw new ServiceException("REVISION_CONFLICT");
    boolean completed =
        "shopping".equals(type)
            ? row.get("batch") != null || !"needed".equals(row.get("state"))
            : Boolean.TRUE.equals(row.get("state")) || "1".equals(String.valueOf(row.get("state")));
    if (completed) throw new ServiceException("已完成或已结算事项不能转派");
    if (candidates(owner, actor, type).stream()
        .noneMatch(x -> String.valueOf(x.get("userId")).equals(target)))
      throw new ServiceException("目标成员已退出、只读或不在可转派空间");
    if (Objects.equals(previous, target)) throw new ServiceException("新负责人未变化");
    if ("shopping".equals(type))
      db.update(
          "update px_shopping_item set claimed_by=?,claimed_at=now(),revision=revision+1 where"
              + " id=?",
          target,
          id);
    else
      db.update(
          "update px_to_do set performer=?,update_by=?,update_time=now() where id=?",
          target,
          actor,
          id);
    record(type, id, owner, previous, target, actor, "transfer", reason.trim());
    return detail(type, id, actor, 1);
  }

  public void record(
      String type,
      Long id,
      String owner,
      String previous,
      String target,
      String actor,
      String action,
      String reason) {
    db.update(
        "insert into"
            + " px_assignment_audit(source_type,source_id,owner_id,previous_assignee,assignee,operator_id,action,reason)"
            + " values(?,?,?,?,?,?,?,?)",
        type,
        id,
        owner,
        previous,
        target,
        actor,
        action,
        reason);
  }
}
