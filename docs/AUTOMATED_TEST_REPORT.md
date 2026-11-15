# pnkx-ui 全量自动化测试报告

> 测试日期：2026-11-15
> 被测版本：pnkx-ui PC/APP 双端改造后代码（63 移动路由 + 132 PC 视图）
> 测试方案：`docs/AUTOMATED_TEST_PLAN.md`
> 结论：**全部 206 项检查通过（75 单元/契约 + 131 E2E/视觉），0 失败**；测试过程中发现并修复 **7 个产品缺陷**。

---

## 1. 执行总览

| 层级 | 套件 | 用例数 | 通过 | 失败 | 跳过 | 耗时 |
|---|---|---|---|---|---|---|
| L0 静态守护 | lint + build + check:mobile-map + validate:ui | 4 项 | 4 | 0 | 0 | ~50s |
| L1 单元/契约 | `npm run test:unit`（node:test） | 75 | 75 | 0 | 0 | 0.3s |
| L3 端到端 | `npx playwright test`（00-10 共 11 个 spec，mobile+desktop 双 project） | 91 | 91 | 0 | 0 | 2.0m |
| L4 视觉回归 | visual.spec.mjs（mobile，32 页 light + 8 页 dark） | 40 | 40 | 0 | 0 | 45s |
| **合计** | | **206** | **206** | **0** | **0** | **≈3.5m** |

- 运行环境：macOS arm64 / Node 18 / Playwright 1.61.1（Chromium 149）
- E2E 无真实后端依赖：Mock Server（`tests/mock-server/server.mjs`，160 条 fixtures 路由）占用 8068，vite dev 代理指向；SSE 流式端点可编程分块
- 稳定性：03-bookkeeping spec 连跑 2 轮全绿；全套连跑 2 轮全绿

## 2. L1 单元/契约（75 通过）

| 文件 | 用例 | 覆盖 |
|---|---|---|
| tests/unit/device.test.mjs | 7 | 768px 断点/强制覆盖优先级/清除恢复/modeRef 响应式/localStorage 降级 |
| tests/unit/route-map.test.mjs | 11 | 精确表/关键词两代菜单前缀/长词优先/剥尾段回落/uniapp appPath 全量转换/NEUTRAL_PATHS |
| tests/unit/icons-assets.test.mjs | 3 | 267 个业务 SVG 齐全、四类前缀、icons.js eager glob |
| tests/unit/contracts/（11 个文件） | 54 | 自 uniapp 移植的同源契约：chat-stream / chat-history / chat-plan-card / plan-editing / action-center-state / action-center-mutations / checkout-model / shopping-groups / rule-change-flow / feature-capabilities / operation-plan-presentation |

说明：offline-sync / online-actions / mobile-runtime / shared-cache 4 个契约未移植（App 离线专属，Web 已裁剪，见方案 §5.2）。

## 3. L3 端到端（91 通过，双 project）

| Spec | 用例 | 覆盖要点 |
|---|---|---|
| 00-device | 10 | **设备分流矩阵 P0**：手机→APP 自动跳转、反向分流、PC 专属页回落提示、375↔1280 跨断点实时互跳、?force 强制与清除恢复、/note-share 放行 |
| 01-auth | 4 | 无 token 拦截 / 登录成功进 APP / 密码错误提示 / 401 过期清 token |
| 02-home | 7 | 首页四数据区渲染、公告轮播、铃铛未读数 3 与跳转、FAB 入口、视图切换持久化、行动中心逾期置顶+pin 管理（actionCenterV2）、开关关闭无切换入口 |
| 03-bookkeeping | 12 | 月汇总、记一笔全流程（二级分类弹层+计算器+保存载荷断言）、转账双账户、AI 记账解析回填、金额空拦截、编辑回填、清单翻月/导出、账户总资产/新增、详情/改名、echarts canvas 渲染、预算超支/新建、账期面板+规则启停+三步变更向导（change-preview→commit） |
| 04-todo-diary | 10 | 完成率环/四 Tab、新建与勾选完成（POST/PUT 断言）、筛选、日历标记、标签+搜索防抖（0 请求→400ms 后 1 次）、转派+提醒绑定、日记双视图、心情/天气图标、编辑回填/删除、心情分析+AI 洞察流式 |
| 05-note-book | 9 | 笔记面包屑、新建/编辑、文件夹树管理、Obsidian 浏览/搜索、md 预览+保存+**未保存离开确认**、书架 Tab/分页、TXT 导入、阅读器（plain 全屏/主题字号持久化/进度保存）、书籍编辑 |
| 06-webarticle-shopping | 10 | 收藏筛选组合、粘贴收藏+抓取轮询 0→1、星标/已读、正文 DOMPurify（script 被清除）、编辑、RSS 管理、购物进度/新建、四分组勾选/清空、结算面板（SP2）、认领/释放/转派 |
| 07-ai | 6 | 快捷提示词、SSE 流式逐字渲染（setStream 时序）、operation-plan 事件→计划卡、草稿编辑、pending 双按钮、会话管理+OperationPlans（OP2） |
| 08-reminder-calendar-report-search | 10 | 通知中心全操作、偏好保存、日历 42 格彩点聚合、就地完成、驾驶舱、报告 AI 流式+中断+重新生成（R6）、ReportActions（RA2）、搜索分组、AI 总结、4 类通知 deep link |
| 09-life-domains | 10 | 姨妈 hero/记录互斥/设置边界/统计、纪念日倒计时/新建/提醒绑定+准备待办、卡券使用/评分流转、自动化 JSON 校验/dry-run/执行记录、菜谱/膳食增删、膳食→购物清单（SP2）+订阅+账期（CI2） |
| 10-pc-regression | 6 | **PC 零回归**：登录进工作台、992px 抽屉、后端菜单动态路由到待办分栏页、TagsView 增删、快捷 dock 弹窗、明暗主题翻转 |

