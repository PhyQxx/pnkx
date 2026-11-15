<template>
    <div class="m-page menstruation-page">
        <!-- 当前状态 hero -->
        <div class="menstruation-hero">
            <div class="menstruation-hero__top">
                <div>
                    <span class="menstruation-hero__eyebrow">姨妈助手</span>
                    <span class="menstruation-hero__title">{{ heroTitle }}</span>
                </div>
                <button class="menstruation-hero__setting" @click="goToSettings">
                    <van-icon name="setting-o" size="18" />
                </button>
            </div>

            <div class="menstruation-hero__body">
                <span class="menstruation-hero__status" :class="statusClass">{{ currentStateLabel }}</span>
                <div class="menstruation-hero__summary">
                    <span class="menstruation-hero__summary-value">{{ nextPeriodText }}</span>
                    <span class="menstruation-hero__summary-label">{{ nextPeriodLabel }}</span>
                </div>
            </div>
        </div>

        <!-- 孕周洞察 -->
        <div v-if="pregnancyWeeks" class="menstruation-insight">
            <span class="menstruation-insight__icon">
                <van-icon name="like" size="14" color="var(--pnkx-danger)" />
            </span>
            <div class="menstruation-insight__content">
                <span class="menstruation-insight__title">距离最近一次记录 {{ pregnancyWeeks[2] }}</span>
                <span class="menstruation-insight__text">约 {{ pregnancyWeeks[0] }} 周 {{ pregnancyWeeks[1] }} 天，可结合身体感受复盘。</span>
            </div>
        </div>

        <!-- 快捷操作 -->
        <div class="menstruation-actions">
            <button class="menstruation-action" @click="handleAddRecord">
                <span class="menstruation-action__icon menstruation-action__icon--primary">
                    <van-icon name="plus" size="16" color="#fff" />
                </span>
                <span>记录一下</span>
            </button>
            <button class="menstruation-action" @click="goToStatistics">
                <span class="menstruation-action__icon menstruation-action__icon--info">
                    <van-icon name="bar-chart-o" size="16" color="#fff" />
                </span>
                <span>统计</span>
            </button>
            <button class="menstruation-action" @click="goToSettings">
                <span class="menstruation-action__icon menstruation-action__icon--soft">
                    <van-icon name="setting-o" size="16" color="var(--pnkx-primary)" />
                </span>
                <span>设置</span>
            </button>
        </div>

        <!-- 日历 / 列表双视图 -->
        <div class="view-switch">
            <button
                class="view-switch__item"
                :class="{ active: viewMode === 'calendar' }"
                @click="switchView('calendar')"
            >
                <van-icon name="calendar-o" size="15" />
                <span>日历</span>
            </button>
            <button
                class="view-switch__item"
                :class="{ active: viewMode === 'timeline' }"
                @click="switchView('timeline')"
            >
                <van-icon name="orders-o" size="15" />
                <span>列表</span>
            </button>
        </div>

        <van-pull-refresh v-model="isRefreshing" class="m-pull" success-text="已刷新" @refresh="onRefresh">
            <!-- 日历视图 -->
            <div v-if="viewMode === 'calendar'" class="calendar-view">
                <div class="m-card calendar-card">
                    <van-calendar
                        :poppable="false"
                        :show-confirm="false"
                        :show-mark="false"
                        :show-title="false"
                        :lazy-render="false"
                        :row-height="52"
                        :min-date="minDate"
                        :max-date="maxDate"
                        :default-date="defaultDate"
                        :formatter="calendarFormatter"
                        style="height: 392px"
                        @select="onDateClick"
                        @month-show="onMonthShow"
                    />
                </div>

                <template v-if="selectedDate">
                    <div class="section-header">
                        <div>
                            <span class="section-header__title">{{ formatDateFull(selectedDate) }}</span>
                            <span class="section-header__subtitle">{{ selectedDateRecords.length }} 条记录</span>
                        </div>
                        <button class="section-header__add" @click="handleAddRecord">
                            <van-icon name="plus" size="14" color="var(--pnkx-primary)" />
                        </button>
                    </div>

                    <RecordCard
                        v-for="record in selectedDateRecords"
                        :key="record.id"
                        :record="record"
                        @click="handleRecordEdit"
                        @edit="handleRecordEdit"
                        @delete="handleRecordDelete"
                    />

                    <div v-if="selectedDateRecords.length === 0" class="m-empty">
                        <van-icon name="calendar-o" size="40" color="var(--pnkx-primary)" />
                        <span>这一天还没有记录</span>
                        <span class="m-text-muted">添加经期、体温或检查信息，后续回看会更清楚。</span>
                    </div>
                </template>
            </div>

            <!-- 列表视图 -->
            <van-list
                v-else
                v-model:loading="listLoading"
                :finished="listFinished"
                finished-text="没有更多了"
                @load="onListLoad"
            >
                <div v-if="timelineRecords.length === 0 && !listLoading" class="m-empty">
                    <van-icon name="calendar-o" size="40" color="var(--pnkx-primary)" />
                    <span>暂无记录</span>
                    <span class="m-text-muted">从今天开始记录，周期趋势会慢慢变得可参考。</span>
                </div>
                <RecordCard
                    v-for="record in timelineRecords"
                    :key="record.id"
                    :record="record"
                    @click="handleRecordEdit"
                    @edit="handleRecordEdit"
                    @delete="handleRecordDelete"
                />
            </van-list>
        </van-pull-refresh>

        <button class="m-fab m-fab--no-tabbar" @click="handleAddRecord">
            <van-icon name="plus" size="22" />
        </button>
    </div>
