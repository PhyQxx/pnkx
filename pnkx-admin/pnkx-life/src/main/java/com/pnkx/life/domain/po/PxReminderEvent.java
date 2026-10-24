package com.pnkx.life.domain.po;

import java.util.Date;
import lombok.Data;

@Data
public class PxReminderEvent {
  private Long id;
  private Long reminderId;
  private String userId;
  private Date occurrenceAt;
  private String sourceType;
  private Long sourceId;
  private String title;
  private String sourceDueDate;
  private String state;
  private Date readAt;
  private Date snoozeUntil;
  private Long revision;
}
