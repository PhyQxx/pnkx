package com.pnkx.service.impl;

import com.pnkx.domain.po.PxBookkeepingRecurring;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 周期记账日期计算单元测试（覆盖跨月/跨年/周末边界）
 *
 * @author PHY
 */
class PxBookkeepingRecurringServiceTest {

    private PxBookkeepingRecurring monthRule(int day) {
        PxBookkeepingRecurring rule = new PxBookkeepingRecurring();
        rule.setFrequency("month");
        rule.setDayNumber(day);
        return rule;
    }

    private PxBookkeepingRecurring weekRule(int weekday) {
        PxBookkeepingRecurring rule = new PxBookkeepingRecurring();
        rule.setFrequency("week");
        rule.setDayNumber(weekday);
        return rule;
    }

    @Test
    void 每月规则_常规推进() {
        // 9月23日 → 10月15日
        assertEquals(LocalDate.of(2026, 10, 15),
                PxBookkeepingRecurringService.computeNextRunDate(monthRule(15), LocalDate.of(2026, 9, 23)));
    }

    @Test
    void 每月规则_跨年() {
        // 12月30日 → 次年1月28日
        assertEquals(LocalDate.of(2027, 1, 28),
                PxBookkeepingRecurringService.computeNextRunDate(monthRule(28), LocalDate.of(2026, 12, 30)));
    }

    @Test
    void 每月规则_基准日恰为执行日应顺延到下月() {
        // 9月15日当天已执行 → 10月15日（不含基准日）
        assertEquals(LocalDate.of(2026, 10, 15),
                PxBookkeepingRecurringService.computeNextRunDate(monthRule(15), LocalDate.of(2026, 9, 15)));
    }

    @Test
    void 每月规则_月末基准() {
        // 1月31日（非执行日）→ 2月28日
        assertEquals(LocalDate.of(2026, 2, 28),
                PxBookkeepingRecurringService.computeNextRunDate(monthRule(28), LocalDate.of(2026, 1, 31)));
    }

    @Test
    void 每周规则_常规推进() {
        // 周四(2026-09-24) → 下周一(2026-09-28)
        assertEquals(LocalDate.of(2026, 9, 28),
                PxBookkeepingRecurringService.computeNextRunDate(weekRule(1), LocalDate.of(2026, 9, 24)));
    }

    @Test
    void 每周规则_基准日为同周同天应顺延七天() {
        // 周五(2026-09-25)执行日周五 → 10月2日
        assertEquals(LocalDate.of(2026, 10, 2),
                PxBookkeepingRecurringService.computeNextRunDate(weekRule(5), LocalDate.of(2026, 9, 25)));
    }

    @Test
    void 每周规则_周日边界() {
        // 周日(2026-09-27) → 下周日(2026-10-04)
        assertEquals(LocalDate.of(2026, 10, 4),
                PxBookkeepingRecurringService.computeNextRunDate(weekRule(7), LocalDate.of(2026, 9, 27)));
    }
}