</template>

<script>
import {
    listMenstruationRecord,
    getPxMenstruationRecordList,
    getLastStartDate,
    delMenstruationRecord
} from '@/api/px/life/menstruationRecord'
import { mConfirm, mSuccess, mError } from '@/mobile/utils/ui'
import RecordCard from './components/RecordCard.vue'

export default {
    name: 'MMenstruation',
    components: {
        RecordCard
    },
    data() {
        return {
            viewMode: 'calendar',
            selectedDate: '',
            menstruationRecords: [],
            allRecordsMap: {},
            timelineRecords: [],
            currentState: '',
            stateOptions: [],
            pregnancyWeeks: null,
            cycle: 28,
            duration: 5,
            loading: false,
            isRefreshing: false,
            listLoading: false,
            listFinished: false,
            fetching: false,
            pageNum: 1,
            pageSize: 10,
            total: 0,
            currentMonth: '',
            activatedOnce: false
        }
    },
    computed: {
        minDate() {
            const year = new Date().getFullYear()
            return new Date(year - 1, 0, 1)
        },
        maxDate() {
            const year = new Date().getFullYear()
            return new Date(year + 1, 11, 31)
        },
        defaultDate() {
            return this.selectedDate ? new Date(this.selectedDate.replace(/-/g, '/')) : new Date()
        },
        currentStateLabel() {
            const state = this.stateOptions.find(item => item.dictValue === this.currentState)
            if (state) return state.dictLabel
            const fallback = {
                whyl: '未怀孕',
                ymq: '姨妈期',
                aqq: '安全期',
                plq: '排卵期'
            }
            return fallback[this.currentState] || '待记录'
        },
        statusClass() {
            if (this.currentState === 'aqq') return 'menstruation-hero__status--info'
            if (['ymq', 'plq', 'whyl'].includes(this.currentState)) return 'menstruation-hero__status--danger'
            return ''
        },
        heroTitle() {
            if (this.currentState === 'ymq') return '照顾好现在的自己'
            if (this.currentState === 'plq') return '关注排卵窗口'
            if (this.currentState === 'aqq') return '今天是平稳期'
            return '记录身体节律'
        },
        lastStartRecord() {
            const records = [...this.menstruationRecords, ...this.timelineRecords]
                .filter(item => item.type === '0' && item.date)
                .sort((a, b) => new Date(b.date.replace(/-/g, '/')) - new Date(a.date.replace(/-/g, '/')))
            return records[0] || null
        },
        nextPeriodDate() {
            if (!this.lastStartRecord) return ''
            const date = new Date(this.lastStartRecord.date.replace(/-/g, '/'))
            date.setDate(date.getDate() + Number(this.cycle || 28))
            return this.formatDate(date)
        },
        nextPeriodText() {
            if (!this.nextPeriodDate) return '--'
            const days = this.getDaysBetween(new Date(), new Date(this.nextPeriodDate.replace(/-/g, '/')))
            if (days > 0) return `${days} 天`
            if (days === 0) return '今天'
            return `已过 ${Math.abs(days)} 天`
        },
        nextPeriodLabel() {
            return this.nextPeriodDate ? `预计下次 ${this.formatMonthDay(this.nextPeriodDate)}` : '记录一次开始日期后预测'
        },
        selectedDateRecords() {
            return this.allRecordsMap[this.selectedDate] || []
        },
        calendarMarks() {
            const marks = {}
            const push = (date, label) => {
                if (!date) return
                if (!marks[date]) marks[date] = []
                if (!marks[date].includes(label)) marks[date].push(label)
            }
            const startRecords = this.menstruationRecords.filter(item => item.type === '0')
            const endRecords = this.menstruationRecords.filter(item => item.type === '1')
            startRecords.forEach(record => push(record.date, '开始'))
            endRecords.forEach(record => push(record.date, '结束'))
            this.menstruationRecords.forEach(record => {
                if (record.makeLove) push(record.date, '同房')
                if (record.items) push(record.date, '检查')
            })
            // 预测：最近一次开始日期 + 周期，向后标记经期时长天
            if (startRecords.length > 0) {
                const sorted = [...startRecords].sort((a, b) => new Date(b.date.replace(/-/g, '/')) - new Date(a.date.replace(/-/g, '/')))
                const lastStart = new Date(sorted[0].date.replace(/-/g, '/'))
                const today = new Date()
                const nextStart = new Date(lastStart)
                nextStart.setDate(nextStart.getDate() + Number(this.cycle || 28))
                if (nextStart > today) {
                    for (let i = 0; i < Number(this.duration || 5); i++) {
                        const predictDate = new Date(nextStart)
                        predictDate.setDate(predictDate.getDate() + i)
                        push(this.formatDate(predictDate), '预测')
                    }
                }
            }
            return marks
        }
    },
    mounted() {
        this.loadConfigState()
        this.loadSettings()
        this.loadStateOptions()
        this.loadLastStartDate()
        this.initCalendar()
        this.loadMenstruationRecords()
    },
    activated() {
        if (this.activatedOnce) {
            this.refreshData()
        }
        this.activatedOnce = true
    },
    methods: {
        formatDate(date) {
            if (!date) return ''
            const d = new Date(date)
            return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
        },
        formatMonthDay(date) {
            if (!date) return ''
            const d = new Date(String(date).replace(/-/g, '/'))
            return `${d.getMonth() + 1}月${d.getDate()}日`
        },
        formatDateFull(date) {
            if (!date) return ''
            const d = new Date(String(date).replace(/-/g, '/'))
            const weekMap = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
            return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${weekMap[d.getDay()]}`
        },
        getDaysBetween(date1, date2) {
            const d1 = new Date(this.formatDate(date1).replace(/-/g, '/'))
            const d2 = new Date(this.formatDate(date2).replace(/-/g, '/'))
            return Math.floor((d2 - d1) / (1000 * 60 * 60 * 24))
        },
        loadConfigState() {
            this.getConfigKey('ymdqzt').then(response => {
                this.currentState = response.msg
            }).catch(() => {})
        },
        loadSettings() {
            const cycle = localStorage.getItem('menstruation_cycle')
            const duration = localStorage.getItem('menstruation_duration')
            if (cycle) this.cycle = parseInt(cycle)
            if (duration) this.duration = parseInt(duration)
        },
        async loadStateOptions() {
            try {
                const response = await this.getDicts('px_life_menstruation')
                if (response.code === 200) {
                    this.stateOptions = response.data || []
                }
            } catch (error) {
                // 字典加载失败时使用兜底文案
            }
        },
        async loadLastStartDate() {
            try {
                const response = await getLastStartDate()
                if (response.code === 200 && response.data) {
                    const lastDate = new Date(String(response.data.date).replace(/-/g, '/'))
                    const today = new Date()
                    const days = Math.floor((today - lastDate) / (1000 * 60 * 60 * 24))
                    if (days >= 0) {
                        this.pregnancyWeeks = [
                            Math.floor(days / 7),
                            days % 7,
                            `${days} 天`
                        ]
                    }
                }
            } catch (error) {
                // 静默失败，洞察卡片不展示
            }
        },
        initCalendar() {
            const now = new Date()
            this.selectedDate = this.formatDate(now)
            this.currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
        },
        calendarFormatter(day) {
            const date = `${day.date.getFullYear()}-${String(day.date.getMonth() + 1).padStart(2, '0')}-${String(day.date.getDate()).padStart(2, '0')}`
            const marks = this.calendarMarks[date]
            if (marks && marks.length > 0) {
                day.bottomInfo = marks.slice(0, 2).join('·')
                if (marks.includes('开始')) day.className = 'mc-day--start'
                else if (marks.includes('结束')) day.className = 'mc-day--end'
                else if (marks.includes('预测')) day.className = 'mc-day--predict'
                else day.className = 'mc-day--mark'
            }
            return day
        },
        switchView(mode) {
            if (this.viewMode === mode) return
            this.viewMode = mode
            if (mode === 'calendar') {
                this.loadMenstruationRecords()
            } else {
                // van-list 挂载后会自动触发 load
                this.pageNum = 1
                this.timelineRecords = []
                this.listFinished = false
            }
        },
        async loadMenstruationRecords() {
            if (this.loading) return
            this.loading = true
            try {
                const response = await listMenstruationRecord({ date: this.currentMonth })
                if (response.code === 200) {
                    const records = (response.rows || []).map(item => ({
                        ...item,
                        date: item.date ? item.date.substring(0, 10) : ''
                    }))
                    this.menstruationRecords = records
                    this.buildRecordsMap()
                }
            } catch (error) {
                mError('加载失败')
            } finally {
                this.loading = false
                this.isRefreshing = false
            }
        },
        buildRecordsMap() {
            this.allRecordsMap = {}
            this.menstruationRecords.forEach(record => {
                if (record.date) {
                    if (!this.allRecordsMap[record.date]) {
                        this.allRecordsMap[record.date] = []
                    }
                    this.allRecordsMap[record.date].push(record)
                }
            })
        },
        async loadTimelinePage() {
            if (this.fetching) {
                this.listLoading = false
                return
            }
            this.fetching = true
            this.listLoading = true
            try {
                const response = await getPxMenstruationRecordList({
                    pageNum: this.pageNum,
                    pageSize: this.pageSize
                })
                if (response.code === 200) {
                    const newRecords = (response.rows || []).map(item => ({
                        ...item,
                        date: item.date ? item.date.substring(0, 10) : ''
                    }))
                    this.total = response.total || 0
                    if (this.pageNum === 1) {
                        this.timelineRecords = newRecords
                    } else {
                        this.timelineRecords = [...this.timelineRecords, ...newRecords]
                    }
                    this.listFinished = this.timelineRecords.length >= this.total
                    this.pageNum++
                }
            } catch (error) {
                mError('加载失败')
            } finally {
                this.fetching = false
                this.listLoading = false
                this.isRefreshing = false
            }
        },
        onListLoad() {
            this.loadTimelinePage()
        },
        onRefresh() {
            this.refreshData()
        },
        refreshData() {
            if (this.viewMode === 'calendar') {
                this.loadMenstruationRecords()
            } else {
                this.pageNum = 1
                this.listFinished = false
                this.loadTimelinePage()
            }
        },
        onMonthShow({ date }) {
            const month = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
            if (month === this.currentMonth) return
            this.currentMonth = month
            this.loadMenstruationRecords()
        },
        onDateClick(date) {
            this.selectedDate = this.formatDate(date)
        },
        handleAddRecord() {
            const query = this.selectedDate && this.viewMode === 'calendar' ? `?date=${this.selectedDate}` : ''
            this.$router.push(`/m/menstruation/record${query}`)
        },
        handleRecordEdit(record) {
            this.$router.push(`/m/menstruation/record?id=${record.id}`)
        },
        handleRecordDelete(record) {
            mConfirm('确认删除', '确定要删除这条记录吗？').then(async () => {
                try {
                    const response = await delMenstruationRecord(record.id)
                    if (response.code === 200) {
                        mSuccess('删除成功')
                        this.refreshData()
                    }
                } catch (error) {
                    mError('删除失败')
                }
            }).catch(() => {})
        },
        goToSettings() {
            this.$router.push('/m/menstruation/settings')
        },
        goToStatistics() {
            this.$router.push('/m/menstruation/statistics')
        }
    }
}
</script>

<style lang="scss" scoped>
.menstruation-page {
    padding-bottom: 88px;
}

.menstruation-hero {
    padding: 15px;
    border-radius: 14px;
    background: linear-gradient(135deg, var(--pnkx-danger-soft) 0%, var(--pnkx-surface) 54%, var(--pnkx-primary-soft) 100%);
    border: 1px solid var(--pnkx-border-soft);

    &__top {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 12px;
    }

    &__eyebrow {
        display: block;
        font-size: 12px;
        color: var(--pnkx-danger);
        font-weight: 600;
        margin-bottom: 4px;
    }

    &__title {
        display: block;
        font-size: 20px;
        line-height: 1.2;
        font-weight: 700;
        color: var(--pnkx-text);
    }

    &__setting {
        width: 32px;
        height: 32px;
        border-radius: 50%;
        border: 1px solid var(--pnkx-border-soft);
        background: var(--pnkx-surface);
        color: var(--pnkx-primary);
        display: flex;
        align-items: center;
        justify-content: center;

        &:active {
            opacity: 0.8;
        }
    }

    &__body {
        display: flex;
        align-items: flex-end;
        justify-content: space-between;
        gap: 12px;
        margin-top: 18px;
    }

    &__status {
        padding: 5px 10px;
        border-radius: 999px;
        font-size: 12px;
        font-weight: 600;
        color: var(--pnkx-text-secondary);
        background: var(--pnkx-surface-muted);

        &--danger {
            color: var(--pnkx-danger);
            background: var(--pnkx-danger-soft);
        }

        &--info {
            color: var(--pnkx-info);
            background: var(--pnkx-info-soft);
        }
    }

    &__summary {
        text-align: right;
    }

    &__summary-value {
        display: block;
        font-size: 23px;
        line-height: 1.1;
        font-weight: 800;
        color: var(--pnkx-danger);
    }

    &__summary-label {
        display: block;
        margin-top: 4px;
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }
}

.menstruation-insight {
    display: flex;
    align-items: center;
    gap: 9px;
    padding: 9px 11px;
    margin-top: 9px;
    border-radius: 10px;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);

    &__icon {
        width: 22px;
        height: 22px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        background: var(--pnkx-danger-soft);
        flex-shrink: 0;
    }

    &__content {
        flex: 1;
        min-width: 0;
    }

    &__title {
        display: block;
        font-size: 13px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__text {
        display: block;
        margin-top: 2px;
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }
}

.menstruation-actions {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
    margin-top: 9px;
}

.menstruation-action {
    display: flex;
    align-items: center;
    gap: 6px;
    min-height: 38px;
    padding: 0 9px;
    border-radius: 10px;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    font-size: 12px;
    color: var(--pnkx-text);
    font-weight: 600;

    &__icon {
        width: 21px;
        height: 21px;
        border-radius: 7px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;

        &--primary {
            background: var(--pnkx-danger);
        }

        &--info {
            background: var(--pnkx-primary);
        }

        &--soft {
            background: var(--pnkx-primary-soft);
        }
    }
}

.view-switch {
    display: flex;
    padding: 4px;
    gap: 4px;
    margin-top: 9px;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 999px;

    &__item {
        flex: 1;
        height: 32px;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 4px;
        border: none;
        border-radius: 999px;
        color: var(--pnkx-text-muted);
        background: transparent;
        font-size: 13px;
        font-weight: 600;

        &.active {
            color: var(--pnkx-primary);
            background: var(--pnkx-primary-soft);
        }
    }
}

.calendar-view,
.calendar-card {
    margin-top: 12px;
}

.calendar-card {
    padding: 8px;
}

:deep(.van-calendar__bottom-info) {
    font-size: 9px;
}

:deep(.mc-day--start) {
    .van-calendar__bottom-info {
        color: var(--pnkx-danger);
        font-weight: 600;
    }
}

:deep(.mc-day--end) {
    .van-calendar__bottom-info {
        color: var(--pnkx-info);
        font-weight: 600;
    }
}

:deep(.mc-day--predict) {
    .van-calendar__bottom-info {
        color: var(--pnkx-warning);
    }
}

:deep(.mc-day--mark) {
    .van-calendar__bottom-info {
        color: var(--pnkx-text-secondary);
    }
}

.section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin: 14px 2px 10px;

    &__title {
        display: block;
        font-size: 15px;
        font-weight: 700;
        color: var(--pnkx-text);
    }

    &__subtitle {
        display: block;
        margin-top: 3px;
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }

    &__add {
        width: 28px;
        height: 28px;
        border-radius: 50%;
        border: none;
        display: flex;
        align-items: center;
        justify-content: center;
        background: var(--pnkx-primary-soft);
    }
}
</style>
