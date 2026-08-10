<script setup lang="ts">
import { NButton, NInput, NSpin, useMessage } from 'naive-ui'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  pending?: boolean
}

const { getToken, tokenPrefix } = useToken()
const { $markdownIt } = useNuxtApp()
const message = useMessage()
const color = useColorMode()

const isLoggedIn = computed(() => !!getToken())

// 浮窗开合状态
const open = ref(false)
// 输入框内容
const inputText = ref('')
// 消息列表
const messages = ref<ChatMessage[]>([])
// 是否正在等待响应
const loading = ref(false)
// 滚动容器引用
const scrollBody = ref<HTMLElement | null>(null)

const baseURL = (import.meta.env.VITE_APP_BASE_URL as string) || 'https://admin.pnkx.top:8/prod-api'

// 主题色：跟随 app.vue 配置
const primaryColor = computed(() => (color.value === 'dark' ? '#6366f1' : '#f97316'))

/**
 * 渲染 Markdown 为 HTML
 */
function renderMarkdown(text: string): string {
  if (!text) return ''
  try {
    return $markdownIt(text)
  } catch {
    return text
  }
}

/**
 * 滚动到底部
 */
function scrollToBottom() {
  nextTick(() => {
    if (scrollBody.value) {
      scrollBody.value.scrollTop = scrollBody.value.scrollHeight
    }
  })
}

/**
 * 切换浮窗
 */
function toggleOpen() {
  open.value = !open.value
  // 首次打开若没有消息，加一条欢迎语
  if (open.value && messages.value.length === 0) {
    messages.value.push({
      role: 'assistant',
      content: isLoggedIn.value
        ? '你好呀～我是 AI 助手，可以帮你记账、记日记、加待办、查账单分析等，有什么可以帮你的吗？'
        : '你好呀～游客模式可以进行普通问答；登录后还可以记账、写日记和创建待办。有什么想聊的吗？'
    })
  }
  scrollToBottom()
}

/**
 * 发送消息（SSE 流式）
 */
