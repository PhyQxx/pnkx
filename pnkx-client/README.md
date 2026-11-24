## 启动项目
```shell
#首先安装依赖
pnpm install

# 本地运行
pnpm dev

# 打包分析
pnpm analyze

# 打包
pnpm build

```

## 环境与部署

使用 Node.js 22.22.2（见 `.nvmrc`）及 pnpm 10.34.4。
依赖安装使用 `pnpm install --frozen-lockfile`，提交 `pnpm-lock.yaml` 保证构建一致。
开发读取 `.env.dev`，生产构建读取 `.env.prod`；执行前确认 API 地址。

Docker 使用多阶段构建，仅打包 `.output`，通过 Node 启动服务。
`bash build.sh` 构建成功后替换容器，检查 `/api/health`；失败时尝试恢复旧容器。
健康检查仅验证应用进程，不代表后端业务服务可用。

## 性能检查

构建后运行 `node scripts/measure-client.mjs`，统计入口、首页和默认布局的静态 JS 依赖。
该统计不包含路由预取、运行时网络请求，不代表浏览器实测加载时间。

本轮优化前：1,042,582 字节（gzip 350,480）；优化后：1,000,362 字节（gzip 338,500）。
AI 面板首次点击时加载，关闭后保留会话；流式内容每 50ms 合并更新，结束时立即刷新尾部。
Markdown 解析器按 Nuxt 应用实例复用，不在每次渲染时重新初始化。

使用项目要求的 Node 版本执行 `node --test tests/*.test.cjs` 运行回归测试。
