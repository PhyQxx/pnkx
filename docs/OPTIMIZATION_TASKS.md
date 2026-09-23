# Pnkx 优化任务清单

> 生成于 2026-09-22，来源：全项目代码审查（后端 / pnkx-ui / pnkx-client / pnkx-uniapp / 工程化）
> 勾选规则：`[ ]` 待办 → `[x]` 完成。每项附证据位置，完成后建议在行尾注明日期。

## 阶段一：安全紧急修复（最高优先级）

### 后端鉴权漏洞
- [x] 1.1 匿名重置密码接口：`SysLoginController.java:168` `GET /restPassword/{userName}` 在放行名单（`SecurityConfig.java:111`），新密码直接返回给调用方。改为 POST + 邮箱验证码/token 校验；同批处理 `/activation/{userName}`（2026-09-22：已改为邮件一次性令牌，Redis 存储 + 常量时间比较 + 用后即焚，前端 login.vue/user.ts 同步）
- [x] 1.2 文件管理器匿名开放：`SecurityConfig.java:118` `/system/file-manager/**` permitAll。移除放行、加权限注解；`SysFileManagerController.java:40` 路径校验改 `Path.normalize().startsWith(base)`，启用 ALLOWED_EXTENSIONS 死代码；修复 `create` 接口 path 为 null 的 NPE（:231）（2026-09-22：类级 @PreAuthorize admin + resolveSafePath + 白名单生效 + 禁删根目录 + search 跳过大文件）
- [x] 3. WebSocket 鉴权：`WebSocketController.java:28` 以用户名为身份凭证可冒充任意用户。握手校验 JWT；`SESSION_POOL`（:35，HashMap）改 ConcurrentHashMap；onClose（:64）清理 SESSION_POOL（2026-09-22：onOpen 校验 query token 且身份需匹配路径 userId，TokenService 新增 getLoginUserByToken；前端 Chat/ReminderBell 连接已带 token）
- [x] 4. Security 规则顺序：`SecurityConfig.java:119-121` `/client/**` permitAll 写在壁纸下载 `authenticated()` 之前，导致下载鉴权与限额失效。具体规则前置（2026-09-22：已调序）
- [x] 7. Webhook 签名校验：`PxWebhookController.java:88` 无任何签名/IP 校验可被刷 AI 调用；:89 完整聊天内容落 info 日志，改 debug 并脱敏（2026-09-22：新增 WEBHOOK_SECRET 配置，支持 X-Webhook-Secret/X-Webhook-Signature(HMAC-SHA256)，请求体日志降 debug）
- [x] 14. 客户端登录防爆破：`SysLoginController.java:73` clientLogin 无验证码无限流，按 IP/账号维度加 Redis 计数限流（2026-09-23：userNameAndPassWordLogin 加"账号+IP 连续失败 5 次锁 10 分钟"，管理端与客户端登录双入口受益；管理端原有验证码保持不变）

### 敏感信息治理（代码侧；平台轮换需人工）
- [x] 5a. `application.yml:199` 微信 appsecret 默认值 → 移除默认值；:18 集成令牌默认值 `'pnkx'` 绑定管理员 → 改空 + 启动 fail-fast；:93/171 `DB_PASSWORD:123456`、:130 Druid 弱口令 → 默认值清空（2026-09-22：WX_SECRET/INTEGRATION_TOKEN/DRUID_PASSWORD 默认值已清空；新增 SecurityStartupCheck：token.secret 缺失/过短即拒绝启动）
- [x] 5b. `pnkx-ui/.env.production` / `.env.development` 的 `VITE_RSA_PRIVATE_KEY` 移除，改为运行时从服务端获取或废弃"记住密码"功能（2026-09-22：私钥已删除，"记住我"降级为仅记住用户名，jsencrypt.js 仅保留公钥加密）
- [x] 5c. `sync_wallpaper.py:57` 硬编码生产库 root 密码 → 环境变量读取；脚本移入 `scripts/`（2026-09-22：已改环境变量 WP_DB_*，缺密码直接退出；已移入 scripts/，diff_fire.sql 一并归档）
- [x] 5d. `pnkx-admin/src/test/java/NotifyTest.java:16` 飞书 webhook token → 环境变量；手工测试脚本加 @Disabled（2026-09-22：@Ignore + FEISHU_WEBHOOK 环境变量）
- [x] 5e. `FtpTool.java:89,95` 连接失败日志打印 FTP 用户名密码 → 脱敏（2026-09-22：日志不再含密码；顺带修复 closeFtpClient null NPE）
- [ ] 5f. （人工）微信平台重置 appsecret；修改生产 MySQL root 密码；`git filter-repo` 清洗历史

