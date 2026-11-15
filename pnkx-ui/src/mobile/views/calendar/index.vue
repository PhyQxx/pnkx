<template>
    <div class="calendar-page">
        <!-- 月份导航 -->
        <div class="cal-header">
            <div class="cal-header__nav" @click="shiftMonth(-1)"><van-icon name="arrow-left" /></div>
            <div class="cal-header__title" @click="goToday">{{ year }}年{{ month + 1 }}月</div>
            <div class="cal-header__nav" @click="shiftMonth(1)"><van-icon name="arrow" /></div>
        </div>

        <van-pull-refresh v-model="refreshing" class="m-pull" @refresh="onRefresh">
            <!-- 今日驾驶舱 -->
            <div class="m-page cockpit">
                <div class="m-card cockpit-card" @click="goToday">
                    <div class="cockpit-card__row">
                        <div class="cockpit-card__item">
                            <span class="cockpit-card__value">{{ cockpit.todayEventCount || 0 }}</span>
                            <span class="cockpit-card__label">今日事件</span>
                        </div>
                        <div class="cockpit-card__divider"></div>
                        <div class="cockpit-card__item">
                            <span class="cockpit-card__value" :class="{ 'cockpit-card__value--danger': (cockpit.todoCount || 0) > 0 }">{{ cockpit.todoCount || 0 }}</span>
                            <span class="cockpit-card__label">今日待办</span>
                        </div>
                        <div class="cockpit-card__divider"></div>
                        <div class="cockpit-card__item cockpit-card__item--wide">
                            <span class="cockpit-card__value cockpit-card__value--text">
                                {{ cockpit.nextCommemoration ? cockpit.nextCommemoration.name + ' · ' + cockpit.nextCommemoration.daysLeft + '天后' : '—' }}
                            </span>
                            <span class="cockpit-card__label">下一纪念日</span>
                        </div>
                    </div>
                    <div v-if="todayTodoList.length > 0" class="cockpit-card__todos">
                        <div v-for="t in todayTodoList" :key="t.id" class="cockpit-card__todo" :class="{ 'cockpit-card__todo--overdue': t.overdue }">
                            <van-icon :name="t.overdue ? 'warning-o' : 'clock-o'" />
                            <span class="cockpit-card__todo-text">{{ t.content }}</span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 星期 -->
            <div class="m-page cal-body">
                <div class="cal-weekdays">
                    <span v-for="(w, i) in weekdays" :key="i" class="cal-weekday">{{ w }}</span>
                </div>

                <!-- 日期网格 -->
                <div class="cal-grid">
                    <div
                        v-for="(day, i) in calendarDays"
                        :key="i"
                        class="cal-day"
                        :class="{
                            'cal-day--other': !day.currentMonth,
                            'cal-day--today': day.isToday,
                            'cal-day--selected': day.dateStr === selectedDate
                        }"
                        @click="selectDay(day)"
                    >
                        <span class="cal-day__num">{{ day.day }}</span>
                        <div v-if="day.eventCount > 0" class="cal-day__dots">
                            <span
                                v-for="(color, ci) in day.eventColors"
                                :key="ci"
                                class="cal-day__dot"
                                :style="{ background: colorOf(color) }"
                            ></span>
                        </div>
                    </div>
                </div>

                <!-- 图例 -->
                <div v-if="legendTypes.length > 0" class="cal-legend">
                    <span v-for="t in legendTypes" :key="t" class="cal-legend__item">
                        <span class="cal-day__dot" :style="{ background: colorOf(t) }"></span>
                        {{ typeLabel(t) }}
                    </span>
                </div>
            </div>

            <!-- 选中日的事件 -->
            <div class="m-page">
                <div class="m-card cal-events">
                    <div class="cal-events__title">
                        <span class="m-section-title cal-events__heading">{{ selectedDateLabel }}</span>
                        <span v-if="dayEvents.length > 0" class="cal-events__count">{{ dayEvents.length }}项</span>
                    </div>
                    <div v-if="dayEvents.length > 0" class="cal-events__list">
                        <div
                            v-for="(ev, i) in dayEvents"
                            :key="i"
                            class="cal-event"
                            @click="navigateToSource(ev)"
                        >
                            <div class="cal-event__bar" :style="{ background: colorOf(ev.color) }"></div>
                            <div class="cal-event__body">
                                <div class="cal-event__title">{{ ev.title }}</div>
                                <div class="cal-event__type">{{ typeLabel(ev.sourceType) }} · {{ statusLabel(ev.status) }}</div>
                            </div>
                            <span
                                v-if="ev.sourceType === 'todo' && ev.status !== 'completed'"
                                class="cal-event__done"
                                @click.stop="completeTodo(ev)"
                            >完成</span>
                            <van-icon name="arrow" class="cal-event__arrow" />
                        </div>
                    </div>
                    <div v-else class="cal-events__empty">这天没有安排</div>
                    <div class="quick-actions">
                        <span class="quick-actions__item" @click="createForDate('todo')">＋待办</span>
                        <span class="quick-actions__item" @click="createForDate('bookkeeping')">＋记账</span>
                        <span class="quick-actions__item" @click="createForDate('diary')">＋日记</span>
                        <span class="quick-actions__item" @click="createForDate('meal')">调整计划</span>
                    </div>
                </div>
            </div>
        </van-pull-refresh>
    </div>
