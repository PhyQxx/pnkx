<template>
  <view class="preference-page subpage-shell">
    <view class="card">
      <view class="row"><view><text class="title">站内实时提醒</text><text class="hint">应用在线时通过 WebSocket 接收</text></view><switch :checked="form.websocketEnabled" @change="form.websocketEnabled = $event.detail.value" color="#5B9EEE" /></view>
      <view class="row"><view><text class="title">App 推送</text><text class="hint">应用在后台时显示系统通知</text></view><switch :checked="form.pushEnabled" @change="form.pushEnabled = $event.detail.value" color="#5B9EEE" /></view>
      <view class="row"><view><text class="title">邮件提醒</text><text class="hint">发送到账号绑定邮箱</text></view><switch :checked="form.emailEnabled" @change="form.emailEnabled = $event.detail.value" color="#5B9EEE" /></view>
    </view>
    <view class="card">
      <text class="section-title">免打扰时段</text>
      <picker mode="time" :value="form.quietStart" @change="form.quietStart = $event.detail.value"><view class="row"><text>开始时间</text><text>{{ form.quietStart }} ›</text></view></picker>
      <picker mode="time" :value="form.quietEnd" @change="form.quietEnd = $event.detail.value"><view class="row"><text>结束时间</text><text>{{ form.quietEnd }} ›</text></view></picker>
      <text class="hint">免打扰期间到期的提醒会保留，并在时段结束后投递。</text>
    </view>
    <button class="save" :loading="saving" @click="save">保存设置</button>
  </view>
</template>

<script>
import { getReminderPreference, saveReminderPreference } from '@/api/px/life/reminder'
export default {
  data() { return { saving: false, form: { websocketEnabled: true, pushEnabled: true, emailEnabled: true, quietStart: '23:00', quietEnd: '07:00' } } },
  async onLoad() { const res = await getReminderPreference(); this.form = { ...this.form, ...(res.data || {}) } },
  methods: {
    async save() {
      this.saving = true
      try { await saveReminderPreference(this.form); uni.showToast({ title: '已保存', icon: 'success' }) } finally { this.saving = false }
    }
  }
}
</script>

<style lang="scss" scoped>
.preference-page { min-height: 100vh; padding: 24rpx $page-padding; background: $bg-page; }
.card { margin-bottom: 22rpx; padding: 0 28rpx; border-radius: $radius-xl; background: $bg-card; box-shadow: $shadow-card; }
.row { display: flex; align-items: center; justify-content: space-between; gap: 20rpx; padding: 28rpx 0; border-bottom: 1rpx solid $border-light; color: $text-primary; }
.row:last-child { border-bottom: none; }
.row view { display: flex; flex-direction: column; gap: 8rpx; }
.title, .section-title { color: $text-primary; font-size: $font-body; font-weight: 600; }
.section-title { display: block; padding-top: 26rpx; }
.hint { display: block; padding-bottom: 24rpx; color: $text-tertiary; font-size: $font-caption; line-height: 1.5; }
.row .hint { padding-bottom: 0; }
.save { margin-top: 30rpx; border-radius: $radius-xl; color: #fff; background: $primary; }
</style>
