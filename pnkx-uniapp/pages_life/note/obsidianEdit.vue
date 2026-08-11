<template>
  <view class="obs-edit">
    <!-- Toolbar -->
    <view class="edit-toolbar">
      <view class="edit-toolbar__info">
        <text class="edit-toolbar__name">{{ fileName }}</text>
        <text v-if="modified" class="edit-toolbar__modified">未保存</text>
      </view>
      <view class="edit-toolbar__actions">
        <view class="edit-toolbar__btn edit-toolbar__btn--save" @click="handleSave">
          <text class="edit-toolbar__btn-text">{{ saving ? '保存中…' : '保存' }}</text>
        </view>
      </view>
    </view>

    <!-- Markdown 预览/编辑切换 -->
    <view class="edit-mode-bar">
      <view
        class="edit-mode-bar__item"
        :class="{ 'edit-mode-bar__item--active': mode === 'edit' }"
        @click="mode = 'edit'"
      >
        <text>编辑</text>
      </view>
      <view
        class="edit-mode-bar__item"
        :class="{ 'edit-mode-bar__item--active': mode === 'preview' }"
        @click="mode = 'preview'"
      >
        <text>预览</text>
      </view>
    </view>

    <!-- 编辑区 -->
    <view v-if="mode === 'edit'" class="edit-area">
      <textarea
        v-model="content"
        class="edit-textarea"
        :auto-height="false"
        :maxlength="-1"
        placeholder="开始书写…"
        @input="onContentChange"
      />
    </view>

    <!-- 预览区 -->
    <scroll-view v-else scroll-y class="preview-area">
      <view v-if="isMarkdown" class="preview-content">
        <mp-html :content="renderedContent" />
      </view>
      <view v-else class="preview-content">
        <text class="preview-plain">{{ content }}</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { readFile, writeFile } from '@/api/system/fileManager'

export default {
  data() {
    return {
      filePath: '',
      content: '',
      originalContent: '',
      modified: false,
      saving: false,
      mode: 'edit'
    }
  },
  computed: {
    fileName() {
      if (!this.filePath) return ''
      return this.filePath.split('/').pop()
    },
    isMarkdown() {
      return this.fileName.toLowerCase().endsWith('.md')
    },
    renderedContent() {
      // 简易 markdown 转 HTML（标题、加粗、列表、代码块）
      let html = this.content
      // 代码块
      html = html.replace(/```[\s\S]*?```/g, m => '<pre><code>' + m.replace(/```\w*\n?/g, '').replace(/```/g, '') + '</code></pre>')
      // 标题
      html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>')
      html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>')
      html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>')
      // 加粗/斜体
      html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
      html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>')
      // 行内代码
      html = html.replace(/`([^`]+)`/g, '<code>$1</code>')
      // 列表
      html = html.replace(/^- (.+)$/gm, '<li>$1</li>')
      // 段落（换行）
      html = html.replace(/\n\n/g, '</p><p>')
      html = '<p>' + html + '</p>'
      return html
    }
  },
  onLoad(options) {
    this.filePath = decodeURIComponent(options.path || '')
    this.loadFile()
  },
  onBackPress() {
    if (this.modified) {
      uni.showModal({
        title: '提示',
        content: '有未保存的修改，是否保存？',
        confirmText: '保存',
        cancelText: '放弃',
        success: (res) => {
          if (res.confirm) {
            this.handleSave(true)
          } else {
            this.modified = false
            uni.navigateBack()
          }
        }
      })
      return true
    }
    return false
  },
  methods: {
    loadFile() {
      uni.showLoading({ title: '加载中…' })
      readFile(this.filePath).then(res => {
        const data = res.data || {}
        this.content = data.content || ''
        this.originalContent = this.content
      }).catch(() => {
        uni.showToast({ title: '读取失败', icon: 'none' })
      }).finally(() => {
        uni.hideLoading()
      })
    },
    onContentChange() {
      this.modified = this.content !== this.originalContent
    },
    async handleSave(navigateBack) {
      if (!this.modified || this.saving) return
      this.saving = true
      try {
        await writeFile({ path: this.filePath, content: this.content })
        this.originalContent = this.content
        this.modified = false
        uni.showToast({ title: '已保存', icon: 'success' })
        if (navigateBack) {
          setTimeout(() => uni.navigateBack(), 800)
        }
      } catch (e) {
        uni.showToast({ title: '保存失败', icon: 'none' })
      }
      this.saving = false
    }
  }
}
</script>

<style lang="scss" scoped>
.obs-edit {
  min-height: 100vh;
  background: $bg-page;
  display: flex;
  flex-direction: column;
}

/* Toolbar */
.edit-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: $bg-card;
  padding: $spacing-sm $page-padding;
  box-shadow: $shadow-xs;

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    font-size: $font-body;
    font-weight: $font-weight-semibold;
    color: $text-primary;
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__modified {
    font-size: $font-mini;
    color: #e6a23c;
  }

  &__btn {
    padding: $spacing-xs $spacing-lg;
    border-radius: $radius-md;

    &--save {
      background: $primary;
    }

    &:active {
      opacity: 0.8;
    }
  }

  &__btn-text {
    font-size: $font-caption;
    color: #fff;
  }
}

/* Mode bar */
.edit-mode-bar {
  display: flex;
  background: $bg-card;
  border-bottom: 2rpx solid $gray-100;

  &__item {
    flex: 1;
    text-align: center;
    padding: $spacing-sm 0;
    font-size: $font-caption;
    color: $text-tertiary;

    &--active {
      color: $primary;
      font-weight: $font-weight-semibold;
      border-bottom: 3rpx solid $primary;
    }
  }
}

/* Edit area */
.edit-area {
  flex: 1;
  padding: $spacing-md $page-padding;
}

.edit-textarea {
  width: 100%;
  height: calc(100vh - 280rpx);
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-md;
  font-size: $font-body;
  line-height: 1.8;
  box-sizing: border-box;
  font-family: -apple-system, 'SF Mono', Monaco, monospace;
}

/* Preview */
.preview-area {
  flex: 1;
  padding: $spacing-md $page-padding;
}

.preview-content {
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-md;
}

.preview-plain {
  font-size: $font-body;
  line-height: 1.8;
  color: $text-primary;
  white-space: pre-wrap;
}
</style>
