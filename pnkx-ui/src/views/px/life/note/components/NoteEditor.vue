<!-- eslint-disable vue/no-mutating-props -->
<!--
 * @Author: PHY
 * @Description: 笔记编辑预览区（标题/排序/富文本/保存）—— 从 index.vue 拆出
 * 说明：note 对象按引用与父组件共享，字段就地编辑并触发父组件自动保存
-->
<template>
  <main class="preview-area" v-loading="loading">
    <div class="preview-content">
      <!-- 文件夹空状态 -->
      <div v-if="activeType === 'folder'" class="empty-preview">
        <svg-icon icon-class="文件夹" class="preview-empty-icon" />
        <p>选择一个笔记开始编辑</p>
      </div>

      <!-- 笔记编辑区 -->
      <template v-else-if="activeType === 'note' && editor">
        <div class="note-header-bar">
          <input
            v-model="note.title"
            placeholder="输入笔记标题..."
            class="title-input"
          />
          <input
            v-model="note.order"
            placeholder="排序"
            class="order-input"
            type="number"
          />
        </div>
        <div class="editor-wrapper">
          <editor
            :key="note.id"
            ref="editor"
            height="100%"
            v-model="note.richText"
          />
        </div>

        <!-- 浮动保存按钮 -->
        <div class="fab-save" @click="$emit('save')" title="保存 (自动保存已启用)">
          <svg-icon icon-class="保存" />
        </div>
      </template>
    </div>
  </main>
</template>

<script>
import Editor from '@/components/Editor'

export default {
  name: 'NoteEditor',
  components: { Editor },
  emits: ['save'],
  props: {
    // 当前选中项类型：folder 显示空状态，note 显示编辑器
    activeType: { type: String, default: '' },
    // 是否挂载编辑器（父组件控制重载场景）
    editor: { type: Boolean, default: true },
    // 笔记对象（按引用共享，就地编辑）
    note: { type: Object, required: true },
    loading: { type: Boolean, default: false }
  }
}
</script>

<style lang="scss" scoped>
.preview-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.preview-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.empty-preview {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);

  .preview-empty-icon {
    font-size: 80px;
    opacity: 0.2;
    margin-bottom: var(--space-5);
  }

  p {
    font-size: var(--text-base);
  }
}

.note-header-bar {
  display: flex;
  gap: var(--space-4);
  padding: 24px 32px var(--space-4);
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-primary);

  .title-input {
    flex: 1;
    font-size: var(--text-2xl);
    font-weight: var(--font-semibold);
    color: var(--text-primary);
    border: none;
    background: transparent;
    padding: var(--space-2) 0;
    border-bottom: 2px solid transparent;
    transition: all var(--duration-normal) var(--ease-default);

    &::placeholder {
      color: var(--text-tertiary);
    }

    &:focus {
      outline: none;
      border-bottom-color: var(--color-primary);
    }
  }

  .order-input {
    width: 80px;
    font-size: var(--text-sm);
    color: var(--text-secondary);
    border: none;
    background: var(--bg-hover);
    padding: var(--space-2) var(--space-3);
    border-radius: var(--radius-sm);
    text-align: center;

    &:focus {
      outline: none;
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
    }
  }
}

.editor-wrapper {
  flex: 1;
  padding: var(--space-4) 32px 32px;
  background: var(--bg-card);
  overflow: hidden;
}

// 浮动保存按钮
.fab-save {
  position: fixed;
  right: 32px;
  bottom: 32px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--color-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-lg);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-default);
  z-index: 100;

  .svg-icon {
    font-size: var(--text-xl);
  }

  &:hover {
    transform: scale(1.1) rotate(5deg);
    box-shadow: var(--shadow-lg);
  }

  &:active {
    transform: scale(0.95);
  }
}
</style>
