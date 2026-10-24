<template>
    <el-button :disabled="!eligible.length" @click="open()">结束采购（{{ eligible.length }} 项）</el-button
    ><el-button @click="loadBatches">我的采购批次</el-button>
    <div v-for="b in batches" :key="b.id">
        批次 {{ b.id }} · {{ b.create_time }} · {{ b.record_id ? '已记账' : '未记账'
        }}<el-button v-if="!b.record_id" link @click="open(b.id)">补记支出</el-button>
    </div>
    <el-dialog v-model="visible" title="采购结算" width="min(480px, 94vw)" append-to-body
        ><p v-if="!batchId">结算 {{ eligible.length }} 项已买到的物品。家里已有、已结算条目不包含在内。</p>
        <el-checkbox v-if="!batchId" v-model="form.recordExpense">同时记录本次支出</el-checkbox
        ><el-form v-if="form.recordExpense" label-width="90px"
            ><el-form-item label="实际总额"
                ><el-input-number v-model="form.totalAmount" :min="0" :precision="2" /></el-form-item
            ><el-form-item label="扣费日期"
                ><el-date-picker v-model="form.paidAt" type="date" value-format="YYYY-MM-DD" /></el-form-item
            ><el-form-item label="账户"
                ><el-select v-model="form.accountId"
                    ><el-option
                        v-for="a in accounts"
                        :key="a.id"
                        :value="a.id"
                        :label="a.accountName" /></el-select></el-form-item
            ><el-form-item label="支出分类"
                ><el-select v-model="form.classificationId"
                    ><el-option
                        v-for="c in categories"
                        :key="c.id"
                        :value="c.id"
                        :label="c.typeName" /></el-select></el-form-item></el-form
        ><template #footer
            ><el-button @click="visible = false">取消</el-button
            ><el-button type="primary" :loading="saving" @click="save">确认结算</el-button></template
        ></el-dialog
    >
</template>
<script setup>
import { ref, computed } from 'vue';
import request from '@/utils/request';
import { listAccount } from '@/api/px/life/bookkeeping/account';
import { listClassification } from '@/api/px/life/bookkeeping/classification';
const props = defineProps({
        items: { type: Array, default: () => [] },
        listId: { type: [Number, String], default: null }
    }),
    emit = defineEmits(['changed']);
const eligible = computed(() => props.items.filter((i) => i.fulfillmentState === 'purchased' && !i.batchId));
const batchId = ref(null),
    batches = ref([]);
async function loadBatches() {
    const r = await request({ url: `/shoppingList/${props.listId}/batches` });
    batches.value = r.data || [];
}
const visible = ref(false),
    saving = ref(false),
    form = ref({}),
    accounts = ref([]),
    categories = ref([]);
async function open(id = null) {
    batchId.value = id;
    form.value = {
        requestId: `purchase_${Date.now()}_${Math.random().toString(36).slice(2)}`,
        purchasedItemIds: eligible.value.map((i) => i.id),
        recordExpense: !!id,
        totalAmount: 0,
        paidAt: new Date().toLocaleDateString('sv-SE')
    };
    visible.value = true;
    const [a, c] = await Promise.all([
        listAccount({ pageSize: 100 }),
        listClassification({ pageSize: 100, typeDifference: '1' })
    ]);
    accounts.value = a.rows || [];
    categories.value = c.rows || [];
}
async function save() {
    if (saving.value) return;
    saving.value = true;
    try {
        await request({
            url: batchId.value
                ? `/shopping-batches/${batchId.value}/record-expense`
                : `/shoppingList/${props.listId}/checkout`,
            method: 'post',
            data: form.value
        });
        visible.value = false;
        await loadBatches();
        emit('changed');
    } finally {
        saving.value = false;
    }
}
</script>
