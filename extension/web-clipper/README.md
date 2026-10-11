# pnkx 网址收藏 浏览器插件

一键收藏当前网页到 pnkx「网址收藏」，后端自动抓取正文并 AI 生成摘要标签。

## 功能

- 点击图标 → 显示当前页标题/链接 → 填备注（可选）→ 收藏
- 快捷键 `Ctrl+Shift+S`（Mac `Cmd+Shift+S`）一键静默收藏，系统通知提示结果
- 自动登录换取 token（pnkx 登录无验证码），token 过期自动重新登录
- 调用 `/client/webArticle` 接口，收藏归属当前登录用户，按用户隔离

## 安装（Chrome / Edge 开发者模式）

1. 打开浏览器扩展管理页
   - Chrome：`chrome://extensions`
   - Edge：`edge://extensions`
2. 打开右上角「开发者模式」
3. 点击「加载已解压的扩展程序」，选择本目录（`code/extension/web-clipper`）
4. 点击插件图标 → 「设置」：
   - 服务器地址：后端 API 根地址（如 `https://api.pnkx.top`，含端口）
   - 用户名/密码：pnkx 登录账号
   - 点「测试连接」验证通过即可使用

## 使用

- 任意网页点击插件图标 → 「收藏」
- 或直接 `Ctrl/Cmd+Shift+S`，收藏成功弹出系统通知
- 收藏后在管理端「我的应用 → 网址收藏」或 App「生活助手 → 网址收藏」查看 AI 整理结果

## 文件结构

```
web-clipper/
├── manifest.json    # MV3 清单（含快捷键命令、host_permissions 免 CORS）
├── common.js        # 共享逻辑：登录换 token、收藏、通知
├── popup.html/js    # 弹窗：当前页信息 + 备注 + 收藏
├── options.html/js  # 设置：服务器地址 / 账号密码 / 测试连接
├── background.js    # Service Worker：快捷键静默收藏
└── icons/           # 16/48/128 图标
```

> 密码仅保存在浏览器本地（chrome.storage.local），仅用于向后端登录换取 JWT 令牌，不会发送给任何第三方。
