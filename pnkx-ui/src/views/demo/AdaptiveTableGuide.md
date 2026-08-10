为了在实际项目中更优雅地实现“表格高度自适应且内部滚动”，我们可以针对复杂的页面结构（如带左侧树的“用户管理”页面）进行如下改造。

### 1. 复杂页面布局改造方案 (带左侧树)

在 `system/user/index.vue` 这类页面中，我们需要保证左侧树和右侧表格都能独立滚动，且整体不超出屏幕。

```vue
<template>
  <!-- 容器：限制高度并禁止全局滚动 -->
  <div class="app-container adaptive-layout">
    <el-row :gutter="20" class="full-height-row">
      
      <!-- 左侧部门树：独立滚动 -->
      <el-col :span="4" :xs="24" class="left-aside">
        <div class="aside-header">
           <el-input placeholder="搜索部门" v-model="deptName" clearable size="small" />
        </div>
        <div class="aside-content">
          <el-tree :data="deptOptions" default-expand-all />
        </div>
      </el-col>

      <!-- 右侧用户数据：Flex 垂直布局 -->
      <el-col :span="20" :xs="24" class="right-main">
        
        <!-- 搜索表单 (固定高度) -->
        <el-form :model="queryParams" ref="queryForm" :inline="true" class="search-area">
          <el-form-item label="用户名称">
            <el-input v-model="queryParams.userName" size="small" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="small">搜索</el-button>
          </el-form-item>
        </el-form>

        <!-- 操作按钮 (固定高度) -->
        <el-row :gutter="10" class="action-area mb8">
          <el-col :span="1.5">
            <el-button type="primary" size="small">新增</el-button>
          </el-col>
        </el-row>

        <!-- 表格容器 (自适应剩余高度) -->
        <div class="table-container">
          <el-table :data="userList" height="100%" v-loading="loading" border>
            <el-table-column prop="userName" label="用户名" />
            <!-- 其他列 -->
          </el-table>
        </div>

        <!-- 分页 (固定高度) -->
        <div class="pagination-area">
          <el-pagination :total="total" layout="total, sizes, prev, pager, next" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style lang="scss" scoped>
.adaptive-layout {
  /* 适配顶部导航栏 + TagsView 的高度 (约 86px) */
  height: calc(100vh - 86px);
  padding: 20px;
  overflow: hidden; /* 核心：禁止页面滚动 */
  box-sizing: border-box;

  .full-height-row {
    height: 100%;
    display: flex;
  }
}

/* 左侧树样式 */
.left-aside {
  display: flex;
  flex-direction: column;
  height: 100%;
  border-right: 1px solid var(--pnkx-border);
  
  .aside-header {
    flex-shrink: 0;
    margin-bottom: 15px;
  }
  
  .aside-content {
    flex: 1;
    overflow-y: auto; /* 核心：树太长时内部滚动 */
  }
}

/* 右侧主区域样式 */
.right-main {
  height: 100%;
  display: flex;
  flex-direction: column;

  .search-area, .action-area {
    flex-shrink: 0; /* 禁止被压缩 */
  }

  .table-container {
    flex: 1; /* 自动填充剩余空间 */
    overflow: hidden; /* 核心：交给 el-table 内部滚动 */
    position: relative;
    border: 1px solid var(--pnkx-border);
    border-radius: 4px;
  }

  .pagination-area {
    flex-shrink: 0;
    padding-top: 15px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
```

### 2. 关键点总结 (避坑指南)

1.  **`height="100%"` 是灵魂**：`el-table` 如果不设置 `height` 属性，它会根据内容自动撑开，导致父级容器出现滚动条。设置了 `height`（或 `max-height`），它才会计算内部 `tbody` 的高度并启用滚动。
2.  **`overflow: hidden` 必须配套**：包裹表格的父元素 (`.table-container`) 必须设置 `overflow: hidden`，否则 Flex 布局的自适应高度计算可能会失效。
3.  **动态高度适配**：
    *   如果项目中 `Navbar` 或 `TagsView` 的显示是动态的，建议使用 CSS 变量或在布局组件中计算高度，然后通过 `provide/inject` 传给页面。
    *   最简单的方法是使用 `height: 100%` 层层透传，直到 `app-main` 容器。
4.  **Flex 布局的 `flex-shrink: 0`**：对于搜索框和分页栏，一定要设置 `flex-shrink: 0`，防止在屏幕高度较小时，这些固定内容的区域被压缩得看不见。

### 3. 推荐做法
在 `src/assets/styles/index.scss` 中定义一个通用的辅助类：
```scss
.full-height-container {
  height: calc(100vh - 86px); /* 根据实际情况调整 */
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 15px;
  
  .flex-main {
    flex: 1;
    overflow: hidden;
  }
}
```
这样在任何页面只需给外层 div 加上这个类，然后给表格包裹一个 `.flex-main` 即可快速实现。
