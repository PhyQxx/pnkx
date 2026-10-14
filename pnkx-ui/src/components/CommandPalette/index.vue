<template>
  <transition name="wb-pal">
    <div v-if="visible" class="wb-pal" role="dialog" aria-label="全局搜索与快速操作">
      <div class="wb-pal__mask" @click="close" />
      <div class="wb-pal__panel">
        <div class="wb-pal__input">
          <svg-icon icon-class="search" class-name="p-ic16" />
          <input ref="input" v-model="keyword" placeholder="搜索资料、生活记录，或输入指令，如「记一笔」…" @input="onInput" @keydown.enter="runFirst" @keydown.esc="close" />
          <span class="wb-pal__kbd">Esc</span>
        </div>
        <div class="wb-pal__body">
          <template v-for="(group, gi) in groups">
            <div v-if="group.items.length" :key="group.name" class="wb-pal__group" :style="{ animationDelay: gi * 0.024 + 's' }">
              <div class="wb-pal__gt">{{ group.name }}</div>
              <div v-for="(item, i) in group.items" :key="item.key" class="wb-pal__row" :class="{ sel: firstKey === item.key }" :data-key="item.key" @click="run(item)">
                <svg-icon :icon-class="item.icon" class-name="p-ic15" />
                <span class="wb-pal__t">{{ item.title }}</span>
                <span class="wb-pal__h">{{ item.hint }}</span>
              </div>
            </div>
          </template>
          <div v-if="!anyResult" class="wb-pal__empty">没有匹配结果</div>
        </div>
        <div class="wb-pal__foot">Enter 打开第一条 · Esc 关闭</div>
      </div>
    </div>
  </transition>
</template>

<script>
/**
 * ⌘K 命令面板（PRODUCT_UIUE_REDESIGN §4.4）
 * 全局搜索（SearchService 12 类资源，经 /r/{type}/{id} 统一解析跳转）
 * 与快速操作（页面跳转 / 记一笔）混排。
 */
import { globalSearchPage } from '@/api'

const TYPE_LABEL = { file: '文件', note: '笔记', webArticle: '剪藏', todo: '待办', article: '文章', diary: '日记', bookkeeping: '账单', commemoration: '纪念日', subscription: '订阅', recipe: '菜谱', shopping: '购物', book: '书城' }

