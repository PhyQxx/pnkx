<template>
    <el-dialog
        v-model="visible"
        title="设置提醒"
        width="420px"
        append-to-body
        @open="handleOpen"
    >
        <div v-loading="loading" class="reminder-setting">
            <el-form label-width="90px" label-position="left">
                <el-form-item label="提醒对象">
                    <el-tag size="small">{{ sourceLabel }}</el-tag>
                    <span class="obj-name">{{ sourceName || '-' }}</span>
                </el-form-item>

                <el-form-item label="事件时间">
                    <span class="event-time">{{ eventTimeText }}</span>
                </el-form-item>

                <el-form-item label="提前提醒">
                    <el-select v-model="form.leadMinutes" placeholder="选择提前量" style="width: 100%">
                        <el-option :value="15" label="提前 15 分钟"/>
                        <el-option :value="60" label="提前 1 小时"/>
                        <el-option :value="1440" label="提前 1 天"/>
                        <el-option :value="4320" label="提前 3 天"/>
                        <el-option :value="10080" label="提前 1 周"/>
                        <el-option :value="20160" label="提前 2 周"/>
                        <el-option :value="0" label="准点提醒"/>
                        <el-option :value="-1" label="自定义"/>
                    </el-select>
                </el-form-item>

                <el-form-item v-if="form.leadMinutes === -1" label="自定义分钟">
                    <el-input-number v-model="customMinutes" :min="0" :step="5" style="width: 100%"/>
                </el-form-item>

                <el-form-item label="提醒时间">
                    <el-date-picker
                        v-model="computedRemindTime"
                        type="datetime"
                        placeholder="提醒时间"
                        format="YYYY-MM-DD HH:mm"
                        value-format="YYYY-MM-DD HH:mm:ss"
                        style="width: 100%"
                        :disabled="true"
                    />
                    <div class="tip">系统将在此时间通过站内通知 + 邮件提醒你</div>
                </el-form-item>

                <el-form-item label="启用">
                    <el-switch v-model="form.enabled"/>
                </el-form-item>
            </el-form>
        </div>

        <template #footer>
            <el-button @click="visible = false">取消</el-button>
            <el-button type="danger" plain :disabled="!hasBound" @click="handleUnbind">删除提醒</el-button>
            <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
        </template>
    </el-dialog>
</template>

<script>
import dayjs from 'dayjs'
import {bindReminder, unbindReminder} from '@/api/px/life/reminder'

export default {
    name: 'ReminderSetting',
    props: {
        // 来源类型 todo / commemoration / menstruation / subscription
        sourceType: {type: String, required: true},
        // 来源实体ID
        sourceId: {type: [Number, String], required: true},
        // 来源事件时间（到期时间），Date 或字符串
        eventTime: {type: [Date, String], default: null},
        // 展示名称
        sourceName: {type: String, default: ''},
        // 已绑定的提醒配置（外部传入，用于回显）
        bound: {type: Object, default: null}
    },
    emits: ['saved', 'unbind'],
    data() {
        return {
            visible: false,
            loading: false,
            saving: false,
            hasBound: false,
            customMinutes: 30,
            form: {
                leadMinutes: 60,
                enabled: true,
                remindTime: null
            }
        }
    },
    computed: {
        sourceLabel() {
            const map = {todo: '待办', commemoration: '纪念日', menstruation: '经期', subscription: '订阅'}
            return map[this.sourceType] || '事件'
        },
        eventTimeText() {
            if (!this.eventTime) return '未设置'
            return dayjs(this.eventTime).format('YYYY-MM-DD HH:mm')
        },
        computedRemindTime() {
            if (!this.eventTime) return null
            const minutes = this.form.leadMinutes === -1
                ? this.customMinutes
                : this.form.leadMinutes
            const t = dayjs(this.eventTime).subtract(minutes, 'minute')
            return t.format('YYYY-MM-DD HH:mm:ss')
        }
    },
    watch: {
        bound: {
            immediate: true,
            handler(val) {
                if (val) {
                    this.hasBound = true
                    this.form.leadMinutes = val.leadMinutes ?? 60
                    this.form.enabled = val.enabled !== false
                } else {
                    this.hasBound = false
                    this.form.leadMinutes = 60
                    this.form.enabled = true
                }
            }
        }
    },
    methods: {
        open() {
            this.visible = true
        },
        handleOpen() {
            // 初始化自定义分钟
            if (this.form.leadMinutes === -1) {
                this.customMinutes = this.bound?.leadMinutes || 30
            }
        },
        buildPayload() {
            const minutes = this.form.leadMinutes === -1
                ? this.customMinutes
                : this.form.leadMinutes
            return {
                sourceType: this.sourceType,
                sourceId: this.sourceId,
                userId: String(this.$store.getters.id),
                leadMinutes: minutes,
                enabled: this.form.enabled,
                remindTime: this.computedRemindTime
            }
        },
        handleSave() {
            if (!this.computedRemindTime) {
                this.$message.warning('无法计算提醒时间，请确认事件时间已设置')
                return
            }
            this.saving = true
            bindReminder(this.buildPayload()).then(() => {
                this.$message.success('提醒已保存')
                this.hasBound = true
                this.visible = false
                this.$emit('saved')
            }).finally(() => {
                this.saving = false
            })
        },
        handleUnbind() {
            this.$confirm('确认删除该提醒？', '提示', {type: 'warning'}).then(() => {
                unbindReminder(this.sourceType, this.sourceId).then(() => {
                    this.$message.success('提醒已删除')
                    this.hasBound = false
                    this.visible = false
                    this.$emit('unbind')
                })
            }).catch(() => {
            })
        }
    }
}
</script>

<style lang="scss" scoped>
.reminder-setting {
    .obj-name {
        margin-left: 8px;
        font-weight: 600;
    }

    .event-time {
        color: var(--pnkx-text-secondary);
    }

    .tip {
        font-size: 12px;
        color: var(--pnkx-text-placeholder);
        line-height: 1.5;
        margin-top: 4px;
    }
}
</style>
