-- ============================================================
-- V1.1.5 分享资源默认备注
--   默认备注用于百度网盘类分享文案复制
-- ============================================================

ALTER TABLE `px_share_resource`
    MODIFY COLUMN `remark` VARCHAR(500) DEFAULT '复制这段内容打开「百度网盘APP 即可获取」' COMMENT '备注';

UPDATE `px_share_resource`
SET `remark` = '复制这段内容打开「百度网盘APP 即可获取」'
WHERE `remark` IS NULL
   OR `remark` = '';
