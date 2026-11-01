<template>
    <section class="tool-catalog" aria-label="AI 工具目录">
        <header class="catalog-heading">
            <div><span class="eyebrow">AI CAPABILITIES</span><h1>AI 能帮你做什么</h1><p>浏览工具能力，找到适合当前事情的助手。</p></div>
            <el-button :loading="loading" @click="load">刷新工具</el-button>
        </header>
        <div class="catalog-stats">
            <div><strong>{{ loaded ? registeredCount : '—' }}</strong><span>服务端已注册</span></div>
            <div><strong>{{ loaded ? confirmationCount : '—' }}</strong><span>包含确认流程</span></div>
            <div><strong>{{ plannedCount }}</strong><span>规划中的工具</span></div>
        </div>
        <p class="catalog-note">已注册不代表所有账号均可执行，实际操作仍受权限与功能开关约束。确认标记按工具级别展示，工具内的查询通常无需确认。</p>
        <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />
        <div class="catalog-filters">
            <el-input v-model="search" placeholder="搜索工具名称、用途或 Tool ID" clearable aria-label="搜索工具" />
            <el-select v-model="status" aria-label="筛选实现状态"><el-option label="全部状态" value="all" /><el-option label="已注册" value="registered" /><el-option label="规划中" value="planned" /></el-select>
            <el-select v-model="category" aria-label="筛选分类"><el-option label="全部分类" value="all" /><el-option v-for="group in categories" :key="group" :label="group" :value="group" /></el-select>
        </div>
        <p class="result-count" aria-live="polite">{{ loading ? '正在读取服务端工具…' : `显示 ${filtered.length} 个工具` }}</p>
        <div v-loading="loading" class="tool-grid">
            <article v-for="tool in filtered" :key="tool.name" class="tool-card" :class="{ planned: tool.status === 'planned' }">
                <div class="card-top"><span class="category-label">{{ tool.category }}</span><el-tag size="small" :type="tool.status === 'registered' ? 'success' : 'info'">{{ tool.status === 'registered' ? '已注册' : `规划中 · ${tool.phase}` }}</el-tag></div>
                <h2>{{ tool.title }}</h2><code>{{ tool.name }}</code><p class="tool-summary">{{ tool.summary }}</p>
                <p v-if="tool.status === 'registered'" class="confirmation">{{ tool.requiresConfirmation ? '写入操作有确认流程' : '无统一确认步骤' }}</p>
                <details v-if="tool.description"><summary>查看服务端说明</summary><p class="server-description">{{ tool.description }}</p></details>
                <div v-if="tool.example" class="tool-example"><span>试着这样说</span><p>{{ tool.example }}</p><el-button size="small" @click="copy(tool.example)">复制示例</el-button></div>
                <p v-else-if="tool.status === 'planned'" class="planned-note">尚未开放调用，按开发计划逐步接入。</p>
            </article>
        </div>
        <el-empty v-if="!loading && !filtered.length" description="没有匹配的工具，试试其他关键词或分类" />
    </section>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import request from '@/utils/request';
import { buildToolCatalog, plannedTools } from '@/utils/aiToolCatalog';
const loading = ref(false), loaded = ref(false), error = ref(''), tools = ref([...plannedTools]);
const search = ref(''), status = ref('all'), category = ref('all');
const categories = ['饮食与采购', '财务', '日程', '内容', '生活', '通用'];
const registeredCount = computed(() => tools.value.filter(t => t.status === 'registered').length);
const confirmationCount = computed(() => tools.value.filter(t => t.status === 'registered' && t.requiresConfirmation).length);
const plannedCount = computed(() => tools.value.filter(t => t.status === 'planned').length);
const filtered = computed(() => tools.value.filter(t => (status.value === 'all' || status.value === t.status) &&
    (category.value === 'all' || category.value === t.category) &&
    `${t.title} ${t.name} ${t.summary} ${t.description || ''}`.toLowerCase().includes(search.value.trim().toLowerCase())));
async function load() {
    loading.value = true; error.value = '';
    try {
        const response = await request({ url: '/ai/tools', method: 'get' });
        if (!Array.isArray(response.data)) throw new Error('invalid response');
        tools.value = buildToolCatalog(response.data); loaded.value = true;
    } catch (_) {
        error.value = loaded.value ? '刷新失败，当前显示上次成功读取的工具。请稍后重试。' : '工具清单读取失败，暂时仅展示开发规划。请检查登录状态或稍后重试。';
    } finally { loading.value = false; }
}
async function copy(text) {
    try { await navigator.clipboard.writeText(text); ElMessage.success('示例已复制，可粘贴到 AI 助手'); }
    catch (_) { ElMessage.warning('浏览器不允许复制，请手动选中示例文字复制'); }
}
onMounted(load);
</script>
<style scoped>
.tool-catalog{--catalog-accent:#405bd5;color:var(--el-text-color-primary);padding:28px;max-width:1320px;margin:auto}.catalog-heading{display:flex;justify-content:space-between;align-items:center;gap:16px}.eyebrow{font-size:11px;letter-spacing:2px;color:var(--catalog-accent);font-weight:700}h1{font-size:28px;margin:10px 0} .catalog-heading p,.catalog-note{color:var(--el-text-color-secondary);line-height:1.7}.catalog-stats{display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin:24px 0 16px}.catalog-stats>div{border:1px solid var(--el-border-color-lighter);background:var(--el-fill-color-light);padding:20px;border-radius:14px;display:flex;align-items:center;gap:14px}.catalog-stats strong{font-size:30px;color:var(--catalog-accent)}.catalog-stats span{font-size:13px}.catalog-note{font-size:12px}.catalog-filters{display:grid;grid-template-columns:minmax(180px,1fr) 150px 150px;gap:12px;margin-top:24px}.result-count{font-size:12px;color:var(--el-text-color-secondary);margin:18px 0}.tool-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:16px}.tool-card{border:1px solid var(--el-border-color-lighter);border-radius:16px;padding:22px;background:var(--el-bg-color);display:flex;flex-direction:column;min-width:0}.tool-card:hover{border-color:var(--el-color-primary-light-5);box-shadow:0 6px 20px #3147810a}.card-top{display:flex;justify-content:space-between;align-items:center;gap:8px}.category-label{font-size:12px;color:var(--el-text-color-secondary)}h2{font-size:18px;margin:18px 0 7px}.tool-card code{font-size:12px;color:var(--catalog-accent);overflow-wrap:anywhere}.tool-summary{font-size:14px;line-height:1.8;min-height:48px}.confirmation{font-size:12px;color:var(--el-text-color-secondary)}details{font-size:12px;margin-bottom:14px}summary{cursor:pointer;color:var(--catalog-accent)}.server-description{white-space:pre-wrap;overflow-wrap:anywhere;line-height:1.7}.tool-example{margin-top:auto;padding:14px;background:var(--el-fill-color-light);border-radius:10px}.tool-example>span{font-size:11px;color:var(--el-text-color-secondary)}.tool-example p{font-size:13px;line-height:1.7;overflow-wrap:anywhere}.planned{border-style:dashed}.planned-note{font-size:12px;color:var(--el-text-color-secondary);margin-top:auto;line-height:1.7}@media(max-width:640px){.tool-catalog{padding:16px}.catalog-heading{align-items:flex-start}h1{font-size:22px}.catalog-stats{gap:8px}.catalog-stats>div{padding:12px;display:block}.catalog-stats strong{display:block;font-size:25px;margin-bottom:6px}.catalog-filters{grid-template-columns:1fr 1fr}.catalog-filters>.el-input{grid-column:1/-1}.tool-grid{grid-template-columns:1fr}}
</style>
