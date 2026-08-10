<!--
 * @File: index
 * @Author: PHY
 * @Date: 2026/07/04
 * @Description: 家庭日历 - 聚合待办/纪念日/经期/记账
-->
<template>
    <div class="calendar-container">
        <!-- Today Cockpit 今日概览 -->
        <div class="cockpit-strip">
            <div class="cockpit-item todo-item" v-if="cockpit.todoCount > 0">
                <span class="cockpit-icon">📋</span>
                <span class="cockpit-text">{{ cockpit.todoCount }} 条待办到期</span>
                <el-tag v-if="hasOverdue" type="danger" size="small">有逾期</el-tag>
            </div>
            <div class="cockpit-item" v-if="cockpit.nextCommemoration">
                <span class="cockpit-icon">🎉</span>
                <span class="cockpit-text">{{ cockpit.nextCommemoration.name }} 还有
                    <strong>{{ cockpit.nextCommemoration.daysLeft }}</strong> 天</span>
            </div>
            <div class="cockpit-item" v-if="!cockpit.todoCount && !cockpit.nextCommemoration && !cockpitLoading">
                <span class="cockpit-icon">✅</span>
                <span class="cockpit-text">今天没有待办和纪念日</span>
            </div>
        </div>

        <!-- 日历主体 -->
        <div v-loading="loading" class="calendar-main">
            <!-- 视图切换 -->
            <div class="view-switcher">
                <el-radio-group v-model="viewMode" size="small">
                    <el-radio-button value="month">月视图</el-radio-button>
                    <el-radio-button value="week">周视图</el-radio-button>
                </el-radio-group>
                <el-button-group v-if="viewMode === 'week'" size="small" style="margin-left: 12px">
                    <el-button @click="shiftWeek(-1)">上一周</el-button>
                    <el-button @click="shiftWeek(0)">本周</el-button>
                    <el-button @click="shiftWeek(1)">下一周</el-button>
                </el-button-group>
            </div>

            <!-- 月视图（el-calendar） -->
            <el-calendar v-if="viewMode === 'month'" v-model="currentDate">
                <template #date-cell="{ date, data }">
                    <div class="day-cell" :class="{ 'is-today': data.type === 'current-month' && isToday(data.day) }">
                        <span class="day-number">{{ data.day.slice(8) }}</span>
                        <div class="day-events">
                            <div v-for="(ev, i) in getEventsByDate(data.day)"
                                 :key="i"
                                 class="event-dot"
                                 :class="'event-' + ev.color"
                                 :title="ev.title"
                                 @click="handleEventClick(ev)">
                                <span class="event-title">{{ ev.title }}</span>
                            </div>
                            <div v-if="getEventsByDate(data.day).length > 3"
                                 class="event-more">
                                +{{ getEventsByDate(data.day).length - 3 }} 条
                            </div>
                        </div>
                    </div>
                </template>
            </el-calendar>

            <!-- 周视图（CSS Grid 等分 7 列） -->
            <div v-else class="week-view">
                <div class="week-grid week-header">
                    <div v-for="(d, i) in weekDays" :key="'h'+i"
                         class="week-header-cell"
                         :class="{ 'is-today': isToday(d.full) }">
                        <span class="week-weekday">{{ weekdayLabels[i] }}</span>
                        <span class="week-date">{{ d.label }}</span>
                    </div>
                </div>
                <div class="week-grid week-body">
                    <div v-for="(d, i) in weekDays" :key="'b'+i"
                         class="week-day-cell"
                         :class="{ 'is-today': isToday(d.full) }">
                        <div v-for="(ev, j) in getEventsByDate(d.full)" :key="j"
                             class="event-dot"
                             :class="'event-' + ev.color"
                             :title="ev.title"
                             @click="handleEventClick(ev)">
                            <span class="event-title">{{ ev.title }}</span>
                        </div>
                        <div v-if="getEventsByDate(d.full).length === 0" class="week-empty">—</div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 图例 -->
        <div class="legend">
            <span class="legend-item"><span class="legend-dot event-todo"></span>待办</span>
            <span class="legend-item"><span class="legend-dot event-commemoration"></span>纪念日</span>
            <span class="legend-item"><span class="legend-dot event-menstruation"></span>经期</span>
            <span class="legend-item"><span class="legend-dot event-bookkeeping"></span>记账</span>
        </div>
    </div>
