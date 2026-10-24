package com.pnkx.life.mapper;

import com.pnkx.life.domain.po.PxReminderEvent;
import java.util.*;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PxReminderEventMapper {
  String C =
      "id,reminder_id reminderId,user_id userId,occurrence_at occurrenceAt,source_type"
          + " sourceType,source_id sourceId,source_due_date sourceDueDate,title,state,read_at"
          + " readAt,snooze_until snoozeUntil,revision";

  @Insert(
      "insert into"
          + " px_reminder_event(reminder_id,user_id,occurrence_at,source_type,source_id,title,source_due_date)"
          + " values(#{reminderId},#{userId},#{occurrenceAt},#{sourceType},#{sourceId},#{title},#{sourceDueDate})"
          + " on duplicate key update id=id")
  int ensure(PxReminderEvent event);

  @Select(
      "select "
          + C
          + " from px_reminder_event where reminder_id=#{reminderId} and user_id=#{userId} and"
          + " occurrence_at=#{occurrenceAt}")
  PxReminderEvent occurrence(PxReminderEvent event);

  @Select(
      "select "
          + C
          + " from px_reminder_event where user_id=#{user} order by occurrence_at desc,id desc"
          + " limit 100")
  List<PxReminderEvent> list(String user);

  @Select("select " + C + " from px_reminder_event where id=#{id} and user_id=#{user}")
  PxReminderEvent get(@Param("id") Long id, @Param("user") String user);

  @Select(
      "select "
          + C
          + " from px_reminder_event where state='snoozed' and snooze_until<=now() limit 100")
  List<PxReminderEvent> due();

  @Update(
      "update px_reminder_event set state=#{state},snooze_until=#{snoozeUntil},revision=revision+1"
          + " where id=#{id} and user_id=#{userId} and revision=#{revision}")
  int update(PxReminderEvent event);

  @Update(
      "update px_reminder_event set read_at=coalesce(read_at,now()) where id=#{id} and"
          + " user_id=#{user}")
  int read(@Param("id") Long id, @Param("user") String user);
}
