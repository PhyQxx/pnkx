# Pnkx 功能设计优化与开发实施方案

版本：1.1 · 更新：2026-10-24 · 状态：主要功能已实现并通过本地验证，未发布；完整范围差异见交付说明

## 1. 文档用途与边界

本文将系统功能评估转化为可拆分任务、设计接口、实施迁移和验收的方案。范围覆盖 PC 管理端、uniapp 移动端及后端；公共博客前台本轮不改。目标是减少用户跨模块操作、区分计划与事实、使自动化可理解且可恢复。

实际交付、接口和验证记录见 [功能优化实现与交付说明](FUNCTION_OPTIMIZATION_IMPLEMENTATION.md)。本文保留目标设计及后续扩展，不代表所有设计细项均已交付。

本文的“现状”来自设计时的仓库代码核对；历史文档勾选不代表已做线上验证。“新增/建议”是本轮目标设计，不代表当前系统已有该接口或表。接口路径、表名为建议命名，实施时需核对现有迁移与路由冲突；现有字段类型、用户 ID 类型、AjaxResult 响应约定必须保留兼容。本文不直接修改生产数据、不执行数据库迁移。

### 1.1 交付范围

| 编号 | 功能包 | 优先级 | 本轮设计目标 |
|---|---|---|---|
| F01 | 今日行动中心 | P0 | 从聚合信息变成可处理事项 |
| F02 | 统一周期账单 | P0 | 预计、待确认、实际入账分离，统一出账入口 |
| F03 | 提醒处理闭环 | P0 | 已读、投递、业务处理分离 |
| F04 | 餐饮采购闭环 | P1 | 人数换算、食材合并、差异更新、采购记账 |
| F05 | AI/自动化执行反馈 | P1 | 结构化预览、分步结果、失败恢复、受控撤销 |
| F06 | 家庭协作 | P2 | 认领、负责人、共享边界和退出规则 |
| F07 | 报告转行动 | P2 | 可追溯结论、人工采纳、后续复盘 |

不在本轮：银行/支付平台扣款接入、自动取消第三方订阅、完整库存与保质期系统、复杂财务结算、通用可视化工作流编辑器、AI 自主执行未经批准的写操作。

### 1.2 已核对的复用基础

路径均相对于仓库根目录。

| 现有实现 | 已确认能力 | 本次处理 |
|---|---|---|
| `pnkx-uniapp/pages/function/index.vue` | 三个快捷入口、后台菜单分组、搜索 | 保留全部功能入口，增加行动入口和常用项 |
| `pnkx-ui/src/views/px/life/lifeManager/index.vue` | 日历/菜谱/餐饮/购物/订阅/报告标签页 | 保留专业管理页，新增场景入口 |
| `PxCalendarController` | `GET /calendar/month`、`GET /calendar/cockpit` | 新增独立行动投影，旧接口不破坏 |
| `PxSubscription` / `PxSubscriptionController` | 周期、金额、下次扣费、启停、账户分类、预测 | 增加记账模式、生命周期、账期实例 |
| `PxSubscriptionServiceImpl.generateDueRecords` | 订阅到期生成账单 | 改为调用统一账期服务 |
| `PxAutomationServiceImpl.subscriptionCharge` | 自动化写账单并推进日期 | 同样调用统一账期服务，禁止另行直写 |
| `PxBookkeepingRecurringService` | 周期记账服务 | 接入统一账期服务，复用现有规则 |
| `PxShoppingItem` | quantity 为 String、checked、addedFromMeal | 增量增加结构化数量和来源明细 |
| `PxMealPlan` | 日期、餐次、菜谱，无已见人数字段 | 增加用餐人数，历史默认 1 份 |
| `PxMealPlanController` | `/mealPlan/transferToShopping` | 迁移至预览/提交，旧接口走同一服务 |
| `PxLifeNotification` | 状态 0 已发送/1 失败/2 已读 | 保留旧字段兼容，另建事件处理状态 |
| `PxAutomationController` | 模板、试运行、执行列表、失败重试 | 扩展计划与步骤结果，不重复建立规则系统 |
| `SysDataGroupController` | 现有共享空间及成员退出入口 | 复用空间模型，不另建家庭体系 |
| `PxLifeReportController` | 数据、流式报告、历史 | 增加结构化洞察和采纳记录 |

历史范围参考 `docs/INTEROP_IMPLEMENTATION_PLAN.md`；真机验证仍参考 `docs/DEVICE_REGRESSION_CHECKLIST.md`。

## 2. 统一业务约定

### 2.1 身份、权限与时间

- 用户身份从登录态获取，禁止信任请求中的 ownerId/createBy；空间操作除校验成员身份，还校验对象共享范围和动作权限。
- 私人、共享两种上下文明确显示；切换空间后重新查询，不复用另一空间缓存。
- 业务日期使用用户/空间配置时区；现有用户未配置时迁移默认 Asia/Shanghai。事件时间存统一时间点并带偏移输出，账期使用 DATE，不用服务器默认时区推导。
- 金额使用十进制定点数，首期仅延续现有单币种人民币，不增加汇率换算；数量使用 DECIMAL(12,3)。计算不经过浮点数。
- 新业务实体使用 `revision BIGINT` 做乐观并发控制；现有 String `version` 保留给离线同步，不直接当作数值版本递增。

