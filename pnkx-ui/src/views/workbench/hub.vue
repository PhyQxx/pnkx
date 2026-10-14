<template>
  <div class="hub">
    <!-- ============ 左栏：视图 / 类型 / 标签树 / 整理建议 ============ -->
    <aside class="hub__left">
      <div class="hub-card">
        <div class="hub-gt">视图</div>
        <button v-for="v in views" :key="v.key" class="hub-li" :class="{ on: view === v.key }" @click="setView(v.key)">
          <svg-icon :icon-class="v.icon" class-name="h-ic15" />{{ v.label }}
          <span v-if="counts[v.key] != null" class="hub-cnt">{{ counts[v.key] }}</span>
        </button>
        <button class="hub-li" @click="go('/resources/recovery')"><svg-icon icon-class="clear" class-name="h-ic15" />回收站</button>
      </div>

      <div class="hub-card">
        <div class="hub-gt">类型</div>
        <button v-for="t in types" :key="t.value" class="hub-li" :class="{ on: query.type === t.value }" @click="setType(t.value)">
          <svg-icon :icon-class="t.icon" class-name="h-ic15" />{{ t.label }}
        </button>
      </div>

      <div class="hub-card">
        <div class="hub-gt">标签</div>
        <template v-for="tag in tagTree" :key="tag.id">
          <button class="hub-li" :class="{ on: isTagOn(tag.id) }" @click="toggleTag(tag.id)">
            <svg-icon icon-class="dict" class-name="h-ic15" />{{ tag.name }}
          </button>
          <button v-for="child in tag.children" :key="'c' + child.id" class="hub-li hub-li--sub" :class="{ on: isTagOn(child.id) }" @click="toggleTag(child.id)">
            <svg-icon icon-class="dict" class-name="h-ic13" />{{ child.name }}
          </button>
        </template>
        <div v-if="!tagTree.length" class="hub-empty-mini">暂无标签，在筛选或移动端创建</div>
      </div>

      <div class="hub-card hub-card--cta" @click="go('/resources/organize')">
        <div class="hub-gt">整理建议</div>
        <div class="hub-cta-row">
          <span class="hub-cta-ic"><svg-icon icon-class="build" class-name="h-ic14" /></span>
          <div><b>智能整理</b><i>重复合并 · 补标题 · 打标签</i></div>
        </div>
      </div>
    </aside>

    <!-- ============ 中栏：搜索 + 列表 ============ -->
    <section class="hub__mid">
      <div class="hub-card hub-listcard">
        <div class="hub-tools">
          <div class="hub-search">
            <svg-icon icon-class="search" class-name="h-ic14" />
            <input v-model="query.q" placeholder="搜索资料正文…" @input="debouncedSearch" />
          </div>
          <button v-for="c in quickChips" :key="c.value" class="hub-chip" :class="{ on: query.type === c.value }" @click="setType(c.value)">{{ c.label }}</button>
          <button class="hub-chip hub-chip--new" @click="newNote"><svg-icon icon-class="加号" class-name="h-ic12" />新建笔记</button>
        </div>

        <!-- 骨架 -->
        <div v-if="loading && !rows.length" class="hub-skel">
          <div v-for="n in 5" :key="n" class="hub-skel__row">
            <div class="hub-skel__badge" />
            <div class="hub-skel__lines"><div class="hub-skel__line" style="width: 62%" /><div class="hub-skel__line" style="width: 88%" /><div class="hub-skel__line hub-skel__line--thin" style="width: 30%" /></div>
          </div>
        </div>

        <!-- 空态 -->
        <div v-else-if="!rows.length" class="hub-bigempty">
          <span class="hub-bigempty__glyph">🗂</span>
          <b>没有符合条件的资料</b>
          <i>换个类型或标签试试，或先去「整理建议」处理待清理项</i>
        </div>

        <!-- 列表 -->
        <div v-else ref="listEl" class="hub-rows" @scroll="onScroll">
          <div
            v-for="(row, i) in rows" :key="row.type + ':' + row.id"
            class="hub-row" :class="{ on: selected && selected.type === row.type && selected.id === row.id }"
            :style="{ animationDelay: Math.min(i, 6) * 0.03 + 's' }"
            @click="select(row)"
          >
            <span class="hub-badge" :class="'hub-badge--' + row.type">{{ glyph(row.type) }}</span>
            <div class="hub-row__main">
              <div class="hub-row__title">
                <b>{{ row.title || '（无标题）' }}</b>
                <button v-if="row.type === 'webArticle'" class="hub-convert" @click.stop="openConvert(row)"><svg-icon icon-class="edit" class-name="h-ic12" />转笔记</button>
              </div>
              <div class="hub-row__sn">{{ row.subtitle }}</div>
              <div class="hub-row__meta">
                <span v-for="t in (row.tags || []).slice(0, 3)" :key="t" class="hub-minitag">{{ t }}</span>
                <span>·</span><span>{{ typeLabel(row.type) }}</span>
                <span>·</span><span>{{ row.updatedAt }}</span>
              </div>
            </div>
          </div>
          <div class="hub-foot">
            <span>显示 {{ rows.length }} / {{ total }} 条</span>
            <span>标签统一 · 双链引用 · 版本历史 · 回收站</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ 右栏：详情 ============ -->
    <aside class="hub-card hub__right">
      <template v-if="detail">
        <div :key="detail.type + detail.id" class="hub-detail">
          <div class="hub-dtype"><svg-icon :icon-class="typeIcon(detail.type)" class-name="h-ic11" />{{ typeLabel(detail.type) }}<em v-if="detail.inbox"> · 收件箱</em></div>
          <h2 class="hub-dtitle">{{ detail.title || '（无标题）' }}</h2>
          <div class="hub-dmeta">{{ detail.updatedAt || '' }} · 归属本人<template v-if="detail.revision != null"> · revision {{ detail.revision }}</template></div>
          <div class="hub-dprev">{{ previewText }}</div>

          <div v-if="(detail.tags || []).length" class="hub-dsec">标签</div>
          <div v-if="(detail.tags || []).length" class="hub-dtags">
            <span v-for="t in detail.tags" :key="t" class="hub-dtag"><svg-icon icon-class="dict" class-name="h-ic11" />{{ t }}</span>
          </div>

          <div v-if="links.length" class="hub-dsec">引用</div>
          <div v-for="l in links" :key="'l' + l.targetType + l.targetId" class="hub-linkrow" @click="openRef(l.targetType, l.targetId)">
            <svg-icon icon-class="link" class-name="h-ic13" />
            <span class="hub-linkrow__t">{{ l.title || typeLabel(l.targetType) }}<small>引用</small></span>
            <svg-icon icon-class="右箭头" class-name="h-ic13 hub-linkrow__arr" />
          </div>

          <div v-if="backlinks.length" class="hub-dsec">反向链接</div>
          <div v-for="b in backlinks" :key="'b' + b.sourceType + b.sourceId" class="hub-linkrow" @click="openRef(b.sourceType, b.sourceId)">
            <svg-icon icon-class="back" class-name="h-ic13" />
            <span class="hub-linkrow__t">{{ b.title || typeLabel(b.sourceType) }}<small>被引用于</small></span>
            <svg-icon icon-class="右箭头" class-name="h-ic13 hub-linkrow__arr" />
          </div>

          <div v-if="detail.type === 'note' && history.length" class="hub-dsec">版本历史 · 最近 {{ history.length }} 版</div>
          <div v-for="(v, i) in history" :key="i" class="hub-verrow" @click="viewVersion(v)">
            <span class="hub-verrow__dot" :class="{ cur: i === 0 }" />
            <span>{{ v.title ? v.title.slice(0, 18) : '历史版本 v' + v.version }}</span>
            <span class="hub-verrow__time">{{ v.updatedAt || ('v' + v.version) }}</span>
          </div>

          <div class="hub-dacts">
            <button v-if="detail.type === 'webArticle'" class="hub-abtn hub-abtn--pri" @click="openConvert(detail)"><svg-icon icon-class="edit" class-name="h-ic14" />转为笔记</button>
            <button v-else class="hub-abtn hub-abtn--pri" @click="openRef(detail.type, detail.id)"><svg-icon icon-class="edit" class-name="h-ic14" />编辑</button>
            <button class="hub-abtn hub-abtn--ghost" @click="openConvert(detail, 'todo')"><svg-icon icon-class="todo" class-name="h-ic14" />转待办</button>
          </div>
        </div>
      </template>
      <div v-else class="hub-nosel">
        <span class="hub-bigempty__glyph">←</span>
        <b>选择一条资料查看详情</b>
        <i>引用、反向链接与版本历史将在此展示</i>
      </div>
    </aside>

    <!-- 转换弹窗（剪藏 → 笔记/待办） -->
    <el-dialog v-model="convertDialog" title="检查内容后创建" width="520px" append-to-body>
      <el-form label-width="72px">
        <el-form-item label="创建为">
          <el-radio-group v-model="conversion.targetType">
            <el-radio-button value="note">笔记</el-radio-button>
            <el-radio-button value="todo">待办</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="conversion.title" maxlength="200" placeholder="标题" /></el-form-item>
        <el-form-item label="正文">
          <el-input v-model="conversion.content" type="textarea" :rows="7" maxlength="100000" placeholder="正文；待办说明最多500字" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="convertDialog = false">取消</el-button>
        <el-button type="primary" :loading="converting" @click="convert">确认创建并关联来源</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
