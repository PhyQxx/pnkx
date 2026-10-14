package com.pnkx.life.service.recurrence;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
class RecurrenceRuleTest {
 private TodoSeries rule(String type){TodoSeries r=new TodoSeries();r.setTitle("test");r.setStartDate("2026-01-01");r.setRuleType(type);return r;}
 @Test void monthEndClampsWithoutDriftingTheNextMonth(){var r=rule("monthly");r.setMonthDay(31);assertEquals(LocalDate.of(2026,2,28),RecurrenceRule.onOrAfter(r,LocalDate.of(2026,2,1)));assertEquals(LocalDate.of(2026,3,31),RecurrenceRule.onOrAfter(r,LocalDate.of(2026,3,1)));}
 @Test void leapYearAndWeeklySelection(){var r=rule("monthly");r.setMonthDay(31);assertEquals(LocalDate.of(2028,2,29),RecurrenceRule.onOrAfter(r,LocalDate.of(2028,2,1)));r.setRuleType("weekly");r.setWeekDays("1,5");assertEquals(LocalDate.of(2026,10,12),RecurrenceRule.onOrAfter(r,LocalDate.of(2026,10,10)));}
 @Test void dstGapAndOverlapResolveToOneOccurrence(){var r=rule("daily");r.setZoneId("America/New_York");r.setLocalTime("02:30");assertEquals(LocalTime.of(3,30),RecurrenceRule.at(r,LocalDate.of(2026,3,8)).toLocalTime());r.setLocalTime("01:30");assertEquals(ZoneOffset.ofHours(-4),RecurrenceRule.at(r,LocalDate.of(2026,11,1)).getOffset());}
 @Test void dailyIntervalAnchorsToStart(){var r=rule("daily");r.setIntervalDays(3);assertEquals(LocalDate.of(2026,1,7),RecurrenceRule.onOrAfter(r,LocalDate.of(2026,1,5)));}
}
