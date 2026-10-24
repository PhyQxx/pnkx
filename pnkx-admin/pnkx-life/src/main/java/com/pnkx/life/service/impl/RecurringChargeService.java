package com.pnkx.life.service.impl;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.mapper.PxBookkeepingRecurringMapper;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class RecurringChargeService {
  @jakarta.annotation.Resource private ChargeExecutionService executions;
  private final PxBookkeepingRecurringMapper rules;
  @jakarta.annotation.Resource private LifeFeatureGate featureGate;
  private final ChargeInstanceService instances;

  public RecurringChargeService(
      PxBookkeepingRecurringMapper rules, ChargeInstanceService instances) {
    this.rules = rules;
    this.instances = instances;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
  public LocalDate charge(Long id, String owner, LocalDate expected, LocalDate today) {
    return charge(id, owner, expected, today, false);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
  public LocalDate charge(
      Long id, String owner, LocalDate expected, LocalDate today, boolean force) {
    if (featureGate != null && !featureGate.enabled("chargeInstanceV2", owner)) return null;
    var rule = rules.lockForCharge(id, owner);
    if (rule == null || !Boolean.TRUE.equals(rule.getEnabled())) return null;
    LocalDate due = SubscriptionSchedule.date(rule.getNextRunDate());
    if (!due.equals(expected) || due.isAfter(today)) return null;
    if (!force && executions != null && !executions.ready(owner, "recurring", id, due)) return null;
    if (rule.getMoney() == null || rule.getMoney().signum() <= 0)
      throw new ServiceException("周期金额无效");
    instances.generate(
        owner,
        "recurring",
        id,
        rule.getName(),
        due,
        rule.getMoney(),
        rule.getAccount(),
        "2".equals(rule.getTypeDifference()) ? Long.valueOf(0L) : rule.getType(),
        rule.getOtherAccount(),
        rule.getPostingMode(),
        null);
    LocalDate next = PxBookkeepingRecurringService.computeNextRunDate(rule, due);
    if (rules.advanceRunDate(id, java.sql.Date.valueOf(next), java.sql.Date.valueOf(due)) != 1)
      throw new ServiceException("规则推进失败");
    if (executions != null) executions.resolved(owner, "recurring", id, due);
    return next;
  }
}
