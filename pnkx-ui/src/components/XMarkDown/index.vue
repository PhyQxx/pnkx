<template>
  <div class="x-markdown-container">
    <MarkdownRenderer :markdown="processedContent" :enable-breaks="true" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { MarkdownRenderer } from 'x-markdown-vue'
import 'x-markdown-vue/style'

const props = defineProps({
  content: {
    type: String,
    default: ''
  }
})

// 对内容进行预处理，确保 Markdown 标签识别正确
const processedContent = computed(() => {
  if (!props.content) return ''

  // 1. 处理可能出现的转义换行符
  let text = props.content.replace(/\\n/g, '\n')

  // 2. 确保 Markdown 块级元素前有换行（针对 AI 输出有时会挤在一起的情况）
  // 例如：把 "文本### 标题" 修复为 "文本\n### 标题"
  text = text.replace(/([^\n])(#{1,6}\s+)/g, '$1\n$2')
  text = text.replace(/([^\n])(\n*[-*+]\s+)/g, '$1\n$2')
  text = text.replace(/([^\n])(\n*\d+\.\s+)/g, '$1\n$2')

  return text
})
</script>

<style lang="scss" scoped>
.x-markdown-container {
  font-size: 15px; /* 基础字号 */
  line-height: 1.8; /* 行高优化 */
  color: var(--el-text-color-primary);
  white-space: normal;
  word-break: break-word;

  :deep(.x-markdown) {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;

    /* 核心样式覆盖 */
    p {
      margin: 8px 0 12px;
      &:last-child { margin-bottom: 0; }
    }

    /* 优化标题样式和间距 */
    h1, h2, h3, h4, h5, h6 {
      margin-top: 20px;
      margin-bottom: 12px;
      font-weight: 600;
      line-height: 1.4;
    }

    /* 列表间距 */
    ul, ol {
      padding-left: 24px;
      margin-bottom: 12px;
      li {
        margin-bottom: 6px;
      }
    }

    /* 表格样式 */
    table {
      width: 100%;
      border-collapse: collapse;
      margin-bottom: 16px;
      th, td {
        border: 1px solid var(--el-border-color-lighter);
        padding: 8px 12px;
      }
      th {
        background: var(--el-fill-color-light);
      }
    }

    /* 引用块样式 */
    blockquote {
      margin: 12px 0;
      padding: 8px 16px;
      border-left: 4px solid var(--el-border-color);
      color: var(--el-text-color-secondary);
      background: var(--el-fill-color-blank);
    }
  }
}
</style>