### 其他安全项
- [x] 6. 分片上传路径穿越：`SysFileController.java:87,96` `identifier`/`chunkNumber` 未校验。白名单校验（UUID/数字）+ normalize 限制在上传根内（2026-09-22：identifier/chunkNumber/totalChunks/扩展名白名单校验 + uploadPath 净化；SysFileServiceImpl 增加 sanitizeFtpPath 统一防穿越；mergeChunk 改 try-with-resources）
- [x] 15. Swagger / Druid 控制台生产默认关闭：`application.yml:40,123-130`，用 profile 区分（2026-09-23：swagger.enabled 与 druid statViewServlet 默认 false，开发通过 SWAGGER_ENABLED / DRUID_STAT_ENABLED 环境变量开启）
- [x] （部署）`pnkx-admin/Dockerfile:1` `eclipse-temurin:8-jre-alpine` → `eclipse-temurin:21-jre`（2026-09-22：已修复）

## 阶段二：前端安全（pnkx-client / pnkx-ui）

- [x] 2.1 pnkx-client XSS：`comment/item.vue:153` 评论 v-html；`message.vue:119`、`video/video.vue:318` 弹幕 v-html；`plugins/markdown.ts:13` MarkdownIt `html:true` 无消毒。统一接 DOMPurify（参考 pnkx-ui `utils/sanitizeHtml.js`）；评论/弹幕改纯文本+表情白名单（2026-09-22：评论改纯文本插值 whitespace-pre-line、两处弹幕改插值、markdown 输出经 isomorphic-dompurify 消毒；`pnpm build` 验证通过）
- [x] 2.2 pnkx-ui 漏网 v-html：`views/px/chat/record/index.vue:120`（手写 regex 转 HTML）、`views/index.vue:134` 接 sanitizeHtml（2026-09-22：renderMarkdown 输出消毒 + safeMenstruation 计算属性）
- [x] 2.3 硬编码 token/地址：`nuxt.config.ts:29` AI embed token → 环境变量 AI_EMBED_URL（.env.dev/.env.prod 注入，未配置不加载脚本；**该 token 已在历史泄露，建议在 AI 平台重置**）；baseURL 兜底统一到 `src/utils/apiBase.ts` 一处（useHttp/AiAssistant/file.ts 三处引用）（2026-09-23）
- [x] 2.4 `.gitignore` 补 `.env.local` / `.env.*.local`（敏感本地覆盖不入库）；`.env.dev/.env.prod` 为部署必需的非敏感配置保留入库（2026-09-23）
- [ ] （pnkx-ui）token 存 localStorage（`utils/auth.js`）→ 至少 sessionStorage + 短有效期

## 阶段三：性能优化

### pnkx-ui（首屏 chunk 原 6.28MB → 2.20MB，2026-09-22 实测）
- [x] 3.1 `main.js` cherry-markdown/quill/x-markdown/vue-cropper 全局同步注册 → defineAsyncComponent 异步注册；prism.js/css（零引用）与 video-js.css（移入 Video 组件）移出入口
- [ ] 3.2 Element Plus 全量引入+全量图标 → unplugin-vue-components 按需（风险较高未做；element-plus chunk 1.07MB 为并行加载）
- [x] 3.3 582KB china.json → initRegionEcharts 内动态 import（2026-09-22）
- [x] 3.4 完整版 Vue alias 与无效 cytoscape alias 移除；chunkSizeWarningLimit 恢复 500（2026-09-22）
- [x] 3.5 死代码清理：live2d.js/waifu-tips/loveWord/prism、PnkxImageUpload/SelectButton/PanThumb/Tag/Region/Iframe/FileUpload 组件、favicon.ico 848KB→13KB（2026-09-22；注意 lodash 不能删——CherryMarkdownEditor/pinyin 仍在 require）
- [ ] 3.5b 后续项：4.29MB cherry 全家桶共享 chunk（mermaid/katex/hljs）可做 cherry 引擎裁剪；「验证码」路由 chunk 467KB 为内联 SVG 资源可外置

### pnkx-uniapp（主包原逼近 2MB 上限）
- [x] 3.6 glacier-aurora-bg.png 1.63MB → JPEG 153KB（质量 78，分辨率不变，9 处引用已更新）（2026-09-22）
- [x] 3.7 moment → dayjs（全局属性 $moment 保持兼容，唯一调用点 API 兼容）（2026-09-22）
- [x] 3.8 pages.json 加 lazyCodeLoading: requiredComponents；手动全量注册 30+ uni-ui 组件的 easycom 恢复未做（防 tree-shaking 误删的兜底，需真机验证后再动）

