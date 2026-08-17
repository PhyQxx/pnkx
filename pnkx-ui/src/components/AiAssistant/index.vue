<template>
  <div class="ai-assistant" :class="{ 'is-external': externalTrigger }">
    <!-- 悬浮按钮 -->
    <transition name="ai-fab">
      <div v-if="!externalTrigger && !isOpen" class="ai-fab" @click="toggleOpen">
        <el-icon :size="28"><ChatDotRound /></el-icon>
      </div>
    </transition>

    <!-- 聊天面板 -->
    <transition name="ai-panel">
      <div v-if="isOpen" class="ai-panel">
        <!-- 头部 -->
        <div class="ai-panel-header">
          <div class="header-left">
            <div class="avatar-icon">
              <el-icon :size="18"><Monitor /></el-icon>
            </div>
            <div class="header-info">
              <span class="header-title">AI 助手</span>
              <span class="header-status">{{ isThinking ? '思考中...' : '在线' }}</span>
            </div>
          </div>
          <div class="header-actions">
            <el-tooltip content="新对话" placement="top">
              <el-icon class="action-btn" @click="newConversation"><Plus /></el-icon>
            </el-tooltip>
            <el-tooltip content="关闭" placement="top">
              <el-icon class="action-btn" @click="toggleOpen"><Close /></el-icon>
            </el-tooltip>
          </div>
        </div>

        <!-- 消息区域 -->
        <div class="ai-panel-body" ref="bodyRef">
          <!-- 欢迎消息 -->
          <template v-if="messages.length === 0">
            <Welcome
              title="你好，我是 AI 助手"
              description="我可以帮你分析记账、管理生活、回答问题等。"
            >
              <template #extra>
                <Prompts :items="promptItems" wrap @item-click="handleQuickPrompt" />
              </template>
            </Welcome>
          </template>

          <!-- 消息列表 -->
          <template v-else>
            <BubbleList :list="bubbleList">
              <template #content="{ item }">
                <XMarkDown :content="item.content" />
              </template>
            </BubbleList>

            <div v-if="showPendingActions" class="pending-actions">
              <el-button type="primary" size="small" :loading="isLoading" @click="handlePendingAction('confirm')">确认保存</el-button>
              <el-button size="small" :disabled="isLoading" @click="handlePendingAction('cancel')">取消</el-button>
            </div>

            <!-- 思考中 -->
            <Thinking v-if="isThinking && !isStreaming" content="思考中..." />
          </template>
        </div>

        <!-- 输入区域 -->
        <div class="ai-panel-footer">
          <XSender
            ref="senderRef"
            placeholder="输入消息..."
            :loading="isLoading"
            @submit="handleSend"
          />
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { ChatDotRound, Monitor, Close, Plus, Wallet, EditPen, Calendar, List } from '@element-plus/icons-vue'
import { BubbleList, Welcome, Prompts, Thinking, XSender } from 'vue-element-plus-x'
import { cancelPendingAction, chatStream, confirmPendingAction } from '@/api/px/ai/chat'

defineProps({
  externalTrigger: {
    type: Boolean,
    default: false
  }
})

const isOpen = ref(false)
const isLoading = ref(false)
const isThinking = ref(false)
const isStreaming = ref(false)
const messages = ref([])
const bodyRef = ref(null)
const senderRef = ref(null)
let messageId = 0

const bubbleList = computed(() => {
  return messages.value.map(msg => ({
    id: msg.id,
    role: msg.role,
    content: filterInstructionText(msg.content),
    placement: msg.role === 'user' ? 'end' : 'start',
    loading: msg.role === 'assistant' && isStreaming.value && msg === messages.value[messages.value.length - 1]
  }))
})

const showPendingActions = computed(() => {
  const last = messages.value[messages.value.length - 1]
  return !!last && last.role === 'assistant' && last.pendingAction && !isThinking.value
})

const promptItems = [
  { key: 'bookkeeping', label: '午餐花了30块', icon: Wallet },
  { key: 'todo', label: '提醒我明天开会', icon: List },
  { key: 'diary', label: '今天心情不错', icon: EditPen },
  { key: 'analysis', label: '分析本月消费', icon: Calendar },
]

function filterInstructionText(content) {
  if (typeof content !== 'string') return content
  return content
    .replace(/\n?回复“确认”保存，回复“取消”放弃。/g, '')
    .replace(/\[?\s*PENDING_CONFIRM\s*\]?/gi, '')
    .trim()
}

function toggleOpen() {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    nextTick(() => scrollToBottom())
  }
}

function newConversation() {
  messages.value = []
}

function handleQuickPrompt(item) {
  senderRef.value?.setText(item.label)
  handleSend()
}

