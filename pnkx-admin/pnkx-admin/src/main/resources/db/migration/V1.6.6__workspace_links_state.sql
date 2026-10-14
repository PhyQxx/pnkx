CREATE TABLE px_resource_link (
    owner_id VARCHAR(64) NOT NULL,
    source_type VARCHAR(24) NOT NULL,
    source_id BIGINT NOT NULL,
    target_type VARCHAR(24) NOT NULL,
    target_id BIGINT NOT NULL,
    relation_type VARCHAR(24) NOT NULL DEFAULT 'related',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(owner_id,source_type,source_id,target_type,target_id,relation_type),
    KEY idx_target(target_type,target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE px_resource_state (
    user_id VARCHAR(64) NOT NULL,
    resource_type VARCHAR(24) NOT NULL,
    resource_id BIGINT NOT NULL,
    inbox TINYINT NOT NULL DEFAULT 0,
    last_opened_at DATETIME DEFAULT NULL,
    PRIMARY KEY(user_id,resource_type,resource_id),
    KEY idx_recent(user_id,last_opened_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE px_resource_action (
    owner_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    source_type VARCHAR(24) NOT NULL,
    source_id BIGINT NOT NULL,
    target_type VARCHAR(24) NOT NULL,
    target_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(owner_id,request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
