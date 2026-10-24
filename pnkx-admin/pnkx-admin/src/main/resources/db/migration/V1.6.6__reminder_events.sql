CREATE TABLE px_reminder_event (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 reminder_id BIGINT NOT NULL,
 user_id VARCHAR(64) NOT NULL,
 occurrence_at DATETIME NOT NULL,
 source_type VARCHAR(32) NOT NULL,
 source_id BIGINT NOT NULL,
 title VARCHAR(255) NOT NULL,
 state VARCHAR(24) NOT NULL DEFAULT 'open',
 read_at DATETIME NULL,
 snooze_until DATETIME NULL,
 revision BIGINT NOT NULL DEFAULT 0,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_reminder_occurrence(reminder_id,user_id,occurrence_at),
 KEY ix_reminder_snooze(state,snooze_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE px_life_notification ADD COLUMN event_id BIGINT NULL;
