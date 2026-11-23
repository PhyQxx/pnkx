<template>
    <div class="ai-chat" :style="{ '--chat-kb': keyboardInset + 'px' }">
        <!-- 顶部工具行：新建会话 / 操作计划入口（标题栏是共享的，入口放页面内） -->
        <div class="chat-toolbar">
            <button type="button" class="chat-toolbar__btn" :disabled="isLoading || voiceBusy" @click="newConversation">
                <van-icon name="plus" />
                <span>新建会话</span>
            </button>
            <feature-gate feature="operationPlanV2">
                <button
                    type="button"
                    class="chat-toolbar__btn"
                    :class="{ 'is-active': showOperationPlans }"
                    @click="showOperationPlans = !showOperationPlans"
                >
                    <van-icon name="notes-o" />
                    <span>操作计划与恢复</span>
                </button>
            </feature-gate>
        </div>

        <!-- 操作计划与恢复面板（operationPlanV2） -->
        <div v-if="showOperationPlans" class="chat-plans">
            <feature-gate feature="operationPlanV2">
                <div class="chat-plans__inner">
                    <operation-plans />
                </div>
            </feature-gate>
        </div>

        <!-- 消息区域 -->
        <div ref="bodyRef" class="chat-body">
            <!-- 欢迎消息 -->
            <div v-if="messages.length === 0" class="welcome">
                <div class="welcome__avatar">AI</div>
                <div class="welcome__title">你好，有什么可以帮你的？</div>
                <div class="welcome__subtitle">你可以直接跟我说：</div>
                <div class="welcome__prompts">
                    <button
                        v-for="prompt in quickPrompts"
                        :key="prompt.key"
                        type="button"
                        class="welcome__prompt"
                        @click="handleQuickPrompt(prompt.text)"
                    >
                        <span class="welcome__prompt-icon">{{ prompt.icon }}</span>
                        <span class="welcome__prompt-text">{{ prompt.text }}</span>
                    </button>
                </div>
            </div>

            <!-- 消息列表 -->
            <div v-for="msg in messages" :key="msg.id" class="message" :class="msg.role">
                <div v-if="msg.role === 'assistant'" class="message__avatar">AI</div>
                <div class="message__bubble" :class="msg.role">
                    <div
                        v-if="msg.role === 'assistant'"
                        class="message__content m-rich-content"
                        v-html="formatMessage(msg.content)"
                    ></div>
                    <div v-else class="message__content message__content--user">{{ msg.content }}</div>
                    <chat-plan-card v-if="msg.planId" :plan-id="msg.planId" :disabled="isLoading" />
                    <span
                        v-if="isStreaming && msg === messages[messages.length - 1] && msg.role === 'assistant'"
                        class="typing-cursor"
                    >▋</span>
                </div>
            </div>

            <!-- 回复确认保存/取消 -->
            <div v-if="showPendingActions" class="pending-actions">
                <button type="button" class="pending-btn pending-btn--confirm" @click="handlePendingAction('confirm')">
                    确认保存
                </button>
                <button type="button" class="pending-btn" @click="handlePendingAction('cancel')">取消</button>
            </div>

            <!-- 思考中 -->
            <div v-if="isThinking" class="message assistant">
                <div class="message__avatar">AI</div>
                <div class="message__bubble assistant message__bubble--thinking">
                    <div class="typing-indicator">
                        <span class="dot"></span>
                        <span class="dot"></span>
                        <span class="dot"></span>
                    </div>
                </div>
            </div>
        </div>

        <!-- 输入区域（固定在页面底部） -->
        <div class="chat-footer">
            <voice-input :disabled="isLoading" @busy="voiceBusy = $event" @result="appendVoiceText" />
            <div class="chat-footer__wrapper">
                <input
                    v-model="inputText"
                    class="chat-input"
                    type="text"
                    placeholder="输入消息..."
                    :disabled="isLoading"
                    @keyup.enter="handleSend"
                />
                <button
                    type="button"
                    class="send-btn"
                    :disabled="!inputText.trim() || isLoading || voiceBusy"
                    :class="{ 'is-disabled': !inputText.trim() || isLoading || voiceBusy }"
                    @click="handleSend"
                >
                    ➤
                </button>
            </div>
        </div>
    </div>
</template>

<script>
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { createChatStreamParser } from '@/utils/chatStreamParser'
import { createChatHistoryStore, newSessionId, toLocalMessage } from '@/utils/chatHistoryPolicy'
import {
    cancelPendingAction,
    chatStream,
    confirmPendingAction,
    listChatSessions,
    listChatMessages,
    appendChatMessage,
    clearChatSession
} from '@/api/px/ai/chat'
import VoiceInput from './components/VoiceInput.vue'
import ChatPlanCard from './components/ChatPlanCard.vue'
import OperationPlans from './components/OperationPlans.vue'

