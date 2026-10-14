CREATE TABLE px_workspace_lock (
    owner_id VARCHAR(64) NOT NULL PRIMARY KEY
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE px_resource_tag (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_id VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    normalized_name VARCHAR(100) NOT NULL,
    parent_id BIGINT NOT NULL DEFAULT 0,
    color VARCHAR(7) NOT NULL DEFAULT '#409EFF',
    revision BIGINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_owner_name(owner_id, normalized_name),
    KEY idx_owner_parent(owner_id, parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE px_resource_tag_rel (
    owner_id VARCHAR(64) NOT NULL,
    tag_id BIGINT NOT NULL,
    resource_type VARCHAR(24) NOT NULL,
    resource_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(owner_id, tag_id, resource_type, resource_id),
    KEY idx_resource(owner_id, resource_type, resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE px_resource_tag_backfill (
    owner_id VARCHAR(64) NOT NULL,
    resource_type VARCHAR(24) NOT NULL,
    last_id BIGINT NOT NULL DEFAULT 0,
    done TINYINT NOT NULL DEFAULT 0,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(owner_id, resource_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
