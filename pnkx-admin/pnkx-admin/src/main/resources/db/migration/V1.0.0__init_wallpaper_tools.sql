-- ============================================================
-- V1.0.0 壁纸下载 + 工具台配置 初始化脚本
--
-- 设计原则：全部幂等
--   - 建表用 CREATE TABLE IF NOT EXISTS
--   - 数据用 INSERT ... SELECT ... WHERE NOT EXISTS 去重
--   - 菜单按 perms 唯一键去重，不依赖具体 menu_id
--   - 角色授权用 INSERT IGNORE
-- 因此无论目标库是「全新空库」还是「已运行过的库」，
-- 执行本脚本都能得到一致、正确的最终状态。
-- ============================================================

-- ----------------------------
-- 1. 壁纸文件夹表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `px_wallpaper_folder`
(
    `id`          BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(100) DEFAULT NULL COMMENT '文件夹名称',
    `cover`       VARCHAR(500) DEFAULT NULL COMMENT '封面地址',
    `parent_id`   BIGINT(20)   DEFAULT 0 COMMENT '父级id（0为根目录）',
    `order`       VARCHAR(20)  DEFAULT NULL COMMENT '排序',
    `del_flag`    TINYINT(1)   DEFAULT 0 COMMENT '删除标志（0存在 1删除）',
    `version`     VARCHAR(20)  DEFAULT NULL COMMENT '版本号',
    `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
    `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_del_flag` (`del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='壁纸文件夹';

-- ----------------------------
-- 2. 壁纸表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `px_wallpaper`
(
    `id`          BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(100) DEFAULT NULL COMMENT '壁纸名称',
    `url`         VARCHAR(500) DEFAULT NULL COMMENT '壁纸地址',
    `thumbnail`   VARCHAR(500) DEFAULT NULL COMMENT '缩略图地址',
    `folder`      BIGINT(20)   DEFAULT 0 COMMENT '所属文件夹id（0为根目录）',
    `width`       INT(11)      DEFAULT NULL COMMENT '宽度（px）',
    `height`      INT(11)      DEFAULT NULL COMMENT '高度（px）',
    `order`       VARCHAR(20)  DEFAULT NULL COMMENT '排序',
    `del_flag`    TINYINT(1)   DEFAULT 0 COMMENT '删除标志（0存在 1删除）',
    `version`     VARCHAR(20)  DEFAULT NULL COMMENT '版本号',
    `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
    `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_folder` (`folder`),
    KEY `idx_del_flag` (`del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='壁纸';

-- ----------------------------
-- 3. 壁纸文件夹示例数据（仅首次插入）
-- ----------------------------
INSERT INTO `px_wallpaper_folder`(`name`, `parent_id`, `order`, `del_flag`, `create_time`)
SELECT '风景', 0, '1', 0, NOW() WHERE NOT EXISTS (SELECT 1 FROM `px_wallpaper_folder` WHERE `name` = '风景' AND `parent_id` = 0);
INSERT INTO `px_wallpaper_folder`(`name`, `parent_id`, `order`, `del_flag`, `create_time`)
SELECT '动漫', 0, '2', 0, NOW() WHERE NOT EXISTS (SELECT 1 FROM `px_wallpaper_folder` WHERE `name` = '动漫' AND `parent_id` = 0);
INSERT INTO `px_wallpaper_folder`(`name`, `parent_id`, `order`, `del_flag`, `create_time`)
SELECT '简约', 0, '3', 0, NOW() WHERE NOT EXISTS (SELECT 1 FROM `px_wallpaper_folder` WHERE `name` = '简约' AND `parent_id` = 0);

-- ============================================================
-- 4. sys_config：小程序工具台工具列表（sys.tools.list）
--    config_type = miniprogram 分组
-- ============================================================
INSERT INTO `sys_config`(`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
SELECT '小程序工具台工具列表', 'sys.tools.list',
       '[{"id":"wallpaper","name":"壁纸下载","icon":"picture","color":"#38BDF8","desc":"精选高清壁纸，一键保存到相册","type":"page","target":"/pages_life/wallpaper/index"},{"id":"blog","name":"博客后台","icon":"article","color":"#60A5FA","desc":"管理文章、照片与博客内容","type":"web","target":"https://pnkx.top"}]',
       'miniprogram', 'admin', NOW(), '工具台首页展示的工具列表（JSON 数组）'
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'sys.tools.list');

-- ============================================================
-- 5. sys_menu：壁纸管理（单级菜单，点击直接进入）+ 按钮权限
--    （壁纸与文件夹合并为左树右表单页面，文件夹按钮挂在壁纸菜单下）
--    所有插入均按 perms 去重，不依赖具体 menu_id
-- ============================================================

-- 5.1 壁纸管理（C 菜单，单级点击直接进入）
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '壁纸管理', 0, 100, 'wallpaper', 'px/life/wallpaper/index', 1, 0, 'C', '0', '0', 'wallpaper:list', 'image', 'admin', NOW(), '壁纸下载管理'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'wallpaper:list');

-- 5.3 壁纸按钮（F）
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '壁纸查询', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'wallpaper:list' LIMIT 1), 1, '', NULL, 1, 0, 'F', '0', '0', 'wallpaper:query', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'wallpaper:query');
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '壁纸新增', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'wallpaper:list' LIMIT 1), 2, '', NULL, 1, 0, 'F', '0', '0', 'wallpaper:add', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'wallpaper:add');
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '壁纸修改', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'wallpaper:list' LIMIT 1), 3, '', NULL, 1, 0, 'F', '0', '0', 'wallpaper:edit', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'wallpaper:edit');
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '壁纸删除', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'wallpaper:list' LIMIT 1), 4, '', NULL, 1, 0, 'F', '0', '0', 'wallpaper:remove', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'wallpaper:remove');
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '壁纸导出', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'wallpaper:list' LIMIT 1), 5, '', NULL, 1, 0, 'F', '0', '0', 'wallpaper:export', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'wallpaper:export');

-- 5.4 壁纸文件夹按钮（F）：文件夹与壁纸合并为一个左树右表页面，
--      文件夹作为壁纸页面内的左侧树管理，故不再单独建文件夹菜单，
--      仅保留新增/修改/删除按钮，挂在壁纸菜单（wallpaper:list）下。
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '壁纸文件夹新增', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'wallpaper:list' LIMIT 1), 6, '', NULL, 1, 0, 'F', '0', '0', 'wallpaper:folder:add', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'wallpaper:folder:add');
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '壁纸文件夹修改', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'wallpaper:list' LIMIT 1), 7, '', NULL, 1, 0, 'F', '0', '0', 'wallpaper:folder:edit', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'wallpaper:folder:edit');
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '壁纸文件夹删除', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'wallpaper:list' LIMIT 1), 8, '', NULL, 1, 0, 'F', '0', '0', 'wallpaper:folder:remove', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'wallpaper:folder:remove');

-- ============================================================
-- 6. sys_menu：工具台配置（挂在「系统管理」目录下）
-- ============================================================

-- 6.1 工具台配置（C 菜单），parent_id = 系统管理目录 menu_id
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '工具台配置', (SELECT menu_id FROM `sys_menu` WHERE `menu_name` = '系统管理' AND `menu_type` = 'M' LIMIT 1),
       110, 'toolsConfig', 'system/toolsConfig/index', 1, 0, 'C', '0', '0', 'system:toolsConfig:list', 'tool', 'admin', NOW(), '小程序工具台工具列表配置'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:toolsConfig:list');

-- 6.2 工具台配置按钮（F）
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '工具台查询', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'system:toolsConfig:list' LIMIT 1), 1, '', NULL, 1, 0, 'F', '0', '0', 'system:toolsConfig:query', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:toolsConfig:query');
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '工具台新增', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'system:toolsConfig:list' LIMIT 1), 2, '', NULL, 1, 0, 'F', '0', '0', 'system:toolsConfig:add', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:toolsConfig:add');
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '工具台修改', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'system:toolsConfig:list' LIMIT 1), 3, '', NULL, 1, 0, 'F', '0', '0', 'system:toolsConfig:edit', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:toolsConfig:edit');
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '工具台删除', (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'system:toolsConfig:list' LIMIT 1), 4, '', NULL, 1, 0, 'F', '0', '0', 'system:toolsConfig:remove', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:toolsConfig:remove');

-- ============================================================
-- 7. sys_role_menu：授权给超级管理员（role_id = 1）
--    按子查询动态查找 menu_id，避免硬编码主键
-- ============================================================
INSERT IGNORE INTO `sys_role_menu`(`role_id`, `menu_id`)
SELECT 1, m.menu_id
FROM `sys_menu` m
WHERE m.perms IN (
    'wallpaper:list', 'wallpaper:query', 'wallpaper:add', 'wallpaper:edit', 'wallpaper:remove', 'wallpaper:export',
    'wallpaper:folder:add', 'wallpaper:folder:edit', 'wallpaper:folder:remove',
    'system:toolsConfig:list', 'system:toolsConfig:query', 'system:toolsConfig:add', 'system:toolsConfig:edit', 'system:toolsConfig:remove'
);
