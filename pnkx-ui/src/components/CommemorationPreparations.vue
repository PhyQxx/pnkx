<template>
    <section v-if="items.length">
        <h3>未来30天纪念日准备</h3>
        <div v-for="i in items" :key="i.id" class="preparation">
            <span>{{ i.name }} · {{ i.occurrenceDate }} · {{ i.daysUntil === 0 ? '今天' : i.daysUntil + '天后' }}</span>
            <el-tag>{{ labels[i.preparationStatus] || '尚未准备' }}</el-tag>
            <template v-if="canEdit && !['accepted', 'completed', 'removed'].includes(i.preparationStatus)"
                ><el-button link @click="open(i)">{{
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
.preparation {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    padding: 10px 0;
}
</style>
