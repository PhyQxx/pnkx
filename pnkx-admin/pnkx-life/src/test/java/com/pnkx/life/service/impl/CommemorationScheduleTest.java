package com.pnkx.life.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CommemorationScheduleTest {
  @Test
  void annualOccurrenceCrossesYear() {
    assertEquals(
        LocalDate.of(2027, 1, 2),
        CommemorationSchedule.next(LocalDate.of(2000, 1, 2), true, LocalDate.of(2026, 12, 25)));
  }

  @Test
  void leapDayClampsAndReturnsToLeapAnchor() {
    var original = LocalDate.of(2024, 2, 29);
    assertEquals(
        LocalDate.of(2025, 2, 28),
        CommemorationSchedule.next(original, true, LocalDate.of(2025, 2, 1)));
    assertEquals(
        LocalDate.of(2028, 2, 29),
        CommemorationSchedule.next(original, true, LocalDate.of(2028, 2, 1)));
  }

  @Test
  void todayIsIncludedAndExpiredOneOffIsNot() {
    var today = LocalDate.of(2026, 10, 24);
    assertEquals(today, CommemorationSchedule.next(today, false, today));
    assertNull(CommemorationSchedule.next(today.minusDays(1), false, today));
  }

  @Test
  void futureOriginalDateDoesNotCreateEarlierAnniversary() {
    var future = LocalDate.of(2028, 1, 1);
    assertEquals(future, CommemorationSchedule.next(future, true, LocalDate.of(2026, 1, 1)));
  }
}