</template>

<script>
import {getMonthEvents, getCockpit} from '@/api/px/life/calendar'

export default {
    name: 'FamilyCalendar',
    data() {
        return {
            currentDate: new Date(),
            viewMode: 'month',
            weekOffset: 0,
            loading: false,
            cockpitLoading: false,
            events: [],
            cockpit: {},
            // 按日期分组的事件缓存
            eventsByDate: {},
            weekdayLabels: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
        }
    },
    computed: {
        hasOverdue() {
            return (this.cockpit.todayTodos || []).some(t => t.overdue)
        },
        weekDays() {
            const base = new Date(this.currentDate)
            // 周一为一周起点
            const day = base.getDay() || 7
            const monday = new Date(base)
            monday.setDate(base.getDate() - day + 1 + this.weekOffset * 7)
            const days = []
            for (let i = 0; i < 7; i++) {
                const d = new Date(monday)
                d.setDate(monday.getDate() + i)
                days.push({
                    full: this.formatDate(d),
                    label: `${d.getMonth() + 1}/${d.getDate()}`
                })
            }
            return days
        }
    },
    watch: {
        currentDate() {
            this.loadMonthEvents()
        }
    },
    mounted() {
        this.loadMonthEvents()
        this.loadCockpit()
    },
    methods: {
        loadMonthEvents() {
            const d = new Date(this.currentDate)
            const year = d.getFullYear()
            const month = d.getMonth() // 0-based
            // 取上月15日到下月15日（覆盖跨月的事件显示）
            const startDate = new Date(year, month - 1, 15)
            const endDate = new Date(year, month + 2, 0) // 下下月0日=下月底
            const sd = this.formatDate(startDate)
            const ed = this.formatDate(endDate)
            this.loading = true
            getMonthEvents(sd, ed).then(res => {
                this.events = res.data || []
                this.buildIndex()
            }).finally(() => {
                this.loading = false
            })
        },
        buildIndex() {
            const map = {}
            for (const ev of this.events) {
                const day = ev.date // "yyyy-MM-dd"
                if (!day) continue
                const key = day.substring(0, 10)
                if (!map[key]) map[key] = []
                map[key].push(ev)
            }
            this.eventsByDate = map
        },
        getEventsByDate(day) {
            // day 格式 "YYYY-MM-DD"
            const key = day.substring(0, 10)
            return (this.eventsByDate[key] || []).slice(0, 4) // 最多显示4条
        },
        loadCockpit() {
            this.cockpitLoading = true
            getCockpit().then(res => {
                this.cockpit = res.data || {}
            }).finally(() => {
                this.cockpitLoading = false
            })
        },
        handleEventClick(ev) {
            if (ev.route) {
                // 记账详情按项目约定读取 recordId 参数
                const queryKey = ev.sourceType === 'bookkeeping' ? 'recordId' : 'id'
                this.$router.push({path: ev.route, query: {[queryKey]: ev.sourceId}}).catch(() => {})
            }
        },
        shiftWeek(offset) {
            if (offset === 0) {
                this.weekOffset = 0
            } else {
                this.weekOffset += offset
            }
        },
        isToday(dayStr) {
            const today = new Date()
            const y = today.getFullYear()
            const m = String(today.getMonth() + 1).padStart(2, '0')
            const d = String(today.getDate()).padStart(2, '0')
            return dayStr === `${y}-${m}-${d}`
        },
        formatDate(date) {
            const y = date.getFullYear()
            const m = String(date.getMonth() + 1).padStart(2, '0')
            const d = String(date.getDate()).padStart(2, '0')
            return `${y}-${m}-${d}`
        }
    }
}
</script>

<style lang="scss" scoped>
.calendar-container {
    padding: 12px;
    display: flex;
    flex-direction: column;
    gap: 12px;
}

