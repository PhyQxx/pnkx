-- ============================================================
-- V1.1.2 待办增强：优先级 + 看板状态 + 子任务 + 拖拽排序
--   - priority      优先级（0无 1低 2中 3高 4紧急）
--   - kanban_status 看板状态（0待办 1进行中 2已完成，与 status 字段并存兼容）
--   - parent_id     父任务ID（子任务关联，顶级任务为 0/NULL）
--   - sort_order    看板列内拖拽排序
-- 幂等：用 information_schema 判断列是否存在再 ADD
-- ============================================================

-- priority 优先级
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'px_to_do' AND column_name = 'priority');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `px_to_do` ADD COLUMN `priority` TINYINT DEFAULT 0 COMMENT ''优先级（0无 1低 2中 3高 4紧急）''',
    'SELECT ''priority 已存在''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- kanban_status 看板状态
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'px_to_do' AND column_name = 'kanban_status');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `px_to_do` ADD COLUMN `kanban_status` TINYINT DEFAULT 0 COMMENT ''看板状态（0待办 1进行中 2已完成）''',
    'SELECT ''kanban_status 已存在''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- parent_id 父任务ID（子任务）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'px_to_do' AND column_name = 'parent_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `px_to_do` ADD COLUMN `parent_id` BIGINT DEFAULT 0 COMMENT ''父任务ID（0=顶级任务）''',
    'SELECT ''parent_id 已存在''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sort_order 拖拽排序
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'px_to_do' AND column_name = 'sort_order');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `px_to_do` ADD COLUMN `sort_order` INT DEFAULT 0 COMMENT ''看板列内排序''',
    'SELECT ''sort_order 已存在''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 看板状态索引（按状态+排序查询）
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'px_to_do' AND index_name = 'idx_kanban');
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `px_to_do` ADD INDEX `idx_kanban` (`kanban_status`, `sort_order`)',
    'SELECT ''idx_kanban 已存在''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 数据迁移：用已有 status 初始化 kanban_status
--   status=1(已完成) → kanban_status=2(已完成)
--   status=0(未完成) → kanban_status=0(待办)
-- ============================================================
UPDATE `px_to_do` SET `kanban_status` = 2 WHERE `status` = 1 AND `kanban_status` = 0;
UPDATE `px_to_do` SET `kanban_status` = 0 WHERE `status` = 0 AND (`kanban_status` IS NULL OR `kanban_status` = 0);

-- ============================================================
-- 菜单：待办看板（挂在待办事项下，parent_id 动态查询）
-- ============================================================
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '待办看板', (SELECT menu_id FROM `sys_menu` WHERE `path` = 'todo' AND `menu_type` = 'C' LIMIT 1), 2, 'kanban', 'px/life/todo/kanban', 1, 0, 'C', '0', '0', 'px:life:todo:kanban', 'drag', 'admin', NOW(), '待办看板视图（拖拽管理）'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'px:life:todo:kanban');
