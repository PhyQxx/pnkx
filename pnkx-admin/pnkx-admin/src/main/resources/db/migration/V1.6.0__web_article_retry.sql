-- ============================================================
-- V1.6.0 网址收藏：失败自动重试（二期）
-- 设计文档：docs/WEB_ARTICLE_DESIGN.md 第 7 节
-- ============================================================

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'px_web_article' AND column_name = 'retry_count') = 0,
              'ALTER TABLE px_web_article ADD COLUMN retry_count INT NOT NULL DEFAULT 0 COMMENT ''自动重试次数，上限3次'' AFTER ai_error', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 失败重试定时任务：每 30 分钟扫描抓取/分析失败且 10 分钟内未变动的记录自动重试（最多 3 次）
INSERT INTO `sys_job`(`job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `remark`, `create_by`, `create_time`)
SELECT '网址收藏失败重试', 'LIFE', 'webArticleRetryTask.execute', '0 */30 * * * ?', '3', '1', '0', '定时重试抓取/AI分析失败的网址收藏（最多3次，超限需人工触发）', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_job` WHERE `invoke_target` = 'webArticleRetryTask.execute');

-- 回滚说明（仅人工回滚时执行）：
-- DELETE FROM sys_job WHERE invoke_target = 'webArticleRetryTask.execute';
-- ALTER TABLE px_web_article DROP COLUMN retry_count;
