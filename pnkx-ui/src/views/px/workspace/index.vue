<template>
  <section class="workspace">
    <header class="page-head">
      <div class="page-head-text">
        <h1>资料中心</h1>
        <p>把笔记、剪藏和待办放在同一个主题里</p>
      </div>
      <router-link class="page-head-link" to="/resources/search">
        <el-icon><Search /></el-icon>搜索全部生活记录
      </router-link>
    </header>
    <div class="workspace-body">
      <aside class="side">
        <nav class="side-group" aria-label="资料工具">
          <button v-for="t in toolList" :key="t.key" type="button" class="side-link" :class="{ active: tool === t.key }" @click="toggleTool(t.key)">
            <el-icon><component :is="t.icon" /></el-icon>{{ t.label }}
          </button>
        </nav>
        <div class="side-group">
          <h2 class="side-title">快捷视图</h2>
          <button type="button" class="quick-view" :class="{ active: activeView === 'all' }" @click="showAll">
            <el-icon><Collection /></el-icon>全部资料
          </button>
          <button type="button" class="quick-view" :class="{ active: activeView === 'inbox' }" @click="showInbox">
            <el-icon><Inbox /></el-icon>收件箱
          </button>
          <button type="button" class="quick-view" :class="{ active: activeView === 'recent' }" @click="showRecent">
            <el-icon><Reading /></el-icon>继续阅读
          </button>
        </div>
        <div class="side-group side-tags">
          <h2 class="side-title">主题标签
            <el-button text size="small" class="tag-new" @click="editTag()"><el-icon><Plus /></el-icon>新建</el-button>
          </h2>
          <el-tree class="tag-tree" :class="{ capped: tagTree.length }" :data="tagTree" node-key="id" :props="{ label: 'name', children: 'children' }"
            default-expand-all empty-text="还没有主题标签" @node-click="selectTheme">
            <template #default="{ data }">
              <span class="tag-node" :class="{ active: activeView === 'tag:' + data.id }">
                <span class="tag-dot" :style="{ background: data.color || 'var(--pnkx-primary)' }"></span>
                <span class="tag-name">{{ data.name }}</span>
                <el-button class="tag-manage" text size="small" @click.stop="editTag(data)">管理</el-button>
              </span>
            </template>
          </el-tree>
        </div>
        <div class="side-foot">
          <el-button class="legacy-btn" text :loading="importing" @click="importLegacy">
            <el-icon><RefreshLeft /></el-icon>迁移已有标签
          </el-button>
          <p v-if="importStatus" role="status">{{ importStatus }}</p>
        </div>
      </aside>
      <main class="content">
        <div v-if="tool" class="tool-panel" :data-tool="tool">
          <div class="tool-panel-head">
            <div class="tool-panel-title">
              <h2>{{ toolMeta.label }}</h2>
              <p>{{ toolMeta.desc }}</p>
            </div>
            <el-button text @click="closeTool"><el-icon><Close /></el-icon>返回资料列表</el-button>
          </div>
          <component :is="toolView" embedded />
        </div>
        <template v-else>
        <form class="filter-card" @submit.prevent="search(1)">
          <div class="filter-main">
            <el-input v-model="filters.q" class="filter-q" clearable placeholder="搜索标题或正文…" @clear="search(1)">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-select v-model="filters.type" class="filter-type" @change="onTypeChange">
              <el-option v-for="(name, type) in types" :key="type" :label="name" :value="type" />
            </el-select>
            <el-button type="primary" native-type="submit"><el-icon><Filter /></el-icon>筛选</el-button>
            <el-button text @click="resetFilters">重置</el-button>
            <el-button text class="advanced-toggle" @click="advancedOpen = !advancedOpen">
              高级筛选<span v-if="advancedCount" class="advanced-count">{{ advancedCount }}</span>
              <el-icon class="advanced-arrow" :class="{ open: advancedOpen }"><ArrowDown /></el-icon>
            </el-button>
          </div>
          <div v-show="advancedOpen" class="filter-advanced">
            <div class="filter-advanced-row">
              <el-select v-model="filters.tagIds" multiple clearable collapse-tags collapse-tags-tooltip placeholder="选择标签" class="filter-tags">
                <el-option v-for="tag in tags" :key="tag.id" :value="tag.id" :label="tag.name">
                  <span class="tag-option"><span class="tag-dot" :style="{ background: tag.color || 'var(--pnkx-primary)' }"></span>{{ tag.name }}</span>
                </el-option>
              </el-select>
              <el-select v-model="filters.tagMode" style="width: 110px">
                <el-option label="满足全部" value="all" /><el-option label="满足任一" value="any" />
              </el-select>
              <template v-if="filters.type === 'webArticle'">
                <el-select v-model="filters.readFlag" clearable placeholder="阅读状态" style="width: 110px">
                  <el-option label="已读" :value="true" /><el-option label="未读" :value="false" />
                </el-select>
                <el-select v-model="filters.starFlag" clearable placeholder="收藏状态" style="width: 110px">
                  <el-option label="已收藏" :value="true" /><el-option label="未收藏" :value="false" />
                </el-select>
              </template>
              <el-date-picker v-model="dates" type="daterange" value-format="YYYY-MM-DD"
                start-placeholder="开始日期" end-placeholder="结束日期" style="width: 240px" />
            </div>
            <div class="filter-advanced-row">
              <el-checkbox v-model="filters.includeDescendants">包含子标签</el-checkbox>
              <el-checkbox v-model="filters.untagged">未整理</el-checkbox>
            </div>
          </div>
        </form>
        <div class="toolbar">
          <span class="toolbar-count">共 {{ total }} 条资料<template v-if="selected.length">，已选 {{ selected.length }} 条</template></span>
          <div class="toolbar-actions">
            <el-button :disabled="!selected.length" @click="openBatch"><el-icon><PriceTag /></el-icon>批量加标签</el-button>
            <el-button :disabled="!selected.length" @click="batchInbox">
              <el-icon><Box /></el-icon>{{ filters.inbox ? '批量移出收件箱' : '批量加入收件箱' }}
            </el-button>
            <span class="view-switch" role="group" aria-label="切换视图">
              <button type="button" class="view-btn" :class="{ active: !cards }" title="列表视图" @click="cards = false"><el-icon><List /></el-icon></button>
              <button type="button" class="view-btn" :class="{ active: cards }" title="卡片视图" @click="cards = true"><el-icon><Menu /></el-icon></button>
            </span>
          </div>
        </div>
        <el-empty v-if="!loading && !rows.length" description="没有匹配的资料，试试调整筛选条件" />
        <div v-loading="loading" class="resources" :class="{ cards }">
          <article v-for="row in rows" :key="key(row)" class="resource">
            <div class="resource-head">
              <el-checkbox :model-value="selected.some(item => key(item) === key(row))" :aria-label="'选择' + row.title" @change="toggle(row)" />
              <span class="type-chip" :class="'t-' + row.type"><el-icon><component :is="typeIcons[row.type] || 'Document'" /></el-icon>{{ types[row.type] }}</span>
              <router-link :to="row.route" class="title" @click="visit(row)">{{ row.title }}</router-link>
              <small class="time" :title="row.updatedAt">{{ fmtTime(row.updatedAt) }}</small>
            </div>
            <p v-if="row.subtitle" class="subtitle">{{ row.subtitle }}</p>
            <div v-if="row.tags.length" class="resource-tags">
              <el-tag v-for="tag in row.tags" :key="tag" size="small" effect="plain" round>{{ tag }}</el-tag>
            </div>
            <footer class="resource-foot">
              <el-button text size="small" @click="inbox(row)">
                <el-icon><Box /></el-icon>{{ filters.inbox ? '移出收件箱' : '放入收件箱' }}
              </el-button>
              <el-button v-if="row.type === 'webArticle'" text size="small" @click="openConversion(row)">
                <el-icon><Switch /></el-icon>转为笔记/待办
              </el-button>
              <span class="foot-space"></span>
              <el-button v-if="row.type === 'note'" text size="small" @click="openHistory(row)">
                <el-icon><Clock /></el-icon>历史版本
              </el-button>
              <el-button text size="small" @click="openTags(row)"><el-icon><PriceTag /></el-icon>管理标签</el-button>
            </footer>
            <resource-links :type="row.type" :id="row.id" />
          </article>
        </div>
        <div v-if="total" class="pagination-row">
          <el-pagination background :current-page="page" :page-size="20" :total="total" layout="total, prev, pager, next" @current-change="search" />
        </div>
        </template>
      </main>
    </div>
    <el-dialog v-model="conversionDialog" title="将剪藏转为行动" width="min(600px,95vw)">
      <p class="dialog-tip">检查标题和正文后创建，来源会自动关联。</p>
      <div class="dialog-form">
        <el-select v-model="conversion.targetType" style="width: 140px"><el-option label="笔记" value="note" /><el-option label="待办" value="todo" /></el-select>
        <el-input v-model="conversion.title" placeholder="标题" maxlength="200" />
        <el-input v-model="conversion.content" type="textarea" :rows="10" placeholder="内容" />
      </div>
      <template #footer><el-button :loading="saving" type="primary" @click="convert">确认创建</el-button></template>
    </el-dialog>
    <el-dialog v-model="tagDialog" :title="tagForm.id ? '管理标签' : '新建标签'" width="min(480px, 95vw)">
      <el-form class="dialog-form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="tagForm.name" maxlength="100" placeholder="标签名称" /></el-form-item>
        <el-form-item label="父标签"><el-select v-model="tagForm.parentId"><el-option label="根标签" :value="0" /><el-option v-for="tag in tags.filter(t => t.id !== tagForm.id)" :key="tag.id" :value="tag.id" :label="tag.name" /></el-select></el-form-item>
        <el-form-item label="颜色"><el-color-picker v-model="tagForm.color" /></el-form-item>
        <el-form-item v-if="tagForm.id" label="合并到"><div class="merge-row"><el-select v-model="mergeTarget" placeholder="选择保留的标签"><el-option v-for="tag in tags.filter(t => t.id !== tagForm.id)" :key="tag.id" :value="tag.id" :label="tag.name" /></el-select><el-button :disabled="!mergeTarget" @click="merge">合并</el-button></div></el-form-item>
      </el-form>
      <template #footer><el-button v-if="tagForm.id" type="danger" plain @click="remove">删除标签</el-button><el-button @click="tagDialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="relationDialog" :title="editingRow ? '资料标签' : '批量添加标签'" width="min(480px, 95vw)">
      <el-select v-model="selectedTags" multiple filterable placeholder="选择标签" style="width: 100%"><el-option v-for="tag in tags" :key="tag.id" :value="tag.id" :label="tag.name" /></el-select>
      <p v-if="batchResult">{{ batchResult }}</p>
      <template #footer><el-button @click="relationDialog = false">关闭</el-button><el-button type="primary" :loading="saving" @click="applyTags">保存</el-button></template>
    </el-dialog>
  </section>
