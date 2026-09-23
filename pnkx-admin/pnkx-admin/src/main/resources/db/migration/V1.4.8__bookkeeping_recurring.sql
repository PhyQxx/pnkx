-- 周期记账：固定支出（房租/会员等）按规则自动生成记账记录
-- frequency: month=每月固定日（day_number 1-28），week=每周星期几（day_number 1-7，周一=1）
-- 定时任务每日扫描 next_run_date 到期的规则生成记录并推进下次执行日
-- 2026-09-23

CREATE TABLE IF NOT EXISTS `px_bookkeeping_recurring` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `version`        VARCHAR(20)   DEFAULT NULL COMMENT '版本号',
    `name`           VARCHAR(100)  NOT NULL COMMENT '规则名称（如：房租）',
    `frequency`      VARCHAR(10)   NOT NULL COMMENT '频率（month 每月 / week 每周）',
    `day_number`     INT           NOT NULL COMMENT '月频率为几号(1-28)；周频率为周几(1-7，周一=1)',
    `type_difference` CHAR(1)      NOT NULL DEFAULT '1' COMMENT '收支类型（0收入 1支出 2转账）',
    `type`           BIGINT        DEFAULT 0 COMMENT '分类ID',
    `account`        BIGINT        DEFAULT NULL COMMENT '账户ID',
    `other_account`  BIGINT        DEFAULT NULL COMMENT '转入账户ID（转账时）',
    `money`          DECIMAL(12, 2) NOT NULL COMMENT '金额',
    `remark`         VARCHAR(255)  DEFAULT NULL COMMENT '备注（生成的记录会追加周期标记）',
    `next_run_date`  DATE          NOT NULL COMMENT '下次执行日',
    `enabled`        TINYINT       DEFAULT 1 COMMENT '是否启用（1启用 0停用）',
    `last_run_date`  DATE          DEFAULT NULL COMMENT '上次执行日',
    `del_flag`       TINYINT       DEFAULT 0 COMMENT '删除标志',
    `create_by`      VARCHAR(64)   DEFAULT '' COMMENT '创建者（生成的记账记录归属该用户）',
    `create_time`    DATETIME      DEFAULT NULL COMMENT '创建时间',
    `update_by`      VARCHAR(64)   DEFAULT '' COMMENT '更新者',
    `update_time`    DATETIME      DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_next_run` (`enabled`, `next_run_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '周期记账规则表';

-- 定时任务：每日 08:30 扫描到期规则生成记账记录（错过（停机等）由 misfire 策略补跑）
INSERT INTO `sys_job`(`job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `remark`, `create_by`, `create_time`)
SELECT '周期记账生成', 'LIFE', 'bookkeepingRecurringTask.execute', '0 30 8 * * ?', '3', '1', '0', '每日按周期规则自动生成记账记录', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_job` WHERE `invoke_target` = 'bookkeepingRecurringTask.execute');
