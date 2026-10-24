CREATE TABLE px_charge_execution (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,owner_id VARCHAR(64) NOT NULL,
 source_type VARCHAR(24) NOT NULL,source_id BIGINT NOT NULL,due_date DATE NOT NULL,
 status VARCHAR(24) NOT NULL DEFAULT 'failed',error_code VARCHAR(64) NOT NULL DEFAULT 'GENERATION_FAILED',
 attempt_count INT NOT NULL DEFAULT 1,update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_charge_execution(owner_id,source_type,source_id,due_date),KEY ix_charge_execution_owner(owner_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
