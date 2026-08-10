# Skill: pnkx-modern-ui-refactor

> 将旧版 Element UI 表格/卡片页面重构为笔记页面风格的现代分栏布局。
> 本 skill 定义了项目统一的现代 UI 设计系统，适用于所有业务模块的 UI 改造。

---

## 触发条件

当用户说"按照笔记页面风格重构 X 页面"、"用现代 UI 重做 X"、"X 模块 UI 改造"时，加载此 skill。

---

## 设计系统

### 1. CSS 变量（每个组件根元素必须声明）

以下变量必须原样复制到组件根元素的 `<style>` 块中，**不得修改值**：

```scss
.YOUR-CONTAINER-CLASS {
  --sidebar-bg: rgba(248, 249, 250, 0.95);
  --sidebar-border: rgba(0, 0, 0, 0.06);
  --card-bg: #ffffff;
  --card-hover-bg: rgba(64, 158, 255, 0.04);
  --card-active-bg: rgba(64, 158, 255, 0.08);
  --text-primary: #303133;
  --text-secondary: #909399;
  --text-muted: #c0c4cc;
  --accent-color: #409eff;
  --accent-gradient: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  --shadow-sm: 0 2px 8px rgba(0, 0, 0, 0.04);
  --shadow-md: 0 4px 16px rgba(0, 0, 0, 0.08);
  --shadow-lg: 0 8px 32px rgba(0, 0, 0, 0.12);
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --transition-base: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
```

### 2. 容器布局

```scss
.YOUR-CONTAINER-CLASS {
  display: flex;
  height: calc(100vh - 84px);
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eef5 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}
```

### 3. 左侧边栏（固定结构）

```html
<aside class="sidebar">
  <!-- 搜索栏 -->
  <div class="search-wrapper">
    <div class="search-box">
      <svg-icon icon-class="搜索" class="search-icon" />
      <input v-model="searchCode" placeholder="搜索..." class="search-input">
    </div>
  </div>

  <!-- 列表内容（可定制） -->
  <div class="item-list" v-loading="listLoading">
    <!-- 空状态 -->
    <div v-if="list.length < 1" class="empty-state">
      <svg-icon icon-class="XXX" class="empty-icon" />
      <p>暂无内容</p>
      <p class="hint">右键或点击右下角按钮新增</p>
    </div>

    <!-- 列表项（transition-group 包裹） -->
    <transition-group v-else name="item-list" tag="div" class="item-items">
      <div v-for="(item, index) in filteredList" :key="item.id"
        class="item-card"
        :class="{ active: active && active.id === item.id }"
        :style="{ animationDelay: `${index * 0.05}s` }"
        @click="handleSelect(item)"
        @contextmenu.prevent.stop="handleContextMenu($event, item)">
        <!-- 卡片内容 -->
      </div>
    </transition-group>
  </div>
</aside>
```

边栏样式：

```scss
.sidebar {
  width: 320px;  // 或 360px，根据卡片内容量调整
  background: var(--sidebar-bg);
  backdrop-filter: blur(20px);
  border-right: 1px solid var(--sidebar-border);
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-sm);
  position: relative;
  z-index: 10;
}
```

### 4. 右侧详情区（固定结构）

```html
<main v-loading="loading" class="detail-area">
  <!-- 空状态 -->
  <div v-if="!active" class="empty-detail">
    <svg-icon icon-class="XXX" class="empty-detail-icon" />
    <p>选择一条记录查看详情</p>
  </div>

  <!-- 详情内容 -->
  <div v-else class="item-detail">
    <!-- 头部信息栏 -->
    <div class="detail-header">...</div>
    <!-- 主体内容（可定制） -->
    <div class="detail-body">...</div>
  </div>
</main>
```

### 5. 必需组件模式

以下模式在所有改造中必须出现，除非目标页面明确不需要：

#### 5.1 搜索栏（`.search-wrapper`）

```scss
.search-wrapper {
  padding: 20px;
  border-bottom: 1px solid var(--sidebar-border);

  .search-box {
    position: relative;
    display: flex;
    align-items: center;

    .search-icon {
      position: absolute;
      left: 14px;
      font-size: 16px;
      color: var(--text-muted);
      pointer-events: none;
    }

    .search-input {
      width: 100%;
      height: 40px;
      padding: 0 16px 0 42px;
      border: none;
      border-radius: var(--radius-md);
      background: var(--card-bg);
      font-size: 14px;
      color: var(--text-primary);
      box-shadow: var(--shadow-sm);
      transition: var(--transition-base);

      &::placeholder { color: var(--text-muted); }
      &:focus {
        outline: none;
        box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1), var(--shadow-md);
      }
    }
  }
}
```

#### 5.2 列表卡片（`.item-card`）

每张卡片必须包含：
- 图标区域：40×40px 渐变圆角方块
- 信息区域：标题（14px 500 weight）+ 描述文字（12px secondary）
- 右侧摘要区域：状态标签或数值

卡片样式（固定模式）：