### 2.2 写操作公共契约

所有新增写操作携带 `requestId`（客户端 UUID）、`expectedRevision`（已有对象必填）。同一用户、动作、requestId 的重试返回同一结果；同一 requestId 不同载荷返回 `IDEMPOTENCY_CONFLICT`。服务端持久化载荷摘要和结果引用，幂等记录至少保留 90 天；账期与采购结算还必须有永久业务唯一约束，不依赖此保留期。

- 返回沿用 AjaxResult：`code/msg/data`；业务错误通过稳定的 `errorCode` 区分，不依赖文案解析。
- 错误至少覆盖 `REVISION_CONFLICT`、`INVALID_TRANSITION`、`SOURCE_UNAVAILABLE`、`PERMISSION_DENIED`、`PREVIEW_EXPIRED`、`IDEMPOTENCY_CONFLICT`。
- 冲突返回最新 revision 和可刷新提示，不静默覆盖用户修改。
- 写入后返回对象 ID、最新状态、revision、受影响对象引用；客户端据此刷新详情和今日中心。
- 业务写入与幂等结果同一事务提交；通知采用事务后投递及可重试事件，投递失败不撤销已成功业务。
- 新增财务确认、共享认领、AI 提交首期要求联网；断网保留输入草稿并显示“尚未提交”，不得显示为已完成。其他既有离线能力保留。

### 2.3 唯一的业务事实来源

今日中心和提醒只是业务数据的投影，不维护独立的“任务已完成”事实。完成待办必须调用待办服务，确认账单必须调用账期服务。业务成功后再更新投影；投影失败可重放修复。所有端共享同一套状态、校验和幂等规则。

## 3. F01 今日行动中心

### 3.1 页面结构与操作

入口：PC 工作台 `pnkx-ui/src/views/index.vue`；移动首页 `pnkx-uniapp/pages/index.vue`。生活助手继续作为全部功能目录，增加用户固定的最多 6 个常用入口，不按算法自动改变顺序。

页面从上到下：

1. 上下文栏：日期、私人/当前共享空间。
2. 待处理：逾期待办、待确认账单、需要决策的续费、执行失败。
3. 今日安排：今日未完成待办、餐饮计划、纪念日准备事项。
4. 快速记录：记账、待办、日记。
5. 场景入口：本周吃饭、纪念日准备、月末复盘；首期只启用已交付流程。

卡片包含标题、来源、期限、状态、原因、一个主动作及详情。已完成默认折叠到“今日已处理”；信息类事件不混入待处理计数。默认每组最多 5 条，显示总数和“查看全部”。排序固定为逾期优先、到期时间、创建时间，不用 AI 决定优先级。

### 3.2 查询与数据

新增 `GET /life/action-center?date=YYYY-MM-DD&scope=personal|group&groupId=...`；分组详情 `GET /life/action-center/items?bucket=attention&cursor=...&limit=20`，limit 上限 100。

返回 `generatedAt/timezone/summary/buckets/partialErrors`；item 至少包含：

```json
{
  "itemKey": "charge:1024",
  "sourceType": "charge_instance",
  "sourceId": "1024",
  "title": "确认本月视频会员账单",
  "status": "pending",
  "dueDate": "2026-10-23",
  "reason": "已到预计扣费日，尚未确认",
  "revision": 3,
  "actions": [{"code": "confirm_charge", "label": "确认入账", "requiresForm": true}],
  "route": {"name": "charge-detail", "params": {"id": "1024"}}
}
```

route 使用跨端语义路由，由各端白名单映射，不接受后端任意 URL 执行。actions 只描述可用动作；真正提交仍校验权限和状态。

首期复用聚合服务按来源读取，无需新建“今日任务表”；新增用户偏好记录保存固定入口。某来源异常时，返回该分组不可用而不是把数量显示为 0。无数据时提示“今天暂无待处理事项”，提供快速记录。

### 3.3 验收

- 同一账期从日历和提醒进入，今日中心仅显示一个待处理项。
- 完成待办后刷新首页，计数与详情一致；在另一端已完成的卡片不能再次产生副作用。
- 未获授权的共享账单金额不出现在标题、摘要、计数明细中。
- 今日中心首屏主要业务操作不超过“点击卡片动作→填写必要信息→确认”三个交互步骤。

## 4. F02 统一周期账单

### 4.1 用户流程与状态

订阅/周期规则分别保留原有表单；新增“入账方式”：`manual_confirm` 到期提醒确认（新建默认）或 `auto_record` 按规则自动记账。文案明确“系统记录账目，不代表银行已扣款”。

订阅生命周期：`active`、`paused`、`cancelled`；试用通过独立 `trialEndDate` 表达，试用期内不生成付费账期。终止追踪时展示“仅停止系统内计划，请自行到服务平台取消续费”。

账期实例状态：

