<template>
    <div class="item-actions" @click.stop>
        <div class="item-actions__status">
            <span>{{ labels[item.fulfillmentState] || '待购' }}</span>
            <span v-if="item.claimedBy" class="item-actions__claim">· 认领人 {{ item.claimedBy }}</span>
        </div>
        <template v-if="!item.batchId">
            <div class="item-actions__buttons">
                <van-button size="small" plain round @click="act(item.claimedBy ? 'release' : 'claim')">
                    {{ item.claimedBy ? '释放' : '认领' }}
                </van-button>
                <van-button size="small" plain round @click="act('fulfillment', 'available')">家里已有</van-button>
                <van-button
                    size="small"
                    plain
                    round
                    @click="act('fulfillment', item.fulfillmentState === 'purchased' ? 'needed' : 'purchased')"
                >
                    {{ item.fulfillmentState === 'purchased' ? '待购' : '已买到' }}
                </van-button>
            </div>
            <m-assignment-history type="shopping" :source-id="item.id" @changed="$emit('changed')" />
        </template>
        <div v-else class="item-actions__settled">已结算</div>
    </div>
</template>

<script>
import { actShoppingItem } from '@/api/px/life/shopping'
import MAssignmentHistory from '@/mobile/components/AssignmentHistory.vue'

export default {
    name: 'MShoppingItemActions',
    components: { MAssignmentHistory },
    props: {
        item: { type: Object, required: true }
    },
    emits: ['changed'],
    data() {
        return {
            labels: { needed: '待购', available: '家里已有', purchased: '已买到' },
            busy: false
        }
    },
    methods: {
        async act(action, state) {
            if (this.busy) return
            this.busy = true
            try {
                await actShoppingItem(this.item.id, action, {
                    requestId: `shopping_${Date.now()}_${Math.random().toString(36).slice(2)}`,
                    expectedRevision: this.item.revision,
                    state
                })
            } finally {
                this.busy = false
                this.$emit('changed')
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.item-actions {
    width: 100%;
    margin-top: 8px;
    padding-top: 8px;
    border-top: 1px dashed var(--pnkx-border-soft);

    &__status {
        font-size: 12px;
        color: var(--pnkx-text-secondary);
    }

    &__claim {
        color: var(--pnkx-primary);
    }

    &__buttons {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
        margin-top: 8px;
    }

    &__settled {
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }
}
</style>
