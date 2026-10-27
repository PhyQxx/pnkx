<template>
    <section class="action-center">
        <div class="ac-header">
            <div class="ac-header__left">
                <h2 class="ac-title">今天要处理</h2>
                <p class="ac-subtitle">{{ todayLabel }}</p>
            </div>
            <div class="ac-header__summary">
                <span v-if="summary.todos" class="ac-chip ac-chip--danger">待办 {{ summary.todos }}</span>
                <span v-if="summary.failures" class="ac-chip ac-chip--warning">出账失败 {{ summary.failures }}</span>
                <span v-if="summary.reminders" class="ac-chip ac-chip--primary">提醒 {{ summary.reminders }}</span>
                <el-button circle size="small" :loading="loading" @click="load">
                    <el-icon><Refresh /></el-icon>
                </el-button>
            </div>
        </div>

        <div class="ac-toolbar">
            <el-select v-model="space" class="ac-space" @change="changeSpace"
                ><el-option label="私人空间" value="personal" /><el-option
                    v-for="g in spaces"
                    :key="g.id"
                    :label="g.group_name"
                    :value="String(g.id)"
            /></el-select>
            <div class="ac-pins">
                <button v-for="key in pinned" :key="key" class="ac-pin" @click="go(key)">{{ entryLabels[key] }}</button>
                <el-button link :disabled="!preferencesReady || savingPins" @click="editPins">管理</el-button>
            </div>
        </div>
        <div v-if="editing" class="ac-pins-editor">
            <el-checkbox-group v-model="draftPinned" :max="6" :disabled="savingPins"
                ><el-checkbox v-for="(label, key) in entryLabels" :key="key" :label="key">{{
                    label
                }}</el-checkbox></el-checkbox-group
            ><el-alert v-if="pinsError" :title="pinsError" type="error" :closable="false" /><el-button
                :loading="savingPins"
                @click="savePinned"
                >保存顺序</el-button
            ><el-button :disabled="savingPins" @click="editing = false">取消</el-button
            ><small>按勾选顺序排列，保存后生效</small>
        </div>

        <el-alert v-if="error" :title="error" type="error" :closable="false" class="ac-error" />
        <p v-if="loading" class="loading-note" role="status">正在加载当前空间，稍候即可处理事项…</p>

        <div class="ac-section ac-section--danger">
            <div class="ac-section__head">
                <span class="ac-dot ac-dot--danger"></span>
                <span class="ac-section__title">到期与逾期待办</span>
                <span v-if="data.todoCount != null" class="ac-count">{{ data.todoCount }}</span>
            </div>
            <div
                v-for="(todo, index) in expanded ? data.todos || [] : (data.todos || []).slice(0, 5)"
                :key="todo.id"
                class="ac-item"
                :class="{ 'ac-item--overdue': isOverdue(todo) }"
                :style="{ '--i': index }"
            >
                <div class="ac-item__main">
                    <div class="ac-item__title">{{ todo.content }}</div>
                    <div class="ac-item__meta">
                        <span class="ac-chip ac-chip--plain">{{ shortTime(todo.planEndTime) }}</span>
                        <span v-if="isOverdue(todo)" class="ac-tag ac-tag--danger">已逾期</span>
                    </div>
                </div>
                <el-button
                    v-if="data.canEdit"
                    size="small"
                    type="primary"
                    round
                    :disabled="loading || busyItems['todo:' + todo.id]"
                    @click="complete(todo)"
                    >完成</el-button
                >
            </div>
            <el-empty v-if="data.todoCount === 0" description="暂无到期或逾期待办" :image-size="50" />
            <el-button v-if="data.todoCount > 5" link type="primary" @click="expanded = !expanded">{{
                expanded ? '收起' : '查看全部'
            }}</el-button
            ><el-pagination
                v-if="expanded && data.todoCount > 20"
                v-model:current-page="page"
                :page-size="20"
                :total="data.todoCount"
                layout="prev,pager,next"
                @current-change="load"
            />
        </div>

        <feature-gate feature="chargeInstanceV2"
            ><div class="ac-section ac-section--warning">
                <div class="ac-section__head">
                    <span class="ac-dot ac-dot--warning"></span>
                    <span class="ac-section__title">出账失败</span>
                    <span v-if="(data.generationFailures || []).length" class="ac-count">{{
                        data.generationFailures.length
                    }}</span>
                </div>
                <div v-for="f in data.generationFailures || []" :key="'failure' + f.id" class="ac-item ac-item--warning">
                    <div class="ac-item__main">
                        <div class="ac-item__title">{{ f.source_name }} · {{ f.due_date }}</div>
                        <div class="ac-item__meta">
                            <span class="ac-chip ac-chip--plain">已尝试 {{ f.attempt_count }} 次</span>
                            <span class="ac-chip ac-chip--plain">下次不早于 {{ shortTime(f.next_retry_at) || '下一轮调度' }}</span>
                        </div>
                    </div>
                    <div>
                        <el-button size="small" round :disabled="busyItems['charge:' + f.id]" @click="retryCharge(f, 'retry')">重试</el-button
                        ><el-button link size="small" :disabled="busyItems['charge:' + f.id]" @click="retryCharge(f, 'dismiss')"
                            >忽略</el-button
                        >
                    </div>
                </div>
            </div></feature-gate
        >

        <div class="ac-section ac-section--success">
            <div class="ac-section__head">
                <span class="ac-dot ac-dot--success"></span>
                <span class="ac-section__title">今日餐饮</span>
                <span v-if="(data.meals || []).length" class="ac-count">{{ data.meals.length }}</span>
            </div>
            <div v-for="m in data.meals || []" :key="m.id" class="ac-item">
                <div class="ac-item__main">
                    <div class="ac-item__title">{{ m.title }}</div>
                    <div class="ac-item__meta"><span class="ac-chip ac-chip--plain">{{ m.servings }} 份</span></div>
                </div>
                <el-button link type="primary" @click="go('meal')">查看计划</el-button>
            </div>
        </div>

        <feature-gate feature="reminderEventV2"
            ><div v-if="space === 'personal'" class="ac-section ac-section--primary">
                <div class="ac-section__head">
                    <span class="ac-dot ac-dot--primary"></span>
                    <span class="ac-section__title">提醒事项</span>
                    <span v-if="activeReminders.length" class="ac-count">{{ activeReminders.length }}</span>
                </div>
                <div v-for="event in activeReminders.slice(0, 5)" :key="event.id" class="ac-item">
                    <div class="ac-item__main">
                        <div class="ac-item__title">{{ event.title }}</div>
                        <div class="ac-item__meta">
                            <span class="ac-tag" :class="event.state === 'open' ? 'ac-tag--primary' : 'ac-tag--info'">{{
                                states[event.state] || event.state
                            }}</span>
                            <span class="ac-chip ac-chip--plain">{{ event.readAt ? '已读' : '未读' }}</span>
                        </div>
                    </div>
                    <div class="ac-item__actions">
                        <el-button
                            v-if="!event.readAt"
                            link
                            size="small"
                            :disabled="busyItems['reminder:' + event.id]"
                            @click="act(event, 'read')"
                            >标记已读</el-button
                        ><template v-if="['open', 'snoozed'].includes(event.state)"
                            ><el-button link size="small" :disabled="busyItems['reminder:' + event.id]" @click="act(event, 'snooze')"
                                >一小时后提醒</el-button
                            ><el-button link size="small" :disabled="busyItems['reminder:' + event.id]" @click="act(event, 'dismiss')"
                                >忽略本次</el-button
                            ></template
                        >
                    </div>
                </div>
            </div></feature-gate
        >

        <el-collapse v-if="space === 'personal'" class="ac-collapse"
            ><el-collapse-item title="周期账单：待确认与历史" name="charges"
                ><feature-gate feature="chargeInstanceV2"><charge-instances /></feature-gate></el-collapse-item
            ><el-collapse-item title="AI 与报告操作计划" name="plans"
                ><feature-gate feature="operationPlanV2"><operation-plans /></feature-gate></el-collapse-item
        ></el-collapse>
    </section>
