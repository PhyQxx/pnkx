# Pnkx 功能待办清单

> 生成于 2026-09-23，来源：功能完善度评估（基于代码证据）
> 优先级原则：先补"已承诺但断裂"的闭环，再加新能力。

## A. 完善现有功能（闭环断裂项）

- [x] A1 **邮件订阅闭环**（2026-09-23：新增 newArticle 邮件模板；notifyNewArticle 走 @Async("notifyExecutor") 线程池异步群发（新增 AsyncConfig）；发文入口挂触发，失败不影响发文）
- [x] A2 **App 离线推送**（2026-09-23 代码侧就绪：px_push_device 表（V1.4.9）+ UniPushService（个推 REST v2，token Redis 缓存，未配置三要素全静默跳过）+ /system/push/device 上报接口 + WebSocketReminderPushChannel 双通道（站内 WS + 离线 uniPush）；客户端 App.vue 启动登记 clientId + 通知点击监听；**启用步骤**：DCloud 中心开通 uniPush 2.0 → HBuilderX 勾选 Push 模块 → 服务器注入 UNIPUSH_APPID/APPKEY/MASTERSECRET → 重新打包 App）
- [x] A3 **评论回复通知**（2026-09-23 更正：功能原本已存在（addMessage 回复分支+reply 模板），此前评估为误报；本轮加固——邮件改 notifyExecutor 异步发送，SMTP 故障不再阻塞评论提交（原同步发送失败会抛异常导致评论失败））
- [x] A4 **博客传播三件套**（2026-09-23：src/server/routes/rss.xml.ts + sitemap.xml.ts（注意 srcDir=src 时 Nitro 只扫 src/server）+ public/robots.txt；文章页动态 OG/Twitter meta + 全局兜底；构建产物冒烟通过，线上文章数据已成功输出 RSS item）
- [x] A5 WebSocket 断线自动重连（2026-09-23：ReminderBell 增加指数退避重连 1s→60s，连接成功重置；销毁标记阻断重连；轮询继续兜底）

## B. 新增功能（按投入产出排序）

- [x] B1 **数据自动备份**（2026-09-23：DbBackupTask 纯 JDBC 全量导出（DDL+INSERT，二进制 hex、流式 fetch）→ gzip → FTP pnkx-backup/ 目录，按文件名日期保留 30 天；上传失败本地保留兜底；V1.4.6 注册 sys_job 每日 03:00，管理端可手动触发）
- [x] B2 **记账预算**（2026-09-23：px_bookkeeping_budget 表（V1.4.7，总预算=type_id 0 保证唯一键生效）；CRUD+状态接口（SQL 联当月支出实时计算已用/剩余/百分比）；统计页 BudgetPanel 进度条（80% 黄/超支红）+ 设置弹窗；超支推送已完成：记账后实时检查，超支/≥90% 预警经推送通道提醒，同日同预算不重复）
- [x] B3 **周期记账**（2026-09-23：px_bookkeeping_recurring 表（V1.4.8，month/week 两频率）；规则 CRUD+到期生成（事务，单条失败不中断，记录归属规则创建者）；BookkeepingRecurringTask 每日 08:30（sys_job 注册）；记账页新增「周期记账」tab；停用期间到期账在重新启用后补记（符合固定支出语义））
- [x] B4 **月度账单导出**（2026-09-23：/bookkeeping/record/export 按当前筛选条件导出 Excel；记账页搜索栏「导出」按钮）
- [ ] B5 **年度报告**：AI 生活周报（AiLifeReportTask）扩展年度维度 + 前端可视化长页
- [ ] B6 登录设备管理（在线用户列表支持踢下线）/ 可选 2FA（低优先级）

## 执行记录

- 2026-09-23：完成 A1 + A4 + B1（后端编译零错误、client 构建通过、RSS/sitemap/robots 端点冒烟通过）
- 2026-09-23：完成 A3（更正+加固）/ A5 / B4 / B2 / B3（后端全量编译 0 错误、pnkx-ui 构建通过）
- 2026-09-23：完成 B2 增强（超支实时推送）+ A2 代码侧就绪（uniPush 双通道）
