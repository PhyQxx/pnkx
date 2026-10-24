ALTER TABLE px_subscription ADD COLUMN posting_mode VARCHAR(24) NOT NULL DEFAULT 'auto_record';
ALTER TABLE px_bookkeeping_recurring ADD COLUMN posting_mode VARCHAR(24) NOT NULL DEFAULT 'auto_record';
CREATE TABLE px_charge_instance (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 owner_id VARCHAR(64) NOT NULL,
 source_type VARCHAR(24) NOT NULL,
 source_id BIGINT NOT NULL,
 source_name VARCHAR(255) NOT NULL,
 due_date DATE NOT NULL,
 expected_amount DECIMAL(18,2) NOT NULL,
 actual_amount DECIMAL(18,2) NULL,
 account_id BIGINT NULL,
 classification_id BIGINT NULL,
 other_account_id BIGINT NULL,
 status VARCHAR(24) NOT NULL DEFAULT 'pending',
 record_id BIGINT NULL,
 revision BIGINT NOT NULL DEFAULT 0,
 request_id VARCHAR(64) NULL,
 request_hash VARCHAR(64) NULL,
 reason VARCHAR(500) NULL,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_charge_period(owner_id,source_type,source_id,due_date),
 UNIQUE KEY uk_charge_record(record_id),
 KEY ix_charge_owner_state(owner_id,status,due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
