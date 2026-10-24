package com.pnkx.web.controller.life;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.life.domain.po.PxToDo;
import com.pnkx.life.mapper.PxToDoMapper;
import com.pnkx.life.service.impl.ReminderEventService;
import com.pnkx.life.service.impl.SubscriptionSchedule;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/life/action-center")
public class PxActionCenterController {
  private final PxToDoMapper todos;
  private final ReminderEventService reminders;

  @jakarta.annotation.Resource
  private com.pnkx.life.service.impl.ChargeExecutionService chargeExecutions;

  @jakarta.annotation.Resource
  private com.pnkx.web.service.operation.CommemorationPreparationService preparations;

  private final JdbcTemplate db;

  public PxActionCenterController(
      PxToDoMapper todos, ReminderEventService reminders, JdbcTemplate db) {
    this.todos = todos;
    this.reminders = reminders;
    this.db = db;
  }

  @GetMapping("/spaces")
  public AjaxResult spaces() {
    return AjaxResult.success(
        db.queryForList(
            "select g.id,g.group_name from px_data_group g join px_data_group_member m on"
                + " m.group_id=g.id where m.user_id=? and m.status='active' and g.status='0' order"
                + " by g.id",
            SecurityUtils.getUserId()));
  }

  @GetMapping
  public AjaxResult get(
      @RequestParam(defaultValue = "personal") String scope,
      @RequestParam(required = false) Long groupId,
      @RequestParam(required = false) String date,
      @RequestParam(defaultValue = "1") int page) {
    if (page < 1 || page > 10000) throw new ServiceException("无效页码");
    if (!Set.of("personal", "group").contains(scope)) throw new ServiceException("无效空间");
    String owner = SecurityUtils.getUserId();
    var result = new LinkedHashMap<String, Object>();
    var errors = new ArrayList<String>();
    LocalDate day = date == null ? LocalDate.now(SubscriptionSchedule.ZONE) : LocalDate.parse(date);
    String today = day.toString();
    result.put("date", today);
    result.put("scope", scope);
    result.put("timezone", "Asia/Shanghai");
    result.put("generatedAt", Instant.now().toString());
    List<String> owners = List.of(owner);
    boolean allowTodo = true, allowMeal = true, allowCommemoration = true, canEdit = true;
    if ("group".equals(scope)) {
      var groups =
          db.queryForList(
              "select g.visibility_json,m.role from px_data_group g join px_data_group_member m on"
                  + " m.group_id=g.id where g.id=? and g.status='0' and m.user_id=? and"
                  + " m.status='active'",
              groupId,
              owner);
      if (groups.isEmpty()) throw new ServiceException("PERMISSION_DENIED");
      var modules = JSON.parseArray(String.valueOf(groups.get(0).get("visibility_json")));
      allowTodo = modules.contains("todo");
      allowMeal = modules.contains("meal");
      allowCommemoration = modules.contains("commemoration");
      canEdit = !"viewer".equals(groups.get(0).get("role"));
      owners =
          db.queryForList(
              "select cast(user_id as char) from px_data_group_member where group_id=? and"
                  + " status='active' order by user_id",
              String.class,
              groupId);
    }
    result.put("canEdit", canEdit);
    final List<String> visible = owners;
    try {
      var q = new PxToDo();
      q.setStatus(false);
      q.getParams().put("scopeUserIds", visible);
      q.getParams().put("scopeAll", false);
      var due =
          allowTodo
              ? todos.selectPxToDoList(q).stream()
                  .filter(t -> visible.contains(t.getCreateBy()))
                  .filter(
                      t ->
                          t.getPlanEndTime() != null
                              && t.getPlanEndTime().length() >= 10
                              && t.getPlanEndTime().substring(0, 10).compareTo(today) <= 0)
                  .sorted(Comparator.comparing(PxToDo::getPlanEndTime).thenComparing(PxToDo::getId))
                  .toList()
              : List.<PxToDo>of();
      result.put("todoCount", due.size());
      result.put("todos", due.stream().skip((page - 1L) * 20).limit(20).toList());
    } catch (Exception e) {
      errors.add("todo");
    }
    if ("personal".equals(scope)) {
      try {
        result.put("reminders", reminders.list(owner));
        result.put("generationFailures", chargeExecutions.list(owner));
      } catch (Exception e) {
        errors.add("reminder");
      }
    }
    try {
      if (allowMeal) {
        var args = new ArrayList<Object>();
        args.add(today);
        args.addAll(visible);
        result.put(
            "meals",
            db.queryForList(
                "select id,title,meal_type,servings from px_meal_plan where plan_date=? and"
                    + " del_flag='0' and create_by in ("
                    + String.join(",", Collections.nCopies(visible.size(), "?"))
                    + ") order by meal_type,id",
                args.toArray()));
      }
    } catch (Exception e) {
      errors.add("meal");
    }
    try {
      if (allowCommemoration)
        result.put("commemorations", preparations.upcoming(visible, owner, day));
    } catch (Exception e) {
      errors.add("commemoration");
    }
    result.put("partialErrors", errors);
    return AjaxResult.success(result);
  }

  @GetMapping("/preferences")
  public AjaxResult preferences() {
    var rows =
        db.queryForList(
            "select entries_json,revision from px_action_preference where owner_id=?",
            SecurityUtils.getUserId());
    return AjaxResult.success(
        rows.isEmpty()
            ? Map.of("entries_json", "[\"todo\",\"meal\",\"shopping\"]", "revision", 0)
            : rows.get(0));
  }

  @PutMapping("/preferences")
  @Transactional
  public AjaxResult savePreferences(@RequestBody JSONObject input) {
    var entries = input.getJSONArray("entries");
    if (entries == null
        || entries.size() > 6
        || entries.stream().distinct().count() != entries.size()
        || !Set.of("todo", "meal", "shopping", "bookkeeping", "diary", "report")
            .containsAll(entries)) throw new ServiceException("请选择最多6个不同入口");
    String owner = SecurityUtils.getUserId();
    db.update(
        "insert ignore into px_action_preference(owner_id,entries_json) values(?,'[]')", owner);
    if (db.update(
            "update px_action_preference set entries_json=?,revision=revision+1,update_time=now()"
                + " where owner_id=? and revision=?",
            entries.toJSONString(),
            owner,
            input.getLong("expectedRevision"))
        != 1) throw new ServiceException("REVISION_CONFLICT");
    return preferences();
  }
}