</template>
<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { createLatestLoader, parsePinned } from '../utils/actionCenterState';
import { useRouter } from 'vue-router';
import { createActionMutations } from '../utils/actionCenterMutations';
import { Refresh } from '@element-plus/icons-vue';
import request from '@/utils/request';
import ChargeInstances from '@/components/ChargeInstances.vue';
import OperationPlans from '@/components/OperationPlans.vue';
const router = useRouter(),
    space = ref('personal'),
    spaces = ref([]),
    page = ref(1),
    expanded = ref(false),
    pinned = ref([]),
    editing = ref(false),
    prefRevision = ref(0),
    draftPinned = ref([]),
    savingPins = ref(false),
    preferencesReady = ref(false),
    pinsError = ref('');
const entryLabels = {
    todo: '待办',
    meal: '本周吃饭',
    shopping: '购物',
    bookkeeping: '记账',
    diary: '日记',
    report: '月末复盘'
};
const menuTitles = {
    todo: ['待办事项', '待办'],
    meal: ['餐饮计划', '生活管家'],
    shopping: ['购物清单', '生活管家'],
    bookkeeping: ['生活账本', '记账'],
    diary: ['日记'],
    report: ['生活报告', 'AI生活报告', '生活管家']
};
function go(key) {
    const route = router.getRoutes().find((r) => menuTitles[key]?.includes(r.meta?.title));
    if (route) router.push(route.path);
}
function changeSpace() {
    mutations.contextChanged();
    data.value = {};
    page.value = 1;
    expanded.value = false;
    load();
}
function editPins() {
    if (savingPins.value || !preferencesReady.value) return;
    draftPinned.value = [...pinned.value];
    pinsError.value = '';
    editing.value = !editing.value;
}
async function savePinned() {
    if (savingPins.value) return;
    savingPins.value = true;
    pinsError.value = '';
    const entries = [...draftPinned.value];
    try {
        const r = await request({
            url: '/life/action-center/preferences',
            method: 'put',
            data: { entries, expectedRevision: prefRevision.value }
        });
        prefRevision.value = r.data.revision;
        pinned.value = entries;
        editing.value = false;
    } catch {
        pinsError.value = '保存失败，选择已保留；如在其他设备修改过，请刷新页面后重试';
    } finally {
        savingPins.value = false;
    }
}
const data = ref({}),
    error = ref(''),
    loading = ref(false);
