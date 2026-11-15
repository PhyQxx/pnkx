<template>
    <div class="record-page">
        <!-- 月份汇总头部 -->
        <div class="record-header">
            <div class="record-header__top">
                <div class="record-header__export" @click="handleExport">
                    <van-icon name="down" />
                    <span>导出</span>
                </div>
                <div class="record-header__month-selector">
                    <div class="record-header__arrow" @click="changeMonth(-1)">
                        <van-icon name="arrow-left" />
                    </div>
                    <span class="record-header__month-text">{{ currentMonthStr }}</span>
                    <div class="record-header__arrow" @click="changeMonth(1)">
                        <van-icon name="arrow" />
                    </div>
                </div>
            </div>
            <div class="record-header__summary">
                <div class="record-header__summary-item">
                    <span class="record-header__label">支出</span>
                    <span class="record-header__value record-header__value--expense">{{ moneyFilter(expenditure) }}</span>
                </div>
                <div class="record-header__summary-item">
                    <span class="record-header__label">收入</span>
                    <span class="record-header__value record-header__value--income">{{ moneyFilter(income) }}</span>
                </div>
            </div>
        </div>

        <!-- 记录列表 -->
        <van-pull-refresh v-model="isRefreshing" class="m-pull" @refresh="onRefresh">
            <div v-if="!loading && recordList.length === 0" class="m-empty record-empty">
                <van-icon name="orders-o" size="56" />
                <span>暂无记录</span>
                <span class="record-empty__hint">点击下方按钮开始记账</span>
                <van-button size="small" round plain type="primary" icon="plus" @click="handleAdd">记一笔</van-button>
            </div>

            <van-list
                v-model:loading="loading"
                :finished="finished"
                finished-text="没有更多了"
                @load="loadMore"
            >
                <div v-for="day in recordList" :key="day.date" class="record-day">
                    <div class="record-day__header">
                        <div class="record-day__left">
                            <span class="record-day__number">{{ getDayNumber(day.date) }}</span>
                            <span class="record-day__week">{{ getWeekDay(day.date) }}</span>
                        </div>
                        <div class="record-day__right">
                            <div v-if="day.expenditure > 0" class="record-day__summary">
                                <van-icon name="minus" />
                                <span class="record-day__expense">{{ moneyFilter(day.expenditure) }}</span>
                            </div>
                            <div v-if="day.income > 0" class="record-day__summary">
                                <van-icon name="plus" />
                                <span class="record-day__income">{{ moneyFilter(day.income) }}</span>
                            </div>
                        </div>
                        <div class="record-day__spending-bar">
                            <div class="record-day__bar-expense" :style="{ width: getExpenseRatio(day) + '%' }" />
                            <div class="record-day__bar-income" :style="{ width: getIncomeRatio(day) + '%' }" />
                        </div>
                    </div>

                    <van-swipe-cell v-for="record in day.records" :key="record.id">
                        <template #right>
                            <van-button square type="primary" text="编辑" class="record-swipe__btn" @click="handleEdit(record)" />
                            <van-button square type="danger" text="删除" class="record-swipe__btn" @click="handleSwipeDelete(record)" />
                        </template>
                        <record-card :record="record" @click="handleEdit(record)" />
                    </van-swipe-cell>
                </div>
            </van-list>
        </van-pull-refresh>

        <!-- 记一笔 FAB -->
        <button class="m-fab m-fab--no-tabbar" @click="handleAdd">
            <van-icon name="plus" />
        </button>
    </div>
</template>

<script>
import { listRecord, delRecord, exportRecord } from '@/api/px/life/bookkeeping/record'
import { moneyFilter } from '@/utils/filters'
import { mConfirm, mError, mLoading, mSuccess, closeToast } from '@/mobile/utils/ui'
import RecordCard from './components/RecordCard.vue'

