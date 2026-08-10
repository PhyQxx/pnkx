package com.pnkx.quartz.task;

import com.pnkx.service.IPxSubscriptionService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 订阅自动出账定时任务。
 * <p>
 * 由 sys_job 配置 invokeTarget = "subscriptionTask.generateDueRecords" 调用。
 * 建议 cron 每天凌晨 1 点：0 0 1 * * ?
 *
 * @author PHY
 * @date 2026/07/05
 */
@Component("subscriptionTask")
public class SubscriptionTask {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionTask.class);

    @Resource
    private IPxSubscriptionService subscriptionService;

    /**
     * invokeTarget: subscriptionTask.generateDueRecords
     */
    public void generateDueRecords() {
        long start = System.currentTimeMillis();
        log.info("【订阅出账】调度开始");
        try {
            int count = subscriptionService.generateDueRecords();
            log.info("【订阅出账】调度完成，处理 {} 个订阅，耗时 {}ms", count, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("【订阅出账】调度异常", e);
        }
    }
}
