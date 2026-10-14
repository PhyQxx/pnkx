package com.pnkx.life.service.recurrence;
import com.pnkx.common.exception.ServiceException;
import java.time.*;
import java.util.*;
public final class RecurrenceRule {
 private RecurrenceRule() {}
 public static void validate(TodoSeries rule) {
  try {
   if(rule.getTitle()==null || rule.getTitle().isBlank() || rule.getTitle().length()>200) throw new IllegalArgumentException();
   if(!Set.of("daily","weekly","monthly","completion").contains(rule.getRuleType())) throw new IllegalArgumentException();
   if(rule.getIntervalDays()==null || rule.getIntervalDays()<1 || rule.getIntervalDays()>365) throw new IllegalArgumentException();
   if(rule.getPaused()==null)throw new IllegalArgumentException();
   ZoneId.of(rule.getZoneId()); LocalTime.parse(rule.getLocalTime());
   LocalDate start=LocalDate.parse(rule.getStartDate());
   if(rule.getEndDate()!=null && LocalDate.parse(rule.getEndDate()).isBefore(start)) throw new IllegalArgumentException();
   if(rule.getMonthDay()==null || rule.getMonthDay()<1 || rule.getMonthDay()>31) throw new IllegalArgumentException();
   if(rule.getLeadMinutes()!=null && (rule.getLeadMinutes()<0 || rule.getLeadMinutes()>10080)) throw new IllegalArgumentException();
   if(rule.getRuleType().equals("weekly")) days(rule.getWeekDays());
  } catch(RuntimeException ex) { throw new ServiceException("重复规则不正确，请检查日期、时区、星期和间隔"); }
 }
 private static Set<Integer> days(String csv) {
  Set<Integer> result=new HashSet<>();for(String value:csv.split(",")) { int day=Integer.parseInt(value);if(day<1||day>7)throw new IllegalArgumentException();result.add(day); }return result;
 }
 public static LocalDate onOrAfter(TodoSeries rule,LocalDate date) {
  LocalDate start=LocalDate.parse(rule.getStartDate());if(date.isBefore(start))date=start;
  return switch(rule.getRuleType()) {
   case "weekly" -> { Set<Integer> days=days(rule.getWeekDays());while(!days.contains(date.getDayOfWeek().getValue()))date=date.plusDays(1);yield date; }
   case "monthly" -> { LocalDate candidate=date.withDayOfMonth(Math.min(rule.getMonthDay(),date.lengthOfMonth()));if(candidate.isBefore(date)){LocalDate next=date.plusMonths(1);candidate=next.withDayOfMonth(Math.min(rule.getMonthDay(),next.lengthOfMonth()));}yield candidate; }
   case "daily" -> { long diff=java.time.temporal.ChronoUnit.DAYS.between(start,date);long remainder=diff%rule.getIntervalDays();yield remainder==0?date:date.plusDays(rule.getIntervalDays()-remainder); }
   default -> date;
  };
 }
 public static ZonedDateTime at(TodoSeries rule,LocalDate day) {
  // Java's zone rules move nonexistent DST times forward and use the earlier offset for overlaps.
  return day.atTime(LocalTime.parse(rule.getLocalTime())).atZone(ZoneId.of(rule.getZoneId()));
 }
}
