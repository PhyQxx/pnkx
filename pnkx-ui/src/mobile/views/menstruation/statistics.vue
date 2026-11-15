<template>
    <div class="m-page menstruation-statistics-page">
        <!-- 平均周期 hero -->
        <div class="stats-hero">
            <span class="stats-hero__eyebrow">周期统计</span>
            <span class="stats-hero__title">{{ stats.avgCycleText }} 天</span>
            <span class="stats-hero__text">平均周期，基于已记录的开始日期计算</span>
        </div>

        <!-- 统计卡片 -->
        <div class="stats-grid">
            <div class="stat-card">
                <span class="stat-card__value">{{ stats.avgCycleText }}</span>
                <span class="stat-card__label">平均周期</span>
            </div>
            <div class="stat-card stat-card--info">
                <span class="stat-card__value">{{ stats.avgDurationText }}</span>
                <span class="stat-card__label">经期时长</span>
            </div>
            <div class="stat-card stat-card--success">
                <span class="stat-card__value">{{ stats.totalRecords }}</span>
                <span class="stat-card__label">记录总数</span>
            </div>
            <div class="stat-card stat-card--warning">
                <span class="stat-card__value">{{ stats.totalCycles }}</span>
                <span class="stat-card__label">经期次数</span>
            </div>
        </div>

        <!-- 周期趋势 -->
        <div class="m-card trend-section">
            <div class="section-header">
                <span class="section-header__title">周期趋势</span>
                <span class="section-header__sub">最近记录的间隔概览</span>
            </div>

            <div v-if="cycleTrend.length > 0" class="trend-list">
                <div v-for="(item, index) in cycleTrend" :key="index" class="trend-item">
                    <div class="trend-item__meta">
                        <span class="trend-item__date">{{ item.date }}</span>
                        <span class="trend-item__days">{{ item.daysText }}</span>
                    </div>
                    <div class="trend-item__bar">
                        <div class="trend-item__bar-fill" :style="{ width: item.percent + '%' }"></div>
                    </div>
                </div>
            </div>

            <div v-else class="m-empty">
                <van-icon name="bar-chart-o" size="34" color="var(--pnkx-danger)" />
                <span>趋势还不够清晰</span>
                <span class="m-text-muted">至少记录两次经期开始后，就能计算周期间隔。</span>
            </div>
        </div>

        <!-- 最近记录 -->
        <div class="m-card recent-section">
            <div class="section-header">
                <span class="section-header__title">最近记录</span>
                <span class="section-header__sub">点击可继续编辑</span>
            </div>

            <div v-if="recentRecords.length === 0" class="m-empty">
                <van-icon name="calendar-o" size="34" color="var(--pnkx-danger)" />
                <span>暂无记录</span>
            </div>

            <div v-else class="record-list">
                <div
                    v-for="record in recentRecords"
                    :key="record.id"
                    class="record-item"
                    @click="handleRecordClick(record)"
                >
                    <div class="record-item__date">
                        <span class="record-item__day">{{ record.dayText }}</span>
                        <span class="record-item__month">{{ record.monthText }}</span>
                    </div>
                    <div class="record-item__main">
                        <span class="record-item__type" :class="record.typeClass">{{ record.typeText }}</span>
                        <span class="record-item__desc">{{ record.descText }}</span>
                    </div>
                    <van-icon name="arrow" size="14" color="var(--pnkx-text-muted)" />
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import { getPxMenstruationRecordList } from '@/api/px/life/menstruationRecord'
import { mError, mLoading, closeToast } from '@/mobile/utils/ui'

export default {
    name: 'MMenstruationStatistics',
    data() {
        return {
            stats: {
                avgCycle: null,
                avgCycleText: '--',
                avgDuration: 5,
                avgDurationText: '5',
                totalRecords: 0,
                totalCycles: 0
            },
            recentRecords: [],
            allRecords: [],
            cycleTrend: []
        }
    },
    mounted() {
        this.loadStatistics()
    },
    methods: {
        formatDate(date) {
            if (!date) return ''
            return String(date).substring(0, 10)
        },
        getDay(date) {
            const value = this.formatDate(date)
            return value ? value.substring(8, 10) : '--'
        },
        getMonth(date) {
            const value = this.formatDate(date)
            if (!value) return ''
            const d = new Date(value.replace(/-/g, '/'))
            return `${d.getMonth() + 1}月`
        },
        getTypeText(type) {
            if (type === '0') return '经期开始'
            if (type === '1') return '经期结束'
            return '普通记录'
        },
        getTypeClass(type) {
            if (type === '0') return 'start'
            if (type === '1') return 'end'
            return 'normal'
        },
        getRecordDesc(record) {
            const desc = []
            if (record.temperature) desc.push(`体温 ${record.temperature}℃`)
            if (record.weight) desc.push(`体重 ${record.weight}kg`)
            if (record.items) desc.push(record.items)
            if (record.makeLove) desc.push('同房')
            return desc.length ? desc.join(' · ') : '未填写更多信息'
        },
        normalizeRecordDisplay(record) {
            return {
                ...record,
                dayText: this.getDay(record.date),
                monthText: this.getMonth(record.date),
                typeText: this.getTypeText(record.type),
                typeClass: this.getTypeClass(record.type),
                descText: this.getRecordDesc(record)
            }
        },
        async loadStatistics() {
            mLoading('加载中...')
            try {
                const response = await getPxMenstruationRecordList({
                    pageNum: 1,
                    pageSize: 100
                })
                if (response.code === 200) {
                    this.allRecords = (response.rows || []).map(item => ({
                        ...item,
                        date: item.date ? item.date.substring(0, 10) : ''
                    }))
                    this.recentRecords = this.allRecords.slice(0, 10).map(this.normalizeRecordDisplay)
                    this.calculateStats()
                }
                closeToast()
            } catch (error) {
                mError('加载失败')
            }
        },
        calculateStats() {
            this.stats.totalRecords = this.allRecords.length

            const startRecords = this.allRecords
                .filter(r => r.type === '0')
                .sort((a, b) => new Date(b.date.replace(/-/g, '/')) - new Date(a.date.replace(/-/g, '/')))

            this.stats.totalCycles = startRecords.length

            const cycles = []
            for (let i = 0; i < startRecords.length - 1; i++) {
                const days = this.getDaysBetween(startRecords[i].date, startRecords[i + 1].date)
                if (days > 0 && days < 60) {
                    cycles.push({
                        date: this.formatDate(startRecords[i].date),
                        days
                    })
                }
            }

            if (cycles.length > 0) {
                const avgCycle = cycles.reduce((sum, item) => sum + item.days, 0) / cycles.length
                this.stats.avgCycle = Math.round(avgCycle)
                this.stats.avgCycleText = String(this.stats.avgCycle)
            } else {
                this.stats.avgCycle = null
                this.stats.avgCycleText = '--'
            }

            const duration = localStorage.getItem('menstruation_duration')
            this.stats.avgDuration = duration ? parseInt(duration) : 5
            this.stats.avgDurationText = String(this.stats.avgDuration)

            const maxDays = Math.max(...cycles.map(item => item.days), 35)
            this.cycleTrend = cycles.slice(0, 6).map(item => {
                const percent = Math.max(12, Math.min(100, Math.round((item.days / maxDays) * 100)))
                return {
                    ...item,
                    daysText: `${item.days} 天`,
                    percent
                }
            })
        },
        getDaysBetween(date1, date2) {
            const d1 = new Date(String(date1).replace(/-/g, '/'))
            const d2 = new Date(String(date2).replace(/-/g, '/'))
            return Math.floor((d1 - d2) / (1000 * 60 * 60 * 24))
        },
        handleRecordClick(record) {
            this.$router.push(`/m/menstruation/record?id=${record.id}`)
        }
    }
}
</script>

