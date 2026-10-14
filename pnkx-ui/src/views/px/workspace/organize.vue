<template>
  <section class="organize">
    <header v-if="!embedded"><h1>整理建议</h1><router-link to="/resources/hub">返回资料中心</router-link></header>
    <p>扫描自己的笔记、剪藏和待办。空内容、无标签、抓取失败和重复候选只加入收件箱供核查；空标题可预览后补全。不会自动删除资料。</p>
    <el-button :loading="scanning" @click="scan">扫描资料</el-button><el-button :disabled="!scanning" @click="stopped = true">停止扫描</el-button><el-button :loading="auditing" @click="audit">旧标签对账</el-button><p role="status">{{ message }}</p>
    <article v-for="row in rows" :key="row.id"><router-link :to="`/r/${row.resourceType}/${row.resourceId}`">{{ row.title || '未命名资料' }}</router-link><p>{{ row.reason }}</p><blockquote v-if="row.proposedTitle">新标题：{{ row.proposedTitle }}</blockquote><el-button :disabled="saving" @click="apply(row,false)">{{ row.proposedTitle ? '应用标题' : '加入收件箱' }}</el-button><el-button :disabled="saving" @click="apply(row,true)">忽略</el-button><span>{{ results[row.id] }}</span></article>
    <el-empty v-if="!rows.length" description="暂无待处理建议" /><el-button v-if="hasMore" @click="load(nextAfter)">下一批建议</el-button><el-button @click="load(0)">刷新</el-button>
    <pre v-if="auditReport">{{ auditReport }}</pre>
  </section>
</template>
<script>
import { scanOrganize, listOrganize, applyOrganize, auditTags } from '@/api/workspace'
export default {
  name: 'ResourceOrganize', props: { embedded: Boolean }, data() { return { rows: [], nextAfter: 0, hasMore: false, scanning: false, stopped: false, saving: false, auditing: false, auditReport: '', message: '', results: {} } },
  mounted() { this.load(0) }, beforeUnmount() { this.stopped = true },
  methods: {
    async load(after) { const result = await listOrganize(after); this.rows = result.data.rows; this.nextAfter = result.data.nextAfter; this.hasMore = result.data.hasMore },
    async scan() { this.scanning = true; this.stopped = false; let checked = 0; try { for (const type of ['note','webArticle','todo']) { let after = 0; while (!this.stopped) { const result = await scanOrganize(type,after); checked += result.data.checked; this.message = `已检查 ${checked} 条。重复比较限每批 100 条。`; after = result.data.lastId; if (result.data.done) break } } await this.load(0) } finally { this.scanning = false } },
    async apply(row,ignore) { this.saving = true; try { const result = await applyOrganize(row.id,ignore); this.results[row.id] = ({ applied: '已应用', ignored: '已忽略', stale: '资料已变化，请重新扫描' })[result.data] || result.data } finally { this.saving = false } },
    async audit() { this.auditing = true; let checked = 0; const differences = []; try { for (const type of ['webArticle','todo']) { let after = 0; let done = false; while (!done) { const result = await auditTags(type,after); checked += result.data.checked; differences.push(...result.data.differences.map(row => ({ type,...row }))); after = result.data.lastId; done = result.data.done } } this.auditReport = `已检查 ${checked} 条，差异 ${differences.length} 条。\n` + JSON.stringify(differences,null,2) } finally { this.auditing = false } }
  }
}
</script>
<style scoped>.organize { padding: 24px; }header { display: flex; justify-content: space-between; align-items: center; }article { margin: 20px 0; padding: 20px; border: 1px solid #ddd; border-radius: 12px; }pre { white-space: pre-wrap; overflow-wrap: anywhere; }</style>
