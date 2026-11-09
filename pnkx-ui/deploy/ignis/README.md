# Ignis iframe 适配

2026-11-09 已应用到容器 `ignis`（Ignis 0.8.10，Obsidian 1.12.7）。

Obsidian 启动函数沿 `window.parent` 查找 Electron require，跨域嵌入时抛出 SecurityError。embed-compat.js 在 Ignis loader 之前将当前 renderer 的 parent getter 指向自身。没有修改 Obsidian 文件、关闭同源策略或向父页面暴露 require；独立窗口保持原样。依赖真实父窗口的插件可能需要另行适配。

服务器目录 `/vol1/1000/docker/ignis/embed-compat/` 保存原始 loader、适配后的 loader 和修改前 Compose 文件。Compose 以只读方式挂载适配后的文件到 `/app/packages/shim/dist/shim-loader.js`。另外挂载 Ignis 自己的 HTML 模板到 `/app/apps/ignis-server/server/assets/index.html`，将模板的 `__SHIM_LOADER_SRC__` 替换为 `__SHIM_LOADER_SRC__&embed=pnkx1`，使一年 immutable 缓存失效。原始模板保存在 index.original.html。

升级 Ignis 时必须从新镜像提取 loader，重新运行 prepare-loader.py，再重建容器；不要继续挂载旧版 loader。示例（先从新镜像的临时容器复制源文件）：

```sh
python3 prepare-loader.py shim-loader.original.js shim-loader.js
```

升级时也要从新镜像提取 HTML 模板并重新添加缓存标记。

回滚：删除 HTML 模板挂载行以及 Compose 中 `./embed-compat/shim-loader.js:/app/packages/shim/dist/shim-loader.js:ro` 挂载行，然后 `docker compose up -d --no-deps ignis`。笔记库与 Obsidian 本体没有被修改。

本地测试：`node --test deploy/ignis/embed-compat.test.cjs`（在 pnkx-ui 下运行）。浏览器验收需验证跨域 iframe 内目录和编辑器渲染，不以 HTTP 200 或 iframe load 事件代替启动成功。