```scss
.item-card {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  background: var(--card-bg);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: var(--transition-base);
  box-shadow: var(--shadow-sm);
  border-left: 3px solid transparent;
  animation: fadeSlideIn 0.4s ease forwards;
  opacity: 0;

  &.active {
    background: var(--card-active-bg);
    border-left-color: var(--accent-color);
  }

  &:hover {
    transform: translateX(4px);
    box-shadow: var(--shadow-md);
    background: var(--card-hover-bg);
  }
}

@keyframes fadeSlideIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
```

#### 5.3 图标方块渐变色板

不同模块使用不同的渐变色，避免视觉雷同：

| 模块类型 | 渐变色 |
|---------|--------|
| 笔记/文件夹 | `linear-gradient(135deg, #ffd89b 0%, #f2994a 100%)` |
| 纪念日 | `linear-gradient(135deg, #f093fb 0%, #f5576c 100%)` |
| 待办/任务 | `linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)` |
| 账本/记账 | `linear-gradient(135deg, #d4fc79 0%, #96e6a1 100%)` |
| 聊天/消息 | `linear-gradient(135deg, #89f7fe 0%, #66a6ff 100%)` |
| 博客/文章 | `linear-gradient(135deg, #fccb90 0%, #d57eeb 100%)` |
| 相册/图片 | `linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)` |
| 通用/默认 | `linear-gradient(135deg, #667eea 0%, #764ba2 100%)` |

图标方块样式：

```scss
.card-icon-wrapper {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: <选择对应模块的渐变色>;
  border-radius: 10px;
  margin-right: 12px;
  flex-shrink: 0;

  .card-icon {
    font-size: 20px;
    color: white;
  }
}
```

#### 5.4 浮动操作按钮（FAB）

```html
<div class="fab-action" title="新增" @click="handleAdd">
  <i class="el-icon-plus" />
</div>
```

```scss
.fab-action {
  position: fixed;
  right: 32px;
  bottom: 32px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--accent-gradient);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);
  cursor: pointer;
  transition: var(--transition-base);
  z-index: 100;

  i, .svg-icon { font-size: 24px; }

  &:hover {
    transform: scale(1.1) rotate(90deg);
    box-shadow: 0 6px 28px rgba(102, 126, 234, 0.5);
  }

  &:active { transform: scale(0.95); }
}
```

#### 5.5 右键上下文菜单

```html
<transition name="context-menu">
  <div v-if="contextMenuVisible"
    v-clickOutSide="closeContextMenu"
    class="context-menu"
    :style="contextMenuStyle">
    <div v-for="item in contextMenuItems" :key="item.id"
      class="menu-item" @click="handleContextAction(item)">
      <svg-icon :icon-class="item.icon" class="menu-icon" />
      <span>{{ item.name }}</span>
    </div>
  </div>
</transition>
```

菜单样式（固定模式，不可修改）：

```scss
.context-menu {
  position: fixed;
  background: var(--card-bg);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  padding: 8px 0;
  min-width: 180px;
  z-index: 9999;
  border: 1px solid var(--sidebar-border);

  .menu-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 16px;
    font-size: 14px;
    color: var(--text-primary);
    cursor: pointer;
    transition: var(--transition-base);

    .menu-icon {
      font-size: 16px;
      color: var(--text-secondary);
    }

    &:hover {
      background: var(--card-hover-bg);
      color: var(--accent-color);
      .menu-icon { color: var(--accent-color); }
    }
  }
}
```

菜单定位计算：

```javascript
handleContextMenu(event, item) {
  // item 为 null 时是空白区域右键，为对象时是卡片右键
  if (item) {
    this.active = item
    this.contextMenuItems = [
      { id: 1, name: '编辑', icon: '编辑' },
      { id: 2, name: '删除', icon: '删除' }
    ]
  } else {
    this.contextMenuItems = [
      { id: 3, name: '新增', icon: '编辑02' }
    ]
  }
  this.contextMenuVisible = true
  this.contextMenuStyle = `top: ${Math.min(event.y, window.innerHeight - this.contextMenuItems.length * 48)}px; left: ${Math.min(event.x - 180, window.innerWidth - 200)}px;`
}
```

#### 5.6 现代对话框（`modern-dialog`）

```html
<el-dialog
  :title="title"
  :visible.sync="dialogVisible"
  width="520px"
  custom-class="modern-dialog"
  :modal-append-to-body="true">
  <el-form ref="form" :model="form" :rules="rules"
    label-position="top" class="modern-form">
    <!-- 表单项 -->
  </el-form>
  <div slot="footer" class="dialog-footer">
    <el-button class="btn-cancel" @click="dialogVisible = false">取消</el-button>
    <el-button :loading="saveLoading" type="primary"
      class="btn-confirm" @click="handleSave">确定</el-button>
  </div>
</el-dialog>
```

对话框样式（固定模式）：

