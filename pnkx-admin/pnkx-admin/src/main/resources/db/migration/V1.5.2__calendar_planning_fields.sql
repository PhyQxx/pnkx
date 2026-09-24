-- 统一时间轴需要显式的购物计划日和阅读目标日。
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'px_shopping_list' AND column_name = 'planned_date') = 0,
              'ALTER TABLE px_shopping_list ADD COLUMN planned_date DATE DEFAULT NULL COMMENT ''计划采购日期'' AFTER order_num', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'px_book' AND column_name = 'target_finish_date') = 0,
              'ALTER TABLE px_book ADD COLUMN target_finish_date DATE DEFAULT NULL COMMENT ''目标读完日期'' AFTER status', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'px_shopping_list' AND index_name = 'idx_shopping_planned_date') = 0,
              'ALTER TABLE px_shopping_list ADD INDEX idx_shopping_planned_date (create_by, planned_date, del_flag)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'px_book' AND index_name = 'idx_book_target_finish_date') = 0,
              'ALTER TABLE px_book ADD INDEX idx_book_target_finish_date (create_by, target_finish_date, del_flag)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 回滚说明（仅人工回滚时执行）：
-- ALTER TABLE `px_shopping_list` DROP INDEX `idx_shopping_planned_date`, DROP COLUMN `planned_date`;
-- ALTER TABLE `px_book` DROP INDEX `idx_book_target_finish_date`, DROP COLUMN `target_finish_date`;
