-- ============================================================
-- V1.1.9 订阅管理
--   px_subscription 订阅表（周期性订阅/账单，到期自动出账 + 续费提醒）
-- 幂等：IF NOT EXISTS；菜单按 perms 去重
-- ============================================================

CREATE TABLE IF NOT EXISTS `px_subscription` (
    `id`                BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`              VARCHAR(128)  NOT NULL                COMMENT '订阅名称（如：Netflix、iCloud）',
    `amount`            DECIMAL(12,2) NOT NULL                COMMENT '金额',
    `cycle`             VARCHAR(16)   NOT NULL                COMMENT '周期单位（daily/weekly/monthly/yearly）',
    `cycle_interval`    INT(11)       DEFAULT 1               COMMENT '周期间隔（如每2月 = monthly + 2）',
    `next_payment_date` DATE          NOT NULL                COMMENT '下次扣费日期',
    `account_id`        BIGINT(20)    DEFAULT NULL            COMMENT '关联记账账户ID',
    `classification_id` BIGINT(20)    DEFAULT NULL            COMMENT '关联记账分类ID（支出分类）',
    `payment_method`    VARCHAR(64)   DEFAULT NULL            COMMENT '支付方式备注',
    `logo`              VARCHAR(512)  DEFAULT NULL            COMMENT '订阅 Logo URL',
    `reminder_lead_days` INT(11)      DEFAULT 3               COMMENT '提前几天提醒续费',
    `enabled`           TINYINT(1)    DEFAULT 1               COMMENT '是否启用（1启用 0停用）',
    `version`           VARCHAR(64)   DEFAULT NULL            COMMENT '版本号',
    `client_uuid`       VARCHAR(64)   DEFAULT NULL            COMMENT '客户端唯一标识',
    `del_flag`          CHAR(1)       DEFAULT '0'             COMMENT '删除标志（0存在 1删除）',
    `create_by`         VARCHAR(64)   DEFAULT NULL            COMMENT '创建者',
    `create_time`       DATETIME      DEFAULT NULL            COMMENT '创建时间',
    `update_by`         VARCHAR(64)   DEFAULT NULL            COMMENT '更新者',
    `update_time`       DATETIME      DEFAULT NULL            COMMENT '更新时间',
    `remark`            VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_next_payment` (`next_payment_date`, `enabled`),
    KEY `idx_create_by` (`create_by`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='订阅管理';

-- 菜单：订阅管理（挂在「生活助手」下）
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '订阅管理', (SELECT menu_id FROM `sys_menu` WHERE `menu_name` = '生活助手' AND `menu_type` = 'M' LIMIT 1), 13, 'subscription', 'px/life/subscription/index', 1, 0, 'C', '0', '0', 'px:life:subscription:list', 'money', 'admin', NOW(), '订阅管理（周期自动出账+续费提醒）'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'px:life:subscription:list');

-- 定时任务：订阅自动出账（每天凌晨1点扫描到期订阅）
INSERT INTO `sys_job`(`job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `remark`, `create_by`, `create_time`)
SELECT '订阅自动出账', 'LIFE', 'subscriptionTask.generateDueRecords', '0 0 1 * * ?', '3', '1', '0', '扫描到期订阅并自动生成记账条目', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_job` WHERE `invoke_target` = 'subscriptionTask.generateDueRecords');

-- 给所有有「待办事项」权限的角色也分配订阅管理菜单
INSERT INTO `sys_role_menu`(`role_id`, `menu_id`)
SELECT rm.role_id, (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'px:life:subscription:list' LIMIT 1)
FROM `sys_role_menu` rm
JOIN `sys_menu` m ON rm.menu_id = m.menu_id AND m.menu_id = 2016
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm2
    WHERE rm2.role_id = rm.role_id
    AND rm2.menu_id = (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'px:life:subscription:list' LIMIT 1)
);
