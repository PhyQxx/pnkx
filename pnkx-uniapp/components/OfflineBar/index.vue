<template>
  <view v-if="!isOnline" class="offline-bar" @tap="showSyncStatus">
    <text class="offline-icon">⚠</text>
    <text class="offline-text">离线模式 · 联网后自动同步</text>
    <view v-if="pendingCount > 0" class="pending-badge">
      <text class="pending-text">{{ pendingCount }}</text>
    </view>
  </view>
</template>

<script>
import { isOnline } from '@/utils/network'

export default {
  name: 'OfflineBar',
  data() {
    return {
      isOnline,
      pendingCount: 0,
      _timer: null
    }
  },
  mounted() {
    this.refreshPendingCount()
    this._timer = setInterval(() => this.refreshPendingCount(), 10000)
  },
  beforeUnmount() {
    if (this._timer) {
      clearInterval(this._timer)
      this._timer = null
    }
  },
  methods: {
    async refreshPendingCount() {
      // #ifdef APP-PLUS
      try {
        const sqliteDB = require('@/utils/sqliteDB').default
        this.pendingCount = await sqliteDB.getAllPendingCount()
      } catch (e) {
        // sqliteDB 未初始化时忽略
      }
      // #endif
    },
    showSyncStatus() {
      const statusText = this.isOnline ? '已连接' : '未连接'
      uni.showModal({
        title: '离线同步状态',
        content: `待同步操作：${this.pendingCount} 条\n当前网络：${statusText}`,
        showCancel: false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.offline-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10rpx 0;
  background: linear-gradient(90deg, #E8A33D, #F0B860);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9999;
}

.offline-icon {
  font-size: 24rpx;
  margin-right: 8rpx;
}

.offline-text {
  color: #fff;
  font-size: 22rpx;
  font-weight: 500;
}

.pending-badge {
  background: #F56C6C;
  border-radius: 50%;
  min-width: 32rpx;
  height: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 12rpx;
  padding: 0 8rpx;
}

.pending-text {
  color: #fff;
  font-size: 20rpx;
  font-weight: 600;
}
</style>