</template>
<script>
import { defineAsyncComponent } from 'vue'
import ResourceLinks from '@/components/ResourceLinks'
import { batchInbox as updateBatchInbox, getResource, visitResource, setInbox, convertArticle, listResources, listTags, saveTag, deleteTag, mergeTags, getResourceTags, setResourceTags, batchTags, backfillTags } from '@/api/workspace'
export default {
  name: 'ResourceWorkspace',
  components: { ResourceLinks,
    ToolAsk: defineAsyncComponent(() => import('./ask')),
    ToolFiles: defineAsyncComponent(() => import('./files')),
    ToolSeries: defineAsyncComponent(() => import('./series')),
    ToolOrganize: defineAsyncComponent(() => import('./organize')),
    ToolRecovery: defineAsyncComponent(() => import('./recovery')) },
  data() {
    return { filters: { q: '', type: 'all', tagIds: [], tagMode: 'all', includeDescendants: true, untagged: this.$route.query.untagged === 'true', recent: this.$route.query.recent === 'true', inbox: undefined }, dates: [],
      rows: [], tags: [], total: 0, page: 1, loading: false, cards: localStorage.getItem('resourceHubCards') === '1', selected: [], sequence: 0, advancedOpen: false,
      types: { all: '全部', file: '文件', note: '笔记', webArticle: '剪藏', todo: '待办' }, typeIcons: { file: 'Folder', note: 'EditPen', webArticle: 'Link', todo: 'Checked' }, tagDialog: false, tagForm: {}, mergeTarget: null,
      tool: '', toolList: [
        { key: 'ask', label: '资料问答', icon: 'ChatDotRound' },
        { key: 'files', label: '个人文件', icon: 'FolderOpened' },
        { key: 'series', label: '重复待办', icon: 'CopyDocument' },
        { key: 'organize', label: '整理建议', icon: 'MagicStick' },
        { key: 'recovery', label: '回收站', icon: 'Delete' }],
      conversionDialog: false, conversion: {}, conversionId: null, relationDialog: false, selectedTags: [], editingRow: null, batchResult: '', saving: false, importing: false, importStatus: '', stopped: false }
  },
  computed: {
    tagTree() {
      const nodes = new Map(this.tags.map(tag => [tag.id, { ...tag, children: [] }]))
      const roots = []
      nodes.forEach(node => { if (nodes.has(node.parentId)) nodes.get(node.parentId).children.push(node); else roots.push(node) })
      return roots
    },
    activeView() {
      if (this.filters.inbox === true) return 'inbox'
      if (this.filters.recent) return 'recent'
      if (this.filters.tagIds.length === 1 && !this.filters.untagged) return 'tag:' + this.filters.tagIds[0]
      return 'all'
    },
    advancedCount() {
      return [this.filters.tagIds.length, this.filters.untagged, this.filters.type === 'webArticle' && this.filters.readFlag !== undefined && this.filters.readFlag !== null, this.filters.type === 'webArticle' && this.filters.starFlag !== undefined && this.filters.starFlag !== null, Array.isArray(this.dates) && this.dates.length ? 1 : 0].filter(Boolean).length
    },
    toolView() { return { ask: 'ToolAsk', files: 'ToolFiles', series: 'ToolSeries', organize: 'ToolOrganize', recovery: 'ToolRecovery' }[this.tool] },
    toolMeta() {
      const meta = { ask: { label: '资料问答', desc: '基于自己的资料检索回答，确认草稿后才写入笔记或待办' },
        files: { label: '个人文件', desc: '私有文件与目录管理，支持预览、下载与关联' },
        series: { label: '重复待办', desc: '周期任务的重复规则管理' },
        organize: { label: '整理建议', desc: '扫描资料给出整理建议，不会自动删除内容' },
        recovery: { label: '历史与回收站', desc: '笔记历史版本对比还原，已删除资料恢复' } }
      return meta[this.tool] || {}
    }
  },
  watch: { cards(value) { localStorage.setItem('resourceHubCards', value ? '1' : '0') } },
  mounted() { this.advancedOpen = this.advancedCount > 0; this.refreshTags(); this.search(1) },
  beforeUnmount() { this.sequence++; this.stopped = true },
  methods: {
    async toggleTool(key) {
      if (this.tool === key) { await this.closeTool(); return }
      await this.clearHistoryQuery(key !== 'recovery')
      this.tool = key
      this.scrollPanelIntoView()
    },
    async closeTool() {
      await this.clearHistoryQuery(true)
      this.tool = ''
    },
    async clearHistoryQuery(strip) {
      if (!strip || !('id' in this.$route.query)) return
      const query = { ...this.$route.query }; delete query.id
      try { await this.$router.replace({ query }) } catch { /* 同路由并发导航忽略 */ }
    },
    async openHistory(row) {
      try { await this.$router.replace({ query: { ...this.$route.query, id: String(row.id) } }) } catch { /* 同上 */ }
      this.tool = 'recovery'
      this.scrollPanelIntoView()
    },
    scrollPanelIntoView() { this.$nextTick(() => document.querySelector('.tool-panel')?.scrollIntoView({ behavior: 'smooth', block: 'start' })) },
    fmtTime(time) { const text = String(time || ''); return text.length > 16 ? text.slice(0, 16) : text },
    showAll() { if (this.tool) this.closeTool(); this.filters.tagIds = []; this.filters.inbox = undefined; this.filters.recent = false; this.filters.untagged = false; this.search(1) },
    showInbox() { if (this.tool) this.closeTool(); this.filters.inbox = true; this.filters.recent = false; this.search(1) },
    showRecent() { if (this.tool) this.closeTool(); this.filters.inbox = undefined; this.filters.recent = true; this.search(1) },
    onTypeChange() { if (this.filters.type === 'webArticle') this.advancedOpen = true; this.search(1) },
    resetFilters() {
      this.filters = { q: '', type: 'all', tagIds: [], tagMode: 'all', includeDescendants: true, untagged: false, recent: false, inbox: undefined }
      this.dates = []
      this.search(1)
    },
    async batchInbox() { const result = await updateBatchInbox(this.selected, !this.filters.inbox); const failed = result.data.filter(item => item.status !== 'success').length; this.$message.info(`已处理 ${result.data.length - failed} 项，失败 ${failed} 项`); await this.search(this.page) },
    async visit(row) { try { await visitResource(row) } catch { /* Navigation rechecks access. */ } },
    async inbox(row) { const adding = this.filters.inbox !== true; await setInbox(row, adding); if (!adding) await this.search(this.page); this.$message.success(adding ? '已放入收件箱' : '已移出收件箱') },
    async openConversion(row) {
      const source = (await getResource(row)).data
      this.conversionId = row.id
      this.conversion = { targetType: 'note', title: source.title || '', content: source.content || '', requestId: crypto.randomUUID ? crypto.randomUUID() : Date.now().toString(36) + '-' + Math.random().toString(36).slice(2) + '-' + Math.random().toString(36).slice(2) }
      this.conversionDialog = true
    },
    async convert() {
      if (this.saving) return
      this.saving = true
      try { const res = await convertArticle(this.conversionId, this.conversion); this.conversionDialog = false; this.$router.push(`/r/${res.data.type}/${res.data.id}`) }
      finally { this.saving = false }
    },
    key(row) { return row.type + ':' + row.id },
    toggle(row) { const rowKey = this.key(row); const index = this.selected.findIndex(item => this.key(item) === rowKey); if (index < 0) this.selected.push(row); else this.selected.splice(index, 1) },
    async refreshTags() { this.tags = (await listTags()).data },
    async search(page = 1) {
      const sequence = ++this.sequence; this.loading = true
      try {
        const response = await listResources({ ...this.filters, startDate: this.dates?.[0], endDate: this.dates?.[1], pageNum: page, pageSize: 20 })
        if (sequence !== this.sequence) return
        this.rows = response.data.rows; this.total = response.data.total; this.page = page; this.selected = []
      } finally { if (sequence === this.sequence) this.loading = false }
    },
    selectTheme(tag) { if (this.tool) this.closeTool(); this.filters.tagIds = [tag.id]; this.filters.untagged = false; this.filters.inbox = undefined; this.filters.recent = false; this.search(1) },
    editTag(tag) { this.tagForm = tag ? { ...tag } : { name: '', parentId: 0, color: '#409EFF' }; this.mergeTarget = null; this.tagDialog = true },
    async save() { this.saving = true; try { await saveTag(this.tagForm); this.tagDialog = false; await this.refreshTags(); await this.search(this.page) } finally { this.saving = false } },
    async remove() { await this.$confirm('删除此标签及其关联，资料内容会保留，子标签会移到上一级。', '删除标签'); await deleteTag(this.tagForm.id); this.filters.tagIds = []; this.tagDialog = false; await this.refreshTags(); await this.search(1) },
    async merge() { await this.$confirm('将关联资料合并到目标标签，源标签会删除。', '合并标签'); await mergeTags(this.tagForm.id, this.mergeTarget); this.filters.tagIds = []; this.tagDialog = false; await this.refreshTags(); await this.search(1) },
    async openTags(row) { this.selectedTags = (await getResourceTags(row)).data; this.editingRow = row; this.batchResult = ''; this.relationDialog = true },
    openBatch() { this.editingRow = null; this.selectedTags = []; this.batchResult = ''; this.relationDialog = true },
    async applyTags() {
      this.saving = true
      try {
        if (this.editingRow) { await setResourceTags(this.editingRow, this.selectedTags); this.relationDialog = false }
        else { const res = await batchTags(this.selected, this.selectedTags); const success = res.data.filter(row => row.status === 'success').length; this.batchResult = `成功 ${success} 条，失败 ${res.data.length - success} 条。共享但不属于自己的资料不能修改私人标签。` }
        await this.search(this.page)
      } finally { this.saving = false }
    },
    async importLegacy() {
      if (this.importing) return
      this.importing = true; let processed = 0
      try {
        for (const type of ['todo', 'webArticle']) {
          let done = false
          while (!done && !this.stopped) { const res = await backfillTags(type); processed += res.data.processed; done = res.data.done; this.importStatus = `本次已处理 ${processed} 条资料` }
        }
        await this.refreshTags(); await this.search(1)
      } finally { this.importing = false }
    }
  }
}
</script>
<style scoped>
.workspace { padding: 20px 24px 32px; }

