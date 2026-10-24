-- 保留现有自动出账行为；不能从历史短月反推出原始锚点，按当前计划日回填。
ALTER TABLE px_subscription ADD COLUMN anchor_day INT NULL COMMENT '周期扣费日期锚点 1-31';
UPDATE px_subscription SET anchor_day = DAY(next_payment_date) WHERE next_payment_date IS NOT NULL;
