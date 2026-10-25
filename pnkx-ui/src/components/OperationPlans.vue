<template>
    <section class="plans" aria-label="操作计划">
        <header class="head">
            <div>
                <h2>操作计划</h2>
                <p>先确认，再执行。每一步都有结果可查。</p>
            </div>
            <el-button :loading="loading" :disabled="busy || draftEditing" @click="load">刷新</el-button>
        </header>
        <div class="toolbar">
            <div class="filters" aria-label="筛选计划">
                <button
                    v-for="f in filters"
                    :key="f.key"
                    :aria-pressed="filter === f.key"
                    :class="{ active: filter === f.key }"
                    @click="filter = f.key"
                >
                    {{ f.label }} <span>{{ count(f.key) }}</span>
                </button>
            </div>
            <el-input v-model="query" clearable placeholder="搜索计划名称" aria-label="搜索计划名称" class="search" />
        </div>
        <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
        <el-skeleton v-if="loading && !plans.length" :rows="3" animated class="skeleton" />
        <div v-else class="plan-list">
            <article v-for="p in filtered" :key="p.id" class="plan-row">
                <div class="plan-copy">
                    <div class="title-line">
                        <h3>{{ p.title }}</h3>
                        <el-tag :type="tone(p.status)" effect="light">{{ labels[p.status] || p.status }}</el-tag>
                    </div>
                    <p>{{ hint(p.status) }}</p>
                </div>
                <el-button
                    :type="attention(p.status) ? 'primary' : 'default'"
                    :plain="attention(p.status)"
                    :disabled="busy || draftEditing"
                    @click="inspect(p.id)"
                    >{{ actionLabel(p.status) }}</el-button
                >
            </article>
        </div>
        <el-empty
            v-if="!loading && !filtered.length && !error"
            :description="
                plans.length ? '没有匹配的计划，试试其他筛选或关键词' : '还没有操作计划，在 AI 对话中创建第一份草稿吧'
            "
            :image-size="64"
        />
        <footer class="list-note">显示最近加载的 {{ plans.length }} 个计划 · 待处理包含待确认与执行失败</footer>
        <el-dialog
            v-model="visible"
            title="计划详情"
            width="min(720px,94vw)"
            top="8vh"
            append-to-body
            :close-on-click-modal="!busy && !draftEditing"
            :close-on-press-escape="!busy && !draftEditing"
            :show-close="!busy && !draftEditing"
        >
            <div class="detail-body" :aria-busy="detailLoading">
                <el-skeleton v-if="detailLoading" :rows="4" animated />
                <el-alert v-if="detailError" :title="detailError" type="error" :closable="false" show-icon />
                <template v-if="detail && !detailLoading"
                    ><div class="detail-heading">
                        <h3>{{ detail.title }}</h3>
                        <el-tag :type="tone(detail.status)">{{ labels[detail.status] || detail.status }}</el-tag>
                    </div>
                    <p class="hint">{{ hint(detail.status) }}</p>
                    <PlanDraftEditor
                        :plan="detail"
                        :disabled="busy || draftEditing"
                        @editing="draftEditing = $event"
                        @updated="acceptRevision" />
                    <p class="progress">{{ completed }} / {{ detail.steps?.length || 0 }} 步已完成</p>
                    <article v-for="s in detail.steps || []" :key="s.step_number" class="step">
                        <div class="step-heading">
                            <span class="step-number">{{ s.step_number }}</span
                            ><strong>{{ tools[s.tool_code] || s.tool_code }}</strong
                            ><el-tag :type="tone(s.status)" size="small">{{ labels[s.status] || s.status }}</el-tag>
                        </div>
                        <dl class="fields">
                            <template v-for="(f, i) in fields(s.input_json)" :key="i"
                                ><dt>{{ f.label }}</dt>
                                <dd>{{ f.value }}</dd></template
                            >
                        </dl>
                        <p v-if="result(s).sourceId" class="receipt">
                            已创建记录 #{{ result(s).sourceId
                            }}<span v-if="result(s).affected"> · {{ result(s).affected }} 个商品</span>
                        </p>
                        <el-alert
                            v-if="s.error_msg"
                            :title="s.error_msg"
                            type="error"
                            :closable="false"
                            show-icon
                        /><el-button
                            v-if="result(s).sourceId && ['todo', 'meal'].includes(result(s).sourceType)"
                            link
                            type="primary"
                            @click="openResult(s)"
                            >查看已创建内容</el-button
                        >
                    </article>
                    <el-alert
                        v-if="compensation"
                        :title="`将移除 ${compensation.length} 个本计划创建且未修改的对象。确认后执行撤销。`"
                        type="warning"
                        :closable="false"
                        show-icon
                /></template>
            </div>
            <template #footer
                ><div class="actions">
                    <el-button :disabled="busy || draftEditing" @click="visible = false">{{
                        detail ? '关闭' : '返回'
                    }}</el-button
                    ><template v-if="detail && !detailLoading && !draftEditing"
                        ><el-button
                            v-if="['awaiting_confirmation', 'failed'].includes(detail.status)"
                            :disabled="busy || draftEditing"
                            @click="run('cancel')"
                            >取消计划</el-button
                        ><el-button
                            v-if="['succeeded', 'partially_succeeded'].includes(detail.status) && !compensation"
                            :loading="busy"
                            @click="previewUndo"
                            >预览撤销</el-button
                        ><el-button v-if="compensation" type="danger" :loading="busy" @click="run('compensate')"
                            >确认撤销</el-button
                        ><el-button
                            v-else-if="detail.status === 'awaiting_confirmation'"
                            type="primary"
                            :loading="busy"
                            @click="run('confirm')"
                            >确认并执行</el-button
                        ><el-button
                            v-else-if="['failed', 'partially_succeeded'].includes(detail.status)"
                            type="primary"
                            :loading="busy"
                            @click="run('retry')"
                            >重试未完成步骤</el-button
                        ><template v-else-if="detail.status === 'running'"
                            ><el-button :disabled="busy || draftEditing" @click="inspect(detail.id)">刷新进度</el-button
                            ><el-button :loading="busy" @click="run('retry')">尝试恢复执行</el-button></template
                        ></template
                    ><el-button v-else-if="detailError" @click="inspect(selectedId)">重新加载</el-button>
                </div></template
            >
        </el-dialog>
    </section>
