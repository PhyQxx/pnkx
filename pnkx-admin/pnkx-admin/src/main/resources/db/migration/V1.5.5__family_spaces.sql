-- 将既有数据群组升级为家庭/情侣空间，并显式约束可共享的业务域。
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'px_data_group' AND column_name = 'space_type') = 0,
              'ALTER TABLE px_data_group ADD COLUMN space_type VARCHAR(16) NOT NULL DEFAULT ''family'' COMMENT ''family/couple/project'' AFTER group_code', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'px_data_group' AND column_name = 'owner_user_id') = 0,
              'ALTER TABLE px_data_group ADD COLUMN owner_user_id BIGINT DEFAULT NULL COMMENT ''空间所有者'' AFTER space_type', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'px_data_group' AND column_name = 'visibility_json') = 0,
              'ALTER TABLE px_data_group ADD COLUMN visibility_json JSON DEFAULT NULL COMMENT ''可共享业务域'' AFTER owner_user_id', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'px_data_group' AND column_name = 'ownership_policy') = 0,
              'ALTER TABLE px_data_group ADD COLUMN ownership_policy VARCHAR(32) NOT NULL DEFAULT ''retain_creator'' COMMENT ''成员退出后的数据归属策略'' AFTER visibility_json', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'px_data_group_member' AND column_name = 'role') = 0,
              'ALTER TABLE px_data_group_member ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT ''member'' COMMENT ''owner/admin/member'' AFTER user_id', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'px_data_group_member' AND column_name = 'status') = 0,
              'ALTER TABLE px_data_group_member ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT ''active'' COMMENT ''active/left'' AFTER role', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'px_data_group_member' AND column_name = 'left_time') = 0,
              'ALTER TABLE px_data_group_member ADD COLUMN left_time DATETIME DEFAULT NULL COMMENT ''退出时间'' AFTER status', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'px_data_group_member' AND index_name = 'idx_group_member_active') = 0,
              'ALTER TABLE px_data_group_member ADD INDEX idx_group_member_active (user_id, status, group_id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `px_data_group`
SET `owner_user_id` = CAST(`create_by` AS UNSIGNED),
    `visibility_json` = JSON_ARRAY('todo','shopping','meal','commemoration','budget')
WHERE `owner_user_id` IS NULL;

UPDATE `px_data_group_member` m
JOIN `px_data_group` g ON g.id = m.group_id
SET m.role = IF(m.user_id = g.owner_user_id, 'owner', 'member')
WHERE m.status = 'active';

CREATE TABLE IF NOT EXISTS `px_space_member_audit` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `group_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `action` VARCHAR(32) NOT NULL,
    `operator_id` BIGINT NOT NULL,
    `ownership_policy` VARCHAR(32) NOT NULL,
    `detail_json` JSON DEFAULT NULL,
    `create_time` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_space_audit_group` (`group_id`, `create_time`),
    KEY `idx_space_audit_user` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='空间成员与数据归属审计';

-- 回滚说明：先 DROP TABLE px_space_member_audit；再删除 idx_group_member_active 与本迁移新增列。
