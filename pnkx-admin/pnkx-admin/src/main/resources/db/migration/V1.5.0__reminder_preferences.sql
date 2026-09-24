CREATE TABLE IF NOT EXISTS `px_reminder_preference` (
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `websocket_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '站内实时提醒',
    `email_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '邮件提醒',
    `push_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'App推送',
    `quiet_start` TIME DEFAULT '23:00:00' COMMENT '免打扰开始',
    `quiet_end` TIME DEFAULT '07:00:00' COMMENT '免打扰结束',
    `update_time` DATETIME DEFAULT NULL,
    PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户提醒偏好';
