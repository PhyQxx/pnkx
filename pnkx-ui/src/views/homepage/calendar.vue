<template>
    <div class="calendar">
        <el-calendar v-model="day" v-loading="loading">
            <template #date-cell="{ date, data }">
                <div class="date-template" @click="handleOpenDay(data)">
                    <div class="day">{{ data.day.slice(5) }}</div>
                    <div class="affair">
                        <!-- 姨妈助手-周期提醒 -->
                        <div class="icon one-box" v-if="ovulation(data).actual || ovulation(data).estimate">
                            <el-tooltip class="item"
                                        effect="light"
                                        content="月经正常的情况下，女性从下次月经第一天开始算，倒数第14天为排卵日"
                                        placement="top"
                            >
                                <svg-icon
                                    :style="ovulation(data).estimate ? 'opacity:0.4;' : ''"
                                    icon-class="收藏02"
                                />
                            </el-tooltip>
                        </div>
                        <!-- 姨妈助手-姨妈开始提醒 -->
                        <div class="icon one-box" v-if="start(data).actual || start(data).estimate">
                            <el-tooltip class="item" effect="light"
                                        :content="start(data).estimate ? '预计姨妈开始' : '姨妈开始'" placement="top"
                            >
                                <svg-icon
                                    :style="start(data).estimate ? 'opacity:0.4;' : ''"
                                    icon-class="播放"
                                />
                            </el-tooltip>
                        </div>
                        <!-- 姨妈助手-姨妈结束提醒 -->
                        <div class="icon one-box" v-if="end(data).actual || end(data).estimate">
                            <el-tooltip class="item" effect="light"
                                        :content="end(data).estimate ? '预计姨妈结束' : '姨妈结束'" placement="top"
                            >
                                <svg-icon
                                    :style="end(data).estimate ? 'opacity:0.4;' : ''"
                                    icon-class="暂停"
                                />
                            </el-tooltip>
                        </div>
                        <!-- 姨妈助手-LOVE -->
                        <div class="icon one-box" v-if="findMenstruationRecord(data).makeLove">
                            <el-tooltip class="item" effect="light"
                                        content="啪啪虽爽，注意身体" placement="top"
                            >
                                <svg-icon icon-class="x-色"/>
                            </el-tooltip>
                        </div>
                        <!-- 姨妈助手-孕检 -->
                        <div class="icon one-box"
                             v-if="findMenstruationRecord(data).items || findMenstruationRecord(data).results"
                        >
                            <el-tooltip class="item" effect="light"
                                        content="孕检记录~" placement="top"
                            >
                                <svg-icon icon-class="picture"/>
                            </el-tooltip>
                        </div>
                        <!-- 日记 -->
                        <div class="icon one-box" v-if="diary(data)">
                            <el-tooltip class="item" effect="light"
                                        :content="diary(data)" placement="top"
                            >
                                <svg-icon icon-class="时间-time"/>
                            </el-tooltip>
                        </div>
                        <!-- 待办 -->
                        <div class="icon one-box todo" v-if="todo(data)">
                            <el-tooltip class="item" effect="light"
                                        :content="todo(data)" placement="top"
                            >
                                <svg-icon icon-class="todo"/>
                            </el-tooltip>
                        </div>
                        <!-- 纪念日 -->
                        <div class="icon one-box commemoration" v-if="commemorationDay(data)">
                            <el-tooltip class="item" effect="light"
                                        :content="commemorationDay(data)" placement="top"
                            >
                                <svg-icon icon-class="纪念日"/>
                            </el-tooltip>
                        </div>
                    </div>
                </div>
            </template>
        </el-calendar>

        <!-- 日历抽屉 -->
        <el-drawer
            :title="drawer.title"
            v-model="drawer.visible"
            direction="rtl"
            :modal="false"
        >
            <div class="day-content">
                <!-- 排卵日提醒 -->
                <div v-if="ovulation(drawer).actual || ovulation(drawer).estimate" class="thing-item">
                    <div class="thing-title">
                        <svg-icon icon-class="收藏02"/>
                        <span>排卵日提醒</span>
                    </div>
                    <div class="thing-content">
                        月经正常的情况下，女性从下次月经第一天开始算，倒数第14天为排卵日
                    </div>
                </div>
                <!-- 姨妈提醒-开始 -->
                <div
                    v-if="start(drawer).actual || start(drawer).estimate"
                    class="thing-item"
                >
                    <div class="thing-title">
                        <svg-icon icon-class="播放"/>
                        <span>姨妈提醒</span>
                    </div>
                    <div class="thing-content">
                        {{ start(drawer).estimate ? '预计姨妈开始' : '姨妈开始' }}
                    </div>
                </div>
                <!-- 姨妈提醒-结束 -->
                <div
                    v-if="end(drawer).actual || end(drawer).estimate"
                    class="thing-item"
                >
                    <div class="thing-title">
                        <svg-icon icon-class="暂停"/>
                        <span>姨妈提醒</span>
                    </div>
                    <div class="thing-content">
                        {{ end(drawer).estimate ? '预计姨妈结束' : '姨妈结束' }}
                    </div>
                </div>
                <!-- 姨妈助手-LOVE -->
                <div v-if="findMenstruationRecord(drawer).makeLove" class="thing-item">
                    <div class="thing-title">
                        <svg-icon icon-class="x-色"/>
                        <span>姨妈提醒</span>
                    </div>
                    <div class="thing-content">
                        啪啪虽爽，注意身体
                    </div>
                </div>
                <!-- 姨妈助手-孕检 -->
                <div class="thing-item"
                     v-if="findMenstruationRecord(drawer).items || findMenstruationRecord(drawer).results"
                >
                    <div class="thing-title">
                        <svg-icon icon-class="picture"/>
                        <span>孕检记录</span>
                    </div>
                    <div class="thing-content">
                        <div class="thing-content-item">
                            <div class="label">检查项目：</div>
                            <div class="value">{{ findMenstruationRecord(drawer).items }}</div>
                        </div>
                        <div class="thing-content-item">
                            <div class="label">检查结果：</div>
                            <div class="value">{{ findMenstruationRecord(drawer).results }}</div>
                        </div>
                    </div>
                </div>
                <!-- 日记 -->
                <div class="thing-item" v-if="diary(drawer)">
                    <div class="thing-title">
                        <svg-icon icon-class="时间-time"/>
                        <span>日记</span>
                    </div>
                    <div class="thing-content">
                        {{ diary(drawer) }}
                    </div>
                </div>
                <!-- 待办 -->
                <div class="thing-item" v-if="todo(drawer)">
                    <div class="thing-title">
                        <svg-icon icon-class="todo"/>
                        <span>待办</span>
                    </div>
                    <div class="thing-content">
                        {{ todo(drawer) }}
                    </div>
                </div>
                <!-- 纪念日 -->
                <div class="thing-item" v-if="commemorationDay(drawer)">
                    <div class="thing-title">
                        <svg-icon icon-class="纪念日"/>
                        <span>纪念日</span>
                    </div>
                    <div class="thing-content">
                        {{ commemorationDay(drawer) }}
                    </div>
                </div>
            </div>
        </el-drawer>
    </div>
