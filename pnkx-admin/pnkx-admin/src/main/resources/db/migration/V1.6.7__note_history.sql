CREATE TABLE px_note_history (
    note_id BIGINT NOT NULL,
    owner_id VARCHAR(64) NOT NULL,
    revision BIGINT NOT NULL,
    title VARCHAR(255),
    content LONGTEXT,
    rich_text LONGTEXT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (note_id, revision),
    KEY idx_note_history_owner (owner_id, note_id, revision)
);
