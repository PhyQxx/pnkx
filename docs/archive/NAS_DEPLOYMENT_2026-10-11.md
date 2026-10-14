# NAS 发布记录：资料工作台

发布日期：2026-10-11。发布标识：`20261011-230200`。用户明确授权升级 NAS。

## 发布结果

- 已升级 NAS 上的 `pnkx-admin` 后端、`pnkx-ui` 管理端、`pnkx-uniapp` 移动端 H5。
- 管理端入口：[资料中心](https://admin.pnkx.top:8/resources/hub)、[资料问答](https://admin.pnkx.top:8/resources/ask)。
- Java 21 原生进程已重启；保留原命令行、环境、工作目录及 Jpom 应用标识。未改动 VPS 博客前台。
- 本次直接部署本地工作区构建产物，没有提交或推送 GitHub。之后从旧 GitHub 版本触发 Jpom 构建会覆盖本次发布，应先同步对应源码。
- 原生 Android/iOS App 安装包没有发布到设备。旧 App 不提交 revision 的笔记更新会得到 409；需更新对应客户端，不能把 NAS/H5 升级等同于手机安装包升级。

## 备份与发布文件

NAS 上的完整备份：

`/vol1/docker/jpom/backups/pnkx/20261011-230200`

包含旧后端 JAR、管理端和 H5 压缩包、约 84 MB 压缩数据库备份，以及恢复启动所需的环境/命令行快照。目录仅管理员可访问；环境快照可能包含凭据，不应复制到公开目录或提交版本库。

发布目录：`/vol1/docker/jpom/releases/pnkx/20261011-230200`。保留部署清单 `deployed.json`、验证结果 `verified.json`、迁移/查询演练日志及发布包。临时生产库副本和解包验证依赖已经删除。

| 产物 | SHA-256 |
|---|---|
| 后端 JAR | `f82ef0882e0aa12775d5deb01e52602ddb3fe629fbacaf413de035ba5b8677ef` |
| 管理端发布包 | `3691ac79e662bd4fae94ad824e6d4f0e120d100a6fe6be96f866b068a79bba65` |
| H5 发布包 | `e18b2812b3205350d72e58b43987dfb3e536ebde243fbe7e50d4ef7fa7812476` |
| 公网管理端 index.html | `774db9b3fef64dbfadee5a57453fa86c9ae628b20cf42c30164cf1c990550663` |

## 数据库与服务配置

- 升级前为 V1.6.3；先将完整备份恢复到独立副本，使用实际 Flyway 和本次 JAR 中的迁移完成演练。
- 副本与生产均成功执行 V1.6.4–V1.7.3，共 10 项迁移；没有修改已执行的历史迁移记录。
- 生产核验：138 条笔记均有 revision，138 条初始同步日志；重复待办 Quartz 任务仅注册 1 条。
- 副本验证了实际 MyBatis 的 12 类搜索及联合搜索，均成功。没有仅依赖最小测试表结构推断生产兼容。
- 私有文件目录：`/vol1/docker/jpom/data/pnkx-private-files`，默认每用户 200 MiB。
- 持久配置：`/vol1/docker/jpom/data/pnkx-admin/config/application.properties`，仅新增私有文件根目录和配额。原有 multipart 上限可容纳本次 20 MiB 单文件限制。
- 沿用默认 AI 模型和凭据，未修改模型配置或发起收费模型调用。

## 部署后验证

- 公网管理端首页 HTTP 200，内容哈希与部署文件一致；`/resources/hub`、`/resources/ask` SPA 回退正常。
- 公网入口 JavaScript 静态资源可访问，返回脚本而非 HTML。
- 资料、文件、笔记变更、问答、重复待办接口对匿名请求均返回现有 `code=401` 鉴权协议。
- 公网 OpenAPI 已包含新问答 SSE、私有文件上传、笔记变更与重复待办路由。
- SSO 发现端点可用，issuer 与升级前实测一致。当前实测值是 `http://admin.pnkx.top:8`；本次没有按旧运维笔记的不同值擅自修改 SSO。
- H5 发布目录含 7 个新的资料中心页面模块；已校验发布包和文件部署。未单独验证 H5 的公网域名入口或手机真机运行。
- 静态目录切换保留旧哈希资源供尚未刷新的页面加载，清理了 macOS 打包附带的 `._*` 元数据文件。

## 本地 H5 构建修复

部署时发现移动端缺少直接声明的 Vue、Vuex，且 H5 运行时与编译器版本不一致。已添加并锁定 Vue 3.4.21、Vuex 4.1.0、与编译器相同的 uni-h5 版本，更新 pnpm 锁文件，并提供可复现入口：

```bash
cd pnkx-uniapp
pnpm install --frozen-lockfile
pnpm build:h5
```

输出：`pnkx-uniapp/unpackage/dist/build/h5`。原构建和新增命令均实际通过，构建仍有既有 Sass 弃用提示。

## 回滚与待验收

如需回滚，应先停止当前后端，再恢复备份中的旧 JAR 和两端静态文件，使用受保护快照中的原启动参数/环境重启。静态切换前的目录也保留在部署根目录下的 `.pnkx-ui-20261011-230200-previous` 和 `.pnkx-uniapp-20261011-230200-previous`。

新增数据表应保留，**不自动覆盖生产数据库或删除新增数据**。旧后端不维护新的 revision/变更日志，紧急回滚后需要配套客户端及再次升级的同步补偿方案；完整数据库恢复会丢失备份之后的新写入，应单独评估。

仍待完成：登录后的真实用户全流程、H5 公网入口与手机真机双设备、真实默认模型问答/长流取消、实际提醒送达、性能压测。旧 App 离线库应先同步/导出，再升级；本次没有触碰手机本地数据。
