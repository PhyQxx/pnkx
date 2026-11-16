# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在本代码库中工作时提供指导。

## 项目概述

这是"Pnkx-UniApp"（Pei你看雪），一个基于 UniApp 的移动应用，支持 iOS/Android APP、微信小程序和 H5。使用 Vue 2 构建，是一款生活管理类应用，包含博客/内容管理、财务记账、待办事项、纪念日提醒、情侣相关功能。

## 开发环境

本项目使用 **HBuilderX** 作为主 IDE。没有 npm 构建脚本 - 开发、构建和发布通过 HBuilderX 集成工具完成。

开发步骤：
1. 在 HBuilderX 中打开项目
2. 使用 HBuilderX 内置的预览/调试功能进行开发
3. 使用 HBuilderX 的发布工具构建不同平台

## 配置文件

- **config.js**: 全局配置，包括 API 基础地址 (`https://admin.pnkx.top:8/prod-api`)、应用信息和功能开关。本地开发可使用注释掉的 `localhost:8068`。
- **pages.json**: 页面路由配置，包含子包懒加载。定义了 3 个主 tabBar 页面（首页、工作台、我的）。
- **manifest.json**: 应用清单，版本 1.4.1，微信小程序 ID (`wx1a797ab499d31dac`)，以及各平台特定配置。
- **main.js**: 应用入口 - 注册全局组件（`SvgIcon`、`NoData`）、插件、过滤器和 Vuex store。使用 moment.js 并设置中文语言环境。

## 架构

### 页面组织

- **pages/**: 主页面（预加载）- login、index、function（工作台）、mine
- **pages_system/**: 用户管理和通用工具页面（子包，懒加载）
- **pages_life/**: 生活功能页面，包含记账（子包，懒加载）
- **pages_blog/**: 博客相关页面（子包，当前为空）
- **staticPackage/**: 静态资源页面（占位子包）

### API 层 (`api/`)

API 按功能域组织：
- `api/login.js`: 认证（登录、登出、获取用户信息、验证码）
- `api/px/blog/`: 博客系统（文章、照片、留言等）
- `api/px/life/`: 生活功能，包括：
  - `bookkeeping/`: 财务管理（账户、记录、分类、统计）
  - `card.js`: 情侣卡片系统
  - `todo.js`: 待办事项管理
  - `menstruationRecord.js`: 经期记录
  - `commemorationDay.js`: 纪念日提醒
- `api/system/`: 系统工具（配置、字典、菜单、用户、通知）

**API 模式**: 所有 API 函数使用 `utils/request.js` 中的 `request` 封装：
```javascript
// 列表查询，使用 params
export function listRecord(query) {
  return request({
    url: '/bookkeeping/record/list',
    method: 'get',
    params: query
  })
}

// CRUD 操作遵循 RESTful 规范
// GET 获取单项，POST 创建，PUT 更新，DELETE 删除
```

### 状态管理 (`store/`)

Vuex store 采用模块化结构：
- `store/modules/user.js`: 用户认证状态（token、角色、权限、用户信息）
- 用户数据通过 `utils/storage.js` 和 `utils/constant.js` 持久化存储
- Getters 定义在 `store/getters.js`

**核心 Actions**:
- `Login`: 认证用户并存储 token
- `GetInfo`: 获取用户详情、角色、权限
- `LogOut`: 清除 token 和所有存储的用户数据

### 认证与路由守卫

**permission.js** 实现路由拦截器：
- 白名单页面：`/pages/login`、`/pages/register`、`/pages/common/webview/index`
- 未认证时自动跳转到登录页
- 已认证时访问登录页自动跳转到根路径

**Token 管理** (`utils/auth.js`):
- 使用 Bearer token 认证
- Token 通过 `utils/request.js` 自动附加到请求
- 401 响应触发重新认证提示

### 请求封装 (`utils/request.js`)

集中式 HTTP 客户端，具有：
- 自动附加 token（可通过 `isToken: false` header 禁用）
- 10 秒默认超时
- 错误处理及用户友好提示（通过 `utils/errorCode.js`）
- 401 处理：提示用户重新登录
- 500 及其他错误处理：toast 通知

### 工具函数 (`utils/`)

- `request.js`: HTTP 客户端封装
- `auth.js`: Token 管理（getToken、setToken、removeToken）
- `pnkx.js`: 自定义工具（日期、数组、排序、防抖/节流）
- `common.js`: UI 辅助函数（toast、showConfirm、tansParams）
- `storage.js`: 本地存储封装
- `constant.js`: 存储键和常量
- `filters.js`: Vue 过滤器

### 全局组件

- `SvgIcon`: 图标组件，使用拼音命名规范
- `NoData`: 空状态组件

### 插件 (`plugins/`)

- `plugins/index.js`: 插件注册
- `plugins/auth.js`: 认证插件
- `plugins/tab.js`: 导航插件
- `plugins/modal.js`: 模态框插件

## 命名规范

- API 函数遵循 RESTful 模式：`list*`、`get*`、`add*`、`update*`、`del*`
- 目录使用 kebab-case，文件名根据上下文使用 kebab-case 或 camelCase
- 组件使用 PascalCase
- 页面标题和 UI 文本使用中文

## 依赖

- `moment`: 日期/时间处理（配置为中文语言环境）
- `js-pinyin`: 拼音转换（用于图标命名）
- Vuex: 状态管理

## 样式相关
- `uview-ui`: UI 框架
- 尺寸单位优先使用rpx

## 图标规范
- SVG 图标文件名使用拼音命名（如 `aixin.svg`、`jizhang.svg`）
- 图标存放在 `staticPackage/static/svg/` 目录
- 使用 `SvgIcon` 全局组件渲染图标
