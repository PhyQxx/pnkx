<!--
 * @Author: PHY
 * @Description: 本月账单分析对话框（SSE 流式 AI 分析 + 打字机效果）—— 从 views/index.vue 拆出
 * 用法：v-model:visible 打开即自动发起分析
-->
<template>
    <el-dialog
      title="本月账单分析"
      v-model="visibleModel"
      width="80vw"
      top="5vh"
      custom-class="modern-dialog"
      :modal-append-to-body="true"
    >
      <div class="analysis-content">
        <div v-if="dialogVisibleLoading && !aiAnalysis" class="analysis-loading">
          <i class="el-icon-loading" />
          <p class="loading-hint">AI 正在分析您的账单数据...</p>
        </div>
        <div v-if="aiAnalysis" ref="analysisResult" class="analysis-result-wrap">
          <XMarkDown :content="aiAnalysis" />
          <span v-if="isStreaming" class="typing-cursor">▋</span>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="visibleModel = false">关闭</el-button>
          <el-button type="primary" class="btn-confirm" @click="visibleModel = false">确定</el-button>
        </div>
      </template>
    </el-dialog>
</template>

<script>
import { getAIAnalysisStream } from '@/api/px/life/bookkeeping/record'

export default {
  name: 'BillAnalysisDialog',
  emits: ['update:visible'],
  props: {
    // 对话框可见性（打开即发起分析）
    visible: { type: Boolean, default: false }
  },
  data() {
    return {
      // 分析加载标志
      dialogVisibleLoading: false,
      // AI分析结果
      aiAnalysis: '',
      // 打字机缓冲区
      typewriterBuffer: '',
      // 是否正在打字
      isTyping: false,
      // 流式进行中
      isStreaming: false,
      // 强制刷新 key
      refreshKey: 0
    }
  },
  computed: {
    visibleModel: {
      get() {
        return this.visible
      },
      set(value) {
        this.$emit('update:visible', value)
      }
    }
  },
  watch: {
    visible(value) {
      if (value) {
        this.startAnalysis()
      }
    }
  },
  methods: {
    /**
     * 账单分析（流式）
     */
    startAnalysis() {
      this.dialogVisibleLoading = true
      this.aiAnalysis = ''
      this.isStreaming = true
      this.typewriterBuffer = ''
      this.isTyping = false

      getAIAnalysisStream().then(response => {
        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        const read = () => {
          reader.read().then(({ done, value }) => {
            if (done) {
              if (buffer.trim()) {
                let content = buffer.replace(/data:/g, '').trim()
                if (content && content !== '[DONE]') {
                  this.typewriterBuffer += content
                  this.startTypewriter()
                }
              }
              this.dialogVisibleLoading = false
              this.isStreaming = false
              return
            }

            const chunk = decoder.decode(value, { stream: true })
            buffer += chunk

            // 按双换行分割消息（SSE 协议）
            const parts = buffer.split('\n\n')
            buffer = parts.pop() || ''

            for (const part of parts) {
              let content = part.split('\n')
                .map(line => line.startsWith('data:') ? line.substring(5) : line)
                .join('\n')
                .replace(/\n$/, '')
              if (content && content !== '[DONE]') {
                this.typewriterBuffer += content
              }
            }

            if (this.typewriterBuffer && !this.isTyping) {
              this.startTypewriter()
            }

            read()
          })
        }
        read()
      }).catch(() => {
        this.dialogVisibleLoading = false
        this.isStreaming = false
      })
    },
    /**
     * 启动打字机效果
     */
    startTypewriter() {
      if (this.isTyping || !this.typewriterBuffer) return
      this.isTyping = true
      this.isStreaming = true

      const BATCH_SIZE = 8
      const MIN_DELAY = 8
      let lastTime = 0
      let batchCount = 0

      const typeNext = (currentTime) => {
        if (!this.typewriterBuffer) {
          this.isTyping = false
          return
        }

        if (currentTime - lastTime < MIN_DELAY) {
          requestAnimationFrame(typeNext)
          return
        }
        lastTime = currentTime

        let batch = this.typewriterBuffer.substring(0, BATCH_SIZE)
        this.typewriterBuffer = this.typewriterBuffer.substring(BATCH_SIZE)
        this.aiAnalysis += batch
        batchCount++

        // 每8批强制刷新一次，解决 vue-markdown 响应式问题
        if (batchCount % 8 === 0) {
          this.refreshKey++
        }

        this.$nextTick(() => {
          const el = this.$refs.analysisResult
          if (el) el.scrollTop = el.scrollHeight
        })

        requestAnimationFrame(typeNext)
      }

      requestAnimationFrame(typeNext)
    }
  }
}
</script>

