CREATE TABLE IF NOT EXISTS `px_life_report_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `period` VARCHAR(16) NOT NULL COMMENT '周期：week/month',
    `report_type` VARCHAR(32) NOT NULL COMMENT '报告类型',
    `source` VARCHAR(16) NOT NULL DEFAULT 'manual' COMMENT '生成来源：manual/scheduled',
    `content` LONGTEXT NOT NULL COMMENT 'Markdown报告正文',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    PRIMARY KEY (`id`),
    KEY `idx_life_report_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI生活报告历史';
