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
        // 已登录：拉取用户信息后进入工具台（各工具的入口）
        this.$store.dispatch('GetInfo').then(() => {
          uni.reLaunch({ url: '/pages/tools' })
        }).catch(() => {
          uni.reLaunch({ url: '/pages/tools' })
        })
      } else {
        // 未登录直接进入工具台（工具台免登录）
        uni.reLaunch({ url: '/pages/tools' })
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
  background: linear-gradient(160deg, #6C9EFF 0%, #A8C8FF 50%, #D6E4FF 100%);
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
  margin-bottom: 48rpx;
}

.launch-spinner {
  width: 48rpx;
  height: 48rpx;
  border: 4rpx solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
