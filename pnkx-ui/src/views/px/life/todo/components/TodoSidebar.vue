<!--
 * @Author: PHY
 * @Description: 待办页左侧边栏（搜索/导航过滤/标签过滤）—— 从 index.vue 拆出
-->
<template>
  <aside class="sidebar">
    <!-- 搜索栏 -->
    <div class="search-wrapper">
      <div class="back-btn" @click="$emit('back')">
        <svg-icon icon-class="back" />
      </div>
      <div class="search-box">
        <svg-icon icon-class="搜索" class="search-icon" />
        <el-select
          v-model="searchModel"
          class="search-select"
          :remote-method="(q) => $emit('search', q)"
          clearable
          :loading="searchLoading"
          filterable
          default-first-option
          remote
          placeholder="搜索待办..."
          @change="(v) => $emit('change', v)"
        >
          <el-option
            v-for="item in options"
            :key="item.id"
            :label="item.content"
            :value="item.id"
          />
        </el-select>
      </div>
    </div>

    <!-- 导航过滤 -->
    <div class="nav-list">
      <div
        v-for="nav in navList"
        :key="nav.key"
        class="nav-card"
        :class="{ active: activeNav === nav.key }"
        @click="$emit('select-nav', nav.key)"
      >
        <div class="nav-icon-wrapper" :style="{ background: nav.gradient }">
          <svg-icon :icon-class="nav.icon" class="nav-icon" />
        </div>
        <div class="nav-info">
          <span class="nav-label">{{ nav.label }}</span>
          <span class="nav-count">{{ nav.count }}</span>
        </div>
      </div>
    </div>

    <!-- 标签过滤 -->
    <div class="tag-section">
      <div class="section-title">标签</div>
      <div class="tag-cloud">
        <span
          v-for="(tag, index) in labelOptions"
          :key="tag"
          class="tag-item"
          :class="[
            tagTypes[index % tagTypes.length],
            { active: activeTag === tag }
          ]"
          @click="$emit('select-tag', tag)"
        >
          {{ tag }}
        </span>
      </div>
    </div>
  </aside>
</template>

<script>
export default {
  name: 'TodoSidebar',
  emits: ['back', 'search', 'change', 'select-nav', 'select-tag', 'update:modelValue'],
  props: {
    // 搜索关键字（v-model）
    modelValue: { type: [String, Number], default: '' },
    // 远程搜索结果
    options: { type: Array, default: () => [] },
    searchLoading: { type: Boolean, default: false },
    navList: { type: Array, default: () => [] },
    activeNav: { type: String, default: '1' },
    labelOptions: { type: Array, default: () => [] },
    activeTag: { type: String, default: '' },
    tagTypes: { type: Array, default: () => [] }
  },
  computed: {
    searchModel: {
      get() {
        return this.modelValue
      },
      set(value) {
        this.$emit('update:modelValue', value)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.sidebar {
  width: 320px;
  background: var(--bg-card);
  backdrop-filter: blur(20px);
  border-right: 1px solid var(--border-primary);
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-sm);
  position: relative;
  z-index: 10;
  overflow-y: auto;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--border-primary);
    border-radius: 3px;

    &:hover {
      background: var(--text-tertiary);
    }
  }
}

// 搜索栏
.search-wrapper {
  padding: var(--space-5);
  display: flex;
  gap: var(--space-3);
  align-items: center;
  border-bottom: 1px solid var(--border-primary);

  .back-btn {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: var(--radius-sm);
    background: var(--bg-card);
    cursor: pointer;
    transition: all var(--duration-normal) var(--ease-default);
    box-shadow: var(--shadow-sm);

    &:hover {
      background: var(--color-primary);
      color: white;
      transform: translateX(-2px);
    }

    .svg-icon {
      font-size: 18px;
    }
  }

  .search-box {
    flex: 1;
    position: relative;

    .search-icon {
      position: absolute;
      left: 14px;
      top: 50%;
      transform: translateY(-50%);
      font-size: var(--text-base);
      color: var(--text-tertiary);
      pointer-events: none;
      z-index: 1;
    }

    .search-select {
      width: 100%;

      ::v-deep .el-input__inner {
        height: 40px;
        padding: 0 var(--space-4) 0 42px;
        border: none;
        border-radius: var(--radius-md);
        background: var(--bg-card);
        font-size: var(--text-sm);
        color: var(--text-primary);
        box-shadow: var(--shadow-sm);

        &::placeholder {
          color: var(--text-tertiary);
        }

        &:focus {
          outline: none;
          box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.12), var(--shadow-md);
        }
      }
    }
  }
}

// 导航列表
.nav-list {
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  border-bottom: 1px solid var(--border-primary);
}

.nav-card {
  display: flex;
  align-items: center;
  padding: 14px var(--space-4);
  background: var(--bg-card);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-default);
  box-shadow: var(--shadow-sm);
  border-left: 3px solid transparent;

  &:hover {
    transform: translateX(4px);
    box-shadow: var(--shadow-md);
    background: var(--bg-hover);
  }

  &.active {
    background: var(--bg-hover);
    border-left-color: var(--color-primary);
  }

  .nav-icon-wrapper {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 10px;
    margin-right: 14px;

    .nav-icon {
      font-size: 18px;
      color: white;
    }
  }

  .nav-info {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;

    .nav-label {
      font-size: var(--text-sm);
      font-weight: var(--font-semibold);
      color: var(--text-primary);
    }

    .nav-count {
      font-size: var(--text-sm);
      color: var(--text-secondary);
      background: var(--bg-hover);
      padding: 2px 10px;
      border-radius: 12px;
      min-width: 28px;
      text-align: center;
    }
  }
}

// 标签区域
.tag-section {
  padding: var(--space-5) var(--space-4);

  .section-title {
    font-size: var(--text-xs);
    font-weight: var(--font-semibold);
    color: var(--text-secondary);
    text-transform: uppercase;
    letter-spacing: 1px;
    margin-bottom: 14px;
  }

  .tag-cloud {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
  }
}

.tag-item {
  display: inline-block;
  padding: 4px 14px;
  border-radius: 16px;
  font-size: var(--text-sm);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-default);
  border: 1px solid transparent;

  &:hover {
    transform: translateY(-1px);
  }

  &.active {
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.25);
    transform: scale(1.05);
  }

  &.success {
    background: rgba(103, 194, 58, 0.1);
    color: #67c23a;
    border-color: rgba(103, 194, 58, 0.2);
  }

  &.warning {
    background: rgba(230, 162, 60, 0.1);
    color: #e6a23c;
    border-color: rgba(230, 162, 60, 0.2);
  }

  &.danger {
    background: rgba(245, 108, 108, 0.1);
    color: #f56c6c;
    border-color: rgba(245, 108, 108, 0.2);
  }

  &.info {
    background: rgba(144, 147, 153, 0.1);
    color: #909399;
    border-color: rgba(144, 147, 153, 0.2);
  }

  // default
  background: rgba(64, 158, 255, 0.1);
  color: var(--color-primary);
  border-color: rgba(64, 158, 255, 0.2);

  &.success {
    background: rgba(103, 194, 58, 0.1);
    color: #67c23a;
    border-color: rgba(103, 194, 58, 0.2);
  }

  &.active {
    background: var(--color-primary);
    color: white;
    border-color: var(--color-primary);
  }
}
</style>