| 当前状态 | 动作/条件 | 后续状态 | 结果 |
|---|---|---|---|
| planned | 到达预计日期，人工模式 | pending | 进入待处理，无实际账单 |
| planned | 到达预计日期，自动模式 | posted | 创建实际账单，标注自动记录 |
| pending | 确认金额/账户/日期 | posted | 原子创建实际账单并关联 |
| pending | 跳过本期并填写原因 | skipped | 不计支出，保留历史 |
| pending | 用户记录扣费失败 | payment_failed | 不计支出，可稍后确认 |
| payment_failed | 用户确认最终扣费成功 | posted | 创建一次实际账单 |
| planned/pending/payment_failed | 取消该期 | cancelled | 不计支出，保留原因 |

系统执行失败不能伪装成支付失败；单独记录 executionStatus/errorCode。posted 不允许重新确认或直接跳过；更正金额通过现有账单编辑服务并同步关联快照，账单作废后标记关联状态 `voided`，保留原实例，不自动补记。

### 4.2 核心规则

1. 每个来源规则每个账期最多一条实例，业务键为 `(owner_id, source_type, source_id, period_key)`；period_key 使用预定发生日期，而非任务运行日期。
2. 订阅调度、周期调度、自动化、人工补记、AI 确认统一调用 ChargeInstanceService；任何入口不得直接绕过它插入周期账单。
3. 创建实例后推进来源的“下次计划日期”；未确认账期可与后续账期并存，不能因为未确认堵住未来账期。
4. 月末锚点：原约定 31 日，短月用最后一天，下月仍回到 31 日；独立保存 anchorDay，不从短月结果继续推导。2 月 29 日年度规则在非闰年用 2 月末。
5. 过去应生成而未生成的账期按唯一键补齐；每批上限 100 期，剩余继续调度，不静默丢弃。人工模式只生成待确认，不把欠缺记录当作真实消费。
6. 暂停默认不积累新账期；恢复默认从恢复日之后的首个计划日开始。提供显式“预览并补齐暂停期间账期”；旧规则语义迁移见第 11 节。
7. 调价默认仅影响尚未到期的 planned 实例；pending、posted 保留金额快照。修改周期时先预览差异，已发生账期不重排。
8. 不同来源可能描述同一消费时按名称、金额、账户和日期给出重复提示，但不自动合并。用户选择主规则并停用另一规则；系统唯一键只保证同一规则同一期不重复。
9. 统计和预算实际消耗只读取已入账的实际账单；预测单独展示 planned/pending，不混入实际支出。已入账实例不重复进入预测。

### 4.3 数据变更（建议）

| 表/实体 | 新增字段或表 | 约束 |
|---|---|---|
| 订阅、周期规则 | posting_mode、lifecycle、anchor_day、timezone、revision；订阅增加 trial_end_date/cancelled_at | 保留 enabled 兼容映射 |
| px_charge_instance（新增） | id、owner_id、source_type/source_id、period_key、due_date、expected_amount、actual_amount、account_id、classification_id、status、record_id、record_state、revision、origin、reason、posted_at | 来源账期唯一键；record_id 唯一；金额非负；索引 owner/status/due_date |
| px_charge_execution（新增） | instance_id、request_id、status、error_code、attempt_count、next_retry_at | 技术失败重试，不改变支付事实 |
| 实际账单 | charge_instance_id（可空） | 唯一关联，旧账单为空 |

公共新增表均带 create/update 时间及操作者；字符串长度、外键策略沿用工程规范。来源快照包含名称、金额、周期、模式，防止删除规则后无法解释历史。删除规则停止后续生成，已入账历史不级联删除。

### 4.4 接口契约（新增）

| 接口 | 输入 | 输出/规则 |
|---|---|---|
| GET /charge/instances | sourceType/sourceId/status/startDate/endDate/pageNum/pageSize | 分页账期及关联账单，时间范围限制一年 |
| GET /charge/instances/{id} | id | 实例、来源快照、执行历史、可执行动作 |
| POST /charge/instances/{id}/confirm | requestId/expectedRevision/actualAmount/accountId/classificationId/paidAt | recordId/实例最新状态；原子入账 |
| POST /charge/instances/{id}/skip | requestId/expectedRevision/reason | skipped；posted 拒绝 |
| POST /charge/instances/{id}/payment-failed | requestId/expectedRevision/reason | payment_failed；只表达用户记录 |
| POST /charge/rules/change-preview | sourceType/sourceId/expectedRevision/changes | previewId/新增、修改、取消实例清单 |
| POST /charge/rules/change-commit | requestId/previewId | 应用预览；来源变化则要求重新预览 |

表单：账单确认必须显示预计金额与实际金额，账户和分类无默认有效值时要求选择。账期详情显示“由哪条规则、哪次操作生成”，重复点击确认返回已有 recordId。

### 4.5 验收样例

- 订阅任务和自动化同时执行同一账期：一条实例、一条实际账单，来源日期只推进一次。
- 1 月 31 日月付：2 月末发生后，3 月仍是 31 日。
- 月付 30 元，用户确认实际 25 元：实际支出/预算计 25，保留预计 30。
- 人工模式到期一周未确认：实际支出不增加，待处理仍存在。
- 已入账后修改规则金额：历史实际金额不变；取消规则后历史可查。

## 5. F03 提醒处理闭环

### 5.1 三种状态分开

