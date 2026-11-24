CREATE TABLE IF NOT EXISTS px_analytics_event (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 event_id CHAR(36) NOT NULL,
 visitor_id CHAR(36) NOT NULL,
 session_id CHAR(36) NOT NULL,
 page_view_id CHAR(36) NOT NULL,
 event_name VARCHAR(40) NOT NULL,
 page_key VARCHAR(40) NOT NULL,
 content_id VARCHAR(20) NULL,
 operation_id CHAR(36) NULL,
 referrer_host VARCHAR(253) NULL,
 user_id VARCHAR(64) NULL,
 duration_ms BIGINT NOT NULL DEFAULT 0,
 occurred_at DATETIME(3) NOT NULL,
 received_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 UNIQUE KEY uk_event (event_id),
 KEY idx_time (occurred_at),
 KEY idx_page_time (page_key, occurred_at),
 KEY idx_visitor_time (visitor_id, occurred_at),
 KEY idx_session_time (session_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
-- 给需要查看行为分析的运营角色授予 admin:visits:list；超级管理员默认可用。
-- 回滚时先关闭 NUXT_PUBLIC_ANALYTICS_ENABLED，不删除事实表。
