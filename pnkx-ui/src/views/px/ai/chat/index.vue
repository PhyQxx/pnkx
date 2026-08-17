<template>
  <div class="ai-chat-container">
    <div v-if="showSidebar" class="chat-sidebar">
      <div class="sidebar-header">
        <el-button type="primary" style="width: 100%" @click="newConversation">
          <el-icon><Plus /></el-icon>
          新建对话
        </el-button>
      </div>
      <div class="conversation-list">
        <div
          v-for="(conv, index) in conversations"
          :key="conv.id"
          class="conversation-item"
          :class="{ active: currentConvIndex === index }"
          @click="switchConversation(index)"
        >
          <el-icon class="conv-icon"><ChatDotRound /></el-icon>
          <span class="conv-title">{{ conv.title }}</span>
          <el-icon class="conv-delete" @click.stop="deleteConversation(index)"><Close /></el-icon>
        </div>
      </div>
    </div>

    <div class="chat-main">
      <div class="chat-header">
        <el-button text @click="showSidebar = !showSidebar">
          <el-icon><Operation /></el-icon>
        </el-button>
        <span class="header-title">AI 助手</span>
        <el-select v-model="selectedModel" placeholder="选择模型" size="small" style="width: 180px; margin-left: auto">
          <el-option
            v-for="model in models"
            :key="model.id"
            :label="model.modelName"
            :value="model.id"
          />
        </el-select>
      </div>

      <div ref="chatBodyRef" class="chat-body">
        <template v-if="currentMessages.length === 0">
          <Welcome
            title="你好，我是 AI 助手"
            description="基于 AgentScope 驱动，我可以帮你分析记账、管理生活、回答问题等。"
          >
            <template #extra>
              <Prompts :items="promptSuggestions" wrap @item-click="handlePromptSelect" />
            </template>
          </Welcome>
        </template>
        <template v-else>
          <BubbleList :list="bubbleList" :max-height="'calc(100vh - 220px)'">
            <template #content="{ item }">
              <XMarkDown :content="item.content" />
            </template>
          </BubbleList>
          <div v-if="showPendingActions" class="pending-actions">
            <el-button type="primary" :loading="isLoading" @click="handlePendingAction('confirm')">确认保存</el-button>
            <el-button :disabled="isLoading" @click="handlePendingAction('cancel')">取消</el-button>
          </div>
          <Thinking v-if="isThinking" content="正在思考中..." />
        </template>
      </div>

      <div class="chat-footer">
        <div class="footer-toolbar">
          <el-tooltip
            content="开启后 AI 会先深度思考再回答，质量更高但更慢；关闭则直接快速回答"
            placement="top"
          >
            <el-switch
              v-model="thinkingEnabled"
              inline-prompt
              active-text="深度思考"
              inactive-text="快速回答"
              size="small"
            />
          </el-tooltip>
        </div>
        <div class="sender-wrapper">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="3"
            placeholder="输入消息，按 Enter 发送..."
            resize="none"
            @keydown.enter.prevent="handleSend"
          />
          <div class="sender-actions">
            <el-button type="primary" :loading="isLoading" @click="handleSend">
              发送
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { Plus, ChatDotRound, Close, Operation } from '@element-plus/icons-vue'
import { BubbleList, Welcome, Prompts, Thinking } from 'vue-element-plus-x'
import { cancelPendingAction, chatStream, confirmPendingAction, listModels } from '@/api/px/ai/chat'

const inputText = ref('')
const isLoading = ref(false)
const isThinking = ref(false)
const thinkingEnabled = ref(false)
const showSidebar = ref(true)
const chatBodyRef = ref(null)
const models = ref([])
const selectedModel = ref(null)

const conversations = ref([
  { id: Date.now(), title: '新对话', messages: [] }
])
const currentConvIndex = ref(0)

const promptSuggestions = [
  { key: 'bookkeeping', label: '午餐花了30块' },
  { key: 'todo', label: '提醒我明天开会' },
  { key: 'diary', label: '今天心情不错' },
  { key: 'analysis', label: '分析本月消费' }
]

