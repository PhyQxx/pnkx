package com.pnkx.life.service.impl;

import com.alibaba.fastjson.*;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.domain.po.*;
import com.pnkx.life.mapper.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 规则变更以持久化预览提交；已发生实例保留独立快照。 */
@Service
public class ChargeRuleChangeService {
  @jakarta.annotation.Resource private ChargeInstanceService instances;
  private final JdbcTemplate db;
  private final PxSubscriptionMapper subscriptions;
  private final PxBookkeepingRecurringMapper recurring;
  private final PxSubscriptionServiceImpl subscriptionService;
  private final PxBookkeepingRecurringService recurringService;

  public ChargeRuleChangeService(
      JdbcTemplate db,
      PxSubscriptionMapper subscriptions,
      PxBookkeepingRecurringMapper recurring,
      PxSubscriptionServiceImpl subscriptionService,
      PxBookkeepingRecurringService recurringService) {
    this.db = db;
    this.subscriptions = subscriptions;
    this.recurring = recurring;
    this.subscriptionService = subscriptionService;
    this.recurringService = recurringService;
  }

  private JSONObject source(String type, Long id, String owner, boolean lock) {
    Object value;
    if ("subscription".equals(type)) {
      var s = lock ? subscriptions.selectForCharge(id) : subscriptions.selectPxSubscriptionById(id);
      if (s == null || !owner.equals(s.getCreateBy()) || "1".equals(s.getDelFlag()))
        throw new ServiceException("SOURCE_UNAVAILABLE");
      value = s;
    } else if ("recurring".equals(type)) {
      value = lock ? recurring.lockForCharge(id, owner) : recurring.selectById(id, owner);
      if (value == null) throw new ServiceException("SOURCE_UNAVAILABLE");
    } else throw new ServiceException("INVALID_ACTION");
    // 查询联表名称不参与规则版本，避免锁定查询与普通查询的字段差异。
    var result = JSON.parseObject(JSON.toJSONString(value));
    result.remove("accountName");
    result.remove("typeName");
    result.remove("params");
    return result;
  }

  public JSONObject preview(JSONObject input, String owner) {
    String type = input.getString("sourceType");
    Long id = input.getLong("sourceId");
    JSONObject old = source(type, id, owner, false), changes = input.getJSONObject("changes");
    if (changes == null) throw new ServiceException("缺少变更内容");
    var allowed =
        "subscription".equals(type)
            ? Set.of(
                "name",
                "amount",
                "cycle",
                "cycleInterval",
                "nextPaymentDate",
                "accountId",
                "classificationId",
                "paymentMethod",
                "reminderLeadDays",
                "enabled",
                "lifecycle",
                "trialEndDate",
                "postingMode",
                "remark")
            : Set.of(
                "name",
                "money",
                "frequency",
                "dayNumber",
                "account",
                "type",
                "otherAccount",
                "typeDifference",
                "postingMode",
                "remark",
                "enabled");
    var merged = JSON.parseObject(old.toJSONString());
    var differences = new JSONArray();
    for (String key : allowed) {
      if (!changes.containsKey(key)) continue;
      Object before = old.get(key), after = changes.get(key);
      if (Objects.equals(JSON.toJSONString(before), JSON.toJSONString(after))) continue;
      var diff = new JSONObject(true);
      diff.put("field", key);
      diff.put("before", before);
      diff.put("after", after);
      differences.add(diff);
      merged.put(key, after);
    }
    if ("subscription".equals(type)
        && changes.containsKey("trialEndDate")
        && changes.get("trialEndDate") == null) {
      var params = new JSONObject();
      params.put("clearTrialEndDate", true);
      merged.put("params", params);
    }
    merged.put("id", id);
    var payload = new JSONObject(true);
    payload.put("sourceType", type);
    payload.put("sourceId", id);
    payload.put("rule", merged);
    String preview = UUID.randomUUID().toString();
    db.update(
        "insert into px_operation_preview(id,owner_id,kind,payload,source_hash,expires_at)"
            + " values(?,?,'charge_rule',?,?,date_add(now(),interval 15 minute))",
        preview,
        owner,
        payload.toJSONString(),
        ShoppingWorkflowService.hash(old.toJSONString()));
    var result = new JSONObject();
    result.put("previewId", preview);
    result.put("changes", differences);
    Object amount = merged.get("subscription".equals(type) ? "amount" : "money");
    var duplicates =
        db.queryForList(
            "select name from px_subscription where create_by=? and enabled=1 and del_flag='0' and"
                + " amount=? and not(?='subscription' and id=?) union all select name from"
                + " px_bookkeeping_recurring where create_by=? and enabled=1 and del_flag=0 and"
                + " money=? and not(?='recurring' and id=?) limit 10",
            owner,
            amount,
            type,
            id,
            owner,
            amount,
            type,
            id);
    result.put("duplicates", duplicates);
    result.put("notice", "仅修改后续规则；待确认与已入账账期保留原金额。终止追踪不会取消服务平台续费。");
    return result;
  }

