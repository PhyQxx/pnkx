<template>
    <van-popup
        v-model:show="visible"
        position="bottom"
        round
        safe-area-inset-bottom
    >
        <div class="m-reminder">
            <div class="m-reminder__header">
                <span class="m-reminder__title">提醒设置</span>
                <van-icon name="cross" size="18" @click="close" />
            </div>

            <div class="m-reminder__summary">
                <div class="m-reminder__name">{{ sourceName || '生活事项' }}</div>
                <div class="m-reminder__time">事件时间：{{ displayEventTime }}</div>
            </div>

            <van-cell title="提前提醒" is-link :value="leadLabel" @click="openLeadPicker" />
            <div class="m-reminder__row">
                <div class="m-reminder__row-title">提醒渠道</div>
                <div class="m-reminder__row-value">通知中心 + 已启用渠道</div>
            </div>
            <div v-if="bound" class="m-reminder__hint">当前已开启提醒，保存会更新提醒时间。</div>

            <div class="m-reminder__actions">
                <van-button v-if="bound" block plain type="danger" @click="remove">关闭提醒</van-button>
                <van-button block type="primary" :loading="saving" @click="save">保存提醒</van-button>
            </div>
        </div>

        <!-- 提前量选择 -->
        <van-popup v-model:show="leadPickerShow" position="bottom" round>
            <van-picker
                v-model="leadValues"
                :columns="leadColumns"
                title="提前提醒"
                @cancel="leadPickerShow = false"
                @confirm="onLeadConfirm"
            />
        </van-popup>
    </van-popup>
</template>

<script>
import { listReminder, bindReminder, unbindReminder } from '@/api/px/life/reminder'
import { showToast, mSuccess, mError } from '@/mobile/utils/ui'

export default {
    name: 'MReminderSetting',
    props: {
        sourceType: { type: String, required: true },
        sourceId: { type: [Number, String], required: true },
        sourceName: { type: String, default: '' },
        eventTime: { type: [String, Date], required: true },
        annual: { type: Boolean, default: false }
    },
    emits: ['change'],
    data() {
        return {
            visible: false,
            bound: false,
            saving: false,
            leadMinutes: 60,
            leadPickerShow: false,
            leadValues: [60],
            leadOptions: [
                { label: '事件发生时', value: 0 },
                { label: '提前 10 分钟', value: 10 },
                { label: '提前 30 分钟', value: 30 },
                { label: '提前 1 小时', value: 60 },
                { label: '提前 1 天', value: 1440 },
                { label: '提前 3 天', value: 4320 },
                { label: '提前 1 周', value: 10080 }
            ]
        }
    },
    computed: {
        leadColumns() {
            return this.leadOptions.map(item => ({ text: item.label, value: item.value }))
        },
        leadLabel() {
            const item = this.leadOptions.find(o => o.value === this.leadMinutes)
            return item ? item.label : '提前 1 小时'
        },
        leadIndex() {
            const index = this.leadOptions.findIndex(item => item.value === this.leadMinutes)
            return index < 0 ? 3 : index
        },
        displayEventTime() {
            return String(this.eventTime || '').replace('T', ' ').substring(0, 16)
        }
    },
    methods: {
        async open() {
            await this.load()
            this.visible = true
        },
        close() {
            this.visible = false
        },
        async load() {
            try {
                const response = await listReminder({
                    sourceType: this.sourceType,
                    sourceId: this.sourceId,
                    pageNum: 1,
                    pageSize: 1
                })
                const reminder = (response.rows || [])[0]
                this.bound = !!reminder
                if (reminder && reminder.leadMinutes !== undefined && reminder.leadMinutes !== null) {
                    this.leadMinutes = Number(reminder.leadMinutes)
                }
            } catch (e) {
                this.bound = false
            }
        },
        openLeadPicker() {
            this.leadValues = [this.leadOptions[this.leadIndex].value]
            this.leadPickerShow = true
        },
        onLeadConfirm({ selectedOptions }) {
            const option = selectedOptions && selectedOptions[0]
            if (option) this.leadMinutes = option.value
            this.leadPickerShow = false
        },
        normalizedEventDate() {
            const raw = String(this.eventTime || '').replace(/-/g, '/')
            const date = new Date(raw.length <= 10 ? raw + ' 09:00:00' : raw)
            if (this.annual && !Number.isNaN(date.getTime())) {
                const now = new Date()
                date.setFullYear(now.getFullYear())
                if (date.getTime() <= now.getTime()) date.setFullYear(now.getFullYear() + 1)
            }
            return date
        },
        formatDateTime(date) {
            const pad = value => String(value).padStart(2, '0')
            return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
        },
        async save() {
            const eventDate = this.normalizedEventDate()
            if (Number.isNaN(eventDate.getTime())) {
                showToast('事件时间无效')
                return
            }
            this.saving = true
            try {
                const remindDate = new Date(eventDate.getTime() - this.leadMinutes * 60 * 1000)
                await bindReminder({
                    sourceType: this.sourceType,
                    sourceId: Number(this.sourceId),
                    remindTime: this.formatDateTime(remindDate),
                    leadMinutes: this.leadMinutes,
                    enabled: true
                })
                this.bound = true
                this.close()
                this.$emit('change', true)
                mSuccess('提醒已保存')
            } catch (e) {
                mError('保存失败')
            } finally {
                this.saving = false
            }
        },
        async remove() {
            try {
                await unbindReminder(this.sourceType, this.sourceId)
                this.bound = false
                this.close()
                this.$emit('change', false)
                showToast('提醒已关闭')
            } catch (e) {
                mError('操作失败')
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.m-reminder {
    padding: 16px 16px calc(16px + env(safe-area-inset-bottom));
    background: var(--pnkx-surface);
    border-radius: 16px 16px 0 0;

    &__header {
        display: flex;
        align-items: center;
        justify-content: space-between;
    }

    &__title {
        font-size: 16px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__summary {
        display: flex;
        flex-direction: column;
        gap: 4px;
        margin: 14px 0;
        padding: 12px;
        border-radius: 10px;
        background: var(--pnkx-surface-muted);
    }

    &__name {
        color: var(--pnkx-text);
        font-size: 14px;
        font-weight: 600;
    }

    &__time,
    &__hint {
        color: var(--pnkx-text-muted);
        font-size: 12px;
    }

    &__row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 13px 0;
        border-bottom: 1px solid var(--pnkx-border-soft);
    }

    &__row-title {
        color: var(--pnkx-text);
        font-size: 14px;
    }

    &__row-value {
        color: var(--pnkx-text-secondary);
        font-size: 13px;
    }

    &__hint {
        margin-top: 10px;
    }

    &__actions {
        display: flex;
        gap: 10px;
        margin-top: 16px;

        .van-button {
            flex: 1;
        }
    }
}
</style>