const currentMessages = computed(() => {
  return conversations.value[currentConvIndex.value]?.messages || []
})

const showPendingActions = computed(() => {
  const last = currentMessages.value[currentMessages.value.length - 1]
  return !!last && last.role === 'assistant' && last.pendingAction && !isThinking.value
})

const bubbleList = computed(() => {
  return currentMessages.value.map(msg => ({
    key: msg.id,
    role: msg.role,
    content: filterInstructionText(msg.content)
  }))
})

function filterInstructionText(content) {
  if (typeof content !== 'string') return content
  return content
    .replace(/\n?回复“确认”保存，回复“取消”放弃。/g, '')
    .replace(/\[?\s*PENDING_CONFIRM\s*\]?/gi, '')
    .trim()
}

onMounted(async () => {
  try {
    const res = await listModels()
    models.value = res.data || []
    const defaultModel = models.value.find(m => m.isDefault === '1')
    if (defaultModel) {
      selectedModel.value = defaultModel.id
    }
  } catch (e) {
    console.error('获取模型列表失败', e)
  }
})

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || isLoading.value) return

  inputText.value = ''
  await sendText(text)
}

async function handlePendingAction(action) {
  if (isLoading.value) return
  clearPendingActions()
  isLoading.value = true
  isThinking.value = true

  try {
    const res = action === 'confirm'
      ? await confirmPendingAction()
      : await cancelPendingAction()
    isThinking.value = false
    addMessage('assistant', '')
    await readStreamResponse(res)
  } catch (e) {
    isThinking.value = false
    addMessage('assistant', '网络请求失败，请稍后重试。')
  } finally {
    isLoading.value = false
    await scrollToBottom()
  }
}

async function sendText(text) {
  isLoading.value = true
  isThinking.value = true

  addMessage('user', text)

  const conv = conversations.value[currentConvIndex.value]
  if (conv.messages.filter(m => m.role === 'user').length === 1) {
    conv.title = text.slice(0, 20) + (text.length > 20 ? '...' : '')
  }

  await scrollToBottom()

  try {
    const history = currentMessages.value.map(msg => ({
      role: msg.role,
      content: msg.content
    }))
    const res = await chatStream(text, history, selectedModel.value, thinkingEnabled.value)
    isThinking.value = false
    addMessage('assistant', '')
    await readStreamResponse(res)
  } catch (e) {
    isThinking.value = false
    addMessage('assistant', '网络请求失败，请稍后重试。')
  } finally {
    isLoading.value = false
    await scrollToBottom()
  }
}

function addMessage(role, content) {
  const message = {
    id: Date.now() + Math.random(),
    role,
    content,
    pendingAction: false
  }
  conversations.value[currentConvIndex.value].messages.push(message)
  return message
}

function updateLastAssistant(content) {
  const messages = currentMessages.value
  const last = messages[messages.length - 1]
  if (last && last.role === 'assistant') {
    last.content = content
    last.pendingAction = isPendingDraft(content)
  }
}

function clearPendingActions() {
  currentMessages.value.forEach(msg => {
    msg.pendingAction = false
  })
}

function isPendingDraft(content) {
  if (typeof content !== 'string') return false
  const text = content.toUpperCase()
  if (content.includes('草稿已过期') || content.includes('草稿已失效') || content.includes('没有待确认的草稿')) return false
  return text.includes('回复“确认”保存') || text.includes('PENDING_CONFIRM')
}

async function readStreamResponse(response) {
  if (!response.ok || !response.body) {
    updateLastAssistant('抱歉，请求出错了。')
    return
  }

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
  if (chatBodyRef.value) {
    chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
  }
}

function handlePromptSelect(item) {
  inputText.value = item.label
  handleSend()
}

function newConversation() {
  conversations.value.push({
    id: Date.now(),
    title: '新对话',
    messages: []
  })
  currentConvIndex.value = conversations.value.length - 1
}