  public JSONObject backfillPreview(JSONObject input, String owner) {
    String type = input.getString("sourceType");
    Long sourceId = input.getLong("sourceId");
    var rule = source(type, sourceId, owner, false);
    if (rule.getBooleanValue("enabled") || "cancelled".equals(rule.getString("lifecycle")))
      throw new ServiceException("请仅为暂停中的规则补齐账期");
    boolean subscription = "subscription".equals(type);
    var due =
        SubscriptionSchedule.date(rule.getDate(subscription ? "nextPaymentDate" : "nextRunDate"));
    var today = java.time.LocalDate.now(SubscriptionSchedule.ZONE);
    var periods = new JSONArray();
    int scanned = 0;
    while (!due.isAfter(today) && scanned++ < 100) {
      if (!subscription
          || rule.getDate("trialEndDate") == null
          || !due.isBefore(SubscriptionSchedule.date(rule.getDate("trialEndDate"))))
        periods.add(due.toString());
      due =
          subscription
              ? SubscriptionSchedule.next(
                  due,
                  rule.getString("cycle"),
                  rule.getInteger("cycleInterval"),
                  rule.getInteger("anchorDay"))
              : PxBookkeepingRecurringService.computeNextRunDate(
                  rule.toJavaObject(PxBookkeepingRecurring.class), due);
    }
    if (periods.isEmpty()) throw new ServiceException("该规则暂无可补齐的已到期付费账期");
    var payload = new JSONObject(true);
    payload.put("sourceType", type);
    payload.put("sourceId", sourceId);
    payload.put("periods", periods);
    payload.put("nextDate", due.toString());
    payload.put("rule", rule);
    String id = UUID.randomUUID().toString();
    db.update(
        "insert into px_operation_preview(id,owner_id,kind,payload,source_hash,expires_at)"
            + " values(?,?,'charge_backfill',?,?,date_add(now(),interval 15 minute))",
        id,
        owner,
        payload.toJSONString(),
        ShoppingWorkflowService.hash(rule.toJSONString()));
    var result = new JSONObject();
    result.put("previewId", id);
    result.put("periods", periods);
    result.put("amountPerPeriod", rule.get(subscription ? "amount" : "money"));
    result.put("notice", "仅补齐待确认账期，不直接增加支出；规则保持暂停。每批最多100期，剩余可再次预览补齐。");
    return result;
  }

  private int backfill(JSONObject input, String owner) {
    String type = input.getString("sourceType");
    boolean sub = "subscription".equals(type);
    Long id = input.getLong("sourceId");
    var rule = input.getJSONObject("rule");
    for (String period : input.getJSONArray("periods").toJavaList(String.class))
      instances.generate(
          owner,
          type,
          id,
          rule.getString("name"),
          java.time.LocalDate.parse(period),
          rule.getBigDecimal(sub ? "amount" : "money"),
          rule.getLong(sub ? "accountId" : "account"),
          sub
              ? rule.getLong("classificationId")
              : ("2".equals(rule.getString("typeDifference"))
                  ? Long.valueOf(0L)
                  : rule.getLong("type")),
          sub ? null : rule.getLong("otherAccount"),
          "manual_confirm",
          sub ? "sub_" + id + "_" + period : null);
    if (sub) {
      var update = new PxSubscription();
      update.setId(id);
      update.setNextPaymentDate(java.sql.Date.valueOf(input.getString("nextDate")));
      return subscriptions.updatePxSubscription(update);
    }
    return recurring.advanceRunDate(
        id,
        java.sql.Date.valueOf(input.getString("nextDate")),
        java.sql.Date.valueOf(
            input.getJSONArray("periods").getString(input.getJSONArray("periods").size() - 1)));
  }

  @Transactional(rollbackFor = Exception.class)
  public JSONObject commit(String id, String owner) {
    var rows =
        db.queryForList(
            "select *,expires_at<=now() expired from px_operation_preview where id=? and owner_id=?"
                + " and kind in ('charge_rule','charge_backfill') for update",
            id,
            owner);
    if (rows.isEmpty()) throw new ServiceException("SOURCE_UNAVAILABLE");
    var preview = rows.get(0);
    if ("completed".equals(preview.get("status")))
      return JSON.parseObject((String) preview.get("result_json"));
    if (Boolean.TRUE.equals(preview.get("expired"))
        || "1".equals(String.valueOf(preview.get("expired"))))
      throw new ServiceException("PREVIEW_EXPIRED");
    var input = JSON.parseObject((String) preview.get("payload"));
    String type = input.getString("sourceType");
    Long sourceId = input.getLong("sourceId");
    var current = source(type, sourceId, owner, true);
    if (!ShoppingWorkflowService.hash(current.toJSONString()).equals(preview.get("source_hash")))
      throw new ServiceException("REVISION_CONFLICT");
    int count =
        "charge_backfill".equals(preview.get("kind"))
            ? backfill(input, owner)
            : "subscription".equals(type)
                ? subscriptionService.updatePxSubscription(
                    input.getJSONObject("rule").toJavaObject(PxSubscription.class))
                : recurringService.update(
                    input.getJSONObject("rule").toJavaObject(PxBookkeepingRecurring.class));
    if (count != 1) throw new ServiceException("REVISION_CONFLICT");
    var result = new JSONObject();
    result.put("sourceId", sourceId);
    result.put("sourceType", type);
    db.update(
        "update px_operation_preview set status='completed',result_json=? where id=?",
        result.toJSONString(),
        id);
    return result;
  }
}