/**
 * 工作台·资料三栏工作台（PRODUCT_UIUE_REDESIGN §4.3，对齐高保真原型 docs/prototype/pnkx-pc-proto.html）
 * 左：视图/类型/标签树/整理建议；中：搜索+资源列表（滚动加载）；右：详情（双链/版本历史/转笔记）。
 */
import { listResources, listTags, getResource, getLinks, getBacklinks, noteHistory, convertArticle, visitResource } from '@/api/workspace'

const TYPE_META = { note: { label: '笔记', glyph: '笔', icon: 'edit' }, webArticle: { label: '剪藏', glyph: '剪', icon: 'dict' }, file: { label: '文件', glyph: '文', icon: 'file' }, todo: { label: '待办', glyph: '办', icon: 'todo' } }

export default {
  name: 'WorkbenchHub',
  data() {
    return {
      query: { q: '', type: 'all', tagIds: [], tagMode: 'all', includeDescendants: true },
      view: 'inbox',
      types: [
        { value: 'note', label: '笔记', icon: 'edit' },
        { value: 'webArticle', label: '剪藏', icon: 'dict' },
        { value: 'file', label: '文件', icon: 'file' },
        { value: 'todo', label: '待办', icon: 'todo' }
      ],
      quickChips: [
        { value: 'all', label: '全部' },
        { value: 'webArticle', label: '剪藏' },
        { value: 'note', label: '笔记' }
      ],
      tags: [],
      rows: [],
      total: 0,
      page: 1,
      hasMore: false,
      loading: false,
      seq: 0,
      timer: null,
      counts: {},
      selected: null,
      detail: null,
      links: [],
      backlinks: [],
      history: [],
      convertDialog: false,
      converting: false,
      conversion: {},
      convertRow: null
    }
  },
  computed: {
    views() {
      return [
        { key: 'inbox', label: '收件箱', icon: 'message' },
        { key: 'all', label: '全部资料', icon: 'list' },
        { key: 'read', label: '继续阅读', icon: 'education' }
      ]
    },
    tagTree() {
      const byParent = {}
      for (const t of this.tags) {
        const pid = t.parentId || 0
        ;(byParent[pid] = byParent[pid] || []).push(t)
      }
      const roots = byParent[0] || []
      for (const r of roots) r.children = byParent[r.id] || []
      return roots
    },
    previewText() {
      const c = this.detail?.content || this.detail?.richText || ''
      const text = String(c).replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
      return text ? (text.length > 260 ? text.slice(0, 260) + '……' : text) : '（无正文预览）'
    }
  },
  mounted() {
    this.refreshTags()
    this.search(1)
  },
  methods: {
    glyph(type) { return TYPE_META[type]?.glyph || '资' },
    typeLabel(type) { return TYPE_META[type]?.label || type },
    typeIcon(type) { return TYPE_META[type]?.icon || 'document' },
    isTagOn(id) { return this.query.tagIds.includes(id) },
    go(path) { this.$router.push(path).catch(() => {}) },
    newNote() {
      const to = this.resolveMenu('我的笔记')
      if (to) this.go(to)
      else this.$message?.info('未找到笔记页面菜单')
    },
    resolveMenu(title) {
      const routes = this.$store.state.permission.sidebarRouters || []
      const walk = (list, base) => {
        for (const r of list) {
          if (r.hidden) continue
          const path = r.path?.startsWith('/') ? r.path : base + '/' + (r.path || '')
          if (r.meta?.title === title && r.component) return '/workbench' + path
          const child = r.children ? walk(r.children, path) : null
          if (child) return child
        }
        return null
      }
      return walk(routes, '')
    },
    async refreshTags() {
      try { this.tags = (await listTags()).data || [] } catch (e) { this.tags = [] }
    },
    setView(v) {
      this.view = v
      if (v !== 'all') this.query.type = 'all'
      this.search(1)
    },
    setType(t) {
      this.query.type = t
      if (t !== 'all') this.view = 'all'
      this.search(1)
    },
    toggleTag(id) {
      const i = this.query.tagIds.indexOf(id)
      if (i < 0) this.query.tagIds = [...this.query.tagIds, id]
      else this.query.tagIds = this.query.tagIds.filter(x => x !== id)
      this.view = 'all'
      this.search(1)
    },
    debouncedSearch() {
      clearTimeout(this.timer)
      this.timer = setTimeout(() => this.search(1), 300)
    },
    viewParams() {
      const p = { ...this.query, pageNum: 1, pageSize: 20 }
      if (this.view === 'inbox') p.inbox = true
      else if (this.view === 'read') p.recent = true
      return p
    },
    async search(page = 1, append = false) {
      const seq = ++this.seq
      this.loading = true
      try {
        const res = await listResources({ ...this.viewParams(), pageNum: page, pageSize: 20 })
        if (seq !== this.seq) return
        const d = res.data || {}
        this.rows = append ? this.rows.concat(d.rows || []) : (d.rows || [])
        this.total = d.total || 0
        this.page = page
        this.hasMore = !!d.hasMore
        this.counts[this.view] = this.total
        if (!append) {
          if (this.rows.length) this.select(this.rows[0])
          else { this.selected = null; this.detail = null; this.links = []; this.backlinks = []; this.history = [] }
        }
      } finally {
        if (seq === this.seq) this.loading = false
      }
    },
    onScroll(e) {
      const el = e.target
      if (this.hasMore && !this.loading && el.scrollTop + el.clientHeight >= el.scrollHeight - 40) {
        this.search(this.page + 1, true)
      }
    },
    async select(row) {
      this.selected = row
      this.detail = null
      this.links = []
      this.backlinks = []
      this.history = []
      try {
        const [d, l, b] = await Promise.all([
          getResource(row),
          getLinks(row).catch(() => ({ data: [] })),
          getBacklinks(row).catch(() => ({ data: [] }))
        ])
        if (this.selected !== row) return
        this.detail = d.data || {}
        this.links = (l.data || []).filter(x => !x.unavailable)
        this.backlinks = (b.data || []).filter(x => !x.unavailable)
        visitResource(row).catch(() => {})
        if (row.type === 'note') {
          noteHistory(row.id).then(h => { if (this.selected === row) this.history = (h.data || []).slice(0, 5) }).catch(() => {})
        }
      } catch (e) {
        this.detail = { ...row }
      }
    },
    openRef(type, id) {
      this.go(`/workbench/r/${type}/${id}`)
    },
    viewVersion(v) {
      if (v && v.noteId) this.go(`/workbench/r/note/${v.noteId}`)
    },
    openConvert(row, preset) {
      this.convertRow = row
      getResource(row).then(res => {
        const source = res.data || {}
        this.conversion = {
          targetType: preset || 'note',
          title: source.title || '',
          content: source.content || '',
          requestId: (crypto.randomUUID ? crypto.randomUUID() : Date.now().toString(36) + '-' + Math.random().toString(36).slice(2))
        }
        this.convertDialog = true
      })
    },
    async convert() {
      if (this.converting) return
      this.converting = true
      try {
        const res = await convertArticle(this.convertRow.id, this.conversion)
        this.convertDialog = false
        const created = res.data
        if (created) this.openRef(created.type || 'note', created.id)
        this.search(1)
      } finally {
        this.converting = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/design-tokens.scss' as t;

.hub { position: absolute; inset: 0; display: flex; gap: 12px; padding: 14px 16px 16px; overflow: hidden; }
.hub__left { width: 216px; flex: none; display: flex; flex-direction: column; gap: 10px; overflow-y: auto; scrollbar-width: thin; }
.hub__mid { flex: 1; min-width: 0; display: flex; }
.hub__right { width: 308px; flex: none; overflow-y: auto; scrollbar-width: thin; padding: 16px 16px 14px; }

.h-ic11 { font-size: 11px; }
.h-ic12 { font-size: 12px; }
.h-ic13 { font-size: 13px; }
.h-ic14 { font-size: 14px; }
.h-ic15 { font-size: 15px; }
.hub-card {
  background: var(--wb-card);
  border: 1px solid var(--wb-border);
  border-radius: 15px;
  backdrop-filter: blur(20px);
  box-shadow: t.$shadow-sm;
  padding: 10px 8px;
}
.hub__mid .h-ic11 { font-size: 11px; }
.h-ic12 { font-size: 12px; }
.h-ic13 { font-size: 13px; }
.h-ic14 { font-size: 14px; }
.h-ic15 { font-size: 15px; }
.hub-card { flex: 1; display: flex; flex-direction: column; overflow: hidden; padding: 10px; min-height: 0; }
.hub-gt { font-size: 10.5px; font-weight: 700; color: var(--wb-text-3); letter-spacing: 0.5px; padding: 2px 8px 6px; }

/* 左栏列表项 */
.hub-li {
  position: relative; display: flex; align-items: center; gap: 9px; width: 100%;
  height: 34px; padding: 0 9px; border: none; border-radius: 9px;
  background: transparent; color: var(--wb-text-2); font-size: 12.5px; font-weight: 600;
  cursor: pointer; white-space: nowrap; text-align: left; font-family: inherit;
  transition: background 0.14s ease, color 0.14s ease;
  &:hover { background: var(--wb-hover); color: var(--wb-text); }
  &.on { background: rgba(79, 134, 247, 0.12); color: var(--wb-accent); }
  &--sub { height: 30px; font-size: 12px; font-weight: 500; padding-left: 26px; }
}
.hub-cnt { margin-left: auto; font-size: 10.5px; color: var(--wb-text-3); font-variant-numeric: tabular-nums; }
.hub-empty-mini { padding: 10px 8px; font-size: 11px; color: var(--wb-text-3); }
.hub-card--cta { cursor: pointer; transition: transform 0.16s t.$ease-out, box-shadow 0.18s ease;
  &:hover { transform: translateY(-2px); box-shadow: t.$shadow-lg; } }
.hub-cta-row { display: flex; align-items: center; gap: 10px; padding: 4px 6px; b { font-size: 12.5px; display: block; } i { font-style: normal; font-size: 10.5px; color: var(--wb-text-3); } }
.hub-cta-ic { width: 30px; height: 30px; border-radius: 9px; background: rgba(245, 158, 11, 0.14); color: #b45309; display: flex; align-items: center; justify-content: center; }

/* 工具行 */
.hub-tools { display: flex; align-items: center; gap: 8px; padding-bottom: 10px; }
.hub-search {
  flex: 1; display: flex; align-items: center; gap: 8px; height: 36px; padding: 0 12px;
  border-radius: 11px; background: var(--wb-card); border: 1px solid var(--wb-border); color: var(--wb-text-3);
  input { flex: 1; border: none; outline: none; background: transparent; font-size: 12.5px; color: var(--wb-text); font-family: inherit; min-width: 0; }
}
.hub-chip {
  display: flex; align-items: center; gap: 5px; padding: 7px 12px; border-radius: 100px;
  border: 1px solid var(--wb-border); background: var(--wb-card);
  font-size: 11.5px; font-weight: 600; color: var(--wb-text-2); cursor: pointer; white-space: nowrap; font-family: inherit;
  transition: background 0.15s ease, color 0.15s ease, transform 0.12s t.$ease-out;
  &.on { background: var(--wb-accent); border-color: var(--wb-accent); color: #fff; }
  &:active { transform: scale(0.94); }
  &--new { color: var(--wb-accent); }
}

/* 骨架 */
.hub-skel { display: flex; flex-direction: column; gap: 10px; padding: 4px; }
.hub-skel__row { display: flex; gap: 11px; padding: 12px 13px; border-radius: 13px; }
.hub-skel__badge { width: 34px; height: 34px; border-radius: 11px; flex: none; background: var(--wb-hover); position: relative; overflow: hidden;
  &::after { content: ''; position: absolute; inset: 0; transform: translateX(-100%); background: linear-gradient(90deg, transparent, var(--wb-card), transparent); animation: hub-shimmer 1.2s ease-in-out infinite; } }
.hub-skel__lines { flex: 1; display: flex; flex-direction: column; gap: 8px; justify-content: center; }
.hub-skel__line { height: 12px; border-radius: 6px; background: var(--wb-hover); position: relative; overflow: hidden;
  &--thin { height: 9px; }
  &::after { content: ''; position: absolute; inset: 0; transform: translateX(-100%); background: linear-gradient(90deg, transparent, var(--wb-card), transparent); animation: hub-shimmer 1.2s ease-in-out infinite; } }
@keyframes hub-shimmer { to { transform: translateX(100%); } }

/* 空态 / 未选择 */
.hub-bigempty, .hub-nosel { display: flex; flex-direction: column; align-items: center; justify-content: center; flex: 1; gap: 6px; padding: 40px 20px; text-align: center;
  &__glyph { font-size: 40px; }
  b { font-size: 13.5px; color: var(--wb-text-2); }
  i { font-style: normal; font-size: 11.5px; color: var(--wb-text-3); } }
.hub-nosel .hub-bigempty__glyph { animation: hub-nudge 2.4s ease-in-out infinite; }
@keyframes hub-nudge { 0%, 100% { transform: translateX(0); } 50% { transform: translateX(-6px); } }

/* 行 */
.hub-rows { flex: 1; overflow-y: auto; scrollbar-width: thin; }
.hub-row {
  position: relative; display: flex; gap: 11px; align-items: flex-start;
  padding: 12px 13px; border-radius: 13px; cursor: pointer; transition: background 0.14s ease;
  animation: hub-rise 0.28s t.$ease-out both;
  &:hover { background: var(--wb-hover); }
  &.on { background: rgba(79, 134, 247, 0.1); }
  &.on::before { content: ''; position: absolute; left: 0; top: 12px; bottom: 12px; width: 3px; border-radius: 3px; background: var(--wb-accent); }
}
@keyframes hub-rise { from { opacity: 0; transform: translateY(9px); } to { opacity: 1; transform: none; } }
.hub-badge {
  width: 34px; height: 34px; border-radius: 11px; flex: none;
  display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 700;
  &--note { background: rgba(139, 124, 246, 0.14); color: #7c6af0; }
  &--webArticle { background: rgba(79, 134, 247, 0.13); color: var(--wb-accent); }
  &--file { background: rgba(33, 186, 115, 0.13); color: #0f9d63; }
  &--todo { background: rgba(245, 158, 11, 0.14); color: #b45309; }
}
.hub-row__main { flex: 1; min-width: 0; }
.hub-row__title { display: flex; align-items: center; gap: 8px; b { font-size: 13px; color: var(--wb-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; } }
.hub-convert {
  flex: none; display: flex; align-items: center; gap: 4px; border: none; cursor: pointer; font-family: inherit;
  padding: 3px 9px; border-radius: 8px; font-size: 10.5px; font-weight: 700; color: var(--wb-accent); background: rgba(79, 134, 247, 0.12);
  transition: background 0.15s ease;
  &:hover { background: rgba(79, 134, 247, 0.2); }
}
.hub-row__sn { font-size: 11.5px; color: var(--wb-text-2); margin-top: 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.hub-row__meta { display: flex; align-items: center; gap: 6px; margin-top: 6px; font-size: 10.5px; color: var(--wb-text-3); flex-wrap: wrap; }
.hub-minitag { font-size: 10px; padding: 1px 7px; border-radius: 100px; background: var(--wb-hover); color: var(--wb-text-2); font-weight: 600; }
.hub-foot { display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; font-size: 11px; color: var(--wb-text-3); }

/* 详情 */
.hub-detail { animation: hub-swap 0.24s t.$ease-out both; display: flex; flex-direction: column; }
@keyframes hub-swap { from { opacity: 0; transform: translateX(10px); } to { opacity: 1; transform: none; } }
.hub-dtype {
  display: inline-flex; align-items: center; gap: 5px; align-self: flex-start;
  font-size: 10.5px; font-weight: 700; padding: 3px 9px; border-radius: 100px;
  background: rgba(79, 134, 247, 0.12); color: var(--wb-accent);
  em { font-style: normal; opacity: 0.8; }
}
.hub-dtitle { font-size: 16px; font-weight: 700; line-height: 1.45; margin: 10px 0 4px; color: var(--wb-text); overflow-wrap: anywhere; }
.hub-dmeta { font-size: 11px; color: var(--wb-text-3); }
.hub-dprev { font-size: 12.5px; line-height: 1.75; color: var(--wb-text-2); margin-top: 12px; padding: 12px 13px; background: var(--wb-hover); border-radius: 11px; }
.hub-dsec { font-size: 11px; font-weight: 700; color: var(--wb-text-3); letter-spacing: 0.4px; margin: 16px 0 8px; }
.hub-dtags { display: flex; flex-wrap: wrap; gap: 6px; }
.hub-dtag { display: flex; align-items: center; gap: 4px; font-size: 11px; font-weight: 600; color: var(--wb-accent); background: rgba(79, 134, 247, 0.12); padding: 4px 10px; border-radius: 100px; }
.hub-linkrow {
  display: flex; align-items: center; gap: 8px; padding: 8px 10px; border-radius: 10px; cursor: pointer;
  font-size: 12px; font-weight: 600; color: var(--wb-text); transition: background 0.14s ease;
  &:hover { background: var(--wb-hover); }
  small { display: block; font-size: 10px; color: var(--wb-text-3); font-weight: 500; }
}
.hub-linkrow__arr { margin-left: auto; color: var(--wb-text-3); }
.hub-verrow { display: flex; align-items: center; gap: 9px; padding: 7px 10px; border-radius: 9px; font-size: 11.5px; color: var(--wb-text-2); cursor: pointer;
  &:hover { background: var(--wb-hover); } }
.hub-verrow__dot { width: 7px; height: 7px; border-radius: 50%; background: var(--wb-text-3); flex: none;
  &.cur { background: #16a34a; } }
.hub-verrow__time { margin-left: auto; font-size: 10.5px; color: var(--wb-text-3); }
.hub-dacts { display: flex; gap: 8px; margin-top: 16px; }
.hub-abtn {
  flex: 1; display: flex; align-items: center; justify-content: center; gap: 6px;
  height: 36px; border: none; border-radius: 10px; font-size: 12px; font-weight: 700; cursor: pointer; font-family: inherit;
  transition: transform 0.12s t.$ease-out, opacity 0.15s ease;
  &:active { transform: scale(0.96); }
  &--pri { background: var(--wb-accent); color: #fff; &:hover { background: var(--wb-accent); } }
  &--ghost { background: var(--wb-hover); color: var(--wb-text); }
}
</style>
