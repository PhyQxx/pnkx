# pnkx-ui PC/APP 双端自适应改造计划（移植 pnkx-uniapp 功能）

> 文档版本：v1.1（2026-11-15）
> 范围：`code/pnkx-ui`（Vue3 + Element Plus 管理后台）
> 功能蓝本：`code/pnkx-uniapp`（uni-app 移动端，65 页）
> 核心诉求：**同一个 pnkx-ui，根据屏幕分辨率自动呈现 PC 后台效果或 APP 移动效果**，并把 pnkx-uniapp 的功能完整移植到 Web 端。
>
> **✅ 实施状态（2026-11-15）：Phase 0-4 已全部完成，Phase 5 完成（PWA/浏览器通知/提醒 WebSocket 为可选项未实施，见 §13）。**
> 交付：`src/mobile/` 移动应用树（63 个移动路由 / 90 个 .vue 文件）、设备分流守卫（768px 断点 + 强制覆盖）、Vant 4 按需集成（独立 chunk 114KB）、267 个业务 SVG 图标迁移、PC api 层补齐（actionCenter/automation/search/chargeInstance/mealPlan/recipe 等新模块 + reminder/report/shopping/menu 补端点）。
> 验证：`npm run build` 全量通过；`npm run check:mobile-map` 映射一致性通过；eslint 0 error；未登录态设备分流冒烟测试通过（本地无后端，登录后全功能回归需在带后端环境执行）。
> 开发指南：`pnkx-ui/docs/MOBILE_UI_GUIDE.md`；移植规范：`pnkx-ui/src/mobile/CONVENTIONS.md`。

---

## 目录

