-- ============================================================
-- V1.0.3 数据权限：多对多群组
--   1. px_data_group       数据群组（家庭组/项目组等）
--   2. px_data_group_member 群组成员（多对多，成员间数据互看）
-- 幂等：所有建表用 IF NOT EXISTS
-- ============================================================

-- 数据群组
CREATE TABLE IF NOT EXISTS `px_data_group` (
    `id`          BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '主键',
    `group_name`  VARCHAR(64)   NOT NULL                COMMENT '群组名称',
    `group_code`  VARCHAR(64)   DEFAULT NULL            COMMENT '群组编码',
    `status`      CHAR(1)       DEFAULT '0'             COMMENT '状态（0正常 1停用）',
    `remark`      VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
    `create_by`   VARCHAR(64)   DEFAULT NULL            COMMENT '创建者(userId)',
    `create_time` DATETIME      DEFAULT NULL            COMMENT '创建时间',
    `update_by`   VARCHAR(64)   DEFAULT NULL            COMMENT '更新者(userId)',
    `update_time` DATETIME      DEFAULT NULL            COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_code` (`group_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='数据权限群组';

-- 群组成员
CREATE TABLE IF NOT EXISTS `px_data_group_member` (
    `id`         BIGINT(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `group_id`   BIGINT(20)  NOT NULL                COMMENT '群组ID',
    `user_id`    BIGINT(20)  NOT NULL                COMMENT '用户ID',
    `create_time` DATETIME   DEFAULT NULL            COMMENT '加入时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='数据权限群组成员';

-- ============================================================
-- 菜单：数据权限群组（挂在「系统管理」目录下，parent_id=1）
--   按 perms 去重，幂等
-- ============================================================

-- 数据权限群组（C 菜单）
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '数据权限群组', 1, 99, 'dataGroup', 'system/dataGroup/index', 1, 0, 'C', '0', '0', 'system:dataGroup:list', 'peoples', 'admin', NOW(), '数据权限群组管理（成员间数据互见）'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:dataGroup:list');

-- 数据权限群组按钮（F）
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '群组查询', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'system:dataGroup:list' LIMIT 1), 1, '', NULL, 1, 0, 'F', '0', '0', 'system:dataGroup:query', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:dataGroup:query');
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '群组新增', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'system:dataGroup:list' LIMIT 1), 2, '', NULL, 1, 0, 'F', '0', '0', 'system:dataGroup:add', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:dataGroup:add');
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '群组修改', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'system:dataGroup:list' LIMIT 1), 3, '', NULL, 1, 0, 'F', '0', '0', 'system:dataGroup:edit', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:dataGroup:edit');
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '群组删除', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'system:dataGroup:list' LIMIT 1), 4, '', NULL, 1, 0, 'F', '0', '0', 'system:dataGroup:remove', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:dataGroup:remove');

