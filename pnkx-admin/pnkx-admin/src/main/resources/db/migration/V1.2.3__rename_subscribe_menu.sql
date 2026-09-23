-- ============================================================
-- V1.2.3 将博客"邮箱订阅"菜单名明确化，与"付费订阅"区分
--
-- 背景：项目存在两个"订阅"概念
--   1. 博客邮箱订阅（PxEmailSubscribe，原 PxSubscribe）→ component = 'px/blog/subscribe/index'
--   2. 生活付费订阅（PxSubscription）→ component = 'px/life/subscription/index'
--   菜单名都叫"订阅"，容易混淆。本脚本将前者改为"邮箱订阅"，明确语义。
--
-- 配套代码改动：实体已由 PxSubscribe 重命名为 PxEmailSubscribe（V1.2.3 同期）。
--   表名 px_subscribe、接口路径 /admin/subscribe、/client/addSubscribe 均保持不变。
--
-- 幂等：用 component 精确定位，重复执行无副作用。
-- ============================================================

UPDATE `sys_menu`
SET `menu_name` = '邮箱订阅',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = CONCAT(IFNULL(`remark`, ''), ' [原名"订阅管理"，已明确为邮箱订阅]')
WHERE `component` = 'px/blog/subscribe/index'
  AND `menu_type` = 'C';
