-- ============================================================
-- V1.4.0  菜单新增「移动端路由」字段 + 回填生活助手默认路由
-- ============================================================
-- 背景：sys_menu.path/component 是 Web 后台路由，与小程序 pages_life 路径不一致。
-- 新增 app_path 列专门记录小程序路由，为空时移动端按 path 推导（兼容旧菜单）。
-- 同时给小程序已有页面的生活菜单回填 app_path，并把新建的 4 个功能设为 App 菜单。
-- ============================================================

-- 1. 新增 app_path 列（VARCHAR，可空）
ALTER TABLE `sys_menu` ADD COLUMN `app_path` VARCHAR(255) NULL COMMENT '移动端路由（小程序专用，如 /pages_life/reminder/index）' AFTER `is_app`;

-- 2. 回填已有生活菜单的默认移动端路由（按 menu_name 匹配，幂等）
UPDATE `sys_menu` SET `app_path` = '/pages_life/bookkeeping/index'           WHERE `menu_name` = '生活账本' AND `app_path` IS NULL;
UPDATE `sys_menu` SET `app_path` = '/pages_life/diary/index'                 WHERE `menu_name` = '我的日记' AND `app_path` IS NULL;
UPDATE `sys_menu` SET `app_path` = '/pages_life/todo/index'                  WHERE `menu_name` = '待办事项' AND `app_path` IS NULL;
UPDATE `sys_menu` SET `app_path` = '/pages_life/note/index'                  WHERE `menu_name` = '我的笔记' AND `app_path` IS NULL;
UPDATE `sys_menu` SET `app_path` = '/pages_life/commemorationDay/index'      WHERE `menu_name` = '纪念日'   AND `app_path` IS NULL;
UPDATE `sys_menu` SET `app_path` = '/pages_life/menstruationAssistant/index' WHERE `menu_name` = '姨妈助手' AND `app_path` IS NULL;
UPDATE `sys_menu` SET `app_path` = '/pages_life/card/index'                  WHERE `menu_name` = '情侣卡券' AND `app_path` IS NULL;
UPDATE `sys_menu` SET `app_path` = '/pages_life/report/index'                WHERE `menu_name` = '生活报告' AND `app_path` IS NULL;
UPDATE `sys_menu` SET `app_path` = '/pages_life/book/index'                  WHERE `menu_name` = '我的书城' AND `app_path` IS NULL;
UPDATE `sys_menu` SET `app_path` = '/pages_life/ai/chat'                     WHERE `menu_name` = 'AI助手'   AND `app_path` IS NULL;

-- 3. 新建的 4 个生活功能菜单：回填 app_path 并开启 App 菜单（之前 is_app 默认 NULL，小程序看不到）
UPDATE `sys_menu` SET `app_path` = '/pages_life/reminder/index',     `is_app` = '1' WHERE `menu_name` = '提醒中心' AND `is_app` IS NULL;
UPDATE `sys_menu` SET `app_path` = '/pages_life/recipe/index',       `is_app` = '1' WHERE `menu_name` = '菜谱库'   AND `is_app` IS NULL;
UPDATE `sys_menu` SET `app_path` = '/pages_life/mealPlan/index',     `is_app` = '1' WHERE `menu_name` = '餐饮计划' AND `is_app` IS NULL;
UPDATE `sys_menu` SET `app_path` = '/pages_life/shoppingList/index', `is_app` = '1' WHERE `menu_name` = '购物清单' AND `is_app` IS NULL;
