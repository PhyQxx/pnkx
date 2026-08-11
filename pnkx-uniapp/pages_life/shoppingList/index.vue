<template>
  <view class="shopping-page">
    <view class="list" v-if="list.length > 0">
      <view
        class="card"
        v-for="item in list"
        :key="item.id"
        @click="openDetail(item)"
      >
        <view class="card__icon" :style="{ background: iconBg(item) }">
          <text class="card__emoji">{{ item.icon || '🛒' }}</text>
        </view>
        <view class="card__body">
          <view class="card__top">
            <text class="card__name">{{ item.name }}</text>
            <text class="card__time">{{ formatTime(item.createTime) }}</text>
          </view>
          <view class="card__progress">
            <view class="card__progress-bar">
              <view class="card__progress-fill" :style="{ width: progress(item).percent + '%' }"></view>
            </view>
            <text class="card__progress-text">{{ progress(item).done }}/{{ progress(item).total }}</text>
          </view>
        </view>
        <view class="card__arrow">›</view>
      </view>
    </view>

    <!-- Empty -->
    <view class="empty" v-else-if="!loading">
      <text class="empty__emoji">🛒</text>
      <text class="empty__text">还没有购物清单</text>
      <text class="empty__hint">点击下方按钮创建第一个清单</text>
    </view>

    <!-- Create FAB -->
    <view class="fab" @click="showCreate = true">
      <text class="fab__text">+</text>
    </view>

    <!-- Create Dialog -->
    <uni-popup ref="createPopup" type="dialog" :is-mask-click="true" v-if="showCreate" @change="onPopupChange">
      <uni-popup-dialog
        ref="createInput"
        type="input"
        title="新建购物清单"
        placeholder="请输入清单名称"
        :before-close="true"
        @confirm="handleCreate"
        @close="showCreate = false"
      ></uni-popup-dialog>
    </uni-popup>

    <view class="safe-bottom"></view>
  </view>
</template>

<script>
import {
  listShoppingList,
  listShoppingItem,
  addShoppingList
} from '@/api/px/life/shoppingList'

export default {
  data() {
    return {
      list: [],
      loading: true,
      showCreate: false,
      // 缓存每个清单的进度（id -> {done, total}）
      progressCache: {}
    }
  },
  onLoad() {
    this.loadList()
  },
  onShow() {
    if (!this.loading) this.loadList()
  },
  methods: {
    async loadList() {
      this.loading = true
      const res = await listShoppingList({ pageNum: 1, pageSize: 100 })
      this.list = res.rows || []
      this.loading = false
      // 并发拉取每个清单的购物项进度
      this.list.forEach(item => {
        listShoppingItem(item.id).then(r => {
          const items = r.rows || r.data || []
          const done = items.filter(i => i.checked).length
          this.$set(this.progressCache, item.id, { done, total: items.length })
        }).catch(() => {})
      })
    },
    progress(item) {
      return this.progressCache[item.id] || { done: 0, total: 0 }
    },
    iconBg(item) {
      const bgs = [
        'rgba(91, 158, 238, 0.12)',
        'rgba(52, 211, 153, 0.12)',
        'rgba(251, 191, 36, 0.12)',
        'rgba(255, 159, 67, 0.12)'
      ]
      return bgs[(item.id || 0) % bgs.length]
    },
    formatTime(time) {
      if (!time) return ''
      return this.$parseTime(time, '{m}-{d}')
    },
    openDetail(item) {
      uni.navigateTo({
        url: '/pages_life/shoppingList/detail?id=' + item.id + '&name=' + encodeURIComponent(item.name)
      })
    },
    async handleCreate(name) {
      if (!name || !name.trim()) {
        uni.showToast({ title: '请输入名称', icon: 'none' })
        return
      }
      try {
        await addShoppingList({ name: name.trim(), icon: '🛒' })
        this.showCreate = false
        uni.showToast({ title: '已创建', icon: 'success' })
        this.loadList()
      } catch (e) {
        uni.showToast({ title: '创建失败', icon: 'none' })
      }
    },
    onPopupChange(e) {
      if (!e.show) this.showCreate = false
    }
  }
}
</script>

<style lang="scss" scoped>
.shopping-page {
  min-height: 100vh;
  background: $bg-page;
  padding: $spacing-md $page-padding;
  padding-bottom: 160rpx;
}

.list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.card {
  display: flex;
  align-items: center;
  background: $bg-card;
  border-radius: $radius-lg;
  box-shadow: $shadow-card;
  padding: $spacing-md;

  &:active {
    opacity: 0.85;
  }

  &__icon {
    width: 80rpx;
    height: 80rpx;
    border-radius: $radius-md;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    margin-right: $spacing-md;
  }

  &__emoji {
    font-size: 40rpx;
  }

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__top {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  &__name {
    font-size: $font-body;
    font-weight: $font-weight-semibold;
    color: $text-primary;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
  }

  &__time {
    font-size: $font-mini;
    color: $text-tertiary;
    flex-shrink: 0;
    margin-left: $spacing-sm;
  }

  &__progress {
    display: flex;
    align-items: center;
    margin-top: $spacing-sm;
  }

  &__progress-bar {
    flex: 1;
    height: 8rpx;
    background: $gray-100;
    border-radius: $radius-full;
    overflow: hidden;
    margin-right: $spacing-sm;
  }

  &__progress-fill {
    height: 100%;
    background: linear-gradient(90deg, $primary, $primary-light);
    border-radius: $radius-full;
    transition: width 0.3s;
  }

  &__progress-text {
    font-size: $font-mini;
    color: $text-tertiary;
  }

  &__arrow {
    font-size: 40rpx;
    color: $text-tertiary;
    margin-left: $spacing-sm;
  }
}

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

  &__hint {
    font-size: $font-caption;
    color: $text-tertiary;
    margin-top: $spacing-xs;
  }
}

.fab {
  position: fixed;
  right: 40rpx;
  bottom: 60rpx;
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: linear-gradient(360deg, #287BF8 0%, #6EA8FF 100%);
  box-shadow: 0 4rpx 16rpx rgba(40, 123, 248, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99;

  &:active {
    transform: scale(0.95);
  }

  &__text {
    color: #fff;
    font-size: 56rpx;
    font-weight: 300;
    line-height: 1;
  }
}

.safe-bottom {
  height: env(safe-area-inset-bottom);
}
</style>
