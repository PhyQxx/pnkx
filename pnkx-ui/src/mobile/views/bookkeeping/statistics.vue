<template>
    <div class="stat-page">
        <!-- 月份汇总头部 -->
        <div class="stat-header">
            <div class="stat-header__month-selector">
                <div class="stat-header__arrow" @click="changeMonth(-1)">
                    <van-icon name="arrow-left" />
                </div>
                <span class="stat-header__month-text">{{ currentMonthStr }}</span>
                <div class="stat-header__arrow" @click="changeMonth(1)">
                    <van-icon name="arrow" />
                </div>
            </div>
            <div class="stat-header__summary">
                <div class="stat-header__summary-item">
                    <span class="stat-header__label">收入</span>
                    <span class="stat-header__value">{{ moneyFilter(income) }}</span>
                </div>
                <div class="stat-header__summary-item">
                    <span class="stat-header__label">支出</span>
                    <span class="stat-header__value">{{ moneyFilter(expenditure) }}</span>
                </div>
            </div>
        </div>

        <div class="m-page">
            <!-- 环比趋势卡 -->
            <div class="stat-trend" v-if="prevIncome !== null">
                <div class="stat-trend__card">
                    <span class="stat-trend__label">较上月支出</span>
                    <div class="stat-trend__value" :class="expenseTrend > 0 ? 'stat-trend__value--up' : 'stat-trend__value--down'">
                        <van-icon :name="expenseTrend > 0 ? 'arrow-up' : 'arrow-down'" />
                        <span>{{ Math.abs(expenseTrend) }}%</span>
                    </div>
                </div>
                <div class="stat-trend__card">
                    <span class="stat-trend__label">较上月收入</span>
                    <div class="stat-trend__value" :class="incomeTrend > 0 ? 'stat-trend__value--up' : 'stat-trend__value--down'">
                        <van-icon :name="incomeTrend > 0 ? 'arrow-up' : 'arrow-down'" />
                        <span>{{ Math.abs(incomeTrend) }}%</span>
                    </div>
                </div>
            </div>

            <!-- 图表卡片 -->
            <div class="stat-chart m-card">
                <van-tabs v-model:active="activeTab" shrink>
                    <van-tab title="趋势图" name="line" />
                    <van-tab title="分类图" name="pie" />
                </van-tabs>

                <div v-show="activeTab === 'line'" class="stat-chart__content">
                    <div ref="lineChartRef" class="stat-chart__canvas" />
                    <div v-if="lineData.length === 0" class="m-empty stat-chart__empty">本月暂无收支数据</div>
                </div>

                <div v-show="activeTab === 'pie'" class="stat-chart__content">
                    <div v-if="pieData.length > 0" class="stat-chart__donut">
                        <div ref="pieChartRef" class="stat-chart__canvas" />
                        <div class="stat-chart__donut-center">
                            <div class="stat-chart__donut-total">{{ moneyFilter(totalExpense) }}</div>
                            <div class="stat-chart__donut-label">总支出</div>
                        </div>
                    </div>
                    <div v-else class="m-empty">本月暂无支出数据</div>

                    <div class="stat-legend" v-if="pieData.length > 0">
                        <div class="stat-legend__item" v-for="(item, index) in pieData" :key="index">
                            <span class="stat-legend__dot" :style="{ backgroundColor: colorOf(index) }" />
                            <span class="stat-legend__name">{{ item.name }}</span>
                            <span class="stat-legend__value">{{ moneyFilter(item.value) }}</span>
                            <span class="stat-legend__percent">{{ getPercent(item.value) }}%</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import echarts from '@/utils/echarts'
import { getLineChart, getPrimaryStatistics } from '@/api/px/life/bookkeeping/statistics'
import { listRecord } from '@/api/px/life/bookkeeping/record'
import { moneyFilter } from '@/utils/filters'

