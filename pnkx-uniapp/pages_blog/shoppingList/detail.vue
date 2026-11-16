<!--
 * @File: shoppingList/detail
 * @Author: PHY
 * @Date: 2026/08/04
 * @Description: 购物清单详情页 — 购物项勾选/增删/清空已购
-->
<template>
  <view class="detail-page">
    <!-- 顶部清单信息 -->
    <view class="header-card">
      <view class="header-row">
        <view class="header-icon">
          <uni-icons type="cart" size="26" color="#fff" />
        </view>
        <view class="header-info">
          <text class="header-title">{{ listName }}</text>
          <text class="header-meta">{{ checkedCount }} / {{ items.length }} 已购 · {{ percent }}%</text>
        </view>
      </view>
      <view class="progress-bar">
        <view class="progress-inner" :style="{ width: percent + '%' }"></view>
      </view>
    </view>

    <!-- 工具条 -->
    <view class="toolbar">
      <view class="toolbar-stat">
        <text class="stat-active">待购 {{ uncheckedCount }}</text>
        <text class="stat-done">已购 {{ checkedCount }}</text>
      </view>
      <view v-if="checkedCount > 0" class="toolbar-clear" @click="handleClearChecked">
        <uni-icons type="trash" size="15" color="#EF4444" />
        <text>清空已购</text>
      </view>
    </view>

    <!-- 购物项列表 -->
    <scroll-view class="content-scroll" scroll-y>
      <view v-if="items.length === 0 && !loading" class="empty-state">
        <view class="empty-icon">
          <uni-icons type="cart" size="42" :color="themeColor" />
        </view>
        <text class="empty-text">清单是空的</text>
        <text class="empty-tip">点击下方"添加"录入要买的东西</text>
      </view>

      <view v-else class="item-list">
        <view
          v-for="item in sortedItems"
          :key="item.id"
          class="item-card"
          :class="{ 'item-card--checked': isChecked(item) }"
        >
          <!-- 勾选按钮 -->
          <view class="item-check" @click="toggleCheck(item)">
            <uni-icons
              :type="isChecked(item) ? 'checkbox-filled' : 'circle'"
              :size="24"
              :color="isChecked(item) ? '#0EA5E9' : '#CBD5E1'"
            />
          </view>

          <view class="item-main">
            <text class="item-name">{{ item.name }}</text>
            <view class="item-tags">
              <text v-if="item.quantity" class="item-qty">{{ item.quantity }}</text>
              <view v-if="item.addedFromMeal" class="item-source">
                <uni-icons type="soup-noodles" size="12" color="#10B981" />
                <text>来自膳食</text>
              </view>
            </view>
          </view>

          <view class="item-del" @click="handleDeleteItem(item)">
            <uni-icons type="close" size="16" color="#CBD5E1" />
          </view>
        </view>
      </view>

      <view class="bottom-placeholder"></view>
    </scroll-view>

    <!-- 底部添加栏 -->
    <view class="add-bar">
      <input
        v-model="newItemName"
        class="add-input"
        placeholder="添加要买的东西..."
        confirm-type="done"
        @confirm="handleAddItem"
      />
      <view class="add-btn" :class="{ disabled: !newItemName.trim() }" @click="handleAddItem">
        <uni-icons type="plus" size="22" color="#fff" />
      </view>
    </view>
  </view>
</template>

<script>
import { getShoppingList, listShoppingItem, addShoppingItem, updateShoppingItem, delShoppingItem, clearCheckedShoppingItem } from '@/api/px/life/shopping'

