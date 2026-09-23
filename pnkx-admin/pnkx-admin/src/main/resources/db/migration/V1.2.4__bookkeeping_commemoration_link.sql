-- ============================================================
-- V1.2.4 记账记录增加纪念日关联字段
--
-- 功能：记账记录（px_bookkeeping_record）可关联纪念日（px_commemoration_day），
--      实现"礼物类支出 → 纪念日"的数据联动。
--
-- 联动逻辑（代码层 PxBookkeepingRecordServiceImpl）：
--   1. 用户手动指定 commemorationDayId 时直接保存
--   2. 未指定且消费分类名含"礼物"时，自动匹配消费时间前后 30 天内最近的纪念日
--
-- 幂等：用 INFORMATION_SCHEMA 判断列是否存在，重复执行无副作用
-- ============================================================

ALTER TABLE `px_bookkeeping_record`
    ADD COLUMN `commemoration_day_id` BIGINT(20) NULL COMMENT '关联纪念日ID（礼物类支出联动）' AFTER `client_uuid`;
