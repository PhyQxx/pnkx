<template>
    <section v-if="schema.length" class="draft-editor">
        <el-button v-if="!editing" :disabled="disabled" @click="begin">修改草稿</el-button
        ><template v-else
            ><p>保存后将生成新预览，旧草稿失效。请再次核对后确认执行。</p>
            <el-form label-position="top" :disabled="saving"
                ><el-form-item v-for="f in schema" :key="f.key" :label="f.label"
                    ><el-switch v-if="f.boolean" v-model="values[f.key]" /><el-date-picker
                        v-else-if="f.date || f.datetime"
                        v-model="values[f.key]"
                        :type="f.datetime ? 'datetime' : 'date'"
                        :value-format="f.datetime ? 'YYYY-MM-DD HH:mm:ss' : 'YYYY-MM-DD'"
                        placeholder="选择时间"
                        style="width: 100%"
                    /><el-input
                        v-else
                        v-model="values[f.key]"
                        :type="f.multiline ? 'textarea' : 'text'"
                        :rows="4" /></el-form-item></el-form
            ><el-alert v-if="error" :title="error" type="error" :closable="false" /><el-button
                :loading="saving"
                @click="save"
                >保存并重新预览</el-button
            ><el-button :disabled="saving" @click="finish">取消修改</el-button></template
        >
    </section>
</template>
<script setup>
import { ref, computed, watch } from 'vue';
import request from '@/utils/request';
import { editableFields, draftValues, revisionBody } from '../utils/planEditing';
const props = defineProps({ plan: Object, disabled: Boolean }),
    emit = defineEmits(['updated', 'editing']);
const editing = ref(false),
    saving = ref(false),
    values = ref({}),
    error = ref(''),
    schema = computed(() => editableFields(props.plan));
function begin() {
    values.value = draftValues(props.plan);
    editing.value = true;
    error.value = '';
    emit('editing', true);
}
function finish() {
    editing.value = false;
    emit('editing', false);
}
watch(() => props.plan?.id, finish);
async function save() {
    if (saving.value) return;
    saving.value = true;
    error.value = '';
    try {
        const r = await request({
            url: `/operation-plans/${props.plan.id}/revise`,
            method: 'post',
            data: revisionBody(props.plan, values.value)
        });
        finish();
        emit('updated', r.data);
    } catch (e) {
        error.value = e?.message || '保存失败，草稿已保留，请重试';
    } finally {
        saving.value = false;
    }
}
</script>
<style scoped>
.draft-editor {
    margin: 16px 0;
    padding: 16px;
    background: var(--el-fill-color-lighter);
    border-radius: 10px;
}
.draft-editor p {
    font-size: 13px;
    line-height: 1.6;
}
</style>
