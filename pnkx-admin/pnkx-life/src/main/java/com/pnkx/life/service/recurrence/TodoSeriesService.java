package com.pnkx.life.service.recurrence;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.domain.po.PxToDo;
import com.pnkx.life.domain.po.PxLifeReminder;
import com.pnkx.life.mapper.TodoSeriesMapper;
import com.pnkx.life.mapper.PxToDoMapper;
import com.pnkx.life.service.IPxLifeReminderService;
import com.pnkx.life.service.NoteAccessService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
@Service
public class TodoSeriesService {
 @Resource private TodoSeriesMapper mapper;
 @Resource private PxToDoMapper todos;
 @Resource private IPxLifeReminderService reminders;
 public List<TodoSeries> list(){ return mapper.list(NoteAccessService.currentUserId()); }
 @Transactional
 public TodoSeries save(TodoSeries input,String scope) {
  RecurrenceRule.validate(input);String owner=NoteAccessService.currentUserId();input.setOwnerId(owner);
  if(!Set.of("future","all").contains(scope)) throw new ServiceException("编辑范围不正确；仅本次请在待办详情编辑");
  LocalDate today=LocalDate.now(ZoneId.of(input.getZoneId()));
  if(input.getId()==null) {
   if(mapper.list(owner).size()>=200)throw new ServiceException("最多 200 个重复系列");
   input.setNextDate(RecurrenceRule.onOrAfter(input,LocalDate.parse(input.getStartDate())).toString());input.setRevision(1L);mapper.insert(input);
  } else {
   TodoSeries existing=mapper.lock(input.getId());if(existing==null || !owner.equals(existing.getOwnerId()))throw NoteAccessService.unavailable();
   if(!Objects.equals(input.getRevision(),existing.getRevision()))throw new ServiceException("重复规则已变化，请重新加载");
   input.setNextDate(RecurrenceRule.onOrAfter(input,today).toString());
   input.setWaitingCompletion(input.getRuleType().equals("completion") && existing.getLastTodoId()!=null);
   if(mapper.update(input)==0)throw new ServiceException("重复规则已变化，请重新加载");
   input.setRevision(input.getRevision()+1);
   // Already-created incomplete occurrences retain their dates; all-scope edits replace their title and reminders.
   if(scope.equals("all"))for(Long id:mapper.unfinished(input.getId(),owner)) {
    PxToDo current=todos.selectPxToDoById(id);PxToDo patch=new PxToDo();patch.setId(id);patch.setContent(input.getTitle());patch.setUpdateTime(new Date());todos.updatePxToDo(patch);
    reminders.unbindReminder("todo",id,owner);
    if(!Boolean.TRUE.equals(input.getPaused()) && current.getPlanEndTime()!=null) bind(input,id,LocalDateTime.parse(current.getPlanEndTime(),DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atZone(ZoneId.systemDefault()).toInstant());
   }
  }
  return input;
 }
 public List<Long> active(long after){return mapper.active(after);}
 /** Scheduler-only entry. Owner is loaded from the locked series, never a request-supplied user. */
 @Transactional
 public int generate(Long id) {
  TodoSeries series=mapper.lock(id);if(series==null||Boolean.TRUE.equals(series.getPaused()))return 0;
  LocalDate today=LocalDate.now(ZoneId.of(series.getZoneId()));
  if(Boolean.TRUE.equals(series.getWaitingCompletion())) {
   PxToDo last=todos.selectPxToDoById(series.getLastTodoId());
   if(last!=null&&!Boolean.TRUE.equals(last.getStatus()))return 0;
   LocalDate completed=last==null||last.getFinishTime()==null?today:LocalDateTime.parse(last.getFinishTime(),DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of(series.getZoneId())).toLocalDate();
   if(completed.isBefore(LocalDate.parse(series.getNextDate())))completed=LocalDate.parse(series.getNextDate());
   series.setNextDate(completed.plusDays(series.getIntervalDays()).toString());series.setWaitingCompletion(false);
  }
  int created=0;LocalDate date=LocalDate.parse(series.getNextDate());
  for(int scanned=0;scanned<31 && !date.isAfter(today);scanned++) {
   if(series.getEndDate()!=null && date.isAfter(LocalDate.parse(series.getEndDate())))break;
   Long todoId=mapper.occurrenceId(id,date.toString());
   if(todoId==null) {
    PxToDo todo=new PxToDo();todo.setContent(series.getTitle());todo.setCreateBy(series.getOwnerId());todo.setPerformer(series.getOwnerId());todo.setCreateTime(new Date());todo.setUpdateTime(new Date());todo.setStatus(false);todo.setParentId(0L);todo.setKanbanStatus(0);
    Instant instant=RecurrenceRule.at(series,date).toInstant();String local=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(instant.atZone(ZoneId.systemDefault()));todo.setPlanStartTime(local);todo.setPlanEndTime(local);
    todos.insertPxToDo(todo);todoId=todo.getId();mapper.occurrence(id,date.toString(),todoId);bind(series,todoId,instant);created++;
   }
   series.setLastTodoId(todoId);
   if(series.getRuleType().equals("completion")){series.setWaitingCompletion(true);break;}
   date=RecurrenceRule.onOrAfter(series,date.plusDays(1));
  }
  series.setNextDate(date.toString());mapper.progress(series);return created;
 }
 private void bind(TodoSeries series,Long id,Instant instant) {
  if(series.getLeadMinutes()==null)return;
  PxLifeReminder reminder=new PxLifeReminder();reminder.setSourceType("todo");reminder.setSourceId(id);reminder.setUserId(series.getOwnerId());reminder.setEnabled(true);reminder.setLeadMinutes(series.getLeadMinutes());reminder.setRemindTime(Date.from(instant.minusSeconds(series.getLeadMinutes()*60L)));reminders.bindReminder(reminder);
 }
}
