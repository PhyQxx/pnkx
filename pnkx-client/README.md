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
