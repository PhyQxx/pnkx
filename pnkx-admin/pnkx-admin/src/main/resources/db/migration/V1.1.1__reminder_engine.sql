-- ============================================================
-- V1.1.1 统一提醒引擎
--   1. px_life_reminder      提醒配置（按实体挂载，支持 todo/commemoration/menstruation/subscription）
--   2. px_life_notification  投递日志（防重发 + 历史记录）
-- 幂等：所有建表用 IF NOT EXISTS；菜单按 perms 去重
-- ============================================================

-- 提醒配置表
CREATE TABLE IF NOT EXISTS `px_life_reminder` (
    `id`                 BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `source_type`        VARCHAR(32)  NOT NULL                COMMENT '来源类型（todo 待办 / commemoration 纪念日 / menstruation 经期 / subscription 订阅）',
    `source_id`          BIGINT(20)   NOT NULL                COMMENT '来源实体ID',
    `user_id`            VARCHAR(64)  NOT NULL                COMMENT '接收提醒的用户ID（userId）',
    `remind_time`        DATETIME     NOT NULL                COMMENT '提醒触发时间（已计算提前量后的绝对时间）',
    `lead_minutes`       INT(11)      DEFAULT 0               COMMENT '提前量（分钟），相对来源事件时间的提前分钟数',
    `enabled`            TINYINT(1)   DEFAULT 1               COMMENT '是否启用（1启用 0停用）',
    `last_triggered_time` DATETIME    DEFAULT NULL            COMMENT '上次触发时间（防重发）',
    `version`            VARCHAR(64)  DEFAULT NULL            COMMENT '版本号（离线幂等）',
    `client_uuid`        VARCHAR(64)  DEFAULT NULL            COMMENT '客户端唯一标识（离线幂等去重）',
    `del_flag`           CHAR(1)      DEFAULT '0'             COMMENT '删除标志（0存在 1删除）',
    `create_by`          VARCHAR(64)  DEFAULT NULL            COMMENT '创建者（userId）',
    `create_time`        DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`          VARCHAR(64)  DEFAULT NULL            COMMENT '更新者（userId）',
    `update_time`        DATETIME     DEFAULT NULL            COMMENT '更新时间',
    `remark`             VARCHAR(255) DEFAULT NULL            COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_due` (`enabled`, `del_flag`, `remind_time`),
    KEY `idx_source` (`source_type`, `source_id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='统一提醒配置';

-- 投递日志表（防重发 + 历史记录）
CREATE TABLE IF NOT EXISTS `px_life_notification` (
    `id`           BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `reminder_id`  BIGINT(20)   DEFAULT NULL            COMMENT '关联提醒配置ID',
    `user_id`      VARCHAR(64)  NOT NULL                COMMENT '接收用户ID',
    `channel`      VARCHAR(16)  NOT NULL                COMMENT '投递渠道（websocket / email）',
    `source_type`  VARCHAR(32)  DEFAULT NULL            COMMENT '来源类型（冗余便于查询）',
    `source_id`    BIGINT(20)   DEFAULT NULL            COMMENT '来源实体ID（冗余便于查询）',
    `title`        VARCHAR(255) NOT NULL                COMMENT '通知标题',
    `content`      TEXT                                 COMMENT '通知内容',
    `send_time`    DATETIME     NOT NULL                COMMENT '发送时间',
    `status`       CHAR(1)      DEFAULT '0'             COMMENT '状态（0 已发送 1 发送失败 2 已读）',
    `create_time`  DATETIME     DEFAULT NULL            COMMENT '记录时间',
    PRIMARY KEY (`id`),
    KEY `idx_reminder` (`reminder_id`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_send_time` (`send_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='提醒投递日志';

-- ============================================================
-- 菜单：提醒中心（挂在「生活」目录下，parent_id 动态查询 life 目录）
-- 幂等：按 perms 去重
-- ============================================================

-- 今日提醒聚合页（C 菜单，复用既有 /reminder/today 接口能力）
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '提醒中心', (SELECT menu_id FROM `sys_menu` WHERE `menu_name` = '生活助手' AND `menu_type` = 'M' LIMIT 1), 0, 'reminder', 'px/life/reminder/index', 1, 0, 'C', '0', '0', 'px:life:reminder:list', 'message', 'admin', NOW(), '统一提醒中心（待办/纪念日/经期/订阅）'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'px:life:reminder:list');

-- 按钮权限（F）
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '提醒查询', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'px:life:reminder:list' LIMIT 1), 1, '', NULL, 1, 0, 'F', '0', '0', 'px:life:reminder:query', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'px:life:reminder:query');

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '提醒新增', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'px:life:reminder:list' LIMIT 1), 2, '', NULL, 1, 0, 'F', '0', '0', 'px:life:reminder:add', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'px:life:reminder:add');

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '提醒修改', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'px:life:reminder:list' LIMIT 1), 3, '', NULL, 1, 0, 'F', '0', '0', 'px:life:reminder:edit', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'px:life:reminder:edit');

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '提醒删除', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'px:life:reminder:list' LIMIT 1), 4, '', NULL, 1, 0, 'F', '0', '0', 'px:life:reminder:remove', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'px:life:reminder:remove');

-- ============================================================
-- 参数配置：提醒调度器默认提前量与渠道开关
-- ============================================================
INSERT INTO `sys_config`(`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
SELECT '提醒-默认提前量(分钟)', 'sys.life.remind.default.lead.minutes', '60', 'Y', 'admin', NOW(), '未单独设置提前量时的默认值（分钟）'
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'sys.life.remind.default.lead.minutes');

INSERT INTO `sys_config`(`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
SELECT '提醒-WebSocket渠道开关', 'sys.life.remind.channel.websocket', 'true', 'Y', 'admin', NOW(), '是否通过 WebSocket 推送站内实时提醒'
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'sys.life.remind.channel.websocket');

INSERT INTO `sys_config`(`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
SELECT '提醒-邮件渠道开关', 'sys.life.remind.channel.email', 'true', 'Y', 'admin', NOW(), '是否通过邮件发送提醒'
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'sys.life.remind.channel.email');

INSERT INTO `sys_config`(`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
SELECT '提醒-超时后停止天数', 'sys.life.remind.stop.days.after', '3', 'Y', 'admin', NOW(), '来源事件过期多少天后停止提醒'
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'sys.life.remind.stop.days.after');

-- ============================================================
-- 定时任务：统一提醒调度（每 5 分钟扫描到期提醒并分发）
--   invokeTarget = lifeReminderTask.dispatch
--   concurrent = 1（禁止并发，防止上一轮未跑完就叠加）
--   幂等：按 invoke_target 去重
-- ============================================================
INSERT INTO `sys_job`(`job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `remark`, `create_by`, `create_time`)
SELECT '统一提醒调度', 'LIFE', 'lifeReminderTask.dispatch', '0 */5 * * * ?', '3', '1', '0', '扫描到期提醒并分发（WebSocket+邮件）', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_job` WHERE `invoke_target` = 'lifeReminderTask.dispatch');
