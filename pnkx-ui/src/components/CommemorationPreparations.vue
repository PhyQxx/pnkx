<template>
    <section v-if="items.length">
        <div v-for="i in items" :key="i.id" class="cp-item">
            <div class="cp-item__main">
                <div class="cp-item__title">{{ i.name }}</div>
                <div class="cp-item__meta">
                    <span class="cp-chip">{{ i.occurrenceDate }}</span>
                    <span class="cp-chip">{{ i.daysUntil === 0 ? '今天' : i.daysUntil + '天后' }}</span>
                    <span class="cp-tag" :class="{ 'cp-tag--ready': ['accepted', 'completed'].includes(i.preparationStatus) }">{{
                        labels[i.preparationStatus] || '尚未准备'
                    }}</span>
                </div>
            </div>
            <template v-if="canEdit && !['accepted', 'completed', 'removed'].includes(i.preparationStatus)"
                ><el-button link type="primary" @click="open(i)">{{
                    i.preparationStatus === 'dismissed' ? '重新准备' : '创建准备待办'
                }}</el-button
                ><el-button v-if="i.preparationStatus !== 'dismissed'" link @click="dismiss(i)"
                    >忽略本次</el-button
                ></template
            >
        </div>
        <el-dialog v-model="visible" title="准备纪念日" width="min(520px,94vw)" append-to-body>
            <p>{{ selected?.name }} · {{ selected?.occurrenceDate }}。确认后创建你负责的待办。</p>
            <el-input v-model="form.content" type="textarea" maxlength="2000" placeholder="具体准备事项" />
            <el-date-picker v-model="form.dueDate" type="date" value-format="YYYY-MM-DD" placeholder="准备完成日期" />
            <el-alert v-if="error" :title="error" type="warning" :closable="false" />
            <template #footer
                ><el-button @click="visible = false">取消</el-button
                ><el-button :loading="busy" type="primary" @click="confirm">预览并确认</el-button></template
            >
        </el-dialog>
    </section>
</template>
<script setup>
import { ref } from 'vue';
import { ElMessageBox } from 'element-plus';
import request from '@/utils/request';
defineProps({ items: { type: Array, default: () => [] }, canEdit: { type: Boolean, default: false } });
const emit = defineEmits(['changed']),
    visible = ref(false),
    selected = ref(null),
    form = ref({}),
    busy = ref(false),
    error = ref('');
const labels = {
    draft: '待确认',
    accepted: '准备中',
    completed: '准备完成',
    dismissed: '已忽略本次',
    removed: '准备待办已移除'
};
let preview = null;
function open(i) {
    selected.value = i;
    form.value = { content: `准备${i.name}`, dueDate: i.suggestedDueDate, occurrenceDate: i.occurrenceDate };
    preview = null;
    error.value = '';
    visible.value = true;
}
async function confirm() {
    if (busy.value) return;
    busy.value = true;
    try {
        const signature = JSON.stringify(form.value);
        if (!preview || preview.signature !== signature) {
            const r = await request({
                url: `/life/action-center/commemorations/${selected.value.id}/preview`,
                method: 'post',
                data: form.value
            });
            preview = { signature, ...r.data };
        }
        await ElMessageBox.confirm(`${preview.content}，完成日期：${preview.dueDate}`, '确认创建准备待办');
        await request({
            url: '/life/action-center/commemorations/commit',
            method: 'post',
            data: { previewId: preview.previewId }
        });
        visible.value = false;
        emit('changed');
    } catch (e) {
        if (e !== 'cancel' && e !== 'close')
            error.value = '未确认成功，可重试同一请求；若日期或来源已变化，请关闭后刷新再预览。';
    } finally {
        busy.value = false;
    }
}
async function dismiss(i) {
    await request({
        url: `/life/action-center/commemorations/${i.id}/dismiss`,
        method: 'post',
        data: { occurrenceDate: i.occurrenceDate }
    });
    emit('changed');
}
</script>
<style scoped>
.cp-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 10px 12px;
    margin-bottom: 8px;
    background: #f6f2ff;
    border: 1px solid transparent;
    border-radius: 10px;
    transition: border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
    animation: cp-in 0.25s ease both;
}
.cp-item:hover {
    border-color: #d9c9ff;
    box-shadow: var(--el-box-shadow-light);
    transform: translateY(-1px);
}
.cp-item__main {
    min-width: 0;
    flex: 1;
}
.cp-item__title {
    font-size: 14px;
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
.cp-item__meta {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: 4px;
    flex-wrap: wrap;
}
.cp-chip {
    padding: 1px 8px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    background: var(--el-fill-color);
    border-radius: 999px;
}
.cp-tag {
    padding: 1px 8px;
    font-size: 12px;
    color: #9a6bff;
    background: #f1ebff;
    border-radius: 4px;
}
.cp-tag--ready {
    color: var(--el-color-success);
    background: var(--el-color-success-light-9);
}
@keyframes cp-in {
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
