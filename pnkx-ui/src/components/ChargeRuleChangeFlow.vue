<template>
    <el-dialog
        v-model="visible"
        :title="task?.kind === 'backfill' ? '补齐暂停期间账期' : '调整账期规则'"
        width="min(660px,94vw)"
        top="6vh"
        append-to-body
        :close-on-click-modal="false"
        :close-on-press-escape="!busy"
        :show-close="!busy"
        @close="cancel"
    >
        <div v-if="task" class="flow">
            <el-steps :active="step" simple class="flow-steps">
                <el-step title="变化摘要" />
                <el-step title="影响账期" />
                <el-step title="确认执行" />
            </el-steps>

            <!-- 第 1 步:变化摘要 -->
            <div v-if="step === 0" class="flow-body">
                <template v-if="task.kind === 'change'">
                    <el-table :data="diffs" size="small">
                        <el-table-column label="字段" prop="label" width="120" />
                        <el-table-column label="当前值" prop="before" />
                        <el-table-column label="调整为" prop="after" />
                    </el-table>
                    <el-alert
                        v-if="(task.preview.duplicates || []).length"
                        type="warning"
                        :closable="false"
                        :title="'存在同金额规则：' + task.preview.duplicates.map((d) => d.name).join('、') + '，请核对是否重复并保留主规则。'"
                    />
                    <p v-if="!diffs.length" class="hint">没有检测到字段变化。</p>
                </template>
                <template v-else>
                    <p class="lead">将补齐以下 {{ task.preview.periods.length }} 个暂停期间账期：</p>
                    <div class="period-list">
                        <el-tag v-for="p in task.preview.periods" :key="p" size="small" effect="plain">{{ p }}</el-tag>
                    </div>
                    <p class="hint">每期预计 {{ task.preview.amountPerPeriod }} 元，只生成待确认账期，不直接增加支出。</p>
                </template>
            </div>

            <!-- 第 2 步:影响账期 -->
            <div v-else-if="step === 1" class="flow-body">
                <template v-if="task.kind === 'change'">
                    <p class="lead">
                        该规则当前有 <strong>{{ affected.length }}</strong> 个未入账账期，将保留原金额快照，不受本次调整影响；之后的新账期按新规则生成。
                    </p>
                    <p v-if="affectedLoading" class="hint">正在加载账期…</p>
                    <el-alert v-if="affectedError" type="warning" :closable="false" :title="affectedError" />
                    <el-button v-if="affectedError" size="small" @click="loadAffected">重试加载</el-button>
                    <el-table v-if="affected.length" :data="affected" size="small" max-height="260">
                        <el-table-column label="账期" prop="dueDate" width="120" />
                        <el-table-column label="预计金额" prop="expectedAmount" width="110" />
                        <el-table-column label="状态">
                            <template #default="scope">{{ statusLabels[scope.row.status] || scope.row.status }}</template>
                        </el-table-column>
                    </el-table>
                </template>
                <template v-else>
                    <p class="lead">补齐后这些账期会出现在「周期账单」中，等待你逐期确认入账：</p>
                    <div class="period-list">
                        <el-tag v-for="p in task.preview.periods" :key="p" size="small" type="warning" effect="plain">{{ p }}</el-tag>
                    </div>
                    <p class="hint">规则保持暂停；剩余超过 100 期的部分可再次预览补齐。</p>
                </template>
            </div>

            <!-- 第 3 步:确认执行 -->
            <div v-else class="flow-body">
                <p class="lead">{{ task.kind === 'change' ? '确认后立即生效：' : '确认后生成待确认账期：' }}</p>
                <p class="notice">{{ task.preview.notice }}</p>
                <p class="hint">预览有效期约 15 分钟；来源若被再次修改，需要重新预览。</p>
                <el-alert v-if="commitError" type="error" :closable="false" :title="commitError" />
                <el-button v-if="commitError" size="small" @click="refreshPreview">重新预览</el-button>
            </div>
        </div>
        <template #footer>
            <el-button :disabled="busy" @click="cancel">取消</el-button>
            <el-button v-if="step > 0" :disabled="busy" @click="step--">上一步</el-button>
            <el-button v-if="step < 2" type="primary" :disabled="step === 0 && task?.kind === 'change' && !diffs.length" @click="next">
                下一步
            </el-button>
            <el-button v-else type="primary" :loading="committing" :disabled="committing" @click="confirmCommit">
                确认执行
            </el-button>
        </template>
    </el-dialog>
