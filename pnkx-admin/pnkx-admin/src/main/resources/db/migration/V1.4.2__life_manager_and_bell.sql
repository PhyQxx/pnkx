-- ============================================================
-- V1.4.2  生活管家整合家庭日历/订阅 + 提醒中心合并到右上角铃铛
-- ============================================================

-- 1. 家庭日历、订阅管理 → 合并进「生活管家」(px/life/lifeManager/index)
UPDATE `sys_menu` SET `visible` = '1' WHERE `component` IN (
  'px/life/calendar/index',
  'px/life/subscription/index'
);

-- 2. 提醒中心菜单 → 合并到右上角铃铛抽屉，菜单隐藏
UPDATE `sys_menu` SET `visible` = '1' WHERE `component` = 'px/life/reminder/index';

-- 2.1 生活报告 → 合并进「生活管家」（PC 端隐藏，移动端保留独立入口）
UPDATE `sys_menu` SET `visible` = '1' WHERE `component` = 'px/life/report/index';

-- 3. 生活管家菜单置顶（生活助手下 order 最小）
UPDATE `sys_menu` SET `order_num` = 0
WHERE `component` = 'px/life/lifeManager/index';

-- 4. 移动端：菜谱/餐饮/购物/提醒中心 合并后不再单独显示，is_app 改 0
--    （生活管家作为移动端统一入口）
UPDATE `sys_menu` SET `is_app` = '0' WHERE `component` IN (
  'px/life/recipe/index',
  'px/life/meal/index',
  'px/life/shopping/index',
  'px/life/reminder/index'
);

-- 5. 生活管家：移动端路由指向容器页（覆盖 V1.4.0 设的 /pages_life/recipe/index）
UPDATE `sys_menu` SET `app_path` = '/pages_life/lifeManager/index'
WHERE `component` = 'px/life/lifeManager/index';
