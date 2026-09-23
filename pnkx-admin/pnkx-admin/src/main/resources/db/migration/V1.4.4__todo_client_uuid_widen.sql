-- ============================================================
-- V1.4.4 待办 client_uuid 列加宽到 VARCHAR(64)
--   px_to_do 基表来自旧库导入，client_uuid 原宽度不足；
--   内部系统集成（Aria Companion Hub）以 aria:{uuid} 形式写入
--   client_uuid 做幂等去重，长度 41，旧列宽会触发 Data too long。
-- 幂等：仅当列宽 < 64 时执行 MODIFY，重复执行无副作用。
-- ============================================================

SET @col_width = (SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'px_to_do' AND column_name = 'client_uuid');
SET @sql = IF(@col_width IS NOT NULL AND @col_width < 64,
    'ALTER TABLE `px_to_do` MODIFY COLUMN `client_uuid` VARCHAR(64) DEFAULT NULL COMMENT ''客户端唯一标识（离线幂等去重）''',
    'SELECT ''client_uuid 已是 64 或列不存在''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
