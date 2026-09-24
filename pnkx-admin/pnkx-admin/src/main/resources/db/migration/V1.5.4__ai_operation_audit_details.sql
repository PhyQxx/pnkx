-- AI 写操作审计拆分保存执行计划、实际结果和补偿/回滚信息。
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'px_ai_operation_log' AND column_name = 'plan_json') = 0,
              'ALTER TABLE px_ai_operation_log ADD COLUMN plan_json JSON DEFAULT NULL COMMENT ''确认前执行计划'' AFTER parsed_json', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'px_ai_operation_log' AND column_name = 'result_json') = 0,
              'ALTER TABLE px_ai_operation_log ADD COLUMN result_json JSON DEFAULT NULL COMMENT ''执行结果'' AFTER plan_json', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'px_ai_operation_log' AND column_name = 'rollback_json') = 0,
              'ALTER TABLE px_ai_operation_log ADD COLUMN rollback_json JSON DEFAULT NULL COMMENT ''回滚或补偿信息'' AFTER result_json', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 回滚说明（仅人工回滚时执行）：
-- ALTER TABLE `px_ai_operation_log`
--   DROP COLUMN `rollback_json`, DROP COLUMN `result_json`, DROP COLUMN `plan_json`;
