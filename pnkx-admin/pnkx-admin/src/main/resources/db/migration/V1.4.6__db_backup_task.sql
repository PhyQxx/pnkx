-- 数据库自动备份任务注册：每日 03:00 全量逻辑备份 → gzip → FTP（保留 30 天）
-- 实现见 com.pnkx.quartz.task.DbBackupTask（纯 JDBC 导出，容器/裸机通用）
-- 2026-09-23

INSERT INTO `sys_job`(`job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `remark`, `create_by`, `create_time`)
SELECT '数据库自动备份', 'SYSTEM', 'dbBackupTask.execute', '0 0 3 * * ?', '3', '1', '0', '每日凌晨全量逻辑备份并上传FTP，保留30天', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_job` WHERE `invoke_target` = 'dbBackupTask.execute');
