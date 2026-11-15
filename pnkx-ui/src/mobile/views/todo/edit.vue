<template>
    <div class="todo-edit m-form">
        <!-- 分工与转派记录（编辑模式） -->
        <MAssignmentHistory
            v-if="isEditMode && todoForm.id"
            type="todo"
            :source-id="todoForm.id"
            :users="userList"
            @changed="assignmentChanged"
        />

        <van-cell-group inset class="edit-card">
            <div class="field-block">
                <div class="field-label">内容 <span class="required">*</span></div>
                <van-field
                    v-model="todoForm.content"
                    type="textarea"
                    rows="3"
                    autosize
                    maxlength="500"
                    placeholder="请输入待办事项内容..."
                    class="field-textarea"
                />
                <div class="field-count">{{ (todoForm.content || '').length }}/500</div>
            </div>
        </van-cell-group>

        <van-cell-group inset class="edit-card">
            <!-- 到期提醒 -->
            <van-cell
                v-if="isEditMode && todoForm.id && todoForm.planEndTime"
                is-link
                center
                @click="openReminder"
            >
                <template #title>
                    <div class="cell-title">到期提醒</div>
                    <div class="cell-hint">设置后会在通知中心和已启用渠道提醒</div>
                </template>
                <template #default>
                    <van-tag v-if="reminderBound" type="primary" plain>已开启</van-tag>
                    <span v-else class="cell-hint">未设置</span>
                </template>
            </van-cell>

            <!-- 执行者 -->
            <van-cell is-link center @click="openPerformerPicker">
                <template #title>
                    <div class="cell-title">执行者</div>
                </template>
                <template #default>
                    <span :class="performerNameDisplay ? 'cell-value' : 'cell-hint'">
                        {{ performerNameDisplay || '请选择执行者' }}
                    </span>
                </template>
            </van-cell>

            <!-- 计划开始时间 -->
            <van-cell is-link center title="计划开始时间" :value="todoForm.planStartTime || '请选择开始时间'" @click="openTimePicker('start')" />

            <!-- 计划结束时间 -->
            <van-cell is-link center title="计划结束时间" :value="todoForm.planEndTime || '请选择结束时间'" @click="openTimePicker('end')" />

            <!-- 完成状态 -->
            <van-cell center title="完成状态">
                <template #right-icon>
                    <van-switch :model-value="switchStatus" size="20" @update:model-value="onStatusChange" />
                </template>
            </van-cell>
        </van-cell-group>

        <!-- 标签 -->
        <van-cell-group inset class="edit-card">
            <div class="field-block">
                <div class="field-label">标签</div>
                <div v-if="labelList.length > 0" class="label-selector">
                    <span
                        v-for="label in labelList"
                        :key="label"
                        class="label-chip"
                        :class="{ active: selectedLabels.includes(label) }"
                        @click="toggleLabel(label)"
                    >{{ label }}</span>
                </div>
                <div v-else class="cell-hint">暂无标签可选</div>
            </div>
        </van-cell-group>

        <!-- 备注 -->
        <van-cell-group inset class="edit-card">
            <div class="field-block">
                <div class="field-label">备注</div>
                <van-field
                    v-model="todoForm.remark"
                    type="textarea"
                    rows="2"
                    autosize
                    maxlength="500"
                    placeholder="补充备注信息..."
                    class="field-textarea"
                />
            </div>
        </van-cell-group>

        <!-- 删除（编辑模式） -->
        <div v-if="isEditMode" class="danger-zone">
            <van-button block plain type="danger" icon="delete-o" @click="handleDelete">删除待办</van-button>
        </div>

        <!-- 底部保存 -->
        <div class="m-sticky-actions">
            <van-button block round type="primary" :loading="saving" @click="handleSave">保存</van-button>
        </div>

        <!-- 执行者选择 -->
        <van-popup v-model:show="performerShow" position="bottom" round safe-area-inset-bottom>
            <div class="popup-container">
                <div class="popup-header">
                    <span class="popup-title">选择执行者</span>
                    <van-icon name="cross" size="18" @click="performerShow = false" />
                </div>
                <div class="popup-content">
                    <div
                        v-for="user in userList"
                        :key="user.userId"
                        class="user-item"
                        :class="{ selected: selectedUserIds.includes(user.userId) }"
                        @click="choiceUser(user)"
                    >
                        <div class="user-info">
                            <van-icon name="manager-o" size="16" color="var(--pnkx-primary)" />
                            <span class="user-name">{{ user.nickName }}</span>
                        </div>
                        <van-icon
                            :name="selectedUserIds.includes(user.userId) ? 'checked' : 'circle'"
                            :color="selectedUserIds.includes(user.userId) ? 'var(--pnkx-primary)' : 'var(--pnkx-border)'"
                            size="20"
                        />
                    </div>
                    <div v-if="userList.length === 0" class="m-empty">暂无成员</div>
                </div>
                <div class="popup-footer">
                    <van-button block round type="primary" @click="confirmPerformer">确定</van-button>
                </div>
            </div>
        </van-popup>

        <!-- 时间选择 -->
        <van-popup v-model:show="timePickerShow" position="bottom" round>
            <div class="time-picker">
                <van-date-picker
                    v-model="dateValues"
                    title="选择日期"
                    :min-date="minPickDate"
                    :max-date="maxPickDate"
                    :columns-type="['year', 'month', 'day']"
                />
                <van-time-picker v-model="timeValues" title="选择时间" :columns-type="['hour', 'minute']" />
                <div class="time-picker__actions">
                    <van-button block plain @click="timePickerShow = false">取消</van-button>
                    <van-button block type="primary" @click="confirmTime">确定</van-button>
                </div>
            </div>
        </van-popup>

        <!-- 到期提醒 -->
        <MReminderSetting
            v-if="isEditMode && todoForm.id && todoForm.planEndTime"
            ref="reminderSetting"
            source-type="todo"
            :source-id="todoForm.id"
            :source-name="todoForm.content"
            :event-time="todoForm.planEndTime"
            @change="onReminderChange"
        />
    </div>
