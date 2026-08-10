<template>
  <view class="ai-chat">
    <!-- 消息区域 -->
    <scroll-view class="chat-body" scroll-y :scroll-into-view="scrollToId" :scroll-with-animation="false">
      <!-- 欢迎消息 -->
      <view v-if="messages.length === 0" class="welcome">
        <view class="welcome-avatar">
          <text class="welcome-avatar-text">AI</text>
        </view>
        <text class="welcome-title">你好，有什么可以帮你的？</text>
        <text class="welcome-subtitle">你可以直接跟我说：</text>
        <view class="quick-prompts">
          <view
            v-for="prompt in quickPrompts"
            :key="prompt.key"
            class="quick-prompt"
            @click="handleQuickPrompt(prompt.text)"
          >
            <text class="quick-prompt-icon">{{ prompt.icon }}</text>
            <text class="quick-prompt-text">{{ prompt.text }}</text>
          </view>
        </view>
      </view>

      <!-- 消息列表 -->
      <view v-for="msg in messages" :key="msg.id" class="message" :class="msg.role">
        <view v-if="msg.role === 'assistant'" class="msg-avatar">
          <text class="msg-avatar-text">AI</text>
        </view>
        <view class="msg-bubble" :class="msg.role">
          <mp-html v-if="msg.role === 'assistant'" class="msg-content" :content="formatMessage(msg.content)" :tag-style="htmlTagStyle" />
          <text v-else class="msg-content">{{ msg.content }}</text>
          <text v-if="isStreaming && msg === messages[messages.length - 1] && msg.role === 'assistant'" class="typing-cursor">▋</text>
        </view>
      </view>

      <view v-if="showPendingActions" class="pending-actions">
        <view class="pending-btn confirm" @click="handlePendingAction('confirm')">确认保存</view>
        <view class="pending-btn cancel" @click="handlePendingAction('cancel')">取消</view>
      </view>

      <!-- 思考中 -->
      <view v-if="isThinking" class="message assistant">
        <view class="msg-avatar">
          <text class="msg-avatar-text">AI</text>
        </view>
        <view class="msg-bubble assistant thinking-bubble">
          <view class="typing-indicator">
            <view class="dot"></view>
            <view class="dot"></view>
            <view class="dot"></view>
          </view>
        </view>
      </view>

      <!-- 底部占位，防止消息被输入框遮挡 -->
      <view :style="{ height: (footerHeight + 24) + 'px' }"></view>
      <!-- 滚动锚点 -->
      <view :id="scrollAnchorId" style="height: 1px;"></view>
    </scroll-view>

    <!-- 输入区域（固定在底部） -->
    <view class="chat-footer" ref="footerRef">
      <view class="input-wrapper">
        <input
          v-model="inputText"
          class="chat-input"
          placeholder="输入消息..."
          :disabled="isLoading"
          confirm-type="send"
          @confirm="handleSend"
          :adjust-position="true"
        />
        <view class="send-btn" :class="{ disabled: !inputText.trim() || isLoading }" @click="handleSend">
          <text class="send-icon">➤</text>
        </view>
      </view>
      <view class="safe-bottom"></view>
    </view>
  </view>
</template>

<script>
import { cancelPendingAction, chatStream, confirmPendingAction } from '@/api/px/ai/chat'

