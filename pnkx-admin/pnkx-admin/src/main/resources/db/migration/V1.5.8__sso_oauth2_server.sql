-- ============================================================
-- V1.5.8  单点登录（SSO）：pnkx 升级为 OAuth2/OIDC 授权服务器
--
-- 背景：pnkx 作为唯一身份源（IdP），re-role / Jpom / kids-learn /
-- wujie-im / companion-hub / checkin-platform / ev-pet 七个系统
-- 通过标准授权码流程接入，账号密码只保留在 sys_user。
--
-- 本迁移：
--   1) Spring Authorization Server 官方 JDBC 三表
--      （客户端注册 / 授权记录 / 授权同意，列名与官方 schema 一致）；
--   2) sso_client_access：client 级访问白名单（特权系统准入控制）；
--   3) sso_jwk：令牌签名 RSA 密钥对（PEM 持久化，保证 JWKS 重启不变）；
--   4) 后台「SSO 应用管理」菜单（挂在「系统管理」下，仅超管可见）。
-- ============================================================

-- ---------- oauth2_registered_client：接入应用注册 ----------
-- 列名与 Spring Authorization Server 1.3 官方 schema 完全一致
-- （JdbcRegisteredClientRepository 按列名读写，不可改动）
CREATE TABLE IF NOT EXISTS `oauth2_registered_client`
(
    `id`                            varchar(100)  NOT NULL COMMENT '主键（UUID）',
    `client_id`                     varchar(100)  NOT NULL COMMENT '客户端标识',
    `client_id_issued_at`           timestamp     NULL     DEFAULT CURRENT_TIMESTAMP COMMENT '签发时间',
    `client_secret`                 varchar(200)  NULL COMMENT 'BCrypt 后的密钥',
    `client_secret_expires_at`      timestamp     NULL COMMENT '密钥过期时间（空=永不过期）',
    `client_name`                   varchar(200)  NOT NULL COMMENT '应用名称',
    `client_authentication_methods` varchar(1000) NOT NULL COMMENT '客户端认证方式（basic/post/none）',
    `authorization_grant_types`     varchar(1000) NOT NULL COMMENT '授权类型（authorization_code,refresh_token）',
    `redirect_uris`                 varchar(1000) NULL COMMENT '回调地址（逗号分隔）',
    `post_logout_redirect_uris`     varchar(1000) NULL COMMENT '登出回调地址',
    `scopes`                        varchar(1000) NOT NULL COMMENT '授权范围',
    `client_settings`               varchar(2000) NOT NULL COMMENT '客户端设置（JSON）',
    `token_settings`                varchar(4000) NOT NULL COMMENT '令牌设置（JSON）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sso_client_id` (`client_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='SSO 接入应用注册';

-- ---------- oauth2_authorization：授权码/令牌记录（重启不丢） ----------
CREATE TABLE IF NOT EXISTS `oauth2_authorization`
(
    `id`                            varchar(100) NOT NULL,
    `registered_client_id`          varchar(100) NOT NULL,
    `principal_name`                varchar(200) NOT NULL,
    `authorization_grant_type`      varchar(100) NOT NULL,
    `authorized_scopes`             varchar(1000) NULL,
    `attributes`                    blob NULL,
    `state`                         varchar(500) NULL,
    `authorization_code_value`      blob NULL,
    `authorization_code_issued_at`  timestamp NULL,
    `authorization_code_expires_at` timestamp NULL,
    `authorization_code_metadata`   blob NULL,
    `access_token_value`            blob NULL,
    `access_token_issued_at`        timestamp NULL,
    `access_token_expires_at`       timestamp NULL,
    `access_token_metadata`         blob NULL,
    `access_token_type`             varchar(100) NULL,
    `access_token_scopes`           varchar(1000) NULL,
    `oidc_id_token_value`           blob NULL,
    `oidc_id_token_issued_at`       timestamp NULL,
    `oidc_id_token_expires_at`      timestamp NULL,
    `oidc_id_token_metadata`        blob NULL,
    `refresh_token_value`           blob NULL,
    `refresh_token_issued_at`       timestamp NULL,
    `refresh_token_expires_at`      timestamp NULL,
    `refresh_token_metadata`        blob NULL,
    `user_code_value`               blob NULL,
    `user_code_issued_at`           timestamp NULL,
    `user_code_expires_at`          timestamp NULL,
    `user_code_metadata`            blob NULL,
    `device_code_value`             blob NULL,
    `device_code_issued_at`         timestamp NULL,
    `device_code_expires_at`        timestamp NULL,
    `device_code_metadata`          blob NULL,
    PRIMARY KEY (`id`),
    KEY `idx_sso_authorization_principal` (`principal_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='SSO 授权与令牌记录';

-- ---------- oauth2_authorization_consent：用户对应用的授权同意 ----------
CREATE TABLE IF NOT EXISTS `oauth2_authorization_consent`
(
    `registered_client_id` varchar(100)  NOT NULL,
    `principal_name`       varchar(200)  NOT NULL,
    `authorities`          varchar(1000) NOT NULL,
    PRIMARY KEY (`registered_client_id`, `principal_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='SSO 授权同意记录';

-- ---------- sso_client_access：client 级访问白名单 ----------
-- 无记录 = 所有 pnkx 用户可登录；有记录 = 仅命中用户/角色可登录。
-- 特权系统（Jpom 运维、签到台等）必须配置，防止开放注册用户越权进入。
CREATE TABLE IF NOT EXISTS `sso_client_access`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `client_id`     varchar(100) NOT NULL COMMENT '应用 client_id',
    `subject_type`  varchar(20)  NOT NULL COMMENT 'user=指定用户ID / role=指定角色Key',
    `subject_value` varchar(100) NOT NULL COMMENT 'userId 或 roleKey',
    `remark`        varchar(200) NULL COMMENT '备注',
    `create_time`   datetime     NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   datetime     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sso_client_subject` (`client_id`, `subject_type`, `subject_value`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='SSO 应用访问白名单';

-- ---------- sso_jwk：令牌签名密钥对（单行） ----------
-- 首次启动生成 RSA 2048 并落库，之后重启复用，保证 JWKS 端点公钥稳定。
CREATE TABLE IF NOT EXISTS `sso_jwk`
(
    `id`          varchar(10) NOT NULL COMMENT '固定 main',
    `public_key`  text        NOT NULL COMMENT 'PEM 公钥',
    `private_key` text        NOT NULL COMMENT 'PEM 私钥',
    `create_time` datetime    NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='SSO 令牌签名密钥';

-- ---------- 菜单：SSO 应用管理（挂在「系统管理」下） ----------
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 'SSO应用管理', (SELECT menu_id FROM `sys_menu` WHERE `menu_name` = '系统管理' AND `menu_type` = 'M' LIMIT 1), 99, 'sso', 'system/sso/index', 1, 0, 'C', '0', '0', 'system:sso:list', 'lock', 'admin', NOW(), '单点登录接入应用管理'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:sso:list');

-- 按钮权限：查询/新增/修改/删除
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT t.menu_name, (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'system:sso:list' LIMIT 1), t.order_num, '', '', 1, 0, 'F', '0', '0', t.perms, '#', 'admin', NOW(), ''
FROM (SELECT 'SSO应用查询' menu_name, 1 order_num, 'system:sso:query' perms
      UNION ALL SELECT 'SSO应用新增', 2, 'system:sso:add'
      UNION ALL SELECT 'SSO应用修改', 3, 'system:sso:edit'
      UNION ALL SELECT 'SSO应用删除', 4, 'system:sso:remove') t
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = t.perms);

-- 菜单默认仅超管（role_id=1，RuoYi 内置管理员角色）可见，不写入 sys_role_menu