- 投递状态：pending/sent/failed，按渠道分别维护。
- 阅读状态：每个接收人 unread/read。
- 业务事件状态：open/snoozed/resolved/cancelled，由业务事实决定。

新增 `px_reminder_event`：event_key、source_type/source_id、occurrence_key、state、snooze_until、resolved_at、resolution_action、revision。唯一键 `(source_type, source_id, occurrence_key)`；周期事件 occurrence_key 必须含账期或提醒发生期，不能按 sourceId 把所有未来提醒一起结束。

新增 `px_reminder_event_recipient`：event_id/user_id/read_at/个人静音设置；投递记录关联 event_id 与 recipient。原 PxLifeNotification.status 继续提供旧端所需值，新端读取独立状态。

### 5.2 动作设计

| 来源 | 主动作 | 次动作 | 结束条件 |
|---|---|---|---|
| 待办 | 完成 | 改期、稍后提醒 | 业务待办已完成或取消 |
| 周期账单 | 确认入账 | 本期跳过、记录扣费失败 | posted/skipped/cancelled |
| 纪念日 | 查看准备计划 | 创建准备待办、忽略本次 | 本次提醒忽略或到期结束；纪念日仍保留 |
| 预算 | 查看明细 | 调整预算、忽略本次 | 本次预警处理；后续新阈值事件可再发 |
| 自动化失败 | 查看失败步骤 | 重试、放弃本次 | 执行成功或明确放弃 |

“稍后提醒”仅改变个人提醒时间，不修改业务截止日；“改期”修改业务日期并重算提醒。免打扰只改变投递时间，不改变来源到期时间。

新增 `GET /reminder/events`、`POST /reminder/events/{id}/read`、`POST /reminder/events/{id}/snooze`、`POST /reminder/events/{id}/dismiss`。完成/确认等业务动作调用对应业务接口，成功后发布事件。snooze 输入 until，必须在未来且不超过 30 天；read 按人生效，dismiss 信息型提醒按接收人生效，不能替其他成员完成任务。

源对象删除则卡片显示“来源已移除”、动作禁用；待发送渠道任务执行前重新检查事件状态，已结束不再发送。已发出的邮件无法收回，打开链接时展示最新状态。

### 5.3 验收

同一事件邮件、站内、推送产生多条投递，但待处理只算一次；阅读不完成业务；PC 完成任务后移动端刷新显示已处理；旧邮件链接不能再次写入；延期通知不改变原截止日。

## 6. F04 餐饮采购闭环

### 6.1 场景流程

周餐饮页选择日期、餐次、菜谱、人数 → 点击生成采购 → 查看合并预览 → 标记已有 → 确认写入清单 → 采购勾选 → 完成采购并可选记账。

首期支持按菜谱标准份数线性换算；“适量”等非数字用量原样保留并提示人工确认，不猜测重量。只自动换算同量纲的 g/kg、ml/L；“个”和“克”不转换。名称首期仅做空白、全半角等规范化，不自动把“番茄”和“西红柿”视为同物，允许用户确认别名。

### 6.2 数据与合并规则

- 菜谱增加 baseServings（正数，默认 1），餐饮计划增加 servings（正数）；所需数量=标准数量×计划份数/baseServings。
- 购物项保留 quantity 原文，新增 normalizedQuantity、unit、ingredientKey、fulfillmentState（needed/available/purchased）、revision。
- 新增 `px_shopping_item_source`：itemId、mealPlanId、ingredientId、sourceRevision、requiredQuantity、unit、generationKey；按来源行稳定标识去重，不以遍历数组位置作标识。
- 同一清单、ingredientKey、兼容单位可合并。手工添加行默认不参与自动重算；用户选择合并后记录人工数量分量，后续不能覆盖。
- available 表示家中已有，不是已购买；不计入采购金额。
- 修改计划后生成增减差异预览；未购行可调整，已购行不自动删除或改回未购，而是提示“需求减少，已有采购保留”。
- 删除一个计划仅移除其来源贡献；仍有其他来源或人工数量时保留购物项。

### 6.3 接口与采购批次

| 新增接口 | 输入 | 输出 |
|---|---|---|
| POST /mealPlan/shopping-preview | listId/startDate/endDate/planIds（可选） | previewId、sourceRevisions、added/changed/removed/warnings |
| POST /mealPlan/shopping-commit | requestId/previewId/decisions | affectedItems；版本变化要求重做预览 |
| POST /shoppingItem/{id}/fulfillment | requestId/expectedRevision/state | 更新后的购物项 |
| POST /shoppingList/{id}/checkout-preview | purchasedItemIds | 可结算条目、已有结算提示 |
| POST /shoppingList/{id}/checkout | requestId/previewId/recordExpense/totalAmount/accountId/classificationId/paidAt | batchId/可空 recordId |

新增采购批次及批次条目关联；一个条目首期只允许整行结算一次，不支持部分采购。结算总金额由用户输入，不从数量推算。recordExpense=false 时只存采购批次；日后补记通过批次入口，batchId 与 recordId 唯一绑定。

点击“清空已购”改为“归档已购”，保留采购和来源审计。旧端 checked=true 对应 purchased；新状态 available 对旧端投影为已处理，但旧端写回时若无法保留状态必须提示升级，不能误改为 purchased。