```scss
::v-deep .modern-dialog {
  border-radius: var(--radius-lg) !important;
  overflow: hidden;
  box-shadow: var(--shadow-lg) !important;

  .el-dialog__header {
    padding: 20px 24px 16px;
    border-bottom: 1px solid var(--sidebar-border);
    background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
    .el-dialog__title {
      font-size: 18px;
      font-weight: 600;
      color: var(--text-primary);
    }
  }

  .el-dialog__body { padding: 24px; }

  .el-dialog__footer {
    padding: 16px 24px 20px;
    border-top: 1px solid var(--sidebar-border);
    background: #fafbfc;
  }
}

.modern-form {
  ::v-deep .el-form-item {
    margin-bottom: 20px;
    .el-form-item__label {
      font-size: 13px;
      font-weight: 500;
      color: var(--text-secondary);
      padding-bottom: 8px;
    }
    .el-input__inner {
      border-radius: var(--radius-sm);
      border-color: var(--sidebar-border);
      transition: var(--transition-base);
      &:focus {
        border-color: var(--accent-color);
        box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
      }
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;

  .btn-cancel {
    border-radius: var(--radius-sm);
    padding: 10px 20px;
    transition: var(--transition-base);
    &:hover { background: var(--card-hover-bg); }
  }

  .btn-confirm {
    border-radius: var(--radius-sm);
    padding: 10px 24px;
    background: var(--accent-gradient);
    border: none;
    transition: var(--transition-base);
    &:hover {
      opacity: 0.9;
      transform: translateY(-1px);
    }
  }
}
```

### 6. 动画（固定模式）

每个改造页面必须包含以下动画：

```scss
// 入场动画（卡片列表）
@keyframes fadeSlideIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

// 右键菜单动画
.context-menu-enter-active,
.context-menu-leave-active { transition: all 0.2s ease; }
.context-menu-enter,
.context-menu-leave-to {
  opacity: 0;
  transform: scale(0.95) translateY(-8px);
}

// 列表增删动画
.item-list-enter-active,
.item-list-leave-active { transition: all 0.3s ease; }
.item-list-enter,
.item-list-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}
```

### 7. 全局美化（固定模式）

每个改造页面末尾必须包含：

```scss
// Loading 美化
::v-deep .el-loading-mask {
  background-color: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
}

// 通知美化
::v-deep .el-notification {
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  border: none;
}
```

### 8. 滚动条美化（列表区域必须）

```scss
.item-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;

  &::-webkit-scrollbar { width: 6px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb {
    background: rgba(0, 0, 0, 0.1);
    border-radius: 3px;
    &:hover { background: rgba(0, 0, 0, 0.2); }
  }
}
```

### 9. 空状态样式（固定模式）

```scss
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-muted);

  .empty-icon { font-size: 64px; opacity: 0.3; margin-bottom: 16px; }
  p { margin: 4px 0; }
  .hint { font-size: 12px; opacity: 0.7; }
}

.empty-detail {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);

  .empty-detail-icon { font-size: 80px; opacity: 0.2; margin-bottom: 20px; }
  p { font-size: 16px; }
}
```

---

## 重构检查清单

改造任何页面时，按以下清单逐项确认：

### 结构层
- [ ] 左右分栏布局（sidebar + detail-area）
- [ ] 搜索栏（`.search-wrapper` + `.search-box`）
- [ ] 列表用 `transition-group` 包裹
- [ ] 空状态（列表空 + 详情空，两处）
- [ ] 右键菜单（`v-clickOutSide` + `transition`）
- [ ] FAB 浮动按钮
- [ ] 现代对话框（`custom-class="modern-dialog"`）

### 样式层
- [ ] 根元素声明全部 CSS 变量
- [ ] 卡片有 `fadeSlideIn` 动画 + `animationDelay` 错开
- [ ] 卡片 hover: `translateX(4px)` + `box-shadow: var(--shadow-md)`
- [ ] 卡片 active: `border-left: 3px solid var(--accent-color)`
- [ ] 图标方块使用模块对应渐变色
- [ ] 滚动条美化
- [ ] Loading 美化
- [ ] 通知美化

### 功能层
- [ ] 搜索过滤（computed `filteredList`）
- [ ] 点击选中（`handleSelect`）
- [ ] 右键菜单操作（编辑/删除/新增）
- [ ] 保留原 API 接口，不改请求结构
- [ ] 保留原表单校验规则
- [ ] 保留原路由参数处理（`$route.params`）

### 代码规范
- [ ] 2 空格缩进，单引号，无分号
- [ ] `<style lang="scss" scoped>`
- [ ] Vue 2 Options API
- [ ] ESLint 零 error

---

## 参考实现

| 页面 | 路径 | 说明 |
|------|------|------|
| 笔记 | `src/views/px/life/note/index.vue` | 原始设计源头，含面包屑/文件夹层级 |
| 纪念日 | `src/views/px/life/commemorationDay/index.vue` | 首个改造落地，含实时倒计时瓷贴 |

改造时优先参考纪念日页面，它是笔记模式的简化版本，更适合作为模板复制。
