<!--
 * @File: index
 * @Author: PHY
 * @Date: 2021-11-28 10:53
 * @Description: 姨妈助手
-->
<template>
    <div class="menstruation-assistant">
        <el-row type="flex" justify="space-between">
            <div>当前状态：<span
                class="state">{{
                    stateOptions.find(item => item.dictValue === queryParams.state) && stateOptions.find(item => item.dictValue === queryParams.state).dictLabel
                }}</span>
            </div>
            <div class="tip" v-if="queryParams.state=== 'whyl'">
                孕 <span class="theme-red">{{ pregnancy[0] }}</span> 周 <span
                class="theme-blue">{{ pregnancy[1] }}</span> 天 <span style="font-size: 1.1rem"
                                                                      class="theme-grey">— {{ pregnancy[2] }}</span>
            </div>
            <el-button type="primary" @click="showTable = !showTable">切换展示</el-button>
        </el-row>
        <el-calendar v-if="showTable" v-model="day">
            <template
                #date-cell="{date, data}">
                <div class="one-day theme-blue-text" :class="background(data)" @click="handleOpen(data)">
                    <div class="top">
                        <div class="date one-box">{{ data.day.slice(5) }}</div>
                    </div>
                    <div class="bottom">
                        <div class="icon one-box" v-if="ovulation(data).actual || ovulation(data).estimate">
                            <el-tooltip class="item"
                                        effect="light"
                                        content="月经正常的情况下，女性从下次月经第一天开始算，倒数第14天为排卵日"
                                        placement="top">
                                <svg-icon
                                    :style="ovulation(data).estimate ? 'opacity:0.4;' : ''"
                                    icon-class="收藏02"/>
                            </el-tooltip>
                        </div>
                        <div class="icon one-box" v-if="start(data).actual || start(data).estimate">
                            <el-tooltip class="item" effect="light"
                                        :content="start(data).estimate ? '预计姨妈开始' : '姨妈开始'" placement="top">
                                <svg-icon
                                    :style="start(data).estimate ? 'opacity:0.4;' : ''"
                                    icon-class="播放"/>
                            </el-tooltip>
                        </div>
                        <div class="icon one-box" v-if="end(data).actual || end(data).estimate">
                            <el-tooltip class="item" effect="light"
                                        :content="end(data).estimate ? '预计姨妈结束' : '姨妈结束'" placement="top">
                                <svg-icon
                                    :style="end(data).estimate ? 'opacity:0.4;' : ''"
                                    icon-class="暂停"/>
                            </el-tooltip>
                        </div>
                        <div class="icon one-box" v-if="findMenstruationRecord(data).makeLove">
                            <el-tooltip class="item" effect="light"
                                        content="啪啪虽爽，注意身体" placement="top">
                                <svg-icon icon-class="x-色"/>
                            </el-tooltip>
                        </div>
                        <div class="icon one-box"
                             v-if="findMenstruationRecord(data).items || findMenstruationRecord(data).results">
                            <el-tooltip class="item" effect="light"
                                        content="孕检记录~" placement="top">
                                <svg-icon icon-class="picture"/>
                            </el-tooltip>
                        </div>
                    </div>
                </div>
            </template>
        </el-calendar>
        <el-timeline v-if="!showTable" v-infinite-scroll="loadMore">
            <el-timeline-item :timestamp="parseTime(item.date)"
                              placement="bottom"
                              v-for="item in timelineRecord">
                <el-card>
                    <div class="menstruation-assistant-info">
                        <div class="left">
                            <span>心情：</span>
                            <svg-icon
                                :icon-class="item.mood"
                                class="el-input__icon"
                                style="height: 32px;width: 16px;"/>
                        </div>
                        <div class="right theme-blue-text" @click="handleOpen(null, item)">编辑</div>
                    </div>
                    <div>
                        <p><span>检查项目：</span><span>{{ item.items }}</span></p>
                        <p><span>检查结果：</span><span>{{ item.results }}</span></p>
                        <p><span>备注：</span><span>{{ item.remark }}</span></p>
                    </div>
                </el-card>
            </el-timeline-item>
        </el-timeline>
        <el-drawer
            title="记录美好一天"
            size="40%"
            destroy-on-close
            v-model="dayDialogVisible"
            :before-close="handleSaveDay">
            <el-form v-loading="saveDayLoading" class="day-form" ref="dayForm" :model="dayForm" label-width="10rem">
                <el-form-item label="时间" prop="date">
                    <el-date-picker
                        v-model="dayForm.date"
                        type="date"
                        placeholder="选择日期">
                    </el-date-picker>
                </el-form-item>
                <el-form-item label="检查项目" prop="items">
                    <el-input v-model="dayForm.items" placeholder="请输入检查项目"/>
                </el-form-item>
                <el-form-item label="检查结果" prop="results">
                    <el-input v-model="dayForm.results"
                              type="textarea"
                              :rows="3"
                              placeholder="请输入检查结果"/>
                </el-form-item>
                <el-form-item label="大姨妈来喽" prop="come" v-if="come()">
                    <el-switch v-model="dayForm.come"/>
                </el-form-item>
                <el-form-item label="大姨妈走喽" prop="go" v-if="go()">
                    <el-switch v-model="dayForm.go"/>
                </el-form-item>
                <el-form-item label="是否爱爱" prop="makeLove">
                    <el-switch v-model="dayForm.makeLove"/>
                </el-form-item>
                <el-form-item label="体温（摄氏度）" prop="temperature">
                    <el-input v-model="dayForm.temperature" type="number" placeholder="请输入体温"/>
                </el-form-item>
                <el-form-item label="体重（千克）" prop="weight">
                    <el-input v-model="dayForm.weight" type="number" placeholder="请输入体重"/>
                </el-form-item>
                <el-form-item label="心情">
                    <el-popover
                        placement="bottom-start"
                        width="460"
                        trigger="click"
                        @show="$refs['iconSelect'].reset()"
                    >
                        <icon-select ref="iconSelect" @selected="selected" prefix="x-"/>
                        <template #reference>
                            <el-input v-model="dayForm.mood" placeholder="点击选择心情" readonly>
                                <template #prefix>
                                    <svg-icon
                                        v-if="dayForm.mood"
                                        :icon-class="dayForm.mood"
                                        class="el-input__icon"
                                        style="height: 32px;width: 16px;"
                                    />
                                    <el-icon v-else>
                                        <Search/>
                                    </el-icon>
                                </template>
                            </el-input>
                        </template>
                    </el-popover>
                </el-form-item>
                <el-form-item label="备注" prop="remark">
                    <el-input v-model="dayForm.remark"
                              type="textarea"
                              :rows="3"
                              placeholder="请输入备注"/>
                </el-form-item>
                <el-button class="del-btn" type="danger" @click="handleDelete" :loading="deleteDayLoading">删 除
                </el-button>
            </el-form>
        </el-drawer>
    </div>
