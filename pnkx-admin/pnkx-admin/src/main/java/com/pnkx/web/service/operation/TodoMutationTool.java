package com.pnkx.web.service.operation;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.domain.po.PxToDo;
import com.pnkx.life.service.IPxToDoService;
import com.pnkx.life.service.impl.ShoppingWorkflowService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * AI 待办修改/完成/删除：预览绑定本人目标及内容快照，执行时锁定并核对快照，
 * 目标被并发编辑即失败要求重新生成；结果不提供自动撤销。
 */
@Service
public class TodoMutationTool {
  public static final int MAX_BATCH = 20;

  private final JdbcTemplate db;
  private final IPxToDoService todos;

  public TodoMutationTool(JdbcTemplate db, IPxToDoService todos) {
    this.db = db;
    this.todos = todos;
  }

  public boolean supports(String code) {
    return Set.of("todo.update", "todo.complete", "todo.delete").contains(code);
  }

  /** 把已解析的本人目标收敛为计划输入：目标编号、内容摘要与快照 hash。 */
  public void prepare(String code, JSONObject draft, List<PxToDo> targets) {
    if (targets == null || targets.isEmpty())
      throw new ServiceException("没有可操作的本人待办");
    if (targets.size() > MAX_BATCH)
      throw new ServiceException("一次最多处理" + MAX_BATCH + "条待办");
    JSONArray ids = new JSONArray();
    JSONArray hashes = new JSONArray();
    JSONArray views = new JSONArray();
    for (PxToDo todo : targets) {
      ids.add(todo.getId());
      hashes.add(ShoppingWorkflowService.hash(JSON.toJSONString(todo)));
      JSONObject view = new JSONObject(true);
      view.put("id", todo.getId());
      view.put("content", todo.getContent());
      view.put("done", Boolean.TRUE.equals(todo.getStatus()));
      views.add(view);
    }
    JSONObject input = new JSONObject(true);
    input.put("targetIds", ids);
    input.put("targetHashes", hashes);
    input.put("targets", views);
    if ("todo.update".equals(code)) {
      copyReal(input, draft, "newContent");
      copyReal(input, draft, "planStartTime");
      copyReal(input, draft, "planEndTime");
      copyReal(input, draft, "label");
    }
    draft.clear();
    draft.putAll(input);
  }

  private void copyReal(JSONObject to, JSONObject from, String key) {
    String value = from.getString(key);
    if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value)) to.put(key, value);
  }

  private List<Long> targets(JSONObject a) {
    JSONArray raw = a.getJSONArray("targetIds");
    JSONArray hashes = a.getJSONArray("targetHashes");
    if (raw == null || raw.isEmpty() || hashes == null || hashes.size() != raw.size())
      throw new ServiceException("目标待办无效，请重新生成草稿");
    if (raw.size() > MAX_BATCH) throw new ServiceException("一次最多处理" + MAX_BATCH + "条待办");
    List<Long> ids = new ArrayList<>();
    Set<Long> seen = new HashSet<>();
    for (int i = 0; i < raw.size(); i++) {
      Long id = raw.getLong(i);
      if (id == null || !seen.add(id)) throw new ServiceException("目标待办无效，请重新生成草稿");
      ids.add(id);
    }
    return ids;
  }

  private void checkTargets(JSONObject a, String owner, boolean lock, List<Long> ids) {
    if (lock) {
      String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
      var arguments = new ArrayList<Object>();
      arguments.addAll(ids);
      arguments.add(owner);
      db.queryForList(
          "select id from px_to_do where id in (" + placeholders + ") and create_by=? for update",
          arguments.toArray());
    }
    JSONArray hashes = a.getJSONArray("targetHashes");
    for (int i = 0; i < ids.size(); i++) {
      PxToDo current = todos.selectPxToDoById(ids.get(i));
      if (current == null || !owner.equals(current.getCreateBy()))
          throw new ServiceException("目标待办不存在或不属于本人，请重新生成草稿");
      if (!ShoppingWorkflowService.hash(JSON.toJSONString(current))
          .equals(hashes.getString(i)))
          throw new ServiceException("目标待办已被修改，请重新生成草稿");
    }
  }

  public void validate(String code, JSONObject a, String owner, boolean lock) {
    if (!supports(code)) throw new ServiceException("不支持的计划工具");
    List<Long> ids = targets(a);
    checkTargets(a, owner, lock, ids);
    if (!"todo.update".equals(code)) return;
    JSONObject fields = new JSONObject();
    copyReal(fields, a, "newContent");
    copyReal(fields, a, "planStartTime");
    copyReal(fields, a, "planEndTime");
    copyReal(fields, a, "label");
    if (fields.isEmpty()) throw new ServiceException("请提供修改后的内容或时间");
    String content = fields.getString("newContent");
    if (content != null && content.length() > 2000) throw new ServiceException("待办内容不能超过2000字");
    if (fields.getString("label") != null && fields.getString("label").length() > 100)
      throw new ServiceException("标签不能超过100字");
    for (String field : List.of("planStartTime", "planEndTime")) {
      String value = fields.getString(field);
      if (value == null) continue;
      try {
        if (value.length() == 10) java.time.LocalDate.parse(value);
        else
            LocalDateTime.parse(
                value,
                DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
                    .withResolverStyle(ResolverStyle.STRICT));
      } catch (Exception e) {
        throw new ServiceException("时间格式无效：" + field);
      }
    }
  }

  public JSONObject run(String code, JSONObject a, String key, String owner) {
    List<Long> ids = targets(a);
    int rows = 0;
    if ("todo.delete".equals(code)) {
      rows =
          ids.size() == 1
              ? todos.deletePxToDoById(ids.get(0))
              : todos.deletePxToDoByIds(ids.toArray(new Long[0]));
    } else {
      for (Long id : ids) {
        PxToDo update = new PxToDo();
        update.setId(id);
        update.setUpdateBy(owner);
        if ("todo.complete".equals(code)) {
          update.setStatus(true);
          update.setKanbanStatus(2);
          update.setFinishBy(owner);
          update.setFinishTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        } else {
          String value = a.getString("newContent");
          if (value != null) update.setContent(value);
          value = a.getString("planStartTime");
          if (value != null) update.setPlanStartTime(value);
          value = a.getString("planEndTime");
          if (value != null) update.setPlanEndTime(value);
          value = a.getString("label");
          if (value != null) update.setLabel(value);
        }
        rows += todos.updatePxToDo(update);
      }
    }
    if (rows <= 0) throw new ServiceException("待办未发生变化，请重新确认目标");
    var result = new JSONObject();
    result.put("sourceType", "todo_mutation");
    result.put("sourceId", ids.get(0));
    result.put("targetIds", ids);
    result.put("action", code);
    result.put("affected", rows);
    return result;
  }
}