## 4. L4 视觉回归（40 基线，全部锁定）

- light：32 个主页面（3 Tab + 各域主页/表单页）
- dark：8 个代表页（首页/记一笔/待办/日记/AI/日历/我的/搜索）
- 基线：`tests/e2e/visual.spec.mjs-snapshots/`（40 张 PNG，入库）；阈值 maxDiffPixelRatio 2%、animations disabled
- 明暗主题经 `van-config-provider` + `--pnkx-*` 令牌双轨验证

## 5. 测试中发现并修复的产品缺陷（7 个）

| # | 缺陷 | 位置 | 影响 | 修复 |
|---|---|---|---|---|
| 1 | **未保存离开确认弹窗闪现后自动消失**（路由守卫挂起期间 Vant 函数式 Dialog 被关闭，promise 永久挂起，无法确认保存，存在数据丢失风险） | mobile/views/note/obsidian-edit.vue | Obsidian 编辑页高 | 改为页面内自绘 van-popup 确认弹层（含防重复提交） |
| 2 | **待办搜索防抖失效**：输入非空关键词永远不会触发防抖搜索 | mobile/views/todo/index.vue | 待办页高 | 去掉空值短路，统一 400ms 防抖 |
| 3 | **TodoCard 缺 emits 声明**：原生 click 透传覆盖 $emit 参数，点卡片进「新建」而非编辑 | mobile/views/todo/components/TodoCard.vue | 待办页高 | 补 emits 声明 |
| 4 | **DiaryCard 同样的 emits 缺失**：点日记卡片丢 `?id` | mobile/views/diary/components/DiaryCard.vue | 日记页高 | 补 emits 声明 |
| 5 | **经期记录页删除按钮被固定保存栏遮挡**：scoped `padding-bottom:24px` 覆盖 m-form 的 76px 留白，编辑态无法删除记录 | mobile/views/menstruation/record.vue | 姨妈助手高 | 移除覆盖，留白回归 m-form |
| 6 | **生活报告 SSE 解析丢失换行**：自写逐行解析未处理“多 data 行同帧”契约，AI 报告全部内容挤成一行 | mobile/views/report/index.vue | 生活报告高 | 改用与 uniapp/PC 同源的 chatStreamParser |
| 7 | **账户详情/维护页跳转 404**：页面跳转用连字符路径（account-maintenance）而路由注册为斜杠 | mobile/router/index.js | 记账账户页中 | 路由补 alias（account-details/account-maintenance） |

另修复测试基建问题 2 个（非产品）：
- sse.mjs 对含换行的 data 未按 SSE 规范拆多行 data:，导致流式帧解析错乱
- 移动端全局别名缺失：`$parseTime/$getTimeDifference/$arraySum`（uniapp 命名习惯）在 main.js 补注册（11 处引用 7 个文件）

## 6. 交付物清单

```
pnkx-ui/
├── playwright.config.mjs                 # 双 project + 双 webServer（端口可环境变量隔离）
├── tests/
│   ├── unit/                             # L1（75 用例）
│   │   ├── device.test.mjs / route-map.test.mjs / icons-assets.test.mjs
│   │   └── contracts/ ×11                # uniapp 同源契约
│   ├── mock-server/                      # :8068 Mock Server
│   │   ├── server.mjs                    # 160 fixtures 路由 / token 档案 / __test 控制面
│   │   ├── sse.mjs                       # SSE 可编程分块（规范多行 data）
│   │   └── fixtures/ ×7                  # auth/home/bookkeeping/todo-diary/note-book/ai-life/pc
│   └── e2e/
│       ├── helpers/{setup,mock}.mjs      # 外网屏蔽+pageerror 断言 / 鉴权+覆写+请求日志断言
│       ├── 00-device … 10-pc-regression  # 91 条功能用例
│       └── visual.spec.mjs + snapshots/  # 40 张基线
└── package.json                          # verify / test:unit / test:e2e / test:visual scripts
```

CI：`.github/workflows/ci.yml` 的 pnkx-ui job 已扩展（静态守护→单测→Playwright chromium→E2E→失败时上传报告）。

## 7. 已知边界与建议

1. **未做真实后端联调**：Mock fixtures 字段以 uniapp 契约与 docs/API_*.md 为基准；记账 recurring/charge 两接口在蓝本中即读 `res.data`（fixtures 已顺应），若后端实际信封不同，仅需调整 fixtures（用例断言不变）。
2. **视觉基线跨平台**：基线在 macOS 生成，CI linux 渲染可能有字体差异——首次 CI 若视觉红，用 `--update-snapshots` 在 CI 镜像重建一次基线即可（或后续固定 docker 镜像）。
3. **query 级 mock 不区分**：同 URL 不同 query 返回同一份数据（各 spec 已按此注释断言）；后续如需可在 server.mjs 加 query 匹配。
4. **建议纳入日常**：提交流程跑 `npm run verify && npm run test:unit`；CI 已自动跑全量。新增移动页面的 checklist：注册路由 → map.js 补映射 → spec 补 1-2 条冒烟 → 视觉基线 `--update-snapshots`。

---

*测试执行：ZCode 自动化（2026-11-15）；证据：`node_modules/.cache/pw-report`（HTML 报告）、`playwright.config.mjs`、本文所列 206 项全部通过。*
