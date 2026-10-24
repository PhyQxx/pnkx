package com.pnkx.web.service.operation;

import com.alibaba.fastjson.*;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.mapper.PxBookkeepingRecordMapper;
import com.pnkx.life.service.impl.SubscriptionSchedule;
import java.math.*;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportActionService {
  private final JdbcTemplate db;
  private final PxBookkeepingRecordMapper records;
  private final OperationPlanService plans;

  public ReportActionService(
      JdbcTemplate db, PxBookkeepingRecordMapper records, OperationPlanService plans) {
    this.db = db;
    this.records = records;
    this.plans = plans;
  }

  public List<Map<String, Object>> list(String owner) {
    var rows =
        db.queryForList(
            "select i.*,p.status execution_status from px_report_insight i left join"
                + " px_operation_plan p on p.id=i.plan_id and p.owner_id=i.owner_id where"
                + " i.owner_id=? order by i.id desc limit 30",
            owner);
    for (var row : rows) {
      row.put("candidates", candidates(row));
      if (row.get("plan_id") == null) continue;
      if ("compensated".equals(row.get("execution_status"))
          || "cancelled".equals(row.get("execution_status"))) {
        row.put("action_status", "cancelled");
        continue;
      }
      var results =
          db.queryForList(
              "select result_json from px_operation_step where plan_id=? and"
                  + " tool_code='todo.create' and status='succeeded' order by step_number limit 1",
              String.class,
              row.get("plan_id"));
      if (results.isEmpty()) continue;
      Long source = JSON.parseObject(results.get(0)).getLong("sourceId");
      var targets =
          db.queryForList(
              "select id,content,status,plan_end_time from px_to_do where id=? and create_by=?",
              source,
              owner);
      if (targets.isEmpty()) {
        row.put("action_status", "cancelled");
        continue;
      }
      var target = targets.get(0);
      boolean done =
          Boolean.TRUE.equals(target.get("status"))
              || "1".equals(String.valueOf(target.get("status")));
      row.put("action_status", done ? "completed" : "accepted");
      row.put("target", target);
    }
    return rows;
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> snapshot(String owner, String period) {
    if (!Set.of("week", "month", "year").contains(period)) throw new ServiceException("无效报告周期");
    LocalDate today = LocalDate.now(SubscriptionSchedule.ZONE);
    LocalDate start =
        switch (period) {
          case "week" -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
          case "year" -> today.withDayOfYear(1);
          default -> today.withDayOfMonth(1);
        };
    LocalDate prior =
        switch (period) {
          case "week" -> start.minusWeeks(1);
          case "year" -> start.minusYears(1);
          default -> start.minusMonths(1);
        };
    long days = ChronoUnit.DAYS.between(start, today) + 1;
    LocalDate previousEnd = prior.plusDays(days);
    if (previousEnd.isAfter(start)) previousEnd = start;
    BigDecimal current = amount(owner, start, today.plusDays(1)),
        previous = amount(owner, prior, previousEnd);
    String fact =
        start
            + " 至 "
            + today
            + "实际支出 "
            + current
            + " 元；对比 "
            + prior
            + " 至 "
            + previousEnd.minusDays(1)
            + "支出 "
            + previous
            + " 元。仅统计已记录支出，不含待确认账期、转账。";
    db.update(
        "insert into"
            + " px_report_insight(owner_id,period,period_start,period_end,comparison_start,comparison_end,actual_amount,comparison_amount,fact_text,candidates_json)"
            + " values(?,?,?,?,?,?,?,?,?,?)",
        owner,
        period,
        start.toString(),
        today.toString(),
        prior.toString(),
        previousEnd.minusDays(1).toString(),
        current,
        previous,
        fact,
        ReportCandidates.build(current, previous, today).toJSONString());
    return db.queryForMap("select * from px_report_insight where id=last_insert_id()");
  }

  private BigDecimal amount(String owner, LocalDate from, LocalDate until) {
    var counts = records.selectLifeReportCounts(owner, from + " 00:00:00", until + " 00:00:00");
    Object value = counts == null ? null : counts.get("totalExpense");
    return value == null
        ? BigDecimal.ZERO
        : new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
  }

  public List<Map<String, Object>> sources(Long id, String owner, int page) {
    if (page < 1 || page > 10000) throw new ServiceException("无效页码");
    var insight = owned(id, owner);
    return db.queryForList(
        "select r.id,r.pay_time,r.money,c.type_name from px_bookkeeping_record r join"
            + " px_bookkeeping_classification c on c.id=r.type where r.create_by=? and r.del_flag=0"
            + " and c.type_difference='1' and r.pay_time>=? and r.pay_time<date_add(?,interval 1"
            + " day) order by r.pay_time,r.id limit 30 offset ?",
        owner,
        insight.get("period_start"),
        insight.get("period_end"),
        (page - 1) * 30);
  }

  @Transactional(rollbackFor = Exception.class)
  public JSONObject preview(Long id, String owner, JSONObject input) {
    var insight = lock(id, owner);
    if ("dismissed".equals(insight.get("status"))) throw new ServiceException("该建议已忽略");
    String chosen = input.getString("candidateId");
    if (chosen != null
        && !chosen.isBlank()
        && candidates(insight).toJavaList(JSONObject.class).stream()
            .noneMatch(c -> chosen.equals(c.getString("id"))))
      throw new ServiceException("候选行动不存在，请刷新报告");
    if (input.getString("content") == null || input.getString("content").isBlank())
      throw new ServiceException("请填写行动内容");
    LocalDate due = LocalDate.parse(input.getString("dueDate"));
    if (due.isBefore(LocalDate.now(SubscriptionSchedule.ZONE)))
      throw new ServiceException("完成日期不能早于今天");
    if (insight.get("plan_id") != null) {
      String oldId = (String) insight.get("plan_id");
      var old =
          db.queryForMap(
              "select status,expires_at<=now() expired from px_operation_plan where id=? and"
                  + " owner_id=? for update",
              oldId,
              owner);
      var oldPlan = plans.get(oldId, owner);
      var oldInput =
          JSON.parseObject(oldPlan.getJSONArray("steps").getJSONObject(0).getString("input_json"));
      boolean same =
          Objects.equals(oldInput.getString("content"), input.getString("content"))
              && Objects.equals(oldInput.getString("dueDate"), input.getString("dueDate"));
      boolean expired =
          Boolean.TRUE.equals(old.get("expired")) || "1".equals(String.valueOf(old.get("expired")));
      if (same && !expired && !"cancelled".equals(old.get("status"))) return oldPlan;
      if (!Set.of("awaiting_confirmation", "cancelled").contains(old.get("status")))
        throw new ServiceException("已有行动正在执行或已执行，请先在操作计划中核对");
      if (!"cancelled".equals(old.get("status"))) plans.cancel(oldId, owner);
    }
    var args = new JSONObject();
    args.put("content", input.getString("content"));
    args.put("dueDate", input.getString("dueDate"));
    var step = new JSONObject();
    step.put("toolCode", "todo.create");
    step.put("input", args);
    var steps = new JSONArray();
    steps.add(step);
    var request = new JSONObject();
    request.put("title", "报告后续行动");
    request.put("steps", steps);
    var plan = plans.preview(request, owner);
    db.update(
        "update px_report_insight set plan_id=?,selected_candidate=? where id=?",
        plan.getString("id"),
        chosen,
        id);
    return plan;
  }

  public JSONObject accept(Long id, String owner) {
    var insight = owned(id, owner);
    if (insight.get("plan_id") == null) throw new ServiceException("请先预览行动");
    var result = plans.execute((String) insight.get("plan_id"), owner);
    if ("succeeded".equals(result.getString("status")))
      db.update(
          "update px_report_insight set status='accepted' where id=? and owner_id=?", id, owner);
    return result;
  }

  @Transactional
  public void dismiss(Long id, String owner) {
    var insight = lock(id, owner);
    if (insight.get("plan_id") != null) throw new ServiceException("已创建行动计划，请在操作计划中处理");
    db.update("update px_report_insight set status='dismissed' where id=?", id);
  }

  private JSONArray candidates(Map<String, Object> insight) {
    Object saved = insight.get("candidates_json");
    if (saved != null) return JSON.parseArray(String.valueOf(saved));
    // Legacy snapshots derive stable drafts from immutable amounts/date, without rewriting facts.
    return ReportCandidates.build(
        new BigDecimal(String.valueOf(insight.get("actual_amount"))),
        new BigDecimal(String.valueOf(insight.get("comparison_amount"))),
        LocalDate.parse(String.valueOf(insight.get("period_end")).substring(0, 10)));
  }

  private Map<String, Object> owned(Long id, String owner) {
    var rows =
        db.queryForList("select * from px_report_insight where id=? and owner_id=?", id, owner);
    if (rows.isEmpty()) throw new ServiceException("SOURCE_UNAVAILABLE");
    return rows.get(0);
  }

  private Map<String, Object> lock(Long id, String owner) {
    var rows =
        db.queryForList(
            "select * from px_report_insight where id=? and owner_id=? for update", id, owner);
    if (rows.isEmpty()) throw new ServiceException("SOURCE_UNAVAILABLE");
    return rows.get(0);
  }
}