### 6.4 验收

两道菜分别需要 200g 和 0.3kg 同一种食材，合并为 500g；另有“2 个”时单独展示。重复生成不增量翻倍；删除一顿饭不删除另一顿饭的贡献；结算重试只产生一笔账；计划改动不会撤销已购状态。

## 7. F05 AI 与自动化执行体验

### 7.1 计划不是确认文案

复用工具注册表和执行日志，建立结构化计划，前端由计划渲染，不从 AI 自然语言提取待执行内容。

计划至少包含 planId、schemaVersion、contextScope、expiresAt、sourceRevisions、steps、warnings。步骤含 stepId、toolCode、依赖、动作类型、目标摘要、参数、预期影响、风险、补偿能力。首期预览有效期 15 分钟；过期、权限变化、目标版本变化都必须重新预览。

所有写入计划都显式确认；纯查询无需确认。用户编辑计划后重新验证；首期不允许随意取消有依赖的中间步骤，只能修改输入重新生成。未确定金额、人数、日期等必需参数先补充，不自动代填并提交。

### 7.2 执行状态与恢复

计划状态：draft → awaiting_confirmation → running → succeeded/partially_succeeded/failed；可进一步进入 compensating → compensated/compensation_failed。单步 pending/running/succeeded/failed/skipped/compensated。

- 单步数据库事务，跨步骤不承诺整体原子性；每步幂等键使用 planId+stepId，不因重试生成新键。
- 重试只恢复失败及被依赖阻塞的步骤，成功步骤不重跑；恢复前重检权限和输入版本。
- 外部副作用只能在能力声明支持时补偿；发送出去的邮件不宣称能撤回。
- 撤销前生成补偿预览。目标自执行后被编辑或被引用时禁止直接恢复旧快照，列出冲突让用户处理。
- 财务步骤优先作废/更正，不物理删除审计。撤销失败显示实际已补偿和未补偿步骤。

新增 `px_operation_plan` 与 `px_operation_step`，现有 PxAutomationExecution/PxAiOperationLog 关联 planId；不新建独立自动化模板系统。

新增 `POST /operation-plans/preview`（输入场景和参数）、`POST /operation-plans/{id}/confirm`（requestId/planRevision）、`GET /operation-plans/{id}`、`POST /operation-plans/{id}/retry`、`POST /operation-plans/{id}/compensation-preview`、`POST /operation-plans/{id}/compensate`。confirm 成功返回 executionId 和状态，客户端断线后通过 GET 恢复，不依赖 SSE 消息判定最终成功。

### 7.3 页面与验收

AI 对话内显示计划卡片、确认按钮、进度和结果链接；自动化页提供同一执行详情组件。失败卡片写明“餐饮计划已创建，购物清单失败”，提供继续失败步骤。

验收：第二步失败后重试不重复第一步；流中断后能恢复状态；过期确认不执行；来源被他人修改后不能强制撤销；试运行不写业务数据（允许留执行审计）。

## 8. F06 家庭协作

### 8.1 首期边界

复用现有 DataGroup 空间与角色，先做共享购物认领及待办负责人，不新建家庭账户体系。共同支出分摊作为后续子批次 F06-B，不能阻塞首期。

权限建议映射到现有角色：拥有者管理成员与空间；可编辑成员操作明确共享的任务和购物项；只读成员查看。空间成员身份不自动赋予私人账单、日记、经期数据访问权。

### 8.2 认领与负责人

购物项新增 claimedBy/claimedAt/revision；待办复用已有执行人字段，核实原语义后再映射负责人，不另存两套负责人。创建人不随认领变化。认领通过版本比较原子更新，两人同时认领仅一人成功；另一人看到“已由某成员认领”。允许本人释放，空间管理员可转派并留原因。

新增购物 claim/release 动作接口；共享待办提供负责人选择和转派动作。成员退出后，未完成认领释放并通知其余成员；已完成操作保留操作者历史。待办由空间管理员重新分配。数据归属沿用当前退出策略，不转移私人所有权。

### 8.3 分享与退出

分享确认页列出字段预览，日记正文、附件、关联账单不因分享摘要自动开放；服务端按字段输出，不能只靠前端隐藏。搜索、导出、AI 上下文同样遵守共享范围。

退出立即撤销后续 API 访问；客户端收到权限变化后清除共享缓存，重新联网必须检查成员资格。无法保证收回已下载到离线设备的数据，应在分享说明中明确此边界。

F06-B 后续分摊模型：expenseId/payerId/成员应承担金额/结清记录；各成员分摊之和必须等于总额，差额分配明示。付款记录与成员间结算分别归类，结算不重复计为家庭消费；首期不接真实转账。

### 8.4 验收

并发认领唯一；只读成员不可认领；成员退出后搜索、详情、导出、AI 不再访问共享对象；查看共享购物不暴露关联的私人账单详情；分摊批次如启用，100 元消费及成员间 50 元结算不能汇总为 150 元消费。

## 9. F07 报告转行动

### 9.1 报告结构

每条 insight 分为事实、解释、建议：

