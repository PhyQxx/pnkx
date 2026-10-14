<template>
 <view class="page ws-page subpage-shell"><view class="title">资料问答</view><text>默认模型仅接收授权正文片段。草稿确认后才创建资料；文件正文暂不解析。</text>
 <input v-model="form.q" placeholder="检索关键词（可留空使用最近资料）" maxlength="200"/><picker :range="labels" @change="form.type = types[Number($event.detail.value)]"><button>{{ labels[types.indexOf(form.type)] }}</button></picker>
 <scroll-view scroll-x><view class="tags"><text v-for="tag in tags" :key="tag.id" :class="{active: form.tagIds.includes(tag.id)}" @click="toggle(tag.id)">{{ tag.name }}</text></view></scroll-view>
 <picker mode="date" @change="form.startDate = $event.detail.value"><text>{{ form.startDate || '开始日期' }}</text></picker><picker mode="date" @change="form.endDate = $event.detail.value"><text>{{ form.endDate || '结束日期' }}</text></picker><text @click="form.startDate = ''; form.endDate = ''; form.tagIds = []">清除日期与标签</text>
 <textarea v-model="form.question" placeholder="想从资料中了解什么？" maxlength="2000"/><button :disabled="busy || !form.question.trim()" @click="ask">提问 / 重试</button><button v-if="busy" @click="cancel">取消生成</button><text>{{ message }}</text>
 <view v-if="result" class="card"><text>{{ result.coverage }}</text><text class="answer">{{ result.answer }}</text><view v-for="(source,index) in result.citations" :key="source.type + source.id" class="source" @click="go(source.type, source.id)"><text>[资料 {{ index + 1 }}] {{ source.title }} ↗</text><text>{{ source.excerpt }}</text></view>
 <view class="title">编辑草稿</view><picker :range="['新笔记','新待办']" @change="draft.type = Number($event.detail.value) ? 'todo' : 'note'"><button>{{ draft.type === 'note' ? '新笔记' : '新待办' }}</button></picker><input v-model="draft.title" placeholder="标题" maxlength="200"/><textarea v-model="draft.content" :maxlength="draft.type === 'todo' ? 500 : 100000"/><text v-if="draft.type === 'todo'">待办说明最多 500 字，请精简后确认。</text><button :disabled="applying || result.status === 'applied'" @click="apply">确认创建并关联来源</button><button v-if="result.targetId" @click="go(result.targetType,result.targetId)">打开已创建资料</button></view>
 <view class="title">最近问答与用量</view><text>记录字符数与耗时，不等同 token 或费用。</text><view v-for="row in history" :key="row.requestId" class="card"><text>{{ row.status }} · 输入 {{ row.inputChars }} / 输出 {{ row.outputChars }} 字 · {{ row.elapsedMs }} ms</text><button v-if="['done','applied'].includes(row.status)" @click="open(row.requestId)">打开草稿</button><button v-if="row.status === 'running'" @click="stop(row.requestId)">取消</button></view>
 </view>
</template>
<script>
import { askWorkspace, listTags, qaHistory, qaDetail, cancelQa, applyQa } from '@/api/px/life/workspace'
export default {
 data() { return { form: { question: '', q: '', type: 'all', tagIds: [], startDate: '', endDate: '' }, types: ['all','note','webArticle','todo'], labels: ['全部正文','笔记','剪藏','待办'], tags: [], history: [], result: null, draft: { type: 'note', title: '', content: '' }, busy: false, applying: false, requestId: '', message: '', gone: false } },
 async onLoad() { this.tags = (await listTags()).data; await this.refresh() },
 onUnload() { this.gone = true; if (this.busy) cancelQa(this.requestId).catch(() => {}) },
 methods: {
  toggle(id) { this.form.tagIds = this.form.tagIds.includes(id) ? this.form.tagIds.filter(tag => tag !== id) : [...this.form.tagIds,id] },
  async refresh() { this.history = (await qaHistory()).data },
  install(result) { this.result = result; this.draft = { type: 'note', title: this.form.question.slice(0,100) || '资料问答整理', content: result.answer || '' } },
  async open(id) { this.install((await qaDetail(id)).data) },
  go(type,id) { uni.navigateTo({ url: '/pages_life/workspace/resource?type=' + type + '&id=' + id }) },
  async stop(id) { await cancelQa(id); await this.refresh() },
  async cancel() { await cancelQa(this.requestId); this.message = '取消请求已发送，正在停止模型生成' },
  async ask() {
   this.requestId = 'qa-' + Date.now() + '-' + Math.random().toString(36).slice(2); this.busy = true; this.result = null; this.message = '正在检索与生成，完成后校验引用…'
   try { const { data } = await askWorkspace({ ...this.form, requestId: this.requestId }); if (!this.gone) { this.install(data); this.message = '草稿已保存，尚未写入业务资料' } }
   catch (error) { this.message = error.message || '问答失败，请检查历史记录后重试'; await cancelQa(this.requestId).catch(() => {}) }
   finally { this.busy = false; if (!this.gone) await this.refresh() }
  },
  async apply() {
   await this.$modal.confirm('将创建新资料并关联引用来源。', '确认草稿'); this.applying = true
   try { const { data } = await applyQa(this.result.requestId,this.draft); this.result.status = 'applied'; this.result.targetType = data.type; this.result.targetId = data.id; await this.refresh() } finally { this.applying = false }
  }
 }
}
</script>
<style scoped>
@import '@/static/scss/workspace-common.scss';
.page{padding:28rpx}.title{font-size:36rpx;font-weight:bold;margin:24rpx 0}.page text{display:block;line-height:1.7}.page input,.page textarea{box-sizing:border-box;width:100%;padding:20rpx;background:$bg-card-hover;margin:16rpx 0}.page textarea{height:280rpx}.card{padding:20rpx;border:1px solid $border-color;margin:20rpx 0}.answer{white-space:pre-wrap;margin:20rpx 0}.source{padding:16rpx 0;border-top:1px solid $border-color}.tags{display:flex;gap:20rpx;white-space:nowrap}.active{color:$primary;font-weight:bold}
</style>
