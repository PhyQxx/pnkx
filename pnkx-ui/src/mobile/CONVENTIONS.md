# 移动端页面移植规范（src/mobile）

> 本文档是给「uniapp → pnkx-ui 移动端」移植工作的强约束规范。所有移动端页面必须遵守。

## 目录约定

```
src/mobile/
├── layout/index.vue        # MobileLayout 外壳（导航栏+TabBar，勿改）
├── router/index.js         # 路由注册（统一由主控维护，子任务勿改）
├── router/map.js           # PC↔APP 映射表（统一由主控维护）
├── styles/mobile.scss      # 共享样式类（勿改，可用下面列出的类）
├── utils/ui.js             # Toast/Dialog 等函数式反馈（唯一入口）
├── utils/icons.js          # 业务 SVG 图标 iconUrl(name) / iconNames
├── utils/version.js        # 版本号
├── components/             # 跨域共享移动组件（按任务分配创建）
└── views/<domain>/         # 各业务域页面 + 域内 components/ 子目录
```

## 硬性规则

1. **Options API**（与工程一致），组件 `name` 必须与路由 name 一致（keep-alive 依赖）。
2. **Vant 组件**：模板里直接写 `<van-xxx>`（unplugin 自动按需引入，无需 import）。
3. **函数式反馈**只用 `@/mobile/utils/ui`：`showToast / mSuccess / mError / mConfirm / mLoading / closeToast`。**禁止**直接 `import { showToast } from 'vant'`（会丢样式）。
4. **禁止** uni.* API / rpx / colorui 类 / uni-ui 组件。uniapp API 转换表：
   - `uni.showToast` → `showToast/mSuccess/mError`
   - `uni.showModal` → `mConfirm(title, message)`（Promise，catch=取消）
   - `uni.showLoading/hideLoading` → `mLoading()/closeToast()`
   - `uni.navigateTo({url})` → `this.$router.push(path)`（query 传参）
   - `uni.navigateBack()` → `this.$router.back()`
   - `onLoad(options)` → `mounted()` + `this.$route.query`
   - `onPullDownRefresh` → `<van-pull-refresh>`
   - `onReachBottom` → `<van-list>` 的 load 事件
   - `uni.chooseImage` → `<input type="file" accept="image/*">`
   - `uni.previewImage` → Vant `showImagePreview`（从 vant import，样式在 `@/mobile/utils/ui` 已含 toast/dialog；ImagePreview 需自带 `import 'vant/es/image-preview/style'`）
   - `uni.getStorageSync/setStorageSync` → `localStorage`（JSON 自行序列化）
   - `uni.setNavigationBarTitle` → 路由 `meta.title`（不动态改）
5. **API**：一律 `import { xxx } from '@/api/...'`（PC 端 api 模块）。缺失的端点按 uniapp 对应 api 文件补到 PC 的 src/api 对应文件（保持 request 风格一致）。**不要**新建平行移动端 api 目录。
6. **样式**：
   - 单位只用 px；颜色只用 `var(--pnkx-*)` 设计令牌（bg/surface/surface-muted/border/border-soft/text/text-secondary/text-muted/primary/primary-soft/success(-soft)/danger(-soft)/warning(-soft)/info(-soft)），自动跟随明暗主题。
   - scss 里引用图片用 `@/mobile/assets/...` 别名。
   - 共享类（mobile.scss 已有，直接用）：`m-page`（页边距 12px）、`m-card`、`m-cell-card`、`m-section-title`、`m-grid`/`m-grid-item(__icon)`、`m-stat-bar(__item/__label/__value)`、`m-fab`（悬浮新建按钮，加 `--no-tabbar` 修饰类用于非 TabBar 页）、`m-sticky-actions`（底部固定操作条）+ `m-form`（表单页留白）、`m-empty`、`m-pull`、`m-text-secondary`、`m-text-muted`、`m-rich-content`、`m-dot`。
   - 标题栏：MobileLayout 已渲染 van-nav-bar（标题取路由 meta.title、非 TabBar 页自动有返回键）。需要自定义标题栏的页面在路由 meta 标 `plain: true` 并自绘。**保存/提交类动作用底部 `m-sticky-actions` 按钮**，不放标题栏。
7. **组件名冲突**：域内私有组件放 `views/<domain>/components/`；跨域共享组件（ReminderSetting、AssignmentHistory 等）放 `src/mobile/components/`，仅在被分配的任务中创建。
8. **业务 SVG 图标**：`iconUrl('c-can')`（c-*=记账分类、w-*=天气、x-*=心情、a-*=账户等拼音名）。图标选择器用 `iconNames` 全量列表。
9. **PC 全局组件可直接用**：`FeatureGate`（后端功能开关门，prop 名是 `feature`：`<feature-gate feature="chargeInstanceV2">`）。PC 的 EP 版业务组件（ShoppingCheckout/ChargeInstances 等）**不要**直接在移动端用（样式不适配），按其 API 调用用 Vant 重写精简版。
10. **不要改**这些共享文件：router/*、layout/*、styles/mobile.scss、utils/*、vite.config.js、permission.js、main.js。需要路由/映射/共享样式的变更，写在任务报告里由主控统一处理。
11. **不要跑构建**（并行任务下全局构建会互相干扰）；语法自查靠仔细。
12. uniapp 蓝本在 `/Users/peihaoyu/PHY/pnkx/code/pnkx-uniapp/`，行为契约测试在 `pnkx-uniapp/tests/*.test.mjs`（可读作行为参考）。
13. 页面跳转目标路由可能尚未注册（分批上线）：跳转前用 `this.$router.resolve(path).matched.length > 0` 判断，未注册 toast「功能即将上线」。

## 交互范式（对齐 uniapp 蓝本）

- 列表页：`van-pull-refresh` + `van-list`（分页加载、下拉刷新）+ `m-fab` 新建 + 空态 `m-empty`。
- 详情/编辑页：`van-cell-group inset` + `van-field` 表单 + 底部 `m-sticky-actions` 保存；危险操作（删除）放次要按钮或更多菜单。
- 底部弹层：`van-popup position="bottom" round`（选择器、确认流程）。
- Tab 切换（列表/日历双视图等）：`van-tabs sticky`。
- 日历：`van-calendar`（自定义 formatter 标记）。
- 长按/滑动操作：`van-swipe-cell`。
