package com.pnkx.life.service.impl;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.domain.po.*;
import com.pnkx.life.mapper.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class ReminderEventService {
  @jakarta.annotation.Resource private PxSubscriptionMapper subscriptions;
  @jakarta.annotation.Resource private PxChargeInstanceMapper charges;
  private final PxReminderEventMapper events;
  private final PxToDoMapper todos;
  private final PxLifeReminderMapper reminders;

  public ReminderEventService(
      PxReminderEventMapper events, PxToDoMapper todos, PxLifeReminderMapper reminders) {
    this.events = events;
    this.todos = todos;
    this.reminders = reminders;
  }

  public PxReminderEvent ensure(PxLifeReminder r, String title) {
    var e = new PxReminderEvent();
    e.setReminderId(r.getId());
    e.setUserId(r.getUserId());
    e.setOccurrenceAt(r.getRemindTime());
    e.setSourceType(r.getSourceType());
    e.setSourceId(r.getSourceId());
    e.setTitle(title);
    e.setSourceDueDate(
        r.getRemindTime()
            .toInstant()
            .plusSeconds(60L * (r.getLeadMinutes() == null ? 0 : r.getLeadMinutes()))
            .atZone(SubscriptionSchedule.ZONE)
            .toLocalDate()
            .toString());
    events.ensure(e);
    return reconcile(events.occurrence(e));
  }

  public List<PxReminderEvent> list(String user) {
    return events.list(user).stream().map(this::reconcile).toList();
  }

  public List<PxLifeReminder> snoozed() {
    List<PxLifeReminder> result = new ArrayList<>();
    for (var e : events.due()) {
      e = reconcile(e);
      if (!"snoozed".equals(e.getState())) continue;
      var r = reminders.selectPxLifeReminderById(e.getReminderId());
      if (r != null
          && Boolean.TRUE.equals(r.getEnabled())
          && ("subscription".equals(e.getSourceType())
              || Objects.equals(r.getRemindTime(), e.getOccurrenceAt()))) {
        // 延后的是这一次事件，不把提醒错误地投递为下一账期。
        r.setRemindTime(e.getOccurrenceAt());
        result.add(r);
      } else {
        e.setState("cancelled");
        events.update(e);
      }
    }
    return result;
  }

  public boolean sendable(PxReminderEvent e) {
    return "open".equals(e.getState())
        || ("snoozed".equals(e.getState())
            && e.getSnoozeUntil() != null
            && !e.getSnoozeUntil().after(new Date()));
  }

  public void delivered(PxReminderEvent e) {
    if ("snoozed".equals(e.getState())) {
      e.setState("open");
      e.setSnoozeUntil(null);
      events.update(e);
    }
  }

  public boolean sendable(Long id, String user) {
    if (id == null) return false;
    var e = events.get(id, user);
    return e != null && sendable(reconcile(e));
  }

  public PxReminderEvent act(Long id, String user, String action, Long revision, Date until) {
    var e = events.get(id, user);
    if (e == null) throw new ServiceException("SOURCE_UNAVAILABLE");
    e = reconcile(e);
    if ("read".equals(action)) {
      events.read(id, user);
      return events.get(id, user);
    }
    if (!Objects.equals(e.getRevision(), revision)) throw new ServiceException("REVISION_CONFLICT");
    if (!Set.of("open", "snoozed").contains(e.getState()))
      throw new ServiceException("INVALID_TRANSITION");
    if ("snooze".equals(action)) {
      if (until == null
          || !until.after(new Date())
          || until.toInstant().isAfter(Instant.now().plusSeconds(30L * 86400)))
        throw new ServiceException("延后时间须在未来30天内");
      e.setState("snoozed");
      e.setSnoozeUntil(until);
    } else if ("dismiss".equals(action)) {
      e.setState("cancelled");
      e.setSnoozeUntil(null);
    } else throw new ServiceException("INVALID_ACTION");
    if (events.update(e) != 1) throw new ServiceException("REVISION_CONFLICT");
    return events.get(id, user);
  }

  private PxReminderEvent reconcile(PxReminderEvent e) {
    if (e == null || !Set.of("open", "snoozed").contains(e.getState())) return e;
    var r = reminders.selectPxLifeReminderById(e.getReminderId());
    String state = null;
    if (r == null || "1".equals(r.getDelFlag()) || !Boolean.TRUE.equals(r.getEnabled()))
      state = "cancelled";
    if (r != null
        && "todo".equals(e.getSourceType())
        && !Objects.equals(r.getRemindTime(), e.getOccurrenceAt())) state = "cancelled";
    if ("todo".equals(e.getSourceType())) {
      var t = todos.selectPxToDoById(e.getSourceId());
      if (t == null) state = "cancelled";
      else if (Boolean.TRUE.equals(t.getStatus())) state = "resolved";
    }
    if ("subscription".equals(e.getSourceType())) {
      var sub = subscriptions.selectPxSubscriptionById(e.getSourceId());
      if (sub == null || "1".equals(sub.getDelFlag()) || !Boolean.TRUE.equals(sub.getEnabled()))
        state = "cancelled";
      else if (e.getSourceDueDate() != null) {
        var charge =
            charges.period(e.getUserId(), "subscription", e.getSourceId(), e.getSourceDueDate());
        if (charge != null
            && Set.of("posted", "skipped", "cancelled", "voided").contains(charge.getStatus()))
          state = "resolved";
      }
    }
    if (state != null) {
      e.setState(state);
      e.setSnoozeUntil(null);
      if (events.update(e) == 1) e.setRevision(e.getRevision() + 1);
      else return events.get(e.getId(), e.getUserId());
    }
    return e;
  }
}
