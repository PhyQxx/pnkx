<template>
  <div class="aiwb">
    <!-- 对话区 -->
    <section class="aiwb-chat">
      <header class="aiwb-head">
        <span class="aiwb-head__ic"><svg-icon icon-class="code" class-name="a-ic16" /></span>
        <div>
          <b>AI 助手</b>
          <i>统一入口 · 记账解析 / 资料问答 / 生活报告 / 剪藏整理；写操作先生成草稿</i>
        </div>
      </header>

      <div class="aiwb-skills">
        <button v-for="s in skills" :key="s.q" class="aiwb-skill" @click="ask(s.q)">{{ s.label }}</button>
      </div>

      <div ref="msgs" class="aiwb-msgs">
        <div v-for="(m, i) in messages" :key="i" class="aiwb-msg" :class="m.role">
          <div class="aiwb-bubble">
            <span class="aiwb-text">{{ m.content }}<i v-if="m.streaming" class="aiwb-cursor" /></span>
            <div v-if="m.pendingAction && !m.resolved" class="aiwb-draft">
              <span>待确认操作 · 确认后才会写入</span>
              <div class="aiwb-draft__acts">
                <button class="aiwb-draft__ok" @click="resolveDraft(m, true)">确认执行</button>
                <button class="aiwb-draft__no" @click="resolveDraft(m, false)">取消</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="aiwb-input">
        <svg-icon icon-class="message" class-name="a-ic15" />
        <input v-model="draft" placeholder="问我任何事，或直接说「午饭花了 30」…" @keydown.enter="ask()" />
        <button class="aiwb-send" :disabled="busy" @click="ask()">
          <svg-icon icon-class="upload" class-name="a-ic14" style="color:#fff" />
        </button>
      </div>
    </section>

    <!-- 右栏 -->
    <aside class="aiwb-rail">
      <div class="aiwb-rail__t">技能</div>
      <div class="aiwb-skillrow" v-for="s in skills" :key="'r' + s.q" @click="ask(s.q)">
        <span class="aiwb-skillrow__ic"><svg-icon :icon-class="s.icon" class-name="a-ic14" /></span>
        <div><b>{{ s.label }}</b><i>{{ s.hint }}</i></div>
      </div>
      <div class="aiwb-rail__t aiwb-rail__t--gap">说明</div>
      <p class="aiwb-rail__note">所有写操作（记账 / 待办 / 笔记）均以草稿呈现，确认后落库；资料问答带引用来源，可一键跳转。</p>
    </aside>
  </div>
</template>

<script>
/**
 * AI 工作台（PRODUCT_UIUE_REDESIGN §4.5）
 * 复用 /ai/chat/stream SSE 流式对话与待确认动作（草稿确认）协议。
 */
import { chatStream, confirmPendingAction, cancelPendingAction } from '@/api/px/ai/chat'

const PENDING_MARK = '待确认'

