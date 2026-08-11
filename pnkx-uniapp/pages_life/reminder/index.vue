<template>
  <view class="reminder-page">
    <!-- Tabs -->
    <view class="tabs">
      <view
        v-for="tab in tabs"
        :key="tab.value"
        class="tabs__item"
        :class="{ 'tabs__item--active': currentTab === tab.value }"
        @click="switchTab(tab.value)"
      >
        <text>{{ tab.label }}</text>
        <view v-if="tab.value === 'unread' && unreadCount > 0" class="tabs__badge">{{ unreadCount }}</view>
      </view>
    </view>

    <!-- Notifications -->
    <view class="list" v-if="filteredList.length > 0">
      <uni-swipe-action>
        <uni-swipe-action-item
          v-for="item in filteredList"
          :key="item.id"
          :right-options="[{ text: '删除', style: { backgroundColor: '#EF4444' } }]"
          @click="handleSwipe($event, item)"
        >
          <view class="notice" :class="{ 'notice--unread': item.status !== '2' }" @click="openNotice(item)">
            <view class="notice__icon" :style="{ background: iconOf(item).bg }">
              <text class="notice__emoji">{{ iconOf(item).emoji }}</text>
            </view>
            <view class="notice__body">
              <view class="notice__top">
                <text class="notice__title">{{ item.title }}</text>
                <view v-if="item.status !== '2'" class="notice__dot"></view>
              </view>
              <text class="notice__content">{{ item.content }}</text>
              <text class="notice__time">{{ formatTime(item.sendTime) }}</text>
            </view>
          </view>
        </uni-swipe-action-item>
      </uni-swipe-action>
    </view>

    <!-- Empty -->
    <view class="empty" v-else>
      <text class="empty__emoji">{{ currentTab === 'unread' ? '✅' : '🔔' }}</text>
      <text class="empty__text">{{ currentTab === 'unread' ? '没有未读通知' : '暂无通知' }}</text>
    </view>

    <!-- Footer -->
    <view class="footer" v-if="unreadCount > 0">
      <view class="footer__btn" @click="markAllRead">
        <text class="footer__text">全部标为已读</text>
      </view>
      <view class="safe-bottom"></view>
    </view>
  </view>
</template>

<script>
import { getNotifications, getUnreadCount, markRead, deleteNotification } from '@/api/px/life/reminder'

