# 网址剪藏 + AI 整理（px_web_article）设计方案

> 状态：一/二/三期及优化轮（V1.6.2）均已实施并通过验证（后端 clean 编译 + 46 项单测全绿、管理端构建通过、uniapp 语法校验通过）
> 关联模块：`pnkx-life`（四件套 + 异步任务 + RSS + 离线同步）、`pnkx-admin/web`（控制器 + AI 意图）、`pnkx-framework`（双线程池）、`pnkx-quartz`（重试 + RSS 任务）、`pnkx-ui`（管理端）、`pnkx-uniapp`（移动端）、`pnkx-ai`（AiClient 复用）
> 实施文件清单见文末

## 1. 功能定位

一个「网址剪藏 + AI 整理」功能（类似 Cubox / Readwise Reader 的轻量版）：

- 粘贴链接 → 后端异步抓取网页正文并保存快照到数据库
- AI 自动生成摘要、标签、分类
- 列表页支持搜索、标签筛选、已读/星标管理
- 每条记录可一键打开原文链接，也可在站内阅读快照（防原文失效）

## 2. 整体流程

```
用户提交 URL（管理端添加 / 后续：AI 对话、书签脚本）
        │  立即入库（fetch_status=待抓取），接口即刻返回
        ▼
异步任务：jsoup 抓取网页（UA 伪装 + 超时控制 + 2MB 上限）
        │
        ▼
提取 标题 / 正文 / 作者 / 发布时间 / 封面图 / favicon
HTML 经 jsoup Safelist 清理（XSS 防护）后存 content_html
        │
        ▼
异步任务：AiClient（复用 px_ai_model_config 模型配置）生成
摘要 summary + 标签 tags + 分类 category（结构化 JSON 输出）
        │
        ▼
前端列表：关键字搜索 / 标签筛选 / 已读星标筛选
详情页：站内阅读快照 + 「打开原文」外链
```

## 3. 数据库设计

新建 Flyway 迁移：`pnkx-admin/src/main/resources/db/migration/V1.5.9__web_article.sql`

表 `px_web_article`（遵循 `px_` 前缀 + BaseEntity 规范）：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK | |
| url | varchar(1024) UNIQUE | 归一化后的链接（去 utm 等跟踪参数） |
| domain | varchar(255) | 站点域名 |
| title | varchar(512) | 页面标题 |
| author | varchar(255) | 作者 |
| publish_time | datetime | 原文发布时间（meta 提取，可空） |
| cover_image | varchar(1024) | 封面图（og:image） |
| favicon | varchar(1024) | 站点图标 |
| content_text | longtext | 正文纯文本（AI 输入 + 搜索用） |
| content_html | longtext | XSS 清理后的 HTML 快照 |
| summary | varchar(2048) | AI 摘要 |
| tags | varchar(512) | AI 标签，逗号分隔 |
| category | varchar(64) | AI 分类（枚举：技术/资讯/工具/生活/学习/其他） |
| fetch_status | char(1) | 0待抓取 1成功 2失败 |
| fetch_error | varchar(1024) | 抓取失败原因 |
| ai_status | char(1) | 0待分析 1成功 2失败 |
| ai_error | varchar(1024) | AI 分析失败原因 |
| ai_model | varchar(128) | 分析所用模型 |
| read_flag | char(1) | 已读标记（0未读 1已读，默认0） |
| star_flag | char(1) | 星标（0否 1是，默认0） |
| source | char(1) | 来源：1手动添加 2AI对话 3书签脚本 4RSS订阅 |
| del_flag / create_by / create_time / update_by / update_time / remark | | BaseEntity 标准字段 |

索引：`url` 唯一索引；`create_time`、`category`、`domain` 普通索引。

## 4. 后端设计（pnkx-life 模块）

### 4.1 四件套（套用 wallpaper / book 模式）