export default {
  name: 'WorkbenchAi',
  data() {
    return {
      draft: '',
      busy: false,
      messages: [{ role: 'assistant', content: '你好，我是你的统一 AI 助手。记账、查资料、生活报告、剪藏整理都能一句话完成。' }],
      skills: [
        { label: '记一笔', q: '帮我把昨天午饭记一笔', icon: 'money', hint: '「午饭花了30」→ 草稿' },
        { label: '查资料', q: '我的资料里有哪些讲 Java 并发的？', icon: 'documentation', hint: '带引用的 RAG 检索' },
        { label: '生活周报', q: '给我讲讲这周的生活报告', icon: 'chart', hint: '周 / 月 / 年度分析' },
        { label: '总结剪藏', q: '总结一下我最近收藏的文章', icon: 'link', hint: 'AI 摘要与要点' }
      ]
    }
  },
  methods: {
    scrollBottom() { this.$nextTick(() => { const el = this.$refs.msgs; if (el) el.scrollTop = el.scrollHeight }) },
    ask(preset) {
      const question = (preset || this.draft).trim()
      if (!question || this.busy) return
      this.draft = ''
      this.messages.push({ role: 'user', content: question })
      const m = { role: 'assistant', content: '', streaming: true, pendingAction: false, resolved: false }
      this.messages.push(m)
      this.busy = true
      this.stream(question, m)
    },
    async stream(question, m) {
      const history = this.messages.filter(x => !x.streaming && x.content).slice(-8).map(x => ({ role: x.role === 'user' ? 'user' : 'assistant', content: x.content }))
      try {
        const res = await chatStream(question, history)
        if (!res.ok || !res.body) throw new Error('stream unavailable')
        const reader = res.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        let done = false
        while (!done) {
          const { value, done: rDone } = await reader.read()
          if (rDone) break
          buffer += decoder.decode(value, { stream: true })
          // SSE：data: 行拼接，空行分隔，[DONE] 结束
          let idx
          while ((idx = buffer.indexOf('\n\n')) >= 0) {
            const block = buffer.slice(0, idx); buffer = buffer.slice(idx + 2)
            const data = block.split('\n').filter(l => l.startsWith('data:')).map(l => l.substring(5).replace(/^ /, '')).join('\n')
            if (!data || data === '[DONE]') continue
            m.content += data
            if (m.content.includes(PENDING_MARK)) m.pendingAction = true
            this.scrollBottom()
          }
        }
      } catch (e) {
        m.content += (m.content ? '\n' : '') + '（连接中断，请稍后重试）'
      } finally {
        m.streaming = false
        this.busy = false
        this.scrollBottom()
      }
    },
    async resolveDraft(m, ok) {
      m.resolved = true
      try {
        const fn = ok ? confirmPendingAction : cancelPendingAction
        const res = await fn()
        const reader = res.body ? res.body.getReader() : null
        if (reader) {
          const decoder = new TextDecoder()
          let text = ''
          let done = false
          while (!done) {
            const { value, done: d } = await reader.read()
            if (d) break
            text += decoder.decode(value, { stream: true })
          }
          const tail = text.split('\n').filter(l => l.startsWith('data:')).map(l => l.substring(5)).join('').replace('[DONE]', '')
          if (tail) { this.messages.push({ role: 'assistant', content: tail }); this.scrollBottom() }
        }
      } catch (e) {
        this.$message?.error('操作失败，请重试')
      }
    }
  }
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/design-tokens.scss' as t;

.a-ic14 { font-size: 14px; }
.a-ic15 { font-size: 15px; }
.a-ic16 { font-size: 16px; }
.aiwb { position: absolute; inset: 0; display: flex; gap: 12px; padding: 14px 16px 16px; overflow: hidden; }
.aiwb-chat { flex: 1; min-width: 0; display: flex; flex-direction: column; overflow: hidden; border-radius: 15px; background: var(--wb-card); border: 1px solid var(--wb-border); backdrop-filter: blur(20px); }
.aiwb-head { display: flex; align-items: center; gap: 10px; padding: 14px 16px 10px; b { font-size: 14px; } i { display: block; font-style: normal; font-size: 10.5px; color: var(--wb-text-2); } }
.aiwb-head__ic { width: 34px; height: 34px; border-radius: 11px; background: linear-gradient(135deg, var(--wb-accent), var(--wb-accent)); color: #fff; display: flex; align-items: center; justify-content: center; }
.aiwb-skills { display: flex; gap: 8px; padding: 2px 16px 10px; overflow-x: auto; }
.aiwb-skill { flex: none; padding: 7px 12px; border-radius: 100px; border: 1px solid var(--wb-border); background: var(--wb-card); font-size: 11.5px; font-weight: 600; color: var(--wb-text-2); cursor: pointer; transition: all 0.15s ease;
  &:hover { border-color: var(--wb-accent); color: var(--wb-accent); background: rgba(79,134,247,0.08); } }
.aiwb-msgs { flex: 1; overflow-y: auto; padding: 6px 16px; display: flex; flex-direction: column; gap: 12px; }
.aiwb-msg { display: flex; &.user { justify-content: flex-end; } }
.aiwb-bubble { max-width: 76%; padding: 11px 14px; border-radius: 15px; font-size: 13px; line-height: 1.7; white-space: pre-wrap; overflow-wrap: anywhere;
  animation: aiwb-rise 0.28s t.$ease-out both; }
.aiwb-msg.user .aiwb-bubble { background: var(--wb-accent); color: #fff; border-bottom-right-radius: 6px; }
.aiwb-msg.assistant .aiwb-bubble { background: var(--wb-card); border: 1px solid var(--wb-border); border-bottom-left-radius: 6px; max-width: 86%; }
@keyframes aiwb-rise { from { opacity: 0; transform: translateY(9px); } to { opacity: 1; transform: none; } }
.aiwb-cursor { display: inline-block; width: 7px; height: 14px; margin-left: 3px; vertical-align: -2px; background: var(--wb-accent); border-radius: 2px; animation: aiwb-blink 1.06s steps(2) infinite; }
@keyframes aiwb-blink { 50% { opacity: 0; } }
.aiwb-draft { margin-top: 10px; border: 1px solid var(--wb-border); border-radius: 11px; padding: 10px 12px; font-size: 12px; color: var(--wb-text-2);
  span { display: block; } }
.aiwb-draft__acts { display: flex; gap: 8px; margin-top: 9px; }
.aiwb-draft__ok { flex: 1; border: none; border-radius: 9px; padding: 8px 0; font-size: 12px; font-weight: 700; cursor: pointer; background: var(--wb-accent); color: #fff; }
.aiwb-draft__no { flex: 1; border: none; border-radius: 9px; padding: 8px 0; font-size: 12px; font-weight: 700; cursor: pointer; background: var(--wb-hover); color: var(--wb-text-2); }
.aiwb-input { display: flex; align-items: center; gap: 9px; margin: 8px 16px 14px; padding: 8px 10px 8px 15px; border-radius: 100px; background: var(--wb-card); border: 1px solid var(--wb-border); color: var(--wb-text-3);
  input { flex: 1; border: none; outline: none; background: transparent; font-size: 13px; color: var(--wb-text); font-family: inherit; min-width: 0; } }
.aiwb-send { width: 34px; height: 34px; border-radius: 50%; border: none; cursor: pointer; background: var(--wb-accent); color: #fff; display: flex; align-items: center; justify-content: center;
  &:disabled { opacity: 0.5; } &:active { transform: scale(0.9); } }
.aiwb-rail { width: 250px; flex: none; border-radius: 15px; padding: 14px; overflow-y: auto; background: var(--wb-card); border: 1px solid var(--wb-border); backdrop-filter: blur(20px); }
.aiwb-rail__t { font-size: 13px; font-weight: 700; padding: 2px 2px 6px; &--gap { margin-top: 10px; } }
.aiwb-skillrow { display: flex; align-items: center; gap: 10px; padding: 9px 10px; border-radius: 11px; cursor: pointer; transition: background 0.15s ease;
  &:hover { background: rgba(79,134,247,0.08); } b { font-size: 12.5px; display: block; } i { font-style: normal; font-size: 10.5px; color: var(--wb-text-2); } }
.aiwb-skillrow__ic { width: 30px; height: 30px; flex: none; border-radius: 9px; background: rgba(79,134,247,0.12); color: var(--wb-accent); display: flex; align-items: center; justify-content: center; }
.aiwb-rail__note { font-size: 11.5px; color: var(--wb-text-2); line-height: 1.9; margin: 0; padding: 0 2px; }
</style>
