# pnkx-ui（含 PC/APP 双端）全量自动化测试方案

> 文档版本：v1.1（2026-11-15）
> 被测对象：`code/pnkx-ui` —— PC 后台 + 移动端 `/m` 双形态（63 移动路由 / 132 PC 视图 / 共享 api+store+utils）
> 目标：**不依赖真实后端**即可在本地与 CI 全量跑通「静态守护 → 纯逻辑 → 端到端 → 视觉回归」四层自动化，覆盖设备分流机制与全部业务域关键路径。
>
> **✅ 实施状态（2026-11-15）：已完成并全量跑通——206 项检查全部通过（75 单元/契约 + 91 功能 E2E + 40 视觉基线），0 失败。**
> 实施结果与发现的产品缺陷清单见 `docs/AUTOMATED_TEST_REPORT.md`。与方案差异：视觉基线首版 40 张（light 32 + dark 8）一次成型；记账/生活域用例在实施中按真实页面结构细化。

---

## 目录

1. [现状与原则](#1-现状与原则)
2. [分层架构与选型](#2-分层架构与选型)
3. [目录结构](#3-目录结构)
4. [L0 静态守护](#4-l0-静态守护)
5. [L1 纯逻辑与契约测试（node:test）](#5-l1-纯逻辑与契约测试nodetest)
6. [L3 端到端测试（Playwright + Mock Server）](#6-l3-端到端测试playwright--mock-server)
7. [E2E 全量用例矩阵](#7-e2e-全量用例矩阵)
8. [L4 视觉回归](#8-l4-视觉回归)
9. [CI 集成](#9-ci-集成)
10. [里程碑与工作量估算](#10-里程碑与工作量估算)
11. [风险与对策](#11-风险与对策)
12. [验收标准](#12-验收标准)

---

## 1. 现状与原则

**已有资产**：
- CI：`.github/workflows/ci.yml` 的 `pnkx-ui` job（lint + build）
- 守护脚本：`scripts/validate-light-workbench.mjs`、`validate-design-system.mjs`、`check-pagination-sync.mjs`、`check-mobile-route-map.mjs`（本次新增）
- 蓝本契约测试：`pnkx-uniapp/tests/*.test.mjs` 16 个（node:test 风格），其中 12 个测的纯逻辑 util 在 pnkx-ui `src/utils/` 有同源副本（chatStreamParser、actionCenterState/Mutations、checkoutModel、ruleChangeFlow、shoppingGroups、planEditing、chatHistoryPolicy、operationPlanPresentation、featureCapabilities、sharedCache）

**原则**：
1. 沿用仓库 `node --test` 文化，**不引入 vitest/jest**（避免双测试栈；组件交互由 E2E 覆盖）
2. E2E **不依赖真实后端**：自建轻量 Mock Server 占用 8068（vite dev proxy 天然指向），fixtures 全量可离线跑
3. PC 端现有页面只做冒烟级回归（防零回归底线），移动端全量用例
4. 视觉回归只锁移动端主页面（375px × 明/暗），控制基线维护成本

## 2. 分层架构与选型

| 层 | 工具 | 覆盖 | 触发 |
|---|---|---|---|
| L0 静态守护 | build + eslint + 既有 validate/check 脚本 | 编译期错误、路由映射一致、设计令牌漂移 | `npm run verify`（本地+CI） |
| L1 纯逻辑/契约 | node:test（零新依赖） | device/map/icons + 12 个同源契约测试移植 | `npm run test:unit` |
| L3 端到端 | **Playwright**（新 devDep）+ Mock Server(node:http) | 设备分流、认证、65 页关键路径、SSE 流式 | `npm run test:e2e` |
| L4 视觉回归 | Playwright `toHaveScreenshot` | 移动端 ~40 个主页面 × 明/暗基线 | `npm run test:visual`（CI 仅 chromium） |

选型说明：
- **Playwright 而非 Cypress**：原生支持多视口/触控模拟（设备分流的 resize 断言是核心场景）、双浏览器上下文并行、`page.route` 兜底拦截。
- **独立 Mock Server 而非 page.route 全拦截**：SSE（AI 聊天/生活报告/日记分析）需要真实分块推送，`route.fulfill` 一次性 body 无法模拟流式；Mock Server 用 `http.createServer` 写 chunked response 即可，且 fixtures 路由表对所有用例共享。
- **不做后端集成测试**：后端测试已在 `ci.yml` backend job（mvn test），前端仓只测前端。

## 3. 目录结构

```
pnkx-ui/
├── playwright.config.mjs           # 双 project：mobile(375×812,touch) / desktop(1280×800)
├── tests/
│   ├── unit/                       # L1：node:test
│   │   ├── device.test.mjs
│   │   ├── route-map.test.mjs
│   │   ├── icons.test.mjs
│   │   └── contracts/              # 从 uniapp 移植的 12 个契约测试（import 改相对路径）
│   ├── mock-server/
│   │   ├── server.mjs              # 监听 :8068，加载 fixtures 路由表，支持 SSE
│   │   ├── fixtures/
│   │   │   ├── auth.json           # getInfo/getRouters/captchaImage
│   │   │   ├── features.json       # /life/features 各开关组合
│   │   │   ├── bookkeeping.json    # 分类/账户/流水/统计/预算/账期
│   │   │   ├── life.json           # todo/diary/note/shopping/recipe/mealPlan/subscription/...
│   │   │   ├── ai.json             # 会话/消息/操作计划
│   │   │   └── ...
│   │   └── sse.mjs                 # SSE chunked 响应工具（可编程时序）
│   └── e2e/
│       ├── helpers/
│       │   ├── auth.mjs            # 注入 Admin-Token + 预热 GetInfo/GenerateRoutes
│       │   ├── nav.mjs             # openMobile(path)/openDesktop(path)/resizeTo()
│       │   └── feature-flags.mjs   # 切换 V2 开关组合（改 fixtures 热态）
│       └── specs/
│           ├── 00-device.spec.mjs  # 设备分流矩阵（最高优先级）
│           ├── 01-auth.spec.mjs
│           ├── 02-home.spec.mjs
│           ├── 03-bookkeeping.spec.mjs
│           ├── 04-todo-diary.spec.mjs
│           ├── 05-note-book.spec.mjs
│           ├── 06-webarticle-shopping.spec.mjs
│           ├── 07-ai.spec.mjs
│           ├── 08-reminder-calendar-report-search.spec.mjs
│           ├── 09-life-domains.spec.mjs      # 姨妈/纪念日/卡券/自动化/管家/膳食/菜谱/订阅
│           ├── 10-pc-regression.spec.mjs     # PC 端冒烟（防零回归）
│           └── visual.spec.mjs               # L4 快照
└── tests/e2e/__screenshots__/      # 视觉基线（提交入库）
```

## 4. L0 静态守护

1. 新增聚合命令：`"verify": "npm run lint && npm run build && npm run check:mobile-map && npm run validate:ui"`
2. 断言（既有+新增）：
   - build 成功；`vant-*.js` chunk ≤ 150KB raw（在 check-mobile-route-map.mjs 末尾追加 dist 体积断言，若 dist 存在）
   - eslint **error=0**（警告不阻塞，存量迁移债）
   - map.js ↔ router/index.js 双向一致（已有）
   - 设计令牌/轻工作台锚点（已有 validate 脚本）
3. CI 已有 lint+build，补 `check:mobile-map`、`validate:ui` 两条 step。

## 5. L1 纯逻辑与契约测试（node:test）

### 5.1 新写用例（`tests/unit/`）

| 文件 | 用例数 | 覆盖 |
|---|---|---|
| `device.test.mjs` | 8 | matchMedia 768 断点翻转 → modeRef；强制覆盖 setForcedMode/URL ?force；resolveMode 优先级；localStorage 不可用时降级 |
| `route-map.test.mjs` | 15 | resolveMobileTarget：精确表命中/关键词优先级（bookkeeping 先于 book、note 不误伤 note-share）/无映射 fallback；resolvePcTarget：精确/剥尾段/兜底 /index；uniappPathToMobile：全 key 表/带 query/旧路径推导；NEUTRAL_PATHS 放行 |
| `icons.test.mjs` | 3 | iconUrl 命中 c-*/w-*/a-*；未命中返回空串；iconNames 含 267 项且有序 |

（device.js/map.js 依赖浏览器 API 的部分，用最小 stub：`global.matchMedia`/`global.localStorage` 注入。）

### 5.2 契约测试移植（`tests/unit/contracts/`，12 个）

从 `pnkx-uniapp/tests/` 复制并改 import 为 `../../../src/utils/*.js` 相对路径：

| 移植自 | 被测 PC 同源 util | 说明 |
|---|---|---|
| chat-stream.test.mjs | utils/chatStreamParser.js | SSE 解析（message/operation-plan 事件） |
| chat-history.test.mjs | utils/chatHistoryPolicy.js | 会话持久化/PENDING_CONFIRM 判定 |
| chat-plan-card.test.mjs | utils/operationPlanPresentation.js | 计划卡展示状态机 |
| plan-editing.test.mjs | utils/planEditing.js | schema 草稿编辑 |
| action-center-state.test.mjs | utils/actionCenterState.js | 行动中心状态 |
| action-center-mutations.test.mjs | utils/actionCenterMutations.js | 变更队列 |
| checkout-model.test.mjs | utils/checkoutModel.js | 购物结算模型 |
| shopping-groups.test.mjs | utils/shoppingGroups.js | 购物分组 |
| rule-change-flow.test.mjs | utils/ruleChangeFlow.js | 账期三步变更 |
| feature-capabilities.test.mjs | utils/featureCapabilities.js | 开关缓存 |
| shared-cache.test.mjs | 对应共享缓存 util | 30s 缓存 |
| operation-plan-presentation.test.mjs | utils/operationPlanPresentation.js | （若与 chat-plan-card 重复则合并） |

不移植：offline-sync-contract / online-actions / mobile-runtime-contract（App 离线专属，Web 已裁剪）。

> 移植前先逐个确认 PC util 与 uniapp util **逐字节同源**（chatStreamParser 已确认）；有差异的以 PC 版行为为基准修期望。

命令：`"test:unit": "node --test tests/unit/"`

## 6. L3 端到端测试（Playwright + Mock Server）

### 6.1 基建设计

```
npm run test:e2e
  ├─ webServer[0]: node tests/mock-server/server.mjs   # :8068（vite dev proxy 目标）
  ├─ webServer[1]: npm run dev                         # :80 vite（env.development → /dev-api）
  └─ Playwright: projects = [mobile(375×812,hasTouch), desktop(1280×800)]
```

- **Mock Server**：原生 `node:http`，路由表 = `fixtures/*.json` + 少量行为端点：
  - `POST /login` 校验 fixtures.auth 凭据返回 token；`/captchaImage` 返回占位
  - `GET /getInfo`、`GET /getRouters`（PC 动态菜单最小集：/index + life 各菜单）
  - `GET /life/features`：读运行时开关（支持 `POST /__test/features` 切换组合，用例内热切换 V2 开关）
  - CRUD 端点读写内存态（从 fixtures 深拷贝初始化，用例间隔离：每 worker 重置）
  - `POST /ai/chat/stream`、`GET /lifeReport/stream`、`/diary/analysis/stream`：SSE 分块（可编程时序/含 operation-plan 事件）
  - 统一响应壳：`{code:200,msg:'',data|rows|total}` 与后端一致；可注入 500/401 场景（`POST /__test/fail/{pattern}`）
- **鉴权 helper**：`page.addInitScript` 预置 `localStorage.Admin-Token`，避免每条用例走登录表单；登录表单流程单独一个 spec 覆盖。
- **崩溃断言**：所有用例 `page.on('pageerror')` 收集，afterEach 断言无未捕获异常；控制台 error（网络 4xx/5xx 除外）同样收集。

### 6.2 设备分流矩阵（00-device.spec.mjs，10 条，P0）

| # | 场景 | 期望 |
|---|---|---|
| D1 | mobile 视口直达 `/index` | 302 链最终 URL `/m/home`，TabBar 3 项可见 |
| D2 | mobile 直达 `/m/bookkeeping/record` | 停留原路径（不再跳） |
| D3 | mobile 直达 PC 专属页 `/tool/gen` | 落 `/m/home` + toast「该功能请使用电脑版访问」 |
| D4 | desktop 直达 `/m/todo` | 重定向 PC 对应路由 |
| D5 | desktop 直达 `/m/mine/setting/family`（有精确映射） | 跳 `/system/dataGroup` |
| D6 | `/m/home` 页面拖拽 375→1280 | 200ms 防抖后 URL 变为 `/index`，无抖动循环 |
| D7 | 1280→375 反向 | 回 `/m/home` |
| D8 | `?force=pc`（mobile 视口） | 保持 PC 壳；localStorage 写入 pnkx-force-mode=pc |
| D9 | 清除 force 键 + resize | 恢复自动分流 |
| D10 | `/note-share`（mobile 视口） | 直接渲染，不重定向 |

### 6.3 认证（01-auth.spec.mjs，4 条）

无 token 访问 /m → 落 /login；登录表单提交成功 → /m/home；Mock 返回 401 → 全局提示 + 清 token；修改密码后强制登出回登录页。

## 7. E2E 全量用例矩阵

> 每条 = 页面 + 操作 + 断言；`【F:flag】`= 需 feature flag 组合。合计 ≈ 78 条。

### 02-home.spec.mjs（7 条）
| # | 用例 | 期望 |
|---|---|---|
| H1 | /m/home 渲染 | 问候语含用户名；今日提醒/购物摘要/今日账单/今日待办 4 卡渲染 fixtures 数据 |
| H2 | 公告轮播 | 轮播项可点击弹详情 |
| H3 | 铃铛未读数 | mock unread=3 → 红点显示 3，点击跳 /m/reminder |
| H4 | FAB 四入口 | AI/日记/待办/记账分别跳对应 /m 路由 |
| H5 | 视图切换持久化 | 切「行动中心」→ 刷新仍为行动中心（localStorage pnkx-home-view） |
| H6 |【F:actionCenterV2】行动中心 | 逾期置顶区/待办就地完成/空间切换 pin 排序保存（reload 后保持） |
| H7 | actionCenterV2=off | 无切换 chip，仅原版视图 |

### 03-bookkeeping.spec.mjs（12 条）
| # | 用例 | 期望 |
|---|---|---|
| B1 | /m/bookkeeping 主页 | 月汇总数字 = fixtures 聚合；6 宫格入口可达；最近流水 5 条 |
| B2 | 记一笔：支出 | 类型 Tab/二级分类宫格/计算器输入 25.5/选账户/保存 → mock 库新增，toast 成功，返回列表可见 |
| B3 | 记一笔：转账 | 转账 Tab 双账户选择，保存后类型=转账 |
| B4 | 记一笔：AI 解析 | 输入「午餐花了30块」→ aiParse 回填分类+金额 |
| B5 | 记一笔：校验 | 金额为空保存被拦截 |
| B6 | 编辑回填 | 从清单页点流水进编辑，金额/分类/备注回填一致，修改保存生效 |
| B7 | 账目清单 | 月份翻页加载 fixtures 第二页；月汇总更新；导出按钮触发下载 |
| B8 | 账户管理 | 总资产=分组求和；新增账户（名称/类型/图标选择器）→ 列表出现 |
| B9 | 账户详情/维护 | 详情流水列表；编辑账户改名生效 |
| B10 | 统计 | 收支汇总卡 + 趋势图/分类环图 canvas 渲染（非空 canvas，无 console error） |
| B11 | 预算 | 进度条百分比=fixtures；超支标记红；新建分类预算 |
| B12 |【F:chargeInstanceV2】周期记账+账期 | 规则启停 switch 调 mock；账期面板筛选/确认入账/扣费失败重试；编辑走三步向导（预览→影响→提交） |

### 04-todo-diary.spec.mjs（10 条）
| # | 用例 |
|---|---|
| T1 | /m/todo 渲染：完成率环 + 4 统计 Tab 计数 = fixtures |
| T2 | 新建待办（内容/时间/标签/执行者）→ 列表出现，勾选完成 → 完成率变化 |
| T3 | 筛选：全部/进行中/今日/完成 各自列表正确 |
| T4 | 日历视图：标记点渲染，点日期过滤 |
| T5 | 标签筛选 + 搜索（防抖） |
| T6 | 编辑页：ReminderSetting 绑定提醒（lead time 选择→bind 调用）；AssignmentHistory 转派（选人+原因→transfer 调用） |
| T7 | /m/diary：时间线/日历双视图切换；统计卡数字 |
| T8 | 写日记：心情(x-*)/天气(w-*)选择器图标渲染；保存→时间线出现 |
| T9 | 日记编辑回填 + 删除确认 |
| T10 | 心情分析：分布条形图渲染；本月/全部切换数字变化；AI 洞察 SSE 流式逐字出现（mock 分块） |

### 05-note-book.spec.mjs（9 条）
| # | 用例 |
|---|---|
| N1 | /m/note：文件夹面包屑导航逐级进出；子文件夹统计 |
| N2 | 新建笔记（选文件夹/正文万字上限）→ 列表出现；编辑保存 |
| N3 | 文件夹管理：树形展开/新建/改名/删除（有笔记时删除拦截） |
| N4 | Obsidian：目录浏览/面包屑；搜索结果点击进编辑 |
| N5 | Obsidian 编辑：md 预览渲染（标题/列表/代码块）；保存→writeFile 调用；未保存离开弹确认 |
| N6 | /m/book 书架：状态 Tab/搜索/分页 |
| N7 | TXT 导入：setInputFiles 假 txt → import 调用 → 书架出现新书 |
| N8 | 阅读器：plain 全屏（无默认导航栏）；字号 4 档/4 主题切换持久化；上一章/下一章/目录跳转；进度保存 API 调用 |
| N9 | 书籍编辑：表单保存/删除确认 |

### 06-webarticle-shopping.spec.mjs（10 条）
| # | 用例 |
|---|---|
| W1 | 收藏列表：状态 Tab/分类/标签 chips 筛选组合正确 |
| W2 | FAB 粘贴网址收藏 → addArticle → 抓取中轮询（mock fetchStatus=0→1）→ 列表出现 |
| W3 | 星标/已读切换即时生效（乐观更新 + API 调用） |
| W4 | 阅读页：AI 摘要卡 + 正文 DOMPurify 渲染（script 被清除）；打开原文新窗口；超大正文降级纯文本提示 |
| W5 | 编辑收藏：标签/分类/摘要保存 |
| W6 | RSS：列表状态/失败原因展示；启停/立即抓取/新增/删除 |
| W7 | /m/shopping-list：清单进度 done/total；新建清单；设置采购日期 |
| W8 | 清单详情：添加条目；按履约状态 4 分组；勾选移动分组；清空已勾选 |
| W9 |【F:shoppingPlanV2】结算面板：多选→生成批次→记支出→mock 库变更 |
| W10 | 条目操作：认领/释放/转派（复用 AssignmentHistory） |

### 07-ai.spec.mjs（6 条）
| # | 用例 |
|---|---|
| A1 | /m/ai/chat 渲染：欢迎态 + 快捷提示词 chips |
| A2 | 发送消息：SSE 流式逐字渲染（mock 分块时序）；完成后 markdown 渲染（标题/代码高亮/列表） |
| A3 | SSE operation-plan 事件 → ChatPlanCard 出现（步骤/字段/执行按钮） |
| A4 | 草稿编辑：PlanDraftEditor schema 字段（boolean switch/date picker/multiline）修改后执行 |
| A5 | pending-action：双按钮「确认保存/取消」分别调 confirm/cancel |
| A6 | 会话管理：新建会话（清空+服务端删除）；刷新恢复最近会话历史；【F:operationPlanV2】OperationPlans 面板列表/确认执行/重试 |

### 08-reminder-calendar-report-search.spec.mjs（10 条）
| # | 用例 |
|---|---|
| R1 | 提醒中心：未读/全部 Tab + 角标数；点开已读；滑动删除；失败重试；全部已读 |
| R2 | 提醒偏好：渠道开关保存；免打扰时段 picker |
| R3 | 家庭日历：月历网格 42 格；类型彩点与图例；选中日事件列表聚合 fixtures |
| R4 | 日历：待办就地完成；快捷创建跳转对应 /m 页 |
| R5 | 驾驶舱卡数据渲染 |
| R6 | 生活报告：周期/类型 Tab；汇总三卡；AI 报告 SSE 流式 + AbortController 中断按钮 |
| R7 |【F:reportActionV2】ReportActions：生成快照→候选行动→预览确认→忽略 |
| R8 | 全局搜索：关键词+类型 chips+日期范围；结果按类型分组 |
| R9 | 搜索 AI 总结（mock 同步返回）markdown 渲染 |
| R10 | 通知跳转路由（todo/纪念日/订阅/经期 4 类 deep link 落正确页面） |

### 09-life-domains.spec.mjs（10 条）
| # | 用例 |
|---|---|
| M1 | 姨妈助手：状态 hero（经期中/安全期文案来自 fixtures+字典）；日历标记/列表双视图 |
| M2 | 经期记录：新增（互斥开关/体温体重）/编辑回填/删除 |
| M3 | 经期设置：周期 21-35 stepper 边界；提醒入口 |
| M4 | 经期统计：均值卡 + 趋势条 |
| M5 | 纪念日：列表倒计时；新建（重复每年/图标选择）；编辑态 ReminderSetting + 准备待办面板 |
| M6 | 情侣卡券：持有张数；使用弹窗→张数-1；待确认/待评分流转（confirmCard/scoreCard） |
| M7 | 使用记录分页 + 确认/评分操作 |
| M8 | 自动化：模板市场横滑；规则启停/试运行（dry-run 结果展示）/执行记录/失败重放；编辑器 JSON 校验按钮（非法 JSON 拦截） |
| M9 | 菜谱库/详情（食材/做法/加入膳食计划）；膳食计划周视图增删菜品、份数 |
| M10 |【F:shoppingPlanV2】膳食「生成本周购物清单」预览→提交；订阅管理预测卡/启停/编辑；【F:chargeInstanceV2】订阅账期面板 |

### 10-pc-regression.spec.mjs（6 条，desktop 视口）
| # | 用例 |
|---|---|
| P1 | 登录 → PC /index 工作台渲染（账单/待办/日历/购物 4 区） |
| P2 | 992px 以下侧栏自动收抽屉；汉堡展开 |
| P3 | 后端菜单动态路由：侧栏渲染 getRouters 菜单项，点开 /px/life/todo 表格页 |
| P4 | TagsView 多标签打开/关闭 |
| P5 | 快捷 dock 六入口弹出层（文件管理/聊天室等 dialog 打开） |
| P6 | 明暗主题切换：data-theme 属性翻转 + 令牌色变化 |

## 8. L4 视觉回归

- `visual.spec.mjs`：逐页 `expect(page).toHaveScreenshot()`，视口 375×812，禁用动画，等字体/图片加载。
- 基线对象：3 个 Tab 页 + 18 个域主页面（记一笔/清单/账户/统计/预算/周期/待办/日记/分析/笔记/Obsidian/书架/阅读器/收藏/购物/菜谱/膳食/订阅/AI/提醒/日历/报告/搜索/姨妈/纪念日/卡券/自动化/管家/我的/设置）≈ 30 页 × 明/暗 2 套 ≈ **60 张基线**（首版先 light 全量 + dark 仅 8 个代表页，dark 全量二阶段）。
- 快照失败 diff 上传 CI artifact；基线更新走 `npm run test:visual -- --update-snapshots` + 人工 review diff。

## 9. CI 集成

`ci.yml` 的 `pnkx-ui` job 扩展：

```yaml
      - name: 静态守护
        run: npm run check:mobile-map && npm run validate:ui
      - name: 单元/契约测试
        run: npm run test:unit
      - name: 安装 Playwright
        run: npx playwright install --with-deps chromium
      - name: E2E（chromium，含视觉基线）
        run: npm run test:e2e && npm run test:visual
      - uses: actions/upload-artifact@v4   # 视觉 diff 与 trace
        if: failure()
        with: { name: playwright-report, path: pnkx-ui/playwright-report }
```

- Playwright 报告（trace/screenshot on failure）本地 `npx playwright show-report`
- 时长预算：E2E chromium ≈ 78 用例 × ~3s / 4 workers ≈ 2-3 分钟，CI 可接受

## 10. 里程碑与工作量估算

| 里程碑 | 内容 | 估算 |
|---|---|---|
| MT1 基建 | Playwright 安装/config/双 project、Mock Server（fixtures 全域 + SSE + 行为开关）、auth/nav helpers、`verify`/`test:*` scripts | 3 人日 |
| MT2 L1 | device/map/icons 26 用例 + 12 个契约测试移植（含 import 修正与 PC 行为对齐） | 2 人日 |
| MT3 E2E-P0 | 00-device + 01-auth + 02-home（21 条） | 2 人日 |
| MT4 E2E 全域 | 03-09 spec（57 条，fixtures 补齐按域推进） | 6 人日 |
| MT5 PC 回归 + 视觉 | 10-pc-regression + 首版基线 38 张 | 2 人日 |
| MT6 CI | ci.yml 扩展 + artifact + 文档 | 1 人日 |
| **合计** | | **≈ 16 人日**（fixtures 制作约占 4 人日，与用例同步进行） |

## 11. 风险与对策

| 风险 | 对策 |
|---|---|
| mock fixtures 与真实后端字段漂移 | fixtures 字段以 uniapp 契约测试与后端文档（docs/API_*.md）为基准；L1 契约测试锁纯逻辑，漂移只影响数据展示断言 |
| SSE/流式 mock 时序脆弱 | sse.mjs 可编程时序（chunk 延迟参数化）；断言只看「最终内容 + 至少 2 次增量」，不锁精确时序 |
| 8068 端口与本地真后端冲突 | test:e2e 前置检测端口占用并明确报错提示（脚本内 net.connect 探测）；CI 无此问题 |
| 视觉基线跨平台抖动（字体渲染） | CI 仅 linux chromium 跑视觉；本地更新基线需在 CI 同镜像（后续可加 docker image 固定） |
| keep-alive 页面状态串扰 | 每条用例独立 context（Playwright 默认）；Tab 页切换用例显式 reload 隔离 |
| 用例数增长导致 CI 变慢 | spec 级并行 workers=4；视觉回归独立 job 可拆 |

## 12. 验收标准

1. `npm run verify` → 0 error；`npm run test:unit` → 38+ 用例全绿
2. `npm run test:e2e` → 78+ 用例全绿（无真实后端、无网络外呼）
3. `npm run test:visual` → 基线齐全，无意外 diff
4. CI `pnkx-ui` job 全绿且总时长增量 ≤ 4 分钟
5. 故意注入一个回归（如删掉守卫分流逻辑）→ 00-device 立刻红（有效性验证，交付前演示一次）

---

*实施顺序建议：MT1 → MT2 → MT3（先锁设备分流核心）→ MT4/MT5 → MT6。确认本方案后即可开工。*