```
pnkx-life/src/main/java/com/pnkx/life/
├── domain/po/PxWebArticle.java                      # extends BaseEntity, @Data
├── mapper/PxWebArticleMapper.java                   # MyBatis 接口
├── service/IPxWebArticleService.java
├── service/impl/PxWebArticleServiceImpl.java        # CRUD + 归一化去重
└── service/webarticle/PxWebArticleTaskService.java  # 异步抓取 + 正文提取 + AI 分析
pnkx-life/src/main/resources/mapper/PxWebArticleMapper.xml

pnkx-admin/src/main/java/com/pnkx/web/controller/life/
└── PxWebArticleController.java                      # /webArticle（需登录）
pnkx-framework/.../config/AsyncConfig.java           # 新增 webClipExecutor 线程池
```

### 4.2 核心服务逻辑

**addUrl(url, source)**：
1. URL 归一化（去 `utm_*`、`spm` 等跟踪参数）
2. 按 url 查重，已存在直接返回已有记录
3. 插入 `fetch_status=0, ai_status=0` 的记录，立即返回
4. 提交异步抓取任务

**fetchArticle(id)**（异步，`@Async` + 项目线程池）：
1. jsoup 连接：UA 伪装 Chrome、超时 10s、忽略 contentType、最大 2MB
2. 提取：`<title>` / og:meta / article 标签 / 正文节点
   - 正文提取策略：优先 `<article>` 节点；无则取文本密度最大的 `<div>`；再退化到 `<body>`
   - 正文纯文本 + 保留基本排版（p/h1-h6/strong/a/img/ul/ol/li/blockquote/pre/code）的 HTML
3. `Jsoup.clean(html, Safelist.basicWithImages())` 清理 XSS
4. 更新记录，`fetch_status=1`；异常则 `fetch_status=2` + `fetch_error`

