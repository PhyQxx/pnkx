-- 执行租约用于服务中断恢复；每步持有计划行锁，过期接管不会与仍提交中的步骤重叠。
ALTER TABLE px_operation_plan ADD COLUMN run_token VARCHAR(36) NULL,
 ADD COLUMN lease_until DATETIME NULL;
