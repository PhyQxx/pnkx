-- ============================================================
-- V1.1.7 统一家庭日历
--   菜单：日历（挂在「生活助手」目录下）
-- 幂等：按 perms 去重
-- ============================================================

-- 统一日历页面（C 菜单，挂到「生活助手」下）
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '家庭日历', (SELECT menu_id FROM `sys_menu` WHERE `menu_name` = '生活助手' AND `menu_type` = 'M' LIMIT 1), 0, 'familyCalendar', 'px/life/calendar/index', 1, 0, 'C', '0', '0', 'px:life:calendar:list', 'calendar', 'admin', NOW(), '统一日历（待办/纪念日/经期/记账聚合）'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'px:life:calendar:list');

-- 给所有有待办事项权限的角色也分配日历菜单
INSERT INTO `sys_role_menu`(`role_id`, `menu_id`)
SELECT rm.role_id, (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'px:life:calendar:list' LIMIT 1)
FROM `sys_role_menu` rm
JOIN `sys_menu` m ON rm.menu_id = m.menu_id
WHERE m.menu_id = 2016
AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm2
    WHERE rm2.role_id = rm.role_id
    AND rm2.menu_id = (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'px:life:calendar:list' LIMIT 1)
);
