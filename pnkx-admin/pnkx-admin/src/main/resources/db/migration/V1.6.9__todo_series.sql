CREATE TABLE px_todo_series (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 owner_id VARCHAR(64) NOT NULL,
 title VARCHAR(200) NOT NULL,
 rule_type VARCHAR(16) NOT NULL,
 interval_days INT NOT NULL DEFAULT 1,
 week_days VARCHAR(20) NOT NULL DEFAULT '',
 month_day INT NOT NULL DEFAULT 1,
 zone_id VARCHAR(64) NOT NULL,
 local_time VARCHAR(8) NOT NULL,
 start_date VARCHAR(10) NOT NULL,
 end_date VARCHAR(10),
 next_date VARCHAR(10) NOT NULL,
 paused BOOLEAN NOT NULL DEFAULT FALSE,
 revision BIGINT NOT NULL DEFAULT 1,
 last_todo_id BIGINT,
 waiting_completion BOOLEAN NOT NULL DEFAULT FALSE,
 lead_minutes INT,
 KEY idx_todo_series_owner(owner_id,id)
);
CREATE TABLE px_todo_occurrence (
 series_id BIGINT NOT NULL,
 occurrence_key VARCHAR(10) NOT NULL,
 todo_id BIGINT NOT NULL,
 PRIMARY KEY(series_id,occurrence_key),
 UNIQUE KEY uk_todo_occurrence(todo_id)
);
