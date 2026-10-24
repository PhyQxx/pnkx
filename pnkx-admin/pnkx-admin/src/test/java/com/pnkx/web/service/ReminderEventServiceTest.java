package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.pnkx.life.domain.po.*;
import com.pnkx.life.mapper.*;
import com.pnkx.life.service.impl.ReminderEventService;
import java.util.*;
import org.junit.jupiter.api.*;

class ReminderEventServiceTest {
  PxReminderEventMapper events;
  PxToDoMapper todos;
  PxLifeReminderMapper reminders;
  ReminderEventService service;
  PxReminderEvent event;
  PxLifeReminder reminder;

  @BeforeEach
  void setup() {
    events = mock(PxReminderEventMapper.class);
    todos = mock(PxToDoMapper.class);
    reminders = mock(PxLifeReminderMapper.class);
    service = new ReminderEventService(events, todos, reminders);
    event = new PxReminderEvent();
    event.setId(1L);
    event.setReminderId(2L);
    event.setUserId("1");
    event.setSourceId(3L);
    event.setSourceType("todo");
    event.setState("open");
    event.setRevision(0L);
    reminder = new PxLifeReminder();
    reminder.setEnabled(true);
    when(reminders.selectPxLifeReminderById(2L)).thenReturn(reminder);
    when(events.get(1L, "1")).thenReturn(event);
    when(events.update(any())).thenReturn(1);
    var todo = new PxToDo();
    todo.setStatus(false);
    when(todos.selectPxToDoById(3L)).thenReturn(todo);
  }

  @Test
  void readDoesNotResolveBusiness() {
    service.act(1L, "1", "read", null, null);
    assertEquals("open", event.getState());
    verify(events).read(1L, "1");
    verify(events, never()).update(any());
  }

  @Test
  void completedTodoSuppressesDelivery() {
    var todo = new PxToDo();
    todo.setStatus(true);
    when(todos.selectPxToDoById(3L)).thenReturn(todo);
    assertFalse(service.sendable(1L, "1"));
    assertEquals("resolved", event.getState());
  }

  @Test
  void snoozeChangesEventOnly() {
    Date until = new Date(System.currentTimeMillis() + 3600000);
    service.act(1L, "1", "snooze", 0L, until);
    assertEquals("snoozed", event.getState());
    assertFalse(service.sendable(event));
    verify(reminders, never()).updatePxLifeReminder(any());
  }

  @Test
  void staleRevisionAndOtherOwnerCannotAct() {
    assertThrows(Exception.class, () -> service.act(1L, "1", "dismiss", 9L, null));
    assertThrows(Exception.class, () -> service.act(1L, "2", "dismiss", 0L, null));
    verify(events, never()).update(any());
  }

  @Test
  void disabledReminderCancelsPendingEvent() {
    reminder.setEnabled(false);
    assertFalse(service.sendable(1L, "1"));
    assertEquals("cancelled", event.getState());
  }
}