export default {
    name: 'MBookkeepingRecord',
    components: { RecordCard },
    data() {
        return {
            currentMonth: new Date(),
            recordList: [],
            expenditure: 0,
            income: 0,
            queryParams: {
                pageNum: 1,
                pageSize: 20,
                payTime: ''
            },
            total: 0,
            loading: false,
            finished: false,
            isRefreshing: false
        }
    },
    computed: {
        currentMonthStr() {
            const date = this.currentMonth
            return `${date.getFullYear()}年${date.getMonth() + 1}月`
        }
    },
    mounted() {
        // 首屏第一页由 van-list 挂载时自动触发的 load 完成
    },
    methods: {
        moneyFilter,
        async getRecordList() {
            const year = this.currentMonth.getFullYear()
            const month = String(this.currentMonth.getMonth() + 1).padStart(2, '0')
            this.queryParams.payTime = `${year}-${month}`

            try {
                const res = await listRecord(this.queryParams)
                this.total = res.total || 0

                const groupedRecords = this.groupByDate(res.rows || [])

                if (this.queryParams.pageNum === 1) {
                    this.recordList = groupedRecords
                } else {
                    this.recordList = [...this.recordList, ...groupedRecords]
                }

                this.calculateSummary(res.rows)
                this.finished = this.recordList.length >= this.total
            } catch (e) {
                this.finished = true
            } finally {
                this.loading = false
                this.isRefreshing = false
            }
        },
        groupByDate(records) {
            const grouped = {}

            records.forEach(record => {
                const date = this.parseTime(record.payTime, '{y}-{m}-{d}')
                if (!grouped[date]) {
                    grouped[date] = {
                        date,
                        records: [],
                        expenditure: 0,
                        income: 0
                    }
                }
                grouped[date].records.push(record)

                const typeDiff = record.typeObject && record.typeObject.typeDifference
                if (typeDiff === '1') {
                    grouped[date].expenditure += record.money
                } else if (typeDiff === '0') {
                    grouped[date].income += record.money
                }
            })

            return Object.values(grouped).sort((a, b) =>
                new Date(b.date).getTime() - new Date(a.date).getTime()
            )
        },
        calculateSummary(records) {
            this.expenditure = this.arraySum(
                (records || []).filter(r => r.typeObject && r.typeObject.typeDifference === '1'),
                'money'
            )
            this.income = this.arraySum(
                (records || []).filter(r => r.typeObject && r.typeObject.typeDifference === '0'),
                'money'
            )
        },
        changeMonth(delta) {
            const newMonth = new Date(this.currentMonth)
            newMonth.setMonth(newMonth.getMonth() + delta)
            this.currentMonth = newMonth
            this.refreshList()
        },
        getDayNumber(date) {
            return new Date(date).getDate()
        },
        getWeekDay(date) {
            const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
            return weekDays[new Date(date).getDay()]
        },
        handleEdit(record) {
            if (record.typeObject && record.typeObject.typeDifference === '3') {
                mError('余额变更不可编辑')
                return
            }
            this.$router.push(`/m/bookkeeping/record-add?recordId=${record.id}`)
        },
        getExpenseRatio(day) {
            const total = day.expenditure + day.income
            if (total === 0) return 0
            return Math.round((day.expenditure / total) * 100)
        },
        getIncomeRatio(day) {
            const total = day.expenditure + day.income
            if (total === 0) return 0
            return Math.round((day.income / total) * 100)
        },
        handleAdd() {
            this.$router.push('/m/bookkeeping/record-add')
        },
        async handleExport() {
            const year = this.currentMonth.getFullYear()
            const month = String(this.currentMonth.getMonth() + 1).padStart(2, '0')
            mLoading('正在导出')
            try {
                const res = await exportRecord({ payTime: `${year}-${month}` })
                this.download(res.msg)
            } catch (e) {
                mError('导出失败，请稍后重试')
            } finally {
                closeToast()
            }
        },
        async handleSwipeDelete(record) {
            try {
                await mConfirm('删除确认', '确定删除该记录吗？')
            } catch (e) {
                return
            }
            await delRecord(record.id)
            mSuccess('删除成功')
            this.refreshList()
        },
        async loadMore() {
            if (this.finished) return
            // 先取当前页，再翻页，配合 van-list 的自动首屏 load
            await this.getRecordList()
            this.queryParams.pageNum++
        },
        async onRefresh() {
            await this.refreshList()
        },
        async refreshList() {
            this.queryParams.pageNum = 1
            // 刷新期间置 finished，防止 van-list 在翻页指针就位前触发 load
            this.finished = true
            this.loading = true
            try {
                await this.getRecordList()
                this.queryParams.pageNum = 2
            } finally {
                this.loading = false
                this.isRefreshing = false
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.record-page {
    min-height: calc(100vh - 46px);
    background: var(--pnkx-bg);
    padding-bottom: 80px;
}

.record-header {
    padding: 14px 16px;
    background: linear-gradient(135deg, var(--pnkx-primary) 0%, var(--pnkx-info) 100%);
    color: #fff;

    &__top {
        position: relative;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 12px;
    }

    &__export {
        position: absolute;
        right: 0;
        top: 50%;
        transform: translateY(-50%);
        display: flex;
        align-items: center;
        gap: 3px;
        padding: 5px 12px;
        border-radius: 999px;
        background: rgba(255, 255, 255, 0.18);
        font-size: 12px;
    }

    &__month-selector {
        display: flex;
        align-items: center;
        justify-content: center;
    }

    &__arrow {
        width: 30px;
        height: 30px;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.2);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 14px;

        &:active {
            transform: scale(0.9);
        }
    }

    &__month-text {
        font-size: 18px;
        font-weight: 600;
        margin: 0 16px;
    }

    &__summary {
        display: flex;
        justify-content: space-around;
        padding: 12px;
        background: rgba(255, 255, 255, 0.15);
        border-radius: 12px;
    }

    &__summary-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 4px;
    }

    &__label {
        font-size: 12px;
        opacity: 0.82;
    }

    &__value {
        font-size: 18px;
        font-weight: 600;
    }
}

.record-empty {
    &__hint {
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }
}

.record-day {
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 12px;
    margin: 12px;
    overflow: hidden;

    &__header {
        display: flex;
        flex-direction: column;
        padding: 12px 14px;
        border-bottom: 1px solid var(--pnkx-border-soft);
    }

    &__left {
        display: flex;
        align-items: baseline;
        gap: 5px;
    }

    &__number {
        font-size: 24px;
        font-weight: 600;
        color: var(--pnkx-text);
        line-height: 1.1;
    }

    &__week {
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }

    &__right {
        display: flex;
        gap: 12px;
        margin-left: auto;
    }

    &__summary {
        display: flex;
        align-items: center;
        gap: 2px;
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }

    &__expense {
        color: var(--pnkx-danger);
    }

    &__income {
        color: var(--pnkx-success);
    }

    &__spending-bar {
        display: flex;
        height: 4px;
        background: var(--pnkx-surface-muted);
        border-radius: 999px;
        overflow: hidden;
        margin-top: 8px;
    }

    &__bar-expense {
        background: var(--pnkx-danger);
        transition: width 0.3s ease;
    }

    &__bar-income {
        background: var(--pnkx-success);
        transition: width 0.3s ease;
    }
}

.record-swipe__btn {
    height: 100%;
}
</style>
