# 情侣卡券 后端 API 文档

代码位置：

- Controller：`pnkx-admin/pnkx-admin/src/main/java/com/pnkx/web/controller/life/PxLoversCardController.java`
- Service：`pnkx-admin/pnkx-life/src/main/java/com/pnkx/service/impl/PxLoversCardServiceImpl.java`
- Mapper XML：`pnkx-admin/pnkx-life/src/main/resources/mapper/PxLoversCardMapper.xml`、`PxCardUserMapper.xml`、`PxCardRecordMapper.xml`
- 前端调用：`pnkx-ui/src/api/px/life/card.js`、`pnkx-uniapp/api/px/life/card.js`

数据表：`px_lovers_card`（卡片定义）、`px_card_user`（卡片 ↔ 用户持有余量）、`px_card_record`（使用记录）。

## 1. 通用约定

| 项 | 说明 |
|---|---|
| 前缀 | `/px/card`（`server.servlet.context-path=/`，无额外前缀） |
| 认证 | 需登录态，请求头 `Authorization: Bearer <token>`。`SecurityConfig` 未对 `/px/card/**` 放行，也无 `@PreAuthorize` 细粒度权限 |
| 分页参数 | Query：`pageNum`、`pageSize`、`orderByColumn`、`isAsc`（由 `startPage()` 从请求上下文取） |
| 列表响应 | `TableDataInfo`：`{ total, rows[], code, msg }` |
| 单对象/写操作响应 | `AjaxResult`：`{ code, msg, data }`；写操作走 `toAjax(rows)`，成功时 `data` 为空 |
| 数据范围 | 卡券列表与使用记录列表带 `@DataScopeSelf`：非管理员只返回「本人 + 同群组成员」的 `create_by` 数据；`/open`、`getCardByUserId` 等按当前用户过滤 |

## 2. 数据结构

### PxLoversCard（`px_lovers_card`）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键 |
| title | String | 卡片名称 |
| describe | String | 卡片描述（SQL 里用反引号转义） |
| logo / thumbnail | String | 卡片图 / 缩略图 |
| money | Integer | 价值金额 |
| number | Integer | 定期发放数量（新增时作为初始余量分发给双方） |
| delFlag | Long | 删除标志 |
| version | String | 版本号（客户端离线同步比对用） |
| createBy / createTime / updateBy / updateTime / remark | — | `BaseEntity` 公共字段，`createBy` 为字符串形式的用户 ID |

### PxCardRecord（`px_card_record`）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键 |
| cardId | Long | 卡片 ID |
| userId | Long | 使用用户 ID |
| instructions | String | 使用说明 |
| confirm | Boolean | 确认状态（0 待确认 / 1 已确认） |
| confirmTime | Date | 确认时间 |
| score | Integer | 评分 |
| scoreTime | Date | 评分时间 |
| delFlag / version | — | 同上 |

列表与详情接口返回的是 `PxCardRecordVo`，在上述字段之外附 `cardName`（= `px_lovers_card.title`）、`userName`（= `sys_user.nick_name`）。时间字段序列化格式 `yyyy-MM-dd HH:mm:ss`。

### PxLoversCardVo

继承 `PxLoversCard`，用于 `getCardByUserId`，附 `cardId`、`userId`、`cardNumber`（当前剩余张数）。

## 3. 接口清单

| # | 方法 | 路径 | 说明 |
|---|---|---|---|
| 1 | GET | `/px/card/list` | 卡券定义分页列表 |
| 2 | GET | `/px/card/listRecord` | 使用记录分页列表 |
| 3 | GET | `/px/card/record/{id}` | 单条使用记录 |
| 4 | GET | `/px/card/export` | 导出卡券列表 Excel |
| 5 | GET | `/px/card/{id}` | 卡券详情 |
| 6 | POST | `/px/card` | 新增卡券（同时给双方建余量） |
| 7 | PUT | `/px/card` | 修改卡券 |
| 8 | DELETE | `/px/card/{ids}` | 批量删除，逗号分隔 |
| 9 | GET | `/px/card/getCardByUserId` | 当前用户持有的有效卡券 |
| 10 | POST | `/px/card/useCard` | 发起使用（写记录 + 扣余量） |
| 11 | POST | `/px/card/confirmCard` | 对方确认 |
| 12 | POST | `/px/card/scoreCard` | 使用评分 |
| 13 | GET | `/px/card/getToDoCard` | 待我处理的卡券 |

### 3.1 GET /px/card/list — 卡券列表

Query（`PxLoversCard` 字段，均可选）：`title`（**精确匹配** `title = #{title}`，不是模糊）、`describe`、`logo`、`version` + 分页参数。

响应 `rows` 为 `PxLoversCard` 数组；XML 无默认排序，排序依赖 `orderByColumn`。

### 3.2 GET /px/card/listRecord — 使用记录列表

Query（`PxCardRecordVo`）：`cardName`（`c.title like '%x%'`）、`userId`（= `r.user_id`）、`instructions`、`version` + 分页。

固定 `order by r.create_time desc`；`r.create_by` 走数据范围过滤。响应为 `PxCardRecordVo`（含 `cardName`、`userName`）。

### 3.3 GET /px/card/record/{id}

路径 `id` = `px_card_record.id`。返回单条 `PxCardRecordVo`；记录不存在时 `data` 为 `null`。

### 3.4 GET /px/card/export

