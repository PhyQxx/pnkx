-- 在001建表后执行一次。已有一期数据库仅需此增量；全新数据库依次执行001、002。
ALTER TABLE px_analytics_event ADD COLUMN target_id VARCHAR(20) NULL AFTER content_id;