async function handleSend() {
  const question = inputText.value.trim()
  if (!question || loading.value) return

  // 追加用户消息
  messages.value.push({ role: 'user', content: question })
  // 追加占位的助手消息（流式填充）
  const assistantMsg = reactive<ChatMessage>({ role: 'assistant', content: '', pending: true })
  messages.value.push(assistantMsg)
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  // 构造历史消息（最多取最近 6 条，避免超长）
  const history = messages.value
    .filter(m => !m.pending && m.content)
    .slice(-7, -1)
    .map(m => ({ role: m.role, content: m.content }))

  try {
    const endpoint = isLoggedIn.value ? '/ai/chat/stream' : '/client/ai/chat/stream'
    const headers: Record<string, string> = { 'Content-Type': 'application/json' }
    if (isLoggedIn.value) headers.Authorization = tokenPrefix + getToken()
    const res = await fetch(`${baseURL}${endpoint}`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ question, messages: history })
    })

    if (!res.ok || !res.body) {
      throw new Error(`请求失败（${res.status}）`)
    }

    const reader = res.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let done = false

    assistantMsg.pending = false

    while (!done) {
      const { done: readerDone, value } = await reader.read()
      if (readerDone) break
      buffer += decoder.decode(value, { stream: true })

      // SSE 事件以空行分隔
      const events = buffer.split('\n\n')
      buffer = events.pop() || ''

      for (const evt of events) {
        // 一个事件可能含多行 data:，按 SSE 规范用 \n 连接
        const dataLines: string[] = []
        for (const line of evt.split('\n')) {
          if (line.startsWith('data:')) {
            dataLines.push(line.slice(5))
          }
        }
        if (dataLines.length === 0) continue
        const text = dataLines.join('\n')
        if (text === '[DONE]') {
          done = true
          break
        }
        assistantMsg.content += text
        scrollToBottom()
      }
    }
  } catch (e: any) {
    assistantMsg.pending = false
    assistantMsg.content = '抱歉，出错了：' + (e?.message || '网络异常，请稍后重试')
    if (String(e?.message || '').includes('401')) {
      message.warning('登录已过期，请重新登录')
    }
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

/**
 * 清空对话
 */
function handleClear() {
  messages.value = []
}

/**
 * 输入框回车发送（Shift+Enter 换行）
 */
function handleEnter(e: KeyboardEvent) {
  if (e.shiftKey) return
  e.preventDefault()
  handleSend()
}
</script>

<template>
  <!-- 悬浮触发按钮 -->
  <div
    class="fixed bottom-6 right-6 z-[100] flex h-14 w-14 cursor-pointer items-center justify-center rounded-full shadow-lg transition-transform duration-300 hover:scale-110 max-md:bottom-4 max-md:right-4"
    :style="{ background: primaryColor }"
    @click="toggleOpen"
  >
    <Icon
      :name="open ? 'ri:close-line' : 'ri:robot-2-fill'"
      size="26"
      color="#fff"
    />
  </div>

  <!-- 聊天面板 -->
  <Transition
    enter-active-class="transition-all duration-300 ease-out"
    leave-active-class="transition-all duration-200 ease-in"
    enter-from-class="opacity-0 translate-y-4 scale-95"
    leave-to-class="opacity-0 translate-y-4 scale-95"
  >
    <div
      v-if="open"
      class="fixed bottom-24 right-6 z-[100] flex max-h-[70vh] w-[380px] max-w-[calc(100vw-2rem)] flex-col overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-2xl dark:border-gray-700 dark:bg-gray-800 max-md:bottom-20 max-md:right-4"
    >
      <!-- 头部 -->
      <div
        class="flex items-center justify-between px-4 py-3 text-white"
        :style="{ background: primaryColor }"
      >
        <div class="flex items-center gap-2">
          <Icon name="ri:robot-2-fill" size="20" />
          <span class="text-sm font-semibold">AI 助手</span>
        </div>
        <div class="flex items-center gap-1">
          <button
            class="rounded p-1 transition hover:bg-white/20"
            title="清空对话"
            @click="handleClear"
          >
            <Icon name="ri:delete-bin-6-line" size="16" color="#fff" />
          </button>
          <button
            class="rounded p-1 transition hover:bg-white/20"
            title="关闭"
            @click="open = false"
          >
            <Icon name="ri:close-line" size="18" color="#fff" />
          </button>
        </div>
      </div>

      <!-- 消息区 -->
      <div
        ref="scrollBody"
        class="flex-1 space-y-3 overflow-y-auto px-4 py-4"
        style="min-height: 280px; max-height: calc(70vh - 130px)"
      >
        <div
          v-for="(msg, idx) in messages"
          :key="idx"
          class="flex"
          :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
        >
          <div
            class="max-w-[85%] whitespace-pre-wrap break-words rounded-2xl px-3 py-2 text-sm leading-relaxed"
            :class="msg.role === 'user'
              ? 'rounded-br-sm text-white'
              : 'rounded-bl-sm bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-100'"
            :style="msg.role === 'user' ? { background: primaryColor } : {}"
          >
            <template v-if="msg.role === 'user'">{{ msg.content }}</template>
            <template v-else>
              <span v-if="msg.pending" class="inline-flex items-center gap-1 text-gray-400">
                <NSpin :size="14" />
                <span class="text-xs">思考中...</span>
              </span>
              <div
                v-else
                class="markdown-body !text-sm"
                v-html="renderMarkdown(msg.content)"
              />
            </template>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="border-t border-gray-200 p-3 dark:border-gray-700">
        <div class="flex items-end gap-2">
          <NInput
            v-model:value="inputText"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 4 }"
            placeholder="输入消息，Enter 发送，Shift+Enter 换行"
            :disabled="loading"
            @keydown="handleEnter"
          />
          <NButton
            circle
            type="primary"
            :loading="loading"
            :disabled="!inputText.trim()"
            @click="handleSend"
          >
            <template #icon>
              <Icon name="ri:send-plane-fill" size="16" />
            </template>
          </NButton>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
/* 消息区滚动条美化 */
.overflow-y-auto::-webkit-scrollbar {
  width: 5px;
}
.overflow-y-auto::-webkit-scrollbar-thumb {
  background-color: rgba(150, 150, 150, 0.3);
  border-radius: 3px;
}

/* Markdown 内容样式覆盖（组件内） */
:deep(.markdown-body p) {
  margin: 0.3em 0;
}
:deep(.markdown-body p:first-child) {
  margin-top: 0;
}
:deep(.markdown-body p:last-child) {
  margin-bottom: 0;
}
:deep(.markdown-body ul),
:deep(.markdown-body ol) {
  padding-left: 1.2em;
  margin: 0.3em 0;
}
:deep(.markdown-body code) {
  background-color: rgba(150, 150, 150, 0.2);
  padding: 0.1em 0.3em;
  border-radius: 3px;
  font-size: 0.9em;
}
:deep(.markdown-body pre) {
  margin: 0.4em 0;
  border-radius: 6px;
  overflow-x: auto;
}
</style>
