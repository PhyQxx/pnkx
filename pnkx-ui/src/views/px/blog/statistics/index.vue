<template>
  <div class="statistics-page">
    <el-tabs v-model="activeTab" class="statistics-tabs">
      <el-tab-pane v-for="tab in tabs" :key="tab.name" :label="tab.label" :name="tab.name" />
    </el-tabs>
    <Overview v-if="activeTab === 'overview'" />
    <Analytics v-if="analyticsVisited" v-show="activeTab !== 'overview'" :active-tab="activeTab" />
  </div>
</template>
<script setup>
import { ref, watch } from 'vue'
import Overview from './Overview.vue'
import Analytics from './components/Analytics.vue'
defineOptions({ name: 'Statistics' })
const activeTab = ref('overview')
const analyticsVisited = ref(false)
const tabs = [
  { name: 'overview', label: '统计概览' },
  { name: 'trend', label: '访问趋势' },
  { name: 'pages', label: '页面排行' },
  { name: 'features', label: '功能事件' },
  { name: 'conversions', label: '操作成功率' },
  { name: 'funnels', label: '转化漏斗' },
  { name: 'timeline', label: '访问轨迹' },
  { name: 'retention', label: '数据维护' }
]
watch(activeTab, value => { if (value !== 'overview') analyticsVisited.value = true })
</script>
<style scoped>
.statistics-page { padding: var(--space-6, 24px); }
.statistics-tabs { background: var(--el-bg-color); border-radius: 8px; padding: 0 16px; margin-bottom: 16px; }
.statistics-tabs :deep(.el-tabs__header) { margin-bottom: 0; }
.statistics-page :deep(.dashboard-editor-container) { padding: 0; }
@media (max-width: 700px) { .statistics-page { padding: 12px; } }
</style>
