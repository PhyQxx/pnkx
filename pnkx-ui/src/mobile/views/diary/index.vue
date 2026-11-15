<template>
    <div class="diary-page">
        <!-- 固定头部 -->
        <div class="diary-header">
            <div class="diary-header__panel">
                <div class="diary-header__main">
                    <div class="diary-header__title">我的日记</div>
                    <div class="diary-header__subtitle">{{ headerDateText }}</div>
                </div>
                <div class="diary-header__actions">
                    <div class="diary-header__count" @click="goAnalysis">
                        <span class="diary-header__count-num">{{ totalDiaryCount }}</span>
                        <span class="diary-header__count-label">篇记录</span>
                    </div>
                    <div class="diary-header__analysis" @click="goAnalysis">
                        <van-icon name="bar-chart-o" size="14" />
                        <span>心情分析</span>
                    </div>
                </div>
            </div>

            <div class="diary-header__stats">
                <div class="stat-item">
                    <span class="stat-item__value">{{ monthDiaryCount }}</span>
                    <span class="stat-item__label">本月</span>
                </div>
                <div class="stat-item">
                    <span class="stat-item__value">{{ weekDiaryCount }}</span>
                    <span class="stat-item__label">本周</span>
                </div>
                <div class="stat-item">
                    <span class="stat-item__value">{{ currentStreak }}</span>
                    <span class="stat-item__label">连续</span>
                </div>
            </div>

            <van-search
                v-model="searchKeyword"
                class="diary-header__search"
                placeholder="搜索标题或内容"
                shape="round"
                :clearable="true"
                @search="handleSearch"
                @clear="handleSearch"
                @update:model-value="onSearchInput"
            />

            <div class="diary-header__switch">
                <div class="switch-item" :class="{ active: viewMode === 'timeline' }" @click="switchView('timeline')">
                    <van-icon name="bars" size="15" />
                    <span>时间线</span>
                </div>
                <div class="switch-item" :class="{ active: viewMode === 'calendar' }" @click="switchView('calendar')">
                    <van-icon name="calendar-o" size="15" />
                    <span>日历</span>
                </div>
            </div>
        </div>

        <!-- 内容区域 -->
        <van-pull-refresh v-model="refreshing" class="diary-body m-pull" @refresh="onRefresh">
            <!-- 日历视图 -->
            <div v-if="viewMode === 'calendar'" class="calendar-view">
                <div class="calendar-card m-card">
                    <van-calendar
                        :key="'diary-cal-' + calendarKey"
                        :poppable="false"
                        :show-confirm="false"
                        :show-mark="false"
                        :show-title="false"
                        :lazy-render="false"
                        :row-height="52"
                        :min-date="minDate"
                        :max-date="maxDate"
                        :default-date="defaultCalendarDate"
                        :formatter="calendarFormatter"
                        style="height: 384px"
                        @select="onDateClick"
                    />
                </div>

                <template v-if="selectedDateDiaries.length > 0">
                    <div class="section-title">
                        <span>{{ formatDateFull(selectedDate) }}</span>
                        <span class="section-count">{{ selectedDateDiaries.length }}篇</span>
                    </div>
                    <DiaryCard
                        v-for="diary in selectedDateDiaries"
                        :key="diary.id"
                        :diary="diary"
                        @click="goEdit"
                        @edit="goEdit"
                        @delete="handleDiaryDelete"
                    />
                </template>
                <div v-else class="m-empty">
                    <van-icon name="calendar-o" size="40" color="var(--pnkx-primary)" />
                    <span>这一天还没有记录</span>
                    <span class="m-text-muted">写一篇，让日历多一个亮点</span>
                </div>
            </div>

            <!-- 时间线视图 -->
            <van-list
                v-else
                v-model:loading="listLoading"
                :finished="listFinished"
                finished-text="没有更多了"
                @load="loadMore"
            >
                <div v-if="diaryList.length === 0 && !listLoading && !loading" class="m-empty">
                    <van-icon name="edit" size="42" color="var(--pnkx-primary)" />
                    <span>{{ emptyTitle }}</span>
                    <span class="m-text-muted">{{ emptyTip }}</span>
                </div>
                <DiaryCard
                    v-for="diary in diaryList"
                    :key="diary.id"
                    :diary="diary"
                    @click="goEdit"
                    @edit="goEdit"
                    @delete="handleDiaryDelete"
                />
            </van-list>
        </van-pull-refresh>

        <button class="m-fab" @click="handleAdd">
            <van-icon name="plus" size="22" />
        </button>
    </div>
</template>

<script>
import { listDiary, delDiary } from '@/api/px/life/diary'
import { showToast, mSuccess, mError, mConfirm } from '@/mobile/utils/ui'
import DiaryCard from './components/DiaryCard.vue'

const WEEK_LIST = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

