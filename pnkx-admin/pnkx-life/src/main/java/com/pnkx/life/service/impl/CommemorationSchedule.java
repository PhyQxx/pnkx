package com.pnkx.life.service.impl;

import java.time.*;

public final class CommemorationSchedule {
  private CommemorationSchedule() {}

  /** Gregorian anniversary: Feb 29 falls on Feb 28 in non-leap years. */
  public static LocalDate next(LocalDate original, boolean repeat, LocalDate today) {
    if (!repeat) return original.isBefore(today) ? null : original;
    if (original.isAfter(today)) return original;
    LocalDate next = MonthDay.from(original).atYear(today.getYear());
    if (next.isBefore(today)) next = MonthDay.from(original).atYear(today.getYear() + 1);
    return next;
  }
}
