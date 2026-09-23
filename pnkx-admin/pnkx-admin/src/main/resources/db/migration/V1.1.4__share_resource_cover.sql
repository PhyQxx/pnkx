-- ============================================================
-- V1.1.4 分享资源封面图
--   给 px_share_resource 增加 cover 字段，作为前台分享卡片封面
-- ============================================================

DROP PROCEDURE IF EXISTS pro_add_share_resource_cover;
DELIMITER $$
CREATE PROCEDURE pro_add_share_resource_cover()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'px_share_resource'
                     AND COLUMN_NAME = 'cover') THEN
        ALTER TABLE `px_share_resource`
            ADD COLUMN `cover` VARCHAR(500) DEFAULT NULL COMMENT '封面' AFTER `share_url`;
    END IF;
END$$
DELIMITER ;
CALL pro_add_share_resource_cover();
DROP PROCEDURE IF EXISTS pro_add_share_resource_cover;
