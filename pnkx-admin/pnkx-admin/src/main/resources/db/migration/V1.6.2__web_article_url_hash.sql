-- ============================================================
-- V1.6.2 网址收藏：url_hash 去重键（修复长链接前缀碰撞）+ RSS 失败计数
-- 设计文档：docs/WEB_ARTICLE_DESIGN.md
-- ============================================================

-- 1. 收藏表：url_hash 去重键
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'px_web_article' AND column_name = 'url_hash') = 0,
              'ALTER TABLE px_web_article ADD COLUMN url_hash CHAR(32) NULL COMMENT ''URL去重键(MD5)'' AFTER url', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `px_web_article` SET `url_hash` = MD5(`url`) WHERE `url_hash` IS NULL;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'px_web_article' AND column_name = 'url_hash' AND is_nullable = 'YES') = 1,
              'ALTER TABLE px_web_article MODIFY COLUMN url_hash CHAR(32) NOT NULL COMMENT ''URL去重键(MD5)''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 唯一索引：url(255) 前缀 → url_hash 全长（两个前255字符相同的长URL不再误判重复）
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'px_web_article' AND index_name = 'uk_web_article_url_user') > 0,
              'ALTER TABLE px_web_article DROP INDEX uk_web_article_url_user', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'px_web_article' AND index_name = 'uk_web_article_hash_user') = 0,
              'ALTER TABLE px_web_article ADD UNIQUE KEY uk_web_article_hash_user (url_hash, create_by)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. RSS 订阅表：同样的 url_hash 处理 + 连续失败计数
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'px_web_rss_feed' AND column_name = 'url_hash') = 0,
              'ALTER TABLE px_web_rss_feed ADD COLUMN url_hash CHAR(32) NULL COMMENT ''RSS链接去重键(MD5)'' AFTER url', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `px_web_rss_feed` SET `url_hash` = MD5(`url`) WHERE `url_hash` IS NULL;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'px_web_rss_feed' AND column_name = 'url_hash' AND is_nullable = 'YES') = 1,
              'ALTER TABLE px_web_rss_feed MODIFY COLUMN url_hash CHAR(32) NOT NULL COMMENT ''RSS链接去重键(MD5)''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'px_web_rss_feed' AND index_name = 'uk_rss_feed_url_user') > 0,
              'ALTER TABLE px_web_rss_feed DROP INDEX uk_rss_feed_url_user', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'px_web_rss_feed' AND index_name = 'uk_rss_feed_hash_user') = 0,
              'ALTER TABLE px_web_rss_feed ADD UNIQUE KEY uk_rss_feed_hash_user (url_hash, create_by)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'px_web_rss_feed' AND column_name = 'consecutive_failures') = 0,
              'ALTER TABLE px_web_rss_feed ADD COLUMN consecutive_failures INT NOT NULL DEFAULT 0 COMMENT ''连续失败次数，达3次自动停用'' AFTER fetch_error', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 回滚说明（仅人工回滚时执行）：
-- ALTER TABLE px_web_rss_feed DROP COLUMN consecutive_failures, DROP INDEX uk_rss_feed_hash_user, ADD UNIQUE KEY uk_rss_feed_url_user (url(255), create_by), DROP COLUMN url_hash;
-- ALTER TABLE px_web_article DROP INDEX uk_web_article_hash_user, ADD UNIQUE KEY uk_web_article_url_user (url(255), create_by), DROP COLUMN url_hash;