<style lang="scss" scoped>
.stats-hero {
    padding: 15px;
    border-radius: 14px;
    margin-bottom: 10px;
    background: linear-gradient(135deg, var(--pnkx-danger-soft) 0%, var(--pnkx-surface) 58%, var(--pnkx-primary-soft) 100%);
    border: 1px solid var(--pnkx-border-soft);

    &__eyebrow {
        display: block;
        font-size: 12px;
        color: var(--pnkx-danger);
        font-weight: 600;
    }

    &__title {
        display: block;
        margin-top: 4px;
        font-size: 27px;
        line-height: 1.1;
        font-weight: 800;
        color: var(--pnkx-text);
    }

    &__text {
        display: block;
        margin-top: 5px;
        font-size: 12px;
        color: var(--pnkx-text-secondary);
    }
}

.stats-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
    margin-bottom: 12px;
}

.stat-card {
    padding: 12px;
    border-radius: 12px;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-top: 3px solid var(--pnkx-danger);

    &--info {
        border-top-color: var(--pnkx-info);
    }

    &--success {
        border-top-color: var(--pnkx-success);
    }

    &--warning {
        border-top-color: var(--pnkx-warning);
    }

    &__value {
        display: block;
        font-size: 21px;
        line-height: 1.1;
        font-weight: 800;
        color: var(--pnkx-text);
    }

    &__label {
        display: block;
        margin-top: 5px;
        font-size: 12px;
        color: var(--pnkx-text-secondary);
    }
}

.section-header {
    margin-bottom: 11px;

    &__title {
        display: block;
        font-size: 15px;
        font-weight: 700;
        color: var(--pnkx-text);
    }

    &__sub {
        display: block;
        margin-top: 3px;
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }
}

.trend-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.trend-item {
    &__meta {
        display: flex;
        justify-content: space-between;
        margin-bottom: 5px;
    }

    &__date {
        font-size: 12px;
        color: var(--pnkx-text-secondary);
    }

    &__days {
        font-size: 12px;
        color: var(--pnkx-danger);
        font-weight: 700;
    }

    &__bar {
        height: 8px;
        overflow: hidden;
        border-radius: 999px;
        background: var(--pnkx-surface-muted);
    }

    &__bar-fill {
        height: 100%;
        border-radius: 999px;
        background: linear-gradient(90deg, var(--pnkx-primary-soft), var(--pnkx-danger));
    }
}

.record-list {
    display: flex;
    flex-direction: column;
}

.record-item {
    display: flex;
    align-items: center;
    gap: 9px;
    padding: 9px 0;
    border-bottom: 1px solid var(--pnkx-border-soft);

    &:last-child {
        border-bottom: none;
    }

    &__date {
        width: 36px;
        height: 36px;
        border-radius: 9px;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        background: var(--pnkx-danger-soft);
        flex-shrink: 0;
    }

    &__day {
        font-size: 14px;
        line-height: 1;
        font-weight: 800;
        color: var(--pnkx-danger);
    }

    &__month {
        margin-top: 3px;
        font-size: 10px;
        color: var(--pnkx-danger);
    }

    &__main {
        flex: 1;
        min-width: 0;
    }

    &__type {
        display: block;
        font-size: 13px;
        font-weight: 700;
        color: var(--pnkx-text-secondary);

        &.start {
            color: var(--pnkx-danger);
        }

        &.end {
            color: var(--pnkx-info);
        }
    }

    &__desc {
        display: block;
        margin-top: 4px;
        font-size: 11px;
        color: var(--pnkx-text-muted);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }
}
</style>
