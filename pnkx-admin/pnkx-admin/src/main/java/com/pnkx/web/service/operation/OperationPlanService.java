package com.pnkx.web.service.operation;

import com.alibaba.fastjson.*;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.framework.web.service.DataPermissionService;
import com.pnkx.life.domain.po.*;
import com.pnkx.life.mapper.*;
import com.pnkx.life.service.*;
import com.pnkx.life.service.impl.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 有限工具的持久化执行计划；每步事务同时提交业务结果与步骤状态。 */
@Service
public class OperationPlanService {
  @jakarta.annotation.Resource private LifeFeatureGate featureGate;
  @jakarta.annotation.Resource private LocalCreateTools localCreates;
  @jakarta.annotation.Resource private ShoppingCreateTool shoppingCreates;
  private final JdbcTemplate db;
  private final TransactionTemplate tx;
  private final IPxToDoService todos;
  private final IPxMealPlanService meals;
  private final PxRecipeMapper recipes;
  @jakarta.annotation.Resource private PxRecipeIngredientMapper ingredients;
  private final ShoppingWorkflowService shopping;
  private final DataPermissionService permissions;

  public OperationPlanService(
      JdbcTemplate db,
      PlatformTransactionManager manager,
      IPxToDoService todos,
      IPxMealPlanService meals,
      PxRecipeMapper recipes,
      ShoppingWorkflowService shopping,
      DataPermissionService permissions) {
    this.db = db;
    this.tx = new TransactionTemplate(manager);
    this.todos = todos;
    this.meals = meals;
    this.recipes = recipes;
    this.shopping = shopping;
    this.permissions = permissions;
  }

  public JSONObject preview(JSONObject input, String owner) {
    if (featureGate != null) featureGate.require("operationPlanV2", owner);
    var steps = input.getJSONArray("steps");
    if (steps == null || steps.isEmpty() || steps.size() > 20)
      throw new ServiceException("计划须包含1至20个步骤");
    var safe = new JSONArray();
    for (var step : steps.toJavaList(JSONObject.class)) {
      String tool = step.getString("toolCode");
      var args = step.getJSONObject("input");
      if (args == null) throw new ServiceException("缺少步骤参数");
      validate(tool, args, false, owner);
      var item = new JSONObject(true);
      item.put("toolCode", tool);
      item.put("input", args);
      safe.add(item);
    }
    String id = UUID.randomUUID().toString();
    String title = input.getString("title");
    if (title == null || title.isBlank()) title = "生活操作计划";
    if (title.length() > 255) throw new ServiceException("计划名称过长");
    final String name = title;
    tx.execute(
        t -> {
          db.update(
              "insert into px_operation_plan(id,owner_id,title,expires_at)"
                  + " values(?,?,?,date_add(now(),interval 15 minute))",
              id,
              owner,
              name);
          int i = 0;
          for (var step : safe.toJavaList(JSONObject.class))
            db.update(
                "insert into px_operation_step(plan_id,step_number,tool_code,input_json)"
                    + " values(?,?,?,?)",
                id,
                ++i,
                step.getString("toolCode"),
                step.getJSONObject("input").toJSONString());
          return null;
        });
    return get(id, owner);
  }

