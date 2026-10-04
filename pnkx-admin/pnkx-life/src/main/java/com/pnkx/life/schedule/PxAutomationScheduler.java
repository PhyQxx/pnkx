package com.pnkx.life.schedule;

import com.pnkx.life.service.IPxAutomationService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 生活自动化规则的定时触发器。
 * <p>
 * 调度与业务分离：@Scheduled 从 PxAutomationServiceImpl 拆出，
 * service 保持可被接口/测试直接调用。
 * 运维开关：-Dpnkx.automation.enabled=false（或环境变量）可关闭本调度器
 * 而不影响规则管理接口——多实例/临时验证启动时用它防止重复执行。
 *
 * @author phy
 */
@Component
@ConditionalOnProperty(value = "pnkx.automation.enabled", havingValue = "true", matchIfMissing = true)
public class PxAutomationScheduler {

    private static final Logger log = LoggerFactory.getLogger(PxAutomationScheduler.class);

    @Resource
    private IPxAutomationService automationService;

    @Scheduled(cron = "0 * * * * ?")
    public void triggerDueRules() {
        try {
            int executed = automationService.executeDueRules();
            if (executed > 0) {
                log.info("【自动化】本轮触发 {} 条规则", executed);
            }
        } catch (Exception e) {
            log.error("【自动化】本轮规则触发异常", e);
        }
    }
}