export default {
    name: 'MAiChat',
    components: { ChatPlanCard, OperationPlans, VoiceInput },
    data() {
        return {
            voiceBusy: false,
            showOperationPlans: false,
            sessionId: newSessionId(),
            inputText: '',
            isLoading: false,
            isThinking: false,
            isStreaming: false,
            messages: [],
            messageId: 0,
            keyboardInset: 0,
            quickPrompts: [
                { key: 'bookkeeping', text: '午餐花了30块', icon: '💰' },
                { key: 'todo', text: '提醒我明天开会', icon: '📋' },
                { key: 'diary', text: '今天心情不错', icon: '📝' },
                { key: 'analysis', text: '分析本月消费', icon: '📊' }
            ]
        }
    },
    computed: {
        showPendingActions() {
            const last = this.messages[this.messages.length - 1]
            return !!last && last.role === 'assistant' && last.pendingAction && !last.planId && !this.isThinking
        }
    },
    mounted() {
        this.history = createChatHistoryStore({
            appendMessage: appendChatMessage,
            loadMessages: (sessionId) => listChatMessages(sessionId)
        })
        this.restoreLatestSession()
        // 软键盘弹起自适应：visualViewport 被压缩的量作为高度扣减
        if (window.visualViewport) {
            window.visualViewport.addEventListener('resize', this.handleViewportChange)
            window.visualViewport.addEventListener('scroll', this.handleViewportChange)
            this.handleViewportChange()
        }
    },
    beforeUnmount() {
        if (window.visualViewport) {
            window.visualViewport.removeEventListener('resize', this.handleViewportChange)
            window.visualViewport.removeEventListener('scroll', this.handleViewportChange)
        }
    },
    methods: {
        appendVoiceText(text) {
            this.inputText = this.inputText ? `${this.inputText} ${text}` : text
        },
        handleViewportChange() {
            const vv = window.visualViewport
            if (!vv) return
            const inset = Math.max(0, Math.round(window.innerHeight - vv.height - vv.offsetTop))
            this.keyboardInset = inset
            if (inset > 0) this.scrollToBottom()
        },

        /**
         * 恢复服务端最近一次会话；恢复失败保留空的新会话
         */
        async restoreLatestSession() {
            try {
                const res = await listChatSessions()
                const sessions = res.data || []
                if (!sessions.length) return
                this.sessionId = sessions[0].sessionId
                const msgRes = await listChatMessages(this.sessionId)
                this.messages = (msgRes.data || []).map(toLocalMessage)
                this.messageId = this.messages.reduce((max, m) => Math.max(max, m.id), 0)
                await this.scrollToBottom()
            } catch (e) {
                console.warn('聊天历史恢复失败', e)
            }
        },

        /**
         * Markdown → HTML（过滤指令性文本，DOMPurify 消毒后 v-html 渲染）
         */
        formatMessage(content) {
            if (!content) return ''
            const text = content
                .replace(/\n?回复“确认”保存，回复“取消”放弃。/g, '')
                .replace(/\[?\s*PENDING_CONFIRM\s*\]?/gi, '')
                .trim()
            if (!text) return ''
            const html = marked.parse(text, { gfm: true, breaks: true })
            return DOMPurify.sanitize(String(html))
        },

        newConversation() {
            if (this.isLoading || this.voiceBusy) return
            const previous = this.sessionId
            this.sessionId = newSessionId()
            this.messages = []
            this.messageId = 0
            if (previous) {
                clearChatSession(previous).catch(() => {})
            }
        },

        handleQuickPrompt(text) {
            if (this.isLoading || this.voiceBusy) return
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
            this.messages.forEach((msg) => {
                msg.pendingAction = false
            })
        },

        isPendingDraft(content) {
            if (typeof content !== 'string') return false
            const text = content.toUpperCase()
            if (
                content.includes('草稿已过期') ||
                content.includes('草稿已失效') ||
                content.includes('没有待确认的草稿')
            )
                return false
            return text.includes('回复“确认”保存') || text.includes('PENDING_CONFIRM')
        },

        async handleSend() {
            const text = this.inputText.trim()
            if (!text || this.isLoading || this.voiceBusy) return

            this.inputText = ''
            await this.sendText(text)
        },

        async handlePendingAction(action) {
            if (this.isLoading || this.voiceBusy) return
            this.clearPendingActions()
            this.isLoading = true
            this.isThinking = true
            this.isStreaming = false

            try {
                const response = action === 'confirm' ? await confirmPendingAction() : await cancelPendingAction()
                this.isThinking = false
                this.isStreaming = true
                this.addMessage('assistant', '')
                await this.scrollToBottom()
                await this.readStreamResponse(response)
            } catch (e) {
                this.isThinking = false
                this.removeEmptyAssistant()
                this.addMessage('assistant', '网络请求失败，请稍后重试。')
            } finally {
                this.isLoading = false
                this.isStreaming = false
                await this.scrollToBottom()
            }
        },

        removeEmptyAssistant() {
            const last = this.messages[this.messages.length - 1]
            if (last && last.role === 'assistant' && !last.content && !last.planId) {
                this.messages.pop()
            }
        },

        async sendText(text) {
            this.isLoading = true
            this.isThinking = true
            this.isStreaming = false

            this.addMessage('user', text)
            if (this.history) this.history.recordUser(this.sessionId, text)
            await this.scrollToBottom()

            try {
                // 构建对话历史（最近10条消息）
                const history = this.messages.slice(-10).map((m) => ({
                    role: m.role,
                    content: m.content
                }))
                const response = await chatStream(text, history)
                this.isThinking = false
                this.isStreaming = true
                this.addMessage('assistant', '')
                await this.scrollToBottom()

                await this.readStreamResponse(response)
            } catch (e) {
                this.isThinking = false
                this.removeEmptyAssistant()
                this.addMessage('assistant', '网络请求失败，请稍后重试。')
            } finally {
                this.isLoading = false
                this.isStreaming = false
                await this.scrollToBottom()
            }
        },

        async readStreamResponse(response) {
            const reader = response.body.getReader()
            const decoder = new TextDecoder()
            let fullContent = ''
            const parser = createChatStreamParser(
                (text) => {
                    fullContent += text
                    this.updateLastAssistant(fullContent)
                },
                (id) => {
                    const last = this.messages[this.messages.length - 1]
                    if (last && last.role === 'assistant') last.planId = id
                }
            )
            while (true) {
                const { done, value } = await reader.read()
                if (done) break
                parser.push(decoder.decode(value, { stream: true }))
                await this.scrollToBottom()
            }
            parser.push(decoder.decode())
            parser.finish()
            this.updateLastAssistant(fullContent || '抱歉，AI 暂时无法回复，请稍后再试。')
            const last = this.messages[this.messages.length - 1]
            if (this.history && last && last.role === 'assistant') {
                this.history.recordAssistant(this.sessionId, last.content, last.planId)
            }
        },

        async scrollToBottom() {
            await this.$nextTick()
            const el = this.$refs.bodyRef
            if (el) el.scrollTop = el.scrollHeight
        }
    }
}
</script>

