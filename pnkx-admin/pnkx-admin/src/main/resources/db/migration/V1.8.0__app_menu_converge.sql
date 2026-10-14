-- ============================================================
-- V1.8.0  生活助手宫格收敛：高频四宫格 + 生活管家唯一聚合入口
-- ============================================================
-- 背景（PRODUCT_UIUE_REDESIGN.md §2.3/§3.2）：移动端一级入口收敛，
-- 生活 Tab 只平铺高频四项（账本/日历/待办/日记），其余功能统一收进生活管家二级页。
-- AI助手宫格暂保留（全局 AI 悬浮球落地后再移除）。
-- 幂等：全部带条件守护，重复执行无副作用。
-- ============================================================

-- 1. 高频四宫格：家庭日历升为一级（此前 V1.4.2 将其收入生活管家，App 端无独立入口）
UPDATE `sys_menu` SET `is_app` = '1', `app_path` = '/pages_life/calendar/index'
WHERE `component` = 'px/life/calendar/index' AND (`is_app` IS NULL OR `is_app` = '0' OR `app_path` IS NULL);

-- 2. 收进生活管家的功能：一级宫格移除（is_app=0），PC 端菜单不受影响
--    我的笔记 → 已由「资料」一级 Tab 承载
UPDATE `sys_menu` SET `is_app` = '0' WHERE `menu_name` = '我的笔记' AND `is_app` = '1';
--    纪念日 / 姨妈助手 / 情侣卡券 / 生活报告 / 我的书城 → 生活管家分组内可达
UPDATE `sys_menu` SET `is_app` = '0' WHERE `menu_name` = '纪念日'   AND `is_app` = '1';
UPDATE `sys_menu` SET `is_app` = '0' WHERE `menu_name` = '姨妈助手' AND `is_app` = '1';
UPDATE `sys_menu` SET `is_app` = '0' WHERE `menu_name` = '情侣卡券' AND `is_app` = '1';
UPDATE `sys_menu` SET `is_app` = '0' WHERE `menu_name` = '生活报告' AND `is_app` = '1';
UPDATE `sys_menu` SET `is_app` = '0' WHERE `menu_name` = '我的书城' AND `is_app` = '1';

-- 3. 生活管家保持唯一聚合入口并置顶（V1.4.2 已设 order_num=0，此处兜底）
UPDATE `sys_menu` SET `is_app` = '1', `order_num` = 0, `app_path` = '/pages_life/lifeManager/index'
WHERE `component` = 'px/life/lifeManager/index' AND `is_app` <> '1';