export default {
  name: 'CommandPalette',
  data() {
    return {
      visible: false,
      keyword: '',
      results: [],
      loading: false,
      seq: 0,
      timer: null
    }
  },
  computed: {
    actions() {
      const kw = this.keyword.trim()
      const acts = [
        { key: 'a1', icon: 'money', title: '记一笔', hint: '生活账本', menuTitle: '生活账本' },
        { key: 'a2', icon: 'edit', title: '新建笔记', hint: '资料中心', to: '/workbench/hub' },
        { key: 'a3', icon: 'link', title: '剪藏链接…', hint: '资料中心', to: '/workbench/hub' },
        { key: 'a4', icon: 'message', title: '资料问答', hint: 'AI 助手', to: '/workbench/ai' },
        { key: 'a5', icon: 'chart', title: '生活报告', hint: 'AI 助手', to: '/workbench/ai' },
        { key: 'a6', icon: 'dashboard', title: '回到今天', hint: '工作台', to: '/workbench/today' },
        { key: 'a7', icon: 'system', title: '管理后台', hint: '系统管理', to: '/index' }
      ]
      if (!kw) return acts
      return acts.filter(a => a.title.toLowerCase().includes(kw.toLowerCase()))
    },
    searchGroup() {
      return this.results.map(r => ({
        key: 's:' + r.type + ':' + r.id,
        icon: r.type === 'webArticle' ? 'link' : r.type === 'file' ? 'file' : r.type === 'todo' ? 'todo' : 'documentation',
        title: r.title || '（无标题）',
        hint: `${TYPE_LABEL[r.type] || r.type} · ${r.updatedAt || ''}`,
        to: `/workbench/r/${r.type}/${r.id}`
      }))
    },
    groups() {
      return [
        { name: '快速操作', items: this.actions },
        { name: '资料与记录', items: this.searchGroup }
      ]
    },
    firstKey() { return this.groups.some(g => g.items.length) ? this.groups.find(g => g.items.length).items[0].key : null },
    anyResult() { return this.groups.some(g => g.items.length) }
  },
  mounted() {
    document.addEventListener('keydown', this.onKeydown)
  },
  beforeUnmount() {
    document.removeEventListener('keydown', this.onKeydown)
  },
  methods: {
    onKeydown(e) {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault()
        this.visible ? this.close() : this.open('')
      }
    },
    open(seed) {
      this.visible = true
      this.keyword = seed || ''
      this.results = []
      this.$nextTick(() => this.$refs.input && this.$refs.input.focus())
      if (this.keyword) this.query()
    },
    close() { this.visible = false },
    onInput() {
      clearTimeout(this.timer)
      this.timer = setTimeout(() => this.query(), 300)
    },
    async query() {
      const kw = this.keyword.trim()
      if (!kw) { this.results = []; return }
      const seq = ++this.seq
      this.loading = true
      try {
        const res = await globalSearchPage({ q: kw, type: 'all', pageNum: 1, pageSize: 8 })
        if (seq === this.seq) this.results = res.data?.rows || []
      } catch (e) {
        if (seq === this.seq) this.results = []
      } finally {
        if (seq === this.seq) this.loading = false
      }
    },
    runFirst() {
      for (const g of this.groups) {
        if (g.items.length) { this.run(g.items[0]); return }
      }
    },
    /** 按菜单标题解析动态路由（避免硬编码路径 404） */
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
    run(item) {
      this.close()
      let to = item.to
      if (item.menuTitle) {
        to = this.resolveMenu(item.menuTitle)
        if (!to) { this.$message?.info('未找到对应功能菜单，请从侧栏进入'); return }
      }
      if (/^https?:/.test(to)) window.open(to)
      else this.$router.push(to).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/design-tokens.scss' as t;

.p-ic15 { font-size: 15px; }
.p-ic16 { font-size: 16px; }
.wb-pal { position: fixed; inset: 0; z-index: 3000; }
.wb-pal__mask { position: absolute; inset: 0; background: rgba(15, 23, 42, 0.4); backdrop-filter: blur(2px); }
.wb-pal__panel {
  position: absolute; left: 50%; top: 11%; transform: translateX(-50%);
  width: min(560px, 88vw); border-radius: 16px; overflow: hidden;
  background: var(--wb-card);
  border: 1px solid var(--wb-border); box-shadow: t.$shadow-xl;
  backdrop-filter: blur(28px);
}
.wb-pal__input {
  display: flex; align-items: center; gap: 10px; padding: 14px 16px;
  border-bottom: 1px solid var(--wb-hover); color: var(--wb-text-3);
  input { flex: 1; border: none; outline: none; background: transparent; font-size: 14px; color: var(--wb-text); font-family: inherit; }
}
.wb-pal__kbd { font-size: 10px; padding: 2px 6px; border-radius: 5px; background: var(--wb-hover); border: 1px solid var(--wb-border); color: var(--wb-text-2); font-weight: 600; }
.wb-pal__body { max-height: 330px; overflow-y: auto; padding: 8px; }
.wb-pal__group {
  animation: wb-pal-group-in 0.22s t.$ease-out both;
}
@keyframes wb-pal-group-in { from { opacity: 0; transform: translateY(5px); } to { opacity: 1; transform: none; } }
.wb-pal__gt { font-size: 10.5px; font-weight: 700; color: var(--wb-text-3); letter-spacing: 0.5px; padding: 8px 10px 4px; }
.wb-pal__row {
  display: flex; align-items: center; gap: 10px; padding: 9px 10px;
  border-radius: 10px; cursor: pointer; font-size: 12.5px; font-weight: 600; color: var(--wb-text);
  transition: background 0.12s ease;
  &:hover, &.sel { background: rgba(79, 134, 247, 0.1); }
  .wb-pal__h { margin-left: auto; font-size: 10.5px; color: var(--wb-text-3); font-weight: 500; max-width: 45%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}
.wb-pal__empty { padding: 26px; text-align: center; color: var(--wb-text-3); font-size: 12.5px; }
.wb-pal__foot { padding: 8px 16px; border-top: 1px solid var(--wb-hover); font-size: 10.5px; color: var(--wb-text-3); }

.wb-pal-enter-active { transition: opacity 0.16s ease; }
.wb-pal-enter-active .wb-pal__panel { transition: transform 0.2s t.$ease-out, opacity 0.16s ease; }
.wb-pal-enter-from { opacity: 0; }
.wb-pal-enter-from .wb-pal__panel { transform: translateX(-50%) scale(0.97); opacity: 0; }
</style>
