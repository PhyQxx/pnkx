# 优化改动的验证清单

> 本轮共 14 个提交：安全加固、性能优化、幂等修复、工程化、三项重构、历史清洗。
> 按下述顺序验证：① 启动准备 → ② 冒烟（核心路径）→ ③ 边界用例（安全项）→ ④ 构建产物。
> 每项操作后预期结果已注明，不符合即为本轮改动引入的回归。

## ① 启动准备（有破坏性变化，先读这段）

### 后端必须先设置 TOKEN_SECRET（否则拒绝启动，这是新增的 fail-fast）

```bash
cd pnkx-admin
export TOKEN_SECRET=$(openssl rand -base64 64)   # ≥64 字符，生成一次后固定复用
export DB_PASSWORD='你的本地MySQL密码'
mvn spring-boot:run -pl pnkx-admin
# 或打包运行：mvn -DskipTests package && java -jar pnkx-admin/target/pnkx-admin.jar
```

**验证 fail-fast 本身**：不设 TOKEN_SECRET 直接启动 → 应打印
`token.secret 未配置…服务拒绝启动` 后退出（这是预期行为，不是 bug）。

启动日志中还应看到：
- Flyway 执行 `V1.4.5__wallpaper_like_unique`（点赞去重 + 唯一索引 + 计数校正）；
- 未设 `WX_SECRET` 时有一条 warn（微信登录不可用），不影响启动。

### 三个前端

```bash
# 管理后台
cd pnkx-ui && npm run dev          # 或 npm run build 后托管 dist
# 博客前台
cd ../pnkx-client && pnpm dev
# 移动端 H5（HBuilderX 或 CLI）
```

## ② 冒烟：核心路径

### pnkx-ui 管理后台

| 操作 | 预期 |
|---|---|
| 登录（勾选"记住我"）→ 重开浏览器 | 用户名自动回填，**密码不再回填**（预期：已降级） |
| 首页待办/账单卡片 | 正常渲染（经期提醒卡片文字样式正常 = sanitizeHtml 放行 span） |
| 右上角提醒铃 | 能建立 WebSocket（Network 里 ws 连接 URL 带 `?token=`），实时提醒弹出 |
| 聊天窗（右下角） | WebSocket 连接成功、消息收发正常 |
| 记账页：新增/编辑/删除/查询 | 正常；记录列表、账户管理、分类管理、图表统计四个 tab 均正常 |
| 记账页「批量记账」 | 打开弹窗、加行、填多行（留空=同上）、提交成功——验证抽出的 mixin |
| 记账页「AI 导入」 | 输入多行描述（如"昨天午饭 20 今天打车 15"）→ 解析出记录行 |
| 首页「本月账单分析」 | AI 流式分析正常输出（验证 PxBookkeepingAiService 重构） |
| 博客管理→统计→更多统计 | 进入页面后地图正常显示（china.json 改为按需 chunk，Network 中该请求在进页时才发出） |
| 文件管理（笔记/Obsidian 页） | 管理员可正常列出/读写；新建文件只允许 md/txt/图片等白名单类型 |
| 上传一张大图（相册/头像） | 分片上传进度正常、合并成功、FTP 地址可访问 |

### pnkx-client 博客前台

| 操作 | 预期 |
|---|---|
| 文章详情 | Markdown 正常渲染：代码块高亮、**复制按钮可用**（DOMPurify 消毒后 button/textarea 应保留）、自定义颜色语法正常 |
| 评论：输入含换行/`<script>` 的内容 | 换行正常显示为多行；script 标签原样显示为文本、**不执行** |
| 留言板/视频页弹幕 | 弹幕纯文本显示，HTML 不渲染 |
| 壁纸点赞 | 登录后点赞/取消正常，刷新后状态保持 |
| 注册新账号 | 收到激活邮件，链接带 `activationToken`，点击激活成功 |
| 忘记密码 | 收到重置邮件，链接带 `restToken`，页面弹出新密码，可用其登录 |

### pnkx-uniapp（H5 先测，App/小程序需真机）

| 操作 | 预期 |
|---|---|
| 首页/登录/我的 | 全局背景图正常（JPEG 化后无视觉劣化） |
| AI 助手对话 | H5 流式输出正常；**App/小程序需真机回归**（enableChunked 改造） |
| 首页公告卡片 | 图片正常显示（正则提取改造） |
| 记一笔（含纪念日联动） | 正常；批量导入多笔更快（N+1 已修） |

## ③ 边界用例：安全项

```bash
BASE=http://localhost:8068
# 1. 匿名重置密码应被拒（改前任何人可重置任意账号）
curl -s $BASE/restPassword/admin -X POST | head -c 200
# 预期：报错（缺 resetToken 参数/无效令牌），绝不返回新密码

# 2. 文件管理器匿名访问应 401
curl -s -o /dev/null -w "%{http_code}\n" "$BASE/system/file-manager/list"
# 预期：401

# 3. WebSocket 伪造身份应被拒（用无效 token 连接）
curl -s -i -N -H "Connection: Upgrade" -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Version: 13" -H "Sec-WebSocket-Key: x3JJHMbDL1EzLkh9GBhXDw==" \
  "$BASE/websocket/1?token=invalid" | head -1
# 预期：握手后立即被关闭（连上即收不到消息）；对照：带正确 token 的前端连接正常

# 4. 登录限流：连续 5 次错误密码后
for i in 1 2 3 4 5 6; do
  curl -s -X POST $BASE/clientLogin -H 'Content-Type: application/json' \
    -d '{"userName":"admin","password":"wrong"}' | head -c 80; echo
done
# 预期：第 6 次起返回"登录失败次数过多"，10 分钟后恢复

# 5. Swagger/Druid 默认关闭
curl -s -o /dev/null -w "swagger:%{http_code} " $BASE/swagger-ui/index.html
curl -s -o /dev/null -w "druid:%{http_code}\n" $BASE/druid/index.html
# 预期：均非 200（默认关闭；开发要开：SWAGGER_ENABLED=true DRUID_STAT_ENABLED=true）

# 6. 分片上传路径穿越被拒（抓包改 identifier 参数为 ../../x 后上传）
# 预期：返回"非法的文件标识"，上传目录外无新文件
```