/* Today Cockpit */
.cockpit-strip {
    display: flex;
    gap: 16px;
    padding: 10px 16px;
    background: var(--pnkx-surface-muted);
    border: 1px solid var(--pnkx-border);
    border-radius: var(--pnkx-radius-md);
    flex-wrap: wrap;

    .cockpit-item {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 14px;
        color: var(--pnkx-text);

        .cockpit-icon {
            font-size: 16px;
        }

        strong {
            color: var(--pnkx-primary);
            font-weight: 700;
        }
    }
}

/* 日历主体 */
.calendar-main {
    :deep(.el-calendar) {
        background: var(--pnkx-surface);
        border: 1px solid var(--pnkx-border);
        border-radius: var(--pnkx-radius-md);
    }

    :deep(.el-calendar-table .el-calendar-day) {
        height: auto;
        min-height: 80px;
        padding: 4px 6px;
        vertical-align: top;
    }

    :deep(.el-calendar-table td.is-today .el-calendar-day) {
        background: var(--pnkx-primary-soft);
    }
}

.day-cell {
    min-height: 60px;

    .day-number {
        font-size: 14px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &.is-today .day-number {
        color: var(--pnkx-primary);
    }
}

.day-events {
    margin-top: 4px;
    display: flex;
    flex-direction: column;
    gap: 2px;
}

.event-dot {
    font-size: 11px;
    padding: 1px 6px;
    border-radius: 3px;
    cursor: pointer;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    transition: opacity 0.16s;

    &:hover {
        opacity: 0.8;
    }
}

.event-title {
    font-size: 11px;
    line-height: 1.6;
}

/* 来源着色 */
.event-todo {
    background: rgba(230, 162, 60, 0.15);
    color: #e6a23c;
    border-left: 3px solid #e6a23c;
}

.event-commemoration {
    background: rgba(103, 194, 58, 0.15);
    color: #67c23a;
    border-left: 3px solid #67c23a;
}

.event-menstruation {
    background: rgba(245, 108, 108, 0.15);
    color: #f56c6c;
    border-left: 3px solid #f56c6c;
}

.event-bookkeeping {
    background: rgba(64, 158, 255, 0.15);
    color: #409eff;
    border-left: 3px solid #409eff;
}

.event-more {
    font-size: 11px;
    color: var(--pnkx-text-secondary);
    padding-left: 6px;
}

/* 图例 */
.legend {
    display: flex;
    gap: 16px;
    justify-content: center;
    font-size: 13px;
    color: var(--pnkx-text-secondary);

    .legend-item {
        display: flex;
        align-items: center;
        gap: 4px;
    }

    .legend-dot {
        width: 10px;
        height: 10px;
        border-radius: 2px;
        display: inline-block;
    }
}

/* 视图切换 */
.view-switcher {
    display: flex;
    align-items: center;
    margin-bottom: 8px;
}

/* 周视图 */
.week-view {
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border);
    border-radius: var(--pnkx-radius-md);
    overflow: hidden;
}

/* CSS Grid 等分 7 列，列间用 gap 取代 gutter，列宽天然等分不挤压 */
.week-grid {
    display: grid;
    grid-template-columns: repeat(7, minmax(0, 1fr));
}

.week-header {
    border-bottom: 1px solid var(--pnkx-border);

    .week-header-cell {
        text-align: center;
        padding: 10px 4px;
        background: var(--pnkx-surface-muted);

        &.is-today {
            background: var(--pnkx-primary-soft);
        }

        .week-weekday {
            display: block;
            font-weight: 600;
            font-size: 13px;
            color: var(--pnkx-text);
        }

        .week-date {
            display: block;
            font-size: 12px;
            color: var(--pnkx-text-secondary);
            margin-top: 2px;
        }
    }
}

.week-body {
    .week-day-cell {
        min-height: 300px;
        padding: 8px 6px;
        border-right: 1px solid var(--pnkx-border);

        &:last-child {
            border-right: none;
        }

        &.is-today {
            background: var(--pnkx-primary-soft);
        }

        .week-empty {
            text-align: center;
            color: var(--pnkx-text-placeholder);
            padding-top: 40px;
        }

        .event-dot {
            margin-bottom: 6px;
        }
    }
}
</style>
