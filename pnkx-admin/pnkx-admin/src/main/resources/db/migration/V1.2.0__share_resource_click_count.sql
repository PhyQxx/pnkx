-- ============================================================
-- V1.2.0 分享资源点击次数
--   1. px_share_resource 表新增 click_count 字段
--
-- 说明：MySQL 5.7/8.0 均不支持 MariaDB 风格的
-- ALTER TABLE ... ADD COLUMN IF NOT EXISTS。Flyway 会保证该版本
-- 只成功执行一次，因此这里使用标准 ADD COLUMN 语法。
-- ============================================================

ALTER TABLE `px_share_resource`
    ADD COLUMN `click_count` INT(11) DEFAULT 0 COMMENT '客户端点击次数';