export default {
  data() {
    return {
      tabs: [
        { label: '全部', value: 'all' },
        { label: '未读', value: 'unread' }
      ],
      currentTab: 'all',
      list: [],
      unreadCount: 0
    }
  },
  computed: {
    filteredList() {
      if (this.currentTab === 'unread') {
        return this.list.filter(item => item.status !== '2')
      }
      return this.list
    }
  },
  onLoad() {
    this.loadData()
  },
  onShow() {
    this.loadData()
  },
  methods: {
    switchTab(value) {
      this.currentTab = value
    },
    async loadData() {
      const [listRes, countRes] = await Promise.all([
        getNotifications(),
        getUnreadCount()
      ])
      this.list = listRes.data || []
      this.unreadCount = countRes.data || 0
    },
    iconOf(item) {
      const map = {
        commemoration: { emoji: '🎂', bg: 'rgba(255, 159, 67, 0.12)' },
        menstruation: { emoji: '🌸', bg: 'rgba(253, 102, 151, 0.12)' },
        lovers_card: { emoji: '💕', bg: 'rgba(236, 65, 118, 0.12)' },
        subscription: { emoji: '💰', bg: 'rgba(251, 191, 36, 0.12)' },
        todo: { emoji: '📋', bg: 'rgba(108, 99, 255, 0.12)' }
      }
      return map[item.sourceType] || { emoji: '🔔', bg: 'rgba(91, 158, 238, 0.12)' }
    },
    formatTime(time) {
      if (!time) return ''
      return this.$parseTime(time, '{m}-{d} {h}:{i}')
    },
    async openNotice(item) {
      // 点击未读通知 → 标记已读
      if (item.status !== '2') {
        await markRead([item.id])
        item.status = '2'
        this.unreadCount = Math.max(0, this.unreadCount - 1)
      }
      uni.showModal({
        title: item.title || '通知',
        content: item.content || '暂无内容',
        showCancel: false,
        confirmText: '知道了'
      })
    },
    handleSwipe(e, item) {
      // 左滑按钮索引
      if (e.index === 0) {
        this.deleteOne(item)
      }
    },
    deleteOne(item) {
      uni.showModal({
        title: '提示',
        content: '确认删除这条通知？',
        success: async (res) => {
          if (!res.confirm) return
          await deleteNotification(item.id)
          this.list = this.list.filter(n => n.id !== item.id)
          if (item.status !== '2') {
            this.unreadCount = Math.max(0, this.unreadCount - 1)
          }
          uni.showToast({ title: '已删除', icon: 'none' })
        }
      })
    },
    markAllRead() {
      uni.showModal({
        title: '提示',
        content: '将全部未读通知标为已读？',
        success: async (res) => {
          if (!res.confirm) return
          await markRead([])
          this.list.forEach(n => { n.status = '2' })
          this.unreadCount = 0
          uni.showToast({ title: '已全部已读', icon: 'none' })
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.reminder-page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 160rpx;
}

/* Tabs */
.tabs {
  display: flex;
  background: $bg-card;
  padding: $spacing-md $spacing-lg;
  box-shadow: $shadow-card;
  position: sticky;
  top: 0;
  z-index: $z-base;

  &__item {
    flex: 1;
    text-align: center;
    padding: $spacing-sm 0;
    font-size: $font-body;
    color: $text-secondary;
    position: relative;
    transition: color $duration-fast $ease-default;

    &--active {
      color: $primary-dark;
      font-weight: $font-weight-semibold;
    }

    &--active::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 48rpx;
      height: 4rpx;
      background: $primary;
      border-radius: 4rpx;
    }
  }

  &__badge {
    position: absolute;
    top: 4rpx;
    right: 30%;
    min-width: 32rpx;
    height: 32rpx;
    line-height: 32rpx;
    padding: 0 8rpx;
    border-radius: $radius-full;
    background: $danger;
    color: #fff;
    font-size: $font-mini;
    text-align: center;
  }
}

/* Notice */
.list {
  padding: 0 $page-padding;
}

.notice {
  display: flex;
  align-items: flex-start;
  background: $bg-card;
  padding: $spacing-md;
  border-radius: $radius-lg;
  box-shadow: $shadow-card;
  margin-top: $spacing-sm;

  &--unread {
    border-left: 6rpx solid $primary;
  }

  &__icon {
    width: 72rpx;
    height: 72rpx;
    border-radius: $radius-md;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    margin-right: $spacing-md;
  }

  &__emoji {
    font-size: 36rpx;
  }

  &__body {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
  }

  &__top {
    display: flex;
    align-items: center;
  }

  &__title {
    font-size: $font-body;
    font-weight: $font-weight-semibold;
    color: $text-primary;
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__dot {
    width: 14rpx;
    height: 14rpx;
    border-radius: 50%;
    background: $danger;
    flex-shrink: 0;
    margin-left: $spacing-xs;
  }

  &__content {
    font-size: $font-caption;
    color: $text-tertiary;
    line-height: $line-height-normal;
    margin-top: 6rpx;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
  }

  &__time {
    font-size: $font-mini;
    color: $text-tertiary;
    margin-top: $spacing-xs;
  }
}

/* Empty */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;

  &__emoji {
    font-size: 96rpx;
    margin-bottom: $spacing-md;
    opacity: 0.6;
  }

  &__text {
    font-size: $font-body;
    color: $text-tertiary;
  }
}

/* Footer */
.footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: $bg-card;
  padding: $spacing-md $page-padding 0;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);

  &__btn {
    height: 84rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, $primary, $primary-dark);
    border-radius: $radius-lg;
  }

  &__text {
    font-size: $font-h3;
    font-weight: $font-weight-medium;
    color: $text-inverse;
  }
}

.safe-bottom {
  height: env(safe-area-inset-bottom);
  margin-top: $spacing-sm;
}
</style>
