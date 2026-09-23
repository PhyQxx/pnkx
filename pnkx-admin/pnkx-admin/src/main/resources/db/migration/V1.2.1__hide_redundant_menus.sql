-- ============================================================
-- V1.2.1 隐藏冗余菜单（对个人博客系统无价值的框架自带功能）
--
-- 背景：PNKX 是个人博客/生活管理系统，若依框架自带的「岗位管理」「部门管理」
--      以及历史遗留的「数据权限群组」「APP版本管理」对单用户场景几乎无业务价值。
--      本脚本只在前端菜单层面隐藏（visible='1' 隐藏、status='1' 停用），
--      保留后端接口与权限标识，直接通过 URL 仍可访问，确保向下兼容。
--
-- 幂等：用 component 字段精确定位菜单（component 是若依菜单中最稳定的标识），
--      重复执行不会产生副作用，菜单已隐藏时 UPDATE 命中 0 行亦无影响。
--
-- 注：若依约定 visible='1' 表示隐藏，status='1' 表示停用。
-- ============================================================

-- 1. 岗位管理（若依标准菜单，component = 'system/post/index'）
UPDATE `sys_menu`
SET `visible` = '1', `status` = '1', `update_by` = 'admin', `update_time` = NOW(),
    `remark` = CONCAT(IFNULL(`remark`, ''), ' [已隐藏：个人系统不使用岗位管理]')
WHERE `component` = 'system/post/index'
  AND `menu_type` = 'C';

-- 2. 部门管理（若依标准菜单，component = 'system/dept/index'）
UPDATE `sys_menu`
SET `visible` = '1', `status` = '1', `update_by` = 'admin', `update_time` = NOW(),
    `remark` = CONCAT(IFNULL(`remark`, ''), ' [已隐藏：个人系统不使用部门管理]')
WHERE `component` = 'system/dept/index'
  AND `menu_type` = 'C';

-- 3. 数据权限群组（V1.0.3 引入，component = 'system/dataGroup/index'）
--    主菜单与其下挂的按钮权限（F 类型，parent_id 指向主菜单）一并停用
UPDATE `sys_menu`
SET `visible` = '1', `status` = '1', `update_by` = 'admin', `update_time` = NOW(),
    `remark` = CONCAT(IFNULL(`remark`, ''), ' [已隐藏：个人系统无多用户协作场景]')
WHERE `component` = 'system/dataGroup/index'
   OR `parent_id` IN (
        SELECT menu_id FROM (
            SELECT `menu_id` FROM `sys_menu` WHERE `component` = 'system/dataGroup/index'
        ) t
   );

-- 4. APP版本管理（历史遗留，component = 'system/version/index'）
--    项目当前只有微信小程序端（版本由微信官方控制），暂无独立 APP
UPDATE `sys_menu`
SET `visible` = '1', `status` = '1', `update_by` = 'admin', `update_time` = NOW(),
    `remark` = CONCAT(IFNULL(`remark`, ''), ' [已隐藏：暂无独立APP，小程序版本由微信控制]')
WHERE `component` = 'system/version/index'
  AND `menu_type` = 'C';
