package com.pnkx.web.service.operation;

import com.alibaba.fastjson.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Explicit rule-based drafts, never claims about causes or unobserved spending. */
public final class ReportCandidates {
  private ReportCandidates() {}

  public static JSONArray build(BigDecimal current, BigDecimal previous, LocalDate date) {
    var result = new JSONArray();
    String context =
        current.compareTo(previous) > 0
            ? "本期已记录支出高于对比期，尚不能据此判断原因。"
            : current.signum() == 0 ? "本期暂无已记录支出，不代表没有实际消费。" : "本期与对比期的已记录支出可用于复盘，不能据此判断消费是否合理。";
    result.add(candidate("verify", "核对记录", "核对本期支出记录，检查漏记、重复和分类", date.plusDays(3), context));
    result.add(
        candidate(
            "review",
            "复盘主要支出",
            "查看本期主要支出，写下需要保留或调整的项目",
            date.plusDays(7),
            "基于快照对应的实际支出明细，由你判断哪些项目值得调整。"));
    result.add(
        candidate(
            "plan",
            "安排下期检查",
            "安排一次下期支出检查，核对实际支出与个人计划",
            date.plusDays(14),
            "这是可选的检查任务，不会自动修改预算、取消订阅或生成消费。"));
    return result;
  }

  private static JSONObject candidate(
      String id, String title, String content, LocalDate date, String basis) {
    var c = new JSONObject();
    c.put("id", id);
    c.put("title", title);
    c.put("content", content);
    c.put("dueDate", date.toString());
    c.put("basis", basis);
    c.put("origin", "rule_based");
    return c;
  }
}
