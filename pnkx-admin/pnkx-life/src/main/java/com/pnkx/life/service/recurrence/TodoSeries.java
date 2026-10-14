package com.pnkx.life.service.recurrence;
import lombok.Data;
@Data
public class TodoSeries {
 private Long id;
 private String ownerId;
 private String title;
 private String ruleType;
 private Integer intervalDays=1;
 private String weekDays="";
 private Integer monthDay=1;
 private String zoneId="Asia/Shanghai";
 private String localTime="09:00";
 private String startDate;
 private String endDate;
 private String nextDate;
 private Boolean paused=false;
 private Long revision=1L;
 private Long lastTodoId;
 private Boolean waitingCompletion=false;
 private Integer leadMinutes;
}