</template>

<script>
import { getMonthEvents, getCockpit } from '@/api/px/life/calendar'
import { getDo, updateDo } from '@/api/px/life/todo'
import { uniappPathToMobile } from '@/mobile/router/map'
import { showToast, mSuccess } from '@/mobile/utils/ui'

const WEEKDAYS = ['日', '一', '二', '三', '四', '五', '六']
const COLOR_MAP = {
    todo: 'var(--pnkx-primary)',
    commemoration: 'var(--pnkx-warning)',
    menstruation: 'var(--pnkx-danger)',
    bookkeeping: 'var(--pnkx-info)',
    subscription: 'var(--pnkx-warning)',
    meal_plan: 'var(--pnkx-success)',
    shopping_plan: 'var(--pnkx-primary)',
    reading_goal: 'var(--pnkx-info)',
    life_report: 'var(--pnkx-danger)'
}

export default {
    name: 'MCalendar',
    data() {
        const today = new Date()
        return {
            year: today.getFullYear(),
            month: today.getMonth(),
            weekdays: WEEKDAYS,
            events: [],
            selectedDate: this.formatDate(today),
            cockpit: {},
            refreshing: false
        }
    },
    computed: {
        calendarDays() {
            const firstDay = new Date(this.year, this.month, 1)
            const startWeekday = firstDay.getDay()
            const daysInMonth = new Date(this.year, this.month + 1, 0).getDate()
            const daysInPrev = new Date(this.year, this.month, 0).getDate()
            const todayStr = this.formatDate(new Date())
            const days = []
            // 上月填充
            for (let i = startWeekday - 1; i >= 0; i--) {
                days.push(this.buildDay(new Date(this.year, this.month - 1, daysInPrev - i), false, todayStr))
            }
            // 本月
            for (let d = 1; d <= daysInMonth; d++) {
                days.push(this.buildDay(new Date(this.year, this.month, d), true, todayStr))
            }
            // 下月填充至 42 格
            let nextDay = 1
            while (days.length < 42) {
                days.push(this.buildDay(new Date(this.year, this.month + 1, nextDay++), false, todayStr))
            }
            return days
        },
        dayEvents() {
            return this.events.filter(e => (e.date || '').slice(0, 10) === this.selectedDate)
        },
        selectedDateLabel() {
            if (!this.selectedDate) return ''
            const d = new Date(this.selectedDate)
            return (d.getMonth() + 1) + '月' + d.getDate() + '日'
        },
        legendTypes() {
            return [...new Set(this.events.map(e => e.color).filter(Boolean))]
        },
        todayTodoList() {
            return (this.cockpit.todayTodos || []).slice(0, 3)
        }
    },
    mounted() {
        this.loadAll()
    },
    methods: {
        formatDate(d) {
            const m = (d.getMonth() + 1).toString().padStart(2, '0')
            const day = d.getDate().toString().padStart(2, '0')
            return d.getFullYear() + '-' + m + '-' + day
        },
        buildDay(date, currentMonth, todayStr) {
            const dateStr = this.formatDate(date)
            const dayEvents = this.events.filter(e => (e.date || '').slice(0, 10) === dateStr)
            return {
                day: date.getDate(),
                dateStr,
                currentMonth,
                isToday: dateStr === todayStr,
                eventCount: dayEvents.length,
                eventColors: [...new Set(dayEvents.map(e => e.color))].slice(0, 3)
            }
        },
        shiftMonth(delta) {
            let m = this.month + delta
            let y = this.year
            if (m < 0) { m = 11; y-- } else if (m > 11) { m = 0; y++ }
            this.year = y
            this.month = m
            this.loadEvents()
        },
        goToday() {
            const today = new Date()
            this.year = today.getFullYear()
            this.month = today.getMonth()
            this.selectedDate = this.formatDate(today)
            this.loadEvents()
        },
        async loadAll() {
            this.loadEvents()
            try {
                const res = await getCockpit()
                this.cockpit = res.data || {}
            } catch (e) {
                this.cockpit = {}
            }
        },
        async loadEvents() {
            const start = new Date(this.year, this.month, 1)
            const end = new Date(this.year, this.month + 1, 0)
            try {
                const res = await getMonthEvents(this.formatDate(start), this.formatDate(end))
                this.events = res.data || []
            } catch (e) {
                showToast('加载失败')
            }
        },
        async onRefresh() {
            await this.loadAll()
            this.refreshing = false
        },
        selectDay(day) {
            this.selectedDate = day.dateStr
        },
        colorOf(color) {
            return COLOR_MAP[color] || 'var(--pnkx-text-secondary)'
        },
        typeLabel(type) {
            const map = {
                todo: '待办', commemoration: '纪念日', menstruation: '经期', bookkeeping: '记账',
                subscription: '订阅', meal_plan: '餐饮计划', shopping_plan: '购物计划',
                reading_goal: '阅读目标', life_report: '生活报告'
            }
            return map[type] || type
        },
        statusLabel(status) {
            return { pending: '待处理', completed: '已完成', info: '信息' }[status] || '信息'
        },
        async completeTodo(ev) {
            try {
                const response = await getDo(ev.sourceId)
                await updateDo({ ...response.data, status: true, finishTime: new Date().toISOString() })
                mSuccess('待办已完成')
                this.loadEvents()
            } catch (e) {
                showToast('操作失败')
            }
        },
        createForDate(type) {
            const routes = {
                todo: '/m/todo/edit?date=' + this.selectedDate,
                bookkeeping: '/m/bookkeeping/record/add?date=' + this.selectedDate,
                diary: '/m/diary/edit?date=' + this.selectedDate,
                meal: '/m/meal-plan?date=' + this.selectedDate
            }
            this.go(routes[type])
        },
        navigateToSource(ev) {
            let path = ''
            if (ev.appRoute) {
                const [raw, query] = String(ev.appRoute).split('?')
                const base = uniappPathToMobile(raw)
                path = base ? (query ? base + '?' + query : base) : ''
            }
            if (!path) {
                const map = {
                    todo: '/m/todo',
                    commemoration: '/m/commemoration-day',
                    menstruation: '/m/menstruation'
                }
                path = map[ev.sourceType] || ''
            }
            this.go(path)
        },
        /**
         * 跳转（目标路由可能未注册，兜底提示）
         */
        go(path) {
            if (path && this.$router.resolve(path).matched.length > 0) {
                this.$router.push(path)
            } else {
                showToast('功能即将上线')
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.calendar-page {
    min-height: calc(100vh - 46px);
    background: var(--pnkx-bg);
    padding-bottom: 24px;
}

/* Header */
.cal-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 12px;
    background: var(--pnkx-surface);
    border-bottom: 1px solid var(--pnkx-border-soft);

    &__title {
        font-size: 16px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__nav {
        width: 32px;
        height: 32px;
        border-radius: 50%;
        background: var(--pnkx-bg);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 14px;
        color: var(--pnkx-text-secondary);

        &:active { opacity: 0.6; }
    }
}

.cockpit {
    padding-bottom: 0;

    .cockpit-card {
        margin-bottom: 0;
    }
}

.cockpit-card {
    &__row {
        display: flex;
        align-items: center;
    }

    &__item {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 2px;
        min-width: 0;

        &--wide {
            flex: 1.4;
        }
    }

    &__value {
        max-width: 100%;
        font-size: 18px;
        font-weight: 700;
        color: var(--pnkx-primary);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;

        &--danger { color: var(--pnkx-danger); }
        &--text { font-size: 14px; }
    }

    &__label {
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }

    &__divider {
        width: 1px;
        height: 26px;
        background: var(--pnkx-border-soft);
    }

    &__todos {
        margin-top: 10px;
        padding-top: 10px;
        border-top: 1px solid var(--pnkx-border-soft);
    }

    &__todo {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 12px;
        color: var(--pnkx-text-secondary);
        padding: 3px 0;

        &--overdue {
            color: var(--pnkx-danger);
        }
    }

    &__todo-text {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }
}

.cal-body {
    padding-bottom: 0;
}

.cal-weekdays {
    display: flex;
    margin-bottom: 4px;
}

.cal-weekday {
    flex: 1;
    text-align: center;
    font-size: 11px;
    color: var(--pnkx-text-muted);
}

.cal-grid {
    display: flex;
    flex-wrap: wrap;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 12px;
    padding: 4px;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.cal-day {
    width: calc(100% / 7);
    aspect-ratio: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    position: relative;

    &__num {
        font-size: 14px;
        color: var(--pnkx-text);
        width: 28px;
        height: 28px;
        line-height: 28px;
        text-align: center;
        border-radius: 50%;
    }

    &--other &__num {
        color: var(--pnkx-text-muted);
        opacity: 0.4;
    }

    &--today &__num {
        background: var(--pnkx-primary);
        color: #fff;
        font-weight: 700;
    }

    &--selected:not(.cal-day--today) &__num {
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
        font-weight: 600;
    }

    &__dots {
        position: absolute;
        bottom: 5px;
        display: flex;
        gap: 2px;
    }

    &__dot {
        width: 4px;
        height: 4px;
        border-radius: 50%;
    }
}

.cal-legend {
    display: flex;
    flex-wrap: wrap;
    gap: 4px 12px;
    padding: 10px 2px 0;

    &__item {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }
}

/* Events */
.cal-events {
    &__title {
        display: flex;
        align-items: center;
        margin-bottom: 8px;
    }

    &__heading {
        margin: 0;
        flex: 1;
        min-width: 0;
    }

    &__count {
        font-size: 11px;
        color: var(--pnkx-primary);
    }

    &__empty {
        padding: 16px 0;
        text-align: center;
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }
}

.cal-event {
    display: flex;
    align-items: center;
    padding: 8px 0;
    border-bottom: 1px solid var(--pnkx-border-soft);

    &:last-child { border-bottom: none; }
    &:active { opacity: 0.7; }

    &__bar {
        width: 3px;
        height: 24px;
        border-radius: 2px;
        margin-right: 10px;
        flex-shrink: 0;
    }

    &__body {
        flex: 1;
        min-width: 0;
    }

    &__title {
        font-size: 14px;
        color: var(--pnkx-text);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__type {
        font-size: 11px;
        color: var(--pnkx-text-muted);
        margin-top: 2px;
    }

    &__arrow {
        font-size: 14px;
        color: var(--pnkx-text-muted);
    }

    &__done {
        margin-right: 8px;
        padding: 3px 10px;
        border-radius: 999px;
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
        font-size: 11px;
    }
}

.quick-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 12px;
    padding-top: 10px;
    border-top: 1px solid var(--pnkx-border-soft);

    &__item {
        padding: 5px 12px;
        border-radius: 999px;
        background: var(--pnkx-bg);
        color: var(--pnkx-primary);
        font-size: 12px;

        &:active { opacity: 0.7; }
    }
}
</style>
