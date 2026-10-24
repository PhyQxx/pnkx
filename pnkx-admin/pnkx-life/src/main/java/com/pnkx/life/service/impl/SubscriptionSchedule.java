package com.pnkx.life.service.impl;

import com.pnkx.common.exception.ServiceException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Set;

/** 业务日期与服务器时区隔离，保留月末锚点。 */
public final class SubscriptionSchedule {
  public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

  private SubscriptionSchedule() {}

  public static LocalDate date(Date value) {
    if (value == null) throw new ServiceException("扣费日期不能为空");
    if (value instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
    return value.toInstant().atZone(ZONE).toLocalDate();
  }

  public static void validateCycle(String cycle) {
    if (cycle != null && !Set.of("daily", "weekly", "monthly", "yearly").contains(cycle))
      throw new ServiceException("不支持的订阅周期");
  }

  public static LocalDate next(LocalDate due, String cycle, Integer interval, Integer anchorDay) {
    validateCycle(cycle);
    int n = interval == null ? 1 : interval;
    int anchor = anchorDay == null ? due.getDayOfMonth() : anchorDay;
    if (n < 1 || anchor < 1 || anchor > 31) throw new ServiceException("周期或日期锚点无效");
    return switch (cycle == null ? "monthly" : cycle) {
      case "daily" -> due.plusDays(n);
      case "weekly" -> due.plusWeeks(n);
      case "yearly" -> atAnchor(due.plusYears(n), anchor);
      default -> atAnchor(due.plusMonths(n), anchor);
    };
  }

  private static LocalDate atAnchor(LocalDate date, int anchor) {
    return date.withDayOfMonth(Math.min(anchor, date.lengthOfMonth()));
  }
}
