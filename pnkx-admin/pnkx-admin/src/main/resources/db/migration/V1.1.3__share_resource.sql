-- ============================================================
-- V1.1.3 分享资源
--   1. 新建分享资源表 px_share_resource
--   2. 新增后台「分享资源」菜单和按钮权限
--   3. 插入百度网盘示例数据
-- 幂等：建表和初始化数据均可重复执行
-- ============================================================

CREATE TABLE IF NOT EXISTS `px_share_resource`
(
    `id`            BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`         VARCHAR(100) NOT NULL COMMENT '标题',
    `disk_type`     VARCHAR(50)  DEFAULT NULL COMMENT '云盘',
    `resource_type` VARCHAR(50)  DEFAULT NULL COMMENT '类型',
    `share_url`     VARCHAR(500) NOT NULL COMMENT '分享链接',
    `extract_code`  VARCHAR(50)  DEFAULT NULL COMMENT '提取码',
    `qr_code`       VARCHAR(500) DEFAULT NULL COMMENT '二维码',
    `tags`          VARCHAR(200) DEFAULT NULL COMMENT '标签，英文逗号分隔',
    `sort_order`    INT(11)      DEFAULT 0 COMMENT '排序',
    `status`        CHAR(1)      DEFAULT '1' COMMENT '状态（0停用 1启用）',
    `del_flag`      TINYINT(1)   DEFAULT 0 COMMENT '删除标志（0存在 1删除）',
    `version`       VARCHAR(20)  DEFAULT NULL COMMENT '版本号',
    `create_by`     VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`   DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_by`     VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`   DATETIME     DEFAULT NULL COMMENT '更新时间',
    `remark`        VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_disk_type` (`disk_type`),
    KEY `idx_resource_type` (`resource_type`),
    KEY `idx_status_sort` (`status`, `sort_order`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='分享资源';

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '分享资源', COALESCE((SELECT menu_id FROM `sys_menu` WHERE `menu_name` IN ('博客管理', '博客') AND `menu_type` = 'M' ORDER BY menu_id LIMIT 1), 0),
       90, 'share', 'px/blog/share/index', 1, 0, 'C', '0', '0', 'px:share:list', 'share', 'admin', NOW(), '网盘分享资源维护'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'px:share:list');

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '分享资源查询', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'px:share:list' LIMIT 1), 1, '', NULL, 1, 0, 'F', '0', '0', 'px:share:query', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'px:share:query');

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '分享资源新增', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'px:share:list' LIMIT 1), 2, '', NULL, 1, 0, 'F', '0', '0', 'px:share:add', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'px:share:add');

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '分享资源修改', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'px:share:list' LIMIT 1), 3, '', NULL, 1, 0, 'F', '0', '0', 'px:share:edit', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'px:share:edit');

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '分享资源删除', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'px:share:list' LIMIT 1), 4, '', NULL, 1, 0, 'F', '0', '0', 'px:share:remove', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'px:share:remove');

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '分享资源导出', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'px:share:list' LIMIT 1), 5, '', NULL, 1, 0, 'F', '0', '0', 'px:share:export', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'px:share:export');

INSERT IGNORE INTO `sys_role_menu`(`role_id`, `menu_id`)
SELECT 1, m.menu_id
FROM `sys_menu` m
WHERE m.perms IN ('px:share:list', 'px:share:query', 'px:share:add', 'px:share:edit', 'px:share:remove', 'px:share:export');
