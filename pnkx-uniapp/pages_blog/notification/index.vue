<!--
 * @File: notification/index
 * @Author: PHY
 * @Date: 2026/08/04
 * @Description: 通知中心 — 展示提醒投递记录，支持全部/未读筛选、标记已读
-->
<template>
  <view class="notification-page">
    <!-- 顶部概览 -->
    <view class="fixed-header">
      <view class="overview-panel">
        <view class="overview-main">
          <text class="overview-title">通知中心</text>
          <text class="overview-subtitle">{{ headerSubtitle }}</text>
        </view>
        <view class="overview-badge">
          <text class="badge-number">{{ unreadCount }}</text>
          <text class="badge-label">未读</text>
        </view>
      </view>

      <!-- Tab 切换 -->
      <view class="view-switch">
        <view
          class="switch-item"
          :class="{ active: activeTab === 'all' }"
          @click="switchTab('all')"
        >
          <text>全部</text>
          <text v-if="allCount > 0" class="switch-count">{{ allCount }}</text>
        </view>
        <view
          class="switch-item"
          :class="{ active: activeTab === 'unread' }"
          @click="switchTab('unread')"
        >
          <text>未读</text>
          <text v-if="unreadCount > 0" class="switch-count switch-count--unread">{{ unreadCount }}</text>
        </view>
      </view>
    </view>

    <!-- 通知列表 -->
    <scroll-view
      class="content-scroll"
      scroll-y
      refresher-enabled
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onRefresh"
    >
      <!-- 空状态 -->
      <view v-if="filteredList.length === 0 && !loading" class="empty-state">
        <view class="empty-icon">
          <uni-icons type="email" size="42" :color="themeColor" />
        </view>
        <text class="empty-text">{{ activeTab === 'unread' ? '没有未读通知' : '暂无通知' }}</text>
        <text class="empty-tip">{{ activeTab === 'unread' ? '所有消息都已看完啦' : '提醒触发后会在这里展示' }}</text>
      </view>

      <!-- 列表 -->
      <view v-else class="notification-list">
        <uni-swipe-action-item
          v-for="item in filteredList"
          :key="item.id"
          :right-options="swipeOptions"
          @click="handleDelete(item)"
        >
        <view
          class="notification-card"
          :class="{ 'notification-card--unread': !isRead(item) }"
          @click="handleItemClick(item)"
        >
          <!-- 左侧渠道图标 -->
          <view class="card-icon" :style="{ background: getChannelBg(item.channel) }">
            <uni-icons :type="getSourceIcon(item.sourceType)" size="20" color="#fff" />
          </view>

          <view class="card-body">
            <view class="card-top">
              <text class="card-title">{{ item.title || '收到一条新通知' }}</text>
              <view v-if="!isRead(item)" class="unread-dot"></view>
            </view>
            <text v-if="item.content" class="card-content">{{ item.content }}</text>
            <view class="card-footer">
              <view class="card-tag" :style="{ color: themeColor, background: getChannelBg(item.channel, true) }">
                <text>{{ getChannelLabel(item.channel) }}</text>
              </view>
              <text class="card-time">{{ formatTime(item.sendTime) }}</text>
            </view>
          </view>
        </view>
        </uni-swipe-action-item>
      </view>

      <view class="bottom-safe"></view>
    </scroll-view>

    <!-- 全部已读悬浮按钮 -->
    <view
      v-if="unreadCount > 0"
      class="read-all-button"
      @click="handleMarkAllRead"
    >
      <uni-icons type="checkbox-filled" size="18" color="#fff" />
      <text>全部已读</text>
    </view>
  </view>
</template>

<script>
import { getNotifications, getUnreadCount, markRead, deleteNotification } from '@/api/px/life/reminder'

