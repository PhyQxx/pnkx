-- ============================================================
-- V1.9.1 工具箱扩展：工具使用打点
--   表 sys_tool_use_log：openTool() 每次打开工具记录一条
--   接口：POST /tools/use/log/record（匿名 + IP 限流 120/h）
--         GET  /tools/use/log/stats（登录态，按工具聚合统计）
-- ============================================================

CREATE TABLE IF NOT EXISTS `sys_tool_use_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tool_id`     VARCHAR(50)  NOT NULL                COMMENT '工具标识（sys.tools.list 的 id）',
  `tool_name`   VARCHAR(100) DEFAULT NULL            COMMENT '工具名称（冗余）',
  `ip`          VARCHAR(64)  DEFAULT NULL            COMMENT '来源 IP',
  `source`      VARCHAR(10)  DEFAULT 'mp'            COMMENT '客户端来源：mp/h5/app',
  `create_time` DATETIME     DEFAULT NULL            COMMENT '使用时间',
  PRIMARY KEY (`id`),
  KEY `idx_tool_id` (`tool_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工具箱工具使用打点';
