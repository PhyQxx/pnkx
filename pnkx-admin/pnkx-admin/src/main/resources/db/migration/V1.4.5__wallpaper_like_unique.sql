-- 壁纸点赞幂等化：
--   1. 清理 px_like_record 历史重复数据（同一用户对同一目标同类型仅保留最早一条）
--   2. 增加唯一索引，从数据库层面保证"一人一赞"，并发下的重复插入以唯一键冲突兜底
--   3. 用清洗后的实际记录数校正 px_wallpaper.like_count 冗余字段
-- 2026-09-23

-- 1. 去重（保留最小 id）
DELETE r1 FROM px_like_record r1
    INNER JOIN px_like_record r2
    ON r1.item_id = r2.item_id
        AND r1.type = r2.type
        AND r1.create_by = r2.create_by
        AND r1.id > r2.id;

-- 2. 唯一索引（IF NOT EXISTS 需 MySQL 8.0.29+，用存储过程兼容旧版本）
SET @indexname = 'uk_like_user_item';
SET @tablename = 'px_like_record';
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = @tablename
       AND INDEX_NAME = @indexname) > 0,
    'SELECT 1',
    'CREATE UNIQUE INDEX uk_like_user_item ON px_like_record (item_id, type, create_by)'));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 3. 校正壁纸点赞冗余计数（type '3' 为壁纸点赞）
UPDATE px_wallpaper w
SET w.like_count = (SELECT COUNT(*) FROM px_like_record r
                    WHERE r.item_id = w.id AND r.type = '3' AND r.del_flag = 0);