/* ---------- 页头 ---------- */
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; flex-wrap: wrap; margin-bottom: 16px; }
.page-head h1 { margin: 0; font-size: 22px; font-weight: 700; letter-spacing: 0.5px; color: var(--text-primary); }
.page-head p { margin: 4px 0 0; color: var(--text-secondary); }
.page-head-link { display: inline-flex; align-items: center; gap: 6px; color: var(--text-secondary); font-size: 14px; text-decoration: none; padding: 6px 12px; border-radius: var(--radius-full, 999px); border: 1px solid var(--border-primary); background: var(--bg-card); transition: color .2s, border-color .2s; }
.page-head-link:hover { color: var(--pnkx-primary); border-color: var(--pnkx-primary); }

/* ---------- 布局 ---------- */
.workspace-body { display: flex; gap: 16px; align-items: flex-start; }
.side { width: 240px; flex-shrink: 0; display: flex; flex-direction: column; gap: 12px; position: sticky; top: 96px; max-height: calc(100vh - 116px); overflow-y: auto; scrollbar-width: none; }
.side::-webkit-scrollbar { display: none; }
.content { flex: 1; min-width: 0; }
.side-group { background: var(--bg-card); border: 1px solid var(--border-primary); border-radius: var(--radius-lg); padding: 10px; }
.side-title { display: flex; align-items: center; justify-content: space-between; margin: 0; padding: 4px 12px 8px; font-size: 12px; font-weight: 600; letter-spacing: 2px; color: var(--text-muted); text-transform: uppercase; }
.side-link { display: flex; align-items: center; gap: 10px; width: 100%; padding: 8px 12px; border: none; border-radius: var(--radius-md); background: transparent; color: var(--text-primary); font-size: 14px; text-decoration: none; cursor: pointer; text-align: left; transition: background .15s, color .15s; }
.side-link:hover { background: var(--bg-hover); color: var(--pnkx-primary); }
.side-link .el-icon { color: var(--text-secondary); }
.side-link:hover .el-icon { color: var(--pnkx-primary); }
.side-link.active { background: var(--pnkx-primary-soft); color: var(--pnkx-primary); font-weight: 600; }
.side-link.active .el-icon { color: var(--pnkx-primary); }
.side-link:focus-visible { outline: 2px solid var(--pnkx-primary); outline-offset: 1px; }
.quick-view { display: flex; align-items: center; gap: 10px; width: 100%; padding: 8px 12px; border: none; border-radius: var(--radius-md); background: transparent; color: var(--text-primary); font-size: 14px; cursor: pointer; text-align: left; transition: background .15s, color .15s; }
.quick-view .el-icon { color: var(--text-secondary); }
.quick-view:hover { background: var(--bg-hover); }
.quick-view.active { background: var(--pnkx-primary-soft); color: var(--pnkx-primary); font-weight: 600; }
.quick-view.active .el-icon { color: var(--pnkx-primary); }
.quick-view:focus-visible, .view-btn:focus-visible { outline: 2px solid var(--pnkx-primary); outline-offset: 1px; }

