package com.pnkx.life.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.domain.po.*;
import com.pnkx.life.mapper.*;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** 业务契约测试；数据库行锁/回滚另需 MySQL 集成验收。 */
class SubscriptionChargeServiceTest {
  private final LocalDate due = LocalDate.of(2026, 1, 31);
  private final PxSubscription sub = subscription();
  private PxBookkeepingRecord existing;
  private int inserts;
  private boolean failInsert;
  private final PxSubscriptionMapper subscriptions =
      (PxSubscriptionMapper)
          Proxy.newProxyInstance(
              getClass().getClassLoader(),
              new Class[] {PxSubscriptionMapper.class},
              (p, m, a) -> {
                if (m.getName().equals("selectForCharge")) return sub;
                if (m.getName().equals("updatePxSubscription")) {
                  sub.setNextPaymentDate(((PxSubscription) a[0]).getNextPaymentDate());
                  return 1;
                }
                throw new UnsupportedOperationException(m.getName());
              });
  private final PxBookkeepingRecordMapper records =
      (PxBookkeepingRecordMapper)
          Proxy.newProxyInstance(
              getClass().getClassLoader(),
              new Class[] {PxBookkeepingRecordMapper.class},
              (p, m, a) -> {
                if (m.getName().equals("selectByClientUuid")) return existing;
                if (m.getName().equals("insertPxBookkeepingRecord")) {
                  if (failInsert) throw new ServiceException("模拟写入失败");
                  inserts++;
                  existing = (PxBookkeepingRecord) a[0];
                  existing.setId(9L);
                  return 1;
                }
                throw new UnsupportedOperationException(m.getName());
              });

  private PxSubscription subscription() {
    PxSubscription s = new PxSubscription();
    s.setId(1L);
    s.setCreateBy("owner");
    s.setEnabled(true);
    s.setAmount(new BigDecimal("30"));
    s.setCycle("monthly");
    s.setAnchorDay(31);
    s.setNextPaymentDate(java.sql.Date.valueOf("2026-01-31"));
    return s;
  }

  private SubscriptionChargeService service() {
    return new SubscriptionChargeService(
        subscriptions,
        new ChargeInstanceService(null, records, null, null) {
          @Override
          public PxChargeInstance generate(
              String owner,
              String type,
              Long source,
              String name,
              LocalDate date,
              BigDecimal amount,
              Long account,
              Long classification,
              Long other,
              String mode,
              String key) {
            if (existing == null) {
              var r = new PxBookkeepingRecord();
              r.setCreateBy(owner);
              r.setClientUuid(key);
              records.insertPxBookkeepingRecord(r);
            }
            PxChargeInstance row = new PxChargeInstance();
            row.setRecordId(existing.getId());
            return row;
          }
        });
  }

  @Test
  void staleSecondEntryCannotChargeAgain() {
    var result = service().charge(1L, "owner", due, due);
    assertEquals(LocalDate.of(2026, 2, 28), result.nextDate());
    assertEquals("sub_1_2026-01-31", existing.getClientUuid());
    assertNull(service().charge(1L, "owner", due, due.plusMonths(2)));
    assertEquals(1, inserts);
  }

  @Test
  void repairsLegacyRecordWithoutReinserting() {
    existing = new PxBookkeepingRecord();
    existing.setId(8L);
    existing.setCreateBy("owner");
    assertEquals(8L, service().charge(1L, "owner", due, due).recordId());
    assertEquals(0, inserts);
    assertEquals(LocalDate.of(2026, 2, 28), SubscriptionSchedule.date(sub.getNextPaymentDate()));
  }

  @Test
  void disabledFutureAndWrongOwnerCannotWrite() {
    assertThrows(ServiceException.class, () -> service().charge(1L, "other", due, due));
    assertNull(service().charge(1L, "owner", due, due.minusDays(1)));
    sub.setEnabled(false);
    assertNull(service().charge(1L, "owner", due, due));
    assertEquals(0, inserts);
  }

  @Test
  void trialDoesNotProducePaidPeriod() {
    sub.setTrialEndDate(java.sql.Date.valueOf("2026-02-01"));
    var result = service().charge(1L, "owner", due, due);
    assertFalse(result.created());
    assertEquals(0, inserts);
    assertEquals(LocalDate.of(2026, 2, 28), result.nextDate());
  }

  @Test
  void cancelledCannotChargeEvenIfLegacyEnabledIsTrue() {
    sub.setLifecycle("cancelled");
    assertNull(service().charge(1L, "owner", due, due));
    assertEquals(0, inserts);
  }

  @Test
  void insertionFailureDoesNotAdvanceSchedule() {
    failInsert = true;
    assertThrows(ServiceException.class, () -> service().charge(1L, "owner", due, due));
    assertEquals(due, SubscriptionSchedule.date(sub.getNextPaymentDate()));
  }
}
