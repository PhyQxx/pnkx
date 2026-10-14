package com.pnkx.quartz.task;

import com.pnkx.life.service.impl.PxBookkeepingRecurringService;
import com.pnkx.life.service.recurrence.TodoSeriesService;
import com.pnkx.life.service.IPxSubscriptionService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 周期任务统一入口（周期收一 §3.4）。
 * <p>
 * 重复待办 / 周期记账 / 订阅出账三个域共用一个 Quartz 触发器（recurrenceTask.run），
 * 域与域之间异常隔离——单域失败不影响其余域，统一记录开始/结果/耗时。
 * 旧入口（todoSeriesTask / bookkeepingRecurringTask / subscriptionTask）保留，
 * sys_job 侧已暂停（V1.8.2），如需回滚改回 invoke_target 即可。
 *
 * @author PHY
 * @date 2026-10-14
 */
@Component("recurrenceTask")
public class RecurrenceTask {

    private static final Logger log = LoggerFactory.getLogger(RecurrenceTask.class);

    @Resource
    private TodoSeriesService todoSeriesService;

    @Resource
    private PxBookkeepingRecurringService recurringService;

    @Resource
    private IPxSubscriptionService subscriptionService;

    /**
     * invokeTarget: recurrenceTask.run
     * cron 建议：0 5 0 * * ?（每日 00:05，覆盖原订阅 01:00 / 记账 08:30 / 待办 5 分钟轮询）
     */
    public void run() {
        long start = System.currentTimeMillis();
        log.info("【周期任务】统一调度开始");

        int todoBatches = runDomain("重复待办", () -> {
            long after = 0;
            int generated = 0;
            while (true) {
                var ids = todoSeriesService.active(after);
                for (Long id : ids) {
                    try {
                        todoSeriesService.generate(id);
                        generated++;
                    } catch (RuntimeException ex) {
                        log.error("【周期任务】Series generation failed: {}", id, ex);
                    }
                }
                if (ids.size() < 100) break;
                after = ids.getLast();
            }
            return generated;
        });

        int bills = runDomain("周期记账", () -> recurringService.executeDue());
        int subscriptions = runDomain("订阅出账", () -> subscriptionService.generateDueRecords());

        log.info("【周期任务】统一调度完成：待办批次项 {}，账单 {}，订阅 {}，耗时 {}ms",
                todoBatches, bills, subscriptions, System.currentTimeMillis() - start);
    }

    /**
     * 单域执行：异常隔离并返回该域结果计数（异常返回 -1，便于日志区分"失败"与"0 处理"）
     */
    private int runDomain(String name, DomainAction action) {
        try {
            return action.run();
        } catch (Exception e) {
            log.error("【周期任务】{}域执行异常", name, e);
            return -1;
        }
    }

    @FunctionalInterface
    private interface DomainAction {
        int run();
    }
}
