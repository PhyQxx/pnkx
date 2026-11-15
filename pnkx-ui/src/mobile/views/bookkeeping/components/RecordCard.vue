<template>
    <div class="record-card" @click="$emit('click', record)">
        <div class="record-card__icon">
            <img v-if="iconSrc" :src="iconSrc" :alt="typeName">
            <van-icon v-else :name="isTransfer ? 'exchange' : 'bill-o'" />
        </div>
        <div class="record-card__info">
            <div class="record-card__name">
                <template v-if="isTransfer">{{ accountName }} → {{ otherAccountName }}</template>
                <template v-else>{{ typeName || '未知' }}</template>
            </div>
            <div class="record-card__subtitle">
                <slot name="subtitle">
                    <span>{{ subtitle || record.remark || '无备注' }}</span>
                </slot>
            </div>
        </div>
        <div class="record-card__amount" :class="'record-card__amount--' + amountClass">
            {{ amountPrefix }}{{ moneyFilter(record.money) }}
        </div>
    </div>
</template>

<script>
import { moneyFilter } from '@/utils/filters'
import { iconUrl } from '@/mobile/utils/icons'

export default {
    name: 'BkRecordCard',
    props: {
        // 流水记录对象（含 typeObject/accountObject/otherAccountObject）
        record: { type: Object, required: true },
        // 副标题（默认取备注）
        subtitle: { type: String, default: '' }
    },
    emits: ['click'],
    computed: {
        moneyFilter() {
            return moneyFilter
        },
        isTransfer() {
            const r = this.record
            return !!r.otherAccount || (r.typeObject && r.typeObject.typeDifference === '2')
        },
        typeName() {
            return (this.record.typeObject && this.record.typeObject.typeName) || ''
        },
        accountName() {
            return (this.record.accountObject && this.record.accountObject.accountName) || ''
        },
        otherAccountName() {
            return (this.record.otherAccountObject && this.record.otherAccountObject.accountName) || ''
        },
        iconSrc() {
            if (this.isTransfer) return iconUrl('c-zhuanzhang')
            const icon = this.record.typeObject && this.record.typeObject.typeIcon
            return icon ? iconUrl(icon) : ''
        },
        amountClass() {
            const diff = this.record.typeObject && this.record.typeObject.typeDifference
            if (this.isTransfer) return 'transfer'
            if (diff === '1') return 'expense'
            if (diff === '0') return 'income'
            return 'transfer'
        },
        amountPrefix() {
            if (this.isTransfer) return ''
            const diff = this.record.typeObject && this.record.typeObject.typeDifference
            return diff === '1' ? '-' : '+'
        }
    }
}
</script>

<style lang="scss" scoped>
.record-card {
    display: flex;
    align-items: center;
    padding: 12px 14px;

    &:active {
        background: var(--pnkx-surface-muted);
    }

    &__icon {
        width: 38px;
        height: 38px;
        flex-shrink: 0;
        border-radius: 50%;
        background: var(--pnkx-surface-muted);
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 10px;
        color: var(--pnkx-primary);
        font-size: 18px;
        overflow: hidden;

        img {
            width: 24px;
            height: 24px;
            object-fit: contain;
        }
    }

    &__info {
        flex: 1;
        min-width: 0;
    }

    &__name {
        font-size: 14px;
        font-weight: 500;
        color: var(--pnkx-text);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__subtitle {
        margin-top: 2px;
        font-size: 12px;
        color: var(--pnkx-text-muted);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__amount {
        flex-shrink: 0;
        margin-left: 8px;
        font-size: 15px;
        font-weight: 600;

        &--expense {
            color: var(--pnkx-danger);
        }

        &--income {
            color: var(--pnkx-success);
        }

        &--transfer {
            color: var(--pnkx-text-secondary);
        }
    }
}
</style>
