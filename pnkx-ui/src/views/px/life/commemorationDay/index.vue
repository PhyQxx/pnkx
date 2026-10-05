<!--
 * @File: index
 * @Author: PHY
 * @Date: 2021-11-28 10:53
 * @Description: 纪念日 - Modern UI/UX Refactored
-->
<template>
    <div class="commemoration-container">
        <!-- 左侧列表面板 -->
        <day-sidebar
            v-model="searchCode"
            :list="filteredList"
            :list-loading="listLoading"
            :active-id="active && active.id"
            :now-time="nowTime"
            @select="handleSelectDay"
            @contextmenu="handleContextMenu"
        />

        <!-- 右侧详情面板 -->
        <day-detail-panel
            :active="active"
            :related-expenses="relatedExpenses"
            :loading="loading"
            :now-time="nowTime"
            @edit="handleEdit"
            @set-reminder="handleSetReminder"
            @delete="handleDeleteFromDetail"
        />

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
import DaySidebar from './components/DaySidebar.vue'
import DayDetailPanel from './components/DayDetailPanel.vue'

export default {
    name: 'CommemorationDay',
    components: {IconSelect, ReminderSetting, DaySidebar, DayDetailPanel},
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
