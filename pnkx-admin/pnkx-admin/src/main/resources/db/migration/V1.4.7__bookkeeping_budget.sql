-- 记账预算：月度总预算（type_id=0）与分类预算（type_id=二级分类ID）
-- 状态汇总（已用/剩余/超支）由后端按当月支出实时计算，不落冗余字段
-- 注意：总预算用 type_id=0 而非 NULL，保证 (month, type_id, create_by) 唯一键对总预算同样生效
-- 2026-09-23

CREATE TABLE IF NOT EXISTS `px_bookkeeping_budget` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `version`     VARCHAR(20)  DEFAULT NULL COMMENT '版本号',
    `month`       CHAR(7)      NOT NULL COMMENT '预算归属月 yyyy-MM',
    `type_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '分类ID（0=月度总预算，否则为二级分类ID）',
    `amount`      DECIMAL(12, 2) NOT NULL COMMENT '预算金额',
    `del_flag`    TINYINT      DEFAULT 0 COMMENT '删除标志（0存在 1删除）',
    `create_by`   VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
    `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_month_type_user` (`month`, `type_id`, `create_by`),
    KEY `idx_month_user` (`month`, `create_by`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '记账预算表';
