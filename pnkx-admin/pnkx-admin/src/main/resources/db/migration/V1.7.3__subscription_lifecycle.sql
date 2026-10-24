ALTER TABLE px_subscription ADD COLUMN lifecycle VARCHAR(16) NOT NULL DEFAULT 'active',
 ADD COLUMN trial_end_date DATE NULL,
 ADD COLUMN legacy_resume_policy TINYINT NOT NULL DEFAULT 1;
UPDATE px_subscription SET lifecycle=IF(enabled=1,'active','paused');
