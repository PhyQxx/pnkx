# App / 小程序真机回归清单

> 生成于 2026-09-24。代码与平台编译已通过；本机 HBuilderX 未检测到 Android、iPhone 或 iOS Simulator 设备，因此下列真机交互项需在接入设备后执行。不得用编译成功替代真机通过结论。

## 已完成的前置验证

- [x] HBuilderX 5.26 微信小程序编译成功，产物位于 `pnkx-uniapp/unpackage/dist/dev/mp-weixin`。
- [x] HBuilderX 5.26 App 本地资源导出成功，产物位于 `pnkx-uniapp/unpackage/resources`。
- [x] `pages.json` 可解析，新增页面均进入路由清单。
- [x] 移动端契约测试 6/6 通过：离线同步 3 项，SSE 分片/深链/鉴权下载分享 3 项。
- [x] PC 生产构建、后端 Maven reactor 打包和核心链路测试通过。
- [x] 微信开发者工具启动链路已验证到运行时入口；当前工具返回错误码 10（需要重新登录），登录恢复后可继续预览。

## 设备矩阵

至少覆盖一台 Android 真机和微信开发者工具；发布 iOS 时再补一台 iPhone。每个用例记录设备、系统版本、网络环境、结果和截图/日志路径。

| 平台 | 最低覆盖 | 当前状态 |
| --- | --- | --- |
| Android App | Android 真机，HBuilderX 5.26 标准基座 | 待接入设备 |
| iOS App | iPhone 真机，HBuilderX 5.26 标准基座 | 待接入设备 |
| 微信小程序 | 微信开发者工具 + 真机预览 | 编译通过；开发者工具待重新登录，真机预览待执行 |

## 回归用例

### 流式响应

- [ ] AI 对话能持续追加分片，结束时正确处理 `[DONE]`，中断/重试后不重复写入。
- [ ] 待确认写操作显示草稿；确认、取消、过期分别产生正确状态。

### 下载与分享

- [ ] 月度账单导出后可下载、打开并调用系统分享。
- [ ] 下载失败时提示明确，重试不会产生不可识别的空文件。

### 离线同步

- [ ] 飞行模式下新增、修改、删除待办/日记/账单/笔记后，恢复网络可收敛。
- [ ] 购物、菜谱、餐饮、订阅、经期、预算、周期记账和阅读进度能增量拉取。
- [ ] 制造版本冲突后，可选择丢弃本地、使用服务端或手工合并，数据不静默丢失。

### uniPush 与深链

- [ ] 待办、纪念日、经期、订阅和预算预警均能收到通知。
- [ ] 点击通知直达对应详情；来源已删除时安全回退到列表页。
- [ ] 免打扰时段和渠道开关生效；失败记录可重试且不会重复推送。

### 家庭空间与自动化

- [ ] 两个账号共享待办、购物、餐饮、纪念日和预算，私有日记/笔记/经期/订阅不可见。
- [ ] 所有权转移后原所有者可退出；退出后共享访问立即撤销，历史数据归创建者。
- [ ] 五类自动化模板可试运行、启停和失败重放，重复触发不重复写入。

## 执行命令

```bash
/Applications/HBuilderX.app/Contents/MacOS/cli devices list --platform android
/Applications/HBuilderX.app/Contents/MacOS/cli devices list --platform ios-iPhone
/Applications/HBuilderX.app/Contents/MacOS/cli launch app-android --project /Users/peihaoyu/PHY/pnkx/code/pnkx-uniapp
/Applications/HBuilderX.app/Contents/MacOS/cli launch app-ios --project /Users/peihaoyu/PHY/pnkx/code/pnkx-uniapp --iosTarget device
/Applications/HBuilderX.app/Contents/MacOS/cli launch mp-weixin --project /Users/peihaoyu/PHY/pnkx/code/pnkx-uniapp
```
