-- ============================================================
-- V1.2.2 注册 AI 生活报告定时任务
--
-- 功能：每周一早上 9:00 自动聚合本周生活数据，调用 AI 生成 Markdown 报告，
--      并通过邮件推送给博主（默认 admin）。
--
-- 对应任务类：com.pnkx.quartz.task.AiLifeReportTask
--   invokeTarget = aiLifeReportTask.generate
--
-- cron：0 0 9 ? * MON  → 每周一 09:00
--   misfire_policy = 3（不立即补触发）
--   concurrent = 1（禁止并发）
--   status = 0（正常）
--
-- 幂等：按 invoke_target 去重，重复执行不会产生多条任务
-- ============================================================

INSERT INTO `sys_job`(`job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `remark`, `create_by`, `create_time`)
SELECT 'AI生活周报', 'LIFE', 'aiLifeReportTask.generate', '0 0 9 ? * MON', '3', '1', '0', '每周一聚合生活数据生成AI报告并邮件推送', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_job` WHERE `invoke_target` = 'aiLifeReportTask.generate');
