<script setup lang="ts">
import { NButton, NInput, NSpin, useMessage } from 'naive-ui'
import { createSseParser } from '@/utils/sse'
import { API_BASE_URL } from '@/utils/apiBase'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  pending?: boolean
  notice?: string
}

const { getToken, tokenPrefix } = useToken()
const { $markdownIt } = useNuxtApp()
const message = useMessage()
const color = useColorMode()

const isLoggedIn = computed(() => !!getToken())

// 浮窗开合状态
const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ (event: 'close'): void }>()
// 输入框内容
const inputText = ref('')
// 消息列表
const messages = ref<ChatMessage[]>([])
// 是否正在等待响应
const loading = ref(false)
let activeController: AbortController | undefined
function stopGeneration() { activeController?.abort() }
onBeforeUnmount(stopGeneration)
// 滚动容器引用
const scrollBody = ref<HTMLElement | null>(null)

const baseURL = API_BASE_URL

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
    return text.replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]!))
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
watch(() => props.open, (open) => {
  // 首次打开若没有消息，加一条欢迎语
  if (open && messages.value.length === 0) {
    messages.value.push({
      role: 'assistant',
      content: isLoggedIn.value
        ? '你好呀～我是 AI 助手，可以帮你记账、记日记、加待办、查账单分析等，有什么可以帮你的吗？'
        : '你好呀～游客模式可以进行普通问答；登录后还可以记账、写日记和创建待办。有什么想聊的吗？'
    })
  }
  scrollToBottom()
}, { immediate: true })

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

  const controller = new AbortController()
  activeController = controller
  let timedOut = false
  let timer: ReturnType<typeof setTimeout>
  const resetTimeout = () => {
    clearTimeout(timer)
    timer = setTimeout(() => { timedOut = true; controller.abort() }, 60000)
  }
  resetTimeout()
  let bufferedText = ''
  let flushTimer: ReturnType<typeof setTimeout> | undefined
  const flushText = () => {
    clearTimeout(flushTimer)
    flushTimer = undefined
    if (!bufferedText) return
    assistantMsg.pending = false
    assistantMsg.content += bufferedText
    bufferedText = ''
    scrollToBottom()
  }
  let reader: ReadableStreamDefaultReader<Uint8Array> | undefined
  try {
    const endpoint = isLoggedIn.value ? '/ai/chat/stream' : '/client/ai/chat/stream'
    const headers: Record<string, string> = { 'Content-Type': 'application/json' }
    if (isLoggedIn.value) headers.Authorization = tokenPrefix + getToken()
    const res = await fetch(`${baseURL}${endpoint}`, {
      method: 'POST',
      signal: controller.signal,
      headers,
      body: JSON.stringify({ question, messages: history })
    })

    if (!res.ok || !res.body) {
      throw new Error(`请求失败（${res.status}）`)
    }

    reader = res.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let done = false
    const parse = createSseParser((text) => {
      if (done) return
      if (text === '[DONE]') { done = true; return }
      bufferedText += text
      flushTimer ??= setTimeout(flushText, 50)
    })
    while (!done) {
      const chunk = await reader.read()
      if (chunk.done) {
        parse(decoder.decode())
        break
      }
      resetTimeout()
      parse(decoder.decode(chunk.value, { stream: true }))
    }
    if (!done) assistantMsg.notice = '连接已结束，回复可能不完整，请重新提问。'
  } catch (e: any) {
    assistantMsg.notice = controller.signal.aborted
      ? (timedOut ? '响应超时，已停止生成，请稍后重试。' : '已停止生成')
      : '回复中断：' + (e?.message || '网络异常，请稍后重试')
    if (String(e?.message || '').includes('401')) {
      message.warning('登录已过期，请重新登录')
    }
  } finally {
    flushText()
    clearTimeout(timer!)
    await reader?.cancel().catch(() => {})
    reader?.releaseLock()
    activeController = undefined
    assistantMsg.pending = false
    loading.value = false
    scrollToBottom()
  }
}

/**
 * 清空对话
 */
function handleClear() {
  stopGeneration()
  messages.value = []
}

/**
 * 输入框回车发送（Shift+Enter 换行）
 */
function handleEnter(e: KeyboardEvent) {
  if (e.key !== 'Enter' || e.shiftKey || e.isComposing) return
  e.preventDefault()
  handleSend()
}
</script>

<template>
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
            @click="emit('close')"
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
          v-memo="[msg.content, msg.pending, msg.notice, primaryColor]"
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
              <p v-if="msg.notice" class="mt-2 text-xs text-gray-500" role="status">{{ msg.notice }}</p>
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
          <NButton v-if="loading" @click="stopGeneration">停止</NButton>
          <NButton
            v-else
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
