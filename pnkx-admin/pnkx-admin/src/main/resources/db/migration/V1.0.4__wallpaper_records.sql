-- ============================================================
-- V1.0.4 壁纸点赞/收藏/下载记录
--   1. px_like_record 加冗余字段(壁纸名称+缩略图,点赞/收藏共用)
--   2. 新建下载记录表 px_wallpaper_download_record
-- 幂等：所有改动判断后执行
-- ============================================================

-- 1. px_like_record 加冗余字段（记录原貌，壁纸被删改也不影响列表展示）
SET @dbname = DATABASE();
SET @tablename = 'px_like_record';
SET @columnname = 'item_name';
SET @preparedStatement = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
    'SELECT 1',
    'ALTER TABLE px_like_record ADD COLUMN item_name VARCHAR(255) DEFAULT NULL COMMENT ''目标名称(壁纸名)'''
  )
);
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @columnname = 'item_thumbnail';
SET @preparedStatement = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
    'SELECT 1',
    'ALTER TABLE px_like_record ADD COLUMN item_thumbnail VARCHAR(512) DEFAULT NULL COMMENT ''目标缩略图地址'''
  )
);
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 反填已有记录的冗余字段（item_name / item_thumbnail 为 NULL 的旧数据）
UPDATE px_like_record r
    INNER JOIN px_wallpaper w ON r.item_id = w.id
SET r.item_name      = w.name,
    r.item_thumbnail = w.thumbnail
WHERE r.item_name IS NULL;

-- 2. 新建下载记录表
CREATE TABLE IF NOT EXISTS `px_wallpaper_download_record` (
    `id`             BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `item_id`        BIGINT(20)   NOT NULL                COMMENT '壁纸ID',
    `item_name`      VARCHAR(255) DEFAULT NULL            COMMENT '壁纸名称',
    `item_thumbnail` VARCHAR(512) DEFAULT NULL            COMMENT '壁纸缩略图',
    `download_type`  VARCHAR(16)  DEFAULT 'single'        COMMENT '下载方式 single/zip',
    `create_by`      VARCHAR(64)  DEFAULT NULL            COMMENT '用户ID',
    `create_time`    DATETIME     DEFAULT NULL            COMMENT '下载时间',
    PRIMARY KEY (`id`),
    KEY `idx_create_by` (`create_by`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='壁纸下载记录';

-- 3. 系统参数：每用户每日下载上限（在「系统管理-参数设置」可动态调整）
INSERT INTO `sys_config`(`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
SELECT '壁纸每日下载上限', 'sys.wallpaper.download.daily.limit', '50', 'Y', 'admin', NOW(), '每用户每天最多可下载的壁纸张数'
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'sys.wallpaper.download.daily.limit');

-- 4. 系统参数：下载达上限提醒文案（纯文字，前端弹窗展示）
INSERT INTO `sys_config`(`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
SELECT '壁纸下载达上限提醒', 'sys.wallpaper.download.remind.text', '今日下载次数已达上限，明天再来吧～', 'Y', 'admin', NOW(), '下载达到每日上限时弹窗提醒的文案'
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'sys.wallpaper.download.remind.text');


