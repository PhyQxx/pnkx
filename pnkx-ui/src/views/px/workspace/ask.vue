<template>
  <section class="qa-page">
    <h1 v-if="!embedded">资料问答</h1><p>沿用后台默认模型。仅使用授权正文片段；生成后校验来源，确认后才创建笔记或待办。</p>
    <el-form label-width="90px" @submit.prevent="ask">
      <el-form-item label="检索关键词"><el-input v-model="form.q" maxlength="200" placeholder="留空使用最近资料；可与问题不同" /></el-form-item>
      <el-form-item label="资料范围"><el-select v-model="form.type"><el-option v-for="(label,value) in types" :key="value" :label="label" :value="value" /></el-select></el-form-item>
      <el-form-item label="主题标签"><el-select v-model="form.tagIds" multiple clearable><el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" /></el-select></el-form-item>
      <el-form-item label="创建日期"><el-date-picker v-model="dates" type="daterange" value-format="YYYY-MM-DD" /></el-form-item>
      <el-form-item label="问题"><el-input v-model="form.question" type="textarea" :rows="3" maxlength="2000" /></el-form-item>
      <el-button type="primary" :disabled="busy || !form.question.trim()" @click="ask">提问 / 重试</el-button><el-button v-if="busy" @click="cancel">取消生成</el-button>
    </el-form>
    <p role="status">{{ message }}</p>
    <article v-if="result"><p>{{ result.coverage }}</p><pre>{{ result.answer }}</pre>
      <h2>引用来源</h2><details v-for="(source,index) in result.citations" :key="source.type + source.id"><summary>[资料 {{ index + 1 }}] <router-link :to="`/r/${source.type}/${source.id}`">{{ source.title }}</router-link></summary><p>{{ source.excerpt }}</p><small>{{ source.locator }} · revision {{ source.revision }}</small></details>
      <h2>编辑草稿并确认</h2><el-select v-model="draft.type"><el-option label="新笔记" value="note"/><el-option label="新待办" value="todo"/></el-select><el-input v-model="draft.title" maxlength="200" placeholder="标题"/><el-input v-model="draft.content" type="textarea" :rows="8" :maxlength="draft.type === 'todo' ? 500 : 100000" show-word-limit/>
      <el-button type="primary" :loading="applying" :disabled="result.status === 'applied'" @click="apply">确认创建并关联来源</el-button><router-link v-if="result.targetId" :to="`/r/${result.targetType}/${result.targetId}`">打开已创建资料</router-link>
    </article>
    <h2>最近问答与用量</h2><p>字符数和耗时用于观察用量，不等同于 token 或费用。失败后手动重试会产生新的请求。</p>
    <div v-for="row in history" :key="row.requestId" class="history"><span>{{ row.status }} · 输入 {{ row.inputChars }} / 输出 {{ row.outputChars }} 字 · {{ row.elapsedMs }} ms</span><el-button v-if="['done','applied'].includes(row.status)" text @click="open(row.requestId)">打开草稿</el-button><el-button v-if="row.status === 'running'" text @click="cancelQa(row.requestId).then(refresh)">取消</el-button></div>
  </section>
</template>
<script>
import { listTags, qaHistory, qaDetail, cancelQa, applyQa } from '@/api/workspace'
import { getToken } from '@/utils/auth'
export default {
  props: { embedded: Boolean },
  data() { return { form: { question: '', q: '', type: 'all', tagIds: [] }, types: { all: '全部正文', note: '笔记', webArticle: '剪藏', todo: '待办' }, dates: [], tags: [], history: [], result: null, draft: { type: 'note', title: '', content: '' }, busy: false, applying: false, message: '', requestId: '', aborter: null } },
  async created() { this.tags = (await listTags()).data; await this.refresh() },
  beforeUnmount() { if (this.busy) { cancelQa(this.requestId).catch(() => {}); this.aborter?.abort() } },
  methods: {
    cancelQa,
    async refresh() { this.history = (await qaHistory()).data },
    install(result) { this.result = result; this.draft = { type: 'note', title: this.form.question.slice(0, 100) || '资料问答整理', content: result.answer || '' } },
    async open(id) { this.install((await qaDetail(id)).data) },
    async cancel() { await cancelQa(this.requestId); this.message = '取消请求已发送'; this.aborter?.abort() },
    async ask() {
      this.busy = true; this.result = null; this.message = '正在检索与生成，来源校验后显示完整答案…'; this.requestId = 'qa-' + Date.now() + '-' + Math.random().toString(36).slice(2); this.aborter = new AbortController()
      const token = getToken()
      try {
        const response = await fetch(import.meta.env.VUE_APP_BASE_API + '/workspace/qa/stream', { method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token }, body: JSON.stringify({ ...this.form, requestId: this.requestId, startDate: this.dates?.[0], endDate: this.dates?.[1] }), signal: this.aborter.signal })
        if (!response.ok || !response.headers.get('content-type')?.includes('text/event-stream')) throw new Error('问答请求失败，请检查登录状态')
        const reader = response.body.getReader(), decoder = new TextDecoder(); let buffer = '', done = false, received = false
        while (!done) {
          const chunk = await reader.read(); done = chunk.done; buffer += decoder.decode(chunk.value, { stream: !done }); const blocks = buffer.split('\n\n'); buffer = blocks.pop()
          for (const block of blocks) {
            if (getToken() !== token) { this.aborter.abort(); throw new Error('账号已变化，请重新打开问答') }
            const lines = block.split('\n'), event = lines.find(line => line.startsWith('event: '))?.slice(7), data = JSON.parse(lines.find(line => line.startsWith('data: '))?.slice(6) || '{}')
            if (event === 'error') throw new Error(data.message)
            if (event === 'progress') this.message = `正在生成（${data.outputChars} 字），完成后校验引用…`
            if (event === 'result') { this.install(data); received = true; this.message = '草稿已保存，尚未写入业务资料' }
          }
        }
        if (!received) throw new Error('连接已结束；请从最近问答检查结果或重新提问')
      } catch (error) { this.message = error.name === 'AbortError' ? '已取消生成' : error.message; await cancelQa(this.requestId).catch(() => {}) }
      finally { this.busy = false; this.aborter = null; await this.refresh() }
    },
    async apply() {
      await this.$confirm('将创建新资料并关联引用来源，是否继续？', '确认草稿')
      this.applying = true
      try { const { data } = await applyQa(this.result.requestId, this.draft); this.result.status = 'applied'; this.result.targetType = data.type; this.result.targetId = data.id; await this.refresh() } finally { this.applying = false }
    }
  }
}
</script>
<style scoped>
.qa-page{max-width:960px;margin:auto;padding:24px}.qa-page pre{white-space:pre-wrap;line-height:1.8}.qa-page details,.history{padding:12px;border-bottom:1px solid #ddd}.qa-page .el-input,.qa-page .el-textarea{margin:8px 0}.qa-page p{line-height:1.7}
</style>