export default {
    name: 'MBookkeepingStatistics',
    data() {
        return {
            currentDate: new Date(),
            income: 0,
            expenditure: 0,
            prevIncome: null,
            prevExpenditure: null,
            activeTab: 'line',
            lineData: [],
            pieData: [],
            colors: ['#4F86F7', '#5AD8A6', '#8589FF', '#FBBF24', '#F472B6', '#38BDF8', '#34D399', '#A78BFA'],
            lineChart: null,
            pieChart: null
        }
    },
    computed: {
        currentMonthStr() {
            const date = this.currentDate
            return `${date.getFullYear()}年${date.getMonth() + 1}月`
        },
        totalExpense() {
            return this.pieData.reduce((sum, item) => sum + (item.value || 0), 0)
        },
        expenseTrend() {
            if (this.prevExpenditure === null || this.prevExpenditure === 0) return 0
            return Math.round(((this.expenditure - this.prevExpenditure) / this.prevExpenditure) * 100)
        },
        incomeTrend() {
            if (this.prevIncome === null || this.prevIncome === 0) return 0
            return Math.round(((this.income - this.prevIncome) / this.prevIncome) * 100)
        }
    },
    watch: {
        activeTab() {
            this.$nextTick(this.renderCharts)
        }
    },
    mounted() {
        this.loadData()
        window.addEventListener('resize', this.handleChartResize)
    },
    beforeUnmount() {
        window.removeEventListener('resize', this.handleChartResize)
        if (this.lineChart) {
            this.lineChart.dispose()
            this.lineChart = null
        }
        if (this.pieChart) {
            this.pieChart.dispose()
            this.pieChart = null
        }
    },
    methods: {
        moneyFilter,
        colorOf(index) {
            return this.colors[index % this.colors.length]
        },
        async loadData() {
            await Promise.all([
                this.loadLineChart(),
                this.loadPieChart(),
                this.loadPrevMonth()
            ])
            this.$nextTick(this.renderCharts)
        },
        changeMonth(delta) {
            const newDate = new Date(this.currentDate)
            newDate.setMonth(newDate.getMonth() + delta)
            this.currentDate = newDate
            this.loadData()
        },
        async loadPrevMonth() {
            try {
                const prevDate = new Date(this.currentDate)
                prevDate.setMonth(prevDate.getMonth() - 1)
                const year = prevDate.getFullYear()
                const month = String(prevDate.getMonth() + 1).padStart(2, '0')
                const res = await listRecord({ payTime: `${year}-${month}` })
                const rows = res.rows || []
                this.prevExpenditure = rows.reduce((sum, r) => {
                    return sum + (r.typeObject && r.typeObject.typeDifference === '1' ? r.money : 0)
                }, 0)
                this.prevIncome = rows.reduce((sum, r) => {
                    return sum + (r.typeObject && r.typeObject.typeDifference === '0' ? r.money : 0)
                }, 0)
            } catch (e) {
                this.prevExpenditure = null
                this.prevIncome = null
            }
        },
        async loadLineChart() {
            try {
                const payTime = this.parseTime(this.currentDate)
                const res = await getLineChart({ payTime })
                this.lineData = res.data || []

                this.income = 0
                this.expenditure = 0
                this.lineData.forEach(item => {
                    this.income += item.inflowMoney || 0
                    this.expenditure += item.flowOutMoney || 0
                })
            } catch (e) {
                console.error('获取折线图失败', e)
            }
        },
        async loadPieChart() {
            try {
                const params = {
                    date: this.parseTime(this.currentDate),
                    dimension: 'primaryFlowOut',
                    typeDifference: '1'
                }
                const res = await getPrimaryStatistics(params)
                this.pieData = res.data || []
            } catch (e) {
                console.error('获取饼图失败', e)
            }
        },
        renderCharts() {
            this.initLineChart()
            this.initPieChart()
        },
        initLineChart() {
            if (this.activeTab !== 'line' || !this.$refs.lineChartRef || this.lineData.length === 0) return
            if (!this.lineChart) {
                this.lineChart = echarts.init(this.$refs.lineChartRef)
            }
            this.lineChart.setOption({
                grid: { left: 42, right: 10, top: 30, bottom: 24 },
                tooltip: {
                    trigger: 'axis',
                    axisPointer: { type: 'shadow' }
                },
                legend: {
                    data: ['收入', '支出'],
                    top: 0,
                    textStyle: { color: '#8C9BB3', fontSize: 11 },
                    itemWidth: 12,
                    itemHeight: 8
                },
                xAxis: {
                    type: 'category',
                    data: this.lineData.map(item => item.date),
                    axisLabel: { color: '#8C9BB3', fontSize: 10 },
                    axisLine: { lineStyle: { color: '#D9E2EF' } },
                    axisTick: { show: false }
                },
                yAxis: {
                    type: 'value',
                    axisLabel: { color: '#8C9BB3', fontSize: 10 },
                    splitLine: { lineStyle: { color: '#E7EDF5' } }
                },
                series: [
                    {
                        name: '收入',
                        type: 'bar',
                        data: this.lineData.map(item => item.inflowMoney || 0),
                        itemStyle: { color: '#34D399', borderRadius: [3, 3, 0, 0] },
                        barMaxWidth: 10
                    },
                    {
                        name: '支出',
                        type: 'bar',
                        data: this.lineData.map(item => item.flowOutMoney || 0),
                        itemStyle: { color: '#4F86F7', borderRadius: [3, 3, 0, 0] },
                        barMaxWidth: 10
                    }
                ]
            })
        },
        initPieChart() {
            if (this.activeTab !== 'pie' || !this.$refs.pieChartRef || this.pieData.length === 0) return
            if (!this.pieChart) {
                this.pieChart = echarts.init(this.$refs.pieChartRef)
            }
            this.pieChart.setOption({
                tooltip: {
                    trigger: 'item',
                    formatter: '{b}: {c} ({d}%)'
                },
                series: [
                    {
                        type: 'pie',
                        radius: ['62%', '84%'],
                        center: ['50%', '50%'],
                        avoidLabelOverlap: true,
                        label: { show: false },
                        labelLine: { show: false },
                        data: this.pieData.map((item, index) => ({
                            name: item.name,
                            value: item.value,
                            itemStyle: { color: this.colorOf(index) }
                        }))
                    }
                ]
            })
        },
        handleChartResize() {
            this.lineChart && this.lineChart.resize()
            this.pieChart && this.pieChart.resize()
        },
        getPercent(value) {
            const total = this.pieData.reduce((sum, item) => sum + (item.value || 0), 0)
            if (total === 0) return '0'
            return ((value / total) * 100).toFixed(1)
        }
    }
}
</script>

