# Pnkx-UniApp 开发指南

## 构建和开发

**无 npm 脚本** - 此项目使用 HBuilderX 作为主要开发环境。

- **开发**: 在 HBuilderX 中打开项目，使用内置的预览/调试功能
- **构建**: 使用 HBuilderX 的分发工具构建 iOS/Android、微信小程序或 H5
- **测试**: 未配置测试框架 - 通过 HBuilderX 预览验证更改

## 代码规范

### 导入规范
- 使用 `@/` 别名导入项目根目录模块
- 逻辑分组导入（框架 → 项目模块 → 工具类）
```javascript
import Vue from 'vue'
import request from '@/utils/request'
import {getToken} from '@/utils/auth'
import SvgIcon from '@/components/SvgIcon/index.vue'
```

### 文件和命名规范

**目录**: kebab-case (`api/px/life/bookkeeping/`)

**文件**:
- API 文件: kebab-case 或 camelCase (`record.js`, `card.js`)
- 组件: PascalCase (`SvgIcon/index.vue`)
- 工具类: camelCase (`request.js`, `common.js`)

**标识符**:
- API 函数: 带动作前缀的 camelCase (`listRecord`, `getInfo`, `addRecord`, `updateRecord`, `delRecord`)
- Vue 组件方法: camelCase
- Vuex mutations: UPPER_SNAKE_CASE (`SET_TOKEN`, `SET_NAME`)
- Vuex actions: PascalCase (`Login`, `GetInfo`, `LogOut`)
- CSS 类: kebab-case

### API 模式

所有 HTTP 调用使用 `utils/request.js` 封装:
```javascript
// 列表查询使用 params
export function listRecord(query) {
  return request({
    url: '/bookkeeping/record/list',
    method: 'get',
    params: query
  })
}

// CRUD 遵循 RESTful
export function addRecord(data) {
  return request({
    url: '/bookkeeping/record',
    method: 'post',
    data: data
  })
}
```

### Vue 组件结构

标准模板:
```vue
<template>
  <!-- HTML 内容 -->
</template>

<script>
export default {
  name: "ComponentName",
  components: {},
  props: {
    propName: {
      type: String,
      default: ''
    }
  },
  data() {
    return {}
  },
  onLoad() {},
  onShow() {},
  mounted() {},
  methods: {
    methodName() {}
  }
}
</script>

<style lang="scss" scoped>
/* CSS 内容 */
</style>
```

### 错误处理

集中在 `utils/request.js`:
- 401: 自动重新登录提示
- 500: Toast 错误消息
- 其他错误: 从 `utils/errorCode.js` 显示 Toast
- 使用 `utils/common.js` 中的 `toast()`, `showConfirm()`

### 注释规范

- 业务逻辑使用中文注释
- 工具函数使用 JSDoc 风格
```javascript
/**
 * 时间格式过滤器
 * @param valueTime
 * @returns {string}
 */
```

### 样式规范

- 使用 SCSS，设置 `lang="scss"` 和 `scoped`
- 主单位: `rpx` (响应式像素)
- UI 框架: uview-ui (组件库)
- 主题颜色通过 SCSS 变量 (`$theme-blue`, `$theme-red` 等)

### Vuex Store

`store/modules/` 中的模块化结构:
- 状态从存储初始化
- Mutations 持久化到存储
- Actions 返回 Promise 用于异步操作

### 关键文件

- `config.js`: 全局配置 (API 基础 URL)
- `pages.json`: 路由和 tabBar 配置
- `permission.js`: 路由拦截器和认证守卫
- `utils/auth.js`: Token 管理
- `utils/request.js`: HTTP 客户端封装
