<template>
    <div class="record-list">
        <div v-if="!recordList || recordList.length < 1" class="record-list__empty">今日还没有账单，记一笔吧</div>
        <div v-for="day in recordList" v-else :key="day.payTime" class="record-card">
            <div class="record-card__date">
                <div class="record-card__day">
                    <span class="record-card__day-num">{{ dayOf(day.payTime) }}日</span>
                    <span class="record-card__day-week"> / 周{{ weekOf(day.payTime) }}</span>
                </div>
                <div class="record-card__sum">
                    <span v-if="sumOf(day, '0')" class="record-card__sum-item">
                        <van-icon name="plus" />
                        <span>{{ moneyFilter(sumOf(day, '0')) }}</span>
                    </span>
                    <span v-if="sumOf(day, '1')" class="record-card__sum-item record-card__sum-item--expense">
                        <van-icon name="minus" />
                        <span>{{ moneyFilter(sumOf(day, '1')) }}</span>
                    </span>
                </div>
            </div>
            <div
                v-for="record in day.recordList || []"
                :key="record.id"
                :class="recordClass(record)"
                class="record-day"
                @click="handleEdit(record)"
            >
                <img class="record-day__icon" :src="typeIcon(record)" alt="">
                <div class="record-day__content">
                    <div v-if="isTransfer(record)" class="record-day__type">
                        <span>{{ record.accountObject && record.accountObject.accountName }}</span>
                        <van-icon class="record-day__arrow" name="arrow" />
                        <span>{{ record.otherAccountObject && record.otherAccountObject.accountName }}</span>
                    </div>
                    <div v-else class="record-day__type">{{ record.typeObject && record.typeObject.typeName }}</div>
                    <div class="record-day__remark">{{ record.remark ? record.remark : '' }}</div>
                    <div class="record-day__money">{{ moneyFilter(record.money) }}</div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import { iconUrl } from '@/mobile/utils/icons'
import { showToast } from '@/mobile/utils/ui'

/** 钱数千分位格式化（与 uniapp utils/filters 的 moneyFilter 一致） */
function moneyFilter(valueMoney) {
    let result = ''
    let counter = 0
    let negativeFlag = false
    if (valueMoney === undefined || valueMoney === null || valueMoney === '' || valueMoney === 'null') {
        valueMoney = 0
    }
    if (typeof valueMoney === 'string') {
        if (valueMoney.slice(0, 1) === '-') {
            valueMoney = valueMoney.slice(1)
            negativeFlag = true
        }
        valueMoney = parseFloat(valueMoney).toFixed(2).toString()
    }
    if (typeof valueMoney === 'number') {
        if (valueMoney < 0) {
            valueMoney = valueMoney * -1
            negativeFlag = true
        }
        valueMoney = valueMoney.toFixed(2).toString()
    }
    const decimal = valueMoney.slice(valueMoney.indexOf('.'))
    valueMoney = valueMoney.slice(0, valueMoney.indexOf('.'))
    for (let i = valueMoney.length - 1; i >= 0; i--) {
        counter++
        result = valueMoney.charAt(i) + result
        if (!(counter % 3) && i !== 0) {
            result = ',' + result
        }
    }
    if (negativeFlag) {
        result = '-' + result
    }
    return result + decimal
}

export default {
    name: 'MHomeRecordList',
    props: {
        // 按日分组：[{ payTime: 'yyyy-MM-dd', recordList: [...] }]
        recordList: {
            type: Array,
            default: () => []
        }
    },
    methods: {
        moneyFilter,
        dayOf(payTime) {
            return this.$parseTime(payTime, '{d}')
        },
        weekOf(payTime) {
            return this.$parseTime(payTime, '{a}')
        },
        /** 按收支类型汇总（'0'/'1'），金额为 0 时不展示 */
        sumOf(day, typeDifference) {
            const rows = (day.recordList || []).filter(item => item.typeObject && item.typeObject.typeDifference === typeDifference)
            if (!rows.length) return 0
            return this.$arraySum(rows, 'money')
        },
        isTransfer(record) {
            return record.typeObject && record.typeObject.typeDifference === '2'
        },
        recordClass(record) {
            const diff = record.typeObject && record.typeObject.typeDifference
            if (diff === '1') return 'record-day--expense'
            if (diff === '0') return 'record-day--income'
            if (diff === '2') return 'record-day--transfer'
            return ''
        },
        typeIcon(record) {
            return iconUrl(record.typeObject && record.typeObject.typeIcon) || iconUrl('moren')
        },
        /** 跳转编辑；余额变更记录不可编辑 */
        handleEdit(record) {
            if (record.typeObject && record.typeObject.typeDifference === '3') {
                showToast('抱歉，余额变更不可编辑')
                return
            }
            const path = '/m/bookkeeping/record/add?recordId=' + record.id
            if (this.$router.resolve(path).matched.length > 0) {
                this.$router.push(path)
            } else {
                showToast('功能即将上线')
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.record-list__empty {
    padding: 18px 0;
    text-align: center;
    font-size: 12px;
    color: var(--pnkx-text-muted);
}

.record-card {
    &:not(:first-child) { margin-top: 12px; }

    &__date {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0 12px 8px;
        border-bottom: 1px solid var(--pnkx-border);
    }

    &__day {
        display: flex;
        align-items: baseline;

        &-num {
            font-size: 17px;
            font-weight: 700;
            color: var(--pnkx-text);
        }

        &-week {
            font-size: 12px;
            color: var(--pnkx-text-muted);
        }
    }

    &__sum {
        display: flex;
        align-items: center;
        font-size: 13px;
        color: var(--pnkx-text-secondary);
    }

    &__sum-item {
        display: inline-flex;
        align-items: center;
        gap: 2px;
        color: var(--pnkx-danger);

        .van-icon { font-size: 11px; }

        &--expense {
            margin-left: 8px;
            color: var(--pnkx-primary);
        }
    }
}

.record-day {
    display: flex;
    align-items: center;
    padding: 10px 12px;
    border-bottom: 1px solid var(--pnkx-border-soft);

    &:last-child { border-bottom: 0; }

    &:active { opacity: 0.7; }

    &__icon {
        width: 26px;
        height: 26px;
        object-fit: contain;
        margin-right: 10px;
        flex-shrink: 0;
    }

    &__content {
        flex: 1;
        min-width: 0;
        display: flex;
        align-items: center;
        justify-content: space-between;
    }

    &__type {
        max-width: 60%;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-size: 14px;
        color: var(--pnkx-text);
        display: flex;
        align-items: center;
    }

    &__arrow {
        margin: 0 4px;
        color: var(--pnkx-text-muted);
    }

    &__remark {
        flex: 1;
        margin: 0 8px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }

    &__money {
        min-width: 72px;
        text-align: right;
        font-size: 14px;
        font-weight: 600;
    }

    &--expense .record-day__money { color: var(--pnkx-primary); }
    &--income .record-day__money { color: var(--pnkx-danger); }
    &--transfer .record-day__money { color: var(--pnkx-text-muted); }
}
</style>
