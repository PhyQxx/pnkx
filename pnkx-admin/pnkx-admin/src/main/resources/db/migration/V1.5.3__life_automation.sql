CREATE TABLE IF NOT EXISTS `px_automation_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `client_uuid` VARCHAR(64) NOT NULL,
    `name` VARCHAR(128) NOT NULL,
    `template_code` VARCHAR(64) DEFAULT NULL,
    `trigger_type` VARCHAR(32) NOT NULL DEFAULT 'manual',
    `trigger_config` JSON DEFAULT NULL,
    `condition_json` JSON DEFAULT NULL,
    `action_type` VARCHAR(64) NOT NULL,
    `action_config` JSON DEFAULT NULL,
    `enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `version` VARCHAR(64) DEFAULT NULL,
    `last_run_time` DATETIME DEFAULT NULL,
    `next_run_time` DATETIME DEFAULT NULL,
    `del_flag` CHAR(1) NOT NULL DEFAULT '0',
    `create_by` VARCHAR(64) NOT NULL,
    `create_time` DATETIME DEFAULT NULL,
    `update_by` VARCHAR(64) DEFAULT NULL,
    `update_time` DATETIME DEFAULT NULL,
    `remark` VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_automation_client_uuid` (`client_uuid`),
    KEY `idx_automation_due` (`enabled`, `next_run_time`, `del_flag`),
    KEY `idx_automation_user` (`create_by`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生活自动化规则';

CREATE TABLE IF NOT EXISTS `px_automation_execution` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `rule_id` BIGINT NOT NULL,
    `idempotency_key` VARCHAR(128) NOT NULL,
    `status` VARCHAR(24) NOT NULL,
    `dry_run` TINYINT(1) NOT NULL DEFAULT 0,
    `input_json` JSON DEFAULT NULL,
    `plan_json` JSON DEFAULT NULL,
    `result_json` JSON DEFAULT NULL,
    `error_msg` VARCHAR(1000) DEFAULT NULL,
    `retry_of` BIGINT DEFAULT NULL,
    `start_time` DATETIME DEFAULT NULL,
    `finish_time` DATETIME DEFAULT NULL,
    `create_by` VARCHAR(64) NOT NULL,
    `create_time` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_automation_idempotency` (`idempotency_key`),
    KEY `idx_automation_execution_rule` (`rule_id`, `create_time`),
    KEY `idx_automation_execution_user` (`create_by`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生活自动化执行记录';

-- 回滚说明：先 DROP TABLE px_automation_execution，再 DROP TABLE px_automation_rule。
