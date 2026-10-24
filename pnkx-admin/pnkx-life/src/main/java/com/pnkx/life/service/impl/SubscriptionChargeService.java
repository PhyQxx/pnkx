package com.pnkx.life.service.impl;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.life.domain.po.PxSubscription;
import com.pnkx.life.mapper.PxSubscriptionMapper;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 行锁、账期幂等与计划推进在单期事务内完成，隔离调度与自动化日志事务。 */
@Service
public class SubscriptionChargeService {
  @jakarta.annotation.Resource private ChargeExecutionService executions;
  @jakarta.annotation.Resource private com.pnkx.life.mapper.PxLifeReminderMapper reminders;
  @jakarta.annotation.Resource private ReminderEventService reminderEvents;
  private final PxSubscriptionMapper subscriptions;
  @jakarta.annotation.Resource private LifeFeatureGate featureGate;
  private final ChargeInstanceService instances;

  public SubscriptionChargeService(
      PxSubscriptionMapper subscriptions, ChargeInstanceService instances) {
    this.subscriptions = subscriptions;
    this.instances = instances;
  }

  public record ChargeResult(Long recordId, LocalDate nextDate, boolean created) {}

  @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
  public ChargeResult charge(Long id, String ownerId, LocalDate expectedDue, LocalDate today) {
    return charge(id, ownerId, expectedDue, today, false);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
  public ChargeResult charge(
      Long id, String ownerId, LocalDate expectedDue, LocalDate today, boolean force) {
    if (featureGate != null && !featureGate.enabled("chargeInstanceV2", ownerId)) return null;
    PxSubscription sub = subscriptions.selectForCharge(id);
    if (sub == null || !Objects.equals(ownerId, sub.getCreateBy()))
      throw new ServiceException("订阅不存在或无权操作");
    if ("cancelled".equals(sub.getLifecycle())
        || !Boolean.TRUE.equals(sub.getEnabled())
        || sub.getNextPaymentDate() == null) return null;
    LocalDate due = SubscriptionSchedule.date(sub.getNextPaymentDate());
    if (!due.equals(expectedDue) || due.isAfter(today)) return null;
    if (!force && executions != null && !executions.ready(ownerId, "subscription", id, due))
      return null;
    if (sub.getAmount() == null || sub.getAmount().signum() < 0)
      throw new ServiceException("订阅金额无效");
    LocalDate next =
        SubscriptionSchedule.next(due, sub.getCycle(), sub.getCycleInterval(), sub.getAnchorDay());
    if (sub.getTrialEndDate() != null
        && due.isBefore(SubscriptionSchedule.date(sub.getTrialEndDate()))) {
      var trialUpdate = new PxSubscription();
      trialUpdate.setId(id);
      trialUpdate.setNextPaymentDate(java.sql.Date.valueOf(next));
      if (subscriptions.updatePxSubscription(trialUpdate) != 1)
        throw new ServiceException("试用计划推进失败");
      advanceReminder(sub, next, false);
      return new ChargeResult(null, next, false);
    }
    var instance =
        instances.generate(
            ownerId,
            "subscription",
            id,
            sub.getName(),
            due,
            sub.getAmount(),
            sub.getAccountId(),
            sub.getClassificationId(),
            null,
            sub.getPostingMode(),
            "sub_" + id + "_" + due);
    advanceReminder(sub, next, true);
    PxSubscription update = new PxSubscription();
    update.setId(id);
    update.setNextPaymentDate(java.sql.Date.valueOf(next));
    update.setAnchorDay(sub.getAnchorDay() == null ? due.getDayOfMonth() : sub.getAnchorDay());
    update.setUpdateTime(DateUtils.getNowDate());
    if (subscriptions.updatePxSubscription(update) != 1) throw new ServiceException("扣费计划推进失败");
    if (executions != null) executions.resolved(ownerId, "subscription", id, due);
    return new ChargeResult(instance.getRecordId(), next, true);
  }

  private void advanceReminder(PxSubscription sub, LocalDate next, boolean createEvent) {
    if (reminders == null) return;
    var reminder = reminders.selectBySource("subscription", sub.getId(), sub.getCreateBy());
    if (reminder == null
        || !Boolean.TRUE.equals(reminder.getEnabled())
        || "1".equals(reminder.getDelFlag())) return;
    if (createEvent
        && (featureGate == null || featureGate.enabled("reminderEventV2", sub.getCreateBy())))
      reminderEvents.ensure(reminder, "订阅账期待处理：" + sub.getName());
    var time =
        next.atStartOfDay(SubscriptionSchedule.ZONE)
            .toInstant()
            .minusSeconds(
                60L * (reminder.getLeadMinutes() == null ? 0 : reminder.getLeadMinutes()));
    reminders.reschedule(reminder.getId(), java.util.Date.from(time));
  }
}
