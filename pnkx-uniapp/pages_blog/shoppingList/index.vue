<!--
 * @File: shoppingList/index
 * @Author: PHY
 * @Date: 2026/08/04
 * @Description: 购物清单列表页 — 浏览/新建/删除清单，展示完成进度
-->
<template>
  <view class="shopping-page">
    <view class="fixed-header">
      <view class="overview-panel">
        <view class="overview-main">
          <text class="overview-title">购物清单</text>
          <text class="overview-subtitle">{{ headerSubtitle }}</text>
        </view>
        <view class="overview-stat">
          <text class="stat-number">{{ list.length }}</text>
          <text class="stat-label">个清单</text>
        </view>
      </view>
    </view>

    <scroll-view
      class="content-scroll"
      scroll-y
      refresher-enabled
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onRefresh"
    >
      <!-- 空状态 -->
      <view v-if="list.length === 0 && !loading" class="empty-state">
        <view class="empty-icon">
          <uni-icons type="cart" size="42" :color="themeColor" />
        </view>
        <text class="empty-text">还没有购物清单</text>
        <text class="empty-tip">点击右下角按钮，创建第一个清单</text>
      </view>

      <view v-else class="list-cards">
        <view
          v-for="item in list"
          :key="item.id"
          class="list-card"
          @click="handleDetail(item)"
          @longpress="handleDelete(item)"
        >
          <view class="card-top">
            <view class="card-icon">
              <uni-icons type="cart" size="22" color="#fff" />
            </view>
            <view class="card-title-wrap">
              <text class="card-title">{{ item.name || '未命名清单' }}</text>
              <text class="card-time">{{ formatDate(item.createTime) }}</text>
            </view>
            <uni-icons type="right" size="16" color="#CBD5E1" />
          </view>

          <!-- 进度条 -->
          <view class="card-progress">
            <view class="progress-info">
              <text class="progress-text">{{ progressOf(item).checked }} / {{ progressOf(item).total }}</text>
              <text class="progress-percent">{{ progressOf(item).percent }}%</text>
            </view>
            <view class="progress-bar">
              <view class="progress-bar-inner" :style="{ width: progressOf(item).percent + '%' }"></view>
            </view>
          </view>
        </view>
      </view>

      <view class="bottom-safe"></view>
    </scroll-view>

    <!-- 新建清单 -->
    <view class="fab-button" @click="handleAdd">
      <uni-icons type="plus" size="24" color="#FFFFFF" />
    </view>

    <!-- 新建弹窗 -->
    <uni-popup ref="addPopup" type="dialog">
      <uni-popup-dialog
        ref="addInput"
        mode="input"
        title="新建购物清单"
        placeholder="输入清单名称，如：周末采购"
        :before-close="false"
        @confirm="handleAddConfirm"
      ></uni-popup-dialog>
    </uni-popup>
  </view>
</template>

<script>
import { listShoppingList, addShoppingList, delShoppingList, listShoppingItem } from '@/api/px/life/shopping'

export default {
  name: 'ShoppingListIndex',
  data() {
    return {
      themeColor: '#0EA5E9',
      list: [],
      // 清单进度缓存：{ [listId]: { checked, total } }
      progressMap: {},
      loading: false,
      isRefreshing: false
    }
  },
  computed: {
    headerSubtitle() {
      const weekList = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      const now = new Date()
      return `${now.getMonth() + 1}月${now.getDate()}日 ${weekList[now.getDay()]}`
    }
  },
  onLoad() {
    this.loadList()
  },
  onShow() {
    if (this.$data.__loaded) {
      this.loadList()
    }
  },
  methods: {
    async loadList() {
      this.loading = true
      try {
        const res = await listShoppingList({ pageNum: 1, pageSize: 100 })
        if (res.code === 200) {
          this.list = res.rows || []
          // 并发加载每个清单的进度
          this.list.forEach(item => this.loadProgress(item.id))
        }
      } catch (e) {
        console.error('加载购物清单失败', e)
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.loading = false
        this.isRefreshing = false
        this.$data.__loaded = true
      }
    },

    async loadProgress(listId) {
      try {
        const res = await listShoppingItem({ listId, pageNum: 1, pageSize: 200 })
        if (res.code === 200) {
          const rows = res.rows || []
          const checked = rows.filter(r => r.checked === true || r.checked === 1 || r.checked === '1').length
          this.$set(this.progressMap, listId, {
            checked,
            total: rows.length,
            percent: rows.length === 0 ? 0 : Math.round(checked / rows.length * 100)
          })
        }
      } catch (e) {
        // 静默失败，进度条显示为 0
      }
    },

    progressOf(item) {
      return this.progressMap[item.id] || { checked: 0, total: 0, percent: 0 }
    },

    onRefresh() {
      this.isRefreshing = true
      this.loadList()
    },

    handleDetail(item) {
      uni.navigateTo({
        url: `/pages_blog/shoppingList/detail?id=${item.id}&name=${encodeURIComponent(item.name || '')}`
      })
    },

    handleAdd() {
      this.$refs.addPopup.open('center')
    },

    async handleAddConfirm(value) {
      const name = (value || '').trim()
      if (!name) {
        uni.showToast({ title: '请输入清单名称', icon: 'none' })
        return
      }
      try {
        const res = await addShoppingList({ name })
        if (res.code === 200) {
          uni.showToast({ title: '已创建', icon: 'success' })
          this.loadList()
        } else {
          uni.showToast({ title: res.msg || '创建失败', icon: 'none' })
        }
      } catch (e) {
        console.error('创建清单失败', e)
        uni.showToast({ title: '创建失败', icon: 'none' })
      }
    },

    handleDelete(item) {
      uni.showModal({
        title: '删除清单',
        content: `确定要删除「${item.name || '该清单'}」吗？清单下的所有购物项也会一并删除。`,
        success: async (res) => {
          if (!res.confirm) return
          try {
            const r = await delShoppingList(item.id)
            if (r.code === 200) {
              uni.showToast({ title: '已删除', icon: 'success' })
              this.loadList()
            }
          } catch (e) {
            console.error('删除清单失败', e)
            uni.showToast({ title: '删除失败', icon: 'none' })
          }
        }
      })
    },

    formatDate(t) {
      if (!t) return ''
      return t.substring(0, 10)
    }
  }
}
</script>