export default {
    name: 'MDiary',
    components: {
        DiaryCard
    },
    data() {
        return {
            searchKeyword: '',
            searchTimer: null,
            viewMode: 'timeline',
            selectedDate: '',
            diaryList: [],
            selectedDateDiaries: [],
            allDatesMap: {},
            calendarKey: 0,
            loading: false,
            listLoading: false,
            listFinished: false,
            refreshing: false,
            pageNum: 1,
            pageSize: 10,
            total: 0
        }
    },
    computed: {
        headerDateText() {
            const now = new Date()
            return `${now.getMonth() + 1}月${now.getDate()}日 ${WEEK_LIST[now.getDay()]}`
        },
        totalDiaryCount() {
            return this.total || this.diaryList.length
        },
        monthDiaryCount() {
            const now = new Date()
            const currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
            return this.diaryList.filter(item => item.date && item.date.substring(0, 7) === currentMonth).length
        },
        weekDiaryCount() {
            const now = new Date()
            const day = now.getDay() || 7
            const start = new Date(now)
            start.setHours(0, 0, 0, 0)
            start.setDate(now.getDate() - day + 1)
            const end = new Date(start)
            end.setDate(start.getDate() + 7)
            return this.diaryList.filter(item => {
                if (!item.date) return false
                const date = new Date(String(item.date).replace(/-/g, '/'))
                if (Number.isNaN(date.getTime())) return false
                return date >= start && date < end
            }).length
        },
        currentStreak() {
            const dateSet = {}
            this.diaryList.forEach(item => {
                if (item.date) dateSet[item.date.substring(0, 10)] = true
            })
            let streak = 0
            const cursor = new Date()
            while (dateSet[this.formatDate(cursor)]) {
                streak++
                cursor.setDate(cursor.getDate() - 1)
            }
            return streak
        },
        emptyTitle() {
            return this.searchKeyword ? '没有匹配的日记' : '还没有日记'
        },
        emptyTip() {
            return this.searchKeyword ? '换个关键词再找找' : '点右下角写下今天的第一段'
        },
        minDate() {
            const year = new Date().getFullYear()
            return new Date(year, 0, 1)
        },
        maxDate() {
            const year = new Date().getFullYear()
            return new Date(year + 1, 11, 31)
        },
        defaultCalendarDate() {
            if (!this.selectedDate) return new Date()
            const date = new Date(this.selectedDate.replace(/-/g, '/'))
            return Number.isNaN(date.getTime()) ? new Date() : date
        }
    },
    mounted() {
        this.selectedDate = this.formatDate(new Date())
        this.refreshDiaryList()
    },
    activated() {
        this.refreshDiaryList()
    },
    methods: {
        formatDate(date) {
            if (!date) return ''
            const d = new Date(date)
            if (Number.isNaN(d.getTime())) return ''
            return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
        },
        formatDateFull(date) {
            if (!date) return ''
            const d = new Date(String(date).replace(/-/g, '/'))
            if (Number.isNaN(d.getTime())) return ''
            return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${WEEK_LIST[d.getDay()]}`
        },
        switchView(mode) {
            this.viewMode = mode
            if (mode === 'calendar') {
                this.loadSelectedDateDiaries()
            }
        },
        async refreshDiaryList() {
            if (this.loading) return
            this.loading = true
            this.pageNum = 1
            this.diaryList = []
            this.allDatesMap = {}
            try {
                await this.loadDiaryPage()
            } finally {
                this.loading = false
                this.refreshing = false
                this.listLoading = false
            }
        },
        async loadMore() {
            if (this.loading || this.listFinished) {
                this.listLoading = false
                return
            }
            this.loading = true
            try {
                await this.loadDiaryPage()
            } catch (error) {
                console.error('加载日记列表失败:', error)
            } finally {
                this.loading = false
                this.listLoading = false
            }
        },
        async loadDiaryPage() {
            try {
                const params = {
                    pageNum: this.pageNum,
                    pageSize: this.pageSize
                }
                if (this.searchKeyword) {
                    params.title = this.searchKeyword
                    params.content = this.searchKeyword
                }
                const response = await listDiary(params)
                if (response.code === 200) {
                    const rows = response.rows || []
                    this.total = response.total || 0
                    if (this.pageNum === 1) {
                        this.diaryList = rows
                    } else {
                        this.diaryList = [...this.diaryList, ...rows]
                    }
                    this.pageNum++
                    this.listFinished = this.diaryList.length >= this.total
                    this.buildSelectedDates()
                } else {
                    this.listFinished = false
                }
            } catch (error) {
                console.error('加载日记列表失败:', error)
                this.listFinished = false
                showToast('加载失败')
            }
        },
        buildSelectedDates() {
            this.allDatesMap = {}
            this.diaryList.forEach(diary => {
                const date = diary.date ? diary.date.substring(0, 10) : ''
                if (date) {
                    if (!this.allDatesMap[date]) this.allDatesMap[date] = []
                    this.allDatesMap[date].push(diary)
                }
            })
            this.calendarKey++
            this.loadSelectedDateDiaries()
        },
        loadSelectedDateDiaries() {
            this.selectedDateDiaries = this.allDatesMap[this.selectedDate] || []
        },
        calendarFormatter(day) {
            const date = `${day.date.getFullYear()}-${String(day.date.getMonth() + 1).padStart(2, '0')}-${String(day.date.getDate()).padStart(2, '0')}`
            if (this.allDatesMap[date]) {
                day.bottomInfo = '记'
            }
            return day
        },
        onDateClick(date) {
            this.selectedDate = this.formatDate(date)
            this.loadSelectedDateDiaries()
        },
        onRefresh() {
            this.refreshDiaryList()
        },
        handleSearch() {
            this.refreshDiaryList()
        },
        onSearchInput(value) {
            if (this.searchTimer) clearTimeout(this.searchTimer)
            this.searchTimer = setTimeout(() => {
                this.handleSearch()
            }, 500)
        },
        goEdit(diary) {
            const query = diary && diary.id ? `?id=${diary.id}` : ''
            const path = `/m/diary/edit${query}`
            if (this.$router.resolve(path).matched.length > 0) {
                this.$router.push(path)
            } else {
                showToast('功能即将上线')
            }
        },
        handleAdd() {
            this.goEdit()
        },
        goAnalysis() {
            if (this.$router.resolve('/m/diary/analysis').matched.length > 0) {
                this.$router.push('/m/diary/analysis')
            } else {
                showToast('功能即将上线')
            }
        },
        async handleDiaryDelete(diary) {
            try {
                await mConfirm('删除确认', `确定删除「${diary.title || '无标题日记'}」吗？`)
            } catch (e) {
                return
            }
            try {
                const response = await delDiary(diary.id)
                if (response.code === 200) {
                    mSuccess('删除成功')
                    this.refreshDiaryList()
                }
            } catch (error) {
                console.error('删除日记失败:', error)
                mError('删除失败')
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.diary-page {
    min-height: calc(100vh - 46px);
    padding: 12px 12px 80px;
    box-sizing: border-box;
    background: var(--pnkx-bg);
}

.diary-header {
    margin-bottom: 10px;

    &__panel {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 14px;
        margin-bottom: 10px;
        background: var(--pnkx-surface);
        border: 1px solid var(--pnkx-border-soft);
        border-radius: 12px;
    }

    &__title {
        font-size: 20px;
        font-weight: 700;
        color: var(--pnkx-text);
        line-height: 1.2;
    }

    &__subtitle {
        margin-top: 4px;
        font-size: 12px;
        color: var(--pnkx-text-secondary);
    }

    &__actions {
        display: flex;
        align-items: center;
        gap: 8px;
    }

    &__count {
        width: 56px;
        height: 56px;
        border-radius: 50%;
        background: var(--pnkx-primary);
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        box-shadow: 0 4px 12px rgba(91, 141, 239, 0.35);
    }

    &__count-num {
        font-size: 15px;
        font-weight: 700;
        color: #fff;
        line-height: 1.1;
    }

    &__count-label {
        margin-top: 2px;
        font-size: 9px;
        color: rgba(255, 255, 255, 0.86);
    }

    &__analysis {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 2px;
        width: 44px;
        height: 44px;
        border-radius: 50%;
        border: 1px solid var(--pnkx-primary-soft);
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
        font-size: 9px;
    }

    &__stats {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 6px;
        margin-bottom: 10px;
    }

    &__search {
        padding: 0;
        background: transparent;

        :deep(.van-search__content) {
            background: var(--pnkx-surface);
            border: 1px solid var(--pnkx-border-soft);
        }
    }

    &__switch {
        display: flex;
        gap: 4px;
        padding: 3px;
        margin-top: 10px;
        background: var(--pnkx-surface);
        border: 1px solid var(--pnkx-border-soft);
        border-radius: 999px;

        .switch-item {
            flex: 1;
            height: 32px;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 5px;
            font-size: 12px;
            color: var(--pnkx-text-muted);
            border-radius: 999px;
            transition: all 0.25s;

            &.active {
                color: var(--pnkx-primary);
                background: var(--pnkx-primary-soft);
                font-weight: 600;
            }
        }
    }
}

.stat-item {
    min-width: 0;
    padding: 8px 4px;
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 10px;
    background: var(--pnkx-surface);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;

    &__value {
        font-size: 16px;
        font-weight: 700;
        color: var(--pnkx-text);
        line-height: 1.1;
    }

    &__label {
        font-size: 10px;
        color: var(--pnkx-text-muted);
        line-height: 1.2;
    }
}

.calendar-card {
    padding: 8px 4px 4px;

    :deep(.van-calendar) {
        background: transparent;
    }

    :deep(.van-calendar__bottom-info) {
        bottom: 2px;
        font-size: 10px;
        color: var(--pnkx-primary);
    }

    :deep(.van-calendar__day) {
        height: 52px;
    }

    :deep(.van-calendar__header-subtitle),
    :deep(.van-calendar__month-title) {
        color: var(--pnkx-text);
    }
}

.section-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 14px;
    font-weight: 600;
    color: var(--pnkx-text);
    margin: 14px 2px 10px;

    .section-count {
        font-size: 12px;
        font-weight: 400;
        color: var(--pnkx-text-muted);
    }
}
</style>
