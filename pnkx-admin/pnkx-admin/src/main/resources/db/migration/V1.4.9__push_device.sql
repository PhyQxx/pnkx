-- App 推送设备登记（uniPush 2.0）
-- 客户端在 App 启动时上报 clientId，后端推送时按用户查设备
-- 推送凭据通过环境变量注入（UNIPUSH_APPID/APPKEY/MASTERSECRET），未配置时推送静默跳过
-- 2026-09-23

CREATE TABLE IF NOT EXISTS `px_push_device` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     VARCHAR(64) NOT NULL COMMENT '用户ID',
    `client_id`   VARCHAR(64) NOT NULL COMMENT 'uniPush clientId',
    `platform`    VARCHAR(20) DEFAULT NULL COMMENT '平台（android/ios）',
    `app_version` VARCHAR(20) DEFAULT NULL COMMENT '客户端版本',
    `update_time` DATETIME    DEFAULT NULL COMMENT '最近上报时间',
    `create_time` DATETIME    DEFAULT NULL COMMENT '首次登记时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_client` (`user_id`, `client_id`),
    KEY `idx_client` (`client_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'App推送设备表';