/* 标签树 */
.side-tags { flex: 1; display: flex; flex-direction: column; }
.tag-new { padding: 2px 6px; }
.tag-tree { background: transparent; --el-tree-node-hover-bg-color: var(--bg-hover); border-radius: var(--radius-md); margin: 0 2px; }
.tag-tree.capped { max-height: max(120px, min(320px, calc(100vh - 560px))); overflow: auto; scrollbar-width: thin; }
.tag-tree :deep(.el-tree-node__content) { border-radius: var(--radius-md); height: 32px; margin-bottom: 1px; }
.tag-node { flex: 1; min-width: 0; display: flex; align-items: center; gap: 8px; padding-right: 4px; border-radius: var(--radius-md); }
.tag-node.active .tag-name { color: var(--pnkx-primary); font-weight: 600; }
.tag-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.tag-name { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; color: var(--text-primary); }
.tag-manage { opacity: 0; padding: 2px 6px; transition: opacity .15s; }
.tag-tree :deep(.el-tree-node__content:hover) .tag-manage, .tag-manage:focus-visible { opacity: 1; }
.side-foot { display: flex; flex-direction: column; gap: 2px; padding: 2px 12px 6px; }
.legacy-btn { color: var(--text-muted); font-size: 13px; padding: 4px 0; justify-content: flex-start; }
.legacy-btn:hover { color: var(--pnkx-primary); background: transparent; }
.side-foot p { margin: 0; font-size: 12px; color: var(--text-muted); }