</template>
<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import request from '@/utils/request';
import PlanDraftEditor from './PlanDraftEditor.vue';
import {
    planLabels as labels,
    toolLabels as tools,
    planFilters as filters,
    matchesPlan,
    planTone as tone,
    needsAttention as attention,
    planHint as hint,
    planActionLabel as actionLabel,
    planFields as fields,
    parsePlanValue
} from '../utils/operationPlanPresentation';
const plans = ref([]),
    filter = ref('all'),
    query = ref(''),
    loading = ref(false),
    error = ref(''),
    detail = ref(null),
    detailLoading = ref(false),
    detailError = ref(''),
    visible = ref(false),
    busy = ref(false),
    draftEditing = ref(false),
    compensation = ref(null),
    selectedId = ref(''),
    router = useRouter();
let listRequest = 0,
    detailRequest = 0;
const filtered = computed(() => plans.value.filter((p) => matchesPlan(p, filter.value, query.value))),
    completed = computed(() => (detail.value?.steps || []).filter((s) => s.status === 'succeeded').length);
const count = (key) => plans.value.filter((p) => matchesPlan(p, key)).length;
const result = (s) => parsePlanValue(s.result_json);
function acceptRevision(plan) {
    detail.value = plan;
    selectedId.value = plan.id;
    draftEditing.value = false;
    compensation.value = null;
    load();
}
async function load() {
    const token = ++listRequest;
    loading.value = true;
    try {
        const r = await request({ url: '/operation-plans' });
        if (token === listRequest) {
            plans.value = r.data || [];
            error.value = '';
        }
    } catch {
        if (token === listRequest) error.value = '加载失败，已有结果保留。请点击刷新重试。';
    } finally {
        if (token === listRequest) loading.value = false;
    }
}
async function inspect(id) {
    if (busy.value || draftEditing.value) return;
    const token = ++detailRequest;
    selectedId.value = id;
    detail.value = null;
    compensation.value = null;
    detailError.value = '';
    detailLoading.value = true;
    visible.value = true;
    try {
        const r = await request({ url: `/operation-plans/${id}` });
        if (token === detailRequest) detail.value = r.data;
    } catch {
        if (token === detailRequest) detailError.value = '详情加载失败，请重试。';
    } finally {
        if (token === detailRequest) detailLoading.value = false;
    }
}
async function run(action) {
    if (busy.value || !detail.value) return;
    busy.value = true;
    detailError.value = '';
    try {
        const r = await request({ url: `/operation-plans/${detail.value.id}/${action}`, method: 'post' });
        detail.value = r.data;
        compensation.value = null;
        await load();
    } catch (e) {
        compensation.value = null;
        detailError.value = e?.message || '操作未完成，请刷新详情核对状态后重试。';
    } finally {
        busy.value = false;
    }
}
async function previewUndo() {
    if (busy.value || draftEditing.value) return;
    busy.value = true;
    detailError.value = '';
    try {
        const r = await request({ url: `/operation-plans/${detail.value.id}/compensation-preview`, method: 'post' });
        compensation.value = r.data.compensation;
    } catch (e) {
        detailError.value = e?.message || '当前无法撤销，请在原模块核对内容。';
    } finally {
        busy.value = false;
    }
}
function openResult(s) {
    const r = result(s),
        paths = { todo: '/mytool/todo', meal: '/mytool/mealPlan' };
    if (paths[r.sourceType]) router.push({ path: paths[r.sourceType], query: { id: r.sourceId } });
}
onMounted(load);
onBeforeUnmount(() => {
    listRequest++;
    detailRequest++;
});
</script>
<style scoped>
.plans {
    margin: 16px 0;
    background: var(--el-bg-color, #fff);
    border: 1px solid var(--el-border-color-lighter, #e2e8f0);
    border-radius: 14px;
    overflow: hidden;
    color: var(--el-text-color-primary, #1e293b);
}
.head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24px;
    gap: 16px;
}
.head h2 {
    font-size: 20px;
    margin: 0;
}
.head p,
.plan-copy p,
.hint,
.list-note {
    font-size: 13px;
    color: var(--el-text-color-regular, #475569);
    line-height: 1.6;
}
.head p {
    margin: 6px 0 0;
}
.toolbar {
    display: flex;
    gap: 16px;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px 20px;
    flex-wrap: wrap;
}
.filters {
    display: flex;
    gap: 4px;
    flex-wrap: wrap;
    background: var(--el-fill-color-light, #f1f5f9);
    padding: 4px;
    border-radius: 9px;
}
.filters button {
    border: 0;
    background: transparent;
    padding: 9px 12px;
    cursor: pointer;
    border-radius: 6px;
    font: inherit;
    font-size: 13px;
    color: var(--el-text-color-regular, #475569);
}
.filters button.active {
    background: var(--el-bg-color, #fff);
    color: var(--el-color-primary, #0284c7);
    box-shadow: 0 1px 4px #0f172a12;
    font-weight: 600;
}
.filters span {
    margin-left: 5px;
    font-size: 12px;
}
.filters button:focus-visible {
    outline: 2px solid var(--el-color-primary);
    outline-offset: 2px;
}
.search {
    max-width: 220px;
}
.plan-row {
    padding: 20px 24px;
    display: flex;
    gap: 20px;
    justify-content: space-between;
    align-items: center;
    border-top: 1px solid var(--el-border-color-lighter, #e2e8f0);
}
.plan-copy {
    min-width: 0;
}
.title-line {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
}
.title-line h3 {
    font-size: 15px;
    margin: 0;
    overflow-wrap: anywhere;
}
.plan-copy p {
    margin: 7px 0 0;
}
.list-note {
    padding: 14px 24px;
    background: var(--el-fill-color-lighter, #f8fafc);
}
.skeleton {
    padding: 24px;
}
.detail-body {
    max-height: 62vh;
    overflow: auto;
    padding-right: 6px;
}
.detail-heading {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
}
.detail-heading h3 {
    font-size: 19px;
    margin: 0;
    overflow-wrap: anywhere;
}
.progress {
    color: var(--el-text-color-regular);
    font-size: 13px;
}
.step {
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 10px;
    padding: 18px;
    margin: 14px 0;
}
.step-heading {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
}
.step-number {
    display: grid;
    place-items: center;
    background: var(--el-fill-color-light);
    border-radius: 50%;
    width: 26px;
    height: 26px;
    font-size: 13px;
}
.fields {
    display: grid;
    grid-template-columns: 85px minmax(0, 1fr);
    gap: 10px 14px;
    font-size: 14px;
    line-height: 1.7;
    margin: 18px 0 8px;
}
.fields dt {
    color: var(--el-text-color-regular);
}
.fields dd {
    margin: 0;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
}
.receipt {
    color: var(--el-color-success-dark-2, #15803d);
    font-size: 13px;
}
.actions {
    display: flex;
    gap: 8px;
    justify-content: flex-end;
    flex-wrap: wrap;
}
.actions :deep(.el-button) {
    margin: 0;
}
.hint {
    margin: 10px 0;
}
.plan-row > .el-button {
    flex-shrink: 0;
}
@media (max-width: 600px) {
    .head,
    .plan-row {
        padding: 18px;
    }
    .toolbar {
        padding: 0 18px 16px;
    }
    .search {
        max-width: none;
    }
    .plan-row {
        align-items: flex-start;
        flex-direction: column;
        gap: 12px;
    }
    .plan-row > .el-button {
        align-self: flex-end;
    }
    .fields {
        grid-template-columns: 65px minmax(0, 1fr);
        gap: 8px;
    }
    .step {
        padding: 14px;
    }
    .actions :deep(.el-button) {
        min-height: 40px;
    }
    .list-note {
        padding: 12px 18px;
    }
}
</style>
