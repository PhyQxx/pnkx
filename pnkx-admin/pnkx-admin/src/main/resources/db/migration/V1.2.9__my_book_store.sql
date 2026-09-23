-- ============================================================
-- V1.2.9 我的应用 / 我的书城
-- ============================================================

CREATE TABLE IF NOT EXISTS `px_book` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title` VARCHAR(200) NOT NULL COMMENT '书名',
    `author` VARCHAR(100) DEFAULT NULL COMMENT '作者',
    `description` VARCHAR(1000) DEFAULT NULL COMMENT '简介',
    `status` VARCHAR(20) DEFAULT 'reading' COMMENT '状态 reading/finished/shelved',
    `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志',
    `create_by` VARCHAR(64) NOT NULL COMMENT '创建者用户ID',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_book_user` (`create_by`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='我的书城-书籍';

CREATE TABLE IF NOT EXISTS `px_book_chapter` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `book_id` BIGINT(20) NOT NULL COMMENT '书籍ID',
    `chapter_name` VARCHAR(255) NOT NULL COMMENT '章节名',
    `chapter_no` INT(11) NOT NULL DEFAULT 1 COMMENT '章节序号',
    `content` LONGTEXT COMMENT '纯文本章节内容',
    `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志',
    `create_by` VARCHAR(64) NOT NULL COMMENT '创建者用户ID',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_chapter_book_order` (`book_id`, `chapter_no`, `id`),
    KEY `idx_chapter_user` (`create_by`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='我的书城-章节';

-- 与「生活助手」同级的一级目录
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '我的应用', 0, COALESCE((SELECT MAX(m.order_num) + 1 FROM `sys_menu` m WHERE m.parent_id = 0), 1), 'myapp', NULL, 1, 0, 'M', '0', '0', '', 'component', 'admin', NOW(), '个人应用集合'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '我的应用' AND `parent_id` = 0 AND `menu_type` = 'M');

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '我的书城', (SELECT menu_id FROM `sys_menu` WHERE `menu_name` = '我的应用' AND `parent_id` = 0 AND `menu_type` = 'M' LIMIT 1), 1, 'book', 'px/app/book/index', 1, 0, 'C', '0', '0', 'px:app:book:list', 'documentation', 'admin', NOW(), '个人书籍与章节管理'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'px:app:book:list');

-- 继承拥有「生活助手」目录权限的角色
INSERT INTO `sys_role_menu`(`role_id`, `menu_id`)
SELECT DISTINCT rm.role_id, target.menu_id
FROM `sys_role_menu` rm
JOIN `sys_menu` life ON life.menu_id = rm.menu_id AND life.menu_name = '生活助手' AND life.menu_type = 'M'
JOIN `sys_menu` target ON (target.menu_name = '我的应用' AND target.parent_id = 0) OR target.perms = 'px:app:book:list'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` assigned
    WHERE assigned.role_id = rm.role_id AND assigned.menu_id = target.menu_id
);
