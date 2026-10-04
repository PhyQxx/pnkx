-- ============================================================
-- 记账金额类型收敛：varchar → DECIMAL(18,2)
--
-- 背景：px_bookkeeping_record.money / px_bookkeeping_account.balance
-- / px_bookkeeping_record_model.money 历史上为字符型，浮点运算产物
--（科学计数法如 1.0E-4、空串等）曾写入脏值，且 SQL 侧长期依赖
-- cast(money as decimal(18,2)) 隐式转换。本迁移：
--   1) 清洗无法解析为数字的脏值（置 NULL，不参与 SUM）；
--   2) 三张表金额列统一 DECIMAL(18,2)，与既有 cast 精度一致。
-- ============================================================

-- ---------- px_bookkeeping_record ----------
UPDATE `px_bookkeeping_record`
SET `money` = NULL
WHERE `money` IS NULL
   OR TRIM(`money`) = ''
   OR `money` NOT REGEXP '^-?[0-9]+(\\.[0-9]+)?$';
ALTER TABLE `px_bookkeeping_record`
    MODIFY COLUMN `money` DECIMAL(18, 2) NULL DEFAULT NULL COMMENT '金额';

-- ---------- px_bookkeeping_record_model（记账模板） ----------
UPDATE `px_bookkeeping_record_model`
SET `money` = NULL
WHERE `money` IS NULL
   OR TRIM(`money`) = ''
   OR `money` NOT REGEXP '^-?[0-9]+(\\.[0-9]+)?$';
ALTER TABLE `px_bookkeeping_record_model`
    MODIFY COLUMN `money` DECIMAL(18, 2) NULL DEFAULT NULL COMMENT '金额';

-- ---------- px_bookkeeping_account ----------
UPDATE `px_bookkeeping_account`
SET `balance` = NULL
WHERE `balance` IS NULL
   OR TRIM(`balance`) = ''
   OR `balance` NOT REGEXP '^-?[0-9]+(\\.[0-9]+)?$';
ALTER TABLE `px_bookkeeping_account`
    MODIFY COLUMN `balance` DECIMAL(18, 2) NULL DEFAULT NULL COMMENT '结余（用户设定值，展示值为运行时按流水计算）';

-- inflow / flow_out 仅历史 insert 语句引用过，Java 侧从不写入，
-- 不确定所有存量库都有这两列：存在才迁移，避免 ALTER 报错导致启动失败
SET @has_inflow = (SELECT COUNT(*) FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'px_bookkeeping_account' AND column_name = 'inflow');
SET @sql = IF(@has_inflow = 1,
              'UPDATE `px_bookkeeping_account` SET `inflow` = NULL WHERE `inflow` IS NOT NULL AND (`inflow` NOT REGEXP ''^-?[0-9]+(\\\\.[0-9]+)?$'')',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql = IF(@has_inflow = 1,
              'ALTER TABLE `px_bookkeeping_account` MODIFY COLUMN `inflow` DECIMAL(18, 2) NULL DEFAULT NULL COMMENT ''流入''',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_flow_out = (SELECT COUNT(*) FROM information_schema.columns
                     WHERE table_schema = DATABASE() AND table_name = 'px_bookkeeping_account' AND column_name = 'flow_out');
SET @sql = IF(@has_flow_out = 1,
              'UPDATE `px_bookkeeping_account` SET `flow_out` = NULL WHERE `flow_out` IS NOT NULL AND (`flow_out` NOT REGEXP ''^-?[0-9]+(\\\\.[0-9]+)?$'')',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql = IF(@has_flow_out = 1,
              'ALTER TABLE `px_bookkeeping_account` MODIFY COLUMN `flow_out` DECIMAL(18, 2) NULL DEFAULT NULL COMMENT ''流出''',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