- 事实：金额、周期、对比口径、来源过滤条件及聚合时间。
- 解释：标为 AI 推测，不将相关性表达为因果。
- 建议：最多 3 个可执行候选，默认不创建任务或修改预算。

示例：“本月外卖实际支出 800 元，上月同口径 600 元”是事实；“可能因为工作忙”是推测；“安排下周三顿晚饭”是可采纳行动。上期为 0 不显示增长百分比；当月未结束默认对比上月同长度区间，并明确截止日期。

### 9.2 数据与接口

新增 `px_report_insight`：reportId、metricCode、periodStart/end、comparisonStart/end、value、comparisonValue、dataAsOf、sourceFilter、factText、hypothesis、suggestedAction。

新增 `px_report_action`：insightId、ownerId、status（proposed/accepted/dismissed/completed/cancelled）、planId、targetRefs、acceptedAt、reviewAt。相同 insightId+actionCode+ownerId 的采纳只创建一个行动。

新增 `GET /lifeReport/{id}/insights`、`GET /lifeReport/insights/{id}/sources`（分页且重新校验权限）、`POST /lifeReport/insights/{id}/action-preview`、`POST /lifeReport/actions/{id}/accept`、`POST /lifeReport/actions/{id}/dismiss`。采纳调用 F05 计划机制，用户编辑目标后再确认。

报告事实保留生成时快照；点击来源展示当前数据时提示可能与快照不同。源记录删除后不显示失效明细，但保留有权查看的历史汇总。下期展示目标是否完成及关联数据，不能声称完成行动必然导致改善。

### 9.3 验收

报告聚合可从同口径来源复算；AI 文本不改变事实金额；重复采纳不创建重复待办；建议采纳前无业务写入；权限撤销后来源明细不可查询。

## 10. 页面与工程落点

| 功能 | 后端主要落点 | PC | 移动端 |
|---|---|---|---|
| F01 | CalendarAggregateService、独立 ActionCenter 查询服务 | views/index.vue | pages/index.vue、pages/function/index.vue |
| F02 | Subscription/Recurring/Automation 服务及记账服务 | life/subscription、bookkeeping/RecurringPanel，新增账期详情 | pages_life/subscription、bookkeeping/recurring，新增账期详情 |
| F03 | ReminderController、提醒调度、通知投递服务 | life/reminder、现有通知入口 | pages_life/reminder |
| F04 | MealPlan/Shopping 服务、Automation meal_to_shopping | life/meal、recipe、shopping | pages_life/mealPlan、recipe、shoppingList |
| F05 | AutomationController、AI 操作入口、计划编排服务 | px/ai/chat，补执行详情入口 | pages_life/ai/chat、automation |
| F06 | SysDataGroupController、对象授权及共享查询 | system/dataGroup、todo、shopping | 现有空间入口及 todo、shoppingList |
| F07 | LifeReportController、报告数据服务 | life/report | pages_life/report |

PC 路径前缀为 `pnkx-ui/src/views/px/`（工作台和 system 除外）；移动路径前缀为 `pnkx-uniapp/`。后端新增领域服务放 pnkx-life，控制器放 pnkx-admin web/controller；前端新增 API 封装沿用对应目录，不能在组件内复制请求和状态规则。

## 11. 迁移、兼容与发布

### 11.1 分阶段迁移

1. 新增表和可空字段，保留旧字段、旧 API；Flyway 使用仓库下一可用版本，禁止修改已应用脚本。
2. 对现有规则生成迁移预览：现有启用订阅/周期规则默认保留自动入账语义，posting_mode=auto_record，标注 legacy；新建默认 manual_confirm。不能把历史规则无提示改为人工确认。
3. 现有暂停后补记规则保留 legacyResumePolicy，用户编辑时选择是否切换新规则；不一次性自动补记暂停期间全部历史。
4. 历史实际账单不重建。只有来源和账期可确定的记录才关联实例；仅凭备注猜测的不关联，列入人工核对清单。切换日之前不自动补生成实例。
5. quantity 文本安全可解析时填充结构化字段，不能解析则保留原文及 needsReview；checked=true 映射 purchased，但不能据此推断采购金额或新增账单。
6. 历史通知保留原状态；无法识别发生期时作为只读历史，不批量制造新的待处理事件。

### 11.2 切换与回退

先部署兼容后端→影子查询核对今日聚合→测试账号灰度→双端支持→逐用户切换业务写入。按 ownerId 配置开关：actionCenterV2、chargeInstanceV2、reminderEventV2、shoppingPlanV2、operationPlanV2。

账单切换必须暂停该用户旧调度、记录切换边界、核对待执行任务，再启用统一入口；禁止新旧出账器同时写同一用户。旧 API 在切换用户上也转调新服务，不让旧端绕过约束。

发生问题可关闭新页面和新增自动执行，但已切换用户不能直接恢复旧出账器。保留新实例和审计，先核对账单后人工决定回退范围；数据库采用前向修复，不通过删表回滚真实账目。

新功能下已创建的 available 状态、计划结果等不能被旧端覆盖；API 返回能力版本，旧端不支持时提供只读/升级提示。多端发布差异须进入真机验收。

