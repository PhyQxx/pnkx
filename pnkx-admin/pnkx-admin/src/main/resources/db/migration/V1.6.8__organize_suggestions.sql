CREATE TABLE px_organize_suggestion (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 owner_id VARCHAR(64) NOT NULL,
 resource_type VARCHAR(24) NOT NULL,
 resource_id BIGINT NOT NULL,
 rule_code VARCHAR(32) NOT NULL,
 fingerprint CHAR(64) NOT NULL,
 title VARCHAR(255),
 reason VARCHAR(500) NOT NULL,
 proposed_title VARCHAR(200),
 status VARCHAR(16) NOT NULL DEFAULT 'ready',
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_organize_suggestion(owner_id,resource_type,resource_id,rule_code,fingerprint),
 KEY idx_organize_owner_status(owner_id,status,id)
);
