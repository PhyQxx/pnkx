<template><section class="workspace-summary"><div><h3>我的资料</h3><router-link to="/workbench/hub">打开资料中心 →</router-link></div><p v-if="count !== null"><router-link to="/resources/hub?untagged=true">{{ count }} 条未整理资料</router-link></p><span v-if="recent.length">继续阅读：</span><router-link v-for="row in recent" :key="row.type + ':' + row.id" :to="`/r/${row.type}/${row.id}`" class="recent">{{ row.title }}</router-link><p v-if="error">{{ error }}</p></section></template>
<script>
import { listResources } from '@/api/workspace'
export default {
  data() { return { count: null, recent: [], error: '' } },
  mounted() { this.reload() }, activated() { this.reload() },
  methods: { async reload() { try { const [inbox, recent] = await Promise.all([listResources({ untagged: true, pageSize: 1 }), listResources({ recent: true, pageSize: 3 })]); this.count = inbox.data.total; this.recent = recent.data.rows; this.error = '' } catch { this.error = '资料暂时无法加载' } } }
}
</script>
<style scoped>.workspace-summary { margin-bottom: 20px; padding: 18px; background: var(--bg-card); border: 1px solid var(--border-primary); border-radius: 12px; }.workspace-summary > div { display: flex; align-items: center; justify-content: space-between; }h3 { margin: 0; }.recent { display: inline-block; margin: 8px 16px 0 0; max-width: 280px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }</style>
