CREATE TABLE px_commemoration_preparation (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 owner_id VARCHAR(64) NOT NULL,
 source_id BIGINT NOT NULL,
 occurrence_date DATE NOT NULL,
 source_hash VARCHAR(64) NOT NULL,
 preview_token VARCHAR(36) NOT NULL,
 content VARCHAR(2000) NOT NULL,
 due_date DATE NOT NULL,
 expires_at DATETIME NOT NULL,
 status VARCHAR(24) NOT NULL DEFAULT 'draft',
 todo_id BIGINT NULL,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_preparation_occurrence(owner_id,source_id,occurrence_date),
 UNIQUE KEY uk_preparation_token(preview_token),
 UNIQUE KEY uk_preparation_todo(todo_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
