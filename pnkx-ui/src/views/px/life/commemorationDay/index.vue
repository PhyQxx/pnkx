<!--
 * @File: index
 * @Author: PHY
 * @Date: 2021-11-28 10:53
 * @Description: 纪念日 - Modern UI/UX Refactored
-->
<template>
    <div class="commemoration-container">
        <!-- 左侧列表面板 -->
        <aside class="sidebar">
            <!-- 搜索栏 -->
            <div class="search-wrapper">
                <div class="search-box">
                    <svg-icon icon-class="搜索" class="search-icon"/>
                    <input
                        v-model="searchCode"
                        placeholder="搜索纪念日..."
                        class="search-input"
                    >
                </div>
            </div>

            <!-- 纪念日列表 -->
            <div
                v-loading="listLoading"
                class="day-list"
                @contextmenu.prevent.stop="handleContextMenu($event, null)"
            >
                <div v-if="filteredList.length < 1" class="empty-state">
                    <svg-icon icon-class="纪念日" class="empty-icon"/>
                    <p>暂无纪念日</p>
                    <p class="hint">右键或点击右下角按钮新增</p>
                </div>

                <transition-group v-else name="day-list" tag="div" class="day-items">
                    <div
                        v-for="(item, index) in filteredList"
                        :key="item.id"
                        class="day-card"
                        :class="{ active: active && active.id === item.id }"
                        :style="{ animationDelay: `${index * 0.05}s` }"
                        @contextmenu.prevent.stop="handleContextMenu($event, item)"
                        @click="handleSelectDay(item)"
                    >
                        <div class="card-icon-wrapper">
                            <svg-icon :icon-class="item.icon || '纪念日'" class="card-icon"/>
                        </div>
                        <div class="card-info">
                            <div class="card-name">{{ item.name }}</div>
                            <div class="card-date">
                                {{
                                    item.repeat ? `每年${parseTime(item.date, '{m}月{d}日')}` : parseTime(item.date, '{y}年{m}月{d}日')
                                }}
                            </div>
                        </div>
                        <div class="card-countdown">
                            <span class="countdown-label">{{ getRepeat(item) }}</span>
                            <span class="countdown-value">{{ getCountdownShort(item) }}</span>
                        </div>
                    </div>
                </transition-group>
            </div>
        </aside>

        <!-- 右侧详情面板 -->
        <main v-loading="loading" class="detail-area">
            <!-- 空状态 -->
            <div v-if="!active" class="empty-detail">
                <svg-icon icon-class="纪念日" class="empty-detail-icon"/>
                <p>选择一个纪念日查看详情</p>
            </div>

            <!-- 纪念日详情 -->
            <div v-else class="day-detail">
                <!-- 头部信息 -->
                <div class="detail-header">
                    <div class="detail-icon-wrapper">
                        <svg-icon :icon-class="active.icon || '纪念日'" class="detail-icon"/>
                    </div>
                    <div class="detail-title-section">
                        <h2 class="detail-name">{{ active.name }}</h2>
                        <p class="detail-date">
                            {{
                                active.repeat ? `每年${parseTime(active.date, '{m}月{d}日')}` : parseTime(active.date, '{y}年{m}月{d}日')
                            }}
                        </p>
                    </div>
                    <div class="detail-actions">
                        <el-button type="primary" size="small" @click="handleEdit">
                            <svg-icon icon-class="编辑" class="action-icon"/>
                            编辑
                        </el-button>
                        <el-button type="warning" size="small" plain @click="handleSetReminder">
                            <svg-icon icon-class="通知" class="action-icon"/>
                            提醒
                        </el-button>
                        <el-button type="danger" size="small" @click="handleDeleteFromDetail">
                            <svg-icon icon-class="删除" class="action-icon"/>
                            删除
                        </el-button>
                    </div>
                </div>

                <!-- 倒计时展示 -->
                <div class="countdown-section">
                    <div class="countdown-label-large">{{ getRepeat(active) }}</div>
                    <div class="countdown-tiles">
                        <div class="countdown-tile">
                            <span class="tile-value">{{ getCountdownUnits(active).days }}</span>
                            <span class="tile-unit">天</span>
                        </div>
                        <div class="countdown-separator">:</div>
                        <div class="countdown-tile">
                            <span class="tile-value">{{ getCountdownUnits(active).hours }}</span>
                            <span class="tile-unit">时</span>
                        </div>
                        <div class="countdown-separator">:</div>
                        <div class="countdown-tile">
                            <span class="tile-value">{{ getCountdownUnits(active).minutes }}</span>
                            <span class="tile-unit">分</span>
                        </div>
                        <div class="countdown-separator">:</div>
                        <div class="countdown-tile">
                            <span class="tile-value">{{ getCountdownUnits(active).seconds }}</span>
                            <span class="tile-unit">秒</span>
                        </div>
                    </div>
                </div>

                <!-- 备注 -->
                <div v-if="active.remark" class="detail-remark">
                    <h4>备注</h4>
                    <p>{{ active.remark }}</p>
                </div>
                <div class="detail-remark">
                    <h4>相关支出</h4>
                    <p v-if="relatedExpenses.length === 0">暂无关联支出</p>
                    <p v-for="expense in relatedExpenses" :key="expense.id">
                        {{ parseTime(expense.payTime, '{y}-{m}-{d}') }} · {{ expense.typeObject && expense.typeObject.typeName }} · ¥{{ expense.money }}
                    </p>
                </div>
            </div>
        </main>

        <!-- 浮动新增按钮 -->
        <div class="fab-add" title="新增纪念日" @click="handleAdd">
            <el-icon>
                <Plus/>
            </el-icon>
        </div>

        <!-- 新增/编辑对话框 -->
        <el-dialog
            :title="title"
            v-model="dialogVisible"
            width="520px"
            custom-class="modern-dialog"
            :modal-append-to-body="true"
        >
            <el-form
                ref="form"
                :model="form"
                :rules="rules"
                label-position="top"
                class="modern-form"
            >
                <el-form-item label="纪念日名称" prop="name">
                    <el-input v-model="form.name" placeholder="请输入纪念日名称"/>
                </el-form-item>
                <el-form-item label="纪念日时间" prop="date">
                    <el-date-picker
                        v-model="form.date"
                        type="datetime"
                        value-format="YYYY-MM-DD HH:mm:ss"
                        placeholder="选择纪念日时间"
                        style="width: 100%"
                    />
                </el-form-item>
                <el-form-item label="重复提醒">
                    <el-select v-model="form.repeat" placeholder="选择是否重复提醒" style="width: 100%">
                        <el-option label="每年重复" :value="true"/>
                        <el-option label="仅一次" :value="false"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="图标" prop="icon">
                    <el-popover
                        placement="bottom-start"
                        width="460"
                        trigger="click"
                        @show="$refs['iconSelect'].reset()"
                    >
                        <icon-select ref="iconSelect" @selected="selected"/>
                        <template #reference>
                            <el-input
                                v-model="form.icon"
                                placeholder="点击选择图标"
                                readonly
                            >
                                <template #prefix>
                                    <svg-icon
                                        v-if="form.icon"
                                        :icon-class="form.icon"
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
                <el-form-item label="备注">
                    <el-input
                        v-model="form.remark"
                        type="textarea"
                        placeholder="请输入备注内容"
                        :rows="3"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button class="btn-cancel" @click="dialogVisible = false">
                        取消
                    </el-button>
                    <el-button
                        :loading="saveLoading"
                        type="primary"
                        class="btn-confirm"
                        @click="handleSave"
                    >
                        确定
                    </el-button>
                </div>
            </template>
        </el-dialog>

        <!-- 右键菜单 -->
        <transition name="context-menu">
            <div
                v-if="contextMenuVisible"
                v-clickOutSide="closeContextMenu"
                class="context-menu"
                :style="contextMenuStyle"
            >
                <div
                    v-for="item in contextMenuItems"
                    :key="item.id"
                    class="menu-item"
                    @click="handleContextAction(item)"
                >
                    <svg-icon :icon-class="item.icon" class="menu-icon"/>
                    <span>{{ item.name }}</span>
                </div>
            </div>
        </transition>

        <!-- 提醒设置弹窗 -->
        <reminder-setting
            v-if="active"
            ref="reminderSetting"
            source-type="commemoration"
            :source-id="active.id"
            :source-name="active.name"
            :event-time="active.date"
            :bound="reminderBound"
            @saved="handleReminderSaved"
            @unbind="handleReminderUnbind"
        />
    </div>
