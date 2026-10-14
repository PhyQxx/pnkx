CREATE TABLE px_user_file (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 owner_id VARCHAR(64) NOT NULL,
 parent_id BIGINT NOT NULL DEFAULT 0,
 display_name VARCHAR(255) NOT NULL,
 is_folder BOOLEAN NOT NULL DEFAULT FALSE,
 object_key VARCHAR(36),
 mime VARCHAR(100),
 byte_size BIGINT NOT NULL DEFAULT 0,
 content_hash CHAR(64),
 revision BIGINT NOT NULL DEFAULT 1,
 del_flag INT NOT NULL DEFAULT 0,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_private_file_object(object_key),
 KEY idx_private_file_owner(owner_id,del_flag,parent_id,id)
);
