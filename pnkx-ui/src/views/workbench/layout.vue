<template>
  <div class="wb">
    <!-- ============ 侧边栏 ============ -->
    <aside class="wb-side" :class="{ collapsed }">
      <div class="wb-brand">
        <span class="wb-logo">P</span>
        <span v-if="!collapsed" class="wb-brand__text"><b>Pei你看雪</b><i>个人 · 知识工作台</i></span>
      </div>

      <nav class="wb-nav">
        <div v-for="item in navItems" :key="item.path" class="wb-nav__item" :class="{ on: isActive(item.path) }" @click="$router.push(item.path)">
          <svg-icon :icon-class="item.icon" class-name="wb-ic18" />
          <span v-if="!collapsed">{{ item.title }}</span>
          <em v-if="!collapsed && item.badge" class="wb-nav__badge">{{ item.badge }}</em>
        </div>
      </nav>

      <template v-if="!collapsed && adminMenus.length">
        <div class="wb-side__sec">{{ adminSectionTitle }}</div>
        <nav class="wb-nav">
          <div v-for="c in adminMenus" :key="c.path" class="wb-sub__item" :class="{ on: isActive(c.path) }" @click="go(c.path)">
            <svg-icon :icon-class="c.icon || 'component'" class-name="wb-ic14" />
            <span>{{ c.title }}</span>
          </div>
        </nav>
      </template>

      <div class="wb-side__fill" />
      <div class="wb-user">
        <img v-if="avatar" :src="avatar" class="wb-user__img" alt="avatar" />
        <span v-else class="wb-user__ava">{{ avatarChar }}</span>
        <span v-if="!collapsed" class="wb-user__text"><b>{{ nickName }}</b><i>已登录</i></span>
      </div>
      <button class="wb-collapse" :title="collapsed ? '展开侧栏' : '收起侧栏'" @click="collapsed = !collapsed">
        <svg-icon icon-class="back" class-name="wb-ic14" :class="{ 'wb-collapse__flip': collapsed }" />
      </button>
    </aside>

    <!-- ============ 主区 ============ -->
    <div class="wb-main">
      <header class="wb-top">
        <div class="wb-search" @click="openPalette">
          <svg-icon icon-class="search" class-name="wb-ic15" />
          <span class="wb-search__text">搜索笔记、剪藏、账单、待办… 或输入指令</span>
          <span class="wb-search__kbd">⌘ K</span>
        </div>
        <div class="wb-top__acts">
          <button class="wb-iconbtn" :title="theme === 'light' ? '切换暗色' : '切换亮色'" @click="toggleTheme">
            <svg-icon icon-class="theme" class-name="wb-ic16" />
          </button>
          <button class="wb-adminbtn" title="回到管理后台首页" @click="$router.push('/index')">
            <svg-icon icon-class="system" class-name="wb-ic16" />
            <span>管理后台</span>
          </button>
          <button class="wb-capture" @click="openPalette('记')">
            <svg-icon icon-class="加号" class-name="wb-ic14" />快捷录入
          </button>
        </div>
      </header>

      <main class="wb-body" :class="{ 'wb-body--mirror': isMirrorPage }">
        <router-view />
      </main>
    </div>

    <!-- ⌘K 命令面板 -->
    <command-palette ref="palette" />
  </div>
</template>

<script>
import Layout from '@/layout/index.vue'
import ParentView from '@/components/ParentView/index.vue'
/**
 * 工作台外壳（PRODUCT_UIUE_REDESIGN §4.1）
 * 一级导航只有 今天/生活/资料/AI；RuoYi 管理菜单收进「内容发布」组；
 * 全局搜索与快捷录入统一由 ⌘K 命令面板承载。
 */
import { mapGetters } from 'vuex'
import CommandPalette from '@/components/CommandPalette/index.vue'

