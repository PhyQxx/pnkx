<template>
  <view class="detail-page subpage-shell">
    <!-- Add input -->
    <view class="add-bar">
      <input
        class="add-bar__input"
        v-model="newItemName"
        placeholder="添加购物项…"
        confirm-type="done"
        @confirm="handleAdd"
      />
      <view class="add-bar__btn" @click="handleAdd">
        <text class="add-bar__btn-text">添加</text>
      </view>
    </view>

    <!-- Items -->
    <view class="items" v-if="items.length > 0">
      <view
        class="item"
        v-for="item in items"
        :key="item.id"
        :class="{ 'item--done': item.checked }"
      >
        <view class="item__check" @click="toggle(item)">
          <view class="item__checkbox" :class="{ 'item__checkbox--on': item.checked }">
            <text v-if="item.checked" class="item__tick">✓</text>
          </view>
        </view>
        <text class="item__name">{{ item.name }}</text>
        <text v-if="item.quantity" class="item__qty">{{ item.quantity }}</text>
        <view class="item__del" @click="handleDelete(item)">
          <text class="item__del-icon">×</text>
        </view>
      </view>
    </view>

    <!-- Empty -->
    <view class="empty" v-else>
      <text class="empty__emoji">📝</text>
      <text class="empty__text">清单还是空的</text>
      <text class="empty__hint">在上方输入要买的东西</text>
    </view>

    <!-- Footer -->
    <view class="footer" v-if="doneCount > 0">
      <view class="footer__info">
        <text class="footer__count">已完成 {{ doneCount }}/{{ items.length }}</text>
      </view>
      <view class="footer__btn" @click="handleClearChecked">
        <text class="footer__btn-text">清空已勾选</text>
      </view>
      <view class="safe-bottom"></view>
    </view>
  </view>
</template>

<script>
import {
  listShoppingItem,
  addShoppingItem,
  updateShoppingItem,
  delShoppingItem,
  clearChecked
} from '@/api/px/life/shoppingList'

export default {
  data() {
    return {
      listId: null,
      items: [],
      newItemName: ''
    }
  },
  computed: {
    doneCount() {
      return this.items.filter(i => i.checked).length
    }
  },
  onLoad(options) {
    this.listId = options.id
    if (options.name) {
      uni.setNavigationBarTitle({ title: decodeURIComponent(options.name) })
    }
    this.loadItems()
  },
  methods: {
    async loadItems() {
      const res = await listShoppingItem(this.listId)
      this.items = res.rows || res.data || []
      // 按勾选状态排序：未勾选在前
      this.items.sort((a, b) => {
        if (a.checked === b.checked) return 0
        return a.checked ? 1 : -1
      })
    },
    async handleAdd() {
      const name = this.newItemName.trim()
      if (!name) return
      try {
        await addShoppingItem({
          listId: this.listId,
          name: name,
          checked: false
        })
        this.newItemName = ''
        this.loadItems()
      } catch (e) {
        uni.showToast({ title: '添加失败', icon: 'none' })
      }
    },
    async toggle(item) {
      try {
        await updateShoppingItem({
          id: item.id,
          listId: this.listId,
          name: item.name,
          checked: !item.checked
        })
        item.checked = !item.checked
        // 重新排序
        this.items.sort((a, b) => {
          if (a.checked === b.checked) return 0
          return a.checked ? 1 : -1
        })
      } catch (e) {
        uni.showToast({ title: '操作失败', icon: 'none' })
      }
    },
    handleDelete(item) {
      uni.showModal({
        title: '提示',
        content: '删除"' + item.name + '"？',
        success: async (res) => {
          if (!res.confirm) return
          await delShoppingItem(item.id)
          this.items = this.items.filter(i => i.id !== item.id)
        }
      })
    },
    handleClearChecked() {
      uni.showModal({
        title: '提示',
        content: '确认清空所有已勾选的项目？',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await clearChecked(this.listId)
            this.loadItems()
            uni.showToast({ title: '已清空', icon: 'none' })
          } catch (e) {
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
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 180rpx;
}

/* Add bar */
.add-bar {
  display: flex;
  align-items: center;
  padding: $spacing-md $page-padding;
  background: $bg-card;
  box-shadow: $shadow-card;

  &__input {
    flex: 1;
    height: 72rpx;
    background: $bg-page;
    border-radius: $radius-md;
    padding: 0 $spacing-md;
    font-size: $font-body;
  }

  &__btn {
    margin-left: $spacing-sm;
    padding: 0 $spacing-lg;
    height: 72rpx;
    line-height: 72rpx;
    background: linear-gradient(135deg, $primary, $primary-dark);
    border-radius: $radius-md;

    &:active {
      opacity: 0.85;
    }
  }

  &__btn-text {
    color: #fff;
    font-size: $font-body;
  }
}

/* Items */
.items {
  padding: $spacing-md $page-padding;
}

.item {
  display: flex;
  align-items: center;
  background: $bg-card;
  border-radius: $radius-md;
  box-shadow: $shadow-xs;
  padding: $spacing-md;
  margin-bottom: $spacing-sm;

  &--done {
    opacity: 0.55;

    .item__name {
      text-decoration: line-through;
      color: $text-tertiary;
    }
  }

  &__check {
    margin-right: $spacing-md;
    flex-shrink: 0;
  }

  &__checkbox {
    width: 40rpx;
    height: 40rpx;
    border-radius: 50%;
    border: 3rpx solid $gray-300;
    background: $bg-card;
    display: flex;
    align-items: center;
    justify-content: center;

    &--on {
      background: $primary;
      border-color: $primary;
    }
  }

  &__tick {
    color: #fff;
    font-size: 24rpx;
    font-weight: bold;
  }

  &__name {
    flex: 1;
    font-size: $font-body;
    color: $text-primary;
  }

  &__qty {
    font-size: $font-caption;
    color: $text-tertiary;
    margin-right: $spacing-sm;
  }

  &__del {
    width: 48rpx;
    height: 48rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &__del-icon {
    font-size: 36rpx;
    color: $text-tertiary;
    line-height: 1;
  }
}

/* Empty */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 160rpx;

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

/* Footer */
.footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: $bg-card;
  padding: $spacing-md $page-padding 0;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;

  &__info {
    flex: 1;
  }

  &__count {
    font-size: $font-caption;
    color: $text-tertiary;
  }

  &__btn {
    padding: 0 $spacing-lg;
    height: 72rpx;
    line-height: 72rpx;
    background: $danger;
    border-radius: $radius-md;

    &:active {
      opacity: 0.85;
    }
  }

  &__btn-text {
    color: #fff;
    font-size: $font-body;
  }
}

.safe-bottom {
  height: env(safe-area-inset-bottom);
  width: 100%;
  margin-top: $spacing-sm;
}
</style>