/* ---------- 筛选卡片 ---------- */
.filter-card { background: var(--bg-card); border: 1px solid var(--border-primary); border-radius: var(--radius-lg); padding: 14px 16px; }
.filter-main { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.filter-q { width: 300px; max-width: 100%; }
.filter-type { width: 110px; }
.advanced-toggle .el-icon { margin-left: 2px; }
.advanced-arrow { transition: transform .2s; }
.advanced-arrow.open { transform: rotate(180deg); }
.advanced-count { display: inline-flex; align-items: center; justify-content: center; min-width: 18px; height: 18px; margin-left: 4px; padding: 0 5px; border-radius: 9px; background: var(--pnkx-primary); color: #fff; font-size: 12px; line-height: 1; }
.filter-advanced { margin-top: 12px; padding-top: 12px; border-top: 1px dashed var(--border-primary); display: flex; flex-direction: column; gap: 10px; }
.filter-advanced-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.filter-tags { min-width: 220px; }
.tag-option { display: inline-flex; align-items: center; gap: 8px; }

/* ---------- 工具栏 ---------- */
.toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; margin: 16px 0 12px; }
.toolbar-count { color: var(--text-secondary); font-size: 13px; }
.toolbar-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.view-switch { display: inline-flex; padding: 3px; gap: 2px; background: var(--bg-hover); border-radius: var(--radius-md); }
.view-btn { display: inline-flex; align-items: center; justify-content: center; width: 28px; height: 24px; border: none; border-radius: 5px; background: transparent; color: var(--text-secondary); cursor: pointer; transition: background .15s, color .15s; }
.view-btn:hover { color: var(--pnkx-primary); }
.view-btn.active { background: var(--bg-card); color: var(--pnkx-primary); }

/* ---------- 资料列表 ---------- */
.resources { display: grid; gap: 10px; margin-bottom: 16px; }
.resources.cards { grid-template-columns: repeat(auto-fill, minmax(290px, 1fr)); }
.resource { background: var(--bg-card); border: 1px solid var(--border-primary); border-radius: var(--radius-lg); padding: 12px 16px 8px; transition: border-color .2s; }
.resource:hover { border-color: var(--pnkx-primary); }
.resources.cards .resource:hover { border-color: var(--pnkx-primary); }
.resource-head { display: flex; align-items: center; gap: 10px; min-width: 0; }
.type-chip { display: inline-flex; align-items: center; gap: 4px; flex-shrink: 0; padding: 2px 8px; border-radius: var(--radius-full, 999px); font-size: 12px; font-weight: 600; line-height: 18px; }
.type-chip .el-icon { font-size: 12px; }
.t-file { color: var(--color-info); background: var(--color-info-light); }
.t-note { color: var(--color-warning); background: var(--color-warning-light); }
.t-webArticle { color: var(--color-success); background: var(--color-success-light); }
.t-todo { color: var(--pnkx-primary); background: var(--pnkx-primary-soft); }
.title { flex: 1; min-width: 0; font-weight: 600; font-size: 15px; color: var(--text-primary); text-decoration: none; overflow-wrap: anywhere; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.title:hover { color: var(--pnkx-primary); }
.time { flex-shrink: 0; color: var(--text-muted); font-size: 12px; }
.subtitle { margin: 8px 0 0 30px; color: var(--text-secondary); font-size: 13px; line-height: 1.6; overflow-wrap: anywhere; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; white-space: pre-line; }
.resource-tags { display: flex; flex-wrap: wrap; gap: 6px; margin: 8px 0 0 30px; }
.resource-foot { display: flex; align-items: center; gap: 4px; flex-wrap: wrap; margin: 2px 0 0 22px; }
.foot-space { flex: 1; }
.foot-link { display: inline-flex; align-items: center; gap: 4px; padding: 5px 8px; border-radius: var(--radius-md); color: var(--text-secondary); font-size: 12px; text-decoration: none; }
.foot-link:hover { color: var(--pnkx-primary); background: var(--pnkx-primary-soft); }
.resource-foot .el-button + .el-button { margin-left: 0; }
.resources.cards .subtitle, .resources.cards .resource-tags, .resources.cards .resource-foot { margin-left: 0; }

/* ---------- 内嵌工具面板 ---------- */
.tool-panel { background: var(--bg-card); border: 1px solid var(--border-primary); border-radius: var(--radius-lg); overflow: hidden; }
.tool-panel-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; flex-wrap: wrap; padding: 14px 20px 12px; border-bottom: 1px solid var(--border-secondary); }
.tool-panel-title h2 { margin: 0; font-size: 17px; font-weight: 700; color: var(--text-primary); }
.tool-panel-title p { margin: 4px 0 0; color: var(--text-secondary); font-size: 13px; }
.tool-panel :deep(.qa-page), .tool-panel :deep(.files), .tool-panel :deep(.series), .tool-panel :deep(.organize), .tool-panel :deep(.recovery) { padding: 16px 20px 20px; max-width: none; margin: 0; }

/* ---------- 分页 & 弹窗 ---------- */
.pagination-row { display: flex; justify-content: flex-end; }
.dialog-tip { margin-top: 0; color: var(--text-secondary); }
.dialog-form { display: grid; gap: 12px; }
.merge-row { display: flex; gap: 8px; }

@media (max-width: 900px) {
  .workspace { padding: 12px; }
  .workspace-body { flex-direction: column; }
  .side { width: 100%; position: static; max-height: none; flex-direction: row; flex-wrap: wrap; }
  .side-group { flex: 1 1 240px; }
  .side-tags { flex: 2 1 320px; }
  .side-foot { flex-basis: 100%; }
  .filter-q { width: 100%; }
}
</style>
