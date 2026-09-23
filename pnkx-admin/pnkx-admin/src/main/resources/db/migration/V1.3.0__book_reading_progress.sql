-- 我的书城阅读进度：每本书记录当前用户最后打开的有效章节。
ALTER TABLE `px_book`
    ADD COLUMN `last_read_chapter_id` BIGINT(20) DEFAULT NULL COMMENT '最后阅读章节ID' AFTER `status`,
    ADD COLUMN `last_read_time` DATETIME DEFAULT NULL COMMENT '最后阅读时间' AFTER `last_read_chapter_id`;

CREATE INDEX `idx_book_last_read_chapter` ON `px_book` (`last_read_chapter_id`);
