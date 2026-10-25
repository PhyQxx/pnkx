-- 保存草稿替换关系与请求回执，不自动执行后继计划。
ALTER TABLE px_operation_plan
 ADD COLUMN replacement_id VARCHAR(36) NULL,
 ADD COLUMN revision_request_json LONGTEXT NULL;