export default {
  name: 'NotificationIndex',
  data() {
    return {
      themeColor: '#6366F1',
      activeTab: 'all',
      loading: false,
      isRefreshing: false,
      // 全部通知（按发送时间倒序）
      notificationList: [],
      // 未读数
      unreadCount: 0,
      swipeOptions: [{ text: '删除', style: { backgroundColor: '#EF4444' } }]
    }
  },
  computed: {
    allCount() {
      return this.notificationList.length
    },
    // 当前 Tab 展示的列表
    filteredList() {
      if (this.activeTab === 'unread') {
        return this.notificationList.filter(item => !this.isRead(item))
      }
      return this.notificationList
    },
    headerSubtitle() {
      const weekList = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      const now = new Date()
      return `${now.getMonth() + 1}月${now.getDate()}日 ${weekList[now.getDay()]}`
    }
  },
  onLoad() {
    this.loadAll()
  },
  onShow() {
    // 从详情返回时刷新未读数，保证铃铛红点准确
    this.loadUnreadCount()
  },
  methods: {
    // 一次性加载列表 + 未读数
    async loadAll() {
      this.loading = true
      try {
        await Promise.all([this.loadList(), this.loadUnreadCount()])
      } finally {
        this.loading = false
      }
    },

    async loadList() {
      try {
        const res = await getNotifications()
        if (res.code === 200) {
          const list = res.data || []
          // 按发送时间倒序排列
          this.notificationList = list.sort((a, b) => {
            return this.getTime(b.sendTime) - this.getTime(a.sendTime)
          })
        }
      } catch (e) {
        console.error('加载通知列表失败', e)
      }
    },

    async loadUnreadCount() {
      try {
        const res = await getUnreadCount()
        if (res.code === 200) {
          this.unreadCount = Number(res.data) || 0
        }
      } catch (e) {
        console.error('加载未读数失败', e)
      }
    },

    // 状态：0 已发送 1 发送失败 2 已读
    isRead(item) {
      return String(item.status) === '2'
    },

    switchTab(tab) {
      if (this.activeTab === tab) return
      this.activeTab = tab
    },

    // 点击通知：未读则标记已读
    async handleItemClick(item) {
      if (this.isRead(item)) return
      try {
        const res = await markRead([item.id])
        if (res.code === 200) {
          item.status = '2'
          this.unreadCount = Math.max(this.unreadCount - 1, 0)
        }
      } catch (e) {
        console.error('标记已读失败', e)
      }
    },
    async handleDelete(item) {
      try {
        const wasUnread = !this.isRead(item)
        const res = await deleteNotification(item.id)
        if (res.code === 200) {
          this.notificationList = this.notificationList.filter(n => n.id !== item.id)
          if (wasUnread) this.unreadCount = Math.max(this.unreadCount - 1, 0)
          uni.showToast({ title: '已删除', icon: 'success' })
        }
      } catch (e) {
        uni.showToast({ title: '删除失败', icon: 'none' })
      }
    },

    // 全部已读
    handleMarkAllRead() {
      uni.showModal({
        title: '全部已读',
        content: `确定将 ${this.unreadCount} 条未读通知标记为已读吗？`,
        success: async (res) => {
          if (!res.confirm) return
          try {
            const r = await markRead([])
            if (r.code === 200) {
              this.notificationList.forEach(item => { item.status = '2' })
              this.unreadCount = 0
              uni.showToast({ title: '已全部标记为已读', icon: 'success' })
            }
          } catch (e) {
            console.error('全部已读失败', e)
            uni.showToast({ title: '操作失败', icon: 'none' })
          }
        }
      })
    },

    onRefresh() {
      this.isRefreshing = true
      this.loadAll().finally(() => {
        this.isRefreshing = false
      })
    },

    // 渠道标签
    getChannelLabel(channel) {
      const map = { websocket: '站内', email: '邮件', wechat: '微信' }
      return map[channel] || '通知'
    },

    // 渠道图标背景色
    getChannelBg(channel, light = false) {
      const map = {
        websocket: light ? 'rgba(99, 102, 241, 0.12)' : 'linear-gradient(135deg, #6366F1, #818CF8)',
        email: light ? 'rgba(16, 185, 129, 0.12)' : 'linear-gradient(135deg, #10B981, #34D399)',
        wechat: light ? 'rgba(56, 179, 248, 0.12)' : 'linear-gradient(135deg, #38BDF8, #7DD3FC)'
      }
      return map[channel] || (light ? 'rgba(148, 163, 184, 0.12)' : 'linear-gradient(135deg, #94A3B8, #CBD5E1)')
    },

    // 来源类型图标
    getSourceIcon(sourceType) {
      const map = {
        commemoration: 'star',
        lovers_card: 'heart',
        menstruation: 'heart-filled',
        todo: 'checkbox-filled'
      }
      return map[sourceType] || 'sound'
    },

    getTime(t) {
      const v = t ? new Date(t).getTime() : 0
      return Number.isNaN(v) ? 0 : v
    },

    // 友好的时间显示
    formatTime(t) {
      if (!t) return ''
      const time = new Date(t)
      const now = new Date()
      const diff = now.getTime() - time.getTime()
      const minute = 60 * 1000
      const hour = 60 * minute
      const day = 24 * hour

      if (diff < minute) return '刚刚'
      if (diff < hour) return Math.floor(diff / minute) + ' 分钟前'
      if (diff < day) return Math.floor(diff / hour) + ' 小时前'
      if (diff < 7 * day) return Math.floor(diff / day) + ' 天前'
      // 超过一周显示完整日期
      const m = time.getMonth() + 1
      const d = time.getDate()
      const hh = String(time.getHours()).padStart(2, '0')
      const mm = String(time.getMinutes()).padStart(2, '0')
      return `${m}月${d}日 ${hh}:${mm}`
    }
  }
}
</script>

