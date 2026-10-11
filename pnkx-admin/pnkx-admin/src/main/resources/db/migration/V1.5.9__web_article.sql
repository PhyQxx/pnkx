-- ============================================================
-- V1.5.9 网址收藏 + AI 整理（px_web_article）
-- 设计文档：docs/WEB_ARTICLE_DESIGN.md
-- ============================================================

CREATE TABLE IF NOT EXISTS `px_web_article` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `url` VARCHAR(1024) NOT NULL COMMENT '归一化后的原文链接',
    `domain` VARCHAR(255) DEFAULT NULL COMMENT '站点域名',
    `title` VARCHAR(512) DEFAULT NULL COMMENT '标题',
    `author` VARCHAR(255) DEFAULT NULL COMMENT '作者',
    `publish_time` DATETIME DEFAULT NULL COMMENT '原文发布时间',
    `cover_image` VARCHAR(1024) DEFAULT NULL COMMENT '封面图',
    `favicon` VARCHAR(1024) DEFAULT NULL COMMENT '站点图标',
    `content_text` LONGTEXT COMMENT '正文纯文本',
    `content_html` LONGTEXT COMMENT 'XSS清理后的正文HTML快照',
    `summary` VARCHAR(2048) DEFAULT NULL COMMENT 'AI摘要',
    `tags` VARCHAR(512) DEFAULT NULL COMMENT '标签，逗号分隔',
    `category` VARCHAR(64) DEFAULT NULL COMMENT 'AI分类',
    `fetch_status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '抓取状态：0待抓取 1成功 2失败',
    `fetch_error` VARCHAR(1024) DEFAULT NULL COMMENT '抓取失败原因',
    `ai_status` CHAR(1) NOT NULL DEFAULT '0' COMMENT 'AI分析状态：0待分析 1成功 2失败',
    `ai_error` VARCHAR(1024) DEFAULT NULL COMMENT 'AI分析失败原因',
    `ai_model` VARCHAR(128) DEFAULT NULL COMMENT '分析所用模型',
    `read_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '已读标记：0未读 1已读',
    `star_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '星标：0否 1是',
    `source` CHAR(1) NOT NULL DEFAULT '1' COMMENT '来源：1手动添加 2AI对话 3书签脚本',
    `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0存在 1删除',
    `create_by` VARCHAR(64) NOT NULL COMMENT '创建者用户ID',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_web_article_url_user` (`url`(255), `create_by`),
    KEY `idx_web_article_user` (`create_by`, `del_flag`, `create_time`),
    KEY `idx_web_article_category` (`category`),
    KEY `idx_web_article_domain` (`domain`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网址收藏-AI整理';

-- 「我的应用」目录下的「网址收藏」菜单（与「我的书城」同级）
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '网址收藏', (SELECT menu_id FROM `sys_menu` WHERE `menu_name` = '我的应用' AND `parent_id` = 0 AND `menu_type` = 'M' LIMIT 1), 2, 'webArticle', 'px/app/webArticle/index', 1, 0, 'C', '0', '0', 'px:app:webArticle:list', 'link', 'admin', NOW(), '网址剪藏与AI整理'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'px:app:webArticle:list');

-- 继承拥有「我的应用」目录权限的角色
INSERT INTO `sys_role_menu`(`role_id`, `menu_id`)
SELECT DISTINCT rm.role_id, target.menu_id
FROM `sys_role_menu` rm
JOIN `sys_menu` app ON app.menu_id = rm.menu_id AND app.menu_name = '我的应用' AND app.parent_id = 0 AND app.menu_type = 'M'
JOIN `sys_menu` target ON target.perms = 'px:app:webArticle:list'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` assigned
    WHERE assigned.role_id = rm.role_id AND assigned.menu_id = target.menu_id
);

-- 回滚说明：DELETE FROM sys_role_menu WHERE menu_id IN (SELECT menu_id FROM sys_menu WHERE perms = 'px:app:webArticle:list');
--           DELETE FROM sys_menu WHERE perms = 'px:app:webArticle:list';
--           DROP TABLE px_web_article;