<style lang="scss" scoped>
.ai-chat {
    // 铺满导航栏以下区域；软键盘弹起时通过 --chat-kb 再扣减（visualViewport 自适应）
    height: calc(100vh - var(--van-nav-bar-height, 46px) - env(safe-area-inset-top, 0px) - var(--chat-kb, 0px));
    height: calc(100dvh - var(--van-nav-bar-height, 46px) - env(safe-area-inset-top, 0px) - var(--chat-kb, 0px));
    display: flex;
    flex-direction: column;
    overflow: hidden;
    background: var(--pnkx-bg);
}

// ============ 顶部工具行 ============
.chat-toolbar {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    padding: 6px 12px;
    background: var(--pnkx-surface);
    border-bottom: 1px solid var(--pnkx-border-soft);
}

.chat-toolbar__btn {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: var(--pnkx-text-secondary);
    background: var(--pnkx-surface-muted);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 999px;
    padding: 4px 12px;

    &.is-active {
        color: var(--pnkx-primary);
        background: var(--pnkx-primary-soft);
        border-color: var(--pnkx-primary);
    }

    &:disabled {
        opacity: 0.5;
    }

    &:active {
        opacity: 0.8;
    }
}

// ============ 操作计划面板 ============
.chat-plans {
    flex-shrink: 0;
    max-height: 55%;
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
}

.chat-plans__inner {
    padding: 12px 12px 0;
}

// ============ 消息区域 ============
.chat-body {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    padding-top: 10px;
    -webkit-overflow-scrolling: touch;
}

// 欢迎区域
.welcome {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 40px 20px;
}

.welcome__avatar {
    width: 50px;
    height: 50px;
    border-radius: 50%;
    background: var(--pnkx-primary);
    color: #fff;
    font-size: 18px;
    font-weight: bold;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 12px;
}

