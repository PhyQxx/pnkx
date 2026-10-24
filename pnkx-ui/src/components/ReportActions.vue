<template>
    <el-card shadow="never" class="insights"
        ><template #header
            ><div class="head">
                <strong>报告行动与复盘</strong
                ><el-button :loading="loading" @click="snapshot">生成当前周期事实快照</el-button>
            </div></template
        ><el-alert
            title="事实来自实际账单；快照不会随历史账单修改而改变。采纳行动前请核对内容。"
            type="info"
            :closable="false"
        />
        <div v-for="i in rows" :key="i.id" class="insight">
            <p>{{ i.fact_text }}</p>
            <div v-if="i.status === 'proposed'" class="candidates">
                <small>可选行动：根据事实快照生成的规则草稿，可修改后再确认。</small>
                <div v-for="c in i.candidates || []" :key="c.id" class="candidate">
                    <strong>{{ c.title }}</strong>
                    <p>{{ c.basis }}</p>
                    <el-button link @click="open(i, c)">选择并编辑</el-button>
                </div>
            </div>
            <small
                >生成于 {{ i.create_time }} ·
                {{ i.status === 'accepted' ? '已采纳' : i.status === 'dismissed' ? '已忽略' : '待决策' }}</small
            >
            <p v-if="i.action_status">
                行动复盘：{{ statusLabels[i.action_status] || i.action_status
                }}<span v-if="i.target"> · {{ i.target.content }} · 截止 {{ i.target.plan_end_time }}</span>
            </p>
            <div>
                <el-button link @click="sources(i)">查看当前来源明细</el-button
                ><el-button v-if="i.status === 'proposed'" link @click="open(i)">创建后续行动</el-button
                ><el-button v-if="i.status === 'proposed' && !i.plan_id" link @click="dismiss(i)">忽略</el-button
                ><span v-if="i.execution_status"
                    >计划状态：{{ statusLabels[i.execution_status] || i.execution_status }}</span
                >
            </div>
        </div>
        <el-empty
            v-if="!rows.length"
            description="生成事实快照后，可将复盘结论转成具体行动"
            :image-size="50"
        /><el-dialog v-model="visible" title="预览后续行动" width="min(500px, 94vw)" append-to-body
            ><el-input
                v-model="form.content"
                type="textarea"
                placeholder="具体行动，例如：安排下周三顿晚饭"
            /><el-date-picker v-model="form.dueDate" type="date" value-format="YYYY-MM-DD" placeholder="完成日期" />
            <p>确认后创建一条待办，不会自动调整预算或更改账单。</p>
            <el-alert v-if="actionError" :title="actionError" type="warning" :closable="false" />
            <template #footer
                ><el-button @click="visible = false">取消</el-button
                ><el-button type="primary" :loading="saving" @click="accept">预览并确认</el-button></template
            ></el-dialog
        ><el-dialog v-model="sourceVisible" title="当前来源明细（可能与快照不同）" append-to-body
            ><el-table :data="sourceRows"
                ><el-table-column prop="pay_time" label="日期" /><el-table-column
                    prop="type_name"
                    label="分类" /><el-table-column prop="money" label="金额" /></el-table
            ><el-button
                :disabled="sourcePage === 1"
                @click="
                    sourcePage--;
                    sources(sourceInsight, false);
                "
                >上一页</el-button
            ><el-button
                :disabled="sourceRows.length < 30"
                @click="
                    sourcePage++;
                    sources(sourceInsight, false);
                "
                >下一页</el-button
            ></el-dialog
        ></el-card
    >
</template>
<script setup>
import { ref, onMounted } from 'vue';
import { ElMessageBox } from 'element-plus';
import request from '@/utils/request';
const statusLabels = {
    awaiting_confirmation: '待确认',
    running: '执行中',
    succeeded: '已执行',
    failed: '失败',
    partially_succeeded: '部分完成',
    accepted: '待办进行中',
    completed: '待办已完成',
    cancelled: '已取消或来源移除',
    compensated: '已撤销'
};
const props = defineProps({ period: { type: String, default: 'month' } });
const rows = ref([]),
    loading = ref(false),
    saving = ref(false),
    visible = ref(false),
    form = ref({}),
    selected = ref(null),
    sourceVisible = ref(false),
    sourceRows = ref([]),
    sourcePage = ref(1),
    sourceInsight = ref(null),
    actionError = ref('');
async function load() {
    const r = await request({ url: '/lifeReport/insights' });
    rows.value = r.data || [];
}
async function snapshot() {
    loading.value = true;
    try {
        await request({ url: '/lifeReport/insights', method: 'post', data: { period: props.period } });
        await load();
    } finally {
        loading.value = false;
    }
}
async function open(i, candidate) {
    selected.value = i;
    actionError.value = '';
    form.value = candidate
        ? { content: candidate.content, dueDate: candidate.dueDate, candidateId: candidate.id }
        : { content: '', dueDate: '' };
    if (!candidate && i.plan_id) {
        const p = await request({ url: `/operation-plans/${i.plan_id}` });
        const args = JSON.parse(p.data.steps[0].input_json);
        form.value = { ...args, candidateId: i.selected_candidate };
    }
    visible.value = true;
}
async function accept() {
    if (saving.value) return;
    saving.value = true;
    try {
        const p = await request({
            url: `/lifeReport/insights/${selected.value.id}/action-preview`,
            method: 'post',
            data: form.value
        });
        const step = p.data.steps[0];
        const args = JSON.parse(step.input_json);
        await ElMessageBox.confirm(`${args.content}，完成日期：${args.dueDate}`, '确认创建待办');
        const result = await request({ url: `/lifeReport/insights/${selected.value.id}/accept`, method: 'post' });
        if (result.data.status !== 'succeeded') {
            actionError.value = '行动尚未执行成功，请在操作计划中查看失败原因并重试';
            await load();
            return;
        }
        visible.value = false;
        await load();
    } finally {
        saving.value = false;
    }
}
async function sources(i, reset = true) {
    sourceInsight.value = i;
    if (reset) sourcePage.value = 1;
    const r = await request({ url: `/lifeReport/insights/${i.id}/sources`, params: { pageNum: sourcePage.value } });
    sourceRows.value = r.data || [];
    sourceVisible.value = true;
}
async function dismiss(i) {
    await request({ url: `/lifeReport/insights/${i.id}/dismiss`, method: 'post' });
    await load();
}
onMounted(load);
</script>
<style scoped>
.insights {
    margin: 16px 0;
}
.head {
    display: flex;
    align-items: center;
    justify-content: space-between;
}
.candidate {
    padding: 10px;
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    margin: 8px 0;
}
.insight {
    padding: 12px 0;
    border-bottom: 1px solid #eee;
}
</style>
