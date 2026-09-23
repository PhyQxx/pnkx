-- ============================================================
-- V1.1.2 壁纸分享小程序奖励下载次数
--   1. 新建分享奖励记录表 px_wallpaper_share_reward_record
--      与真实下载记录(px_wallpaper_download_record)隔离，避免污染
--      每日下载次数统计(countTodayDownload)和下载统计报表。
--   2. 系统参数：每次分享奖励的下载次数、每日最多可分享获奖次数。
-- 幂等：所有改动判断后执行
-- ============================================================

-- 1. 新建分享奖励记录表
CREATE TABLE IF NOT EXISTS `px_wallpaper_share_reward_record` (
    `id`            BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `create_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '用户ID',
    `reward_count`  INT(11)      NOT NULL DEFAULT 0      COMMENT '本次奖励的下载次数',
    `create_time`   DATETIME     DEFAULT NULL            COMMENT '分享时间',
    PRIMARY KEY (`id`),
    KEY `idx_create_by` (`create_by`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='壁纸分享小程序奖励下载次数记录';

-- 2. 系统参数：每次分享小程序奖励的下载次数（在「系统管理-参数设置」可动态调整）
INSERT INTO `sys_config`(`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
SELECT '壁纸分享奖励次数', 'sys.wallpaper.download.share.reward', '10', 'Y', 'admin', NOW(), '分享小程序每次奖励的额外下载次数'
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'sys.wallpaper.download.share.reward');

-- 3. 系统参数：每日最多可分享获奖次数（防刷，0 表示不限）
INSERT INTO `sys_config`(`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
SELECT '壁纸每日分享次数上限', 'sys.wallpaper.download.share.daily.times', '3', 'Y', 'admin', NOW(), '每天最多可分享获奖的次数（防刷）'
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'sys.wallpaper.download.share.daily.times');
