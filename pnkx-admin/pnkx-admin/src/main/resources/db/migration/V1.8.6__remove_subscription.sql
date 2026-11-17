-- 订阅功能下线：与 生活账本→周期记账 概念重复，保留周期记账与共享账期引擎。
-- 历史账期行（px_charge_instance / px_charge_execution，source_type='subscription'）保留不动。

-- 菜单及角色授权
DELETE FROM sys_role_menu WHERE menu_id IN (
    SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE component = 'px/life/subscription/index') t
);
DELETE FROM sys_menu WHERE component = 'px/life/subscription/index';

-- 订阅出账定时任务
DELETE FROM sys_job WHERE invoke_target LIKE 'subscriptionTask%';

-- 订阅续费提醒
DELETE FROM px_life_reminder WHERE source_type = 'subscription';

-- 订阅相关自动化规则
DELETE FROM px_automation_rule WHERE trigger_type = 'subscription_due' OR action_type = 'subscription_charge';

-- 订阅表
DROP TABLE IF EXISTS px_subscription;
