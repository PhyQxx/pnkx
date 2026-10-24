ALTER TABLE px_charge_execution ADD COLUMN next_retry_at DATETIME NULL;
CREATE INDEX ix_charge_retry ON px_charge_execution(status,next_retry_at);
