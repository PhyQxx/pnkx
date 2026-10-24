<template>
    <el-button link @click="open">分工与转派记录</el-button>
    <el-dialog v-model="visible" title="分工与转派记录" width="min(640px, 94vw)" append-to-body>
        <el-alert v-if="error" :title="error" type="error" :closable="false" />
        <p>当前负责人：{{ data.assignee || '未分配' }}</p>
        <template v-if="data.canTransfer">
            <el-select v-model="target" placeholder="选择新的负责人"
                ><el-option
                    v-for="c in data.candidates || []"
                    :key="c.userId"
                    :label="'成员 ' + c.userId"
                    :value="String(c.userId)"
            /></el-select>
            <el-input
                v-model="reason"
                type="textarea"
                maxlength="500"
                show-word-limit
                placeholder="填写转派原因（必填）"
                style="margin: 12px 0"
            />
            <el-button type="primary" :loading="busy" :disabled="!target || !reason.trim()" @click="transfer"
                >确认转派</el-button
            >
        </template>
        <el-empty v-if="!data.history?.length" description="暂无分工变更记录" />
        <div v-for="h in data.history || []" :key="h.id" style="padding: 12px 0; border-bottom: 1px solid #eee">
            <div>{{ h.previous_assignee || '未分配' }} → {{ h.assignee || '未分配' }} · 操作人 {{ h.operator_id }}</div>
            <div>{{ h.reason }}</div>
            <small>{{ h.create_time }}</small>
        </div>
        <el-button
            :disabled="page === 1"
            @click="
                page--;
                load();
            "
            >上一页</el-button
        ><el-button
            :disabled="(data.history || []).length < 30"
            @click="
                page++;
                load();
            "
            >下一页</el-button
        >
    </el-dialog>
</template>
<script setup>
import { ref } from 'vue';
import request from '@/utils/request';
const props = defineProps({
        type: { type: String, required: true },
        sourceId: { type: [String, Number], required: true }
    }),
    emit = defineEmits(['changed']);
const visible = ref(false),
    busy = ref(false),
    error = ref(''),
    data = ref({}),
    target = ref(''),
    reason = ref(''),
    page = ref(1);
let attempt = null;
async function load() {
    try {
        data.value = (
            await request({ url: `/life/assignments/${props.type}/${props.sourceId}`, params: { pageNum: page.value } })
        ).data;
        error.value = '';
    } catch (_) {
        data.value = {};
        error.value = '分工记录加载失败，请关闭后重试';
    }
}
async function open() {
    page.value = 1;
    visible.value = true;
    target.value = '';
    reason.value = '';
    attempt = null;
    await load();
}
async function transfer() {
    const body = { assignee: target.value, expectedAssignee: data.value.assignee ?? null, reason: reason.value },
        hash = JSON.stringify(body);
    if (!attempt || attempt.hash !== hash)
        attempt = { hash, body: { ...body, requestId: `assign_${Date.now()}_${Math.random().toString(36).slice(2)}` } };
    busy.value = true;
    try {
        data.value = (
            await request({
                url: `/life/assignments/${props.type}/${props.sourceId}/transfer`,
                method: 'post',
                data: attempt.body
            })
        ).data;
        reason.value = '';
        target.value = '';
        attempt = null;
        page.value = 1;
        emit('changed', data.value.assignee);
    } catch (_) {
        error.value = '转派未确认成功。可重试原请求；若负责人已变化，请关闭后重新打开核对。';
    } finally {
        busy.value = false;
    }
}
</script>