## 12. 可直接拆分的开发任务

各任务完成定义包含后端规则、PC/移动使用入口、接口文档、权限和异常处理；不能仅以表结构或页面存在标记完成。

| 任务 ID | 工作与交付物 | 依赖 | 核心验收 |
|---|---|---|---|
| BASE-01 | 统一动作错误码、requestId、revision 契约及组件 | 无 | 重试和版本冲突行为一致 |
| BASE-02 | 业务日期/周期计算器与账期边界用例 | 无 | 月末、闰年、暂停恢复正确 |
| BILL-01 | 实例表、执行表、唯一约束、迁移预览 | BASE-01/02 | 历史账单不重复生成 |
| BILL-02 | 统一生成/确认/跳过服务，替换三类出账入口 | BILL-01 | 调度与自动化并发仅记一笔 |
| BILL-03 | 双端入账模式、账期列表/详情/确认表单 | BILL-02 | 预计和实际展示分离 |
| REM-01 | 事件/接收人/投递模型及旧状态兼容 | BASE-01 | 多渠道只形成一个业务事件 |
| REM-02 | 业务处理联动、稍后提醒、深链状态检查 | REM-01/BILL-02 | 处理后停止待发送提醒 |
| HOME-01 | 行动聚合、分页、语义路由、局部失败 | BILL-02/REM-01 | 去重、权限、统计一致 |
| HOME-02 | 双端首页行动卡片和常用入口 | HOME-01 | 三步内完成核心动作 |
| SHOP-01 | 数量/单位/份数/来源迁移和换算规则 | BASE-01 | 文本用量无损保留 |
| SHOP-02 | 采购预览、差异提交，改造原转换与自动化 | SHOP-01 | 重复生成不翻倍 |
| SHOP-03 | 已有/已购/归档、采购批次与可选记账 | SHOP-02 | 批次补记幂等 |
| AI-01 | 结构化计划、版本校验、步骤执行与状态恢复 | BASE-01 | 断线和部分失败可恢复 |
| AI-02 | 双端预览/进度/补偿界面，接入餐饮场景 | AI-01/SHOP-02 | 不重复成功步骤 |
| GROUP-01 | 购物认领、待办转派和退出处理 | SHOP-01 | 并发认领和退出授权正确 |
| GROUP-02 | 字段分享预览、搜索/导出/AI 权限一致 | GROUP-01 | 私人字段不随关联泄露 |
| GROUP-B | 可选共同支出分摊与结清记录 | GROUP-02/BILL-03 | 不重复统计消费 |
| REPORT-01 | 洞察快照、事实来源、对比口径 | 无，可独立准备 | 汇总可复算 |
| REPORT-02 | 采纳行动、后续复盘、双端展示 | REPORT-01/AI-01 | 采纳幂等且可追溯 |
| RELEASE-01 | 灰度开关、迁移演练、跨端真机与回退演练 | 每批对应任务 | 不存在新旧重复写入 |

建议交付批次：A=BASE+BILL+REM+HOME；B=SHOP+AI；C=GROUP+REPORT。先交付批次 A 的完整闭环，再扩大范围。任务工作量应由实现者基于数据库、现有测试及人员配置估算，本文不以未经验证的固定工期作为承诺。

## 13. 测试矩阵与上线门槛

| 类别 | 必测场景 | 通过条件 |
|---|---|---|
| 状态机 | 每个允许转换及非法转换 | 状态/副作用一致；非法操作不写入 |
| 并发 | 双击、双端、调度与自动化、两人认领 | 唯一业务结果，无重复账单 |
| 事务 | 插入账单后更新实例失败 | 全部回滚，重试只产生一笔 |
| 日期 | 月末、闰年、跨时区、暂停恢复、欠期 | 与约定账期完全一致 |
| 权限 | 私人/共享/退出/只读/他人 ID | 查询与写入均拒绝越权 |
| 数据迁移 | 历史数量文本、旧状态、旧规则、无来源账单 | 不丢原文，不误认来源，不新增历史支出 |
| 兼容 | 旧端编辑新状态、旧 API、混合版本调度 | 明确拒绝不支持操作或安全适配 |
| 故障 | 推送失败、AI 超时、部分成功、SSE 中断 | 可见真实结果，可幂等恢复 |
| 多端 | PC/H5/App/小程序及通知深链 | 状态一致，联网要求与草稿提示正确 |
| 统计 | planned/pending/posted/voided、共享结清 | 预测与实际分开，无重复计入 |

发布前必须完成：迁移备份和演练；新旧生成入口清点；同一期跨入口并发测试；私人/空间权限测试；账单总额迁移前后对账；PC/H5 冒烟；App/小程序至少确认、提醒深链、断网恢复真机用例。未获得设备时标记未验证，不能以编译成功替代。

建议观测指标：重复业务键冲突数、账期生成失败数、待确认账单龄期、通知处理延迟、计划部分失败率、购物预览修改率、报告采纳率。埋点记录动作类型、匿名对象标识和结果，不记录日记正文、AI 原始敏感内容。上线前采集至少一周基线，再判断优化效果。

## 14. 开发前检查清单与决策记录