function addMessage(role, content) {
  messages.value.push({ id: ++messageId, role, content, pendingAction: false })
}

function updateLastAssistant(content) {
  const last = messages.value[messages.value.length - 1]
  if (last && last.role === 'assistant') {
    last.content = content
    last.pendingAction = isPendingDraft(content)
  }
}

function clearPendingActions() {
  messages.value.forEach(msg => {
    msg.pendingAction = false
  })
}

function isPendingDraft(content) {
  if (typeof content !== 'string') return false
  const text = content.toUpperCase()
  if (content.includes('草稿已过期') || content.includes('草稿已失效') || content.includes('没有待确认的草稿')) return false
  return text.includes('回复“确认”保存') || text.includes('PENDING_CONFIRM')
}

async function handlePendingAction(action) {
  if (isLoading.value) return
  clearPendingActions()
  isLoading.value = true
  isThinking.value = true
  isStreaming.value = false

  try {
    const response = action === 'confirm'
      ? await confirmPendingAction()
      : await cancelPendingAction()

    isThinking.value = false
    isStreaming.value = true
    addMessage('assistant', '')
    await scrollToBottom()
    await readStreamResponse(response)
  } catch (e) {
    isThinking.value = false
    if (messages.value.length > 0 && messages.value[messages.value.length - 1].role === 'assistant' && !messages.value[messages.value.length - 1].content) {
      messages.value.pop()
    }
    addMessage('assistant', '网络请求失败，请稍后重试。')
  } finally {
    isLoading.value = false
    isStreaming.value = false
    await scrollToBottom()
  }
}

async function handleSend() {
  const modelValue = senderRef.value?.getModelValue()
  const text = modelValue?.text?.trim() || ''
  if (!text || isLoading.value) return

  senderRef.value?.clear()
  isLoading.value = true
  isThinking.value = true
  isStreaming.value = false

  addMessage('user', text)
  await scrollToBottom()

  try {
    // 构建对话历史（最近10条消息）
    const history = messages.value.slice(-10).map(m => ({
      role: m.role,
      content: m.content
    }))
    const response = await chatStream(text, history)
    const reader = response.body.getReader()
    const decoder = new TextDecoder()

    isThinking.value = false
    isStreaming.value = true
    addMessage('assistant', '')
    await scrollToBottom()

    let fullContent = ''
    let eventLines = []

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const chunk = decoder.decode(value, { stream: true })
      const lines = chunk.split('\n')

      for (const line of lines) {
        if (line.startsWith('data:')) {
          eventLines.push(line.substring(5))
        } else if (line === '' && eventLines.length > 0) {
          // 空行表示一个SSE事件结束，合并多行data
          const data = eventLines.join('\n')
          eventLines = []
          if (data === '[DONE]') continue
          fullContent += data
          updateLastAssistant(fullContent)
          await scrollToBottom()
        }
      }
    }

    // 处理剩余未结束的事件
    if (eventLines.length > 0) {
      const data = eventLines.join('\n').trim()
      if (data && data !== '[DONE]') {
        fullContent += data
        updateLastAssistant(fullContent)
      }
    }

    if (!fullContent) {
      updateLastAssistant('抱歉，AI 暂时无法回复，请稍后再试。')
    }
  } catch (e) {
    isThinking.value = false
    if (messages.value.length > 0 && messages.value[messages.value.length - 1].role === 'assistant' && !messages.value[messages.value.length - 1].content) {
      messages.value.pop()
    }
    addMessage('assistant', '网络请求失败，请稍后重试。')
  } finally {
    isLoading.value = false
    isStreaming.value = false
    await scrollToBottom()
  }
}

async function readStreamResponse(response) {
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let fullContent = ''
  let eventLines = []

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    const chunk = decoder.decode(value, { stream: true })
    const lines = chunk.split('\n')

    for (const line of lines) {
      if (line.startsWith('data:')) {
        eventLines.push(line.substring(5))
      } else if (line === '' && eventLines.length > 0) {
        const data = eventLines.join('\n')
        eventLines = []
        if (data === '[DONE]') continue
        fullContent += data
        updateLastAssistant(fullContent)
        await scrollToBottom()
      }
    }
  }

  if (eventLines.length > 0) {
    const data = eventLines.join('\n').trim()
    if (data && data !== '[DONE]') {
      fullContent += data
    }
  }

  updateLastAssistant(fullContent || '抱歉，AI 暂时无法回复，请稍后再试。')
}

async function scrollToBottom() {
  await nextTick()
  if (bodyRef.value) {
    bodyRef.value.scrollTop = bodyRef.value.scrollHeight
  }
}

