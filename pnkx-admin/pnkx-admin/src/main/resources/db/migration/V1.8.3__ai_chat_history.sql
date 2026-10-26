-- AI 聊天历史：按用户和会话保存消息，服务端不参与模型上下文组装，
-- 仅在客户端请求时返回本人消息。plan_id 指向操作计划，可为空或已过期。
CREATE TABLE px_ai_chat_message (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 owner_id VARCHAR(64) NOT NULL,
 session_id VARCHAR(64) NOT NULL,
 role VARCHAR(16) NOT NULL,
 content MEDIUMTEXT NOT NULL,
 plan_id VARCHAR(64) NULL,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY ix_chat_owner_session (owner_id, session_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
