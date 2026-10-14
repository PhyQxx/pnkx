<template>
  <div class="lifewb">
    <div class="lifewb__head">
      <h1>生活</h1>
      <span>高频直达 · 其余功能收进生活管家</span>
    </div>

    <!-- 菜单驱动的生活模块宫格（数据源与移动端 sys_menu 同源） -->
    <section v-for="group in groups" :key="group.title" class="lifewb__sec">
      <div class="lifewb__gt">{{ group.title }}</div>
      <div class="lifewb__grid">
        <div v-for="m in group.items" :key="m.path" class="lifewb__mod" @click="$router.push('/workbench' + m.path).catch(() => {})">
          <span class="lifewb__mi"><svg-icon :icon-class="m.icon || 'component'" class-name="l-ic16" /></span>
          <b>{{ m.title }}</b>
          <i v-if="m.hint">{{ m.hint }}</i>
        </div>
      </div>
    </section>

    <div v-if="!groups.length && !loading" class="lifewb__empty">
      暂无可见菜单——工作台生活页按 sys_menu 驱动，请确认账号已分配生活类菜单。
    </div>
  </div>
</template>

<script>
/**
 * 工作台·生活页（PRODUCT_UIUE_REDESIGN §4.6）
 * 高频四宫格 + 生活管家分组：完全由权限菜单（sidebarRouters）驱动，
 * 不硬编码任何动态路由，与移动端 sys_menu 同源。
 */
import { mapGetters } from 'vuex'

const HIGH_FREQ = ['生活账本', '家庭日历', '待办事项', '日记']

export default {
  name: 'WorkbenchLife',
  data() { return { loading: false } },
  computed: {
    ...mapGetters(['sidebarRouters']),
    lifeMenus() {
      // 生活菜单树：标题含「生活」的一级目录，或 path 含 life 的一级目录
      const out = []
      for (const r of this.sidebarRouters || []) {
        if (r.hidden) continue
        const title = r.meta?.title || ''
        if ((title.includes('生活') || (r.path || '').includes('life')) && r.children?.length) out.push(r)
      }
      return out
    },
    flatChildren() {
      const out = []
      for (const dir of this.lifeMenus) {
        const walk = (list, base) => {
          for (const c of list || []) {
            if (c.hidden) continue
            const path = c.path?.startsWith('/') ? c.path : base + '/' + c.path
            if (c.component) out.push({ title: c.meta?.title, path, icon: c.meta?.icon, hint: '' })
            if (c.children?.length) walk(c.children, path)
          }
        }
        walk(dir.children, dir.path)
      }
      return out
    },
    groups() {
      const high = this.flatChildren.filter(m => HIGH_FREQ.includes(m.title))
      const groups = []
      if (high.length) groups.push({ title: '高频', items: high })
      const rest = this.flatChildren.filter(m => !HIGH_FREQ.includes(m.title))
      if (rest.length) groups.push({ title: '生活管家', items: rest })
      return groups
    }
  }
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/design-tokens.scss' as t;

.lifewb { position: absolute; inset: 0; overflow-y: auto; padding: 16px 20px 24px; }
.lifewb__head { display: flex; align-items: baseline; gap: 10px; padding: 2px 2px 12px;
  h1 { margin: 0; font-size: 19px; font-weight: 700; color: var(--wb-text); }
  span { font-size: 12px; color: var(--wb-text-2); } }
.lifewb__sec { margin-bottom: 18px; }
.lifewb__gt { font-size: 13px; font-weight: 700; color: var(--wb-text); padding: 8px 2px 10px; }
.lifewb__grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 12px; }
.lifewb__mod {
  display: flex; flex-direction: column; gap: 8px; padding: 14px 15px; cursor: pointer;
  border-radius: 15px; background: var(--wb-card); border: 1px solid var(--wb-border);
  backdrop-filter: blur(20px); box-shadow: t.$shadow-sm;
  transition: transform 0.16s t.$ease-out, box-shadow 0.18s ease;
  &:hover { transform: translateY(-2px); box-shadow: t.$shadow-lg; }
  b { font-size: 13px; color: var(--wb-text); }
  i { font-style: normal; font-size: 11px; color: var(--wb-text-2); }
}
.l-ic16 { font-size: 16px; }
.lifewb__mi { width: 32px; height: 32px; border-radius: 10px; background: rgba(79, 134, 247, 0.12); color: var(--wb-accent); display: flex; align-items: center; justify-content: center; }
.lifewb__empty { padding: 60px 20px; text-align: center; font-size: 12.5px; color: var(--wb-text-3); }
</style>
