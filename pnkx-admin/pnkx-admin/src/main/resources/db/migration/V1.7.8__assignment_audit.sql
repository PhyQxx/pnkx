CREATE TABLE px_assignment_audit (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  source_type VARCHAR(20) NOT NULL,
  source_id BIGINT NOT NULL,
  owner_id VARCHAR(64) NOT NULL,
  previous_assignee VARCHAR(64) NULL,
  assignee VARCHAR(64) NULL,
  operator_id VARCHAR(64) NOT NULL,
  action VARCHAR(32) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_assignment_source (source_type,source_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
