<template>
    <el-card class="action-center" shadow="never">
        <template #header
            ><div class="header">
                <strong>今天要处理</strong><el-button :loading="loading" @click="load">刷新</el-button>
            </div></template
        >
        <el-select v-model="space" @change="changeSpace"
            ><el-option label="私人空间" value="personal" /><el-option
                v-for="g in spaces"
                :key="g.id"
                :label="g.group_name"
                :value="String(g.id)"
        /></el-select>
        <div class="item">
            <el-button v-for="key in pinned" :key="key" @click="go(key)">{{ entryLabels[key] }}</el-button
            ><el-button link @click="editing = !editing">管理常用入口</el-button>
        </div>
        <div v-if="editing">
            <el-checkbox-group v-model="pinned" :max="6"
                ><el-checkbox v-for="(label, key) in entryLabels" :key="key" :label="key">{{
                    label
                }}</el-checkbox></el-checkbox-group
            ><el-button @click="savePinned">保存顺序</el-button><small>按勾选顺序排列</small>
        </div>
        <el-alert v-if="error" :title="error" type="error" :closable="false" />
        <h3>
            到期与逾期待办 <span v-if="data.todoCount != null">（{{ data.todoCount }}）</span>
        </h3>
        <div v-for="todo in expanded ? data.todos || [] : (data.todos || []).slice(0, 5)" :key="todo.id" class="item">
            <span>{{ todo.content }} · {{ todo.planEndTime }}</span
            ><el-button v-if="data.canEdit" @click="complete(todo)">完成</el-button>
        </div>
        <el-empty v-if="data.todoCount === 0" description="暂无到期或逾期待办" :image-size="50" />
        <el-button v-if="data.todoCount > 5" link @click="expanded = !expanded">{{
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
        <feature-gate feature="chargeInstanceV2"
            ><div v-for="f in data.generationFailures || []" :key="'failure' + f.id" class="item">
                <span
                    >出账失败：{{ f.source_name }} · {{ f.due_date }}（{{ f.attempt_count }}次）<small
                        >请检查规则账户与分类后重试；下次自动尝试不早于 {{ f.next_retry_at || '下一轮调度' }}</small
                    ></span
                ><el-button @click="retryCharge(f, 'retry')">重试</el-button
                ><el-button link @click="retryCharge(f, 'dismiss')">忽略记录</el-button>
            </div>
        </feature-gate>
        <h3>今日餐饮</h3>
        <div v-for="m in data.meals || []" :key="m.id" class="item">
            {{ m.title }} · {{ m.servings }} 份<el-button link @click="go('meal')">查看计划</el-button>
        </div>
        <feature-gate feature="reminderEventV2"
            ><h3 v-if="space === 'personal'">提醒事项</h3>
            <div
                v-for="event in (data.reminders || []).filter((e) => ['open', 'snoozed'].includes(e.state)).slice(0, 5)"
                :key="event.id"
                class="item"
            >
                <div>
                    {{ event.title }} <el-tag>{{ states[event.state] || event.state }}</el-tag
                    ><small>{{ event.readAt ? '已读' : '未读' }}</small>
                </div>
                <div>
                    <el-button v-if="!event.readAt" link @click="act(event, 'read')">标记已读</el-button
                    ><template v-if="['open', 'snoozed'].includes(event.state)"
                        ><el-button link @click="act(event, 'snooze')">一小时后提醒</el-button
                        ><el-button link @click="act(event, 'dismiss')">忽略本次</el-button></template
                    >
                </div>
            </div> </feature-gate
        ><el-collapse v-if="space === 'personal'"
            ><el-collapse-item title="周期账单：待确认与历史" name="charges"
                ><feature-gate feature="chargeInstanceV2"><charge-instances /></feature-gate></el-collapse-item
            ><el-collapse-item title="AI 与报告操作计划" name="plans"
                ><feature-gate feature="operationPlanV2"><operation-plans /></feature-gate></el-collapse-item
        ></el-collapse>
    </el-card>
</template>
<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
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
    prefRevision = ref(0);
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
    data.value = {};
    page.value = 1;
    load();
}
async function savePinned() {
    const r = await request({
        url: '/life/action-center/preferences',
        method: 'put',
        data: { entries: pinned.value, expectedRevision: prefRevision.value }
    });
    prefRevision.value = r.data.revision;
    editing.value = false;
}
const data = ref({}),
    error = ref(''),
    loading = ref(false);
const states = { open: '待处理', snoozed: '稍后提醒', resolved: '已处理', cancelled: '已结束' };
async function load() {
    loading.value = true;
    error.value = '';
    try {
        const r = await request({
            url: '/life/action-center',
            params: {
                scope: space.value === 'personal' ? 'personal' : 'group',
                groupId: space.value === 'personal' ? undefined : space.value,
                page: page.value
            }
        });
        data.value = r.data || {};
        if (data.value.partialErrors?.length) error.value = '部分内容暂时无法加载，请刷新重试';
    } catch (e) {
        error.value = '行动中心加载失败';
    } finally {
        loading.value = false;
    }
}
async function retryCharge(f, action) {
    await request({ url: `/charge/instances/failures/${f.id}/${action}`, method: 'post' });
    await load();
}
async function complete(todo) {
    await request({ url: '/admin/toDo', method: 'put', data: { id: todo.id, status: true, kanbanStatus: 2 } });
    await load();
}
async function act(event, action) {
    await request({
        url: `/reminder/events/${event.id}/${action}`,
        method: 'post',
        data: {
            requestId: `reminder_${Date.now()}_${Math.random().toString(36).slice(2)}`,
            expectedRevision: event.revision,
            until: action === 'snooze' ? new Date(Date.now() + 3600000).toISOString() : null
        }
    });
    await load();
}
onMounted(async () => {
    load();
    const results = await Promise.allSettled([
        request({ url: '/life/action-center/spaces' }),
        request({ url: '/life/action-center/preferences' })
    ]);
    if (results[0].status === 'fulfilled') spaces.value = results[0].value.data || [];
    if (results[1].status === 'fulfilled') {
        pinned.value = JSON.parse(results[1].value.data.entries_json);
        prefRevision.value = results[1].value.data.revision;
    }
});
</script>
<style scoped>
.action-center {
    margin-bottom: 20px;
}
.header,
.item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
}
.item {
    padding: 12px 0;
    border-bottom: 1px solid #eee;
}
small {
    margin-left: 8px;
}
</style>