</template>

<script>
import {addDay, delDay, getDay, listDay, updateDay} from '@/api/px/life/commemorationDay'
import IconSelect from '@/components/IconSelect/index.vue'
import ReminderSetting from '@/components/ReminderSetting'
import {listReminder} from '@/api/px/life/reminder'
import {listRecord} from '@/api/px/life/bookkeeping/record'

export default {
    name: 'CommemorationDay',
    components: {IconSelect, ReminderSetting},
    data() {
        return {
            // 列表加载标志
            listLoading: false,
            // 详情加载标志
            loading: false,
            // 搜索关键字
            searchCode: '',
            // 纪念日列表
            list: [],
            // 当前选中
            active: null,
            relatedExpenses: [],
            // 当前时间
            nowTime: this.parseTime(new Date()),
            // 计时器
            commemorationDayInterval: null,
            // 弹框标志
            dialogVisible: false,
            // 标题
            title: '',
            // 保存loading
            saveLoading: false,
            // 表单
            form: {
                name: '',
                date: this.parseTime(new Date()),
                repeat: false,
                icon: '纪念日',
                remark: ''
            },
            // 表单校验
            rules: {
                name: [
                    {required: true, message: '请输入纪念日名称', trigger: 'blur'}
                ],
                date: [
                    {required: true, message: '请选择纪念日时间', trigger: 'change'}
                ],
                icon: [
                    {required: true, message: '请选择纪念日图标', trigger: 'change'}
                ]
            },
            // 提醒设置
            reminderVisible: false,
            reminderBound: null,
            // 右键菜单
            contextMenuVisible: false,
            contextMenuStyle: '',
            contextMenuItems: [],
            contextMenuTarget: null
        }
    },
    computed: {
        filteredList() {
            if (!this.searchCode) return this.list
            const keyword = this.searchCode.toLowerCase()
            return this.list.filter(item =>
                item.name && item.name.toLowerCase().includes(keyword)
            )
        }
    },
    mounted() {
        this.nowTime = this.parseTime(new Date())
        this.getCommemorationDayList(this.$route.params.id)
        this.commemorationDayInterval = setInterval(() => {
            this.nowTime = this.parseTime(new Date())
        }, 1000)
    },
    unmounted() {
        clearInterval(this.commemorationDayInterval)
    },
    methods: {
        /**
         * 获取纪念日列表
         */
        getCommemorationDayList(selectId) {
            this.listLoading = true
            listDay().then(res => {
                this.list = res.rows
                this.listLoading = false
                if (selectId) {
                    const found = this.list.find(item => String(item.id) === String(selectId))
                    if (found) {
                        this.active = found
                        this.loadRelatedExpenses(found.id)
                    }
                } else if (this.list.length > 0) {
                    this.active = this.list[0]
                    this.loadRelatedExpenses(this.active.id)
                }
            })
        },
        /**
         * 选中纪念日
         */
        handleSelectDay(item) {
            this.active = item
            this.loadRelatedExpenses(item.id)
        },
        loadRelatedExpenses(commemorationDayId) {
            listRecord({ pageNum: 1, pageSize: 20, commemorationDayId }).then(res => {
                this.relatedExpenses = res.rows || []
            }).catch(() => {
                this.relatedExpenses = []
            })
        },
        /**
         * 编辑（从详情面板或右键菜单）
         */
        handleEdit() {
            if (!this.active) return
            this.loading = true
            getDay(this.active.id).then(res => {
                this.form = res.data
                this.title = '编辑纪念日'
                this.dialogVisible = true
                this.loading = false
            })
        },
        /**
         * 删除（从详情面板或右键菜单）
         */
        handleDeleteFromDetail() {
            if (!this.active) return
            this.$confirm(`确认删除《${this.active.name}》纪念日?`, '删除', {
                type: 'warning'
            }).then(() => {
                return delDay(this.active.id)
            }).then(() => {
                this.$notify.success('删除成功')
                this.active = null
                this.getCommemorationDayList()
            }).catch(() => {
            })
        },
        /**
         * 设置提醒：先查询是否已绑定，再打开弹窗
         */
        handleSetReminder() {
            if (!this.active) return
            // 查询当前实体是否已绑定提醒（回显）
            listReminder({sourceType: 'commemoration', sourceId: this.active.id}).then(res => {
                const rows = res.rows || []
                this.reminderBound = rows.length > 0 ? rows[0] : null
            }).catch(() => {
                this.reminderBound = null
            }).finally(() => {
                this.$nextTick(() => {
                    this.$refs.reminderSetting && this.$refs.reminderSetting.open()
                })
            })
        },
        handleReminderSaved() {
            this.$notify.success('提醒已生效')
        },
        handleReminderUnbind() {
            this.reminderBound = null
        },
        /**
         * 新增纪念日
         */
        handleAdd() {
            this.title = '新增纪念日'
            this.dialogVisible = true
            this.form = {
                name: '',
                date: this.parseTime(new Date()),
                repeat: false,
                icon: '纪念日',
                remark: ''
            }
        },
        /**
         * 保存表单
         */
        handleSave() {
            this.saveLoading = true
            this.$refs['form'].validate(valid => {
                if (valid) {
                    if (this.form.id !== undefined) {
                        updateDay(this.form).then(() => {
                            this.saveLoading = false
                            this.$notify.success('修改纪念日成功')
                            this.dialogVisible = false
                            this.getCommemorationDayList()
                        })
                    } else {
                        addDay(this.form).then(() => {
                            this.saveLoading = false
                            this.$notify.success('新增纪念日成功')
                            this.dialogVisible = false
                            this.getCommemorationDayList()
                        })
                    }
                } else {
                    this.saveLoading = false
                }
            })
        },
        /**
         * 选择图标
         */
        selected(name) {
            this.form.icon = name
        },
        /**
         * 获取提示语句
         * @param item
         */
        getRepeat(item) {
            if (item.repeat) {
                return '还有'
            }
            if (this.parseTime(item.date) > this.parseTime(new Date())) {
                return '还有'
            }
            return '已经'
        },
        /**
         * 时间格式（保留原逻辑）
         * @param item
         */
        dateFormat(item) {
            if (this.$moment(this.nowTime).isBefore(item.date)) {
                return this.getTimeDifference(this.nowTime, item.date)
            } else {
                if (this.$moment(this.nowTime).isBefore(new Date().getFullYear() + item.date.slice(4))) {
                    return this.getTimeDifference(this.nowTime, new Date().getFullYear() + item.date.slice(4))
                }
                return this.getTimeDifference(this.nowTime, (Number(new Date().getFullYear()) + 1) + item.date.slice(4))
            }
        },
        /**
         * 获取倒计时单位分解
         * @param item
         */
        getCountdownUnits(item) {
            let targetDate
            if (this.$moment(this.nowTime).isBefore(item.date)) {
                targetDate = item.date
            } else {
                const yearDate = new Date().getFullYear() + item.date.slice(4)
                if (this.$moment(this.nowTime).isBefore(yearDate)) {
                    targetDate = yearDate
                } else {
                    targetDate = (Number(new Date().getFullYear()) + 1) + item.date.slice(4)
                }
            }

            const dateBegin = new Date(this.nowTime.replace(/-/g, '/'))
            const dateEnd = new Date(targetDate.replace(/-/g, '/'))
            const dateDiff = Math.abs(dateEnd.getTime() - dateBegin.getTime())

            const days = Math.floor(dateDiff / (24 * 3600 * 1000))
            const leave1 = dateDiff % (24 * 3600 * 1000)
            const hours = Math.floor(leave1 / (3600 * 1000))
            const leave2 = leave1 % (3600 * 1000)
            const minutes = Math.floor(leave2 / (60 * 1000))
            const leave3 = leave2 % (60 * 1000)
            const seconds = Math.round(leave3 / 1000)

            return {days, hours, minutes, seconds}
        },
        /**
         * 获取简短倒计时（侧边栏用）
         * @param item
         */
        getCountdownShort(item) {
            const units = this.getCountdownUnits(item)
            if (units.days > 0) return units.days + '天'
            if (units.hours > 0) return units.hours + '小时'
            return units.minutes + '分钟'
        },
        /**
         * 右键菜单
         */
        handleContextMenu(event, item) {
            if (item) {
                this.active = item
                this.contextMenuTarget = item
                this.contextMenuItems = [
                    {id: 1, name: '编辑', icon: '编辑'},
                    {id: 2, name: '删除', icon: '删除'}
                ]
            } else {
                this.contextMenuItems = [
                    {id: 3, name: '新增纪念日', icon: '编辑02'}
                ]
            }
            this.contextMenuVisible = true
            this.contextMenuStyle = `top: ${Math.min(event.y, window.innerHeight - this.contextMenuItems.length * 48)}px; left: ${Math.min(event.x - 180, window.innerWidth - 200)}px;`
        },
        /**
         * 关闭右键菜单
         */
        closeContextMenu() {
            this.contextMenuVisible = false
        },
        /**
         * 右键菜单操作
         */
        handleContextAction(item) {
            this.contextMenuVisible = false
            switch (item.id) {
                case 1:
                    this.handleEdit()
                    break
                case 2:
                    this.handleDeleteFromDetail()
                    break
                case 3:
                    this.handleAdd()
                    break
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.commemoration-container {
    display: flex;
    height: calc(100vh - 84px);
    background: var(--bg-body);
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

// 左侧边栏
.sidebar {
    width: 360px;
    background: var(--bg-card);
    backdrop-filter: blur(20px);
    border-right: 1px solid var(--border-primary);
    display: flex;
    flex-direction: column;
    box-shadow: var(--shadow-sm);
    position: relative;
    z-index: 10;
}

// 搜索栏
.search-wrapper {
    padding: var(--space-5);
    border-bottom: 1px solid var(--border-primary);

    .search-box {
        position: relative;
        display: flex;
        align-items: center;

        .search-icon {
            position: absolute;
            left: 14px;
            font-size: var(--text-base);
            color: var(--text-tertiary);
            pointer-events: none;
        }

        .search-input {
            width: 100%;
            height: 40px;
            padding: 0 var(--space-4) 0 42px;
            border: none;
            border-radius: var(--radius-md);
            background: var(--bg-body);
            font-size: var(--text-sm);
            color: var(--text-primary);
            box-shadow: var(--shadow-sm);
            transition: all var(--duration-normal) var(--ease-default);

            &::placeholder {
                color: var(--text-tertiary);
            }

            &:focus {
                outline: none;
                box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1), var(--shadow-md);
            }
        }
    }
}

// 纪念日列表
.day-list {
    flex: 1;
    overflow-y: auto;
    padding: var(--space-4);

    &::-webkit-scrollbar {
        width: 6px;
    }

    &::-webkit-scrollbar-track {
        background: transparent;
    }

    &::-webkit-scrollbar-thumb {
        background: var(--border-primary);
        border-radius: 3px;

        &:hover {
            background: var(--text-tertiary);
        }
    }

    .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        height: 100%;
        color: var(--text-tertiary);

        .empty-icon {
            font-size: 64px;
            opacity: 0.3;
            margin-bottom: var(--space-4);
        }

        p {
            margin: var(--space-1) 0;
        }

        .hint {
            font-size: var(--text-xs);
            opacity: 0.7;
        }
    }

    .day-items {
        display: flex;
        flex-direction: column;
        gap: var(--space-2);
    }
}

// 纪念日卡片
.day-card {
    display: flex;
    align-items: center;
    padding: 14px 16px;
    background: var(--bg-card);
    border-radius: var(--radius-md);
    cursor: pointer;
    transition: all var(--duration-normal) var(--ease-default);
    box-shadow: var(--shadow-sm);
    border-left: 3px solid transparent;
    animation: fadeSlideIn var(--duration-normal) var(--ease-default) forwards;
    opacity: 0;

    &.active {
        background: var(--bg-hover);
        border-left-color: var(--color-primary);
    }

    &:hover {
        transform: translateX(4px);
        box-shadow: var(--shadow-md);
        background: var(--bg-hover);
    }

    .card-icon-wrapper {
        width: 40px;
        height: 40px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
        border-radius: var(--radius-sm);
        margin-right: var(--space-3);
        flex-shrink: 0;

        .card-icon {
            font-size: var(--text-lg);
            color: white;
        }
    }

    .card-info {
        flex: 1;
        min-width: 0;

        .card-name {
            font-size: var(--text-sm);
            font-weight: 500;
            color: var(--text-primary);
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            margin-bottom: var(--space-1);
        }

        .card-date {
            font-size: var(--text-xs);
            color: var(--text-secondary);
        }
    }

    .card-countdown {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        flex-shrink: 0;
        margin-left: var(--space-3);

        .countdown-label {
            font-size: var(--text-xs);
            color: var(--text-tertiary);
            margin-bottom: 2px;
        }

        .countdown-value {
            font-size: var(--text-sm);
            font-weight: var(--font-semibold);
            color: var(--color-primary);
        }
    }
}

@keyframes fadeSlideIn {
    from {
        opacity: 0;
        transform: translateY(10px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

// 右侧详情区域
.detail-area {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.empty-detail {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: var(--text-tertiary);

    .empty-detail-icon {
        font-size: 80px;
        opacity: 0.2;
        margin-bottom: var(--space-5);
    }

    p {
        font-size: var(--text-base);
    }
}

// 纪念日详情
.day-detail {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow-y: auto;
}

.detail-header {
    display: flex;
    align-items: center;
    gap: var(--space-5);
    padding: 28px 32px 20px;
    background: var(--bg-card);
    border-bottom: 1px solid var(--border-primary);

    .detail-icon-wrapper {
        width: 56px;
        height: 56px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
        border-radius: var(--radius-md);
        flex-shrink: 0;
        box-shadow: 0 4px 16px rgba(240, 147, 251, 0.3);

        .detail-icon {
            font-size: var(--text-xl);
            color: white;
        }
    }

    .detail-title-section {
        flex: 1;
        min-width: 0;

        .detail-name {
            font-size: var(--text-xl);
            font-weight: var(--font-semibold);
            color: var(--text-primary);
            margin: 0 0 var(--space-1) 0;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .detail-date {
            font-size: var(--text-sm);
            color: var(--text-secondary);
            margin: 0;
        }
    }

    .detail-actions {
        display: flex;
        gap: var(--space-2);
        flex-shrink: 0;

        .el-button {
            border-radius: var(--radius-sm);

            .action-icon {
                font-size: var(--text-sm);
                margin-right: var(--space-1);
            }
        }
    }
}

// 倒计时展示
.countdown-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 48px 32px;
    background: var(--bg-card);
    margin: var(--space-6) 32px;
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-md);

    .countdown-label-large {
        font-size: var(--text-lg);
        font-weight: 500;
        color: var(--text-secondary);
        margin-bottom: var(--space-6);
    }

    .countdown-tiles {
        display: flex;
        align-items: center;
        gap: var(--space-3);
    }

    .countdown-tile {
        display: flex;
        flex-direction: column;
        align-items: center;
        background: var(--bg-body);
        border-radius: var(--radius-md);
        padding: var(--space-4) var(--space-5);
        min-width: 80px;
        box-shadow: var(--shadow-sm);

        .tile-value {
            font-size: var(--text-2xl);
            font-weight: 700;
            color: var(--text-primary);
            line-height: 1;
            margin-bottom: var(--space-2);
            font-variant-numeric: tabular-nums;
        }

        .tile-unit {
            font-size: var(--text-sm);
            color: var(--text-secondary);
            font-weight: 500;
        }
    }

    .countdown-separator {
        font-size: var(--text-xl);
        font-weight: 700;
        color: var(--text-tertiary);
        line-height: 1;
        padding-bottom: 20px;
    }
}

// 备注
.detail-remark {
    margin: 0 32px var(--space-6);
    padding: var(--space-5) var(--space-6);
    background: var(--bg-card);
    border-radius: var(--radius-md);
    box-shadow: var(--shadow-sm);

    h4 {
        font-size: var(--text-sm);
        font-weight: 500;
        color: var(--text-secondary);
        margin: 0 0 var(--space-2) 0;
    }

    p {
        font-size: var(--text-sm);
        color: var(--text-primary);
        line-height: 1.6;
        margin: 0;
        white-space: pre-wrap;
    }
}

// 浮动新增按钮
.fab-add {
    position: fixed;
    right: 32px;
    bottom: 32px;
    width: 56px;
    height: 56px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--color-primary), var(--color-primary-600));
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);
    cursor: pointer;
    transition: all var(--duration-normal) var(--ease-default);
    z-index: 100;

    i {
        font-size: var(--text-xl);
    }

    &:hover {
        transform: scale(1.1) rotate(90deg);
        box-shadow: 0 6px 28px rgba(102, 126, 234, 0.5);
    }

    &:active {
        transform: scale(0.95);
    }
}

// 右键菜单
.context-menu {
    position: fixed;
    background: var(--bg-card);
    border-radius: var(--radius-md);
    box-shadow: var(--shadow-lg);
    padding: var(--space-2) 0;
    min-width: 180px;
    z-index: 9999;
    border: 1px solid var(--border-primary);

    .menu-item {
        display: flex;
        align-items: center;
        gap: var(--space-3);
        padding: var(--space-3) var(--space-4);
        font-size: var(--text-sm);
        color: var(--text-primary);
        cursor: pointer;
        transition: all var(--duration-normal) var(--ease-default);

        .menu-icon {
            font-size: var(--text-base);
            color: var(--text-secondary);
        }

        &:hover {
            background: var(--bg-hover);
            color: var(--color-primary);

            .menu-icon {
                color: var(--color-primary);
            }
        }
    }
}

// 右键菜单动画
.context-menu-enter-active,
.context-menu-leave-active {
    transition: all var(--duration-fast) var(--ease-default);
}

.context-menu-enter,
.context-menu-leave-to {
    opacity: 0;
    transform: scale(0.95) translateY(-8px);
}

// 列表动画
.day-list-enter-active,
.day-list-leave-active {
    transition: all var(--duration-normal) var(--ease-default);
}

.day-list-enter,
.day-list-leave-to {
    opacity: 0;
    transform: translateX(-20px);
}

// 对话框样式覆盖
::v-deep .modern-dialog {
    border-radius: var(--radius-lg) !important;
    overflow: hidden;
    box-shadow: var(--shadow-lg) !important;

    .el-dialog__header {
        padding: var(--space-5) var(--space-6) var(--space-4);
        border-bottom: 1px solid var(--border-primary);
        background: var(--bg-card);

        .el-dialog__title {
            font-size: var(--text-lg);
            font-weight: var(--font-semibold);
            color: var(--text-primary);
        }
    }

    .el-dialog__body {
        padding: var(--space-6);
    }

    .el-dialog__footer {
        padding: var(--space-4) var(--space-6) var(--space-5);
        border-top: 1px solid var(--border-primary);
        background: var(--bg-body);
    }
}

// 表单样式
.modern-form {
    ::v-deep .el-form-item {
        margin-bottom: var(--space-5);

        .el-form-item__label {
            font-size: var(--text-sm);
            font-weight: 500;
            color: var(--text-secondary);
            padding-bottom: var(--space-2);
        }

        .el-input__inner {
            border-radius: var(--radius-sm);
            border-color: var(--border-primary);
            transition: all var(--duration-normal) var(--ease-default);

            &:focus {
                border-color: var(--color-primary);
                box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
            }
        }
    }
}

// 对话框按钮
.dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: var(--space-3);

    .btn-cancel {
        border-radius: var(--radius-sm);
        padding: 10px 20px;
        transition: all var(--duration-normal) var(--ease-default);

        &:hover {
            background: var(--bg-hover);
        }
    }

    .btn-confirm {
        border-radius: var(--radius-sm);
        padding: 10px 24px;
        background: linear-gradient(135deg, var(--color-primary), var(--color-primary-600));
        border: none;
        transition: all var(--duration-normal) var(--ease-default);

        &:hover {
            opacity: 0.9;
            transform: translateY(-1px);
        }
    }
}

// Loading 美化
::v-deep .el-loading-mask {
    background-color: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(4px);
}

// 通知美化
::v-deep .el-notification {
    border-radius: var(--radius-md);
    box-shadow: var(--shadow-lg);
    border: none;
}
</style>
