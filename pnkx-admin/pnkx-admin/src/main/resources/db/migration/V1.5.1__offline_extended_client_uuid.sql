SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'px_bookkeeping_recurring' AND column_name = 'client_uuid') = 0,
              'ALTER TABLE px_bookkeeping_recurring ADD COLUMN client_uuid VARCHAR(36) DEFAULT NULL COMMENT ''离线创建幂等键'' AFTER id', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'px_menstruation_record' AND column_name = 'client_uuid') = 0,
              'ALTER TABLE px_menstruation_record ADD COLUMN client_uuid VARCHAR(36) DEFAULT NULL COMMENT ''离线创建幂等键'' AFTER id', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'px_book' AND column_name = 'client_uuid') = 0,
              'ALTER TABLE px_book ADD COLUMN client_uuid VARCHAR(36) DEFAULT NULL COMMENT ''离线创建幂等键'' AFTER id', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'px_bookkeeping_recurring' AND index_name = 'uk_recurring_client_uuid') = 0,
              'ALTER TABLE px_bookkeeping_recurring ADD UNIQUE KEY uk_recurring_client_uuid (client_uuid)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'px_menstruation_record' AND index_name = 'uk_menstruation_client_uuid') = 0,
              'ALTER TABLE px_menstruation_record ADD UNIQUE KEY uk_menstruation_client_uuid (client_uuid)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'px_book' AND index_name = 'uk_book_client_uuid') = 0,
              'ALTER TABLE px_book ADD UNIQUE KEY uk_book_client_uuid (client_uuid)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 回滚：分别删除上述三个唯一索引与 client_uuid 列。