数据库侧（V1.4.5 迁移）：

```sql
SHOW INDEX FROM px_like_record;             -- 应有 uk_like_user_item (item_id, type, create_by)
SELECT COUNT(*) FROM px_like_record r       -- 重复点赞应为 0
GROUP BY item_id, type, create_by HAVING COUNT(*)>1;
```

并发点赞幂等（可选）：对同一壁纸快速连点两次 → like_count 只 +1。

## ④ 构建产物与工程化

```bash
# 后端全量编译 + 测试
cd pnkx-admin && mvn clean test-compile && mvn test -pl pnkx-admin -am -Dtest=SysFileManagerControllerTest

# pnkx-ui：lint 0 error + 构建（首屏 JS 应约 2.2MB，原 6.28MB）
cd ../pnkx-ui && npm run lint && npm run build

# CI：推送后 GitHub Actions 四个 job 应全绿
# https://github.com/PhyQxx/pnkx/actions

# FTP 故障演练（可选）：停掉 FTP 服务后上传图片
# 预期：收到"FTP 连接失败"明确报错，数据库不出现 /ftp/xxx/null 脏记录
```

## 已知的环境差异（非 bug）

- Node 18 跑不动 pnkx-client 构建（依赖树要求 ≥20.12），本机用 nvm 的 v22.23.1；
- `~/.npmrc` 指向局域网 registry，离线装包需加 `--registry=https://registry.npmmirror.com`；
- 生产部署环境变量清单见 [DEPLOY.md](./DEPLOY.md)。

## ⑤ 功能开发批次验证（2026-09-23 第二批：11 个新能力）

### A1 邮件订阅通知
1. 博客前台用测试邮箱订阅 → 管理后台发布新文章 → **预期**：订阅邮箱收到「文章更新」邮件（异步发送，发文不等邮件；无订阅者时日志打印"跳过通知"）

### A4 RSS / Sitemap / OG（部署 pnkx-client 后）
```bash
curl -s https://pnkx.top/robots.txt        # 应输出 robots + sitemap 指向
curl -s https://pnkx.top/rss.xml | head    # <channel> + 文章 <item> 列表
curl -s https://pnkx.top/sitemap.xml | head# 静态页 + /post/{id} 列表
```
微信/Twitter 发文章链接 → 出现标题+封面预览卡片

### B1 数据自动备份
1. 管理端「监控→定时任务」→ 找到「数据库自动备份」→ 执行一次
2. **预期**：日志输出"备份统计：N 张表，M 行数据"；FTP 根目录出现 `pnkx-backup/pnkx-backup-日期.sql.gz`；本地临时文件已清理

### B2 记账预算 + 超支推送
1. 统计 tab → 预算面板 → 设置总预算（小于本月已支出）→ **预期**：进度条红色 + 超支标签
2. 再记一笔支出 → **预期**：右上角通知弹「预算超支提醒」（当天同预算不重复弹）
3. 数据库：`SHOW CREATE TABLE px_bookkeeping_budget` 有唯一索引 uk_month_type_user

### B3 周期记账
1. 记账页「周期记账」tab → 新增"测试规则"（每月 28 号 / 金额 1 元）
2. 管理端定时任务执行「周期记账生成」一次（若当天未到 28 号应无生成）
3. 临时把某规则 next_run_date 改为今天再执行 → **预期**：生成一条带「周期·每月」备注的记录，且规则 next_run_date 推进到下月

### B4 账单导出
记账页搜索栏「导出」→ 下载 Excel，内容与当前筛选条件一致（含金额/分类/备注列）

### B5 年度报告
1. 生活报告页 → 周期切「今年」→ **预期**：出现月度支出柱状图 + 分类 Top5 排行
2. 生成 AI 报告 → **预期**：流式输出"年度关键词/消费盘点/高光时刻/写给明年"结构的 Markdown；历史记录里标签显示「年报」

### A3/A5 加固项
- A3：留言/回复提交应即时返回（不再被 SMTP 拖慢）；断开邮件服务后评论仍能提交成功
- A5：管理后台开 DevTools Network 断网 5 秒再恢复 → **预期**：提醒 WebSocket 自动重连（Console 可见重连，无需刷新页面）

### A2 uniPush（需先完成平台开通，见 FEATURE_TASKS.md 四步）
装新包登录后数据库 `px_push_device` 出现设备记录；杀掉 App 进程 → 触发一条提醒 → 锁屏收到通知

### B6 登录设备管理
管理端「系统监控→在线用户」（RuoYi 自带）：列表可见当前会话，「强退」后该会话接口调用返回 401