<style lang="scss" scoped>
.analysis-content {
  min-height: 120px;

  .analysis-loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px 0;
    i { font-size: 32px; color: var(--pnkx-primary); }
    .loading-hint { margin-top: 16px; font-size: 14px; color: var(--text-secondary); }
  }

  .analysis-result-wrap {
    max-height: 60vh;
    overflow-y: auto;
    padding-right: 8px;
    transition: opacity 0.3s ease;

    &::-webkit-scrollbar { width: 4px; }
    &::-webkit-scrollbar-track { background: transparent; }
    &::-webkit-scrollbar-thumb {
      background: rgba(0, 0, 0, 0.15);
      border-radius: 2px;
      &:hover { background: rgba(0, 0, 0, 0.25); }
    }

    .analysis-result {
      font-size: 14px;
      line-height: 1.8;
      color: var(--text-primary);

      ::v-deep h1 {
        font-size: 20px;
        font-weight: 600;
        color: var(--pnkx-primary);
        margin: 20px 0 12px;
        padding-bottom: 8px;
        border-bottom: 2px solid var(--pnkx-primary-soft);
      }
      ::v-deep h2 {
        font-size: 17px;
        font-weight: 600;
        color: var(--text-primary);
        margin: 16px 0 8px;
        padding-left: 8px;
        border-left: 3px solid var(--pnkx-primary);
      }
      ::v-deep h3 { font-size: 15px; font-weight: 600; margin: 12px 0 6px; }
      ::v-deep table {
        border-collapse: collapse;
        width: 100%;
        margin: 12px 0;
        font-size: 13px;
        border-radius: 8px;
        overflow: hidden;
        th, td { border:  1px solid rgba(0,0,0,0.08); padding: 10px 14px; }
        th { background: var(--pnkx-primary-soft); color: var(--pnkx-primary); font-weight: 600; }
        tr:nth-child(even) { background: rgba(0,0,0,0.02); }
        tr:hover { background: var(--pnkx-primary-soft); }
      }
      ::v-deep p { margin: 10px 0; }
      ::v-deep ul, ::v-deep ol { padding-left: 24px; margin:  10px 0; }
      ::v-deep li { margin: 6px 0; line-height: 1.6; }
      ::v-deep code {
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
        padding: 2px 8px;
        border-radius: 4px;
        font-family: 'Monaco', 'Menlo', monospace;
        font-size: 13px;
      }
      ::v-deep pre {
        background: #f8f9fa;
        border-radius: 8px;
        padding: 12px;
        overflow-x: auto;
        code { background: none; padding: 0; color: var(--text-primary); }
      }
      ::v-deep strong { color: var(--pnkx-primary); font-weight: 600; }
      ::v-deep em { color: var(--text-secondary); font-style: italic; }
      ::v-deep blockquote {
        border-left: 4px solid;
        border-color: var(--pnkx-primary);
        padding: 12px 16px;
        margin: 16px 0;
        background: var(--pnkx-primary-soft);
        border-radius: 0 8px 8px 0;
        color: var(--text-secondary);
      }
      ::v-deep hr {
        border: none;
        height: 2px;
        background: var(--pnkx-primary);
        margin: 20px 0;
        border-radius: 1px;
      }
    }
  }

  .typing-cursor {
    display: inline-block;
    color: var(--pnkx-primary);
    font-size: 16px;
    animation: blink 0.8s ease-in-out infinite;
    margin-left: 2px;
    vertical-align: middle;
  }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

// 现代对话框
::v-deep .modern-dialog {
  border-radius: var(--radius-lg) !important;
  overflow: hidden;
  box-shadow: var(--shadow-lg) !important;

  .el-dialog__header {
    padding: 20px 24px 16px;
    border-bottom: 1px solid var(--sidebar-border);
    background: var(--pnkx-surface);
    .el-dialog__title { font-size: 18px; font-weight: 600; color: var(--text-primary); }
  }

  .el-dialog__body { padding: 24px; }

  .el-dialog__footer {
    padding: 16px 24px 20px;
    border-top: 1px solid var(--sidebar-border);
    background: var(--pnkx-surface-muted);
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
    background: var(--pnkx-primary);
    border: none;
    transition: var(--transition-base);
    &:hover { opacity: 0.9; transform: translateY(-1px); }
  }
}
</style>
