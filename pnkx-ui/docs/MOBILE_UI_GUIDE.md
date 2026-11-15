# pnkx-ui 移动端（APP 形态）开发指南

> 双端架构：pnkx-ui 单构建产物，按视口分辨率自动切换 **PC 后台效果 / APP 移动效果**。
> 总体设计见 `docs/PC_APP_DUAL_LAYOUT_PLAN.md`，移植规范见 `src/mobile/CONVENTIONS.md`。

## 1. 形态切换机制

| 视口 | 形态 | 路由树 |
|---|---|---|
| < 768px | APP（底部 TabBar + 页面栈） | `/m/**`（静态路由，`src/mobile/router/index.js`） |
| ≥ 768px | PC（侧栏 + 标签页） | 现有后台路由（不变） |

- 判定：`src/utils/device.js` 的 `matchMedia('(max-width: 767.98px)')`，与 DESIGN.md §8 移动档对齐；PC 壳内部 992px 侧栏抽屉行为保持不变。
- 分流：`src/permission.js` 全局守卫前置拦截——手机访问 PC 路由自动跳 `/m` 对应页（无映射时落 `/m/home` 并提示）；桌面访问 `/m` 路由跳回 PC 对应页。映射表：`src/mobile/router/map.js`。
- 实时切换：`App.vue` 监听 `modeRef`，拖拽窗口跨断点 200ms 防抖后自动互跳。
- 手动覆盖：localStorage `pnkx-force-mode`（`pc`/`mobile`），URL `?force=pc|mobile` 设置；「我的-应用设置-切换电脑版」写 `pc`；清除该键恢复自动识别。
- 登录页两端共用 `/login`（已有 992px 适配），token 存取复用 `@/utils/auth`。

## 2. 目录结构

```
src/mobile/
├── CONVENTIONS.md            # 移植硬性规范（新人必读）
├── layout/index.vue          # MobileLayout：van-nav-bar + 页面栈 + van-tabbar（3 Tab）
├── router/index.js           # /m 路由树（懒加载；meta.tab=TabBar页并 keep-alive；meta.plain=隐藏默认导航栏）
├── router/map.js             # PC↔APP 映射（精确表+关键词表）+ uniapp appPath 转换
├── router/guards.js          # isMobilePath / NEUTRAL_PATHS
├── styles/mobile.scss        # 共享样式类 + Vant 主题变量对齐 --pnkx-* 令牌
├── utils/ui.js               # Toast/Dialog 函数式反馈唯一入口（含样式引入）
├── utils/icons.js            # 业务 SVG 图标（源自 uniapp staticPackage，267 个）
├── utils/version.js          # 版本/站点信息
├── components/               # 跨域共享组件（ReminderSetting、AssignmentHistory）
├── assets/                   # tabbar 图标 / 冰川极光背景 / logo / svg 图标库
└── views/<domain>/           # 各业务域页面（域内 components/ 放私有组件）
```

页面组件的 `name` 必须与路由 `name` 一致（TabBar 页 keep-alive 依赖）。

## 3. 技术要点

- **组件库**：Vant 4（按需，`unplugin-vue-components` + `VantResolver`，仅解析 `van-*` 标签）；与 Element Plus 共存互不影响；独立 `vant` chunk。
- **样式**：只用 px + `--pnkx-*` 设计令牌（自动跟随明暗主题）；共享类 `m-page/m-card/m-cell-card/m-section-title/m-grid/m-stat-bar/m-fab/m-sticky-actions/m-form/m-empty/m-pull/m-rich-content` 等；触控目标 ≥36px；弹窗 ≤ `calc(100vw - 32px)`。
- **主题**：`app.theme`（`data-theme`）→ MobileLayout 的 `van-config-provider` 同步暗色。
- **API**：全部复用 PC `src/api/**`（同后端 `prod-api`）；缺口补在 PC 对应模块，不建平行目录。
- **功能开关**：`<feature-gate feature="xxxV2">`（actionCenterV2/operationPlanV2/chargeInstanceV2/shoppingPlanV2/reportActionV2）。
- **图标**：`iconUrl('c-xxx')`（记账分类 c-*、天气 w-*、心情 x-*、账户 a-*）。
- **跳转兜底**：目标路由可能分批上线，跳转前 `this.$router.resolve(path).matched.length > 0` 判断，未注册 toast「功能即将上线」。

## 4. 移动路由全表

