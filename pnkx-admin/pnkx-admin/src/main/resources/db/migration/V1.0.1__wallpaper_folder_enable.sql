-- ============================================================
-- V1.0.1 壁纸文件夹新增「启用」字段
--
-- 背景：文件夹管理页面支持启用/停用，停用的文件夹及其下属
-- 子文件夹、壁纸在移动端一律不展示。
--
-- 说明：Flyway 保证每个版本脚本只执行一次，故直接使用普通
-- ALTER TABLE ADD COLUMN（MySQL 不支持 ADD COLUMN IF NOT EXISTS，
-- 那是 MariaDB 专属语法）。
-- ============================================================

ALTER TABLE `px_wallpaper_folder`
    ADD COLUMN `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用（0否 1是）';