</template>

<script>
import { listMenstruationRecord } from '@/api/px/life/menstruationRecord'
import { listDiary } from '@/api/px/life/diary'
import { listDo } from '@/api/px/life/todo'
import { listDay } from '@/api/px/life/commemorationDay'

export default {
    name: 'calendar',
    props: {
        menstruationAssistantSetting: {
            type: Object,
            default: () => ({ cycle: undefined, duration: undefined })
        }
    },
    data() {
        return {
            // 当前选中的日期
            day: new Date(),
            // 加载中
            loading: false,
            // html文本只显示文字的正则表达式
            regex: /(<([^>]+)>)/ig,
            // 姨妈记录
            menstruationRecord: [],
            // 日记
            diaryList: [],
            // 待办
            todoList: [],
            // 纪念日
            commemorationDayList: [],
            // 日历抽屉
            drawer: {
                day: '',
                // 标题
                title: '',
                // 是否显示
                visible: false
            }
        }
    },
    watch: {
        day: {
            handler(newDay, oldDay) {
                if (this.parseTime(newDay, '{y}-{m}') !== this.parseTime(oldDay, '{y}-{m}')) {
                    this.getRecord()
                }
            },
            immediate: true
        }
    },
    methods: {
        /**
         * 获取记录
         */
        getRecord() {
            const dateStr = this.parseTime(this.day)
            const monthStr = this.parseTime(this.day, '{y}-{m}')
            
            // 获取姨妈记录
            listMenstruationRecord({ date: dateStr }).then(res => {
                this.menstruationRecord = res.rows.map(item => {
                    return {
                        ...item,
                        date: this.parseTime(item.date, '{y}-{m}-{d}')
                    }
                })
            })
            // 获取日记
            listDiary({ date: dateStr }).then(res => {
                this.diaryList = res.rows;
            })
            // 获取待办
            listDo({ params: { date: dateStr } }).then(res => {
                this.todoList = res.rows;
            })
            // 获取纪念日
            listDay({ date: dateStr }).then(res => {
                this.commemorationDayList = res.rows;
            })
        },
        /**
         * 计算排卵日
         */
        ovulation(data) {
            const startData = this.menstruationRecord.filter(item => {
                return item.type === '0'
            }).map(item => {
                return {
                    ...item,
                    date: this.$dateChange(-14, item.date)
                }
            })
            return {
                estimate: data.day > this.parseTime(new Date()) && startData.length > 0 && this.$getDaysBetween(this.parseTime(startData[0].date, '{y}-{m}-{d}'), data.day) % this.menstruationAssistantSetting?.cycle === 0,
                actual: startData.some(item => this.parseTime(item.date, '{y}-{m}-{d}') === this.parseTime(data.day, '{y}-{m}-{d}'))
            }
        },
        /**
         * 开始标志
         */
        start(data) {
            let estimate = false
            let actual = this.menstruationRecord.find(item => this.parseTime(item.date) === this.parseTime(data.day)) && this.menstruationRecord.find(item => this.parseTime(item.date) === this.parseTime(data.day)).type === '0'
            const startData = this.menstruationRecord.filter(item => {
                return item.type === '0'
            })
            estimate = data.day > this.parseTime(new Date()) && startData.length > 0 && this.$getDaysBetween(this.parseTime(startData[0].date, '{y}-{m}-{d}'), data.day) % this.menstruationAssistantSetting?.cycle === 0
            return {
                estimate: estimate,
                actual: actual
            }
        },
        /**
         * 结束标志
         */
        end(data) {
            let estimate = false
            let actual = this.menstruationRecord.find(item => this.parseTime(item.date) === this.parseTime(data.day)) && this.menstruationRecord.find(item => this.parseTime(item.date) === this.parseTime(data.day)).type === '1'
            const startData = this.menstruationRecord.filter(item => {
                return item.type === '0'
            })
            estimate = data.day > this.parseTime(new Date()) && startData.length > 0 && (this.$getDaysBetween(this.parseTime(startData[0].date, '{y}-{m}-{d}'), data.day) - this.menstruationAssistantSetting?.duration + 1) % this.menstruationAssistantSetting?.cycle === 0
            return {
                estimate: estimate,
                actual: actual
            }
        },
        /**
         * 查询姨妈记录
         * @param data
         * @returns {*|(function(*))|boolean}
         */
        findMenstruationRecord(data) {
            // 获取当前记录
            const findRecord = this.menstruationRecord.find(item => this.parseTime(item.date) === this.parseTime(data.day))
            return findRecord || {}
        },
        /**
         * 日记
         * @param data
         * @returns {*|(function(*))|boolean}
         */
        diary(data) {
            return this.diaryList.find(item => item.date === data.day)?.content?.replace(this.regex, '')?.slice(0, 40)
        },
        /**
         * 待办
         * @param data
         * @returns {*|(function(*))|boolean}
         */
        todo(data) {
            // 待办
            const todoList = this.todoList.filter(item => this.$isDateInRange(item.planStartTime, item.planEndTime, data.day))
            return todoList.map((item, index) => (index + 1) + '.' + item.content?.slice(0, 40)).join('\n')
        },
        /**
         * 纪念日
         * @param data
         */
        commemorationDay(data) {
            const dayList = this.commemorationDayList.filter(item => this.parseTime(item.date, '{m}-{d}') === this.parseTime(data.day, '{m}-{d}'))
            return dayList.map(item => item.name).join('\n')
        },
        /**
         * 查看日历详情
         * @param data
         */
        handleOpenDay(data) {
            this.drawer.day = data.day
            this.drawer.title = this.parseTime(data.day, '{y}年{m}月{d}日')
            this.drawer.visible = true
        }
    }
}
</script>