  /** 原草稿和新预览原子替换；旧编号不会执行修改后的内容。 */
  public JSONObject revise(String id, JSONObject input, String owner) {
    return tx.execute(
        t -> {
          var rows =
              db.queryForList(
                  "select *,expires_at<=now() as expired from px_operation_plan where id=? and"
                      + " owner_id=? for update",
                  id,
                  owner);
          if (rows.isEmpty()) throw new ServiceException("计划不存在或无权编辑");
          var plan = rows.get(0);
          // Return the original replacement for an identical retry, even after it expires or
          // changes.
          if (plan.get("replacement_id") != null) {
            JSONObject previous = JSON.parseObject((String) plan.get("revision_request_json"));
            if (previous != null && previous.equals(input))
              return get((String) plan.get("replacement_id"), owner);
            throw new ServiceException("草稿已被替换，请打开新预览后修改");
          }
          if (!"awaiting_confirmation".equals(plan.get("status"))
              || ((Number) plan.get("expired")).intValue() != 0)
            throw new ServiceException("仅可修改有效期内尚未执行的草稿");
          if (input.getInteger("expectedRevision") == null
              || input.getIntValue("expectedRevision")
                  != ((Number) plan.get("revision")).intValue())
            throw new ServiceException("草稿已变化，请刷新后重试");
          var steps =
              db.queryForList(
                  "select tool_code,input_json from px_operation_step where plan_id=? order by"
                      + " step_number",
                  id);
          if (steps.size() != 1) throw new ServiceException("多步骤计划暂不支持编辑");
          String code = (String) steps.get(0).get("tool_code");
          var editable =
              Map.of(
                  "todo.create",
                  Set.of("content", "planStartTime", "planEndTime", "label", "dueDate"),
                  "note.create",
                  Set.of("title", "content"),
                  "diary.create",
                  Set.of("content", "date", "mood"),
                  "commemoration.create",
                  Set.of("name", "date", "repeat"));
          if (!editable.containsKey(code)) throw new ServiceException("当前类型暂不支持编辑");
          JSONObject patch = input.getJSONObject("fields");
          if (patch == null || patch.isEmpty() || !editable.get(code).containsAll(patch.keySet()))
            throw new ServiceException("包含不支持的编辑字段");
          JSONObject args = JSON.parseObject((String) steps.get(0).get("input_json"));
          args.putAll(patch);
          var step = new JSONObject();
          step.put("toolCode", code);
          step.put("input", args);
          var newSteps = new JSONArray();
          newSteps.add(step);
          var body = new JSONObject();
          body.put("title", plan.get("title"));
          body.put("steps", newSteps);
          JSONObject replacement = preview(body, owner);
          db.update(
              "update px_operation_plan set"
                  + " status='cancelled',revision=revision+1,replacement_id=?,revision_request_json=?"
                  + " where id=?",
              replacement.getString("id"),
              input.toJSONString(),
              id);
          return replacement;
        });
  }

