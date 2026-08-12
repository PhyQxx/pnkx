<template>
  <view class="launch-page">
    <view class="launch-content">
      <image class="launch-logo" src="/static/logo.png" mode="aspectFit" />
      <view class="launch-spinner"></view>
    </view>
  </view>
</template>

<script>
import { getToken } from '@/utils/auth'

export default {
  onLoad() {
    this.checkLogin()
  },
  methods: {
    checkLogin() {
      if (getToken()) {
        this.$store.dispatch('GetInfo').then(() => {
          uni.reLaunch({ url: '/pages/index' })
        }).catch(() => {
          uni.reLaunch({ url: '/pages/login' })
        })
      } else {
        uni.reLaunch({ url: '/pages/login' })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.launch-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    linear-gradient(180deg, rgba(247, 251, 255, 0.02) 0%, rgba(242, 247, 254, 0.7) 100%),
    url('/static/images/glacier-aurora-bg.png') center / cover no-repeat;
}

.launch-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.launch-logo {
  width: 160rpx;
  height: 160rpx;
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.82);
  padding: 14rpx;
  box-shadow: 0 16rpx 42rpx rgba(64, 111, 176, 0.16);
  margin-bottom: 48rpx;
}

.launch-spinner {
  width: 48rpx;
  height: 48rpx;
  border: 4rpx solid rgba(79, 134, 247, 0.18);
  border-top-color: $primary;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