<style scoped lang="scss">
.calendar {
    height: 100%;
    display: flex;
    flex-direction: column;

    ::v-deep .el-calendar {
        flex: 1;
        display: flex;
        flex-direction: column;
        background: transparent;

        .el-calendar__header {
            padding: 8px 12px;
            border-bottom: 1px solid rgba(0, 0, 0, 0.06);
        }

        .el-calendar__body {
            flex: 1;
            padding: 0;
        }
    }
}

.date-template {
    height: 100%;
    display: flex;
    flex-direction: column;
    cursor: pointer;
}

.day {
    font-size: 14px;
    font-weight: 600;
    height: 1.8rem;
    line-height: 1.8rem;
}

.affair {
    display: flex;
    flex-wrap: wrap;
    flex: 1;
    overflow: hidden;

    .one-box {
        margin-right: 4px;

        .svg-icon {
            font-size: 1.1rem;
        }
    }
}

::v-deep .el-calendar-table {
    height: 100%;
    table-layout: fixed;

    .el-calendar-day {
        height: 100%;
        padding: 4px 6px;
        box-sizing: border-box;
    }

    td {
        border: 1px solid rgba(0, 0, 0, 0.06);
    }

    .prev, .next {
        .day { color: #c0c4cc; }
    }

    td.is-selected {
        background-color: rgba(64, 158, 255, 0.06);
    }

    td.is-today {
        .day {
            color: #409eff;
        }
    }
}

.day-content {
    padding: 0 var(--space-4);

    .thing-item {
        margin-bottom: var(--space-4);
        padding: var(--space-3);
        border-radius: var(--radius-md);
        background-color: var(--bg-card);
        box-shadow: var(--shadow-sm);
        transition: box-shadow var(--duration-normal) var(--ease-default);

        &:hover {
            box-shadow: var(--shadow-md);
        }

        .thing-title {
            display: flex;
            align-items: center;
            font-size: var(--text-lg);
            font-weight: var(--font-semibold);
            margin-bottom: var(--space-2);

            span {
                margin-left: var(--space-2);
            }
        }

        .thing-content {
            color: var(--text-secondary);
            font-size: var(--text-sm);

            .thing-content-item {
                display: flex;

                .label {
                    color: var(--text-tertiary);
                    white-space: nowrap;
                }
            }
        }
    }
}
</style>
