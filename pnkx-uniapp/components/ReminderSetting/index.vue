<template>
  <uni-popup ref="popup" type="bottom" :safe-area="true">
    <view class="reminder-setting">
      <view class="reminder-setting__header">
        <text class="reminder-setting__title">提醒设置</text>
        <uni-icons type="closeempty" size="22" color="#64748B" @click="close" />
      </view>
      <view class="reminder-setting__summary">
        <text class="reminder-setting__name">{{ sourceName || '生活事项' }}</text>
        <text class="reminder-setting__time">事件时间：{{ displayEventTime }}</text>
      </view>
      <picker :range="leadOptions" range-key="label" :value="leadIndex" @change="onLeadChange">
        <view class="reminder-setting__row">
          <text>提前提醒</text>
          <view class="reminder-setting__value">
            <text>{{ leadOptions[leadIndex].label }}</text>
            <uni-icons type="right" size="16" color="#94A3B8" />
          </view>
        </view>
      </picker>
      <view v-if="bound" class="reminder-setting__hint">当前已开启提醒，保存会更新提醒时间。</view>
      <view class="reminder-setting__actions">
        <button v-if="bound" class="reminder-setting__button reminder-setting__button--ghost" @click="remove">关闭提醒</button>
        <button class="reminder-setting__button reminder-setting__button--primary" :loading="saving" @click="save">保存提醒</button>
      </view>
    </view>
  </uni-popup>
</template>

<script>
import { listReminder, bindReminder, unbindReminder } from '@/api/px/life/reminder'

export default {
  name: 'ReminderSetting',
  props: {
    sourceType: { type: String, required: true },
    sourceId: { type: [Number, String], required: true },
    sourceName: { type: String, default: '' },
    eventTime: { type: [String, Date], required: true },
    annual: { type: Boolean, default: false }
  },
  data() {
    return {
      bound: false,
      saving: false,
      leadMinutes: 60,
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
      this.$refs.popup.open()
    },
    close() {
      this.$refs.popup.close()
    },
    async load() {
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
    },
    onLeadChange(e) {
      this.leadMinutes = this.leadOptions[Number(e.detail.value)].value
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
        uni.showToast({ title: '事件时间无效', icon: 'none' })
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
        uni.showToast({ title: '提醒已保存', icon: 'success' })
      } finally {
        this.saving = false
      }
    },
    async remove() {
      await unbindReminder(this.sourceType, this.sourceId)
      this.bound = false
      this.close()
      this.$emit('change', false)
      uni.showToast({ title: '提醒已关闭', icon: 'none' })
    }
  }
}
</script>

<style lang="scss" scoped>
.reminder-setting {
  padding: 32rpx $page-padding calc(32rpx + env(safe-area-inset-bottom));
  background: $bg-card;
  border-radius: $radius-2xl $radius-2xl 0 0;
}
.reminder-setting__header,
.reminder-setting__row,
.reminder-setting__value,
.reminder-setting__actions { display: flex; align-items: center; }
.reminder-setting__header { justify-content: space-between; }
.reminder-setting__title { font-size: $font-h2; font-weight: $font-weight-semibold; color: $text-primary; }
.reminder-setting__summary { display: flex; flex-direction: column; gap: 8rpx; margin: 28rpx 0; padding: 24rpx; border-radius: $radius-lg; background: $bg-page; }
.reminder-setting__name { color: $text-primary; font-size: $font-body; font-weight: $font-weight-semibold; }
.reminder-setting__time, .reminder-setting__hint { color: $text-tertiary; font-size: $font-caption; }
.reminder-setting__row { justify-content: space-between; padding: 28rpx 0; border-bottom: 1rpx solid $border-light; color: $text-primary; }
.reminder-setting__value { gap: 8rpx; color: $text-secondary; }
.reminder-setting__hint { margin-top: 18rpx; }
.reminder-setting__actions { gap: 20rpx; margin-top: 32rpx; }
.reminder-setting__button { flex: 1; margin: 0; border-radius: $radius-xl; font-size: $font-body; }
.reminder-setting__button--ghost { color: $danger; background: $danger-light; }
.reminder-setting__button--primary { color: #fff; background: $primary; }
</style>
