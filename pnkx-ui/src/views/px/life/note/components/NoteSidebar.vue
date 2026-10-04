<!--
 * @Author: PHY
 * @Description: 笔记页左侧边栏（搜索/面包屑/文件列表 + 拖拽）—— 从 index.vue 拆出
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
        <input
          v-model="searchModel"
          placeholder="搜索笔记..."
          @input="$emit('search')"
          class="search-input"
        />
      </div>
    </div>

    <!-- 面包屑导航 -->
    <div class="breadcrumb-nav">
      <div
        v-for="(item, index) in params"
        :key="index"
        class="breadcrumb-item"
      >
        <svg-icon v-if="index > 0" icon-class="右箭头" class="separator" />
        <span
          class="breadcrumb-text"
          :class="{ active: index === params.length - 1 }"
          @click="$emit('breadcrumb', index)"
        >
          {{ item.breadcrumb }}
        </span>
      </div>
    </div>

    <!-- 文件列表 -->
    <div
      class="file-list"
      v-loading="listLoading"
      @contextmenu.prevent.stop="$emit('contextmenu', $event, 'empty')"
    >
      <div v-if="list.length < 1" class="empty-state">
        <svg-icon icon-class="文件夹" class="empty-icon" />
        <p>暂无内容</p>
        <p class="hint">右键创建新笔记或文件夹</p>
      </div>

      <transition-group name="file-list" tag="div" v-else class="file-items">
        <div
          v-for="(one, index) in list"
          :key="one.id"
          class="file-card"
          :class="{ active: one.id === activeId }"
          :style="{ animationDelay: `${index * 0.05}s` }"
          @contextmenu.prevent.stop="$emit('contextmenu', $event, one)"
        >
          <!-- 文件夹 -->
          <div
            v-if="one.type === 'folder'"
            class="folder-item"
            :draggable="true"
            @dragstart="$emit('drag', $event, one)"
            @dragover.prevent
            @drop="$emit('drop', $event, one)"
            @click="$emit('open', one, false)"
            @dblclick="$emit('open', one, true)"
          >
            <div class="folder-icon">
              <svg-icon icon-class="文件夹" />
            </div>
            <div class="folder-info">
              <div class="folder-name">{{ one.name }}</div>
              <svg-icon v-if="one.password" icon-class="验证码" class="lock-icon" />
            </div>
          </div>

          <!-- 笔记 -->
          <div
            v-if="one.type === 'note'"
            class="note-item"
            :draggable="true"
            @dragstart="$emit('drag', $event, one)"
            @click="$emit('open', one)"
          >
            <div class="note-header">
              <svg-icon icon-class="编辑02" class="note-icon" />
              <span class="note-title">{{ one.title }}</span>
            </div>
            <div class="note-preview">
              {{ one.content && one.content.replace(regex, '') }}
            </div>
            <div class="note-meta">
              <svg-icon icon-class="时间" class="time-icon" />
              <span>{{ one.updateTime || one.createTime }}</span>
            </div>
          </div>
        </div>
      </transition-group>
    </div>
  </aside>
</template>

<script>
export default {
  name: 'NoteSidebar',
  emits: ['back', 'search', 'breadcrumb', 'open', 'contextmenu', 'drag', 'drop', 'update:modelValue'],
  props: {
    // 搜索关键字（v-model）
    modelValue: { type: String, default: '' },
    // 面包屑参数栈
    params: { type: Array, default: () => [] },
    // 文件/文件夹列表
    list: { type: Array, default: () => [] },
    listLoading: { type: Boolean, default: false },
    // 当前选中项 id
    activeId: { default: undefined },
    // 去除 html 标签的预览正则
    regex: { type: RegExp, default: undefined }
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
    display: flex;
    align-items: center;

    .search-icon {
      position: absolute;
      left: 14px;
      font-size: var(--text-base);
      color: var(--text-tertiary);
      pointer-events: none;
    }

    .search-input {
      width: 100%;
      height: 40px;
      padding: 0 var(--space-4) 0 42px;
      border: none;
      border-radius: var(--radius-md);
      background: var(--bg-hover);
      font-size: var(--text-sm);
      color: var(--text-primary);
      box-shadow: var(--shadow-sm);
      transition: all var(--duration-normal) var(--ease-default);

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

// 面包屑导航
.breadcrumb-nav {
  padding: var(--space-4) var(--space-5);
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
  border-bottom: 1px solid var(--border-primary);
  min-height: 52px;

  .breadcrumb-item {
    display: flex;
    align-items: center;
    gap: var(--space-2);

    .separator {
      font-size: var(--text-xs);
      color: var(--text-tertiary);
    }

    .breadcrumb-text {
      font-size: var(--text-sm);
      color: var(--text-secondary);
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 6px;
      transition: all var(--duration-normal) var(--ease-default);

      &:hover {
        background: var(--bg-hover);
        color: var(--color-primary);
      }

      &.active {
        color: var(--color-primary);
        font-weight: var(--font-semibold);
      }
    }
  }
}

// 文件列表
.file-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-4);

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

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: var(--text-tertiary);

    .empty-icon {
      font-size: 64px;
      opacity: 0.3;
      margin-bottom: var(--space-4);
    }

    p {
      margin: var(--space-1) 0;
    }

    .hint {
      font-size: var(--text-xs);
      opacity: 0.7;
    }
  }

  .file-items {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }
}

// 文件卡片
.file-card {
  animation: fadeSlideIn 0.4s var(--ease-default) forwards;
  opacity: 0;

  &.active {
    .folder-item, .note-item {
      background: var(--bg-hover);
      border-left: 3px solid var(--color-primary);
    }
  }
}

@keyframes fadeSlideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 文件夹项
.folder-item {
  display: flex;
  align-items: center;
  padding: var(--space-4);
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

  .folder-icon {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #ffd89b 0%, #f2994a 100%);
    border-radius: var(--radius-sm);
    margin-right: var(--space-3);

    .svg-icon {
      font-size: var(--text-lg);
      color: white;
    }
  }

  .folder-info {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;

    .folder-name {
      font-size: var(--text-sm);
      font-weight: var(--font-semibold);
      color: var(--text-primary);
    }

    .lock-icon {
      font-size: var(--text-sm);
      color: var(--text-tertiary);
    }
  }
}

// 笔记项
.note-item {
  padding: var(--space-4);
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

  .note-header {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    margin-bottom: var(--space-2);

    .note-icon {
      font-size: var(--text-base);
      color: var(--color-primary);
    }

    .note-title {
      font-size: var(--text-sm);
      font-weight: var(--font-semibold);
      color: var(--text-primary);
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .note-preview {
    font-size: var(--text-sm);
    color: var(--text-secondary);
    line-height: 1.5;
    margin-bottom: var(--space-2);
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
  }

  .note-meta {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: var(--text-xs);
    color: var(--text-tertiary);

    .time-icon {
      font-size: var(--text-xs);
    }
  }
}

// 文件列表动画
.file-list-enter-active,
.file-list-leave-active {
  transition: all var(--duration-normal) var(--ease-default);
}

.file-list-enter,
.file-list-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}
</style>