export default {
  name: 'WorkbenchLayout',
  components: { CommandPalette },
  data() {
    return {
      collapsed: false,
      navItems: [
        { path: '/workbench/today', title: '今天', icon: 'dashboard' },
        { path: '/workbench/life', title: '生活', icon: 'shopping' },
        { path: '/workbench/hub', title: '资料', icon: 'documentation' },
        { path: '/workbench/ai', title: 'AI 助手', icon: 'code' }
      ]
    }
  },
  computed: {
    ...mapGetters(['avatar', 'sidebarRouters']),
    theme() { return this.$store.state.app.theme },
    nickName() { return this.$store.state.user.nickName || this.$store.state.user.name || '我' },
    avatarChar() { return (this.nickName || 'P').charAt(0).toUpperCase() },
    /** 生活类一级目录（工作台只开放生活助手；其余管理目录一律走管理后台入口） */
    lifeDir() {
      for (const r of this.sidebarRouters || []) {
        if (r.hidden || !r.children?.length) continue
        const title = r.meta?.title || ''
        if (title.includes('生活') || (r.path || '').includes('life')) return r
      }
      return null
    },
    adminSectionTitle() { return this.lifeDir?.meta?.title || '生活助手' },
    /** 镜像进来的旧后台页面：内容区统一加内边距（工作台自建页面为自绘布局不加） */
    isMirrorPage() { return !!this.$route.meta?.wbMirror },
    /** 生活助手的页面级子菜单（隐藏项跳过，嵌套目录下钻到叶子） */
    adminMenus() {
      const dir = this.lifeDir
      if (!dir) return []
      const base = dir.path.startsWith('/') ? dir.path : '/' + dir.path
      const out = []
      for (const c of dir.children || []) {
        if (c.hidden) continue
        const abs = c.path?.startsWith('/') ? c.path : base + '/' + (c.path || '')
        const isDir = !c.component || c.component === Layout || c.component === ParentView
        const target = isDir ? this.firstLeaf(c, abs) : abs
        if (target) out.push({ path: '/workbench' + target, title: c.meta?.title, icon: c.meta?.icon })
      }
      return out
    }
  },
  methods: {
    toggleTheme() { this.$store.dispatch('app/toggleTheme') },
    isActive(path) { return this.$route.path.startsWith(path) },
    go(path) { this.$router.push(path).catch(() => {}) },
    firstLeaf(route, base = '') {
      const abs = route.path?.startsWith('/') ? route.path : base + '/' + (route.path || '')
      // 目录节点（Layout/ParentView 组件对象）必须深入到第一个可见叶子；
      // 此前用字符串 'Layout' 比较组件对象永不相等，导致返回目录自身路径、内容区空白
      const isDir = !route.component || route.component === Layout || route.component === ParentView
      if (!isDir) return abs
      for (const c of route.children || []) {
        if (c.hidden) continue
        const found = this.firstLeaf(c, abs)
        if (found) return found
      }
      return null
    },
    openPalette(seed) { this.$refs.palette.open(seed) }
  }
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/design-tokens.scss' as t;

/* ---- 工作台主题变量（运行时切换：html[data-theme] 由 store/app 驱动） ---- */
.wb {
  --wb-text: #1e293b;
  --wb-text-2: #475569;
  --wb-text-3: #94a3b8;
  --wb-border: #e2e8f0;
  --wb-hover: #f1f5f9;
  --wb-card: rgba(255, 255, 255, 0.72);
  --wb-page: linear-gradient(180deg, #f0f6ff, #f8fafc 420px);
  --wb-accent: #3B6FE0;
  --wb-accent-soft: rgba(79, 134, 247, 0.12);
}

[data-theme='dark'] .wb {
  --wb-text: #e2e8f0;
  --wb-text-2: #94a3b8;
  --wb-text-3: #64748b;
  --wb-border: #334155;
  --wb-hover: #1e293b;
  --wb-card: rgba(30, 41, 59, 0.72);
  --wb-page: linear-gradient(180deg, #17233c, #0f172a 420px);
  --wb-accent: #6aa1f9;
  --wb-accent-soft: rgba(106, 161, 249, 0.16);
}

.wb {
  display: flex;
  height: 100vh;
  background: var(--wb-page);
  overflow: hidden;
}

/* ---- 侧栏 ---- */
.wb-side {
  width: 224px;
  flex: none;
  display: flex;
  flex-direction: column;
  padding: 14px 12px;
  background: var(--wb-card);
  border-right: 1px solid var(--wb-border);
  backdrop-filter: blur(20px);
  transition: width 0.28s t.$ease-out;
  overflow-y: auto;
  overflow-x: hidden;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: var(--wb-border); border-radius: 2px; }
  &.collapsed { width: 64px; :deep() .wb-brand__text, :deep() .wb-nav span, :deep() .wb-side__sec, :deep() .wb-user__text { display: none; } }
}
.wb-brand {
  display: flex; align-items: center; gap: 10px; padding: 4px 6px 14px;
  &__text { line-height: 1.3; b { font-size: 14px; color: var(--wb-text); } i { display: block; font-style: normal; font-size: 10px; color: var(--wb-text-3); } }
}
.wb-logo {
  width: 32px; height: 32px; flex: none; border-radius: 10px;
  background: linear-gradient(135deg, t.$sky-500, t.$sky-700);
  color: #fff; font-weight: 800; font-size: 14px;
  display: flex; align-items: center; justify-content: center;
}
.wb-nav { display: flex; flex-direction: column; gap: 3px; }
.wb-nav__item {
  position: relative; display: flex; align-items: center; gap: 11px;
  height: 40px; padding: 0 10px; border-radius: 11px; cursor: pointer;
  font-size: 13px; font-weight: 600; color: var(--wb-text-2);
  transition: background 0.15s ease, color 0.15s ease;
  white-space: nowrap;
  &:hover { background: var(--wb-hover); color: var(--wb-text); }
  &.on { background: rgba(79, 134, 247, 0.12); color: var(--wb-accent); }
  &.on::before { content: ''; position: absolute; left: -12px; top: 9px; bottom: 9px; width: 3px; border-radius: 0 3px 3px 0; background: var(--wb-accent); }
  &--sub { height: 34px; font-size: 12.5px; font-weight: 500; }
  .wb-nav__badge { margin-left: auto; font-style: normal; font-size: 10px; font-weight: 700; background: t.$danger-500; color: #fff; border-radius: 8px; padding: 1px 6px; }
}
.wb-side__sec { font-size: 10.5px; font-weight: 700; color: var(--wb-text-3); letter-spacing: 0.5px; padding: 16px 10px 6px; }
/* ---- 生活助手页面级菜单 ---- */
.wb-sub__item {
  display: flex; align-items: center; gap: 8px;
  height: 30px; padding: 0 10px; border-radius: 9px; cursor: pointer;
  font-size: 12px; font-weight: 500; color: var(--wb-text-2); white-space: nowrap;
  transition: background 0.14s ease, color 0.14s ease;
  svg { color: var(--wb-text-3); }
  &:hover { background: var(--wb-hover); color: var(--wb-text); }
  &.on { background: rgba(79, 134, 247, 0.1); color: var(--wb-accent); svg { color: var(--wb-accent); } }
}
.wb-side__fill { flex: 1; }
.wb-user__img { width: 30px; height: 30px; flex: none; border-radius: 50%; object-fit: cover; border: 1px solid var(--wb-border); }
.wb-user {
  display: flex; align-items: center; gap: 10px; padding: 6px;
  &__ava { width: 30px; height: 30px; flex: none; border-radius: 50%; background: linear-gradient(135deg, t.$sky-500, t.$sky-700); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 800; }
  &__text { line-height: 1.3; b { font-size: 12.5px; display: block; } i { font-style: normal; font-size: 10px; color: var(--wb-text-3); } }
}
.wb-ic18 { font-size: 18px; }
.wb-ic16 { font-size: 16px; }
.wb-ic15 { font-size: 15px; }
.wb-ic14 { font-size: 14px; }
.wb-collapse__flip { transform: rotate(180deg); display: inline-block; }
.wb-collapse {
  border: none; background: transparent; color: var(--wb-text-3);
  width: 30px; height: 30px; border-radius: 9px; cursor: pointer;
  display: flex; align-items: center; justify-content: center; margin: 2px auto 0;
  &:hover { background: var(--wb-hover); color: var(--wb-text-2); }
}

/* ---- 主区 ---- */
.wb-main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.wb-top { flex: none; display: flex; align-items: center; gap: 12px; padding: 14px 20px 0; }
.wb-search {
  flex: 1; max-width: 520px; display: flex; align-items: center; gap: 9px;
  height: 38px; padding: 0 14px; border-radius: 11px; cursor: pointer;
  background: var(--wb-card); border: 1px solid var(--wb-border); color: var(--wb-text-3);
  backdrop-filter: blur(18px);
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
  &:hover { border-color: var(--wb-accent); box-shadow: 0 0 0 3px rgba(79, 134, 247, 0.12); }
  &__text { font-size: 13px; }
  &__kbd { margin-left: auto; font-size: 10px; padding: 2px 6px; border-radius: 5px; background: var(--wb-hover); border: 1px solid var(--wb-border); color: var(--wb-text-2); font-weight: 600; }
}
.wb-top__acts { margin-left: auto; display: flex; align-items: center; gap: 9px; }
.wb-adminbtn {
  display: flex; align-items: center; gap: 6px; height: 36px; padding: 0 13px;
  border-radius: 11px; cursor: pointer;
  border: 1px solid var(--wb-border); background: var(--wb-card); color: var(--wb-text-2);
  font-size: 12.5px; font-weight: 600;
  transition: color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease;
  &:hover { color: var(--wb-accent); border-color: var(--wb-accent); box-shadow: 0 0 0 3px rgba(79, 134, 247, 0.1); }
}
.wb-iconbtn {
  width: 36px; height: 36px; border-radius: 11px; cursor: pointer;
  border: 1px solid var(--wb-border); background: var(--wb-card); color: var(--wb-text-2);
  display: flex; align-items: center; justify-content: center;
  &:hover { color: var(--wb-accent); }
}
.wb-capture {
  display: flex; align-items: center; gap: 7px; height: 36px; padding: 0 15px;
  border: none; border-radius: 11px; cursor: pointer;
  background: linear-gradient(135deg, var(--wb-accent), var(--wb-accent)); color: #fff;
  font-size: 12.5px; font-weight: 700;
  box-shadow: 0 6px 16px rgba(59, 111, 224, 0.35);
  transition: transform 0.12s ease, box-shadow 0.18s ease;
  &:hover { box-shadow: 0 8px 22px rgba(59, 111, 224, 0.45); }
  &:active { transform: scale(0.96); }
}
.wb-body { flex: 1; min-height: 0; position: relative; overflow-y: auto; }
/* 镜像的旧后台页面：统一内容边距，与工作台自建页面留白对齐 */
.wb-body--mirror { padding: 14px 18px 20px; }
/* 自带 app-container padding 的旧页面归零，避免双重边距 */
.wb-body--mirror :deep(.app-container) { padding: 0; }
</style>