const states = { open: '待处理', snoozed: '稍后提醒', resolved: '已处理', cancelled: '已结束' };
const activeReminders = computed(() =>
    (data.value.reminders || []).filter((e) => ['open', 'snoozed'].includes(e.state))
);
const summary = computed(() => ({
    todos: data.value.todoCount || 0,
    failures: (data.value.generationFailures || []).length,
    reminders: activeReminders.value.length
}));
const todayLabel = new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' });
function isOverdue(todo) {
    if (!todo?.planEndTime) return false;
    return new Date(todo.planEndTime.replace(/-/g, '/')).getTime() < Date.now();
}
function shortTime(value) {
    return value ? String(value).slice(5, 16) : '';
}
const loader = createLatestLoader({
    fetch: (params) => request({ url: '/life/action-center', params }),
    onStart: () => {
        loading.value = true;
        error.value = '';
        data.value = {};
    },
    onData: (r) => {
        data.value = r.data || {};
        if (data.value.partialErrors?.length) error.value = '部分内容暂时无法加载，请刷新重试';
    },
    onError: () => {
        error.value = '行动中心加载失败，请刷新重试';
    },
    onSettled: () => {
        loading.value = false;
    }
});
function load() {
    return loader.run({
        scope: space.value === 'personal' ? 'personal' : 'group',
        groupId: space.value === 'personal' ? undefined : space.value,
        page: page.value
    });
}
onBeforeUnmount(() => loader.invalidate());
const busyItems = ref({});
const mutations = createActionMutations({
    request,
    refresh: load,
    onBusy: (key, value) => {
        busyItems.value = { ...busyItems.value, [key]: value };
    },
    onError: (message) => {
        error.value = message;
    },
    makeId: () => `reminder_${Date.now()}_${Math.random().toString(36).slice(2)}`
});
onBeforeUnmount(() => mutations.dispose());
function retryCharge(f, action) {
    return mutations.run(`charge:${f.id}`, action, () => ({
        url: `/charge/instances/failures/${f.id}/${action}`,
        method: 'post'
    }));
}
function complete(t) {
    return mutations.run(`todo:${t.id}`, 'complete', () => ({
        url: '/admin/toDo',
        method: 'put',
        data: { id: t.id, status: true, kanbanStatus: 2 }
    }));
}
function act(e, action) {
    return mutations.run(`reminder:${e.id}`, `${action}:${e.revision}`, ({ id, now }) => ({
        url: `/reminder/events/${e.id}/${action}`,
        method: 'post',
        data: {
            requestId: id,
            expectedRevision: e.revision,
            until: action === 'snooze' ? new Date(now + 3600000).toISOString() : null
        }
    }));
}
onMounted(async () => {
    load();
    const results = await Promise.allSettled([
        request({ url: '/life/action-center/spaces' }),
        request({ url: '/life/action-center/preferences' })
    ]);
    if (results[0].status === 'fulfilled') spaces.value = results[0].value.data || [];
    if (results[1].status === 'fulfilled') {
        pinned.value = parsePinned(results[1].value.data.entries_json, entryLabels);
        prefRevision.value = results[1].value.data.revision;
    }
    preferencesReady.value = true;
});
</script>
<style scoped>
.action-center {
    margin-bottom: 20px;
    padding: 20px;
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 14px;
    animation: ac-rise 0.3s ease both;
}
.ac-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 14px;
}
.ac-title {
    margin: 0;
    font-size: 20px;
    font-weight: 700;
}
.ac-subtitle {
    margin: 4px 0 0;
    font-size: 13px;
    color: var(--el-text-color-secondary);
}
.ac-header__summary {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    justify-content: flex-end;
}
.ac-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
    margin-bottom: 12px;
}
.ac-space {
    width: 180px;
}
.ac-pins {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
}
.ac-pin {
    padding: 6px 14px;
    font-size: 13px;
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    border: none;
    border-radius: 999px;
    cursor: pointer;
    transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.ac-pin:hover {
    transform: translateY(-1px);
    box-shadow: var(--el-box-shadow-light);
}
.ac-pins-editor {
    padding: 12px;
    margin-bottom: 12px;
    background: var(--el-fill-color-lighter);
    border-radius: 10px;
}
.ac-error {
    margin-bottom: 12px;
}
.ac-section {
    padding: 14px 0 6px;
    border-top: 1px solid var(--el-border-color-lighter);
}
.ac-section__head {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
}
.ac-section__title {
    font-size: 15px;
    font-weight: 600;
}
.ac-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
}
.ac-dot--danger {
    background: var(--el-color-danger);
}
.ac-dot--warning {
    background: var(--el-color-warning);
}
.ac-dot--success {
    background: var(--el-color-success);
}
.ac-dot--primary {
    background: var(--el-color-primary);
}
.ac-count {
    min-width: 20px;
    padding: 0 6px;
    font-size: 12px;
    line-height: 18px;
    color: var(--el-text-color-secondary);
    text-align: center;
    background: var(--el-fill-color);
    border-radius: 999px;
}
.ac-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 10px 12px;
    margin-bottom: 8px;
    background: var(--el-fill-color-lighter);
    border: 1px solid transparent;
    border-radius: 10px;
    transition: border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
    animation: ac-item-in 0.25s ease both;
    animation-delay: calc(var(--i, 0) * 30ms);
}
.ac-item:hover {
    border-color: var(--el-color-primary-light-7);
    box-shadow: var(--el-box-shadow-light);
    transform: translateY(-1px);
}
.ac-item--overdue {
    background: var(--el-color-danger-light-9);
}
.ac-item--overdue .ac-item__title {
    color: var(--el-color-danger);
}
.ac-item--warning {
    background: var(--el-color-warning-light-9);
}
.ac-item__main {
    min-width: 0;
    flex: 1;
}
.ac-item__title {
    font-size: 14px;
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
.ac-item__meta {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: 4px;
    flex-wrap: wrap;
}
.ac-item__actions {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: 2px;
}
.ac-chip {
    padding: 1px 8px;
    font-size: 12px;
    border-radius: 999px;
}
.ac-chip--plain {
    color: var(--el-text-color-secondary);
    background: var(--el-fill-color);
}
.ac-chip--danger {
    color: var(--el-color-danger);
    background: var(--el-color-danger-light-9);
}
.ac-chip--warning {
    color: var(--el-color-warning);
    background: var(--el-color-warning-light-9);
}
.ac-chip--primary {
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
}
.ac-tag {
    padding: 1px 8px;
    font-size: 12px;
    border-radius: 4px;
}
.ac-tag--danger {
    color: #fff;
    background: var(--el-color-danger);
}
.ac-tag--primary {
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
}
.ac-tag--info {
    color: var(--el-text-color-secondary);
    background: var(--el-fill-color);
}
.ac-collapse {
    border-top: 1px solid var(--el-border-color-lighter);
}
.loading-note {
    color: var(--el-text-color-secondary);
}
@keyframes ac-rise {
    from {
        opacity: 0;
        transform: translateY(12px);
    }
    to {
        opacity: 1;
        transform: none;
    }
}
@keyframes ac-item-in {
    from {
        opacity: 0;
        transform: translateY(6px);
    }
    to {
        opacity: 1;
        transform: none;
    }
}
</style>
