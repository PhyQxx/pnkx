INSERT INTO sys_job(job_name,job_group,invoke_target,cron_expression,misfire_policy,concurrent,status,remark,create_by,create_time)
SELECT '重复待办生成','LIFE','todoSeriesTask.generate','0 */5 * * * ?','3','1','0','按用户时区生成到期实例，每系列每轮最多补偿31次','admin',CURRENT_TIMESTAMP
WHERE NOT EXISTS(SELECT 1 FROM sys_job WHERE invoke_target='todoSeriesTask.generate');
