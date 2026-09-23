CREATE TABLE IF NOT EXISTS `px_wx_subscription` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `template_type` VARCHAR(32) NOT NULL,
    `accepted` TINYINT(1) NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wx_subscription_user_type` (`user_id`, `template_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信一次性订阅授权';

CREATE TABLE IF NOT EXISTS `px_wx_message_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `template_type` VARCHAR(32) NOT NULL,
    `success` TINYINT(1) NOT NULL DEFAULT 0,
    `response_message` TEXT,
    `send_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_wx_message_user_time` (`user_id`, `send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信订阅消息发送记录';

INSERT INTO `sys_job`(`job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `remark`, `create_by`, `create_time`)
SELECT '微信生活提醒', 'LIFE', 'wxSubscribeMessageTask.sendDaily', '0 0 9 * * ?', '3', '1', '0', '每天9点发送已授权的微信订阅消息', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_job` WHERE `invoke_target` = 'wxSubscribeMessageTask.sendDaily');