以下采用明确默认方案推进设计，评审若修改须同步接口、状态机和验收，不以临时前端文案改变业务规则。

| 决策 | 本文默认 | 影响 |
|---|---|---|
| 新规则入账方式 | 人工确认 | 避免预计消费被当作真实消费 |
| 老规则入账方式 | 保留原自动语义，标识 legacy | 避免升级改变现有用户习惯 |
| 暂停规则恢复 | 新规则不补暂停期；老规则保留原策略 | 需 migration/preview 明确区分 |
| 购物库存 | 仅“家里已有”标记 | 不做库存余额、自动扣减 |
| 采购结算 | 整行结算，用户填总额 | 暂不支持部分采购和自动估价 |
| 协作优先范围 | 认领及负责人 | 分摊独立为后续子批次 |
| AI 写操作 | 全部需预览确认 | 自动化已授权规则按原授权执行，修改规则重新确认 |
| 财务及共享离线写 | 首期联网确认 | 保留草稿，不假成功 |

开发开始时还应核实：线上启用的订阅/周期/自动化任务组合；现有账单删除及余额联动规则；DataGroup 的实际字段授权方式；所有客户端已发布版本；下一可用 Flyway 版本。这些是环境和实现检查项，不影响本方案作为拆分与评审基础，但发现冲突必须先更新设计再编码。


## 15. 开发执行记录

### 2026-10-24：主要功能实现与本地验证

完成双端行动中心、周期账单及规则变更/欠期预览、提醒事件、餐饮采购、购物结算、持久化执行计划、协作权限及报告行动的主要闭环。旧订阅、周期任务和相关自动化统一出账；新规则默认人工确认，历史规则保留自动语义。

新增 14 个迁移脚本及 MySQL 并发/事务/迁移用例。本地 JDK 21 测试共 127 项，失败 0、错误 0、跳过 0；移动离线相关测试 9 项通过；PC 与 H5 生产构建通过；差异空白检查通过。

开发过程中补齐：真实账单修改/作废后的实例联动、生成失败可见与重试、跨操作幂等回执、暂停恢复日期锚点、提醒账期快照、私人财务与 AI 上下文权限、移动离线动作拦截。

实际接口、状态机、配置与上线操作见 [FUNCTION_OPTIMIZATION_IMPLEMENTATION.md](FUNCTION_OPTIMIZATION_IMPLEMENTATION.md)。该文件第 1 节明确列出尚未覆盖的扩展项（包括用户时区、完整转派审计、全部 AI 工具计划化等）；可选共同支出分摊仍为后续批次。

尚未完成的环境验收：App/小程序真机、真实通知投递/深链、生产备份迁移和对账、生产灰度与回退演练。未连接生产数据库、未发布。不得将本地构建成功当作以上验收已完成。

### 2026-10-24：继续补齐

已补双端功能入口与服务端开关联动、共享旧缓存定向清理、管理员带原因转派与分页审计、出账失败退避重试。新增 V1.7.8/V1.7.9；后端累计136项、前端离线相关15项测试通过。详见交付说明第7节。此前“完整转派审计未实现”的记录已由本节更新；真机及生产验收仍未完成。

### 2026-10-24：报告候选行动与纪念日筹备

双端新增三条可编辑的规则候选行动，支持选择、修改预览、幂等采纳；新增未来30天纪念日筹备，覆盖预览创建待办、忽略本次、完成状态联动、跨年/闰年及共享权限。修复旧纪念日查询 OR 分支权限过滤、详情可见性和请求体伪造所有者问题。新增 V1.8.0/V1.8.1，后端累计152项测试通过，前端离线相关15项测试及PC/H5构建通过。详细接口和边界见交付说明第8节。

### 最新增量：共享预览与 AI 创建计划

双端空间管理已增加本人逐记录字段、图片/链接引用的文本预览，授权仍按模块生效。AI 新增笔记、日记、纪念日和待办已接入持久化计划、重试及受限撤销；取消草稿同步取消计划。后端161项、Node15项测试通过，PC/H5构建通过。接口、有效期与剩余范围见交付说明第9节；其他 AI 写工具及逐对象授权尚未全部实现。

### 增量：AI 购物与 UI / UE

AI 购物添加已接入持久化计划，确认前绑定本人目标清单、整批写入原子回滚。双端操作计划增加筛选、搜索、状态层级、分字段确认及失败恢复。后端166项、Node19项测试与PC/H5构建通过。UI截图、体验发现和后续设计清单见 [UI_UX_OPTIMIZATION.md](UI_UX_OPTIMIZATION.md)，功能交付见第10节。

### 2026-10-26：AI 记账与待办变更计划化

AI 记账（bookkeeping.create）与待办修改/完成/删除含批量（todo.update/complete/delete）迁移到持久化执行计划：解析草稿阶段校验金额并收敛输入，执行时锁定并重验分类/账户及待办目标归属与快照，目标被并发编辑即失败要求重新预览；财务与待办变更结果均不自动撤销。记账草稿支持修订金额与备注。后端194项（含真实 Redis）、前端50项测试及 PC/H5 构建通过，无新增迁移。详见交付说明第19节；内容发布等其余 AI 写工具、聊天历史持久化仍待开发。