</template>

<script>
import IconSelect from "@/components/IconSelect/index.vue";
import {
    addMenstruationRecord,
    delMenstruationRecord,
    getLastStartDate,
    getPxMenstruationRecordList,
    listMenstruationRecord,
    updateMenstruationRecord
} from "@/api/px/life/menstruationRecord";

export default {
    name: "MenstruationAssistant",
    components: {
        IconSelect
    },
    data() {
        return {
            queryParams: {
                // 当前状态
                state: '',
                pageSize: 10,
                pageNum: 1
            },
            // 展现形式：true-日期，false-时间线
            showTable: true,
            pregnancy: [0, 0, ''],
            // 当前状态字典
            stateOptions: [],
            // 选择时间
            day: new Date(),
            // 姨妈记录
            menstruationRecord: [],
            // 姨妈记录时间线格式
            timelineRecord: [],
            // 时间线数据条数
            timelineTotal: 0,
            // 每天弹框标志
            dayDialogVisible: false,
            // 保存每天按钮loading
            saveDayLoading: false,
            // 删除每天按钮loading
            deleteDayLoading: false,
            // 每天表单
            dayForm: {
                date: '',
                come: undefined,
                go: undefined,
                mood: '',
                makeLove: false,
            },
            // 经期设置表单
            menstruationAssistantSetting: {
                cycle: undefined,
                duration: undefined
            },
            // 每天表单缓存
            dayFormCache: ''
        }
    },
    mounted() {
        this.getConfigKey("ymzq").then(response => {
            this.menstruationAssistantSetting.cycle = response.msg;
        });
        this.getConfigKey("ymsc").then(response => {
            this.menstruationAssistantSetting.duration = response.msg;
        });
        this.getConfigKey("ymdqzt").then(response => {
            this.queryParams.state = response.msg;
        });
        this.getDicts("px_life_menstruation").then(response => {
            this.stateOptions = response.data;
        });
        this.getLastStartDate();
    },
    watch: {
        day: {
            handler(newDay, oldDay) {
                if (this.parseTime(newDay, '{y}-{m}') !== this.parseTime(oldDay, '{y}-{m}')) {
                    this.getMenstruationRecord();
                }
            },
            immediate: true
        },
        showTable(newValue) {
            if (newValue) {
                this.menstruationRecord = [];
                this.getMenstruationRecord();
            } else {
                this.timelineRecord = [];
                this.getPxMenstruationRecordList()
            }
        }
    },
    methods: {
        /**
         * 背景颜色
         */
        background(data) {
            if (this.menstruationRecord.length > 0) {
                const list = this.menstruationRecord.filter(item => {
                    return item.type === '0' || item.type === '1'
                })
                const recentlyStarted = list.find(item => this.parseTime(item.date) <= this.parseTime(data.day));
                if ((data.day <= this.parseTime(new Date()) && recentlyStarted && recentlyStarted.type === '0') || (this.menstruationRecord.find(item => this.parseTime(item.date) === this.parseTime(data.day)) && this.menstruationRecord.find(item => this.parseTime(item.date) === this.parseTime(data.day)).type === '1')) {
                    return 'actual'
                }
                if (data.day > this.parseTime(new Date())) {
                    const startData = this.menstruationRecord.filter(item => {
                        return item.type === '0'
                    })
                    if (startData) return ''
                    if (0 <= this.$getDaysBetween(this.parseTime(startData[0].date, '{y}-{m}-{d}'), data.day) % this.menstruationAssistantSetting?.cycle && this.$getDaysBetween(this.parseTime(startData[0].date, '{y}-{m}-{d}'), data.day) % this.menstruationAssistantSetting?.cycle < this.menstruationAssistantSetting?.duration) {
                        return 'estimate'
                    }
                }
            }
            return ''
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
         * 开始标志
         */
        start(data) {
            let estimate = false;
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
            let estimate = false;
            let actual = this.menstruationRecord.find(item => this.parseTime(item.date) === this.parseTime(data.day)) && this.menstruationRecord.find(item => this.parseTime(item.date) === this.parseTime(data.day)).type === '1';
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
         * 计算大姨妈走
         */
        go() {
            return true
        },
        /**
         * 计算大姨妈来
         */
        come() {
            if (this.dayDialogVisible) {
                const startData = this.menstruationRecord.filter(item => {
                    return item.type === '0'
                })
                if (!startData || startData.length < 1) {
                    return true
                }
                return !(this.dayForm.date > this.parseTime(startData[0].date, '{y}-{m}-{d}') && this.dayForm.date < this.$dateChange(10, this.parseTime(startData[0].date, '{y}-{m}-{d}')));
            }
            return true
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
         * 保存每天的内容
         */
        handleSaveDay(done) {
            // 如果没有改变，则直接关闭
            if (this.dayFormCache === JSON.stringify(this.dayForm)) {
                done();
                return;
            }
            ;
            this.$refs["dayForm"].validate(valid => {
                if (valid) {
                    if (this.dayForm.come) {
                        this.dayForm.type = '0';
                    } else if (this.dayForm.go) {
                        this.dayForm.type = '1';
                    } else {
                        this.dayForm.type = null;
                    }
                    this.dayForm.state = this.queryParams.state;
                    this.dayForm.date = this.parseTime(this.dayForm.date);
                    this.saveDayLoading = true;
                    if (this.dayForm.id !== undefined) {
                        updateMenstruationRecord(this.dayForm).then(res => {
                            this.saveDayLoading = false;
                            this.$notify.success('修改经期记录成功');
                            this.dayDialogVisible = false;
                            this.menstruationRecord = [];
                            this.getMenstruationRecord();
                        })
                    } else {
                        addMenstruationRecord(this.dayForm).then(res => {
                            this.saveDayLoading = false;
                            this.$notify.success('新增经期记录成功');
                            this.dayDialogVisible = false;
                            this.menstruationRecord = [];
                            this.getMenstruationRecord();
                        })
                    }
                }
            })
        },
        /**
         * 删除记录
         */
        handleDelete() {
            this.$confirm('确认删除该记录？', '删除提示').then(() => {
                this.deleteDayLoading = true;
                delMenstruationRecord(this.dayForm.id).then(res => {
                    this.deleteDayLoading = false;
                    this.$notify.success('删除成功');
                    this.dayDialogVisible = false;
                    this.menstruationRecord = [];
                    this.getMenstruationRecord();
                })
            }).catch(() => {
            });
        },
        /**
         * 打开弹框
         */
        handleOpen(data, item) {
            if (item) {
                this.dayForm = item;
            } else if (this.menstruationRecord.find(item => this.parseTime(item.date) === this.parseTime(data.day))) {
                this.dayForm = {
                    ...this.dayForm,
                    ...JSON.parse(JSON.stringify(this.menstruationRecord.find(item => this.parseTime(item.date) === this.parseTime(data.day))))
                }
                if (this.dayForm.type === '0') {
                    this.dayForm.come = true;
                }
                if (this.dayForm.type === '1') {
                    this.dayForm.go = true;
                }
            } else {
                this.dayForm = {
                    date: data.day,
                    come: undefined,
                    go: undefined,
                    mood: '',
                    makeLove: false,
                }
            }
            // 缓存
            this.dayFormCache = JSON.stringify(this.dayForm);
            this.dayDialogVisible = true;
        },
        /**
         * 选择图标
         */
        selected(name) {
            this.dayForm.mood = name;
        },
        /**
         * 获取姨妈记录
         */
        getMenstruationRecord() {
            listMenstruationRecord({date: this.parseTime(this.day)}).then(res => {
                this.menstruationRecord = this.menstruationRecord.concat(res.rows.map(item => {
                    return {
                        ...item,
                        date: this.parseTime(item.date, '{y}-{m}-{d}'),
                    }
                }));

            })
        },
        /**
         * 获取时间线数据
         */
        getPxMenstruationRecordList() {
            getPxMenstruationRecordList(this.queryParams).then(res => {
                this.timelineRecord = this.timelineRecord.concat(res.rows.map(item => {
                    return {
                        ...item,
                        date: this.parseTime(item.date, '{y}-{m}-{d}'),
                    }
                }));
            })
        },
        /**
         * 加载更多
         */
        loadMore() {
            if (this.timelineRecord.length < this.timelineTotal) {
                this.queryParams.pageNum++;
                this.getPxMenstruationRecordList();
            }
        },
        /**
         * 获取最后一次开始信息
         */
        getLastStartDate() {
            getLastStartDate().then(res => {
                let timeDifference = this.getTimeDifference(res.data.date);
                let day = timeDifference.slice(0, timeDifference.indexOf('天'));
                this.pregnancy = [Math.floor(day / 7), day % 7, timeDifference];
            })
        }
    }
}
</script>

<style lang='scss' scoped>
.el-timeline {
    margin: 7rem var(--space-4) var(--space-4) 0;
    height: 100vh;
    overflow-y: auto;

    .menstruation-assistant-info {
        display: flex;
        justify-content: space-between;
        padding-bottom: var(--space-2);
        border-bottom: 2px solid var(--border-primary);

        .left {
            display: flex;
            align-items: center;
        }

        .weather {
            margin-left: var(--space-4);
        }
    }
}

.menstruation-assistant {
    margin: var(--space-4);
    padding: var(--space-4);

    .el-row {
        align-items: center;
        padding: var(--space-4);
        position: fixed;
        width: 86vw;
        z-index: 999;
        background-color: var(--bg-card);
        border: 1px solid var(--border-primary);
        border-radius: var(--radius-md);
        box-shadow: var(--shadow-sm);
        transition: box-shadow var(--duration-normal) var(--ease-default);

        .state {
            border: 1px solid var(--color-primary);
            cursor: pointer;
            color: var(--color-primary);
            padding: var(--space-2);
            border-radius: var(--radius-sm);
            font-weight: var(--font-bold);
            font-family: 幼圆;
            width: fit-content;
            transition: all var(--duration-normal) var(--ease-default);

            &:hover {
                background: var(--bg-hover);
            }
        }

        .tip {
            font-size: var(--text-xl);
            font-weight: var(--font-bold);
            font-family: 幼圆;
            color: var(--text-primary);
        }
    }

    .one-day {
        display: flex;
        flex-flow: column;
        align-items: center;
        justify-content: center;
        height: 100%;
        transition: background-color var(--duration-normal) var(--ease-default);

        .date {
            margin-bottom: var(--space-2);
            font-size: var(--text-lg);
            font-weight: var(--font-bold);
            height: 2rem;
            width: 4rem;
        }

        .icon {
            height: 2rem;
            font-size: var(--text-xl);
            width: 4rem;
        }

        .top, .bottom {
            display: flex;
            flex-wrap: wrap;

            .one-box:nth-child(2) {
                margin-left: var(--space-4);
            }
        }

        .one-box {
            display: flex;
            align-items: center;
            justify-content: center;
            margin-right: 0.2rem;
        }
    }

    .actual {
        background-color: #FD669799;
        border-radius: var(--radius-sm);
    }

    .estimate {
        background-color: #FD669722;
        border-radius: var(--radius-sm);
    }
}

.el-calendar {
    margin-top: 5rem;
}

::v-deep .el-calendar-table .el-calendar-day {
    padding: 0 !important;
}

.day-form {
    padding: var(--space-4);
}

.del-btn {
    float: right;
}
</style>