</template>

<script>
import { getDo, addDo, updateDo, delDo, getLabelList } from '@/api/px/life/todo'
import { listUser } from '@/api/system/user'
import { showToast, mSuccess, mError, mConfirm, mLoading, closeToast } from '@/mobile/utils/ui'
import MReminderSetting from '@/mobile/components/ReminderSetting/index.vue'
import MAssignmentHistory from '@/mobile/components/AssignmentHistory.vue'

export default {
    name: 'MTodoEdit',
    components: {
        MReminderSetting,
        MAssignmentHistory
    },
    data() {
        return {
            isEditMode: false,
            saving: false,
            fromNotification: false,
            reminderBound: false,
            todoForm: {
                id: null,
                content: '',
                performer: '',
                performerName: '',
                planStartTime: '',
                planEndTime: '',
                label: '',
                remark: '',
                status: false
            },
            labelList: [],
            selectedLabels: [],
            userList: [],
            selectedUserIds: [],
            performerShow: false,
            timePickerShow: false,
            timeTarget: 'start',
            dateValues: [],
            timeValues: []
        }
    },
    computed: {
        performerNameDisplay() {
            return this.todoForm.performerName || ''
        },
        switchStatus() {
            const status = this.todoForm.status
            return status === true || status === 1 || status === '1' || status === 'true' || status === '已完成'
        },
        minPickDate() {
            const year = new Date().getFullYear()
            return new Date(year - 5, 0, 1)
        },
        maxPickDate() {
            const year = new Date().getFullYear()
            return new Date(year + 5, 11, 31)
        }
    },
    mounted() {
        const query = this.$route.query
        this.fromNotification = query.fromNotification === '1'
        if (query.id) {
            this.isEditMode = true
            this.loadTodoDetail(query.id)
        } else {
            this.initNewTodo()
            if (query.date) {
                this.todoForm.planStartTime = query.date + ' 09:00:00'
                this.todoForm.planEndTime = query.date + ' 18:00:00'
            }
        }
        this.loadLabelList()
        this.loadUserList()
    },
    methods: {
        initNewTodo() {
            const now = new Date()
            this.todoForm.planStartTime = this.formatDateTime(now)
            this.todoForm.planEndTime = this.formatDateTime(new Date(now.getTime() + 24 * 60 * 60 * 1000))
        },
        formatDateTime(date) {
            if (!date) return ''
            const d = new Date(date)
            const year = d.getFullYear()
            const month = String(d.getMonth() + 1).padStart(2, '0')
            const day = String(d.getDate()).padStart(2, '0')
            const hours = String(d.getHours()).padStart(2, '0')
            const minutes = String(d.getMinutes()).padStart(2, '0')
            const seconds = String(d.getSeconds()).padStart(2, '0')
            return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
        },
        parseDateTime(value) {
            // 'YYYY-MM-DD HH:mm:ss' → { date: ['2025','01','01'], time: ['09','00'] }
            if (!value) return null
            const raw = String(value).replace('T', ' ')
            const match = raw.match(/^(\d{4})-(\d{2})-(\d{2})[ T]?(\d{2})?:?(\d{2})?/)
            if (!match) return null
            return {
                date: [match[1], match[2], match[3]],
                time: [match[4] || '09', match[5] || '00']
            }
        },
        async loadLabelList() {
            try {
                const response = await getLabelList()
                if (response.code === 200) {
                    this.labelList = response.data || []
                }
            } catch (error) {
                console.error('加载标签列表失败:', error)
            }
        },
        async loadUserList() {
            try {
                const response = await listUser({})
                if (response.code === 200) {
                    this.userList = response.rows || []
                }
            } catch (error) {
                console.error('加载用户列表失败:', error)
            }
        },
        async loadTodoDetail(id) {
            mLoading('加载中...')
            try {
                const response = await getDo(id)
                if (response.code === 200 && response.data) {
                    this.todoForm = { remark: '', status: false, ...response.data }
                    this.selectedLabels = this.parseLabels(this.todoForm.label)
                    if (this.todoForm.performer) {
                        const performerIds = this.todoForm.performer.split(',').filter(p => p)
                        this.selectedUserIds = performerIds.map(p => parseInt(p))
                    }
                }
            } catch (error) {
                console.error('加载待办详情失败:', error)
                if (this.fromNotification) {
                    mError('提醒已失效')
                    this.$router.replace('/m/todo')
                } else {
                    mError('加载失败')
                }
            } finally {
                closeToast()
            }
        },
        parseLabels(labelString) {
            return labelString ? labelString.split(',').filter(l => l.trim()) : []
        },

        // ---- 执行者 ----
        assignmentChanged(value) {
            this.selectedUserIds = String(value).split(',').filter(Boolean).map(Number)
            this.updatePerformData()
            this.loadTodoDetail(this.todoForm.id)
        },
        openPerformerPicker() {
            if (this.todoForm.id) {
                showToast('请通过分工与转派记录修改执行者')
                return
            }
            this.performerShow = true
        },
        choiceUser(user) {
            const index = this.selectedUserIds.indexOf(user.userId)
            if (index > -1) {
                this.selectedUserIds.splice(index, 1)
            } else {
                this.selectedUserIds.push(user.userId)
            }
        },
        confirmPerformer() {
            this.updatePerformData()
            this.performerShow = false
        },
        updatePerformData() {
            const userIds = this.selectedUserIds
            const users = this.userList.filter(user => userIds.includes(user.userId))
            this.todoForm.performer = userIds.join(',')
            this.todoForm.performerName = users.map(user => user.nickName).join(' | ')
        },

        // ---- 时间 ----
        openTimePicker(target) {
            this.timeTarget = target
            const current = this.parseDateTime(
                target === 'start' ? this.todoForm.planStartTime : this.todoForm.planEndTime
            )
            const now = new Date()
            this.dateValues = current
                ? current.date
                : [String(now.getFullYear()), String(now.getMonth() + 1).padStart(2, '0'), String(now.getDate()).padStart(2, '0')]
            this.timeValues = current ? current.time : [String(now.getHours()).padStart(2, '0'), String(now.getMinutes()).padStart(2, '0')]
            this.timePickerShow = true
        },
        confirmTime() {
            const value = `${this.dateValues.join('-')} ${this.timeValues.join(':')}:00`
            if (this.timeTarget === 'start') {
                this.todoForm.planStartTime = value
            } else {
                this.todoForm.planEndTime = value
            }
            this.timePickerShow = false
        },

        // ---- 标签 ----
        toggleLabel(label) {
            const index = this.selectedLabels.indexOf(label)
            if (index > -1) {
                this.selectedLabels.splice(index, 1)
            } else {
                this.selectedLabels.push(label)
            }
        },

        // ---- 完成状态 ----
        onStatusChange(value) {
            this.todoForm.status = value
            if (value) {
                this.todoForm.finishTime = new Date().toISOString()
            }
        },

        // ---- 提醒 ----
        openReminder() {
            if (this.$refs.reminderSetting) {
                this.$refs.reminderSetting.open()
            }
        },
        onReminderChange(bound) {
            this.reminderBound = bound
        },

        // ---- 保存 / 删除 ----
        validateForm() {
            if (!this.todoForm.content || !this.todoForm.content.trim()) {
                showToast('请输入待办内容')
                return false
            }
            if (this.todoForm.planStartTime && this.todoForm.planEndTime) {
                const start = new Date(String(this.todoForm.planStartTime).replace(/-/g, '/')).getTime()
                const end = new Date(String(this.todoForm.planEndTime).replace(/-/g, '/')).getTime()
                if (start > end) {
                    showToast('开始时间不能晚于结束时间')
                    return false
                }
            }
            return true
        },
        async handleSave() {
            if (this.saving || !this.validateForm()) return
            this.saving = true
            mLoading('保存中...')
            try {
                const data = {
                    ...this.todoForm,
                    label: this.selectedLabels.join(','),
                    performer: this.selectedUserIds.join(','),
                    performerName: this.todoForm.performerName,
                    status: this.switchStatus
                }
                const response = this.isEditMode ? await updateDo(data) : await addDo(data)
                if (response.code === 200) {
                    mSuccess('保存成功')
                    setTimeout(() => {
                        this.$router.back()
                    }, 800)
                }
            } catch (error) {
                console.error('保存待办失败:', error)
                mError('保存失败')
            } finally {
                this.saving = false
                closeToast()
            }
        },
        async handleDelete() {
            try {
                await mConfirm('确认删除', '确定要删除这个待办吗？')
            } catch (e) {
                return
            }
            mLoading('删除中...')
            try {
                const response = await delDo(this.todoForm.id)
                if (response.code === 200) {
                    mSuccess('删除成功')
                    setTimeout(() => {
                        this.$router.back()
                    }, 800)
                }
            } catch (error) {
                console.error('删除待办失败:', error)
                mError('删除失败')
            } finally {
                closeToast()
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.todo-edit {
    padding-top: 12px;
}

.edit-card {
    margin: 0 0 12px;
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 12px;
    overflow: hidden;
}

.field-block {
    padding: 13px 14px;
}

.field-label {
    font-size: 14px;
    font-weight: 500;
    color: var(--pnkx-text);
    margin-bottom: 8px;

    .required {
        color: var(--pnkx-danger);
        margin-left: 2px;
    }
}

.field-textarea {
    background: var(--pnkx-surface-muted);
    border-radius: 8px;
    padding: 4px 0;
}

.field-count {
    text-align: right;
    font-size: 12px;
    color: var(--pnkx-text-muted);
    margin-top: 4px;
}

.cell-title {
    font-size: 14px;
    font-weight: 500;
    color: var(--pnkx-text);
    line-height: 1.3;
}

.cell-hint {
    font-size: 12px;
    color: var(--pnkx-text-muted);
    margin-top: 3px;
    line-height: 1.3;
}

.cell-value {
    font-size: 13px;
    color: var(--pnkx-text);
}

.label-selector {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.label-chip {
    padding: 5px 14px;
    background: var(--pnkx-surface-muted);
    border: 1px solid var(--pnkx-border);
    border-radius: 999px;
    font-size: 13px;
    color: var(--pnkx-text-secondary);
    transition: all 0.25s;

    &.active {
        background: var(--pnkx-primary);
        border-color: var(--pnkx-primary);
        color: #fff;
        font-weight: 600;
    }
}

.danger-zone {
    margin-top: 20px;
}

.popup-container {
    background: var(--pnkx-surface);
    border-radius: 16px 16px 0 0;
    max-height: 70vh;
    display: flex;
    flex-direction: column;
}

.popup-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 16px;
    border-bottom: 1px solid var(--pnkx-border-soft);
}

.popup-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--pnkx-text);
}

.popup-content {
    flex: 1;
    overflow-y: auto;
    padding: 6px 0;
}

.user-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;

    &.selected {
        background: var(--pnkx-primary-soft);
    }
}

.user-info {
    display: flex;
    align-items: center;
    gap: 8px;

    .user-name {
        font-size: 14px;
        color: var(--pnkx-text);
    }
}

.popup-footer {
    padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
}

.time-picker {
    background: var(--pnkx-surface);
    border-radius: 16px 16px 0 0;
    padding-bottom: 10px;

    &__actions {
        display: flex;
        gap: 10px;
        padding: 4px 16px calc(10px + env(safe-area-inset-bottom));

        .van-button {
            flex: 1;
        }
    }
}
</style>
