CREATE TABLE IF NOT EXISTS px_note_share (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  resource_type VARCHAR(10) NOT NULL,
  resource_id BIGINT NOT NULL,
  owner_id VARCHAR(64) NOT NULL,
  recipient_id VARCHAR(64) NULL,
  token VARCHAR(64) NULL,
  permission VARCHAR(10) NOT NULL DEFAULT 'read',
  expires_at DATETIME NULL,
  revoked TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_note_share_token (token),
  KEY idx_note_share_resource (owner_id, resource_type, resource_id, revoked),
  KEY idx_note_share_recipient (recipient_id, revoked)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