// ESC 关闭
function handleKeydown(e) {
  if (e.key === 'Escape' && isOpen.value) {
    isOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeydown)
})

defineExpose({
  toggleOpen
})
</script>

<style lang="scss" scoped>
.ai-assistant {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;

  &.is-external {
    top: 50%;
    right: 92px;
    bottom: auto;
    transform: translateY(-50%);
  }
}

// 悬浮按钮
.ai-fab {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, #5A8DEE, #6C63FF);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(90, 141, 238, 0.4);
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: scale(1.08);
    box-shadow: 0 6px 24px rgba(90, 141, 238, 0.5);
  }

  &:active {
    transform: scale(0.95);
  }
}

// 聊天面板
.ai-panel {
  width: 30vw;
  height: 80vh;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

// 头部
.ai-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: linear-gradient(135deg, #5A8DEE, #6C63FF);
  color: #fff;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar-icon {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-info {
  display: flex;
  flex-direction: column;
}

.header-title {
  font-size: 15px;
  font-weight: 600;
}

.header-status {
  font-size: 11px;
  opacity: 0.85;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  font-size: 18px;
  cursor: pointer;
  opacity: 0.85;
  transition: opacity 0.2s;

  &:hover {
    opacity: 1;
  }
}

// 消息区域
.ai-panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  background: #f7f8fa;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: #d0d0d0;
    border-radius: 2px;
  }

  /* PC 弹框使用更紧凑的消息密度，避免小窗口内气泡显得臃肿 */
  :deep(.elx-bubble-list__list) {
    gap: 10px;
  }

  :deep(.elx-bubble__content-wrapper .elx-bubble__content) {
    --elx-bubble-padding-y: 7px;
    --elx-bubble-padding-x: 10px;
    --elx-bubble-radius: 10px;
    min-height: 34px;
    max-width: min(86%, 500px);
  }

  :deep(.x-markdown-container) {
    font-size: 14px;
    line-height: 1.6;
  }

  :deep(.x-markdown-container .x-md-renderer) {
    padding: 0 !important;
    background-color: transparent !important;
    color: inherit !important;
  }

  :deep(.x-markdown-container .x-md-core) {
    p {
      margin: 0 0 6px;

      &:last-child {
        margin-bottom: 0;
      }
    }

    h1, h2, h3, h4, h5, h6 {
      margin-top: 12px;
      margin-bottom: 6px;
    }

    ul, ol {
      padding-left: 20px;
      margin: 4px 0 6px;

      li {
        margin-bottom: 2px;
      }
    }

    blockquote {
      margin: 6px 0;
      padding: 5px 10px;
    }
  }

  /* 修复欢迎语和提示项布局 */
  :deep(.epx-welcome) {
    padding: 4px 0;
    width: 100%;
    box-sizing: border-box;

    .epx-welcome__content {
      width: 100%;
    }

    .epx-welcome__title {
      font-size: 1.25rem;
      margin-bottom: 8px;
    }

    .epx-welcome__description {
      font-size: 0.875rem;
      color: var(--el-text-color-secondary);
      line-height: 1.5;
    }

    .epx-welcome__extra {
      width: 100%;
      margin-top: 16px;
    }
  }

  :deep(.epx-prompts) {
    display: flex !important;
    flex-wrap: wrap !important;
    gap: 8px !important;
    width: 100% !important;
    margin: 0 !important;
    padding: 0 !important;

    .epx-prompts__item {
      margin: 0 !important;
      flex: 0 0 auto;
      max-width: 100%;
      box-sizing: border-box;
      background: #fff;
      border: 1px solid var(--el-border-color-lighter);
      border-radius: 8px;
      padding: 6px 12px;
      transition: all 0.2s;

      &:hover {
        border-color: var(--el-color-primary);
        color: var(--el-color-primary);
        background: var(--el-color-primary-light-9);
      }

      .epx-prompts__item-label {
        font-size: 13px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }
  }
}

.pending-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-start;
  margin: 12px 0 14px 48px;
}

// 输入区域
.ai-panel-footer {
  padding: 12px 14px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
  flex-shrink: 0;
}

// 动画
.ai-fab-enter-active, .ai-fab-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.ai-fab-enter-from {
  opacity: 0;
  transform: scale(0.5);
}

.ai-fab-leave-to {
  opacity: 0;
  transform: scale(0.5);
}

.ai-panel-enter-active, .ai-panel-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.ai-panel-enter-from {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}

.ai-panel-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}

::v-deep .elx-welcome__header {
    flex-direction: column;
    align-items: flex-start;

}
::v-deep .elx-welcome__extra {
    flex-shrink: initial;
}
</style>