  private void validate(String tool, JSONObject args, boolean lock, String owner) {
    if ("shopping.add".equals(tool) && shoppingCreates != null) {
      shoppingCreates.validate(args, owner, lock);
      return;
    }
    if (localCreates != null && localCreates.supports(tool)) {
      localCreates.validate(tool, args, owner, lock);
      return;
    }
    if (!Set.of("todo.create", "meal.create", "shopping.generate").contains(tool))
      throw new ServiceException("不支持的计划工具");
    if ("todo.create".equals(tool)) {
      required(args, "content");
      if (args.getString("dueDate") != null) LocalDate.parse(required(args, "dueDate"));
      for (String field : List.of("planStartTime", "planEndTime")) {
        String value = args.getString(field);
        if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value)) {
          if (value.length() == 10) LocalDate.parse(value);
          else
            java.time.LocalDateTime.parse(
                value,
                java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
                    .withResolverStyle(java.time.format.ResolverStyle.STRICT));
        }
      }
    }
    if ("meal.create".equals(tool)) {
      required(args, "title");
      LocalDate.parse(required(args, "date"));
      int meal = args.getIntValue("mealType");
      if (meal < 1 || meal > 4) throw new ServiceException("请选择餐次");
      BigDecimal servings = args.getBigDecimal("servings");
      if (servings == null || servings.signum() <= 0) throw new ServiceException("请填写用餐人数");
      Long recipeId = args.getLong("recipeId");
      if (recipeId != null) {
        var recipe =
            lock ? recipes.selectForWorkflow(recipeId) : recipes.selectPxRecipeById(recipeId);
        if (recipe == null
            || "1".equals(recipe.getDelFlag())
            || !Objects.equals(owner, recipe.getCreateBy()))
          throw new ServiceException("菜谱不存在或无权使用");
        var parts =
            lock ? ingredients.selectForWorkflow(recipeId) : ingredients.selectByRecipeId(recipeId);
        parts.sort(Comparator.comparing(PxRecipeIngredient::getId));
        recipe.setIngredients(parts);
        String snapshot = ShoppingWorkflowService.hash(JSON.toJSONString(recipe));
        String expected = args.getString("recipeSnapshot");
        if (expected != null && !expected.equals(snapshot))
          throw new ServiceException("菜谱已变化，请重新预览");
        args.put("recipeSnapshot", snapshot);
      }
    }
    if ("shopping.generate".equals(tool)) {
      if (args.getLong("listId") == null) throw new ServiceException("请选择购物清单");
      LocalDate.parse(required(args, "startDate"));
      LocalDate.parse(required(args, "endDate"));
    }
  }

  private String required(JSONObject args, String key) {
    String v = args.getString(key);
    if (v == null || v.isBlank() || v.length() > 2000) throw new ServiceException("请填写有效的" + key);
    return v;
  }

  public JSONObject get(String id, String owner) {
    var p =
        db.queryForList(
            "select id,title,status,expires_at,revision,create_time,replacement_id from"
                + " px_operation_plan where id=? and owner_id=?",
            id,
            owner);
    if (p.isEmpty()) throw new ServiceException("SOURCE_UNAVAILABLE");
    JSONObject out = new JSONObject(p.get(0));
    out.put(
        "steps",
        db.queryForList(
            "select step_number,tool_code,input_json,status,result_json,error_msg from"
                + " px_operation_step where plan_id=? order by step_number",
            id));
    return out;
  }

  public List<Map<String, Object>> list(String owner) {
    return db.queryForList(
        "select id,title,status,create_time from px_operation_plan where owner_id=? order by"
            + " create_time desc limit 30",
        owner);
  }

  public JSONObject execute(String id, String owner) {
    if (featureGate != null) featureGate.require("operationPlanV2", owner);
    get(id, owner);
    String token = UUID.randomUUID().toString();
    Boolean proceed =
        tx.execute(
            t -> {
              var p =
                  db.queryForList(
                          "select status,expires_at <= now() as expired,lease_until > now() as"
                              + " leased from px_operation_plan where id=? and owner_id=? for"
                              + " update",
                          id,
                          owner)
                      .get(0);
              if (Set.of("succeeded", "compensated", "cancelled").contains(p.get("status")))
                return false;
              if ("running".equals(p.get("status")) && truth(p.get("leased"))) return false;
              if ("awaiting_confirmation".equals(p.get("status")) && truth(p.get("expired")))
                throw new ServiceException("PREVIEW_EXPIRED");
              db.update(
                  "update px_operation_plan set"
                      + " status='running',run_token=?,lease_until=date_add(now(),interval 15"
                      + " minute),revision=revision+1 where id=?",
                  token,
                  id);
              return true;
            });
    if (!Boolean.TRUE.equals(proceed)) return get(id, owner);
    var numbers =
        db.queryForList(
            "select step_number from px_operation_step where plan_id=? order by step_number",
            Integer.class,
            id);
    for (int n : numbers) {
      try {
        tx.execute(
            t -> {
              requireLease(id, token);
              var row =
                  db.queryForList(
                          "select * from px_operation_step where plan_id=? and step_number=? for"
                              + " update",
                          id,
                          n)
                      .get(0);
              if ("succeeded".equals(row.get("status"))) return null;
              var args = JSON.parseObject((String) row.get("input_json"));
              String tool = (String) row.get("tool_code");
              validate(tool, args, true, owner);
              JSONObject result = run(tool, args, id + ":" + n, owner);
              db.update(
                  "update px_operation_step set status='succeeded',result_json=?,error_msg=null"
                      + " where plan_id=? and step_number=?",
                  result.toJSONString(),
                  id,
                  n);
              return null;
            });
      } catch (Exception e) {
        org.slf4j.LoggerFactory.getLogger(OperationPlanService.class)
            .warn("操作计划步骤失败 planId={} step={}", id, n, e);
        String msg = e instanceof ServiceException ? e.getMessage() : "该步骤执行失败，请检查输入后重试";
        tx.execute(
            t -> {
              var rows =
                  db.queryForList(
                      "select run_token from px_operation_plan where id=? for update", id);
              if (!token.equals(rows.get(0).get("run_token"))) return null;
              db.update(
                  "update px_operation_step set status='failed',error_msg=? where plan_id=? and"
                      + " step_number=? and status<>'succeeded'",
                  msg,
                  id,
                  n);
              int count =
                  db.queryForObject(
                      "select count(*) from px_operation_step where plan_id=? and"
                          + " status='succeeded'",
                      Integer.class,
                      id);
              db.update(
                  "update px_operation_plan set status=?,run_token=null,lease_until=null where"
                      + " id=?",
                  count > 0 ? "partially_succeeded" : "failed",
                  id);
              return null;
            });
        return get(id, owner);
      }
    }
    db.update(
        "update px_operation_plan set status='succeeded',run_token=null,lease_until=null where id=?"
            + " and run_token=?",
        id,
        token);
    return get(id, owner);
  }

  private boolean truth(Object value) {
    return Boolean.TRUE.equals(value) || "1".equals(String.valueOf(value));
  }

  private void requireLease(String id, String token) {
    var p =
        db.queryForMap("select run_token,status from px_operation_plan where id=? for update", id);
    if (!token.equals(p.get("run_token")) || !"running".equals(p.get("status")))
      throw new ServiceException("EXECUTION_SUPERSEDED");
    db.update(
        "update px_operation_plan set lease_until=date_add(now(),interval 15 minute) where id=?",
        id);
  }

  public JSONObject cancel(String id, String owner) {
    return tx.execute(
        t -> {
          var rows =
              db.queryForList(
                  "select status from px_operation_plan where id=? and owner_id=? for update",
                  id,
                  owner);
          if (rows.isEmpty()) throw new ServiceException("SOURCE_UNAVAILABLE");
          String status = (String) rows.get(0).get("status");
          if ("cancelled".equals(status)) return get(id, owner);
          if (!Set.of("awaiting_confirmation", "failed").contains(status))
            throw new ServiceException("已执行的步骤需先预览撤销");
          db.update(
              "update px_operation_plan set status='cancelled',revision=revision+1 where id=?", id);
          return get(id, owner);
        });
  }

  private String planTime(String value, boolean end) {
    if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) return null;
    return value.length() == 10 ? value + (end ? " 23:59:59" : " 00:00:00") : value;
  }

  private JSONObject run(String tool, JSONObject args, String key, String owner) {
    if ("shopping.add".equals(tool) && shoppingCreates != null)
      return shoppingCreates.run(args, key, owner);
    if (localCreates != null && localCreates.supports(tool))
      return localCreates.run(tool, args, key, owner);
    JSONObject result = new JSONObject();
    if ("todo.create".equals(tool)) {
      var todo = new PxToDo();
      todo.setContent(args.getString("content"));
      if (args.getString("dueDate") != null)
        todo.setPlanEndTime(args.getString("dueDate") + " 23:59:59");
      else todo.setPlanEndTime(planTime(args.getString("planEndTime"), true));
      todo.setPlanStartTime(planTime(args.getString("planStartTime"), false));
      todo.setLabel(args.getString("label"));
      todo.setKanbanStatus(0);
      todo.setStatus(false);
      todo.setCreateBy(owner);
      todo.setPerformer(owner);
      todo.setClientUuid("plan:" + key);
      todo.setVersion(key);
      if (todos.insertPxToDo(todo) != 1 || todo.getId() == null)
        throw new ServiceException("待办创建失败");
      result.put("sourceType", "todo");
      result.put("sourceId", todo.getId());
      result.put(
          "snapshot",
          ShoppingWorkflowService.hash(JSON.toJSONString(todos.selectPxToDoById(todo.getId()))));
    } else if ("meal.create".equals(tool)) {
      var meal = new PxMealPlan();
      meal.setTitle(args.getString("title"));
      meal.setNotes(args.getString("notes"));
      meal.setPlanDate(java.sql.Date.valueOf(args.getString("date")));
      meal.setMealType(args.getInteger("mealType"));
      meal.setRecipeId(args.getLong("recipeId"));
      meal.setServings(args.getBigDecimal("servings"));
      meal.setClientUuid("plan:" + key);
      meal.setVersion(key);
      if (meals.insertPxMealPlan(meal) != 1 || meal.getId() == null)
        throw new ServiceException("餐饮计划创建失败");
      result.put("sourceType", "meal");
      result.put("sourceId", meal.getId());
      result.put(
          "snapshot",
          ShoppingWorkflowService.hash(
              JSON.toJSONString(meals.selectPxMealPlanById(meal.getId()))));
    } else {
      var preview =
          shopping.preview(
              args.getLong("listId"),
              args.getString("startDate"),
              args.getString("endDate"),
              owner);
      result = shopping.commit(preview.getString("previewId"), owner);
      result.put("sourceType", "shopping");
    }
    return result;
  }

  public JSONObject compensationPreview(String id, String owner) {
    var p = get(id, owner);
    if (!Set.of("succeeded", "partially_succeeded", "failed").contains(p.getString("status")))
      throw new ServiceException("当前执行状态不能撤销");
    var steps =
        db.queryForList(
            "select * from px_operation_step where plan_id=? and status='succeeded' order by"
                + " step_number desc",
            id);
    var affected = new JSONArray();
    for (var step : steps) {
      var result = JSON.parseObject((String) step.get("result_json"));
      String kind = result.getString("sourceType");
      if ("shopping".equals(kind)) throw new ServiceException("采购清单已生成，请在购物清单中核对处理，不能自动撤销");
      Object current =
          localCreates != null && localCreates.supportsKind(kind)
              ? localCreates.current(kind, result.getLong("sourceId"), owner, false)
              : "todo".equals(kind)
                  ? todos.selectPxToDoById(result.getLong("sourceId"))
                  : meals.selectPxMealPlanById(result.getLong("sourceId"));
      if (current == null
          || !ShoppingWorkflowService.hash(JSON.toJSONString(current))
              .equals(result.getString("snapshot"))) throw new ServiceException("目标已发生变化，不能覆盖后续编辑");
      affected.add(result);
    }
    p.put("compensation", affected);
    return p;
  }

  public JSONObject compensate(String id, String owner) {
    return tx.execute(
        t -> {
          db.queryForList(
              "select id from px_operation_plan where id=? and owner_id=? for update", id, owner);
          JSONObject p = compensationPreview(id, owner);
          for (var r : p.getJSONArray("compensation").toJavaList(JSONObject.class)) {
            Long source = r.getLong("sourceId");
            String kind = r.getString("sourceType");
            if (localCreates != null && localCreates.supportsKind(kind)) {
              localCreates.checkReferences(kind, source, owner);
              continue;
            }
            boolean todo = "todo".equals(kind);
            db.queryForList(
                todo
                    ? "select id from px_to_do where id=? for update"
                    : "select id from px_meal_plan where id=? for update",
                source);
            Integer refs =
                db.queryForObject(
                    todo
                        ? "select count(*) from px_to_do where parent_id=?"
                        : "select count(*) from px_shopping_source where meal_plan_id=?",
                    Integer.class,
                    source);
            if (refs != null && refs > 0) throw new ServiceException("对象已有后续关联，不能自动撤销");
          }
          p = compensationPreview(id, owner);
          for (var r : p.getJSONArray("compensation").toJavaList(JSONObject.class)) {
            if (localCreates != null && localCreates.supportsKind(r.getString("sourceType")))
              localCreates.remove(r.getString("sourceType"), r.getLong("sourceId"));
            else if ("todo".equals(r.getString("sourceType")))
              todos.deletePxToDoById(r.getLong("sourceId"));
            else meals.deletePxMealPlanById(r.getLong("sourceId"));
          }
          db.update(
              "update px_operation_step set status='compensated' where plan_id=? and"
                  + " status='succeeded'",
              id);
          db.update("update px_operation_plan set status='compensated' where id=?", id);
          return get(id, owner);
        });
  }
}
