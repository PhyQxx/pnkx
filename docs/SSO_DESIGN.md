# 统一单点登录（SSO）设计方案

> 版本：v1.0（2026-10-07）
> 目标：以 pnkx 为唯一身份源（IdP），7 个业务系统通过标准 OAuth2/OIDC 协议接入，实现"一处登录、处处通行"，账号密码只存在于 pnkx 的 `sys_user` 表中。

---

## 1. 接入系统盘点

| 系统 | 技术栈 | 现有登录 | 网络位置 | 用户模型 | 接入评级 |
| --- | --- | --- | --- | --- | --- |
| pnkx（IdP） | SB 3.3.5 / JDK 21 / Spring Security 6 | JWT + Redis 会话，BCrypt | **pnkx.top 公网** | sys_user（多用户） | — |
| re-role 灵境回响 | SB 3.4.1 / Java 17 / Spring Security 完整链 | JWT + refresh token 表 | rr.pnkx.top 公网 | app_user（多用户） | ★ 最容易 |
| Jpom | SB 2.7.18 / Java 8（fork，2.11.12.1） | Session + JWT，**内置 JustAuth OAuth2 登录，支持 Custom 平台** | 2122 端口 | UserModel（H2/MySQL） | ★ 零代码（界面配置） |
| kids-learn 趣学星球 | SB 3.2.5 / Java 17，无 Security 框架 | BCrypt + 双 token + Redis + 拦截器 | pnkx.top 同服务器 | user（多用户，商业产品） | ★★ 容易 |
| wujie-im 無界 | SB 3.2.0 / Java 17，无安全框架 | 手写 JWT，Controller 手动解析 | 本机/内网 19082 | user（多用户） | ★★ 中等 |
| companion-hub | Python FastAPI + Vue 3 monorepo | scrypt + DB 不透明 token | NAS 私网 + Tailscale（已有 X-Integration-Token 机器通道） | 单用户家庭工具 | ★★ 中等 |
| checkin-platform | Python 标准库 http.server（无框架） | 单管理员静态令牌 + Cookie | NAS 私网，端口 5810 | 无用户体系 | ★★★ 需手写（约百行） |
| ev-pet | SB 2.7.18 / Java 17，无密码体系 | 微信 openId / 手机号自动注册；管理端硬编码 admin/admin123；**用户接口无鉴权拦截** | 本地开发 | users（无密码字段） | ★★★★ 先补基础 |

**关键结论：**

1. 7 个系统横跨 Java（4 个 Spring Boot 版本从 2.7 到 3.4）、Python（FastAPI 和标准库）、以及一个第三方开源工具（Jpom），**任何私有协议都会导致 N 套对接代码**，必须选标准协议。
2. Jpom 内置的 JustAuth 支持 Custom OAuth2 平台（界面配置 authorize/token/userInfo 三个端点即可），**天然是标准 OAuth2 客户端**——这直接决定了 IdP 必须提供标准 OAuth2 授权码流程。
3. 因此 IdP 选型为 **Spring Authorization Server（OIDC + OAuth2）**：Java 系客户端用现成库，Python 用 AuthLib，Jpom 零代码配置，无一个系统需要发明轮子。

---

## 2. 总体架构

```
                          ┌────────────────────────────────────┐
                          │   pnkx.top（IdP，公网唯一入口）      │
                          │   Spring Authorization Server       │
                          │   /oauth2/authorize  统一登录页      │
                          │   /oauth2/token  /userinfo  /jwks   │
                          │   用户源：sys_user（BCrypt，不动）    │
                          └──────────────┬─────────────────────┘
                                         │ 授权码 + PKCE（浏览器重定向）
        ┌────────────┬─────────────┬────┴─────┬────────────┬────────────┐
        ▼            ▼             ▼          ▼            ▼            ▼
   re-role       kids-learn    wujie-im   companion-   checkin-      Jpom
   (OIDC 客户端)  (OIDC 拦截器)  (OIDC 拦截器) hub        platform     (JustAuth
   rr.pnkx.top   同服务器       内网 19082  (AuthLib)   (手写百行)    Custom 配置)
                                          NAS+Tailscale  NAS 私网
```

**流量路径（以 checkin-platform 为例）：**

