CREATE TABLE px_action_receipt (
 owner_id VARCHAR(64) NOT NULL,
 request_id VARCHAR(64) NOT NULL,
 request_hash CHAR(64) NOT NULL,
 result_json LONGTEXT NOT NULL,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(owner_id,request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
