<template>
  <section class="recovery" v-loading="loading">
    <header v-if="!embedded"><h1>{{ id ? '笔记历史' : '回收站' }}</h1><router-link to="/resources/hub">返回资料中心</router-link></header>
    <template v-if="id">
      <p>仅显示内容变化前的版本，最多保留最近 50 个。还原会生成新的修订号，当前内容也会留在历史中。</p>
      <el-select v-model="version" placeholder="选择历史版本" @change="compare"><el-option v-for="row in history" :key="row.revision" :value="row.revision" :label="`版本 ${row.revision} · ${row.createTime}`" /></el-select>
      <div v-if="snapshot" class="comparison"><article><h3>历史版本 {{ snapshot.revision }}：{{ snapshot.title }}</h3><pre>{{ snapshot.content }}</pre></article><article><h3>当前版本 {{ current.revision }}：{{ current.title }}</h3><pre>{{ current.content }}</pre></article></div>
      <el-button v-if="snapshot" type="primary" :loading="saving" @click="restore">还原所选版本</el-button><p v-if="message" role="status">{{ message }}</p><el-empty v-if="!history.length" description="还没有历史版本" />
    </template>
    <template v-else>
      <p>恢复笔记时，已不存在的目录会替换为根目录；剪藏保留原网址与关联。</p>
      <article v-for="row in rows" :key="row.type + row.id" class="trash-row"><span>{{ row.type === 'note' ? '笔记' : row.type === 'file' ? '文件/目录' : '剪藏' }} · {{ row.title }}</span><el-button :disabled="saving" @click="recover(row)">恢复</el-button></article>
      <el-empty v-if="!rows.length" description="回收站为空" /><el-pagination :current-page="page" :page-size="20" :total="total" layout="prev, pager, next" @current-change="loadTrash" />
    </template>
  </section>
</template>
<script>
import { noteHistory, noteVersion, restoreVersion, getResource, listTrash, restoreTrash } from '@/api/workspace'
export default {
  name: 'ResourceRecovery',
  props: { embedded: Boolean },
  data() { return { id: this.$route.query.id, loading: false, saving: false, history: [], version: null, snapshot: null, current: {}, message: '', rows: [], page: 1, total: 0, sequence: 0, initSequence: 0, trashSequence: 0 } },
  watch: { '$route.query.id'() { this.id = this.$route.query.id; this.snapshot = null; this.init() } },
  mounted() { this.init() },
  beforeUnmount() { this.sequence++; this.initSequence++; this.trashSequence++ },
  methods: {
    async init() {
      const sequence = ++this.initSequence, id = this.id
      this.sequence++; this.trashSequence++; this.history = []; this.snapshot = null; this.current = {}; this.version = null; this.message = ''; this.rows = []; this.saving = false; this.loading = true
      try {
        if (id) { const result = await noteHistory(id); if (sequence === this.initSequence) this.history = result.data }
        else await this.loadTrash(1)
      } finally { if (sequence === this.initSequence) this.loading = false }
    },
    async compare() {
      const sequence = ++this.sequence, id = this.id, version = this.version
      this.snapshot = null; this.current = {}; this.message = ''
      const [old, now] = await Promise.all([noteVersion(id,version),getResource({ type: 'note', id })])
      if (id !== this.id || sequence !== this.sequence) return
      this.snapshot = old.data; this.current = now.data
    },
    async restore() {
      if (!this.snapshot || this.saving) return
      const id = this.id, version = this.snapshot.revision, revision = this.current.revision, sequence = this.sequence
      try { await this.$confirm('将历史内容还原为当前版本？当前内容会保留为新的历史快照。', '还原版本') } catch { return }
      if (id !== this.id || sequence !== this.sequence || this.saving) return
      this.saving = true
      try {
        await restoreVersion(id,version,revision)
        if (id !== this.id || sequence !== this.sequence) return
        await this.init()
        if (id === this.id) this.message = '已还原，重新打开笔记即可继续编辑'
      } catch { if (id === this.id) this.message = '还原未完成。如其他设备已修改，请重新选择版本比较后再试。' }
      finally { if (id === this.id) this.saving = false }
    },
    async loadTrash(page) {
      const sequence = ++this.trashSequence
      const result = await listTrash({ pageNum: page, pageSize: 20 })
      if (this.id || sequence !== this.trashSequence) return
      this.rows = result.data.rows; this.total = result.data.total; this.page = page
    },
    async recover(row) { this.saving = true; try { await restoreTrash(row); await this.loadTrash(this.page); this.$message.success('已恢复') } finally { this.saving = false } }
  }
}
</script>
<style scoped>.recovery { padding: 24px; }header,.trash-row { display: flex; justify-content: space-between; align-items: center; }.trash-row { padding: 16px; border-bottom: 1px solid #ddd; }.comparison { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }pre { white-space: pre-wrap; overflow-wrap: anywhere; max-height: 55vh; overflow: auto; }@media(max-width:700px) { .comparison { grid-template-columns: 1fr; } }</style>
