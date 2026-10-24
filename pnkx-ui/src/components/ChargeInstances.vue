<template>
    <el-card class="charge-panel" shadow="never">
        <template #header
            ><div class="charge-header">
                <strong>周期账单 · 待确认与历史</strong><el-button :loading="loading" @click="load">刷新</el-button>
            </div></template
        >
        <el-alert v-if="error" :title="error" type="error" :closable="false" />
        <el-select
            v-model="status"
            clearable
            placeholder="全部账期状态"
            @change="
                page = 1;
                load();
            "
            ><el-option v-for="(label, key) in labels" :key="key" :label="label" :value="key"
        /></el-select>
        <el-table v-loading="loading" :data="rows" empty-text="暂无账期，规则到期后会自动生成">
            <el-table-column prop="sourceName" label="来源" />
            <el-table-column prop="dueDate" label="预计日期" />
            <el-table-column prop="expectedAmount" label="预计金额" />
            <el-table-column prop="actualAmount" label="实际金额" />
            <el-table-column label="状态"
                ><template #default="{ row }">{{ labels[row.status] || row.status }}</template></el-table-column
            >
            <el-table-column label="处理" width="280"
                ><template #default="{ row }">
                    <el-button link @click="inspect(row)">详情</el-button>
                    <template v-if="['pending', 'payment_failed'].includes(row.status)">
                        <el-button link type="primary" @click="open(row)">确认入账</el-button>
                        <el-button link @click="resolve(row, 'skip')">跳过本期</el-button>
                        <el-button v-if="row.status === 'pending'" link @click="resolve(row, 'payment-failed')"
                            >扣费失败</el-button
                        > </template
                    ><span v-else>{{ row.reason || '已处理' }}</span>
                </template></el-table-column
            >
        </el-table>
        <div class="charge-header">
            <el-button
                :disabled="page === 1"
                @click="
                    page--;
                    load();
                "
                >上一页</el-button
            ><span>第 {{ page }} 页</span
            ><el-button
                :disabled="rows.length < 20"
                @click="
                    page++;
                    load();
                "
                >下一页</el-button
            >
        </div>
        <el-dialog
            v-model="visible"
            title="确认实际扣费"
            width="min(480px, 94vw)"
            append-to-body
            :close-on-click-modal="false"
        >
            <el-form label-width="100px">
                <el-form-item label="实际金额"
                    ><el-input-number v-model="form.actualAmount" :min="0" :precision="2"
                /></el-form-item>
                <el-form-item label="实际日期"
                    ><el-date-picker v-model="form.paidAt" type="date" value-format="YYYY-MM-DD"
                /></el-form-item>
                <el-form-item label="账户"
                    ><el-select v-model="form.accountId"
                        ><el-option v-for="a in accounts" :key="a.id" :label="a.accountName" :value="a.id" /></el-select
                ></el-form-item>
                <el-form-item v-if="selected.classificationId !== 0" label="分类"
                    ><el-select v-model="form.classificationId"
                        ><el-option v-for="c in categories" :key="c.id" :label="c.typeName" :value="c.id" /></el-select
                ></el-form-item>
            </el-form>
            <template #footer
                ><el-button @click="visible = false">取消</el-button
                ><el-button type="primary" :loading="saving" @click="confirm">确认入账</el-button></template
            >
        </el-dialog>
        <el-dialog v-model="detailVisible" title="账期详情与处理历史" width="min(600px,94vw)" append-to-body>
            <template v-if="detail"
                ><p>{{ detail.instance.sourceName }} · {{ detail.instance.dueDate }}</p>
                <p>
                    预计 ¥{{ detail.instance.expectedAmount }} · 实际
                    {{ detail.instance.actualAmount == null ? '尚未入账' : '¥' + detail.instance.actualAmount }} ·
                    {{ labels[detail.instance.status] }}
                </p>
                <p v-if="detail.instance.recordId">关联账单编号：{{ detail.instance.recordId }}</p>
                <p v-for="(h, index) in detail.history" :key="index">{{ h.create_time }} · {{ historyLabel(h) }}</p>
                <p v-if="!detail.history.length">暂无人工处理记录；该账期由规则生成。</p></template
            >
        </el-dialog>
    </el-card>
</template>
<script setup>
import { ref, onMounted } from 'vue';
import request from '@/utils/request';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listAccount } from '@/api/px/life/bookkeeping/account';
import { listClassification } from '@/api/px/life/bookkeeping/classification';
const rows = ref([]),
    page = ref(1),
    loading = ref(false),
    error = ref(''),
    visible = ref(false),
    saving = ref(false);
const selected = ref({}),
    form = ref({}),
    accounts = ref([]),
    categories = ref([]);
const props = defineProps({ sourceType: { type: String, default: null } });
const status = ref(''),
    detail = ref(null),
    detailVisible = ref(false);
async function inspect(row) {
    const r = await request({ url: `/charge/instances/${row.id}` });
    detail.value = r.data;
    detailVisible.value = true;
}
function historyLabel(h) {
    try {
        const r = JSON.parse(h.result_json);
        return (labels[r.status] || r.status) + (r.reason ? ' · ' + r.reason : '');
    } catch {
        return '处理记录';
    }
}
const labels = {
    pending: '待确认',
    payment_failed: '扣费失败',
    posted: '已入账',
    voided: '账单已作废',
    skipped: '已跳过',
    cancelled: '已取消'
};
const requestId = () => `charge_${Date.now()}_${Math.random().toString(36).slice(2)}`;
async function load() {
    loading.value = true;
    error.value = '';
    try {
        const r = await request({
            url: '/charge/instances',
            params: { pageNum: page.value, pageSize: 20, sourceType: props.sourceType, status: status.value }
        });
        rows.value = r.data || [];
    } catch (e) {
        error.value = '账期加载失败，请重试';
    } finally {
        loading.value = false;
    }
}
async function open(row) {
    selected.value = row;
    form.value = {
        requestId: requestId(),
        expectedRevision: row.revision,
        actualAmount: Number(row.expectedAmount),
        paidAt: row.dueDate,
        accountId: row.accountId,
        classificationId: row.classificationId
    };
    visible.value = true;
    const results = await Promise.allSettled([listAccount({ pageSize: 100 }), listClassification({ pageSize: 100 })]);
    if (results[0].status === 'fulfilled') accounts.value = results[0].value.rows || [];
    if (results[1].status === 'fulfilled') categories.value = results[1].value.rows || [];
}
async function confirm() {
    if (saving.value) return;
    saving.value = true;
    try {
        await request({ url: `/charge/instances/${selected.value.id}/confirm`, method: 'post', data: form.value });
        visible.value = false;
        ElMessage.success('已入账');
        await load();
    } finally {
        saving.value = false;
    }
}
async function resolve(row, action) {
    try {
        const { value } = await ElMessageBox.prompt('请填写原因', '处理本期账单', {
            inputValidator: (v) => !!v?.trim() || '请填写原因'
        });
        await request({
            url: `/charge/instances/${row.id}/${action}`,
            method: 'post',
            data: { requestId: requestId(), expectedRevision: row.revision, reason: value }
        });
        await load();
    } catch (e) {
        if (e !== 'cancel' && e !== 'close') await load();
    }
}
onMounted(load);
</script>
<style scoped>
.charge-panel {
    margin: 16px 0;
}
.charge-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin: 8px 0;
}
</style>
