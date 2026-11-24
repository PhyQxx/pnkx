# PNKX 访客页面与功能统计开发方案

日期：2026-11-24。pnkx-client 负责记录，pnkx-admin 负责接收和查询，pnkx-ui 负责查看。保留现有访客历史和百度统计，新增独立事件数据，不把旧 IP 记录冒充 UV。

## 一期交付范围

1. 全部客户端页面自动记录 PV、匿名 UV、会话、内容 ID、来源域名和前台有效停留。
2. 搜索打开、请求、结果点击；评论和留言提交及结果；AI 打开、请求、完成、失败和停止。
3. 管理端原访客菜单增加行为分析入口，提供概览、页面排行、功能排行、分页事件轨迹，支持日期、页面、访客和会话筛选。
4. 单张事件事实表起步，查询窗口最多 31 天，轨迹分页。不引入消息队列和日汇总，避免未验证的流量假设。后续按执行计划和数据规模决定预聚合。

## 统计口径

- visitorId：浏览器 localStorage 随机 UUID；存储不可用时退化为内存，不能代表自然人或跨设备身份。
- sessionId：标签页内 sessionStorage 存储，30 分钟无有效活动后更新。多标签页是不同会话，访客保持相同。
- pageViewId：每次页面展示独立 UUID。首次进入、路径变化、刷新计 PV；query 和 hash 不拆页。未知路径归为 other，不保存未知路径正文。
- 页面 pageKey：home、post_detail、category、category_detail、archives、video、video_detail、album、message、link、share、login、user、other。动态内容只保存数字 ID。
- 停留：页面可见且最近 60 秒内有指针、键盘或滚动交互，按 5 秒计时、15 秒上报增量。后台挂起间隔不补算。导航和隐藏时补报。平均停留=窗口内停留增量/PV，边界访问可能跨窗口。
- 时间：事件发生时间存北京时间 DATETIME，后端只接受过去 24 小时至未来 5 分钟。另存接收时间。日期查询左闭右开，最多 31 天。
- UV 和会话均在查询窗口内 COUNT DISTINCT，不相加每日 UV。会话数指窗口内活跃会话。
- 功能次数不含页面和停留事件；一期按事件名分别展示人数、次数，成功/失败不混成点击数。operationId 连接同一次尝试。结果是客户端观测结果，不作为审计凭证。

## 事件契约

POST /client/analytics/events，JSON 数组，每批 1 至 20 条。字段：eventId、visitorId、sessionId、pageViewId、eventName、pageKey、contentId、operationId、durationMs、referrerHost、occurredAt。UUID 必须合法，字符串有长度和字典约束。拒绝任意 properties，禁止正文、搜索词、token 和完整 URL。userId 仅从服务端认证上下文补充。

事件字典：page_view、page_duration、search_open、search_submit、search_success、search_fail、search_click、comment_submit、comment_success、comment_fail、ai_open、ai_submit、ai_success、ai_fail、ai_stop。评论在 message 页即留言，可按 pageKey 区分。

前端内存队列最多 100 条，批次失败保留原 eventId，后端唯一索引保证幂等。15 秒发送，隐藏/离页使用 keepalive fetch，失败静默；内存队列不保证关闭浏览器后的补传。开发默认关闭，通过运行时 public.analyticsEnabled 开关显式启用。

## 数据库与接口

迁移文件与代码同目录交付。px_analytics_event 独立建表，event_id 唯一；索引 occurred_at、page_key+occurred_at、visitor_id+occurred_at、session_id+occurred_at。原 px_visits 不修改。

GET /admin/analytics/report：startDate、endDate（包含结束日）、pageKey、visitorId、sessionId，返回 summary/pages/features。GET /admin/analytics/events：相同条件及 pageNum/pageSize（上限100），返回分页时间线。管理接口要求 admin:visits:list 权限，新增明确的访客分析查询权限；旧访客控制器没有显式权限注解，不假定现有角色已有此权限。可在菜单管理的访客菜单下新增按钮权限 admin:visits:list，再分配给运营角色。

匿名采集复用 ToolboxRateLimiter，使用 Redis 按 IP 每分钟最多 120 批限流（Redis 异常时现有组件放行，网关需配置相同的兜底限制），IP 仅作为临时限流键，不进入事件表。事件校验全批执行后事务写入；重复 eventId 不更新原记录。上线网关需限制该接口请求体为 64KB，限制源站直连；应用层限制批次和字段。客户端上报天然可伪造，因此分析仅作产品使用参考。

## 管理页面

原访客页顶部展示行为分析卡片，原列表和导出保持可用。筛选日期默认最近7天；卡片PV/UV/会话/有效停留；页面表展示pageKey、contentId、PV/UV/停留；功能表展示eventName、人数、次数；轨迹展示时间、访客、会话、页面、操作和关联ID。支持点击轨迹访客/会话收窄查询，空状态和请求失败必须可区分。

## 文件划分

- pnkx-client/src/utils/analytics.ts：纯函数及字典，方便 Node 测试。
- pnkx-client/src/plugins/analytics.client.ts：生命周期、身份、采集和发送。
- pnkx-client/src/composables/useAnalytics.ts：组件调用，SSR 安全无操作。
- pnkx-admin/pnkx-blog：事件模型、Mapper、查询 SQL。
- pnkx-admin/pnkx-admin：接收校验、限流、管理查询。
- pnkx-ui/src/api/px/blog/analytics.js：API；访客目录 Analytics.vue：展示。

## 验证与发布

自动验证：路由归类/隐私过滤；非法事件、时间和批次拒绝；重复事件 SQL 唯一约束；管理权限；前端构建；后端编译及定向测试。手工联调：直接进入、刷新、前进后退、切后台、30分钟过期、接口失败重试、跨日查询、无权限账号不能查看、搜索/评论/AI正常和失败路径。

部署顺序：备份数据库→执行建表SQL→后端部署→给运营角色授予 admin:visits:list→管理端部署→客户端生产启用采集→用测试访客核对数据。不得把开发测试写入生产统计。回滚先关客户端开关，再回滚前后端；新增表保留，避免丢数据。

明细保留90天；上线时配置每日低峰分批删除 occurred_at 早于90天的数据，每次最多10000条直到完成，并监测锁等待。首次发布不自动删除历史。后续汇总长期保留需另加汇总表和精确去重结构。

## 后续迭代

二期：播放器实际播放进度、阅读深度、登录/注册、回复评论、相册预览、导航和外链。三期：服务端业务结果确认、操作漏斗、来源/设备/地域分析、回访和日汇总。每项新增事件需同步字典、字段说明、测试和指标口径，不采集任意DOM点击。


## 第二批已落地变更

文章正文位置阈值与原生视频行为已实现，不再仅停留在二期计划。新增事件 read_25/read_50/read_75/read_100、video_select/video_start/video_25/video_50/video_75/video_complete/video_error。阅读位置根据正文矩形和视口底部计算，不代表阅读理解；播放位置也不代表累计观看时长。

新增可选 targetId，只接受1至20位数字，记录目标视频ID，避免将列表页归属误改为视频详情页。新增迁移002，首次安装依次执行001和002。管理端新增操作成功率视图，查询按访客、会话、operationId、功能聚合；提交及结果均受同一个时间和页面筛选窗口约束。多步骤漏斗继续列为后续开发。