| 路由 | name | 页面 | 蓝本（pnkx-uniapp） |
|---|---|---|---|
| /m/home | MHome | 首页（原版/行动中心双视图） | pages/index + ActionCenter |
| /m/function | MFunction | 生活助手（后端菜单宫格） | pages/function/index |
| /m/mine | MMine | 我的 | pages/mine/index |
| /m/mine/avatar | MMineAvatar | 修改头像 | mine/avatar |
| /m/mine/info、/m/mine/info/edit | MMineInfo(Edit) | 个人信息/编辑 | mine/info |
| /m/mine/pwd | MMinePwd | 修改密码 | mine/pwd |
| /m/mine/setting | MMineSetting | 应用设置（主题/切电脑版/清缓存） | mine/setting（去 App 专属项） |
| /m/mine/setting/family | MFamily | 家庭与情侣空间 | mine/setting/family |
| /m/mine/help、/m/mine/about | — | 常见问题/关于 | mine/help、about |
| /m/webview、/m/textview | — | 通用网页/文本页 | common/* |
| /m/bookkeeping | MBookkeeping | 生活账本主页 | bookkeeping/index |
| /m/bookkeeping/record/add | MBookkeepingRecordAdd | 记一笔（计算器+AI 记账） | bookkeeping/record/add |
| /m/bookkeeping/record | MBookkeepingRecord | 账目清单 | bookkeeping/record/index |
| /m/bookkeeping/account(+details/maintenance) | MBookkeepingAccount* | 账户三页 | bookkeeping/account/* |
| /m/bookkeeping/statistics | MBookkeepingStatistics | 账目统计 | bookkeeping/statistics |
| /m/bookkeeping/budget | MBookkeepingBudget | 预算管理 | bookkeeping/budget |
| /m/bookkeeping/recurring | MBookkeepingRecurring | 周期记账+账期实例 | bookkeeping/recurring |
| /m/todo、/m/todo/edit | MTodo(Edit) | 待办 | todo/* |
| /m/diary、/m/diary/edit、/m/diary/analysis | MDiary* | 日记三页 | diary/* |
| /m/note、/m/note/detail、/m/note/folder | MNote* | 笔记三页 | note/* |
| /m/note/obsidian、/m/note/obsidian/edit | MNoteObsidian(Edit) | Obsidian 文件库 | note/obsidian* |
| /m/book(+detail/reader/edit) | MBook* | 书城四页 | book/* |
| /m/web-article(+detail/edit/rss) | MWebArticle* | 网址收藏四页 | webArticle/* |
| /m/shopping-list(+detail) | MShoppingList* | 购物清单两页 | shoppingList/* |
| /m/recipe(+detail)、/m/meal-plan | MRecipe*/MMealPlan | 菜谱/膳食计划 | recipe/*、mealPlan |
| /m/subscription | MSubscription | 订阅管理+账期 | subscription |
| /m/ai/chat | MAiChat | AI 助手（SSE 流式） | ai/chat |
| /m/reminder(+settings) | MReminder* | 提醒中心两页 | reminder/* |
| /m/calendar | MCalendar | 家庭日历 | calendar |
| /m/report | MReport | 生活报告+AI 分析 | report |
| /m/search | MSearch | 全局搜索+AI 总结 | search（新版） |
| /m/menstruation(+record/settings/statistics) | MMenstruation* | 姨妈助手四页 | menstruationAssistant/* |
| /m/commemoration-day(+add) | MCommemorationDay* | 纪念日两页 | commemorationDay/* |
| /m/card(+history) | MCard* | 情侣卡券两页 | card/* |
| /m/automation | MAutomation | 生活自动化 | automation |
| /m/life-manager | MLifeManager | 生活管家六宫格 | lifeManager |

不移植：离线同步（App 专属）、WGT 热更新、uniPush、微信小程序登录、旧版全文检索页。

## 5. 守护脚本

- `npm run check:mobile-map` → `scripts/check-mobile-route-map.mjs`：断言 map.js 与 router/index.js 双向一致（新页面必须同步补映射）。
- 构建断言 `vant` 独立 chunk（首屏增量预算 ≤120KB gzip）。

## 6. 已知边界

- 无后端/弱网下各页由全局 request 拦截器弹错（与 PC 行为一致）。
- 表单页跨断点切换不做草稿保留（切换前有确认或直接跳转）。
- 浏览器通知/推送未接入（提醒走站内轮询），见计划 §13 D5。
