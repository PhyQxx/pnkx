<template>
  <details class="resource-links" @toggle="expanded = $event.target.open; expanded && reload()">
    <summary><el-icon><Link /></el-icon>关联资料与反链</summary>
    <div v-if="expanded" v-loading="loading">
      <el-button text size="small" class="add-link" @click="showPicker = true; search(1)">＋ 关联资料</el-button>
      <p v-if="!links.length" class="links-empty">尚未关联资料</p>
      <div v-for="link in links" :key="link.targetType + ':' + link.targetId + ':' + link.relationType" class="link-row">
        <span v-if="link.unavailable">资料已删除或不可访问</span>
        <router-link v-else :to="`/r/${link.targetType}/${link.targetId}`">{{ link.title || '未命名资料' }}</router-link>
        <el-button v-if="link.relationType === 'related'" text size="small" @click="remove(link)">移除</el-button>
        <small v-else>正文引用</small>
      </div>
      <h4>被引用于（{{ backlinks.length }}）</h4>
      <div v-for="link in backlinks" :key="link.sourceType + ':' + link.sourceId + ':' + link.relationType"><router-link :to="`/r/${link.sourceType}/${link.sourceId}`">{{ link.title || '未命名资料' }}</router-link></div>
    </div>
    <el-dialog v-model="showPicker" title="选择关联资料" width="min(640px,95vw)" append-to-body>
      <el-input v-model="keyword" placeholder="搜索标题或正文" @keyup.enter="search(1)"><template #append><el-button @click="search(1)">搜索</el-button></template></el-input>
      <div v-for="row in candidates" :key="row.type + ':' + row.id" class="link-row"><span>{{ row.title }} · {{ row.type }}</span><el-button :disabled="row.type === type && String(row.id) === String(id)" @click="add(row)">关联</el-button></div>
      <el-pagination :current-page="page" :page-size="10" :total="total" layout="prev, pager, next" @current-change="search" />
    </el-dialog>
  </details>
</template>
<script>
import { getLinks, getBacklinks, addLink, removeLink, listResources } from '@/api/workspace'
export default {
  name: 'ResourceLinks', props: { type: { type: String, required: true }, id: { type: [String, Number], required: true } },
  data() { return { expanded: false, links: [], backlinks: [], loading: false, searchSequence: 0, sequence: 0, showPicker: false, keyword: '', candidates: [], total: 0, page: 1 } },
  watch: { id() { this.sequence++; this.searchSequence++; this.links = []; this.backlinks = []; this.showPicker = false; if (this.expanded) this.reload() } },
  methods: {
    source() { return { type: this.type, id: this.id } },
    async reload() { const sequence = ++this.sequence; this.loading = true; try { const [out, incoming] = await Promise.all([getLinks(this.source()), getBacklinks(this.source())]); if (sequence !== this.sequence) return; this.links = out.data; this.backlinks = incoming.data } finally { if (sequence === this.sequence) this.loading = false } },
    async search(page) { const sequence = ++this.searchSequence; const res = await listResources({ q: this.keyword, pageNum: page, pageSize: 10 }); if (sequence !== this.searchSequence) return; this.candidates = res.data.rows; this.total = res.data.total; this.page = page },
    async add(row) { await addLink(this.source(), row); this.showPicker = false; await this.reload() },
    async remove(link) { await removeLink(this.source(), link); await this.reload() }
  }
}
</script>
<style scoped>
.resource-links { margin: 4px 22px 6px 0; border-top: 1px dashed var(--border-primary); padding: 6px 0 6px 8px; }
.resource-links summary { display: inline-flex; align-items: center; gap: 5px; cursor: pointer; user-select: none; padding: 4px 10px; border-radius: var(--radius-full, 999px); color: var(--text-secondary); font-size: 12px; list-style: none; transition: color .15s, background .15s; }
.resource-links summary::-webkit-details-marker { display: none; }
.resource-links summary:hover, .resource-links[open] summary { color: var(--pnkx-primary); background: var(--pnkx-primary-soft); }
.resource-links h4 { margin: 10px 0 2px; font-size: 12px; color: var(--text-muted); font-weight: 600; }
.resource-links > div { max-height: 260px; overflow: auto; padding-left: 10px; border-left: 2px solid var(--border-secondary); margin-top: 6px; }
.links-empty { margin: 6px 0; color: var(--text-muted); font-size: 13px; }
.add-link { font-size: 13px; }
.link-row { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 7px 0; border-bottom: 1px solid var(--border-secondary); font-size: 13px; overflow-wrap: anywhere; }
.link-row:last-of-type { border-bottom: none; }
</style>
