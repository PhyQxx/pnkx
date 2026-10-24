package com.pnkx.life.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.framework.web.service.DataPermissionService;
import com.pnkx.life.domain.po.*;
import com.pnkx.life.mapper.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShoppingWorkflowService {
  @jakarta.annotation.Resource private LifeFeatureGate featureGate;
  private final JdbcTemplate db;
  private final PxShoppingListMapper lists;
  private final PxShoppingItemMapper items;
  private final PxMealPlanMapper meals;
  private final PxRecipeMapper recipes;
  private final PxRecipeIngredientMapper ingredients;
  private final DataPermissionService permissions;

  public ShoppingWorkflowService(
      JdbcTemplate db,
      PxShoppingListMapper lists,
      PxShoppingItemMapper items,
      PxMealPlanMapper meals,
      PxRecipeMapper recipes,
      PxRecipeIngredientMapper ingredients,
      DataPermissionService permissions) {
    this.db = db;
    this.lists = lists;
    this.items = items;
    this.meals = meals;
    this.recipes = recipes;
    this.ingredients = ingredients;
    this.permissions = permissions;
  }

  private PxShoppingList target(Long id, String user, boolean shared) {
    var l = lists.selectPxShoppingListById(id);
    if (l == null
        || "1".equals(l.getDelFlag())
        || !(user.equals(l.getCreateBy())
            || (shared && permissions.canWrite(l.getCreateBy(), "shopping"))))
      throw new ServiceException("清单不存在或无权操作");
    return l;
  }

  public JSONObject preview(Long listId, String start, String end, String user) {
    return preview(listId, start, end, user, true);
  }

  private JSONObject preview(Long listId, String start, String end, String user, boolean shared) {
    if (featureGate != null) featureGate.require("shoppingPlanV2", user);
    target(listId, user, shared);
    JSONObject data = build(listId, start, end, user, shared, false);
    String id = UUID.randomUUID().toString();
    db.update(
        "insert into px_operation_preview(id,owner_id,kind,payload,source_hash,expires_at)"
            + " values(?,?,'shopping',?,?,date_add(now(),interval 15 minute))",
        id,
        user,
        data.toJSONString(),
        hash(data.toJSONString()));
    data.put("previewId", id);
    return data;
  }

  private JSONObject build(
      Long listId, String start, String end, String user, boolean shared, boolean lock) {
    LocalDate from = LocalDate.parse(start), to = LocalDate.parse(end);
    if (to.isBefore(from) || to.isAfter(from.plusDays(31)))
      throw new ServiceException("采购计划范围最多32天");
    List<Long> visible = shared ? permissions.getVisibleUserIds("meal") : null;
    if (visible == null || visible.isEmpty()) visible = List.of(Long.valueOf(user));
    List<PxMealPlan> plans = new ArrayList<>();
    for (Long uid : visible)
      plans.addAll(
          (lock
              ? meals.selectByDateRangeForWorkflow(start, end, uid.toString())
              : meals.selectByDateRangeForUser(start, end, uid.toString())));
    plans.sort(Comparator.comparing(PxMealPlan::getId));
    JSONArray sources = new JSONArray();
    for (var meal : plans) {
      if (meal.getRecipeId() == null) continue;
      var recipe =
          lock
              ? recipes.selectForWorkflow(meal.getRecipeId())
              : recipes.selectPxRecipeById(meal.getRecipeId());
      if (recipe == null || "1".equals(recipe.getDelFlag())) continue;
      if (!Objects.equals(recipe.getCreateBy(), meal.getCreateBy())
          && !Objects.equals(recipe.getCreateBy(), user))
        throw new ServiceException("计划关联菜谱无权使用，请重新选择菜谱");
      BigDecimal base =
          recipe.getServings() == null
              ? BigDecimal.ONE
              : new BigDecimal(recipe.getServings().toString());
      BigDecimal count = meal.getServings() == null ? BigDecimal.ONE : meal.getServings();
      if (base.signum() <= 0 || count.signum() <= 0) throw new ServiceException("用餐份数必须大于0");
      var parts =
          lock
              ? ingredients.selectForWorkflow(recipe.getId())
              : ingredients.selectByRecipeId(recipe.getId());
      parts.sort(Comparator.comparing(PxRecipeIngredient::getId));
      for (var ingredient : parts) {
        var quantity =
            IngredientQuantity.parse(
                ingredient.getQuantity(), count.divide(base, 6, RoundingMode.HALF_UP));
        String name =
            Normalizer.normalize(ingredient.getName(), Normalizer.Form.NFKC)
                .trim()
                .replaceAll("\\s+", " ");
        String key =
            hash(
                name
                    + "|"
                    + quantity.unit()
                    + (quantity.quantity() == null
                        ? "|" + meal.getId() + ":" + ingredient.getId()
                        : ""));
        JSONObject source = new JSONObject(true);
        source.put("mealId", meal.getId());
        source.put("ingredientId", ingredient.getId());
        source.put("date", SubscriptionSchedule.date(meal.getPlanDate()).toString());
        source.put("key", key);
        source.put("name", name);
        source.put("quantity", quantity.quantity());
        source.put("unit", quantity.unit());
        source.put("raw", quantity.raw());
        source.put("classificationId", ingredient.getClassificationId());
        sources.add(source);
      }
    }
    // 预览包含已有来源、行版本，提交时重新计算以检测计划或购物变更。
    var old =
        db.queryForList(
            "select s.*,i.name,i.revision,i.fulfillment_state,i.del_flag,i.quantity"
                + " item_quantity,m.create_by source_owner from px_shopping_source s join"
                + " px_shopping_item i on i.id=s.item_id left join px_meal_plan m on"
                + " m.id=s.meal_plan_id where s.list_id=? order by s.id"
                + (lock ? " for update" : ""),
            listId);
    JSONObject result = new JSONObject(true);
    result.put("listId", listId);
    result.put("startDate", start);
    result.put("endDate", end);
    result.put("shared", shared);
    result.put("visibleOwners", visible);
    result.put("sources", sources);
    result.put("existing", old);
    TreeMap<String, JSONObject> groups = new TreeMap<>();
    for (var source : sources.toJavaList(JSONObject.class)) {
      var group =
          groups.computeIfAbsent(
              source.getString("key"),
              k -> {
                var g = new JSONObject(true);
                g.put("key", k);
                g.put("name", source.getString("name"));
                g.put("unit", source.getString("unit"));
                g.put("quantity", BigDecimal.ZERO);
                g.put("raw", source.getString("raw"));
                return g;
              });
      if (source.getBigDecimal("quantity") != null)
        group.put(
            "quantity", group.getBigDecimal("quantity").add(source.getBigDecimal("quantity")));
      else group.put("needsReview", true);
    }
    var warnings = new JSONArray();
    final var visibleOwnerStrings =
        visible.stream().map(String::valueOf).collect(java.util.stream.Collectors.toSet());
    for (var row : old) {
      String key = String.valueOf(row.get("group_key"));
      var group =
          groups.computeIfAbsent(
              key,
              k -> {
                var g = new JSONObject(true);
                g.put("key", k);
                g.put("name", row.get("name"));
                g.put("unit", row.get("unit"));
                g.put("quantity", BigDecimal.ZERO);
                g.put("raw", row.get("raw_quantity"));
                return g;
              });
      group.put("beforeQuantity", row.get("item_quantity"));
      String date = String.valueOf(row.get("plan_date"));
      boolean replaced =
          date.compareTo(start) >= 0
              && date.compareTo(end) <= 0
              && visibleOwnerStrings.contains(String.valueOf(row.get("source_owner")));
      if (!replaced) {
        if (row.get("quantity") != null)
          group.put(
              "quantity",
              group.getBigDecimal("quantity").add(new BigDecimal(row.get("quantity").toString())));
        else group.put("needsReview", true);
      }
      if (!"needed".equals(row.get("fulfillment_state"))) group.put("preserveProcessed", true);
    }
    for (var group : groups.values()) {
      String after =
          group.getBooleanValue("needsReview")
              ? group.getString("raw")
              : group.getBigDecimal("quantity").stripTrailingZeros().toPlainString()
                  + group.getString("unit");
      String before = group.getString("beforeQuantity");
      group.put(
          "change",
          before == null
              ? "新增"
              : after.equals(before)
                  ? "不变"
                  : group.getBigDecimal("quantity").signum() == 0
                          && !group.getBooleanValue("needsReview")
                      ? "移除来源"
                      : "调整");
      if (group.getBooleanValue("preserveProcessed")) {
        group.put("change", "保留已处理数量");
        warnings.add(group.getString("name") + "：需求为" + after + "，已处理数量保留" + before);
      }
    }
    result.put("warnings", warnings);
    result.put("groups", new ArrayList<>(groups.values()));
    return result;
  }

  @Transactional(rollbackFor = Exception.class)
  public JSONObject commit(String previewId, String user) {
    var rows =
        db.queryForList(
            "select *,expires_at <= now() as expired from px_operation_preview where id=? and"
                + " owner_id=? and kind='shopping' for update",
            previewId,
            user);
    if (rows.isEmpty()) throw new ServiceException("SOURCE_UNAVAILABLE");
    var preview = rows.get(0);
    if ("completed".equals(preview.get("status")))
      return JSON.parseObject((String) preview.get("result_json"));
    if (("1".equals(String.valueOf(preview.get("expired")))
        || Boolean.TRUE.equals(preview.get("expired"))))
      throw new ServiceException("PREVIEW_EXPIRED");
    JSONObject input = JSON.parseObject((String) preview.get("payload"));
    Long listId = input.getLong("listId");
    boolean shared = input.getBooleanValue("shared");
    var list = target(listId, user, shared);
    db.queryForList("select id from px_shopping_list where id=? for update", listId);
    // 锁定来源及已有购物行，保证重新校验与写入之间不会被并发编辑。
    var lockOwners = input.getJSONArray("visibleOwners");
    var lockArgs = new ArrayList<Object>();
    lockArgs.add(input.getString("startDate"));
    lockArgs.add(input.getString("endDate"));
    lockArgs.addAll(lockOwners);
    var lockedMeals =
        db.queryForList(
            "select id,recipe_id from px_meal_plan where plan_date between ? and ? and create_by in"
                + " ("
                + String.join(",", Collections.nCopies(lockOwners.size(), "?"))
                + ") order by id for update",
            lockArgs.toArray());
    var recipeIds = new TreeSet<Long>();
    for (var row : lockedMeals)
      if (row.get("recipe_id") instanceof Number number) recipeIds.add(number.longValue());
    for (Long recipeId : recipeIds) {
      db.queryForList("select id from px_recipe where id=? for update", recipeId);
      db.queryForList(
          "select id from px_recipe_ingredient where recipe_id=? order by id for update", recipeId);
    }
    db.queryForList(
        "select id from px_shopping_item where list_id=? order by id for update", listId);
    JSONObject current =
        build(listId, input.getString("startDate"), input.getString("endDate"), user, shared, true);
    if (!hash(current.toJSONString()).equals(preview.get("source_hash")))
      throw new ServiceException("REVISION_CONFLICT");
    var oldGroups =
        db.queryForList(
            "select group_key,min(item_id) item_id from px_shopping_source where list_id=? group by"
                + " group_key",
            listId);
    Map<String, Long> groupIds = new HashMap<>();
    for (var g : oldGroups)
      groupIds.put((String) g.get("group_key"), ((Number) g.get("item_id")).longValue());
    var owners = input.getJSONArray("visibleOwners");
    var deleteArgs = new ArrayList<Object>();
    deleteArgs.add(listId);
    deleteArgs.add(input.getString("startDate"));
    deleteArgs.add(input.getString("endDate"));
    deleteArgs.addAll(owners);
    db.update(
        "delete s from px_shopping_source s join px_meal_plan m on m.id=s.meal_plan_id where"
            + " s.list_id=? and s.plan_date between ? and ? and m.create_by in ("
            + String.join(",", Collections.nCopies(owners.size(), "?"))
            + ")",
        deleteArgs.toArray());
    int created = 0;
    JSONArray warnings = new JSONArray();
    for (var source : input.getJSONArray("sources").toJavaList(JSONObject.class)) {
      String key = source.getString("key");
      Long itemId = groupIds.get(key);
      if (itemId == null) {
        var item = new PxShoppingItem();
        item.setListId(listId);
        item.setName(source.getString("name"));
        item.setQuantity(source.getString("raw"));
        item.setClassificationId(source.getLong("classificationId"));
        item.setChecked(false);
        item.setAddedFromMeal(true);
        item.setCreateBy(list.getCreateBy());
        item.setCreateTime(DateUtils.getNowDate());
        item.setClientUuid(UUID.randomUUID().toString());
        if (items.insertPxShoppingItem(item) != 1 || item.getId() == null)
          throw new ServiceException("购物条目写入失败");
        itemId = item.getId();
        groupIds.put(key, itemId);
        created++;
      }
      db.update(
          "insert into"
              + " px_shopping_source(list_id,item_id,meal_plan_id,ingredient_id,plan_date,group_key,quantity,unit,raw_quantity)"
              + " values(?,?,?,?,?,?,?,?,?)",
          listId,
          itemId,
          source.getLong("mealId"),
          source.getLong("ingredientId"),
          source.getString("date"),
          key,
          source.getBigDecimal("quantity"),
          source.getString("unit"),
          source.getString("raw"));
    }
    for (var entry : groupIds.entrySet()) {
      var totals =
          db.queryForList(
                  "select sum(quantity) qty,max(unit) unit,max(raw_quantity) raw,count(*) n from"
                      + " px_shopping_source where list_id=? and group_key=?",
                  listId,
                  entry.getKey())
              .get(0);
      var item = items.selectPxShoppingItemById(entry.getValue());
      if (item == null) continue;
      if (Boolean.TRUE.equals(item.getChecked()) || item.getBatchId() != null) {
        warnings.add(item.getName() + "：保留已购数量，请核对需求变化");
        continue;
      }
      if (((Number) totals.get("n")).intValue() == 0) {
        db.update(
            "update px_shopping_item set del_flag='1',revision=revision+1 where id=?",
            item.getId());
        continue;
      }
      String quantity =
          totals.get("qty") == null
              ? String.valueOf(totals.get("raw"))
              : ((BigDecimal) totals.get("qty")).stripTrailingZeros().toPlainString()
                  + totals.get("unit");
      db.update(
          "update px_shopping_item set"
              + " quantity=?,revision=revision+1,del_flag='0',update_time=now() where id=?",
          quantity,
          item.getId());
    }
    JSONObject result = new JSONObject(true);
    result.put("created", created);
    result.put("warnings", warnings);
    result.put("listId", listId);
    db.update(
        "update px_operation_preview set status='completed',result_json=? where id=?",
        result.toJSONString(),
        previewId);
    return result;
  }

  @Transactional(rollbackFor = Exception.class)
  public JSONObject automatic(Long listId, String start, String end, String owner) {
    return commit(preview(listId, start, end, owner, false).getString("previewId"), owner);
  }

  public static String hash(String text) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
