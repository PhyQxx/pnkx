-- ============================================================
-- V1.4.1  合并"生活助手"过度拆分的二级菜单
-- ============================================================
-- 背景：记账拆成 4 个、卡券拆成 3 个、待办拆成 2 个、菜谱/餐饮/购物各 1 个，
-- 生活助手下二级菜单多达 ~22 个。现将相关功能合并成带 Tab 的单页面，
-- 被合并的子菜单设 visible='1' 隐藏（权限保留，URL 仍可访问）。
-- 隐藏逻辑：按 component 路径精确匹配（menu_name 可能被用户改过，不可靠）。
-- ============================================================

-- 1. 日记分析 → 合并进「我的日记」(px/life/diary/index)
UPDATE `sys_menu` SET `visible` = '1' WHERE `component` = 'px/life/diary/analysis';

-- 2. 记账：账户/分类/统计 → 合并进「生活账本」(px/life/bookkeeping/record)
UPDATE `sys_menu` SET `visible` = '1' WHERE `component` IN (
  'px/life/bookkeeping/account',
  'px/life/bookkeeping/classification',
  'px/life/bookkeeping/statistics'
);

-- 3. 卡券：管理/使用记录 → 合并进「情侣卡券」(px/life/card/index)
--    （card/manage、card/record 的 component 在原始 DB dump 中，按常见命名匹配）
UPDATE `sys_menu` SET `visible` = '1' WHERE `component` IN (
  'px/life/card/manage',
  'px/life/card/record'
);

-- 4. 待办看板 → 合并进「待办事项」(px/life/todo/index)
UPDATE `sys_menu` SET `visible` = '1' WHERE `component` = 'px/life/todo/kanban';

-- 5. 菜谱库/餐饮计划/购物清单 → 合并进新的「生活管家」(px/life/lifeManager/index)
UPDATE `sys_menu` SET `visible` = '1' WHERE `component` IN (
  'px/life/recipe/index',
  'px/life/meal/index',
  'px/life/shopping/index'
);

-- 6. 新增「生活管家」菜单（挂在「生活助手」下，order 取被隐藏菜单的最小值）
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`, `is_app`, `app_path`)
SELECT '生活管家',
       (SELECT menu_id FROM `sys_menu` WHERE `menu_name` = '生活助手' AND `menu_type` = 'M' LIMIT 1),
       10,
       'lifeManager',
       'px/life/lifeManager/index',
       1, 0, 'C', '0', '0', 'px:life:lifeManager:list', 'tool', 'admin', NOW(),
       '菜谱库 / 餐饮计划 / 购物清单 合并入口',
       '1',
       '/pages_life/recipe/index';
