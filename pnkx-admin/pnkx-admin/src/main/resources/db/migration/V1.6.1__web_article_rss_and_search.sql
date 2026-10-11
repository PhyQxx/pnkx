-- ============================================================
-- V1.6.1 网址收藏三期：RSS 订阅自动收藏 + App 菜单 + 全文搜索
-- 设计文档：docs/WEB_ARTICLE_DESIGN.md 第 7 节
-- ============================================================

-- 1. RSS 订阅源表
CREATE TABLE IF NOT EXISTS `px_web_rss_feed` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` VARCHAR(128) NOT NULL COMMENT '订阅名称',
    `url` VARCHAR(1024) NOT NULL COMMENT 'RSS/Atom 链接',
    `enabled` CHAR(1) NOT NULL DEFAULT '1' COMMENT '是否启用：0停用 1启用',
    `last_fetch_time` DATETIME DEFAULT NULL COMMENT '最近抓取时间',
    `last_fetch_count` INT NOT NULL DEFAULT 0 COMMENT '最近一轮新收录条数',
    `fetch_error` VARCHAR(1024) DEFAULT NULL COMMENT '最近抓取失败原因',
    `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0存在 1删除',
    `create_by` VARCHAR(64) NOT NULL COMMENT '创建者用户ID',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rss_feed_url_user` (`url`(255), `create_by`),
    KEY `idx_rss_feed_user` (`create_by`, `del_flag`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网址收藏-RSS订阅源';

-- 2. 收藏内容全文索引（ngram 支持中文分词，标题+摘要+正文联合检索）
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'px_web_article' AND index_name = 'ft_web_article_search') = 0,
              'ALTER TABLE px_web_article ADD FULLTEXT INDEX ft_web_article_search (title, summary, content_text) WITH PARSER ngram', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. App 端菜单：挂在「生活助手」目录下（Web 端已有「我的应用」入口，故 visible 隐藏）
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `app_path`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `is_app`, `create_by`, `create_time`, `remark`)
SELECT '网址收藏', (SELECT menu_id FROM `sys_menu` WHERE `menu_name` = '生活助手' AND `menu_type` = 'M' LIMIT 1), 99, 'webArticleApp', 'px/app/webArticle/index', '/pages_life/webArticle/index', 1, 0, 'C', '1', '0', '', 'link', '1', 'admin', NOW(), '网址剪藏与AI整理（App端）'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `app_path` = '/pages_life/webArticle/index');

-- 4. 角色授权：拥有「生活助手」目录的角色获得 App 菜单
INSERT INTO `sys_role_menu`(`role_id`, `menu_id`)
SELECT DISTINCT rm.role_id, target.menu_id
FROM `sys_role_menu` rm
JOIN `sys_menu` life ON life.menu_id = rm.menu_id AND life.menu_name = '生活助手' AND life.menu_type = 'M'
JOIN `sys_menu` target ON target.app_path = '/pages_life/webArticle/index'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` assigned
    WHERE assigned.role_id = rm.role_id AND assigned.menu_id = target.menu_id
);

-- 5. RSS 定时抓取任务：每 30 分钟拉取启用的订阅源
INSERT INTO `sys_job`(`job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `remark`, `create_by`, `create_time`)
SELECT 'RSS订阅自动收藏', 'LIFE', 'rssFetchTask.execute', '0 */30 * * * ?', '3', '1', '0', '定时拉取启用的RSS订阅源，新文章自动收藏并AI整理', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_job` WHERE `invoke_target` = 'rssFetchTask.execute');

-- 回滚说明（仅人工回滚时执行）：
-- DELETE FROM sys_job WHERE invoke_target = 'rssFetchTask.execute';
-- DELETE FROM sys_role_menu WHERE menu_id IN (SELECT menu_id FROM sys_menu WHERE app_path = '/pages_life/webArticle/index');
-- DELETE FROM sys_menu WHERE app_path = '/pages_life/webArticle/index';
-- ALTER TABLE px_web_article DROP INDEX ft_web_article_search;
-- DROP TABLE px_web_rss_feed;
