package com.pnkx.quartz.task;

import com.pnkx.service.IPxLifeReminderService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 统一提醒引擎定时调度任务。
 * <p>
 * 由 sys_job 表配置 invokeTarget = "lifeReminderTask.dispatch" + cron 表达式调用。
 * 建议 cron 每 5 分钟执行一次：0 *\/5 * * * ?
 * <p>
 * 与 {@link PxTask} 同级，位于 pnkx-quartz 模块，
 * 可直接引用 pnkx-life 的提醒 Service（pnkx-quartz 依赖 pnkx-life）。
 *
 * @author PHY
 * @date 2026/07/02
 */
@Component("lifeReminderTask")
public class LifeReminderTask {

    private static final Logger log = LoggerFactory.getLogger(LifeReminderTask.class);

    @Resource
    private IPxLifeReminderService lifeReminderService;

    /**
     * 扫描到期提醒并分发投递。
     * <p>
     * invokeTarget: lifeReminderTask.dispatch
     */
    public void dispatch() {
        long start = System.currentTimeMillis();
        log.info("【统一提醒】调度开始");
        try {
            int count = lifeReminderService.dispatchReminders();
            log.info("【统一提醒】调度完成，触发 {} 条，耗时 {}ms", count, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("【统一提醒】调度异常", e);
        }
    }
}
