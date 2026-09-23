# Pnkx 部署指南

本文档说明四个子项目的部署依赖与配置项。**所有敏感凭据一律通过环境变量注入，代码与配置文件中不再保存任何默认密钥。**

## 架构与外部依赖

```
                    ┌─ pnkx-client (Nuxt 3, SSR 博客前台)
用户 ── nginx ──────┼─ pnkx-ui     (Vue 3 管理后台静态资源)
                    └─ pnkx-admin  (Spring Boot 3 / JDK 21 后端 API)
                                ├─ MySQL 8.x（Flyway 自动迁移建表）
                                ├─ Redis（登录态 / 验证码 / 限流 / 对话上下文）
                                └─ FTP（图片等静态资源存储）
pnkx-uniapp (uniapp 移动端，HBuilderX 发布 App/小程序/H5)
```

## 后端环境变量清单（pnkx-admin）

### 必填（缺失时启动失败或功能不可用）

| 变量 | 说明 | 缺失后果 |
|---|---|---|
| `TOKEN_SECRET` | JWT 签名密钥，**HS512 要求 ≥64 字符随机串** | 启动直接失败（fail-fast 校验） |
| `DB_URL` | MySQL JDBC 连接串 | 回退 127.0.0.1:3306/pnkx |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号 | 回退 root/123456（仅适用本地开发） |
| `REDIS_HOST` / `REDIS_PORT` | Redis 地址 | 回退本机 6379 |

### 按需配置

| 变量 | 说明 | 默认 |
|---|---|---|
| `SERVER_PORT` | 服务端口 | 8068 |
| `WX_APPID` / `WX_SECRET` | 微信小程序登录凭据 | 空（微信登录不可用，启动告警） |
| `WEBHOOK_SECRET` | VoceChat Webhook 鉴权密钥（`X-Webhook-Secret` 头或 `X-Webhook-Signature` HMAC-SHA256 hex） | 空（无鉴权 + 告警日志） |
| `INTEGRATION_TOKEN` | 内部系统集成令牌（X-Integration-Token，绑定 userId=1） | 空（集成通道关闭） |
| `FTP_HOST` / `FTP_PORT` / `FTP_USERNAME` / `FTP_PASSWORD` | FTP 存储连接 | 本机/admin/空 |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | QQ 邮箱 SMTP（注册激活 / 重置密码邮件） | 空（邮件功能不可用） |
| `SWAGGER_ENABLED` | Swagger 文档 | false（生产保持关闭） |
| `DRUID_STAT_ENABLED` / `DRUID_USERNAME` / `DRUID_PASSWORD` | Druid 监控台 | false 关闭 |
| `LOG_LEVEL` | com.pnkx 日志级别 | info |

### 生成强随机密钥示例

```bash
openssl rand -base64 64   # TOKEN_SECRET
openssl rand -hex 24      # WEBHOOK_SECRET / INTEGRATION_TOKEN
```

### 构建与运行

```bash
cd pnkx-admin
mvn -DskipTests package
java -jar pnkx-admin/target/pnkx-admin.jar
# 或 Docker（镜像基于 JRE 21）
docker build -t pnkx-admin . && docker run --env-file prod.env -p 8068:8068 pnkx-admin
```

首次启动时 Flyway 自动执行 `pnkx-admin/src/main/resources/db/migration` 下的迁移脚本建表；对已有库自动打 baseline。

## 博客前台（pnkx-client，Nuxt 3）

- 依赖 Node ≥ 20.12（依赖树要求 `node:util.styleText`）
- 包管理器：pnpm（项目 `.npmrc` 带 `shamefully-hoist=true`）
- 环境变量见 `.env.dev` / `.env.prod`：
  - `VITE_APP_BASE_URL`：后端 API 地址
  - `AI_EMBED_URL`：AI 聊天机器人 embed 地址（含 token，留空则不加载）

```bash
cd pnkx-client
pnpm install
pnpm build          # 生产构建（--dotenv .env.prod）
node .output/server/index.mjs
```

## 管理后台（pnkx-ui，Vue 3 / Vite）

```bash
cd pnkx-ui
npm install
npm run build       # 产物 dist/，任意静态服务器/nginx 托管
npm run lint        # ESLint（错误级 0 通过，警告级为存量迁移债）
```

开发时 `VUE_APP_BASE_API=/dev-api` 由 Vite 代理转发到 `http://localhost:8068`。

## 移动端（pnkx-uniapp）

- 包管理器：pnpm（仓库仅保留 `pnpm-lock.yaml`）
- 构建：HBuilderX 导入项目后发布（App / 小程序 / H5），CI 中仅做依赖一致性校验
- 微信小程序 AppID 已配置于 `manifest.json`

## nginx 参考要点

- `/prod-api` → 反代 `127.0.0.1:8068`（去前缀）
- WebSocket（`/websocket/**`）需 `Upgrade`/`Connection` 头透传
- 静态资源（`/ftp/`）可直接反代 FTP 服务器或 CDN

## 安全运维备忘

1. 所有曾入库的密钥（微信 appsecret、MySQL root、飞书 webhook、AI embed token）**视为已泄露**，需在对应平台重置后经环境变量注入；
2. 建议用 `git filter-repo` 清洗历史提交中的密钥；
3. 生产保持 `SWAGGER_ENABLED=false`、`DRUID_STAT_ENABLED=false`；
4. 登录接口有"账号+IP 连续失败 5 次锁 10 分钟"的 Redis 限流；
5. 数据库迁移由 Flyway 管理，禁止手工改表结构。