### 后端
- [x] 3.9 Redis KEYS → SCAN 游标（RedisCache.keys 实现，调用方无需改动）（2026-09-22）
- [x] 3.10 记账批量 N+1：insertBatchRecord 预加载分类 Map + 纪念日列表，3N 查询 → 2 次；加 @Transactional（2026-09-22）
- [x] 3.11 FTP 连接治理：无池化（null NPE 与日志泄密已在前序任务修复，池化暂缓——连接频率低收益有限）
- [x] 3.12 点赞幂等：`/client/wallpaper/like` GET→POST；新增 `PxWallpaperLikeService` 事务编排（pnkx-blog/pnkx-life 跨模块逻辑放 web 层）；Flyway V1.4.5 去重历史数据 + 唯一索引 uk_like_user_item(item_id,type,create_by) + 校正 like_count；并发重复点赞由 DuplicateKeyException 兜底；前端 file.ts 同步改 POST（2026-09-23）

## 阶段四：功能 Bug 修复

- [x] 4.1 uniapp upload.js 401 跳转 `/pages/login/login` → `/pages/login`（2026-09-22）
- [x] 4.2 uniapp 错误处理崩溃：request.js/upload.js fail 分支改 `error.errMsg || error.message`；upload.js JSON.parse 包 try/catch（2026-09-22）
- [ ] 4.3 AI 聊天 XHR（`api/px/ai/chat.js`）→ uni.request + enableChunked 跨端方案
- [x] 4.4 `pages/index.vue` 富文本 slice(10) 魔法数字 → 正则提取 img src（2026-09-22）

## 阶段五：工程化与代码质量（长期）

- [x] 5.1 GitHub Actions CI：`.github/workflows/ci.yml` 四个 job——后端 mvn package（JDK 21）、pnkx-ui 构建、pnkx-client 构建（pnpm 9）、uniapp 依赖校验（2026-09-23；推送 main 与 PR 触发）
- [ ] 5.2 核心链路补测试（登录/限额/路径校验）；手工脚本移出 test 源集（NotifyTest 已 @Ignore）（2026-09-23：文件管理器路径校验已补 8 个用例覆盖穿越向量——`SysFileManagerControllerTest`，并借此发现修复 `StringUtils.isEmpty` 对单 NUL 字符返回 true 的怪异行为；登录/限额测试需 Redis 环境，待补）
- [x] 5.3 Flyway baseline schema + DDL 迁移入库（2026-09-23：33 个迁移脚本筛查无密钥/用户数据后入库——.gitignore 改为 `!**/db/migration/*.sql`；V1.1.3 中一条私人网盘分享链接 INSERT 已移除）
- [x] 5.4 uniapp：确认 pnpm 为实际包管理器（node_modules 为 pnpm 结构，pnpm-lock.yaml 入库），删除本地多余的 package-lock.json 与 yarn.lock（均未被 git 跟踪）；移除 `opencode-supermemory`（全项目零引用，疑似 AI 工具误装）；编译链 alpha 版本锁定正式版未做（需 HBuilderX 真机验证，建议有条件时处理）（2026-09-23）
- [ ] 5.5 后端上帝类拆分（ExcelUtil 911 行、PxClientWallpaperController 893 行…）+ 业务逻辑下沉 service
- [ ] 5.6 前端超大组件拆分（pnkx-ui 最大 1772 行、uniapp 最大 1584 行）
- [x] 5.7 pnkx-ui 补 eslint+prettier（2026-09-23：`.eslintrc.cjs`（错误级仅真实缺陷/安全项，迁移债为 warn）+ `.prettierrc.json`；重写失效的 `.eslintignore`；修复全部 13 个存量错误——含订阅页调用不存在的 `exportSubscribe`、`utils/generator/js.js` 的 eval、v-for 缺 key 等；现 0 errors / 238 warnings，CI 已接入 lint 门禁）
- [x] 5.8 依赖升级：jsoup 1.10.3 → 1.17.2（CVE）；json-lib 2.4 → 移除，WxLoginController 迁移 fastjson2；devtools 实际已是 optional 且 Spring Boot repackage 默认排除，无需改（2026-09-23）
- [x] 5.9 异常处理规范化：11 处 printStackTrace → log.error（含补 Logger 声明）；CommonController 上传失败不再向客户端返回 e.getMessage()；ExceptionUtil 的 StringWriter 用法为正常实现保留（2026-09-23）
- [x] 5.10 杂项清理：删除 `D:\phy\1.pnkx\file\uploadPath` 目录（2026-09-22：已删除）；`diff_fire.sql` 移出仓库目录（2026-09-22：已移入 scripts/）；docs/ 截图压缩 5.1MB→1.3MB JPEG + README 引用更新（2026-09-23）；部署文档 `docs/DEPLOY.md`（环境变量清单/服务依赖/安全备忘）（2026-09-23）；按版本打 git tag（待发布时人工执行）