<style lang="scss" scoped>
.notification-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, rgba(99, 102, 241, 0.10) 0%, $bg-page 360rpx);

  .fixed-header {
    position: sticky;
    top: 0;
    z-index: 100;
    background: linear-gradient(180deg, rgba(99, 102, 241, 0.14) 0%, rgba(244, 246, 249, 0.96) 100%);
    padding: 24rpx $page-padding 18rpx;
    box-shadow: 0 8rpx 24rpx rgba(74, 85, 104, 0.06);

    .overview-panel {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 20rpx;

      .overview-main {
        display: flex;
        flex-direction: column;
        gap: 8rpx;

        .overview-title {
          font-size: 40rpx;
          font-weight: 700;
          color: $text-primary;
          line-height: 1.2;
        }

        .overview-subtitle {
          font-size: $font-caption;
          color: $text-secondary;
        }
      }

      .overview-badge {
        min-width: 124rpx;
        height: 124rpx;
        padding: 0 16rpx;
        border-radius: $radius-full;
        background: linear-gradient(135deg, #6366F1 0%, #818CF8 100%);
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        box-shadow: 0 12rpx 28rpx rgba(99, 102, 241, 0.28);

        .badge-number {
          font-size: 36rpx;
          font-weight: 700;
          color: $text-inverse;
          line-height: 1.1;
        }

        .badge-label {
          margin-top: 4rpx;
          font-size: $font-mini;
          color: rgba(255, 255, 255, 0.86);
        }
      }
    }

    .view-switch {
      display: flex;
      padding: 6rpx;
      background-color: rgba(255, 255, 255, 0.82);
      border-radius: $radius-full;
      border: 1rpx solid rgba(99, 102, 241, 0.14);
      gap: 6rpx;

      .switch-item {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 10rpx;
        height: 64rpx;
        font-size: $font-caption;
        color: $text-tertiary;
        border-radius: $radius-full;
        transition: all 0.3s;

        &.active {
          color: #6366F1;
          background-color: #FFFFFF;
          box-shadow: $shadow-sm;
          font-weight: $font-weight-semibold;
        }

        .switch-count {
          min-width: 32rpx;
          height: 32rpx;
          line-height: 32rpx;
          padding: 0 8rpx;
          border-radius: $radius-full;
          font-size: $font-mini;
          text-align: center;
          background-color: rgba(148, 163, 184, 0.18);
          color: $text-secondary;

          &--unread {
            background-color: rgba(99, 102, 241, 0.16);
            color: #6366F1;
          }
        }
      }
    }
  }

  .content-scroll {
    flex: 1;
    height: 0;
    padding: 22rpx $page-padding 0;
  }

  .notification-list {
    display: flex;
    flex-direction: column;
    gap: 18rpx;
    padding-bottom: 180rpx;
  }

  .notification-card {
    display: flex;
    align-items: flex-start;
    gap: 20rpx;
    padding: 26rpx;
    border-radius: $radius-xl;
    background-color: $bg-card;
    box-shadow: $shadow-card;
    transition: transform $duration-fast $ease-default, opacity $duration-fast $ease-default;

    &:active {
      transform: scale(0.985);
      opacity: 0.96;
    }

    // 未读卡片：左侧色条 + 略加重背景
    &--unread {
      position: relative;
      background-color: #FFFFFF;
      border: 1rpx solid rgba(99, 102, 241, 0.18);

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 26rpx;
        bottom: 26rpx;
        width: 6rpx;
        border-radius: $radius-full;
        background: linear-gradient(180deg, #6366F1, #818CF8);
      }
    }

    .card-icon {
      width: 76rpx;
      height: 76rpx;
      border-radius: $radius-lg;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .card-body {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 12rpx;
    }

    .card-top {
      display: flex;
      align-items: center;
      gap: 12rpx;

      .card-title {
        flex: 1;
        min-width: 0;
        font-size: $font-body;
        font-weight: $font-weight-semibold;
        color: $text-primary;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .unread-dot {
        width: 16rpx;
        height: 16rpx;
        border-radius: $radius-full;
        background-color: #6366F1;
        flex-shrink: 0;
      }
    }

    .card-content {
      font-size: $font-caption;
      color: $text-secondary;
      line-height: 1.5;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .card-footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-top: 4rpx;

      .card-tag {
        padding: 4rpx 16rpx;
        border-radius: $radius-full;
        font-size: $font-mini;
      }

      .card-time {
        font-size: $font-mini;
        color: $text-tertiary;
      }
    }
  }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 430rpx;
    padding: $spacing-3xl 0;

    .empty-icon {
      width: 132rpx;
      height: 132rpx;
      border-radius: $radius-full;
      background-color: rgba(99, 102, 241, 0.12);
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .empty-text {
      font-size: $font-body;
      color: $text-secondary;
      margin-top: $spacing-lg;
      font-weight: $font-weight-semibold;
    }

    .empty-tip {
      font-size: $font-caption;
      color: $text-tertiary;
      margin-top: $spacing-xs;
    }
  }

  .bottom-safe {
    height: env(safe-area-inset-bottom);
  }

  .read-all-button {
    position: fixed;
    left: 50%;
    transform: translateX(-50%);
    bottom: calc(env(safe-area-inset-bottom) + 40rpx);
    display: flex;
    align-items: center;
    gap: 10rpx;
    height: 84rpx;
    padding: 0 44rpx;
    background: linear-gradient(135deg, #6366F1 0%, #818CF8 100%);
    border-radius: $radius-full;
    box-shadow: 0 12rpx 28rpx rgba(99, 102, 241, 0.38);
    z-index: 999;
    transition: transform 0.2s;

    text {
      font-size: $font-body;
      font-weight: $font-weight-semibold;
      color: #fff;
    }

    &:active {
      transform: translateX(-50%) scale(0.95);
    }
  }
}
</style>
