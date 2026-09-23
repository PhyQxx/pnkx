package com.pnkx.quartz.task;

import com.pnkx.service.impl.PxBookkeepingRecurringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 周期记账定时任务
 * <p>
 * 每日扫描到期规则（房租/会员等固定支出）自动生成记账记录。
 * 调度：sys_job 配置 invoke_target = bookkeepingRecurringTask.execute（V1.4.8 已注册每日 08:30）。
 *
 * @author PHY
 * @date 2026-09-23
 */
@Component("bookkeepingRecurringTask")
public class BookkeepingRecurringTask {

    private static final Logger log = LoggerFactory.getLogger(BookkeepingRecurringTask.class);

    @Resource
    private PxBookkeepingRecurringService recurringService;

    /**
     * 执行到期的周期记账规则（由 quartz 调度，也可管理端手动触发）
     */
    public void execute() {
        int generated = recurringService.executeDue();
        if (generated > 0) {
            log.info("周期记账手动/定时触发：本次生成 {} 条记录", generated);
        }
    }
}
