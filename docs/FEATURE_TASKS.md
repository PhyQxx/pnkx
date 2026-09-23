# Pnkx 功能待办清单

> 生成于 2026-09-23，来源：功能完善度评估（基于代码证据）
> 优先级原则：先补"已承诺但断裂"的闭环，再加新能力。

## A. 完善现有功能（闭环断裂项）

- [x] A1 **邮件订阅闭环**（2026-09-23：新增 newArticle 邮件模板；notifyNewArticle 走 @Async("notifyExecutor") 线程池异步群发（新增 AsyncConfig）；发文入口挂触发，失败不影响发文）
- [ ] A2 **App 离线推送**：接入 uniPush，App 杀进程后提醒（纪念日/经期/待办）仍可触达；微信侧已有 WxSubscribeMessageTask
- [ ] A3 **评论回复通知**：访客被博主回复后邮件通知（评论体系已有邮箱字段与邮件服务）
- [x] A4 **博客传播三件套**（2026-09-23：src/server/routes/rss.xml.ts + sitemap.xml.ts（注意 srcDir=src 时 Nitro 只扫 src/server）+ public/robots.txt；文章页动态 OG/Twitter meta + 全局兜底；构建产物冒烟通过，线上文章数据已成功输出 RSS item）
- [ ] A5 提醒铃 60s 轮询兜底 → WebSocket 断线自动重连优化（低优先级）

## B. 新增功能（按投入产出排序）

- [x] B1 **数据自动备份**（2026-09-23：DbBackupTask 纯 JDBC 全量导出（DDL+INSERT，二进制 hex、流式 fetch）→ gzip → FTP pnkx-backup/ 目录，按文件名日期保留 30 天；上传失败本地保留兜底；V1.4.6 注册 sys_job 每日 03:00，管理端可手动触发）
- [ ] B2 **记账预算**：月度总预算/分类预算 + 进度条 + 超支走现有提醒引擎推送
- [ ] B3 **周期记账**：固定支出（房租/会员）周期规则 → 定时自动生成记录
- [ ] B4 **月度账单导出**：ExcelUtil 现成，按月导出 Excel/PDF
- [ ] B5 **年度报告**：AI 生活周报（AiLifeReportTask）扩展年度维度 + 前端可视化长页
- [ ] B6 登录设备管理（在线用户列表支持踢下线）/ 可选 2FA（低优先级）

## 执行记录

- 2026-09-23：完成 A1 + A4 + B1（后端编译零错误、client 构建通过、RSS/sitemap/robots 端点冒烟通过）