Query 同 `list`。生成 Excel 并返回 `AjaxResult`（`data` 为文件名，如 `card_xxxx.xlsx`），前端再走 `/common/download?fileName=...&delete=true` 下载。

操作日志：标题「情侣卡券」、类型 `EXPORT`。

### 3.5 GET /px/card/{id}

返回 `PxLoversCard` 全字段。

### 3.6 POST /px/card — 新增

Body（JSON）：`title`、`describe`、`logo`、`thumbnail`、`money`、`number`、`version`、`remark`。

事务内行为：插入 `px_lovers_card` → 按 `number` 为**男方 userId=1、女方 userId=2**（`UserConstants.MAN_USER_ID` / `WOMAN_USER_ID`）各插一条 `px_card_user` 余量。

成功返回新卡券 `id`（`data: <id>`），失败返回 `AjaxResult.error()`。

注意：`createBy/createTime` 是在 mapper 插入**之后**才 set 到实体上、未再落库，所以列表页的数据范围过滤对新卡可能不命中（已知实现细节）。

### 3.7 PUT /px/card — 修改

Body 必带 `id`，其余字段按非空动态更新（`<if>` 拼接，传 `null` 的字段不改）。服务端覆盖 `updateBy`（用户名）、`updateTime`。返回 `toAjax(rows)`。不会重算 `px_card_user` 余量。

### 3.8 DELETE /px/card/{ids}

`ids` 路径参数，多值逗号分隔（如 `/px/card/1,2,3`）。仅删卡券定义，不清理 `px_card_user` / `px_card_record`。

### 3.9 GET /px/card/getCardByUserId — 我的卡券

无参数，取登录用户 `userId`。SQL：`px_lovers_card` left join `px_card_user`，条件 `p.user_id = #{userId} and p.card_number > 0`。

返回 `PxLoversCardVo[]`：`id`（卡券 ID）、`logo`、`title`、`describe`、`money`、`cardId`、`userId`、`cardNumber`（剩余张数）、`version` 及 `px_card_user` 的审计字段。**不分页**，直接 `data` 数组。

### 3.10 POST /px/card/useCard — 使用卡券

Body（`PxCardRecord`）：`cardId`（必填）、`instructions`（使用说明）、`version`、`remark`。

服务端：`userId` 强制覆盖为当前登录用户 → 插入 `px_card_record`（`createBy`、`createTime`）→ 执行
`update px_card_user set card_number = card_number - 1 where card_id = ? and user_id = ?`。事务回滚异常。返回 `toAjax(rows)`。

注意：SQL 未校验 `card_number > 0`，余量可能扣成负数；`confirm` 未显式初始化，依赖 DB 默认值（查询侧按 `confirm = 0` 视为待确认）。

### 3.11 POST /px/card/confirmCard — 确认

Body：`id`（`px_card_record.id`，必填），其余字段可选。服务端强制 `confirm = true`、`confirmTime = now`，走 `updatePxCardRecord` 动态更新。未做归属/权限校验。

### 3.12 POST /px/card/scoreCard — 评分

Body：`id`、`score`（Integer）。服务端设 `scoreTime = now`，同一条 update 落库。未校验 `score` 范围与是否已确认。

### 3.13 GET /px/card/getToDoCard — 待处理

无参数。SQL 条件（`#{userId}` = 当前用户）：

```
(r.user_id = #{userId} and confirm = 1 and r.score = 0) or (r.user_id != #{userId} and confirm = 0)
```

即「我已用、对方已确认但我还没评分的」+「对方发起、还没确认的」。`order by r.create_time desc`，返回 `PxCardRecordVo[]`（含 `cardName`、`userName`）。

## 4. 状态流转

```
useCard ──► px_card_record(confirm=0 待确认)
             │  同事务: px_card_user.card_number - 1
             ▼
confirmCard ──► confirm=1, confirmTime
             ▼
scoreCard   ──► score, scoreTime（流程完结）
```

`getToDoCard` 消费前两个待办节点；`getCardByUserId` 消费余量。

## 5. 相关旁路接口

- **GET `/offline/sync/card?since=&offset=`**（`PxOfflineController`）：App 离线增量拉取，内部直接调 `getCardByUserId()`（未真正按 `since` 过滤），返回 `{ items, hasMore, nextSince }`，`hasMore` 以满 50 条判定。
- **POST `/offline/batch`**：离线批量提交，`tableName = "px_lovers_card"` 仅支持 `DELETE`（按 `payload.id` 删卡券定义）。
- **GET `/open/getSpecifyBusinessData/{businessType}`**：`businessType` 取 `love_card`（卡券全量，走 `selectPxLoversCardList`）或 `card_record`（使用记录全量，走 `selectPxLoversCardRecordList`）。注意 `/open/**` 的匿名放行在 `SecurityConfig` 里是注释掉的，当前仍需登录态。
- **AI 生活提醒**：`AiLifeReminderDataServiceImpl.buildLoversCardData()` 用 `getUsingCardRecord()`（`score` 为空或 0 的未完结记录，最多 5 条）拼 `title/description/confirm/score/userName/createTime`；`LifeReminderHandler` 场景 `lovers_card`。该链路无独立 HTTP 入口，经由 AI 工具意图分发。
- **定时发放**：`PxTask.grantCard()` → `regularGrantCard`（`update px_card_user c set card_number = c.card_number + (select number from px_lovers_card where id = c.card_id)`），由 Quartz 配置调度，非 HTTP 接口。