function switchConversation(index) {
  currentConvIndex.value = index
}

function deleteConversation(index) {
  if (conversations.value.length <= 1) return
  conversations.value.splice(index, 1)
  if (currentConvIndex.value >= conversations.value.length) {
    currentConvIndex.value = conversations.value.length - 1
  }
}
</script>

<style lang="scss" scoped>
.ai-chat-container {
  display: flex;
  height: calc(100vh - 84px);
  background: var(--el-bg-color);
  border-radius: 8px;
  overflow: hidden;
}

.chat-sidebar {
  width: 260px;
  background: var(--el-fill-color-light);
  border-right: 1px solid var(--el-border-color-lighter);
  display: flex;
  flex-direction: column;

  .sidebar-header {
    padding: 16px;
  }

  .conversation-list {
    flex: 1;
    overflow-y: auto;
    padding: 0 8px;
  }

  .conversation-item {
    display: flex;
    align-items: center;
    padding: 10px 12px;
    border-radius: 8px;
    cursor: pointer;
    margin-bottom: 4px;
    transition: background 0.2s;

    &:hover {
      background: var(--el-fill-color);
    }

    &.active {
      background: var(--el-color-primary-light-9);
      color: var(--el-color-primary);
    }

    .conv-icon {
      margin-right: 8px;
      font-size: 16px;
    }

    .conv-title {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 14px;
    }

    .conv-delete {
      opacity: 0;
      font-size: 14px;
      color: var(--el-text-color-secondary);
      transition: opacity 0.2s;
    }

    &:hover .conv-delete {
      opacity: 1;
    }
  }
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);

  .header-title {
    font-size: 16px;
    font-weight: 600;
    margin-left: 8px;
  }
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;

  /* 修复欢迎语和提示项布局 */
  :deep(.epx-welcome) {
    padding: 10px 0;
    width: 100%;
    box-sizing: border-box;

    .epx-welcome__content {
      width: 100%;
    }

    .epx-welcome__title {
      font-size: 1.75rem;
      margin-bottom: 12px;
    }

    .epx-welcome__description {
      font-size: 1rem;
      color: var(--el-text-color-secondary);
      line-height: 1.6;
      margin-bottom: 24px;
    }

    .epx-welcome__extra {
      width: 100%;
    }
  }

  :deep(.epx-prompts) {
    display: flex !important;
    flex-wrap: wrap !important;
    gap: 12px !important;
    width: 100% !important;
    margin: 0 !important;
    padding: 0 !important;

    .epx-prompts__item {
      margin: 0 !important;
      flex: 0 0 auto;
      max-width: 100%;
      box-sizing: border-box;
      background: var(--el-fill-color-blank);
      border: 1px solid var(--el-border-color-lighter);
      border-radius: 12px;
      padding: 10px 20px;
      transition: all 0.2s;
      cursor: pointer;

      &:hover {
        border-color: var(--el-color-primary);
        color: var(--el-color-primary);
        background: var(--el-color-primary-light-9);
        transform: translateY(-2px);
        box-shadow: var(--el-shadow-lighter);
      }
      
      .epx-prompts__item-label {
        font-size: 14px;
        font-weight: 500;
      }
    }
  }
}

.pending-actions {
  display: flex;
  gap: 10px;
  margin: 12px 0 0 48px;
}

.chat-footer {
  padding: 16px 20px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.footer-toolbar {
  display: flex;
  align-items: center;
  padding: 0 4px 8px;
}

.sender-wrapper {
  position: relative;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  background: var(--el-bg-color-overlay);
  padding: 8px;
  transition: border-color 0.2s;

  &:focus-within {
    border-color: var(--el-color-primary);
  }

  :deep(.el-textarea__inner) {
    box-shadow: none;
    background: transparent;
    padding: 0 8px;
    font-size: 14px;

    &:focus {
      box-shadow: none;
    }
  }

  .sender-actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 8px;
    padding-right: 8px;
  }
}
</style>