**analyzeArticle(id)**（异步，抓取成功后触发）：
1. 组 prompt：`content_text` 截断前 6000 字符，要求输出 JSON
2. 调 `AiClient`（现有 `pnkx-ai` 模块），解析 JSON 容错（剥 ```json 围栏）
3. 更新 summary/tags/category，`ai_status=1`；失败 `ai_status=2`

**retryFetch(id) / retryAnalyze(id)**：人工触发重试（改回状态位后重跑异步方法）。

### 4.3 接口设计

管理端 `/webArticle`（需登录，全部按 create_by 隔离为当前用户私有数据）：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /list | 分页列表（keyword/tag/category/readFlag/starFlag/domain 筛选，不含正文大字段） |
| GET | /{id} | 详情（含 content_html / content_text） |
| POST | / | 收藏网址 {url, remark}，归一化去重后异步抓取分析 |
| PUT | / | 人工修正（标题/摘要/标签/分类/已读/星标/备注） |
| DELETE | /{ids} | 软删除（仅本人数据） |
| PUT | /read/{id}/{readFlag} | 标记已读/未读 |
| PUT | /star/{id}/{starFlag} | 星标/取消 |
| PUT | /refetch/{id} | 重新抓取正文并重新分析 |
| PUT | /reanalyze/{id} | 仅重新 AI 分析 |
| GET | /tags | 当前用户全部标签（筛选下拉用） |

> 权限说明：本功能挂在「我的应用」目录下（与「我的书城」同级，见 V1.2.9），菜单权限
> `px:app:webArticle:list`，随迁移脚本幂等插入并对拥有「我的应用」的角色自动授权。
> 用户隔离在 Mapper 层实现（所有读写 where create_by = 当前用户），与书城一致。
> 客户端 `/client/**` 接口移至二期（配合 uniapp 移动端一起做）。

### 4.4 关键技术点

- **XSS 清理**：第三方 HTML 入库前 `Jsoup.clean`，仅保留白名单标签，去 script/事件属性
- **大小限制**：正文超 2MB 截断；`content_text` 只保留纯文本
- **反爬兜底**：失败记 `fetch_error`，人工触发重试；抓不到正文时至少保留标题
- **URL 归一化去重**：去掉 utm_* / spm / from 等参数后作为唯一键
- **异步执行**：`@Async("webClipExecutor")`（`AsyncConfig` 中新增的独立线程池，避免与通知线程池互相阻塞），提交后立即返回；前端列表对 `fetchStatus=0 / aiStatus=0` 的记录每 5 秒静默轮询刷新
- **正文提取**：article > main > 文本量最大容器 > body，四级退化；纯文本转换复用 `PxBookHtmlText`
- **元数据提取**：og:meta / twitter:meta / 标准标签，发布时间用 hutool `DateUtil.parse` 容错解析

## 5. AI 分析（复用 pnkx-ai）

- 模型：走 `px_ai_model_config` 已配置的默认对话模型，`AiClient` 同步调用
- Prompt 要求严格 JSON 输出：`{"summary":"200字内摘要","tags":["标签1","标签2"],"category":"技术|资讯|工具|生活|学习|其他"}`
- 失败可重试；`ai_model` 记录所用模型
- 输出解析容错：剥 markdown 代码围栏、正则提取首个 `{...}` JSON 块

## 6. 前端（pnkx-ui，一期已实施）

```
pnkx-ui/src/api/px/app/webArticle.js                # axios API 封装
pnkx-ui/src/views/px/app/webArticle/index.vue       # 列表页
```

列表页功能（已实现）：
- 「收藏网址」对话框：贴链接 + 备注，提交即返回，后台异步抓取分析
- 筛选：关键字（标题/摘要/链接）、标签下拉（/tags 接口动态加载）、分类、已读状态、星标快捷筛选
- 表格列：标题（未读加粗+未读标）、AI 摘要两行预览、域名、分类 tag、标签、抓取状态、AI 状态（失败 tooltip 显示原因）、收藏时间、操作
- 操作：星标切换、查看（抽屉）、打开原文（新窗口）、编辑、重抓、重析、删除
- 详情抽屉：favicon/域名/作者/发布时间元信息、AI 摘要、备注、正文快照渲染（v-html，服务端已 XSS 清理）、「打开原文」与已读切换；打开详情自动标记已读
- 抓取/分析进行中的记录每 5 秒自动静默刷新列表

## 7. 分期规划

- **一期（已完成）**：建表 + 后端四件套 + 抓取/解析/清理 + AI 摘要标签分类 + 管理端 CRUD/列表/详情/打开原文 + 菜单权限
- **二期（已完成）**：
  - AI 助手对话剪藏：`WebClipHandler`（intent 体系自动注册，intent 名 `web_clip`），对话中发送链接即自动收藏（source=2），URL 正则提取 + 意图分类器 slots 双通道，剪藏幂等无需确认；关键词兜底（`IntentDetectionService` 已加 http/https/www./收藏网址等关键词）
  - 失败自动重试：`WebArticleRetryTask`（quartz，`webArticleRetryTask.execute`，每 30 分钟，V1.6.0 注册 sys_job），扫描失败超 10 分钟冷却且 retry_count < 3 的记录自动重试（抓取失败→重抓+重析；仅 AI 失败→重析）
  - 书签脚本：管理端「书签脚本」对话框生成 bookmarklet（拖到浏览器书签栏，任意网页一键剪藏），页面支持 `?url=&title=` 参数自动提交收藏
  - client 端接口：`PxClientWebArticleController`（`/client/webArticle`，全部端点登录校验 + 用户隔离），为 uniapp 移动端预留
- **三期（已完成）**：
  - uniapp 移动端：`pages_life/webArticle/index.vue`（列表：搜索/未读星标筛选/卡片/分页/下拉刷新/FAB 添加/5秒轮询）+ `detail.vue`（AI 摘要 + rich-text 正文快照 + 打开原文走通用 webview 页 + 星标/已读/复制链接），App 菜单由 V1.6.1 注册（「生活助手」目录下，`is_app='1'` + `app_path`，Web 端隐藏避免重复入口）
  - RSS 订阅自动收藏：`px_web_rss_feed` 表 + 四件套 + `rssFetchTask`（quartz 每 30 分钟，V1.6.1 注册），兼容 RSS 2.0 与 Atom，单源单轮最多 20 条、按旧到新收录，复用收藏链路自动去重 + AI 整理（source=4），管理端「RSS 订阅」抽屉管理订阅（新增/启停/立即抓取/删除）
  - 全文搜索：`title+summary+content_text` 联合 FULLTEXT 索引（ngram 中文分词），关键字筛选改为 `MATCH AGAINST OR LIKE` 混合（LIKE 兜底，ngram 不命中时仍可找到）
- **可选后续**：网页截图快照、标签管理页、收藏导出（Markdown/OPML）

## 8. 风险与约束

1. 反爬：需登录/加密页面抓不到正文 → `fetch_error` + 人工补录兜底（编辑对话框可改标题/摘要/标签）
2. 版权：快照仅自用存档，数据按用户隔离，不对公网展示他人内容
3. AI 成本：仅正文前 6000 字入 prompt；失败重试有人工触发兜底
4. 大字段：`content_html` LONGTEXT，列表查询一律排除该列（XML 中列裁剪），仅详情返回

## 9. 一期实施清单

| 文件 | 说明 |
|---|---|
| `pnkx-admin/src/main/resources/db/migration/V1.5.9__web_article.sql` | 建表 + 菜单 + 角色授权（幂等，含回滚说明） |
| `pnkx-life/.../domain/po/PxWebArticle.java` | 实体（含 keyword/tag 查询参数） |
| `pnkx-life/.../mapper/PxWebArticleMapper.java` + `resources/mapper/PxWebArticleMapper.xml` | Mapper（列表裁剪大字段、归属校验、软删除） |
| `pnkx-life/.../service/IPxWebArticleService.java` + `impl/PxWebArticleServiceImpl.java` | 业务层（URL 归一化、去重、触发异步） |
| `pnkx-life/.../service/webarticle/PxWebArticleTaskService.java` | 异步抓取提取 + AI 分析（webClipExecutor） |
| `pnkx-admin/.../web/controller/life/PxWebArticleController.java` | 管理端接口 |
| `pnkx-framework/.../config/AsyncConfig.java` | 新增 webClipExecutor 线程池 |
| `pnkx-ui/src/api/px/app/webArticle.js` | API 封装 |
| `pnkx-ui/src/views/px/app/webArticle/index.vue` | 列表 + 详情抽屉 + 编辑/收藏对话框 |

## 10. 二期实施清单

| 文件 | 说明 |
|---|---|
| `db/migration/V1.6.0__web_article_retry.sql` | retry_count 列 + sys_job 注册（幂等，含回滚说明） |
| `pnkx-life/.../domain/po/PxWebArticle.java`（增量） | retryCount 字段 |
| `pnkx-life/.../mapper/PxWebArticleMapper.java` + XML（增量） | selectRetryCandidates / incrementRetryCount |
| `pnkx-life/.../service/IPxWebArticleService.java` + Impl（增量） | addArticleByUrl(url, remark, source) 重载 |
| `pnkx-admin/.../web/service/intent/WebClipHandler.java` | AI 对话剪藏意图处理器（自动注册） |
| `pnkx-admin/.../web/service/intent/IntentDetectionService.java`（增量） | web_clip 关键词兜底 |
| `pnkx-quartz/.../task/WebArticleRetryTask.java` | 失败自动重试定时任务 |
| `pnkx-admin/.../web/controller/life/PxClientWebArticleController.java` | client 端接口（uniapp 预留） |
| `pnkx-ui/src/views/px/app/webArticle/index.vue`（增量） | 书签脚本对话框 + url 参数自动收藏 |

## 11. 三期实施清单

| 文件 | 说明 |
|---|---|
| `db/migration/V1.6.1__web_article_rss_and_search.sql` | RSS 表 + FULLTEXT 索引 + App 菜单 + sys_job（幂等，含回滚说明） |
| `pnkx-life/.../domain/po/PxWebRssFeed.java` | RSS 订阅实体 |
| `pnkx-life/.../mapper/PxWebRssFeedMapper.java` + XML | RSS Mapper |
| `pnkx-life/.../service/IPxWebRssFeedService.java` + `impl/PxWebRssFeedServiceImpl.java` | RSS 服务（XML 解析、兼容 RSS/Atom、防重） |
| `pnkx-life/.../mapper/PxWebArticleMapper.xml`（增量） | keyword 筛选加 FULLTEXT MATCH |
| `pnkx-admin/.../web/controller/life/PxWebRssFeedController.java` | RSS 管理接口（/webArticle/feed/**） |
| `pnkx-quartz/.../task/RssFetchTask.java` | RSS 定时抓取（rssFetchTask.execute） |
| `pnkx-uniapp/api/px/life/webArticle.js` | 移动端 API |
| `pnkx-uniapp/pages_life/webArticle/index.vue` | 移动端列表页 |
| `pnkx-uniapp/pages_life/webArticle/detail.vue` | 移动端阅读页 |
| `pnkx-uniapp/pages.json`（增量） | webArticle/index、webArticle/detail 注册 |
| `pnkx-ui/src/api/px/app/webArticle.js`（增量） | feed CRUD 接口 |
| `pnkx-ui/src/views/px/app/webArticle/index.vue`（增量） | RSS 订阅管理抽屉 |

## 12. 优化轮（V1.6.2）实施记录

### 缺陷修复（P0）

| 问题 | 修复 |
|---|---|
| RSS 定时任务无登录态，`SecurityUtils.getUserId()` 返回空串，收藏全部挂到空用户（用户看不到 RSS 收录的文章） | `addArticleByUrl` 增加显式 `userId` 参数（四参重载），RSS 链路传 `feed.createBy()`；空 userId 直接拒绝 |
| AiClient 对相同 prompt 缓存 1 小时，「重新分析」命中缓存返回旧结果 | 分析 prompt 追加 `分析批次：<timestamp>` nonce |
| 管理端详情 v-html 内 `<a>` 无 target，点击整页跳走后台 | 清理白名单 `addEnforcedAttribute("a","target","_blank")` + `rel="noopener"` |

### 健壮性（P1）

- **双线程池**：抓取（webClipExecutor，快）与 AI 分析（webArticleAiExecutor，单线程串行）解耦，RSS 批量场景分析任务排队而非并发轰炸模型 API，也不占住抓取线程；跨池投递经 `SpringUtils.getBean` 取代理
- **url_hash 去重键**（V1.6.2 迁移）：`MD5(url)` 全长唯一索引替换 `url(255)` 前缀索引，收藏表与 RSS 表都改，消除两个前 255 字符相同的长 URL 误判重复；存量数据已回填
- **手动重试重置 retry_count**：手动「重抓/重析」后重新获得 3 次自动重试资格
- **URL 尾部标点 trim**：归一化去除尾部 `.,;:!?` 及中文 `。，、」』」》）` 等，对话中「链接。结尾」不再吃进句号
- **移动端**：卡片长按支持复制链接/删除；正文 HTML 超 30 万字符自动降级纯文本模式（可手动切回渲染网页版），防止小程序 rich-text 卡顿

### 增强（P2）

- **正文提取升级**：干扰节点选择器扩展（nav/menu/sidebar/comment/share/related/recommend/breadcrumb 等 class/id 匹配）+ 链接密度评分（文本量 × (1−链接文本占比)），列表页/聚合页不再误抓导航区块
- **RSS 连续失败自动停用**：`consecutive_failures` 计数，达 3 次自动 `enabled=0` 并在管理端标红提示（红色「停用」tag + tooltip），避免无效轮询；成功后计数清零
- **导出**：管理端「导出」按钮导出全部收藏为 Markdown（标题/链接/分类/标签/AI摘要/正文）；RSS 抽屉「导出 OPML」可导入其他阅读器
- **离线同步接入**：`px_web_article`、`px_web_rss_feed` 加入 `PxOfflineSyncServiceImpl.EXTENDED_SYNC_TABLES` 白名单（模块名 webArticle / webRssFeed），移动端可增量拉取
- **单元测试**：`WebArticleUtilsTest` 18 项（URL 归一化/跟踪参数/尾部标点/链接提取/AI JSON 容错解析/RSS·Atom 解析），全模块 46 项测试全绿

### 涉及文件

- 迁移：`V1.6.2__web_article_url_hash.sql`
- 新增：`pnkx-life/.../service/webarticle/WebArticleUtils.java`、`WebArticleUtilsTest.java`
- 修改：`PxWebArticleServiceImpl`（显式 userId + hash 去重 + Markdown 导出）、`PxWebArticleTaskService`（双池 + nonce + target=_blank + 提取增强）、`PxWebRssFeedServiceImpl`（显式 userId + 去重 + 失败停用）、`AsyncConfig`（webArticleAiExecutor）、`WebClipHandler`、`PxOfflineSyncServiceImpl`、两个 Controller（导出端点）、两个 Mapper XML（url_hash / retry_count / 导出查询）、管理端 `webArticle.js` + `index.vue`、uniapp 两页面

## 13. 安全加固（V1.6.3）

| 问题 | 修复 |
|---|---|
| client 端登录校验空串绕过：`SecurityUtils.getUserId()` 未登录返回 `""` 而非 null，`== null` 判断失效，`/client/webArticle/{id}` 可匿名读取任意收藏详情（含正文） | `requireLogin()` 将空串归一为 null；涉及全部 client 端点 |
| SSRF：登录用户可让服务器抓取内网地址（127.0.0.1、169.254.169.254 云元数据、10/172.16-31/192.168 私有段、IPv6 本地段） | `WebArticleUtils.isInternalUrl()` 字面判断（含 localhost/.internal/组播保留段），收藏正文抓取与 RSS 拉取两条链路前置拦截，拒绝原因落入 fetch_error |
| 幽灵数据善后：若曾运行修复前的 rssFetchTask，库中存在 create_by='' 的不可见收藏 | `V1.6.3__web_article_cleanup_ghost.sql` 物理清理（未部署过则为空操作） |

单元测试增至 20 项（新增 SSRF 拦截正反用例），全绿。

## 14. 补全轮（3-6 项 + 浏览器插件）

### 长文分段分析
正文超 6000 字不再直接截断：按 5000 字/段（最多 4 段 ≈ 2 万字）逐段摘要，
再汇总生成最终摘要/标签/分类（`PxWebArticleTaskService.analyzeLongArticle`）。
单段失败跳过，全部段失败才算分析失败；汇总调用仍带 nonce 绕缓存。

### 运营统计（管理端「统计」抽屉）
- 后端 `GET /webArticle/stats`：总览（总数/未读/星标/来自RSS/抓取与AI成败率）+ 分类分布 + 近30天趋势
- 前端 echarts（复用 `@/utils/echarts` 按需加载）：趋势折线 + 分类环形图，成功率低于 90% 标红

### 标签管理（管理端搜索栏「标签管理」）
- `GET /webArticle/tags/stats`（标签+文章数）、`PUT /tags/rename`（重命名，与已有标签自动合并）、`DELETE /tags/{tag}`（批量移除）
- 逗号分隔标签在 Java 侧拆分/去重/回写（`PxWebArticleServiceImpl.renameTag/deleteTag`）

### 移动端补全（uniapp）
- 编辑页 `edit.vue`：修正标题/摘要/标签/分类/备注（长按卡片「编辑」或阅读页「编辑」入口）
- RSS 管理页 `rss.vue`：列表/新增/启停/立即抓取/删除，自动停用原因展示
- 列表筛选增强：分类横向 chips + 标签横向 chips（动态加载）
- FAB 改为菜单（收藏网址 / RSS 订阅）

### 浏览器插件（Chrome/Edge MV3，`code/extension/web-clipper/`）
- popup：当前页标题/链接 + 备注 + 收藏；options：服务器地址 + 账号密码 + 测试连接
- 认证：无验证码登录接口 `POST /login` 换 JWT（仅存 chrome.storage.local），401 自动重登录
- 快捷键 `Ctrl/Cmd+Shift+S` 静默收藏 + 系统通知；`host_permissions` 声明免除 CORS
- 调用 `/client/webArticle`（已修复空串绕过，按用户隔离）
- 安装/使用说明见 `web-clipper/README.md`

### 验证
后端 clean 编译 + 20 项单测全绿；管理端 eslint+build 通过；uniapp 四页面/插件全部 JS 语法校验通过。
