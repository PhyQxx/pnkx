-- ============================================================
-- V1.9.2 工具使用统计：后台管理菜单（C 类，动态路由 component 指向 pnkx-ui 视图）
--   菜单：工具使用统计（顶层），perms = tools:useLog:list
--   按 perms 去重，不依赖具体 menu_id
-- ============================================================

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '工具使用统计', 0, 101, 'toolUseLog', 'px/life/toolUseLog/index', 1, 0, 'C', '0', '0', 'tools:useLog:list', 'chart', 'admin', NOW(), '工具箱访问记录与次数统计'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'tools:useLog:list');
