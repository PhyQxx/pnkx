-- ============================================================
-- V1.0.1 壁纸点赞、排序、微信登录
--   1. px_wallpaper 增加 like_count 字段（用于按热度排序）
--   2. sys_user 增加 openid 字段（微信小程序登录）
--   幂等：所有 ALTER 用 IF NOT EXISTS 风格（MySQL 无原生支持，用存储过程判断）
-- ============================================================

-- px_wallpaper 增加 like_count
DROP PROCEDURE IF EXISTS pro_add_wallpaper_like_count;
DELIMITER $$
CREATE PROCEDURE pro_add_wallpaper_like_count()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'px_wallpaper' AND COLUMN_NAME = 'like_count') THEN
        ALTER TABLE `px_wallpaper` ADD COLUMN `like_count` INT(11) DEFAULT 0 COMMENT '点赞数' AFTER `folder`;
        ALTER TABLE `px_wallpaper` ADD INDEX `idx_like_count` (`like_count`);
    END IF;
END$$
DELIMITER ;
CALL pro_add_wallpaper_like_count();
DROP PROCEDURE IF EXISTS pro_add_wallpaper_like_count;

-- sys_user 增加 openid（微信小程序登录标识）
DROP PROCEDURE IF EXISTS pro_add_sys_user_openid;
DELIMITER $$
CREATE PROCEDURE pro_add_sys_user_openid()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'openid') THEN
        ALTER TABLE `sys_user` ADD COLUMN `openid` VARCHAR(64) DEFAULT NULL COMMENT '微信openid' AFTER `phonenumber`;
        ALTER TABLE `sys_user` ADD INDEX `idx_openid` (`openid`);
    END IF;
END$$
DELIMITER ;
CALL pro_add_sys_user_openid();
DROP PROCEDURE IF EXISTS pro_add_sys_user_openid;