export default {
  name: 'ShoppingListDetail',
  data() {
    return {
      themeColor: '#0EA5E9',
      listId: null,
      listName: '购物清单',
      items: [],
      loading: false,
      newItemName: ''
    }
  },
  computed: {
    checkedCount() {
      return this.items.filter(i => this.isChecked(i)).length
    },
    uncheckedCount() {
      return this.items.length - this.checkedCount
    },
    percent() {
      if (this.items.length === 0) return 0
      return Math.round(this.checkedCount / this.items.length * 100)
    },
    // 未购置顶，已购沉底
    sortedItems() {
      return [...this.items].sort((a, b) => {
        const ca = this.isChecked(a) ? 1 : 0
        const cb = this.isChecked(b) ? 1 : 0
        return ca - cb
      })
    }
  },
  onLoad(options) {
    this.listId = options.id
    if (options.name) {
      this.listName = decodeURIComponent(options.name)
    }
    this.loadDetail()
  },
  onShow() {
    if (this.$data.__loaded) {
      this.loadItems()
    }
  },
  methods: {
    async loadDetail() {
      await Promise.all([this.loadListInfo(), this.loadItems()])
      this.$data.__loaded = true
    },

    async loadListInfo() {
      try {
        const res = await getShoppingList(this.listId)
        if (res.code === 200 && res.data) {
          this.listName = res.data.name || '购物清单'
        }
      } catch (e) {
        console.error('加载清单信息失败', e)
      }
    },

    async loadItems() {
      this.loading = true
      try {
        const res = await listShoppingItem({ listId: this.listId, pageNum: 1, pageSize: 500 })
        if (res.code === 200) {
          this.items = res.rows || []
        }
      } catch (e) {
        console.error('加载购物项失败', e)
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },

    isChecked(item) {
      return item.checked === true || item.checked === 1 || item.checked === '1'
    },

    // 切换勾选状态
    async toggleCheck(item) {
      const nextChecked = !this.isChecked(item)
      // 乐观更新
      item.checked = nextChecked
      try {
        const res = await updateShoppingItem({ ...item, checked: nextChecked })
        if (res.code !== 200) {
          // 失败回滚
          item.checked = !nextChecked
          uni.showToast({ title: '操作失败', icon: 'none' })
        }
      } catch (e) {
        item.checked = !nextChecked
        console.error('切换勾选失败', e)
        uni.showToast({ title: '操作失败', icon: 'none' })
      }
    },

    async handleAddItem() {
      const name = this.newItemName.trim()
      if (!name) return
      try {
        const res = await addShoppingItem({ listId: this.listId, name, checked: false })
        if (res.code === 200) {
          this.newItemName = ''
          this.loadItems()
        } else {
          uni.showToast({ title: res.msg || '添加失败', icon: 'none' })
        }
      } catch (e) {
        console.error('添加购物项失败', e)
        uni.showToast({ title: '添加失败', icon: 'none' })
      }
    },

    handleDeleteItem(item) {
      uni.showModal({
        title: '删除项',
        content: `删除「${item.name}」？`,
        success: async (res) => {
          if (!res.confirm) return
          try {
            const r = await delShoppingItem(item.id)
            if (r.code === 200) {
              this.loadItems()
            }
          } catch (e) {
            console.error('删除购物项失败', e)
            uni.showToast({ title: '删除失败', icon: 'none' })
          }
        }
      })
    },

    handleClearChecked() {
      const count = this.checkedCount
      uni.showModal({
        title: '清空已购',
        content: `确定清空 ${count} 条已购项吗？`,
        success: async (res) => {
          if (!res.confirm) return
          try {
            const r = await clearCheckedShoppingItem(this.listId)
            if (r.code === 200) {
              uni.showToast({ title: '已清空', icon: 'success' })
              this.loadItems()
            }
          } catch (e) {
            console.error('清空已购失败', e)
            uni.showToast({ title: '操作失败', icon: 'none' })
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.detail-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: $bg-page;
}

// 顶部清单卡
.header-card {
  padding: 28rpx $page-padding;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.12), rgba(56, 189, 248, 0.06));
  border-bottom: 1rpx solid rgba(14, 165, 233, 0.10);

  .header-row {
    display: flex;
    align-items: center;
    gap: 18rpx;
    margin-bottom: 18rpx;

    .header-icon {
      width: 76rpx;
      height: 76rpx;
      border-radius: $radius-lg;
      background: linear-gradient(135deg, #0EA5E9, #38BDF8);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      box-shadow: 0 8rpx 20rpx rgba(14, 165, 233, 0.28);
    }

    .header-info {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 6rpx;

      .header-title {
        font-size: 36rpx;
        font-weight: 700;
        color: $text-primary;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .header-meta {
        font-size: $font-caption;
        color: #0EA5E9;
        font-weight: $font-weight-semibold;
      }
    }
  }

  .progress-bar {
    height: 14rpx;
    border-radius: $radius-full;
    background-color: rgba(14, 165, 233, 0.14);
    overflow: hidden;

    .progress-inner {
      height: 100%;
      border-radius: $radius-full;
      background: linear-gradient(90deg, #0EA5E9, #38BDF8);
      transition: width 0.4s;
    }
  }
}

// 工具条
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx $page-padding;

  .toolbar-stat {
    display: flex;
    align-items: center;
    gap: 20rpx;

    .stat-active {
      font-size: $font-caption;
      color: $text-primary;
      font-weight: $font-weight-semibold;
    }

    .stat-done {
      font-size: $font-caption;
      color: $text-tertiary;
    }
  }

  .toolbar-clear {
    display: flex;
    align-items: center;
    gap: 4rpx;
    padding: 6rpx 16rpx;
    border-radius: $radius-full;
    background-color: rgba(239, 68, 68, 0.08);

    text {
      font-size: $font-caption;
      color: #EF4444;
    }
  }
}

.content-scroll {
  flex: 1;
  height: 0;
  padding: 0 $page-padding;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  padding-bottom: 40rpx;
}

.item-card {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 22rpx 24rpx;
  border-radius: $radius-lg;
  background-color: $bg-card;
  box-shadow: $shadow-sm;
  transition: opacity 0.2s;

  &--checked {
    opacity: 0.55;

    .item-name {
      text-decoration: line-through;
      color: $text-tertiary;
    }
  }

  .item-check {
    width: 52rpx;
    height: 52rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .item-main {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 6rpx;

    .item-name {
      font-size: $font-body;
      color: $text-primary;
      font-weight: $font-weight-medium;
      line-height: 1.4;
    }

    .item-tags {
      display: flex;
      align-items: center;
      gap: 12rpx;

      .item-qty {
        font-size: $font-mini;
        color: $text-secondary;
        padding: 2rpx 12rpx;
        border-radius: $radius-full;
        background-color: $bg-page;
      }

      .item-source {
        display: flex;
        align-items: center;
        gap: 4rpx;
        padding: 2rpx 12rpx;
        border-radius: $radius-full;
        background-color: rgba(16, 185, 129, 0.10);

        text {
          font-size: $font-mini;
          color: #10B981;
        }
      }
    }
  }

  .item-del {
    width: 48rpx;
    height: 48rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400rpx;

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

.bottom-placeholder {
  height: 160rpx;
}

// 底部添加栏
.add-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx $page-padding calc(env(safe-area-inset-bottom) + 16rpx);
  background-color: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(12px);
  box-shadow: 0 -4rpx 20rpx rgba(74, 85, 104, 0.08);

  .add-input {
    flex: 1;
    height: 80rpx;
    padding: 0 24rpx;
    font-size: $font-body;
    color: $text-primary;
    background-color: $bg-page;
    border-radius: $radius-full;
  }

  .add-btn {
    width: 80rpx;
    height: 80rpx;
    border-radius: $radius-full;
    background: linear-gradient(135deg, #0EA5E9, #38BDF8);
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 8rpx 18rpx rgba(14, 165, 233, 0.30);
    flex-shrink: 0;
    transition: transform 0.15s, opacity 0.15s;

    &.disabled {
      opacity: 0.4;
    }

    &:active {
      transform: scale(0.92);
    }
  }
}
</style>