export default {
  data() {
    return {
      inputText: '',
      isLoading: false,
      isThinking: false,
      isStreaming: false,
      messages: [],
      scrollToId: '',
      scrollAnchorId: 'scroll-bottom',
      scrollAnchorToggle: false,
      footerHeight: 56,
      messageId: 0,
      quickPrompts: [
        { key: 'bookkeeping', text: '午餐花了30块', icon: '💰' },
        { key: 'todo', text: '提醒我明天开会', icon: '📋' },
        { key: 'diary', text: '今天心情不错', icon: '📝' },
        { key: 'analysis', text: '分析本月消费', icon: '📊' },
      ],
      htmlTagStyle: {
        h1: 'font-size:32rpx;font-weight:bold;color:#303133;margin:20rpx 0 10rpx;',
        h2: 'font-size:30rpx;font-weight:bold;color:#303133;margin:20rpx 0 10rpx;padding-bottom:8rpx;border-bottom:2rpx solid #e4e7ed;',
        h3: 'font-size:28rpx;font-weight:bold;color:#5A8DEE;margin:16rpx 0 8rpx;padding-left:12rpx;border-left:6rpx solid #5A8DEE;',
        h4: 'font-size:27rpx;font-weight:bold;color:#303133;margin:12rpx 0 6rpx;',
        p: 'margin:8rpx 0;line-height:1.7;',
        blockquote: 'border-left:6rpx solid #5A8DEE;padding:10rpx 16rpx;margin:10rpx 0;background:#f8f9ff;color:#606266;border-radius:0 8rpx 8rpx 0;',
        pre: 'background:#1e1e2e;border-radius:12rpx;padding:20rpx 24rpx;margin:16rpx 0;overflow-x:auto;',
        code: 'font-size:24rpx;font-family:Consolas,Monaco,monospace;',
        table: 'width:100%;border-collapse:collapse;font-size:24rpx;border:2rpx solid #e4e7ed;border-radius:8rpx;overflow:hidden;margin:16rpx 0;',
        th: 'background:#5A8DEE;color:#fff;padding:10rpx 16rpx;text-align:left;font-weight:bold;',
        td: 'padding:10rpx 16rpx;border-bottom:2rpx solid #f0f0f0;',
        ul: 'padding-left:28rpx;margin:6rpx 0;list-style-type:disc;',
        ol: 'padding-left:36rpx;margin:6rpx 0;',
        li: 'margin:6rpx 0;line-height:1.7;',
        hr: 'border:none;border-top:2rpx solid #e4e7ed;margin:16rpx 0;',
        strong: 'font-weight:bold;color:#303133;',
        em: 'font-style:italic;color:#606266;',
        a: 'color:#5A8DEE;text-decoration:none;'
      }
    }
  },
  computed: {
    showPendingActions() {
      const last = this.messages[this.messages.length - 1]
      return !!last && last.role === 'assistant' && last.pendingAction && !this.isThinking
    }
  },
  mounted() {
    this.$nextTick(() => {
      uni.createSelectorQuery().in(this)
        .select('.chat-footer')
        .boundingClientRect(rect => {
          if (rect) this.footerHeight = rect.height
        }).exec()
    })
  },
  methods: {
    /**
     * Markdown → HTML（逐行处理，兼容SSE流前导空格）
     */
    formatMessage(content) {
      if (!content) return ''
      // 0. 过滤指令性文本
      let html = content.replace(/\n?回复“确认”保存，回复“取消”放弃。/g, '')
                        .replace(/\[PENDING_CONFIRM\]/gi, '')
                        .trim()
      const codeBlocks = []
      const inlineCodes = []

      // 1. 提取代码块 → 占位符
      html = html.replace(/```(\w*)\n?([\s\S]*?)```/g, (_, lang, code) => {
        codeBlocks.push('<pre><code>' + this.escapeHtml(code.trim()) + '</code></pre>')
        return '%%CB' + (codeBlocks.length - 1) + '%%'
      })

      // 2. 提取行内代码 → 占位符
      html = html.replace(/`([^`\n]+)`/g, (_, code) => {
        inlineCodes.push('<code style="background:#f0f0f5;color:#e83e8c;padding:2rpx 10rpx;border-radius:6rpx;">' + this.escapeHtml(code) + '</code>')
        return '%%IC' + (inlineCodes.length - 1) + '%%'
      })

      // 3. 转义HTML
      html = html.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

      // 4. 先处理内联格式（加粗、斜体）
      html = html.replace(/\*\*([^*]+?)\*\*/g, '<strong>$1</strong>')
      html = html.replace(/\*([^*\n]+?)\*/g, '<em>$1</em>')

      // 5. 逐行处理块级元素
      const lines = html.split(/\r?\n/)
      const result = []
      let tableRows = []
      let tableHasHeader = false
      let ulItems = []
      let olItems = []

      for (const line of lines) {
        const trimmed = line.trim()

        // 收集中的表格
        if (tableRows.length > 0) {
          if (trimmed.startsWith('|')) {
            if (/^\|[\s\-:|]+\|$/.test(trimmed)) continue
            const cells = trimmed.split('|').slice(1, -1)
            const tag = tableHasHeader ? 'td' : 'th'
            tableHasHeader = true
            tableRows.push('<tr>' + cells.map(c => '<' + tag + '>' + c.trim() + '</' + tag + '>').join('') + '</tr>')
            continue
          }
          result.push('<table>' + tableRows.join('') + '</table>')
          tableRows = []
          tableHasHeader = false
        }

        // 收集中的无序列表
        if (ulItems.length > 0 && !/^[*\-] /.test(trimmed)) {
          result.push('<ul>' + ulItems.join('') + '</ul>')
          ulItems = []
        }

        // 收集中的有序列表
        if (olItems.length > 0 && !/^\d+\. /.test(trimmed)) {
          result.push('<ol>' + olItems.join('') + '</ol>')
          olItems = []
        }

        // 标题
        if (trimmed.startsWith('#### ')) {
          result.push('<h4>' + trimmed.substring(5) + '</h4>')
        } else if (trimmed.startsWith('### ')) {
          result.push('<h3>' + trimmed.substring(4) + '</h3>')
        } else if (trimmed.startsWith('## ')) {
          result.push('<h2>' + trimmed.substring(3) + '</h2>')
        } else if (trimmed.startsWith('# ')) {
          result.push('<h1>' + trimmed.substring(2) + '</h1>')
        }
        // 分割线
        else if (/^---+$/.test(trimmed)) {
          result.push('<hr/>')
        }
        // 引用
        else if (trimmed.startsWith('&gt; ')) {
          result.push('<blockquote><p>' + trimmed.substring(5) + '</p></blockquote>')
        }
        // 表格行
        else if (trimmed.startsWith('|')) {
          if (/^\|[\s\-:|]+\|$/.test(trimmed)) continue
          const cells = trimmed.split('|').slice(1, -1)
          tableRows.push('<tr>' + cells.map(c => '<th>' + c.trim() + '</th>').join('') + '</tr>')
          tableHasHeader = true
        }
        // 无序列表
        else if (/^[*\-] /.test(trimmed)) {
          ulItems.push('<li>' + trimmed.substring(2) + '</li>')
        }
        // 有序列表
        else if (/^\d+\. /.test(trimmed)) {
          olItems.push('<li>' + trimmed.replace(/^\d+\. /, '') + '</li>')
        }
        // 空行
        else if (!trimmed) {
          result.push('')
        }
        // 普通文本
        else {
          result.push(trimmed)
        }
      }

      // 收尾未闭合的块
      if (tableRows.length > 0) result.push('<table>' + tableRows.join('') + '</table>')
      if (ulItems.length > 0) result.push('<ul>' + ulItems.join('') + '</ul>')
      if (olItems.length > 0) result.push('<ol>' + olItems.join('') + '</ol>')

      html = result.join('<br/>')

      // 还原占位符
      inlineCodes.forEach((code, i) => {
        html = html.replace('%%IC' + i + '%%', code)
      })
      codeBlocks.forEach((block, i) => {
        html = html.replace('%%CB' + i + '%%', block)
      })

      return html
    },

    escapeHtml(str) {
      return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    },

    newConversation() {
      this.messages = []
    },

    handleQuickPrompt(text) {
      this.inputText = text
      this.handleSend()
    },

    addMessage(role, content) {
      this.messages.push({ id: ++this.messageId, role, content, pendingAction: false })
    },

    updateLastAssistant(content) {
      const last = this.messages[this.messages.length - 1]
      if (last && last.role === 'assistant') {
        last.content = content
        last.pendingAction = this.isPendingDraft(content)
      }
    },

    clearPendingActions() {
      this.messages.forEach(msg => {
        msg.pendingAction = false
      })
    },

    isPendingDraft(content) {
      if (typeof content !== 'string') return false
      const text = content.toUpperCase()
      if (content.includes('草稿已过期') || content.includes('草稿已失效') || content.includes('没有待确认的草稿')) return false
      return text.includes('回复“确认”保存') || text.includes('PENDING_CONFIRM')
    },

    async handleSend() {
      const text = this.inputText.trim()
      if (!text || this.isLoading) return

      this.inputText = ''
      await this.sendText(text)
    },

    async handlePendingAction(action) {
      if (this.isLoading) return
      this.clearPendingActions()
      this.isLoading = true
      this.isThinking = true
      this.isStreaming = false

      try {
        const stream = action === 'confirm'
          ? confirmPendingAction()
          : cancelPendingAction()
        this.isThinking = false
        this.isStreaming = true
        this.addMessage('assistant', '')
        await this.readStreamResponse(stream)
      } catch (e) {
        this.isThinking = false
        this.addMessage('assistant', '网络请求失败，请稍后重试。')
      } finally {
        this.isLoading = false
        this.isStreaming = false
        await this.scrollToBottom()
      }
    },

    async sendText(text) {
      this.isLoading = true
      this.isThinking = true
      this.isStreaming = false

      this.addMessage('user', text)
      await this.scrollToBottom()

      try {
        const history = this.messages.map(m => ({
          role: m.role,
          content: m.content
        }))
        const stream = chatStream(text, history)
        this.isThinking = false
        this.isStreaming = true
        this.addMessage('assistant', '')
        await this.scrollToBottom()

        await this.readStreamResponse(stream)
      } catch (e) {
        this.isThinking = false
        if (this.messages.length > 0 &&
          this.messages[this.messages.length - 1].role === 'assistant' &&
          !this.messages[this.messages.length - 1].content) {
          this.messages.pop()
        }
        this.addMessage('assistant', '网络请求失败，请稍后重试。')
      } finally {
        this.isLoading = false
        this.isStreaming = false
        await this.scrollToBottom()
      }
    },

    async readStreamResponse({ onChunk, completionPromise }) {
      let fullContent = ''
      let eventLines = []
      let lastRenderTime = 0
      let pendingRender = false
      const RENDER_INTERVAL = 80

      const renderUpdate = () => {
        if (pendingRender) {
          this.updateLastAssistant(fullContent)
          this.scrollToBottom()
          pendingRender = false
        }
      }

      onChunk((chunk) => {
        const lines = chunk.split('\n')
        for (const line of lines) {
          if (line.startsWith('data:')) {
            eventLines.push(line.substring(5))
          } else if (line === '' && eventLines.length > 0) {
            const data = eventLines.join('\n')
            eventLines = []
            if (data === '[DONE]') continue
            fullContent += data
            pendingRender = true
            const now = Date.now()
            if (now - lastRenderTime >= RENDER_INTERVAL) {
              lastRenderTime = now
              renderUpdate()
            }
          }
        }
      })

      try {
        await completionPromise
      } catch (e) {
        // 请求失败，使用已收到的内容
      }

      if (eventLines.length > 0) {
        const data = eventLines.join('\n').trim()
        if (data && data !== '[DONE]') {
          fullContent += data
        }
      }

      renderUpdate()
      this.updateLastAssistant(fullContent || '抱歉，AI 暂时无法回复，请稍后再试。')
    },

    async scrollToBottom() {
      await this.$nextTick()
      this.scrollAnchorToggle = !this.scrollAnchorToggle
      this.scrollAnchorId = this.scrollAnchorToggle ? 'scroll-a' : 'scroll-b'
      this.scrollToId = ''
      await this.$nextTick()
      this.scrollToId = this.scrollAnchorId
    }
  },

  onNavigationBarButtonTap() {
    this.newConversation()
  }
}
</script>

<style lang="scss" scoped>
uni-page-body {
  height: 100%;
}

.ai-chat {
  height: 100%;
  position: relative;
  display: flex;
  flex-direction: column;
}

/* 消息区域 */
.chat-body {
  flex: 1;
  padding-top: 10px;
  overflow-y: auto;
}

/* 欢迎区域 */
.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 40rpx;
}

.welcome-avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #5A8DEE, #6C63FF);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;
}

.welcome-avatar-text {
  color: #fff;
  font-size: 36rpx;
  font-weight: bold;
}

.welcome-title {
  font-size: 34rpx;
  color: #303133;
  font-weight: 600;
  margin-bottom: 12rpx;
}

.welcome-subtitle {
  font-size: 26rpx;
  color: #909399;
  margin-bottom: 32rpx;
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  justify-content: center;
}

.quick-prompt {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 16rpx 24rpx;
  background: #fff;
  border: 2rpx solid #e4e7ed;
  border-radius: 40rpx;

  &:active {
    border-color: #5A8DEE;
    background: #f0f5ff;
  }
}

.quick-prompt-icon {
  font-size: 28rpx;
}

.quick-prompt-text {
  font-size: 26rpx;
  color: #606266;
}

/* 消息 */
.message {
  display: flex;
  gap: 16rpx;
  margin-bottom: 28rpx;
  padding: 0 24rpx;

  &.user {
    justify-content: flex-end;
  }

  &.assistant {
    justify-content: flex-start;
  }
}

.msg-avatar {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #5A8DEE, #6C63FF);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 4rpx;
}

.msg-avatar-text {
  color: #fff;
  font-size: 22rpx;
  font-weight: bold;
}

.msg-bubble {
  max-width: 80%;
  padding: 20rpx 28rpx;
  border-radius: 24rpx;
  font-size: 28rpx;
  line-height: 1.7;
  word-break: break-word;

  &.user {
    background: linear-gradient(135deg, #5A8DEE, #6C63FF);
    color: #fff;
    border-bottom-right-radius: 8rpx;
  }

  &.assistant {
    background: #fff;
    color: #303133;
    border-bottom-left-radius: 8rpx;
    box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
  }
}

.msg-content {
  font-size: 28rpx;
  line-height: 1.7;
}

/* 打字光标 */
.pending-actions {
  display: flex;
  gap: 16rpx;
  margin: -12rpx 0 28rpx 100rpx;
}

.pending-btn {
  height: 64rpx;
  line-height: 64rpx;
  padding: 0 28rpx;
  border-radius: 12rpx;
  font-size: 26rpx;
  border: 2rpx solid #dcdfe6;
  background: #fff;
  color: #606266;

  &.confirm {
    background: #5A8DEE;
    border-color: #5A8DEE;
    color: #fff;
  }

  &:active {
    opacity: 0.82;
  }
}

.typing-cursor {
  display: inline-block;
  color: #667eea;
  font-size: 28rpx;
  animation: blink 0.8s ease-in-out infinite;
  margin-left: 4rpx;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* 思考动画 */
.thinking-bubble {
  padding: 28rpx 36rpx;
}

.typing-indicator {
  display: flex;
  gap: 10rpx;
  align-items: center;

  .dot {
    width: 14rpx;
    height: 14rpx;
    border-radius: 50%;
    background: #c0c4cc;
    animation: typing-bounce 1.4s infinite ease-in-out;

    &:nth-child(1) { animation-delay: 0s; }
    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes typing-bounce {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-12rpx);
    opacity: 1;
  }
}

/* 输入区域（固定底部） */
.chat-footer {
  background: #fff;
  border-top: 2rpx solid #f0f0f0;
}

.input-wrapper {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 24rpx;
}

.chat-input {
  flex: 1;
  height: 72rpx;
  background: #f5f6f8;
  border-radius: 40rpx;
  padding: 0 28rpx;
  font-size: 28rpx;
}

.send-btn {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #5A8DEE, #6C63FF);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.2s;

  &.disabled {
    opacity: 0.4;
  }

  &:active {
    transform: scale(0.95);
  }
}

.send-icon {
  color: #fff;
  font-size: 32rpx;
}

.safe-bottom {
  height: env(safe-area-inset-bottom);
}
</style>