.welcome__title {
    font-size: 17px;
    color: var(--pnkx-text);
    font-weight: 600;
    margin-bottom: 6px;
}

.welcome__subtitle {
    font-size: 13px;
    color: var(--pnkx-text-muted);
    margin-bottom: 16px;
}

.welcome__prompts {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    justify-content: center;
}

.welcome__prompt {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 8px 12px;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border);
    border-radius: 999px;

    &:active {
        border-color: var(--pnkx-primary);
        background: var(--pnkx-primary-soft);
    }
}

.welcome__prompt-icon {
    font-size: 14px;
}

.welcome__prompt-text {
    font-size: 13px;
    color: var(--pnkx-text-secondary);
}

// 消息
.message {
    display: flex;
    gap: 8px;
    margin-bottom: 14px;
    padding: 0 12px;

    &.user {
        justify-content: flex-end;
    }

    &.assistant {
        justify-content: flex-start;
    }
}

.message__avatar {
    width: 30px;
    height: 30px;
    border-radius: 50%;
    background: var(--pnkx-primary);
    color: #fff;
    font-size: 11px;
    font-weight: bold;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    margin-top: 4px;
}

.message__bubble {
    max-width: 80%;
    padding: 10px 14px;
    border-radius: 12px;
    font-size: 14px;
    line-height: 1.7;
    word-break: break-word;

    &.user {
        background: var(--pnkx-primary);
        color: #fff;
        border-bottom-right-radius: 4px;
    }

    &.assistant {
        background: var(--pnkx-surface);
        color: var(--pnkx-text);
        border-bottom-left-radius: 4px;
        box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
        border: 1px solid var(--pnkx-border-soft);
    }
}

.message__content--user {
    white-space: pre-wrap;
}

.message__bubble--thinking {
    padding: 14px 18px;
}

// 回复确认按钮
.pending-actions {
    display: flex;
    gap: 8px;
    margin: -4px 0 14px 50px;
}

.pending-btn {
    height: 32px;
    line-height: 30px;
    padding: 0 14px;
    border-radius: 8px;
    font-size: 13px;
    border: 1px solid var(--pnkx-border);
    background: var(--pnkx-surface);
    color: var(--pnkx-text-secondary);

    &--confirm {
        background: var(--pnkx-primary);
        border-color: var(--pnkx-primary);
        color: #fff;
    }

    &:active {
        opacity: 0.82;
    }
}

// 打字光标
.typing-cursor {
    display: inline-block;
    color: var(--pnkx-primary);
    font-size: 14px;
    animation: ai-chat-blink 0.8s ease-in-out infinite;
    margin-left: 4px;
}

@keyframes ai-chat-blink {
    0%,
    100% {
        opacity: 1;
    }
    50% {
        opacity: 0;
    }
}

// 思考动画
.typing-indicator {
    display: flex;
    gap: 5px;
    align-items: center;

    .dot {
        width: 7px;
        height: 7px;
        border-radius: 50%;
        background: var(--pnkx-text-muted);
        animation: ai-chat-typing 1.4s infinite ease-in-out;

        &:nth-child(1) {
            animation-delay: 0s;
        }
        &:nth-child(2) {
            animation-delay: 0.2s;
        }
        &:nth-child(3) {
            animation-delay: 0.4s;
        }
    }
}

@keyframes ai-chat-typing {
    0%,
    60%,
    100% {
        transform: translateY(0);
        opacity: 0.4;
    }
    30% {
        transform: translateY(-6px);
        opacity: 1;
    }
}

// ============ 输入区域（页面底部） ============
.chat-footer {
    flex-shrink: 0;
    background: var(--pnkx-surface);
    border-top: 1px solid var(--pnkx-border-soft);
    padding-bottom: env(safe-area-inset-bottom);
}

.chat-footer__wrapper {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
}

.chat-input {
    flex: 1;
    min-width: 0;
    height: 38px;
    background: var(--pnkx-surface-muted);
    border: none;
    outline: none;
    border-radius: 999px;
    padding: 0 16px;
    font-size: 14px;
    color: var(--pnkx-text);

    &::placeholder {
        color: var(--pnkx-text-muted);
    }

    &:disabled {
        opacity: 0.6;
    }
}

.send-btn {
    width: 38px;
    height: 38px;
    border-radius: 50%;
    background: var(--pnkx-primary);
    color: #fff;
    font-size: 15px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: opacity 0.2s;

    &.is-disabled {
        opacity: 0.4;
    }

    &:active {
        transform: scale(0.95);
    }
}
</style>
