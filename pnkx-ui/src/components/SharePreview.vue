<template>
    <el-button @click="open">预览我的共享记录与引用</el-button>
    <el-dialog v-model="visible" title="共享内容预览" width="min(760px,94vw)" append-to-body>
        <el-alert
            title="按业务域共享现有及未来记录。下方仅展示你自己的内容，不是逐条授权清单。"
            type="info"
            :closable="false"
        />
        <el-select
            v-model="module"
            @change="
                page = 1;
                load();
            "
            ><el-option v-for="m in modules" :key="m" :label="labels[m] || m" :value="m"
        /></el-select>
        <el-alert v-if="error" :title="error" type="error" :closable="false" />
        <p>{{ data.referenceNotice }}</p>
        <div v-for="item in data.items || []" :key="item.id" class="object">
            <strong>记录 #{{ item.id }}</strong>
            <dl>
                <template v-for="(value, key) in item.fields" :key="key"
                    ><dt>{{ fieldLabels[key] || key }}</dt>
                    <dd>{{ value == null ? '—' : String(value) }}</dd></template
                >
            </dl>
            <p v-if="item.truncated">长正文已截断；其余内容仍可能被共享，请在原记录中核对。</p>
            <div v-if="item.references.length">
                <strong>识别到的引用（未加载）</strong>
                <p v-for="ref in item.references" :key="ref">{{ ref }}</p>
            </div>
        </div>
        <el-empty v-if="!loading && !(data.items || []).length" description="此范围暂无可预览记录" />
        <el-button
            :disabled="page === 1 || loading"
            @click="
                page--;
                load();
            "
            >上一页</el-button
        ><span>第{{ page }}页 · 共{{ data.total || 0 }}条</span
        ><el-button
            :disabled="page * 20 >= (data.total || 0) || loading"
            @click="
                page++;
                load();
            "
            >下一页</el-button
        >
    </el-dialog>
</template>
<script setup>
import { ref, watch } from 'vue';
import request from '@/utils/request';
const props = defineProps({
    modules: { type: Array, default: () => [] },
    groupId: { type: [Number, String], default: null }
});
const visible = ref(false),
    module = ref(''),
    page = ref(1),
    loading = ref(false),
    error = ref(''),
    data = ref({});
let serial = 0;
const labels = { todo: '待办', shopping: '购物条目', meal: '餐饮', commemoration: '纪念日', budget: '预算' };
const fieldLabels = {
    id: '编号',
    content: '内容',
    performer: '执行者',
    plan_start_time: '开始时间',
    plan_end_time: '截止时间',
    status: '状态',
    label: '标签',
    remark: '备注',
    list_id: '清单编号',
    name: '名称',
    quantity: '数量',
    fulfillment_state: '采购状态',
    claimed_by: '认领人',
    title: '标题',
    plan_date: '日期',
    meal_type: '餐次',
    servings: '份数',
    notes: '说明',
    date: '日期',
    repeat: '每年重复',
    month: '月份',
    type_id: '分类编号',
    amount: '预算金额'
};
async function open() {
    module.value = props.modules[0] || '';
    page.value = 1;
    visible.value = true;
    await load();
}
async function load() {
    const id = ++serial;
    data.value = {};
    error.value = '';
    if (!module.value) {
        loading.value = false;
        return;
    }
    loading.value = true;
    try {
        const r = await request({
            url: '/system/dataGroup/share-preview',
            params: { module: module.value, groupId: props.groupId || undefined, pageNum: page.value }
        });
        if (id === serial) data.value = r.data;
    } catch (_) {
        if (id === serial) error.value = '预览加载失败，请重试';
    } finally {
        if (id === serial) loading.value = false;
    }
}
watch(
    () => [props.groupId, JSON.stringify(props.modules)],
    () => {
        serial++;
        visible.value = false;
        data.value = {};
    }
);
</script>
<style scoped>
.object {
    margin: 12px 0;
    padding: 14px;
    border: 1px solid #ddd;
    border-radius: 6px;
    overflow-wrap: anywhere;
}
dl {
    display: grid;
    grid-template-columns: 100px 1fr;
}
dt,
dd {
    margin: 0;
    padding: 5px;
    white-space: pre-wrap;
}
</style>
