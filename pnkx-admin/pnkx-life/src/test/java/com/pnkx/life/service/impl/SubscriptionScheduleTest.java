package com.pnkx.life.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.pnkx.common.exception.ServiceException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import org.junit.jupiter.api.Test;

class SubscriptionScheduleTest {
  @Test
  void monthEndReturnsToOriginalAnchor() {
    LocalDate feb = SubscriptionSchedule.next(LocalDate.of(2026, 1, 31), "monthly", 1, 31);
    assertEquals(LocalDate.of(2026, 2, 28), feb);
    assertEquals(LocalDate.of(2026, 3, 31), SubscriptionSchedule.next(feb, "monthly", 1, 31));
  }

  @Test
  void leapDayReturnsInLeapYear() {
    LocalDate date = LocalDate.of(2024, 2, 29);
    for (int i = 0; i < 4; i++) date = SubscriptionSchedule.next(date, "yearly", 1, 29);
    assertEquals(LocalDate.of(2028, 2, 29), date);
  }

  @Test
  void handlesSqlDatesAndShanghaiMidnight() {
    assertEquals(
        LocalDate.of(2026, 10, 23), SubscriptionSchedule.date(java.sql.Date.valueOf("2026-10-23")));
    assertEquals(
        LocalDate.of(2026, 10, 23),
        SubscriptionSchedule.date(Date.from(Instant.parse("2026-10-22T16:00:00Z"))));
  }

  @Test
  void intervalsAndInvalidConfiguration() {
    LocalDate date = LocalDate.of(2026, 12, 31);
    assertEquals(LocalDate.of(2027, 1, 14), SubscriptionSchedule.next(date, "weekly", 2, 31));
    assertThrows(ServiceException.class, () -> SubscriptionSchedule.next(date, "monthly", 0, 31));
    assertThrows(ServiceException.class, () -> SubscriptionSchedule.next(date, "unknown", 1, 31));
  }
}