1. 用户浏览器访问签到台 → 未登录 → 302 到 `https://pnkx.top/oauth2/authorize?client_id=checkin&redirect_uri=...&state=...&code_challenge=...`
2. pnkx 统一登录页（Redis session）→ 用户输入 sys_user 的账号密码 → 授权 → 302 回 `https://签到台域名/cb?code=...&state=...`
3. 签到台后端（NAS 出站公网）拿 code + client_secret 调 `/oauth2/token` 换 access token，再调 `/userinfo` 拿用户信息（userId、昵称、角色）
4. 签到台发自己的会话 Cookie，登录完成。第二次访问任何系统时，pnkx 的登录 session 仍在 → 免密直达。

**网络前提**：授权码流程中，用户浏览器须同时可达 IdP 和目标系统。公网系统天然满足；NAS 私网系统（companion-hub、checkin-platform）的访问者本来就必须先连 Tailscale/隧道，此前提自动成立。服务端到 IdP 的 token 请求全部是**出站公网**（pnkx.top），NAS 无需任何入站公网暴露。

---

## 3. IdP 侧改造（pnkx）

### 3.1 依赖与过滤器链

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-authorization-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-redis</artifactId>
</dependency>
```

版本由 Boot 3.3.5 BOM 管理（SAS 1.3.x），与现有 Spring Security 6.3 兼容。

新增 `AuthorizationServerConfig`，注册 `authorizationServerSecurityFilterChain`（`@Order(1)`，匹配 `/oauth2/*`、`/userinfo`、`/connect/*`），与现有 API 链并存；`/oauth2/**`、`/userinfo`、`/sso/login` 加入 [SecurityConfig.java](../pnkx-framework/src/main/java/com/pnkx/framework/config/SecurityConfig.java) 现有白名单。

**会话**：授权端点依赖 HttpSession。用 `spring-session-data-redis` 存 SSO 登录态（key 前缀 `sso:session:`，与现有 `login_tokens:` 互不干扰）。pnkx 四端现有 JWT 登录完全不动。

### 3.2 统一登录页

- 新增 `GET /sso/login?continue=<authorize_url>`：简单账号密码页（可复用 pnkx-client 登录组件风格），表单提交走 **现有 `AuthenticationManager`**（`DaoAuthenticationProvider` + `UserDetailsServiceImpl`），认证成功把 `SecurityContext` 写入 session 后跳回 authorize。
- 现有的失败锁定（`login_fail_count:`）、登录日志（`sys_logininfor`）逻辑挂在 `SysLoginService`，登录页 Service 直接复用。
- 微信扫码登录可作为后续增强（uniapp 端已有 openid 体系，登录页加一个"微信扫码"按钮即可覆盖 App 用户）。

### 3.3 数据库（Flyway 迁移）

SAS 官方 JDBC 三表 + 一张自定义访问控制表：

| 表 | 用途 |
| --- | --- |
| `oauth2_registered_client` | 客户端注册（client_id、secret、redirect_uri、scope、PKCE/secret 要求、有效期） |
| `oauth2_authorization` | 授权码/令牌持久化（替代默认内存，支持重启不丢、多实例） |
| `oauth2_authorization_consent` | 用户对 client 的授权记录（勾选"不再询问"） |
| `sso_client_access`（自定义） | client 级访问白名单：`client_id` + `user_id`/`role_key`，见 3.5 |

客户端注册清单（初版）：

| client_id | 类型 | 认证方式 | redirect_uri 示例 |
| --- | --- | --- | --- |
| re-role | 服务端 | client_secret | `https://rr.pnkx.top:8/login/oauth2/code/pnkx` |
| jpom | 服务端 | client_secret | `http://<jpom域名>:2122/oauth2/callback`（以 JustAuth 实际回调为准） |
| kids-learn | 服务端 | client_secret | `https://<api域名>/api/v1/auth/sso/callback` |
| kids-learn-app | 公共 | PKCE | App 内置回调（自定义 scheme / H5 域名） |
| wujie-im | 服务端 | client_secret | `http://<内网域名>:3000/sso/callback` |
| companion-hub | 服务端 | client_secret | Tailscale 网内地址 `/api/v1/auth/oidc/callback` |
| checkin | 服务端 | client_secret | `/sso/callback` |

### 3.4 令牌与用户信息

- **TokenCustomizer**（`OAuth2TokenCustomizer<JwtEncodingContext>`）：ID token / userinfo 注入 claims：

| claim | 来源 | 说明 |
| --- | --- | --- |
| `sub` | userId | 各系统做账号关联的唯一键 |
| `preferred_username` | userName | |
| `name` | nickName | |
| `email` / `email_verified` | email | |
| `picture` | avatar | 完整 URL |
| `phone_number` | phonenumber | |
| `roles` | sys_role.role_key 列表 | 如 `["admin","common"]`，供客户端判定管理员 |

- access token 用 SAS 默认 JWT（RS256，JWKS 端点 `/oauth2/jwks` 发布公钥），各系统可本地验签，也可调 `/userinfo`（Bearer）实时取。
- 令牌有效期：授权码 60s，access token 30min，ID token 15min（各系统换发自己的会话，不依赖 IdP 令牌长存活）。

### 3.5 client 级访问控制（重点）

pnkx 是开放注册的博客，而 Jpom（运维）、companion-hub（家庭中枢）、checkin（账号凭据库）是**特权系统**，绝不能任何 sys_user 都能登。方案：

- `sso_client_access` 配置每个 client 允许的 `user_id` 或 `role_key`；
- 自定义 `AuthorizationRequestResolver`：authorize 请求进入时校验当前用户是否命中白名单，未命中渲染 403 页（"该账号无权访问此系统"）；
- 未配置白名单的 client 视为全员可登（re-role、kids-learn 这类面向访客的系统）。

### 3.6 管理界面（pnkx-ui）

新增"SSO 应用管理"页：`oauth2_registered_client` + `sso_client_access` 的 CRUD + secret 轮换。普通 RuoYi 风格 CRUD，工作量小。

### 3.7 登出

- 一期：各系统独立过期（够用，个人系统会话普遍 8h~7d）；
- 二期：OIDC RP-initiated logout（`/connect/logout`）+ front-channel HTML 逐个通知各系统清 session。

---

## 4. 各系统接入设计

统一模式：**首次 SSO 登录 JIT 建号 + 存量账号绑定**。每个系统用户表加 `sso_id`（varchar，存 pnkx userId）：

```
登录回调 → 查 sso_id = sub 的本地用户
  ├─ 命中 → 发本系统会话
  ├─ 未命中且 preferred_username/email 匹配既有本地账号 → 绑定 sso_id 后登录（可选：要求输本地密码确认）
  └─ 未命中 → JIT 创建本地用户（昵称/头像从 claims 复制）→ 发会话
```

### 4.1 re-role（OIDC 客户端，半天）

- 加 `spring-boot-starter-oauth2-client`，`SecurityConfig` 里 `oauth2Login()` 一条链替代现有 `loginFilter`；本地账密登录可保留作为降级入口。
- `OidcUserMapper`：把 `sub` 写入 `app_user.sso_id`；roles 含 `admin` 时映射 `ROLE_ADMIN`。
- 前端把"登录"按钮指向后端 `/oauth2/authorization/pnkx`，回调后由后端下发现有 JWT（前端 localStorage 体系不变）。

### 4.2 Jpom（界面配置，1 小时 + 验证）

- 管理界面 → 系统设置 → OAuth2 → 新增 **Custom** 平台：clientId/secret + `https://pnkx.top/oauth2/authorize`、`/oauth2/token`、`/userinfo` 三个端点。
- **待验证点**：JustAuth Custom 平台对 userinfo 响应的字段提取（默认按它的 JSON 约定取唯一标识字段）。若与 OIDC 平铺 claims 不匹配，可在 pnkx 侧为 `client_id=jpom` 的 userinfo 响应做一层自适应格式（比如包一层 `data`），不动 Jpom 代码。
- Jpom 侧把 OAuth2 登录绑定到既有的管理员账号，配合 IdP 的 client 白名单（只允许 pnkx 管理员角色）双保险。

### 4.3 kids-learn（OIDC 拦截器，1~2 天）

- 后端加 `GET /api/v1/auth/sso/authorize`（302 到 IdP）+ `GET /api/v1/auth/sso/callback`（手写授权码交换，jjwt 已有，RestClient 调 token/userinfo，约 150 行），成功后走现有 `AuthServiceImpl` 发双 token，前端 localStorage 体系不变。
- 管理后台 `kidslearn-ui` 登录页加"使用 pnkx 账号登录"按钮。
- App（uniapp）：用 webview 打开授权页 + PKCE，回调经桥接拿 code；或一期先不做 App（App 保留验证码登录），Web/管理端先享受 SSO。
- `user` 表加 `sso_id` 列；roles 含 `admin` 时映射 `user_type=3`。

### 4.4 wujie-im（OIDC 拦截器，1~2 天）

- 与 kids-learn 同款：新增 SSO 回调 Controller + 全局 JWT 拦截器。**顺手补齐它缺失的全局鉴权拦截器**（调研发现部分接口可传 fromUserId 越权，SSO 改造时一并修）。
- `user` 表加 `sso_id` 列。

### 4.5 companion-hub（AuthLib，1 天）

- FastAPI + AuthLib 的 `StarletteOAuth2App`：注册 OIDC 客户端，回调里换 token、拉 userinfo，按 `sub` 匹配 `app_user`（单用户系统：直接绑定站长本人的 pnkx 账号，写在配置里）。
- 登录页加"使用 pnkx 登录"入口，本地 scrypt 密码保留为离线降级。
- 现有 `X-Integration-Token` 机器通道（pnkx 的 IntegrationTokenFilter 对端）不受影响，继续用于系统间调用。

### 4.6 checkin-platform（标准库手写，1 天）

- 无框架但代码量可控：`GET /sso/start`（生成 state 存内存 dict，302 到 authorize）+ `GET /sso/callback`（`urllib` 调 token/userinfo，校验 sub 是否在 `.env` 的 `SSO_ALLOWED_SUBS` 白名单 → 发现有 `checkin_session` Cookie）。
- ADMIN_TOKEN 保留为应急后门；默认登录按钮改为 SSO。

### 4.7 ev-pet（先还债，再接入，2~3 天）

- 前置：补全局 JWT 鉴权拦截器（用户端接口目前裸奔）、管理端去掉硬编码 admin/admin123。
- 然后同 kids-learn 模式接 OIDC：`users` 表加 `sso_id`，SSO 登录替代"手机号自动注册"作为 Web 端主登录方式。
- 优先级最低，排最后一期。

---

## 5. 安全设计

1. **协议**：全部走授权码流程；SPA/App/uniapp 等公共客户端强制 PKCE（SAS 按客户端配置拒绝裸授权码）；服务端客户端用 client_secret（post 或 basic）。禁止 implicit；不实现 password grant（避免密码再次流经其他系统）。
2. **redirect_uri**：精确匹配白名单，不允通配。
3. **state/nonce**：客户端库（或手写代码）必须校验。
4. **签名密钥**：IdP 用 RSA/EC 独立密钥对（JWKS 轮换），**不复用**现有 `TOKEN_SECRET`（HS512 对称密钥继续只服务 pnkx 四端自家 JWT），杜绝 secret 泄露面扩大。
5. **访问控制**：特权系统走 3.5 的 client 白名单 + 各系统侧角色映射，双重校验。
6. **令牌短寿命**：IdP 令牌 ≤30min，各系统持有的是自己的会话；泄露影响面小。
7. **审计**：SSO 登录/授权/拒绝均写 `sys_logininfor`（client_id 记在备注列），pnkx 后台可查"哪个账号何时登了哪个系统"。

---

## 6. 实施计划

| 阶段 | 内容 | 产出 | 预估 |
| --- | --- | --- | --- |
| 一（核心）✅ 已完成 | pnkx 接入 SAS：三表 + 登录页 + claims/userinfo + client 白名单 + 管理页 | 可用的 IdP | 3~4 天 |
| 二（标准客户端）✅ 已完成 | re-role 接入 + Jpom 对接方案（TopIAM 平台零代码） | 2 个系统上线 SSO | 1~2 天 |
| 三（拦截器系）✅ 已完成 | kids-learn、wujie-im（含全局鉴权与越权修复）、companion-hub | 5 个系统 SSO | 3~4 天 |
| 四（收尾）✅ 已完成 | checkin-platform 手写流程；ev-pet 补鉴权后接入 | 7/7 系统全覆盖 | 3~4 天 |
| 五（增强，可选） | pnkx 四端登录态与 SSO session 打通、微信扫码登录、OIDC 单点登出 | 体验增强 | 按需 |

### 阶段一落地说明（2026-10-07 实现并通过端到端验证）

- 代码：`pnkx-framework` 新增 `com.pnkx.framework.sso` 包（JWK/claims/白名单/客户端管理）
  与 `AuthorizationServerConfig`；`pnkx-admin` 新增 `SysSsoLoginController`（统一登录页）与
  `SysSsoClientController`（应用管理 REST）；迁移 `V1.5.8__sso_oauth2_server.sql`；
  `pnkx-ui` 新增 系统工具/SSO应用管理 页面。
- 会话：spring-session-data-redis（`SESSION` cookie，默认 7 天），与业务 JWT 并存互不影响。
- 端到端验证通过（本地 MySQL + Redis 实测）：discovery / jwks（密钥落库稳定）/
  未登录跳统一登录页 / 授权码签发（state 校验）/ 授权码换 access+refresh+id_token /
  userinfo（sub=userId、roles、中文昵称）/ refresh_token 轮换 / 已登录会话免登二次授权 /
  client 白名单拒绝（access_denied 回跳客户端）/ JSON 客户端 401 语义 /
  原有 /login JWT 登录链不受影响。
- 部署注意：生产需设置 `SSO_ISSUER=https://pnkx.top`（本地调试留空按请求域名推断）；
  菜单「SSO应用管理」默认仅超管（role_id=1）可见。

### 阶段二落地说明（2026-10-08 实现并通过双系统端到端验证）

**re-role 灵境回响（标准 OIDC 客户端，已实测）**

- 后端：加 `spring-boot-starter-oauth2-client`；`SecurityConfig` 配 `oauth2Login`
  （授权入口 `/api/auth/sso/login/pnkx`，回调 `/api/auth/sso/callback`），并把无 token
  API 的响应恢复为 401（oauth2Login 默认入口点会改成 302，前端 axios 依赖 401）。
- 账号映射：`app_user` 加 `sso_id` 列（V55）；`AuthService.loginWithSso` 按
  sub（pnkx userId）查号，未命中 JIT 建号；**用户名冲突追加后缀，绝不静默绑定同名
  本地账号**（防接管）；pnkx `admin` 角色映射本地 `ROLE_ADMIN`（仅建号时）。
- 回跳：登录成功后重定向 `前端/sso/callback#access_token=...&refresh_token=...&user=...`
  （fragment 不进访问日志），前端 `SsoCallbackView` 落 localStorage，复用既有续期链路。
- 配置（env）：`LINGJING_SSO_ISSUER` / `LINGJING_SSO_CLIENT_SECRET` /
  `LINGJING_SSO_FRONTEND_URI`（默认 dev 值），`LINGJING_SSO_ENABLED=false` 可整体关闭。
- 双服务实测通过：完整授权码流程、免登二次授权、JIT 建号（含主角档案复制）、
  角色映射、种子账号不被绑定、旧本地 JWT 不受影响、无 token 401。

**Jpom（零代码，选 TopIAM 平台而非 Custom，已协议级实测）**

关键结论：Jpom 内置的 JustAuth 平台中，**Custom 平台**解析 userinfo 的
`id`/`username` 字段与 OIDC 不符，而 **TopIAM 平台**（`TopiamAuthOauth2Request`）
按标准 OIDC 解析 `sub`/`preferred_username`/`nickname`/`picture`/`email`，
token 交换用 Basic 认证 + form——与 pnkx 完全兼容。pnkx 侧已补发 `nickname`
claim（OIDC 标准声明）。Jpom 管理界面配置（系统设置 → OAuth2 → TopIAM）：

| 配置项 | 值 |
| --- | --- |
| authorizationUri | `https://pnkx.top/oauth2/authorize` |
| accessTokenUri | `https://pnkx.top/oauth2/token` |
| userInfoUri | `https://pnkx.top/userinfo` |
| clientId / clientSecret | 在 pnkx「SSO应用管理」注册 `jpom` 应用获取（**必须勾选 email scope**，TopIAM 固定请求 openid+email+profile） |
| redirectUri | Jpom 实际访问地址的 OAuth2 回调页（Jpom 登录页发起时自动携带） |
| 自动创建用户 + 权限组 | 按需开启；权限组谨慎选择（运维权限） |

pnkx 侧该 client **必须配置访问白名单**（建议 role=admin），防止开放注册用户
登录运维系统。协议级验证已通过（Basic 换 token + Bearer 取 userinfo，
五个字段全部命中）；真机界面点击流程待生产部署时走一遍。

验证顺序建议：阶段一完成后先用 re-role（框架最全）当金丝雀跑通全流程，再复制到其他系统。

## 7. 风险与待验证点

1. **Jpom JustAuth Custom 的 userinfo 解析约定**——阶段二第一件事，如不匹配在 IdP 侧做响应适配（不动 Jpom 源码，保留后续升级 upstream 的能力）。
2. **Tailscale 环境的浏览器可达性**——companion-hub 回调地址必须是用户浏览器在 VPN 内可达的地址（Tailscale MagicDNS 域名），需实测手机端表现。
3. **kids-learn 商业用户与博客账号的边界**——面向公众的 App 用户（短信验证码登录）与 pnkx 账号是两拨人，SSO 只服务 Web/管理端，App 端是否接由运营决定，技术上用 PKCE 公共客户端预留。
4. **SAS 与现有 Spring Security 白名单的合并**——`/oauth2/**` 放行仅指不经过现有 JWT 过滤器，端点本身仍受 SAS 自己的链保护；注意别把 `/userinfo` 意外暴露为匿名。

### 阶段三落地说明（2026-10-08 实现并验证）

三个系统采用同一套「服务端授权码直连」模式：后端 302 到 pnkx 授权页 →
回调后服务端 Basic 认证换令牌 → 拉 userinfo → 按 `sso_id`（= OIDC sub =
pnkx userId）映射本地账号（未命中 JIT 建号，同名不绑定、冲突加后缀）→
签发本系统自身会话 → 经 URL fragment 回跳前端落地页。

**kids-learn 趣学星球（已双系统 E2E）**

- 后端：`V20261008_01__user_sso_id.sql`（database/migrations/，手动执行）、
  `User.ssoId`、`AuthService.loginWithSso`（JIT 含默认 child_profile 学习档案，
  pnkx admin → userType=3）、`SsoOidcService`（state 存 Redis 5 分钟一次性）、
  `SsoAuthController`（`/api/v1/auth/sso/login|callback`，在拦截器白名单内）。
- 前端（kidslearn-ui）：登录页 SSO 按钮、`/sso/callback` 落地页、路由守卫放行。
- 配置（env）：`SSO_ISSUER` / `SSO_CLIENT_SECRET` / `SSO_FRONTEND_URI`。
- 实测：全流程 + JIT（user_type=3、permissions=admin:*）+ 无 token 401。

**wujie-im 無界（已双系统 E2E，并补齐安全债）**

- SSO：`backend/sql/20261008_sso_user_sso_id.sql`（手动执行）、`User.ssoId`、
  `AuthService.loginWithSso`（JIT 含 user_profile，pnkx admin → role=ADMIN）、
  `SsoOidcService` + `SsoAuthController`（/api/auth/sso/**）；前端 Login.vue
  SSO 按钮 + SsoCallback.vue + 路由。
- **安全修复**（原项目无任何统一鉴权）：新增 `JwtAuthInterceptor` 统一校验
  /api/** Bearer token（排除 /api/auth/**），身份放入 request attribute；
  Friend/Conversation/Robot/Message/Notification/Group/User 七个控制器中
  `fromUserId`/`userId`/`ownerId`/`senderId` 等身份参数一律改为以 token 为准
  （body 参数忽略）；/api/admin/** 额外要求 user.role=ADMIN。
- 实测：SSO 全流程 + JIT（role=ADMIN）+ 无 token 401（原先裸奔）+
  伪造 fromUserId 被忽略（DB 验证真实发送者为 token 身份）+ 普通用户访问
  管理端 403 + 本地账密登录不受影响。

**companion-hub（代码完成 + 构建/类型检查通过，待生产联调）**

- 后端：`AuthService.login_sso`（为已验证归属用户直接建会话）、
  `api/auth_sso.py`（httpx 手写 OIDC 授权码流程，state 进程内存 5 分钟）、
  单用户准入：`ARIA_SSO_ALLOWED_SUB` 白名单（必须显式配置站长本人 pnkx
  userId），且要求本地已完成首次 setup；成功后返回落地页写
  localStorage(ariaChatToken) 回首页。本地密码登录保留为离线降级。
- 配置：`.env.example` 新增 ARIA_SSO_ISSUER / CLIENT_ID / CLIENT_SECRET /
  ALLOWED_SUB / BASE_URL 五项（齐备才启用）；回调 redirect_uri =
  `{BASE_URL}/api/v1/auth/sso/callback`。
- 前端：App.vue 登录卡新增「使用 pnkx 账号登录」按钮（探测 /sso/status
  自动显隐，setup 未完成时隐藏）。
- 注意：本系统部署在 NAS + Tailscale，`ARIA_SSO_BASE_URL` 须填浏览器在
  VPN 内可达的地址；未做运行时 E2E（需整套 compose 栈），部署时验证。

### 阶段四落地说明（2026-10-08 实现并验证）——全部 7 系统接入完成

**checkin-platform 签到台（标准库手写，已 E2E）**

- 新增 `app/sso.py`（纯标准库 OIDC：urllib 换令牌、内存 state 5 分钟一次性）；
  `server.py` 增加 `/sso/status`、`/sso/login`、`/sso/callback` 路由，成功后签发与
  ADMIN_TOKEN 登录一致的 `checkin_session` cookie；前端登录卡新增 SSO 按钮
  （探测 status 自动显隐），失败经 `?sso_error=` 回登录页提示。
- 准入：`SSO_ALLOWED_SUBS` 白名单（pnkx userId，逗号分隔）必须显式配置——
  签到台存各站点加密凭据，属特权系统。ADMIN_TOKEN 保留为应急后门。
- 配置：`.env.example` 新增 SSO_ISSUER / CLIENT_ID / CLIENT_SECRET / ALLOWED_SUBS。
- 实测：全流程 + SSO 会话访问业务接口 200 + 无会话 401。

**ev-pet EV宠物（先还债再接入，已 E2E）**

- 安全修复：新增 `UserAuthInterceptor`（原先 /api/** 用户接口完全无鉴权，现统一
  要求 Bearer token，userId 放入 request attribute，控制器原有的
  getUserIdFromToken 逻辑不受影响）；管理端硬编码 admin/admin123 已移除，改为
  `EVPET_ADMIN_USERNAME/PASSWORD` 环境变量（常量时间比较，未配置即关闭登录通道）；
  顺手修复了仓库 HEAD 本身编译不过的 SocialService 缺 import 问题。
- SSO：`users` 表加 `sso_id`（sql/20261008_sso_user_sso_id.sql，手动执行）、
  `AuthService.loginWithSso`（JIT 含默认宠物）、`SsoOidcService`（SB 2.7，
  RestTemplate 实现）、`SsoAuthController`（/api/auth/sso/**，在用户端鉴权白名单内）；
  PC 前端（ev-pet-pc-web）登录页新增 pnkx 按钮 + `/sso/callback` 落地页。
- 实测：SSO 全流程 + JIT（昵称=测试管理员、默认宠物创建）+ 带 token 200 +
  无 token 401（原先裸奔）+ 旧硬编码凭据登录被拒（code 401）。

### 全局部署清单（上线时逐项确认）

1. pnkx：设置 `SSO_ISSUER=https://pnkx.top`；Flyway 自动执行 V1.5.8；
   在「SSO应用管理」逐个注册 7 个 client（Jpom 勾选 email scope；特权系统配白名单）。
2. re-role：`LINGJING_SSO_ISSUER/CLIENT_SECRET/FRONTEND_URI`；执行 V55 迁移。
3. Jpom：管理界面配置 TopIAM 平台三个端点（见阶段二表格）。
4. kids-learn：执行 V20261008_01 迁移；`SSO_ISSUER/CLIENT_SECRET/FRONTEND_URI`。
5. wujie-im：执行 sql/20261008 迁移；`SSO_ISSUER/CLIENT_SECRET/FRONTEND_URI`。
6. companion-hub：五个 ARIA_SSO_* 环境变量；部署时验证回调（Tailscale 可达性）。
7. checkin-platform：.env 四个 SSO_* 变量（ALLOWED_SUBS 必填）。
8. ev-pet：执行 sql/20261008 迁移；`SSO_ISSUER/CLIENT_SECRET/FRONTEND_URI`；
   管理端需配置 `EVPET_ADMIN_USERNAME/PASSWORD`。
