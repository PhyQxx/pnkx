-- ============================================================
-- V1.8.2  周期收一（§3.4）：三个 Quartz 触发器合一
-- ============================================================
-- 重复待办(todoSeriesTask) / 周期记账(bookkeepingRecurringTask) / 订阅出账(subscriptionTask)
-- 合并为单一入口 recurrenceTask.run（域间异常隔离，行为不变）。
-- 旧 job 暂停保留，回滚只需改回 invoke_target 并停用新 job。
-- ============================================================

INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
VALUES ('周期任务统一调度', 'DEFAULT', 'recurrenceTask.run', '0 5 0 * * ?', '1', '1', '0', 'admin', NOW(),
        '重复待办/周期记账/订阅出账 统一入口（周期收一）');

UPDATE sys_job SET status = '1',
                   remark = CONCAT(IFNULL(remark, ''), ' [已并入 recurrenceTask.run（V1.8.2），暂停保留]')
   WHERE invoke_target IN ('todoSeriesTask.generate', 'bookkeepingRecurringTask.execute', 'subscriptionTask.generateDueRecords')
     AND status = '0';
