<template>
    <div class="shopping-actions"><assignment-history type="shopping" :source-id="item.id" @changed="emit('changed')" />
        <el-tag size="small">{{ labels[item.fulfillmentState] || '待购' }}</el-tag
        ><span v-if="item.claimedBy">认领人 {{ item.claimedBy }}</span
        ><template v-if="!item.batchId"
            ><el-button link @click="act(item.claimedBy ? 'release' : 'claim')">{{
                item.claimedBy ? '释放认领' : '认领'
            }}</el-button
            ><el-button link @click="act('fulfillment', 'available')">家里已有</el-button
            ><el-button
                link
                @click="act('fulfillment', item.fulfillmentState === 'purchased' ? 'needed' : 'purchased')"
                >{{ item.fulfillmentState === 'purchased' ? '改为待购' : '已买到' }}</el-button
            ></template
        ><el-tag v-else type="success">已结算</el-tag>
    </div>
</template>
<script setup>
import request from '@/utils/request';
const props = defineProps({ item: { type: Object, required: true } }),
    emit = defineEmits(['changed']);
const labels = { needed: '待购', available: '家里已有', purchased: '已买到' };
async function act(action, state) {
    try {
        await request({
            url: `/shoppingItem/${props.item.id}/${action}`,
            method: 'post',
            data: {
                requestId: `shopping_${Date.now()}_${Math.random().toString(36).slice(2)}`,
                expectedRevision: props.item.revision,
                state
            }
        });
    } finally {
        emit('changed');
    }
}
</script>
<style scoped>
.shopping-actions {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    align-items: center;
}
</style>