<style lang="scss" scoped>
.shopping-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, rgba(14, 165, 233, 0.10) 0%, $bg-page 360rpx);

  .fixed-header {
    position: sticky;
    top: 0;
    z-index: 100;
    background: linear-gradient(180deg, rgba(14, 165, 233, 0.14) 0%, rgba(244, 246, 249, 0.96) 100%);
    padding: 24rpx $page-padding 18rpx;
    box-shadow: 0 8rpx 24rpx rgba(74, 85, 104, 0.06);

    .overview-panel {
      display: flex;
      align-items: center;
      justify-content: space-between;

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

      .overview-stat {
        min-width: 124rpx;
        height: 124rpx;
        border-radius: $radius-full;
        background: linear-gradient(135deg, #0EA5E9 0%, #38BDF8 100%);
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        box-shadow: 0 12rpx 28rpx rgba(14, 165, 233, 0.28);

        .stat-number {
          font-size: 36rpx;
          font-weight: 700;
          color: $text-inverse;
          line-height: 1.1;
        }

        .stat-label {
          margin-top: 4rpx;
          font-size: $font-mini;
          color: rgba(255, 255, 255, 0.86);
        }
      }
    }
  }

  .content-scroll {
    flex: 1;
    height: 0;
    padding: 22rpx $page-padding 0;
  }

  .list-cards {
    display: flex;
    flex-direction: column;
    gap: 18rpx;
    padding-bottom: 160rpx;
  }

  .list-card {
    padding: 26rpx;
    border-radius: $radius-xl;
    background-color: $bg-card;
    box-shadow: $shadow-card;
    transition: transform $duration-fast $ease-default;

    &:active { transform: scale(0.985); }

    .card-top {
      display: flex;
      align-items: center;
      gap: 18rpx;
      margin-bottom: 20rpx;

      .card-icon {
        width: 72rpx;
        height: 72rpx;
        border-radius: $radius-lg;
        background: linear-gradient(135deg, #0EA5E9, #38BDF8);
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        box-shadow: 0 8rpx 18rpx rgba(14, 165, 233, 0.26);
      }

      .card-title-wrap {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
        gap: 6rpx;

        .card-title {
          font-size: $font-body;
          font-weight: $font-weight-semibold;
          color: $text-primary;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .card-time {
          font-size: $font-mini;
          color: $text-tertiary;
        }
      }
    }

    .card-progress {
      .progress-info {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 10rpx;

        .progress-text {
          font-size: $font-caption;
          color: $text-secondary;
          font-weight: $font-weight-semibold;
        }

        .progress-percent {
          font-size: $font-caption;
          color: #0EA5E9;
          font-weight: 700;
        }
      }

      .progress-bar {
        height: 12rpx;
        border-radius: $radius-full;
        background-color: rgba(14, 165, 233, 0.12);
        overflow: hidden;

        .progress-bar-inner {
          height: 100%;
          border-radius: $radius-full;
          background: linear-gradient(90deg, #0EA5E9, #38BDF8);
          transition: width 0.4s;
        }
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
      background-color: rgba(14, 165, 233, 0.12);
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

  .fab-button {
    position: fixed;
    right: $page-padding;
    bottom: 92rpx;
    width: 104rpx;
    height: 104rpx;
    background: linear-gradient(135deg, #0EA5E9 0%, #38BDF8 100%);
    border-radius: $radius-full;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 12rpx 28rpx rgba(14, 165, 233, 0.38);
    z-index: 999;
    transition: transform 0.2s;

    &:active { transform: scale(0.92); }
  }
}
</style>
