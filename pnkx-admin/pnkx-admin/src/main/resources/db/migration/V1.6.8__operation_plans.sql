CREATE TABLE px_operation_plan (
 id VARCHAR(64) PRIMARY KEY, owner_id VARCHAR(64) NOT NULL, title VARCHAR(255) NOT NULL,
 status VARCHAR(32) NOT NULL DEFAULT 'awaiting_confirmation', expires_at DATETIME NOT NULL,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, revision BIGINT NOT NULL DEFAULT 0,
 KEY ix_plan_owner(owner_id,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE px_operation_step (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,plan_id VARCHAR(64) NOT NULL,step_number INT NOT NULL,
 tool_code VARCHAR(32) NOT NULL,input_json LONGTEXT NOT NULL,status VARCHAR(32) NOT NULL DEFAULT 'pending',
 result_json LONGTEXT NULL,error_msg VARCHAR(500) NULL,
 UNIQUE KEY uk_plan_step(plan_id,step_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