</template>
<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { ElMessage } from 'element-plus';
import request from '@/utils/request';
import { setRuleFlowPresenter, formatDiffs, CHANGE_PREVIEW_URL, BACKFILL_PREVIEW_URL, CHANGE_COMMIT_URL } from '@/utils/ruleChangeFlow';

const visible = ref(false),
    step = ref(0),
    task = ref(null),
    affected = ref([]),
    affectedLoading = ref(false),
    affectedError = ref(''),
    committing = ref(false),
    commitError = ref('');
let resolver = null;
const statusLabels = { pending: '待确认', payment_failed: '扣费失败', planned: '按计划' };
const diffs = computed(() => formatDiffs(task.value?.preview?.changes));
const busy = computed(() => affectedLoading.value || committing.value);
const revoke = setRuleFlowPresenter(open);

function open(nextTask) {
    return new Promise((resolve) => {
        task.value = nextTask;
        step.value = 0;
        affected.value = [];
        affectedError.value = '';
        commitError.value = '';
        resolver = resolve;
        visible.value = true;
        if (nextTask.kind === 'change') loadAffected();
    });
}
async function loadAffected() {
    affectedLoading.value = true;
    affectedError.value = '';
    try {
        const r = await request({
            url: '/charge/instances',
            method: 'get',
            params: { sourceType: task.value.sourceType, sourceId: task.value.sourceId, pageNum: 1, pageSize: 100 }
        });
        const rows = r.rows || r.data?.rows || [];
        affected.value = rows.filter((i) => ['pending', 'payment_failed'].includes(i.status));
    } catch (e) {
        affectedError.value = '账期加载失败，可重试；不影响后续确认。';
        affected.value = [];
    } finally {
        affectedLoading.value = false;
    }
}
function next() {
    step.value++;
    if (step.value === 2) commitError.value = '';
}
async function confirmCommit() {
    if (committing.value) return;
    committing.value = true;
    commitError.value = '';
    try {
        const r = await request({
            url: CHANGE_COMMIT_URL,
            method: 'post',
            data: { previewId: task.value.preview.previewId }
        });
        finish();
        ElMessage.success(task.value.kind === 'change' ? '规则已更新' : '账期已补齐');
        resolver?.({ status: 'committed', result: r });
        resolver = null;
    } catch (e) {
        commitError.value = e?.message || '提交失败，可重试同一预览或重新预览。';
    } finally {
        committing.value = false;
    }
}
async function refreshPreview() {
    const url = task.value.kind === 'change' ? CHANGE_PREVIEW_URL : BACKFILL_PREVIEW_URL;
    try {
        const r = await request({ url, method: 'post', data: task.value.input });
        task.value.preview = r.data;
        commitError.value = '';
        step.value = 0;
    } catch (e) {
        commitError.value = '重新预览失败：' + (e?.message || '请稍后重试');
    }
}
function finish() {
    visible.value = false;
}
function cancel() {
    if (busy.value) return;
    if (!visible.value && !resolver) return;
    finish();
    resolver?.({ status: 'cancelled' });
    resolver = null;
}
onBeforeUnmount(() => {
    revoke();
    resolver?.({ status: 'cancelled' });
});
</script>
<style scoped>
.flow-steps {
    margin-bottom: 16px;
}
.flow-body {
    min-height: 160px;
}
.lead {
    margin: 0 0 10px;
    font-size: 14px;
}
.notice {
    margin: 0 0 8px;
    padding: 10px 12px;
    background: var(--el-fill-color-light);
    border-radius: 8px;
    font-size: 13px;
}
.hint {
    margin: 8px 0;
    font-size: 13px;
    color: var(--el-text-color-secondary);
}
.period-list {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    margin-bottom: 8px;
}
.el-alert {
    margin-top: 8px;
}
</style>