<style lang="scss" scoped>
.stat-page {
    min-height: calc(100vh - 46px);
    background: var(--pnkx-bg);
    padding-bottom: 24px;
}

.stat-header {
    padding: 14px 16px;
    background: linear-gradient(135deg, var(--pnkx-primary) 0%, var(--pnkx-info) 100%);
    color: #fff;

    &__month-selector {
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 12px;
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

.stat-trend {
    display: flex;
    gap: 10px;
    margin-bottom: 12px;

    &__card {
        flex: 1;
        background: var(--pnkx-surface);
        border: 1px solid var(--pnkx-border-soft);
        border-radius: 12px;
        padding: 10px 12px;
    }

    &__label {
        font-size: 12px;
        color: var(--pnkx-text-muted);
        display: block;
        margin-bottom: 4px;
    }

    &__value {
        display: flex;
        align-items: center;
        gap: 2px;
        font-size: 17px;
        font-weight: 600;

        &--up {
            color: var(--pnkx-danger);
        }

        &--down {
            color: var(--pnkx-success);
        }
    }
}

.stat-chart {
    &__content {
        position: relative;
        min-height: 200px;
    }

    &__canvas {
        width: 100%;
        height: 200px;
    }

    &__empty {
        position: absolute;
        inset: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        background: var(--pnkx-surface);
    }

    &__donut {
        position: relative;
    }

    &__donut-center {
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        display: flex;
        flex-direction: column;
        align-items: center;
        pointer-events: none;
    }

    &__donut-total {
        font-size: 17px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__donut-label {
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }
}

.stat-legend {
    padding: 4px 2px 0;

    &__item {
        display: flex;
        align-items: center;
        padding: 8px 0;
        border-bottom: 1px solid var(--pnkx-border-soft);

        &:last-child {
            border-bottom: none;
        }
    }

    &__dot {
        width: 10px;
        height: 10px;
        border-radius: 3px;
        margin-right: 8px;
        flex-shrink: 0;
    }

    &__name {
        flex: 1;
        font-size: 13px;
        color: var(--pnkx-text);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__value {
        font-size: 12px;
        color: var(--pnkx-text-secondary);
        margin-right: 8px;
    }

    &__percent {
        font-size: 13px;
        font-weight: 600;
        color: var(--pnkx-text);
        min-width: 44px;
        text-align: right;
    }
}
</style>
