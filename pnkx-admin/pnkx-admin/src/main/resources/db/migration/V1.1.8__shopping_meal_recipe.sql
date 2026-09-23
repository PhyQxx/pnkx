-- ============================================================
-- V1.1.8 购物清单 + 餐饮计划 + 菜谱库
--   1. px_shopping_list       购物清单（多个并行清单，如超市/五金店）
--   2. px_shopping_item       购物条目（关联记账分类）
--   3. px_recipe               菜谱
--   4. px_recipe_ingredient    菜谱食材
--   5. px_meal_plan            餐饮计划（周网格）
-- 幂等：所有建表用 IF NOT EXISTS；菜单按 perms 去重
-- ============================================================

-- 购物清单
CREATE TABLE IF NOT EXISTS `px_shopping_list` (
    `id`          BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(64)  NOT NULL                COMMENT '清单名称（如：超市、五金店）',
    `icon`        VARCHAR(64)  DEFAULT NULL            COMMENT '图标',
    `order_num`   INT(11)      DEFAULT 0               COMMENT '排序',
    `version`     VARCHAR(64)  DEFAULT NULL            COMMENT '版本号（离线幂等）',
    `client_uuid` VARCHAR(64)  DEFAULT NULL            COMMENT '客户端唯一标识',
    `del_flag`    CHAR(1)      DEFAULT '0'             COMMENT '删除标志（0存在 1删除）',
    `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建者（userId）',
    `create_time` DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新者（userId）',
    `update_time` DATETIME     DEFAULT NULL            COMMENT '更新时间',
    `remark`      VARCHAR(255) DEFAULT NULL            COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_create_by` (`create_by`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='购物清单';

-- 购物条目
CREATE TABLE IF NOT EXISTS `px_shopping_item` (
    `id`              BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `list_id`         BIGINT(20)   NOT NULL                COMMENT '所属清单ID',
    `name`            VARCHAR(128) NOT NULL                COMMENT '商品名称',
    `quantity`        VARCHAR(32)  DEFAULT NULL            COMMENT '数量（自由文本，如 500g、2个）',
    `classification_id` BIGINT(20) DEFAULT NULL            COMMENT '关联记账分类ID（品类分组）',
    `checked`         TINYINT(1)   DEFAULT 0               COMMENT '是否已勾选（1已购 0待购）',
    `added_from_meal` TINYINT(1)   DEFAULT 0               COMMENT '是否来自餐饮计划（1是 0否）',
    `sort_order`      INT(11)      DEFAULT 0               COMMENT '排序',
    `version`         VARCHAR(64)  DEFAULT NULL            COMMENT '版本号',
    `client_uuid`     VARCHAR(64)  DEFAULT NULL            COMMENT '客户端唯一标识',
    `del_flag`        CHAR(1)      DEFAULT '0'             COMMENT '删除标志',
    `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建者',
    `create_time`     DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新者',
    `update_time`     DATETIME     DEFAULT NULL            COMMENT '更新时间',
    `remark`          VARCHAR(255) DEFAULT NULL            COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_list` (`list_id`),
    KEY `idx_create_by` (`create_by`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='购物条目';

-- 菜谱
CREATE TABLE IF NOT EXISTS `px_recipe` (
    `id`          BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`       VARCHAR(128) NOT NULL                COMMENT '菜名',
    `url`         VARCHAR(512) DEFAULT NULL            COMMENT '菜谱链接',
    `notes`       TEXT                                 COMMENT '备注/做法',
    `servings`    INT(11)      DEFAULT 2               COMMENT '份数（人数）',
    `del_flag`    CHAR(1)      DEFAULT '0'             COMMENT '删除标志',
    `version`     VARCHAR(64)  DEFAULT NULL            COMMENT '版本号',
    `client_uuid` VARCHAR(64)  DEFAULT NULL            COMMENT '客户端唯一标识',
    `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建者',
    `create_time` DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新者',
    `update_time` DATETIME     DEFAULT NULL            COMMENT '更新时间',
    `remark`      VARCHAR(255) DEFAULT NULL            COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_create_by` (`create_by`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='菜谱';

-- 菜谱食材
CREATE TABLE IF NOT EXISTS `px_recipe_ingredient` (
    `id`               BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `recipe_id`        BIGINT(20)   NOT NULL                COMMENT '所属菜谱ID',
    `name`             VARCHAR(128) NOT NULL                COMMENT '食材名称',
    `quantity`         VARCHAR(32)  DEFAULT NULL            COMMENT '用量（自由文本）',
    `classification_id` BIGINT(20)  DEFAULT NULL            COMMENT '关联记账分类ID（品类分组）',
    `del_flag`         CHAR(1)      DEFAULT '0'             COMMENT '删除标志',
    PRIMARY KEY (`id`),
    KEY `idx_recipe` (`recipe_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='菜谱食材';

-- 餐饮计划（周网格）
CREATE TABLE IF NOT EXISTS `px_meal_plan` (
    `id`          BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `plan_date`   DATE         NOT NULL                COMMENT '计划日期',
    `meal_type`   TINYINT(2)   NOT NULL                COMMENT '餐次（1早餐 2午餐 3晚餐 4加餐）',
    `recipe_id`   BIGINT(20)   DEFAULT NULL            COMMENT '关联菜谱ID（可空，自由填）',
    `title`       VARCHAR(128) DEFAULT NULL            COMMENT '餐名（菜谱时取菜名，否则手填）',
    `notes`       VARCHAR(512) DEFAULT NULL            COMMENT '备注',
    `sort_order`  INT(11)      DEFAULT 0               COMMENT '排序（同餐次多道菜）',
    `del_flag`    CHAR(1)      DEFAULT '0'             COMMENT '删除标志',
    `version`     VARCHAR(64)  DEFAULT NULL            COMMENT '版本号',
    `client_uuid` VARCHAR(64)  DEFAULT NULL            COMMENT '客户端唯一标识',
    `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建者',
    `create_time` DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新者',
    `update_time` DATETIME     DEFAULT NULL            COMMENT '更新时间',
    `remark`      VARCHAR(255) DEFAULT NULL            COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_date_type` (`plan_date`, `meal_type`),
    KEY `idx_create_by` (`create_by`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='餐饮计划';

-- ============================================================
-- 菜单：购物清单 + 餐饮计划 + 菜谱库（挂在「生活助手」下）
-- ============================================================

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '购物清单', (SELECT menu_id FROM `sys_menu` WHERE `menu_name` = '生活助手' AND `menu_type` = 'M' LIMIT 1), 10, 'shopping', 'px/life/shopping/index', 1, 0, 'C', '0', '0', 'px:life:shopping:list', 'shopping', 'admin', NOW(), '购物清单管理'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'px:life:shopping:list');

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '餐饮计划', (SELECT menu_id FROM `sys_menu` WHERE `menu_name` = '生活助手' AND `menu_type` = 'M' LIMIT 1), 11, 'mealPlan', 'px/life/meal/index', 1, 0, 'C', '0', '0', 'px:life:meal:list', 'education', 'admin', NOW(), '餐饮周计划'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'px:life:meal:list');

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '菜谱库', (SELECT menu_id FROM `sys_menu` WHERE `menu_name` = '生活助手' AND `menu_type` = 'M' LIMIT 1), 12, 'recipe', 'px/life/recipe/index', 1, 0, 'C', '0', '0', 'px:life:recipe:list', 'guide', 'admin', NOW(), '菜谱库管理'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'px:life:recipe:list');

-- 给所有有「待办事项」权限的角色也分配这三个菜单
INSERT INTO `sys_role_menu`(`role_id`, `menu_id`)
SELECT rm.role_id, m.menu_id
FROM `sys_role_menu` rm
JOIN `sys_menu` src ON rm.menu_id = src.menu_id AND src.menu_id = 2016
JOIN `sys_menu` m ON m.perms IN ('px:life:shopping:list', 'px:life:meal:list', 'px:life:recipe:list')
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.menu_id
);