1. [背景与目标](#1-背景与目标)
2. [现状盘点](#2-现状盘点)
3. [总体方案（架构选型）](#3-总体方案架构选型)
4. [技术选型](#4-技术选型)
5. [Phase 0：基础设施改造详细设计](#5-phase-0基础设施改造详细设计)
6. [功能移植清单（分期、逐页）](#6-功能移植清单分期逐页)
7. [API 层对齐清单](#7-api-层对齐清单)
8. [组件映射表（uni-ui → Web 端）](#8-组件映射表uni-ui--web-端)
9. [App 专属功能的 Web 处置](#9-app-专属功能的-web-处置)
10. [测试与验收](#10-测试与验收)
11. [里程碑与工作量估算](#11-里程碑与工作量估算)
12. [风险与对策](#12-风险与对策)
13. [待确认决策点](#13-待确认决策点)
14. [附录：uniapp 65 页 → 移动路由全量映射](#14-附录uniapp-65-页--移动路由全量映射)

---

## 1. 背景与目标

### 1.1 背景

- **pnkx-uniapp** 是移动端（App/小程序/H5），功能极全：记账、待办、日记、笔记（含 Obsidian 文件库）、书城、网址收藏/RSS、购物清单、菜谱/膳食、订阅、账期实例、纪念日、情侣卡券、姨妈助手、提醒中心、家庭日历、生活报告、全局搜索、AI 助手、生活自动化、家庭/情侣空间等 **20+ 业务域、65 个页面**。
- **pnkx-ui** 是 RuoYi 风格 PC 管理后台（Vue3 + Vuex4 + Element Plus + Vite5），与 uniapp **共享同一后端**（`pnkx-admin :8068 /prod-api`），`src/views/px/life/*` 已有 PC 版 CRUD 页面，但完全没有移动端布局。
- 两个前端并存导致：手机浏览器访问 pnkx-ui 体验极差（144 处固定 px 弹窗、hover 依赖的快捷 dock/AI 悬浮窗、表格横滚）；而 uniapp H5 版与后台又是两套代码。

### 1.2 目标

1. **一套代码、两种体验**：pnkx-ui 根据视口分辨率自动切换 —— 宽屏呈现现有 PC 后台效果；窄屏呈现移植自 uniapp 的 APP 效果（底部 TabBar + 移动页面栈）。
2. **功能对齐**：uniapp 的 65 页功能（除 App 专属项，见 §9）全部可在 Web 端使用。
3. **不动 PC 现状**：PC 端现有页面、路由、菜单机制零回归（这是底线约束）。
4. **复用最大化**：后端 API、`src/api`、Vuex store、`utils/featureCapabilities`、SSE 解析器、设计令牌全部复用，移植以**视图层**为主。

### 1.3 非目标

- 不做 uniapp 离线同步（SQLite 写队列/冲突合并）的 Web 版 —— 浏览器恒在线，整体省略（§9.1）。
- 不做 WGT 热更新、App 推送登记、uniPush 路由（§9）。
- 不替换 PC 端现有布局/主题体系，只做增量。
- 不做小程序/微信 OAuth 登录（除非单独排期，见 §13 决策点 D4）。

---

## 2. 现状盘点

### 2.1 pnkx-uniapp 功能全景（移植蓝本）

**结构**：主包 5 页 + `pages_system` 12 页 + `pages_life` 47 页 + `staticPackage` 1 页（仅承载约 240 个拼音命名 SVG 图标），共 65 页。全量清单见 §14 附录。

**TabBar（3 个）**：`pages/index`（首页）、`pages/function/index`（生活助手）、`pages/mine/index`（我的）。

**业务域分组**（页面数）：

| 业务域 | 页面数 | 关键页 |
|---|---|---|
| 认证/个人中心 | 9 | launch、login、mine/*、avatar、pwd、family |
| 首页仪表盘 | 1 | index（原版/行动中心双视图、今日提醒聚合、FAB） |
| 功能导航 | 2 | function/index（后端菜单驱动）、lifeManager |
| 记账 | 9 | 记一笔（计算器键盘+AI 记账）、账目清单、账户、统计、预算、周期记账（账期实例） |
| 待办 | 2 | todo/index、todo/edit（分工转派、到期提醒） |
| 日记 | 3 | index（时间线/日历）、edit（心情天气选择器）、analysis |
| 笔记 | 5 | index、detail、folder、obsidian 文件浏览器、obsidianEdit |
| 书城 | 4 | 书架、详情、阅读器（主题/字号/进度）、编辑 |
| 网址收藏/RSS | 4 | 剪藏列表（AI 整理）、详情、编辑、RSS Feed 管理 |
| 购物/菜谱/膳食 | 5 | 购物清单 2 页（采购结算）、菜谱 2 页、膳食周计划 |
| 订阅 | 1 | 预测 + 账期实例 |
| 纪念日/卡券 | 4 | 纪念日 2 页（准备待办）、卡券 2 页 |
| 姨妈助手 | 4 | 状态 hero、记录、设置、统计 |
| 提醒中心 | 2 | 通知中心、偏好设置 |
| 家庭日历 | 1 | 月历聚合 + 驾驶舱 |
| 生活报告 | 1 | 周期报告 + AI 分析 + 行动快照 |
| 全局搜索 | 2 | 新版（AI 总结）、旧版全文检索 |
| AI 助手 | 1 | SSE 流式聊天 + 操作计划卡 + 会话管理 |
| 生活自动化 | 1 | 规则/模板/试运行/执行记录 |
| 通用 | 3 | webview、textview、（search 旧版） |

**V2 灰度功能**（由后端 `/life/features` 开关控制，PC 端已有同款 `FeatureGate` 机制）：actionCenterV2（行动中心）、operationPlanV2（操作计划）、chargeInstanceV2（账期实例）、shoppingPlanV2（购物结算）、reportActionV2（报告行动）。

**App 专属**（Web 裁剪/替代，详见 §9）：离线同步体系、uniPush 推送、WGT 热更新、微信小程序登录、SQLite/Camera 模块。

**关键机制**（Web 端对应物）：

| uniapp 机制 | Web 端现状/对策 |
|---|---|
| token 存 storage `App-Token`，`utils/request.js` 统一拦截 | PC `src/utils/request.js` 已有等价实现（同源后端），**直接复用** |
| SSE 流式聊天（XHR onprogress + `chatStreamParser`） | PC `AiAssistant` 已带同款 `chatStreamParser.js`，**直接复用** |
| `/life/features` 功能开关（30s 缓存） | PC `src/utils/featureCapabilities.js` + `FeatureGate` 已存在，**直接复用** |
| 提醒 WebSocket | PC `.env` 已有 `VUE_APP_SOCKET`（聊天室在用）；提醒未读数可先用轮询 |
| rpx 单位 / colorui / uni-ui | 换算为 px/vw + Vant4（§4、§8） |
| `staticPackage` 约 240 个拼音 SVG | 复制进移动端图标目录，独立 sprite 注册（§5.6） |

### 2.2 pnkx-ui 现状（承接面）

- **架构**：Vue 3.4 Options API 为主 + Vuex4 + Router4（`createWebHistory`）+ Element Plus 2.7 + Vite5；`@` → `src`；dev 端口 80，proxy `/dev-api` → `localhost:8068`。
- **路由**：静态 `constantRoutes`（login/404/index/profile/note-share/ai-tools/file-manager/wallpaper/book 阅读器/评论管理/gen 等）+ **后端菜单动态路由**（`store/modules/permission.js` 的 `GenerateRoutes` → `getRouters()` → `import.meta.glob('/src/views/**/*.vue')` 解析组件）。
- **布局**：唯一外壳 `src/layout/index.vue`（Sidebar + fixed-header Navbar + TagsView + AppMain + 右侧快捷 dock + 全局聊天室/AI 悬浮窗弹层）。
- **响应式地基（半成品）**：
  - `mixin/ResizeHandler.js`：992px 断点 → Vuex `app.device`（`desktop|mobile`），移动端侧栏变抽屉（`sidebar.scss .mobile` 规则）；
  - `DESIGN.md` §8 已定义目标断点：**≥1200 桌面 / 768–1199 平板 / <768 移动**，及移动规则（触控目标 36/40px、弹窗 `calc(100vw - 32px)` 上限、表格横滚）；
  - `assets/styles/design-tokens.scss` 的 `--pnkx-*` 令牌 + 明暗主题（`data-theme` 属性）；
  - 零散 `@media` 约 15 处（login 992px、首页工作台、阅读器 768px 等）。
  - **缺失**：无移动端 Layout/TabBar、无按设备切换路由的机制、144 处 el-dialog 固定 px 宽、快捷 dock 与 AiAssistant 悬浮窗依赖 hover。
- **已有 life 域 PC 页面**：`src/views/px/life/`（bookkeeping 4 页+Budget/Recurring 面板、calendar、card、commemorationDay、diary+analysis、lifeManager、meal、menstruationAssistant、note、recipe、reminder、report、shopping、subscription、todo、wallpaper），公共组件已有 ActionCenter/OperationPlans/ChargeInstances/ChargeRuleChangeFlow/ShoppingCheckout/ReportActions/ChatPlanCard/PlanDraftEditor/FeatureGate/SharePreview/AssignmentHistory 等 —— **API 层与多数 V2 组件可直接复用到移动端**。

### 2.3 差距结论

1. **API 层基本齐**：PC `src/api/px/life/*` 已覆盖大部分域，仅少数端点缺口（§7）。
2. **视图层是主要工作**：uniapp 的移动交互形态（TabBar、页面栈、计算器键盘、滑动操作、日历标记、阅读器）在 Web 端不存在，需按 uniapp 页面 1:1 移植。
3. **需要一个"APP 壳"**：MobileLayout + TabBar + 设备分流，这是整个改造的地基（Phase 0）。

---

## 3. 总体方案（架构选型）

### 3.1 方案比选

| 方案 | 说明 | 优点 | 缺点 | 结论 |
|---|---|---|---|---|
| **A. 双路由树 `/m/*` + MobileLayout + 设备自动分流** | PC 路由树不动；新增 `src/mobile/` 移动应用树（自有 Layout/页面/路由），顶层按设备重定向互跳 | PC 零回归；移动页面可从 uniapp 近乎 1:1 平移；IA 独立（TabBar+页面栈 vs 菜单+表格）；渐进交付（做完一页映射一页） | 需维护 PC↔APP 路由映射表；同功能两套视图 | ✅ **推荐** |
| B. 单路由树 + 组件变体（`xxx/mobile.vue`） | 动态路由解析时按 device 选组件变体 | URL 统一 | 动态路由组件在 `GenerateRoutes` 一次性解析，设备切换需重建路由树；uniapp 页面结构与菜单页 1:1 映射困难；菜单需后端配合 | ❌ |
| C. 现有页面全面响应式改造 | 只加媒体查询/栅格 | 无新页面 | 65 页 × CRUD 表格改造成卡片流工作量更大且达不到"APP 效果"；uniapp 交互（TabBar/FAB/滑动）无法用媒体查询表达 | ❌ |
| D. iframe 嵌 uniapp H5 构建 | 直接嵌 H5 产物 | 零移植 | 两套构建/部署；登录态打通脏；维护双端 | ❌ |

### 3.2 目标架构

```
pnkx-ui (单构建产物)
├── PC 端（现有，不动）
│   ├── src/layout/               # PC 外壳（Sidebar/Navbar/TagsView/dock）
│   ├── src/views/**              # PC 页面（现有 132 个 .vue）
│   └── 路由: constantRoutes + 后端菜单动态路由
├── APP 端（新增）
│   ├── src/mobile/
│   │   ├── layout/               # MobileLayout：自绘导航栏 + 页面栈 + 底部 TabBar
│   │   ├── views/**              # 移动页面（自 uniapp 移植，Vant4 实现）
│   │   ├── router/index.js       # /m 静态路由树 + TabBar 页 keep-alive
│   │   ├── router/map.js         # PC ↔ APP 路由映射表（分流/互跳用）
│   │   ├── components/           # 移动专属组件（Calculator、MoodWeatherPicker…）
│   │   └── assets/svg/           # 自 staticPackage 复制的业务 SVG 图标
│   └── 路由: /m/** 全静态（不依赖后端菜单）
├── 共享层（复用）
│   ├── src/api/**                # 后端接口模块（按 §7 补缺口）
│   ├── src/store/**              # user/permission/settings/tagsView + app(扩展 device)
│   ├── src/utils/**              # request/chatStreamParser/featureCapabilities…
│   └── src/assets/styles/design-tokens.scss  # --pnkx-* 令牌，移动端同源消费
└── 分流层（新增）
    ├── src/utils/device.js       # matchMedia 检测 + useDevice() + 手动覆盖记忆
    └── src/permission.js 改造     # 全局守卫按 device + 映射表重定向
```

### 3.3 断点与设备判定

| 视口宽度 | 呈现 | 说明 |
|---|---|---|
| **< 768px** | **APP 效果**（`/m/*` + MobileLayout） | 与 DESIGN.md §8 移动档对齐；也是"手机竖屏"的物理宽度 |
| 768 – 1199px | PC 效果（平板档） | 沿用现状：992px 起侧栏自动收为抽屉（`ResizeHandler` 现有行为不动） |
| ≥ 1200px | PC 效果（桌面档） | 现状 |

- 判定统一走 `window.matchMedia('(max-width: 767.98px)')`，SSR 无关（纯 SPA）。
- `app.device` 扩展为 `desktop | pad | mobile` 三态（现有 `mobile` 语义挪给 <768，992 侧栏抽屉改用独立的 `sidebarCollapsed` 判断），保持向后兼容 getter。
- **只按视口分辨率判定，不做 UA 嗅探**（iPad 横屏走 PC、竖屏接近方屏走 PC，符合"大屏即 PC"直觉）。

### 3.4 设备切换体验

1. **自动分流**：进入任意路由时，守卫按当前 device 决定去留 —— 手机访问 `/todo` → 重定向 `/m/todo`；桌面访问 `/m/todo` → 重定向对应 PC 路由（映射表反查，查不到则回 PC 首页）。
2. **实时切换**：窗口 resize 跨过 768 断点时（200ms 防抖），自动跳到当前页的对应形态，路由参数/查询尽量保留。
3. **手动覆盖**：`/m/mine/setting`（或 PC 设置抽屉）提供「电脑版 / 手机版」切换，写 localStorage（`pnkx-force-mode`），带 `?force=pc|mobile` URL 参数可临时强制；覆盖后不再自动切换，直至清除。
4. **降级策略**：映射表查不到对应页时——手机访问 PC 专属页（如代码生成、监控）→ 跳 `/m`（首页）并 toast「该功能请使用电脑版访问」；随移植进度，映射覆盖率逐步到 100%（§14）。

---

## 4. 技术选型

### 4.1 移动组件库：Vant 4（推荐）

- Element Plus 无移动交互组件（TabBar、动作面板、日历标记、滑动单元格、数字键盘），uni-ui 又不可在纯 Web 用。Vant 4（Vue3 原生、按需 tree-shaking、内置暗色 `ConfigProvider`）是主流选择。
- **共存策略**：Vant 仅在 `src/mobile/**` 内使用；PC 端 Element Plus 不动。样式天然隔离（Vant 组件类名 `van-*`），无需 unplugin 隔离插件。
- **按需引入**：新增 devDeps `unplugin-vue-components` + `@vant/auto-import-resolver`（推荐，模板内自动注册）；
- **Vite 配套**（`vite.config.js` 有 `optimizeDeps.include` 显式清单的仓库约定，需同步维护）：`optimizeDeps.include` 增 `vant`；`manualChunks` 增独立 `vant` chunk，避免混入 element-plus chunk；移动端弹层挂载点统一（`teleport` 到 mobile 容器）。
- 备选（不推荐）：全部用 Element Plus 硬凑移动交互 —— 无 TabBar/动作面板等组件，自研成本高于引入 Vant。

### 4.2 单位与样式

- uniapp 的 `rpx`（750 设计稿）移植时按 **1rpx = 0.5px**（375 基准）手工换算为 px；弹性布局用 flex/vw，不再引入 postcss 换算插件（页面为手工移植，非批量转译）。
- colorui 工具类（`text-grey`、`bg-white`、`cuIcon` 等）不移植，一律替换为 `--pnkx-*` 令牌 + Vant 组件默认样式。
- 移动端样式规范直接执行 DESIGN.md §8：内容 padding 12px、触控目标 ≥36px（主操作 40px）、圆角 8px、列表卡片化。

### 4.3 图标方案

- `staticPackage/static/svg/` 约 240 个拼音命名业务 SVG（记账分类 `c-*`、天气 `w-*`、心情 `x-*`、账户 `a-*`）**按需复制**（不是全量）到 `src/mobile/assets/svg/`。
- `vite-plugin-svg-icons` 的 `iconDirs` 支持数组：追加移动端目录，symbolId 用 `m-[name]` 前缀，与 PC 现有 `icon-[name]` 互不冲突；`SvgIcon` 组件加 `m-` 前缀用法。
- 不再依赖 uniapp 的 js-pinyin 运行时映射（Web 端直接 import 文件名）。

### 4.4 网络/实时

- HTTP：完全复用 PC `src/utils/request.js`（同后端、同 token、同错误码处理）。
- AI SSE：复用 PC `chatStreamParser`（`AiAssistant` 在用，与 uniapp 同源逻辑）。
- 提醒实时性：一期用现有接口轮询未读数（`/reminder/notifications/unread/count`，30s）；二期评估接 `.env` 已有的 `VUE_APP_SOCKET` WebSocket（聊天室同款通道）。

---

## 5. Phase 0：基础设施改造详细设计

> 产出：能在手机上打开 `/m` 看到 APP 壳（TabBar 三页占位），且桌面/手机互跳自动分流。不改任何 PC 页面行为。

### 5.1 设备检测 `src/utils/device.js`（新增）

```js
// 职责：
// - isMobileViewport(): matchMedia('(max-width: 767.98px)') 
// - getForcedMode(): localStorage['pnkx-force-mode']  // 'pc' | 'mobile' | null
// - resolveMode(): forced ?? (isMobileViewport() ? 'mobile' : 'pc')
// - watchMode(cb): 断点跨越监听（200ms 防抖），供 App.vue 全局订阅
```

- `store/modules/app.js`：`device` 改三态 `desktop|pad|mobile`，新增 `mode`（`pc|mobile`，含强制覆盖）；保留 `toggleDevice` 兼容旧调用。
- 现有 `ResizeHandler.js`（992 侧栏抽屉）保留但只管 PC 壳内部；跨壳切换职责移交给 `device.js` + 路由守卫。

### 5.2 MobileLayout `src/mobile/layout/index.vue`（新增）

结构（对照 uniapp 自定义导航模式）：

```
<mobile-layout>
  <van-config-provider :theme="dark ? 'dark' : 'light'" :theme-vars="pnkxVars">
    <nav-bar>            # 自绘：返回/标题/右侧动作（对应 uniapp 原生标题栏按钮：编辑/保存/新建）
    <router-view v-slot> # 页面栈，keep-alive 仅缓存 3 个 TabBar 页
    <tabbar v-if="isTabPage">   # 首页 / 生活助手 / 我的
    <back-to-top / offline-toast 等全局浮层>
  </van-config-provider>
</mobile-layout>
```

- TabBar 页路由 `meta: { tab: true }` → 显示 TabBar + keep-alive；非 Tab 页全屏入栈，左上角返回。
- 暗色主题：复用现有 `app.theme`（`data-theme` 属性），映射到 Vant `ConfigProvider`（`--pnkx-*` → `--van-*` 变量映射表放 `src/mobile/styles/vant-theme.js`）。
- **不渲染** PC 的 TagsView/Settings 抽屉/快捷 dock/AiAssistant 悬浮窗（那些都长在 PC `src/layout/index.vue` 内，天然隔离）。

### 5.3 移动路由树 `src/mobile/router/`（新增）

- 全静态路由，前缀 `/m`，路径与 uniapp 页面路径同构（去掉包前缀），例：

| uniapp 页面 | Web 路由 |
|---|---|
| `pages/index` | `/m`（重定向 `/m/home`） |
| `pages/function/index` | `/m/function` |
| `pages/mine/index` | `/m/mine` |
| `pages_life/bookkeeping/record/add` | `/m/bookkeeping/record/add` |
| `pages_life/ai/chat` | `/m/ai/chat` |
| `pages_system/mine/setting/index` | `/m/mine/setting` |

- 完整映射见 §14 附录（65 页全量）。
- 路由懒加载 `() => import(...)`；`meta.tab` 标记 3 个 TabBar 页。

### 5.4 分流守卫（改造 `src/permission.js`）

在现有守卫链（token 校验、GetInfo、GenerateRoutes）**之前**插入设备分流：

```js
router.beforeEach(async (to) => {
  const mode = resolveMode()                       // pc | mobile
  if (mode === 'mobile' && !to.path.startsWith('/m')) {
    return resolveMobileTarget(to)                 // 映射表正查；查不到 → /m + toast
  }
  if (mode === 'pc' && to.path.startsWith('/m')) {
    return resolvePcTarget(to)                     // 映射表反查；查不到 → PC 首页 /index
  }
  // ……原有逻辑（白名单放行 /note-share 等）
})
```

- `src/mobile/router/map.js`：`PC_ROUTE ↔ M_ROUTE` 双向映射（含参数占位，如 `/todo` ↔ `/m/todo`）。
- resize 跨断点：`App.vue` 订阅 `watchMode`，防抖后 `router.replace` 到当前路由的对应形态（若存在映射且未强制覆盖）。
- `tagsView` store 忽略 `/m` 路由（不产生 PC 标签页）。

### 5.5 Vant 集成与构建

1. `npm i vant`；`npm i -D unplugin-vue-components @vant/auto-import-resolver`。
2. `vite.config.js`：`Components({ resolvers: [VantResolver()] })` 限定 `dirs` 不扫描（仅 resolver）；`optimizeDeps.include` 增 `vant`；`manualChunks` 增 `vant` 独立 chunk。
3. `index.html` viewport 已是 `user-scalable=no`（APP 手感），保留；补 `<meta name="theme-color">` 跟随主题。
4. 移动壳内禁用 Element Plus 全局尺寸影响：MobileLayout 根节点加 `class="m-shell"`，必要时 `.m-shell { font-size: 14px }` 归一。

### 5.6 图标与令牌

- `vite-plugin-svg-icons.iconDirs` 追加 `src/mobile/assets/svg`，symbolId `m-[name]`。
- `src/mobile/styles/mobile.scss`：消费 `--pnkx-*` 令牌的移动基础样式（页面背景、卡片、safe-area（`env(safe-area-inset-*)`）、FAB、列表项 44px 高）。

### 5.7 Phase 0 验收

- [ ] 桌面 1280px 访问 `/` 行为与现状完全一致（无回归）。
- [ ] DevTools 375px 访问任意 PC 路由自动落 `/m` 三 Tab 壳（页面为占位卡）。
- [ ] 拖动窗口跨 768 断点，PC↔APP 互跳且不抖动（防抖生效）。
- [ ] `?force=pc` 在手机上可看 PC 壳；清除后恢复自动。
- [ ] vant 独立 chunk 体积合理（首屏 < 120KB gzip 增量预算）。

---

## 6. 功能移植清单（分期、逐页）

> 移植 = 以 uniapp 页面 `.vue` 为蓝本重写为 Web/Vant 版本：模板结构、交互、文案尽量 1:1；`uni.*` API 换 Web 等价（§8）；API 调用直接 import PC `src/api` 模块（缺口先按 §7 补）。
> 工作量为估算人日（含自测），蓝本复杂度已计入。

### Phase 1：骨架与认证（对应 uniapp 主包 + pages_system 基础）

| # | uniapp 蓝本 | 目标路由 | 要点 | 人日 |
|---|---|---|---|---|
| 1.1 | `pages/launch` | （不需要独立页） | Web 无闪屏；App.vue 首帧按 token 分流即可 | 0.5 |
| 1.2 | `pages/login` | 复用 `/login`（已 992px 适配） | 校验码已有；移动壳样式微调；**不做**微信快捷登录 | 1 |
| 1.3 | `pages/mine/index` | `/m/mine` | 头像/昵称头部 + 菜单 + 版本号 | 1.5 |
| 1.4 | `mine/avatar` | `/m/mine/avatar` | 手写裁剪器 → 复用 PC `EditPicture`(vue-cropper) 包一层移动交互 | 1 |
| 1.5 | `mine/info` + `info/edit` | `/m/mine/info`、`/m/mine/info/edit` | 只读列表 + 编辑表单（van-field） | 1.5 |
| 1.6 | `mine/pwd` | `/m/mine/pwd` | 表单（updateUserPwd） | 0.5 |
| 1.7 | `mine/setting` | `/m/mine/setting` | 菜单：改密/清缓存(localStorage+IndexedDB)/家庭空间入口/「切换电脑版」/**不含**检查更新、离线同步 | 1.5 |
| 1.8 | `mine/family` | `/m/mine/setting/family` | 空间列表/成员/共享预览（复用 SharePreview 组件，需移动适配） | 2 |
| 1.9 | `mine/help`、`mine/about` | `/m/mine/help`、`/m/mine/about` | 静态页 | 1 |
| 1.10 | `common/webview`、`textview` | `/m/webview`、`/m/textview` | webview→新窗口 `window.open` 或内嵌 iframe；textview→文本卡 | 1 |
| 1.11 | `pages/function/index`（生活助手） | `/m/function` | 后端菜单驱动宫格（`listByParams({isApp:'1'})`，`appPath` 优先映射到 `/m/*`），本地缓存 `bolgMenu`，emoji 兜底 | 2.5 |

小计 ≈ 14 人日

### Phase 2：核心生活四件套（首页 + 记账 + 待办 + 日记）

| # | uniapp 蓝本 | 目标路由 | 要点 | 人日 |
|---|---|---|---|---|
| 2.1 | `pages/index` 原版视图 | `/m/home` | 问候头部/搜索/通知铃铛/公告轮播/今日提醒聚合卡（getTodayReminders）/购物摘要（复用 HomeShoppingSummary 需适配）/今日账单/今日待办/FAB 四快捷 | 4 |
| 2.2 | ActionCenter 组件 | `/m/home` 内视图切换 | FeatureGate(actionCenterV2)：逾期置顶/提醒事件稍后/空间切换/pin 排序；复用 `utils/actionCenterState` 逻辑 + 移动模板 | 3 |
| 2.3 | `bookkeeping/index` | `/m/bookkeeping` | 月汇总 + 功能宫格 + 最近记录（复用 RecordList 组件移动版） | 2 |
| 2.4 | `bookkeeping/record/add` ★最重 | `/m/bookkeeping/record/add` | 支出/收入/转账 Tab、二级分类宫格、**自研计算器键盘**（移植 pages_life/components/Calculator）、账户选择、图片上传（input file→/common/upload）、记账模板、**AI 记账**（aiParse 填充） | 5 |
| 2.5 | `record/index` | `/m/bookkeeping/record` | 按月翻页流水 + 导出下载 | 1.5 |
| 2.6 | `account/*` 3 页 | `/m/bookkeeping/account`(+`/details`,`/maintenance`) | 总资产分组、账户卡、维护表单（IconSelect 移动版） | 3 |
| 2.7 | `statistics/index` | `/m/bookkeeping/statistics` | 6 组统计图：echarts 移动适配（复用 PC 图表配置逻辑） | 3 |
| 2.8 | `budget/index` | `/m/bookkeeping/budget` | 进度条卡片（budget.js） | 1 |
| 2.9 | `recurring/index` | `/m/bookkeeping/recurring` | 规则启停 + ChargeInstances 面板（复用组件移动适配） | 2 |
| 2.10 | `todo/index`、`todo/edit` | `/m/todo`、`/m/todo/edit` | 完成率统计/列表+日历双视图/标签筛选；编辑页执行者/时间/标签 + ReminderSetting + AssignmentHistory | 3.5 |
| 2.11 | `diary/index`、`edit`、`analysis` | `/m/diary`、`/m/diary/edit`、`/m/diary/analysis` | 时间线/日历、MoodWeatherPicker（x-/w- 图标移植）、万字正文、心情分析图 | 3.5 |

小计 ≈ 31.5 人日

### Phase 3：内容消费域（笔记/书城/收藏/购物/菜谱/膳食/订阅）

| # | uniapp 蓝本 | 目标路由 | 要点 | 人日 |
|---|---|---|---|---|
| 3.1 | `note/index`、`detail`、`folder` | `/m/note`、`/m/note/detail`、`/m/note/folder` | 面包屑/搜索/树形文件夹（van-collapse 递归） | 3 |
| 3.2 | `note/obsidian`、`obsidianEdit` | `/m/note/obsidian`、`/m/note/obsidian/edit` | fileManager 文件浏览/搜索/编辑预览双模式（mp-html→v-html+DOMPurify） | 3 |
| 3.3 | `book/` 4 页 | `/m/book`、`/m/book/detail`、`/m/book/reader`、`/m/book/edit` | 书架分页/TXT 导入（file input）/阅读器（主题/字号/进度保存）/章节目录 | 5 |
| 3.4 | `webArticle/` 4 页 | `/m/web-article`、`/m/web-article/detail`、`/edit`、`/rss` | 剪藏筛选（状态/分类/标签）、粘贴收藏（AI 整理）、阅读页（摘要卡+快照降级）、RSS Feed 管理 | 4 |
| 3.5 | `shoppingList/` 2 页 | `/m/shopping-list`、`/m/shopping-list/detail` | 清单进度/新建/采购日期；明细按履约分组勾选、ShoppingCheckout 结算（复用组件）、清空已勾选 | 3.5 |
| 3.6 | `recipe/` 2 页 | `/m/recipe`、`/m/recipe/detail` | 封面网格、食材/做法、加入膳食计划 | 2 |
| 3.7 | `mealPlan/index` | `/m/meal-plan` | 周视图×餐位槽、份数、生成本周购物清单（transferToShopping 预览/提交） | 3 |
| 3.8 | `subscription/index` | `/m/subscription` | 月/年预测、启停、编辑；ChargeInstances（chargeInstanceV2） | 2 |

小计 ≈ 25.5 人日

### Phase 4：智能与聚合域（AI/提醒/日历/报告/搜索/姨妈/纪念日/卡券/自动化）

| # | uniapp 蓝本 | 目标路由 | 要点 | 人日 |
|---|---|---|---|---|
| 4.1 | `ai/chat` ★最重 | `/m/ai/chat` | SSE 流式（复用 chatStreamParser）、Markdown 渲染（复用 PC XMarkDown 渲染管线或 marked+hl）、快捷提示词、会话历史恢复、pending 确认双按钮、ChatPlanCard+PlanDraftEditor、OperationPlans 面板 | 5 |
| 4.2 | `reminder/` 2 页 | `/m/reminder`、`/m/reminder/settings` | 通知中心（未读/全部/滑动删除/重试/全部已读）、渠道偏好+免打扰 | 2.5 |
| 4.3 | `calendar/index` | `/m/calendar` | 月历标记（van-calendar 定制或自绘网格）、选中日聚合事件、就地完成待办、快捷创建、驾驶舱 | 3.5 |
| 4.4 | `report/index` | `/m/report` | 周期/类型 Tab、汇总卡、AI 流式分析、ReportActions 快照 | 2.5 |
| 4.5 | `search/index`（新） | `/m/search` | 类型/日期/标签筛选、结果分组、AI 总结；旧版 fullRetrieval 不移植（PC 可达） | 2 |
| 4.6 | `menstruationAssistant/` 4 页 | `/m/menstruation/…` | 状态 hero、记录表单、设置、统计趋势 | 3 |
| 4.7 | `commemorationDay/` 2 页 | `/m/commemoration-day`、`/add` | 倒计时卡、编辑+ReminderSetting、CommemorationPreparations | 2 |
| 4.8 | `card/` 2 页 | `/m/card`、`/m/card/history` | 持有卡券、使用弹窗、确认/评分流转、使用记录 | 2 |
| 4.9 | `automation/index` | `/m/automation` | 规则列表/模板市场/启停/试运行/执行记录；JSON 编辑器（van-field textarea + 格式校验） | 2 |
| 4.10 | `lifeManager/index` | `/m/life-manager` | 六宫格入口（与 PC lifeManager 同构） | 0.5 |

小计 ≈ 23 人日

### Phase 5：Web 化适配与收尾

| # | 内容 | 人日 |
|---|---|---|
| 5.1 | 映射表补全至 §14 全量；无对应 PC 页的移动页（如 /m/bookkeeping/record/add）在 PC 端给出入口（快捷 dock/菜单跳 /m 链接） | 1 |
| 5.2 | 提醒未读数轮询 → WebSocket（可选，评估后端通道） | 2 |
| 5.3 | 浏览器 Notification API 对接提醒偏好（可选项，见 §13 D5） | 1.5 |
| 5.4 | PWA 基础（manifest + 图标，"添加到主屏幕"获得近 App 体验；不含 SW 离线） | 1 |
| 5.5 | 全量 QA（§10 矩阵）+ 修复 | 4 |
| 5.6 | 文档：更新 README/DESIGN.md §8 引用、新建 `docs/MOBILE_UI_GUIDE.md`（页面目录/映射表/开发规范） | 1 |

小计 ≈ 10.5 人日

---

## 7. API 层对齐清单

PC `src/api` 已有大部分；以下为对照 uniapp 后**需新增或补端点**的缺口（预计 1.5 人日统一补齐 + 联调）：

| 缺口 | 来源（uniapp） | 落点（pnkx-ui） |
|---|---|---|
| AI 会话管理：sessions/messages 增删查 | `api/px/ai/chat.js` | `src/api/px/ai/chat.js` 增补 |
| 全局搜索：`/admin/globalSearch`、`/summary` | `api/px/life/search.js` | 新建 `src/api/px/life/search.js` |
| 膳食计划：`/mealPlan/week`、transferToShopping | `api/px/life/mealPlan.js` | 新建（PC meal 页部分有，核对） |
| 书城：`/myBook/*`（章节/进度/TXT 导入） | `api/px/life/book.js` | PC 已有 `px/app/book.js`，核对补缺 |
| 记账模板：`/bookkeeping/recordModel` | `api/px/life/bookkeeping/model.js` | 新建 |
| 记账 AI：`/bookkeeping/record/aiParse` | `record.js` | PC bookkeeping/record 增补 |
| 提醒中心：notifications/unread/read/retry + preference | `api/px/life/reminder.js` | PC 已有部分，核对补缺 |
| 家庭日历：`/calendar/month`、`/calendar/cockpit` | `api/px/life/calendar.js` | PC 已有，核对 |
| 自动化：`/automation/rules|templates|run|dry-run|executions` | `api/px/life/automation.js` | 新建 |
| 生活报告行动：`/lifeReport/insights` | `report/index.js` | PC report 增补 |
| 首页聚合：`/reminder/today`（getTodayReminders） | `reminder.js` | 核对补缺 |
| 行动中心：`/life/action-center*`、纪念日 preview/commit | ActionCenter 组件内 | PC 组件已有，直接复用 |
| 账期变更流：`/charge/rules/change-*` | `chargeChange.js` | PC 已有 `px/life/chargeChange.js` ✅ |
| 购物结算：batches/checkout/claim/release/fulfillment | shoppingList.js + 组件 | PC 组件已有 ✅ |
| 邮件简单发送（about 页反馈用） | `email.js` | PC `system/email` 已有 ✅ |

> 原则：移动端**不新建平行 API 模块**，一律 import PC `src/api`；缺口补在 PC 目录。

---

## 8. 组件映射表（uni-ui → Web 端）

| uniapp / uni-ui | Web 移动端等价 | 备注 |
|---|---|---|
| `uni-popup` / `uni-popup-dialog` | Vant `Popup` / `Dialog` | 底部弹层 `position="bottom"` round |
| `uni-easyinput` / `uni-forms` | Vant `Field` / `Field+CellGroup` | 校验自行组织 |
| `uni-datetime-picker` / `uni-calendar` | Vant `DatePicker/TimePicker` / `Calendar` | 日历标记需在 `formatter` 自绘底点 |
| `uni-search-bar` | Vant `Search` | |
| `uni-list` / `uni-card` / `uni-tag` | Vant `Cell/CellGroup` / 卡片自绘 / `Tag` | 卡片用 `--pnkx-*` 令牌 |
| `uni-rate` / `uni-load-more` | Vant `Rate` / `List`（下拉加载）+自绘加载尾 | |
| `uni-swipe-action` | Vant `SwipeCell` | 通知中心滑动删除 |
| `uni-grid` / `uni-collapse` | Vant `Grid` / `Collapse` | |
| `uni-number-box` | Vant `Stepper` | |
| `uni-icons` | Vant `Icon` + PC `SvgIcon`（m- 前缀） | 业务图标走 SVG sprite |
| `mp-html` | `v-html` + DOMPurify（依赖已有）+ hl.js | AI 消息渲染 |
| `uni.chooseImage` | `<input type="file" accept="image/*">` + 现有 ImageUpload 逻辑 | |
| `uni.previewImage` | Vant `ImagePreview` | |
| `uni.setNavigationBarTitle` | 路由 `meta.title` + Navbar | |
| `onNavigationBarButtonTap` | Navbar 右侧 slot 按钮 | 编辑/保存/新建动作 |
| `onPullDownRefresh` | Vant `PullRefresh` | 仅列表页开启 |
| `uni.showToast/modal` | Vant `Toast/Dialog` | |
| `uni.getStorage*` | `js-cookie`/`localStorage`（PC `utils/auth` 已有 token 管理） | 键名沿用 PC 侧 |
| pages_life/components/Calculator | 原样移植（纯 Vue 组件，去 uni API） | 记账键盘 |
| components/SvgIcon（js-pinyin） | 构建期直接引用 `m-*.svg` symbol | §4.3 |

---

## 9. App 专属功能的 Web 处置

| # | App 功能 | Web 处置 | 理由 |
|---|---|---|---|
| 9.1 | 离线同步（SQLite、写队列、冲突合并、offlineProxy） | **整体不移植**；Web 恒在线 | `utils/network.js` 非 App 环境本就恒在线；契约测试仅作行为参考 |
| 9.2 | WGT/APK 热更新检查 | 不移植 | Web 部署即更新；设置页移除"检查更新" |
| 9.3 | uniPush 推送 + pushRouter 深链 | 一期无；Phase 5 可选浏览器 Notification + 站内轮询 | 见 §13 D5 |
| 9.4 | 微信小程序快捷登录（wx.login→/wx/login） | 不移植（Web 无 wx.login 环境） | 保留账号密码+验证码；若依 H5 OAuth 另行立项 |
| 9.5 | 相机/相册 | `<input type=file capture>` + 现有上传组件 | 记账图片、头像 |
| 9.6 | App 版本号展示（about/设置） | 显示 pnkx-ui package.json version | |
| 9.7 | 首页"App 更新下载进度弹窗" | 不移植 | 同 9.2 |
| 9.8 | 分包预载 preloadRule | 不需要 | Web 由路由懒加载+manualChunks 承担 |

---

## 10. 测试与验收

### 10.1 验收标准基线

- **DESIGN.md §8** 移动规则全量执行：内容 padding 12、触控目标 ≥36/40px、弹窗宽度 `≤ calc(100vw - 32px)`、表格横滚、固定操作不遮挡内容。
- **PC 零回归**：桌面 ≥768px 下全部现有页面行为与改造前一致（重点：侧栏抽屉 992、TagsView、动态菜单、全局弹层）。

### 10.2 设备切换矩阵（每期回归）

| 场景 | 期望 |
|---|---|
| 375×812 直达 `/todo` | 301→`/m/todo`（或映射目标），TabBar 可见 |
| 1440×900 直达 `/m/todo` | 重定向 PC 对应路由 |
| 页面中途拖拽窗口跨 768 | 防抖后自动切形态，路由参数保留 |
| `?force=pc`（手机） | 强制 PC 壳；清 localStorage 恢复 |
| 手机访问无移动版的 PC 页 | 落 `/m` 首页 + toast 提示 |
| 刷新/直接回退（bfcache） | 形态判定正确，无死循环重定向 |

### 10.3 功能测试

- 以 uniapp `tests/*.test.mjs` 15 个契约测试为行为基准（chat-stream、chat-plan-card、checkout-model、rule-change-flow、shopping-group、operation-plans 展示等），将纯逻辑部分在 pnkx-ui 下可运行的迁移过来（`node --test`，与现有 `scripts/check-*.mjs` 风格一致）。
- 新增 `scripts/check-mobile-route-map.mjs`：断言 `map.js` 映射双向完整、`/m` 路由都有懒加载组件（CI 型脚本，匹配仓库现有 validate-* 文化）。
- 手工回归沿用 `docs/DEVICE_REGRESSION_CHECKLIST.md` 流程并扩充移动项。

### 10.4 性能预算

- vant chunk 独立且 gzip 首屏增量 ≤ 120KB；移动首屏（/m/home）LCP ≤ 2.5s（本地代理环境）。
- TabBar 页 keep-alive 后来回切换无重复请求。

---

## 11. 里程碑与工作量估算

| 里程碑 | 内容 | 估算 | 累计 |
|---|---|---|---|
| M0 基础设施 | Phase 0 全部（设备检测/MobileLayout/路由树/守卫/Vant/图标/令牌） | 6 人日 | 6 |
| M1 骨架可用 | Phase 1（登录、我的、生活助手宫格、通用页） | 14 人日 | 20 |
| M2 核心四件套 | Phase 2（首页/记账9页/待办/日记） | 31.5 人日 | 51.5 |
| M3 内容域 | Phase 3（笔记/书城/收藏/购物/菜谱/膳食/订阅） | 25.5 人日 | 77 |
| M4 智能域 | Phase 4（AI/提醒/日历/报告/搜索/姨妈/纪念日/卡券/自动化） | 23 人日 | 100 |
| M5 收尾 | Phase 5（映射补全/QA/可选 WS+Notification/PWA/文档） | 10.5 人日 | 110.5 |

> 总计 ≈ **110 人日**（单人全职约 5 个月；Phase 2-4 各域页面相互独立，可 2 人并行压缩到 ~2.5 个月）。
> 最小可用切片（MVP）= M0+M1+2.1/2.4/2.10/2.11（首页+记一笔+待办+日记）≈ **46 人日**，即可先用起来。

---

## 12. 风险与对策

| 风险 | 影响 | 对策 |
|---|---|---|
| PC 端回归（守卫改造影响动态路由） | 高 | 守卫分流逻辑前置短路，仅 `/m` 前缀与移动视口才介入；每期跑 10.2 矩阵桌面项 |
| 记账 add 页/AI 聊天页移植失真（蓝本各 ~1000 行） | 高 | 以 uniapp 契约测试为基准；分段移植（先基础记账后 AI 解析；先流式渲染后会话管理） |
| 双组件库体积膨胀 | 中 | Vant 按需 + 独立 chunk + 首屏预算门禁（10.4） |
| 映射表漂移（新增页面漏配） | 中 | `check-mobile-route-map.mjs` 进 CI 型脚本；新页开发清单含"补映射"步骤 |
| 断点切换状态丢失（表单页跨断点跳转） | 中 | 切换前保留 query/params；对未保存表单弹确认（移动/PC 表单结构不同，不做跨形态草稿） |
| Element Plus 全局样式泄漏进移动壳 | 中 | MobileLayout 根节点作用域隔离验证；Vant teleport 挂载点统一 |
| SSE 在移动浏览器后台节流 | 低 | 页面隐藏时暂停渲染、恢复时补拉会话历史（uniapp 同策略） |
| echarts 移动端事件/字号 | 低 | 统计页图表配置抽公共函数，PC/移动两套尺寸参数 |

---

## 13. 待确认决策点

| # | 决策 | 推荐 | 备选 |
|---|---|---|---|
| D1 | 移动组件库 | **Vant 4**（按需） | 纯 Element Plus 自凑（不推荐，缺移动交互件） |
| D2 | PC/APP 切换断点 | **768px**（对齐 DESIGN.md §8） | 992px（更早进 APP 形态，平板竖屏也走 APP） |
| D3 | 架构 | **双路由树 `/m` + 自动分流（方案 A）** | 单树组件变体（方案 B，改造深、菜单耦合） |
| D4 | 微信登录 | 暂不做 | 若需 H5 微信 OAuth 需后端配套（另行立项） |
| D5 | 浏览器推送 | Phase 5 可选（Notification API + 轮询兜底） | 一期只做站内轮询红点 |
| D6 | 旧版全文检索页（pages_system/search） | 不移植（新版 /m/search 已覆盖+AI 总结） | 一并移植 |

---

## 14. 附录：uniapp 65 页 → 移动路由全量映射

> 蓝本列 = `pnkx-uniapp` 页面路径；目标列 = pnkx-ui 移动路由（Phase 见 §6）。`✂` = 不移植（§9）。

### 主包（5）

| 蓝本 | 目标路由 | Phase |
|---|---|---|
| pages/launch | （App.vue 首帧分流，无独立页） | P1 |
| pages/login | 复用 /login（移动适配微调） | P1 |
| pages/index | /m/home | P2 |
| pages/function/index | /m/function | P1 |
| pages/mine/index | /m/mine | P1 |

### pages_system（12）

| 蓝本 | 目标路由 | Phase |
|---|---|---|
| mine/avatar/index | /m/mine/avatar | P1 |
| mine/info/index | /m/mine/info | P1 |
| mine/info/edit | /m/mine/info/edit | P1 |
| mine/pwd/index | /m/mine/pwd | P1 |
| mine/setting/index | /m/mine/setting | P1 |
| mine/setting/sync | ✂ 离线同步（§9.1） | — |
| mine/setting/family | /m/mine/setting/family | P1 |
| mine/help/index | /m/mine/help | P1 |
| mine/about/index | /m/mine/about | P1 |
| common/webview/index | /m/webview（或直接 window.open） | P1 |
| common/textview/index | /m/textview | P1 |
| search/index（旧版检索） | ✂ 由新版 /m/search 覆盖（D6） | — |

### pages_life（47）

| 蓝本 | 目标路由 | Phase |
|---|---|---|
| ai/chat | /m/ai/chat | P4 |
| automation/index | /m/automation | P4 |
| book/index | /m/book | P3 |
| book/detail | /m/book/detail | P3 |
| book/reader | /m/book/reader | P3 |
| book/edit | /m/book/edit | P3 |
| bookkeeping/index | /m/bookkeeping | P2 |
| bookkeeping/record/add | /m/bookkeeping/record/add | P2 |
| bookkeeping/record/index | /m/bookkeeping/record | P2 |
| bookkeeping/account/index | /m/bookkeeping/account | P2 |
| bookkeeping/account/details | /m/bookkeeping/account/details | P2 |
| bookkeeping/account/maintenance | /m/bookkeeping/account/maintenance | P2 |
| bookkeeping/statistics/index | /m/bookkeeping/statistics | P2 |
| bookkeeping/budget/index | /m/bookkeeping/budget | P2 |
| bookkeeping/recurring/index | /m/bookkeeping/recurring | P2 |
| calendar/index | /m/calendar | P4 |
| card/index | /m/card | P4 |
| card/history | /m/card/history | P4 |
| commemorationDay/index | /m/commemoration-day | P4 |
| commemorationDay/add | /m/commemoration-day/add | P4 |
| diary/index | /m/diary | P2 |
| diary/edit | /m/diary/edit | P2 |
| diary/analysis | /m/diary/analysis | P2 |
| todo/index | /m/todo | P2 |
| todo/edit | /m/todo/edit | P2 |
| menstruationAssistant/index | /m/menstruation | P4 |
| menstruationAssistant/record | /m/menstruation/record | P4 |
| menstruationAssistant/settings | /m/menstruation/settings | P4 |
| menstruationAssistant/statistics | /m/menstruation/statistics | P4 |
| reminder/index | /m/reminder | P4 |
| reminder/settings | /m/reminder/settings | P4 |
| report/index | /m/report | P4 |
| search/index | /m/search | P4 |
| lifeManager/index | /m/life-manager | P4 |
| recipe/index | /m/recipe | P3 |
| recipe/detail | /m/recipe/detail | P3 |
| mealPlan/index | /m/meal-plan | P3 |
| shoppingList/index | /m/shopping-list | P3 |
| shoppingList/detail | /m/shopping-list/detail | P3 |
| subscription/index | /m/subscription | P3 |
| note/index | /m/note | P3 |
| note/detail | /m/note/detail | P3 |
| note/obsidian | /m/note/obsidian | P3 |
| note/obsidianEdit | /m/note/obsidian/edit | P3 |
| note/folder | /m/note/folder | P3 |
| webArticle/index | /m/web-article | P3 |
| webArticle/detail | /m/web-article/detail | P3 |
| webArticle/edit | /m/web-article/edit | P3 |
| webArticle/rss | /m/web-article/rss | P3 |

### staticPackage（1）

| 蓝本 | 处置 |
|---|---|
| pages/placeholder | ✂ 仅资源分包；所需 SVG 按需复制入 `src/mobile/assets/svg/`（§4.3） |

---

*本计划基于 2026-11-14 对 pnkx-ui（132 视图/RuoYi 动态路由/992px 半成品响应式）与 pnkx-uniapp（65 页/20+ 域/App 专属离线体系）的双端代码勘察编制；实现前请先裁决 §13 决策点。*
