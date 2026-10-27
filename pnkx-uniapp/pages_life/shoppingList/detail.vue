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

    <feature-gate feature="shoppingPlanV2"><shopping-checkout ref="checkout" :items="items" :list-id="listId" @changed="loadItems"/></feature-gate>
    <!-- Items:按履约状态分组 -->
    <view class="items" v-if="items.length > 0">
      <template v-for="(meta, key) in visibleGroups" :key="key">
        <view class="group-head">
          <view class="group-dot" :class="'group-dot--' + key"></view>
          <text class="group-title">{{ meta.label }}</text>
          <text class="group-count">{{ groups[key].length }}</text>
          <text
            v-if="key === 'settled'"
            class="group-toggle"
            @click="showSettled = !showSettled"
            >{{ showSettled ? "收起" : "展开" }}</text
          >
        </view>
        <view
          class="item"
          v-for="item in key === 'settled' && !showSettled ? [] : groups[key]"
          :key="item.id"
          :class="{ 'item--done': key !== 'needed' && key !== 'available' }"
        >
          <!-- 待购:勾选即买到 -->
          <view v-if="key === 'needed'" class="item__check" @click="toggle(item)">
            <view class="item__checkbox"></view>
          </view>
          <!-- 已买到:结算选择(功能开启时);未开启沿用勾选切换 -->
          <view
            v-else-if="key === 'purchased'"
            class="item__check"
            @click="checkoutEnabled ? toggleSelect(item.id) : toggle(item)"
          >
            <view
              class="item__checkbox"
              :class="{
                'item__checkbox--on': checkoutEnabled ? selectedIds.includes(item.id) : true,
              }"
            >
              <text v-if="checkoutEnabled ? selectedIds.includes(item.id) : true" class="item__tick">✓</text>
            </view>
          </view>
          <view v-else class="item__check"></view>
          <text class="item__name">{{ item.name }}</text>
          <text v-if="item.quantity" class="item__qty">{{ item.quantity }}</text>
          <feature-gate feature="shoppingPlanV2"><shopping-item-actions :item="item" @changed="loadItems"/></feature-gate>
          <view v-if="key !== 'settled'" class="item__del" @click="handleDelete(item)">
            <text class="item__del-icon">×</text>
          </view>
        </view>
      </template>
    </view>

    <!-- Empty -->
    <view class="empty" v-else>
      <text class="empty__emoji">📝</text>
      <text class="empty__text">清单还是空的</text>
      <text class="empty__hint">在上方输入要买的东西</text>
    </view>

    <!-- 底部工具栏:已买到可勾选结算,已结算不进入 -->
    <view class="footer" v-if="checkoutEnabled && groups.purchased.length">
      <view class="footer__info">
        <text class="footer__count">已选 {{ selectedIds.length }} 项</text>
        <text class="footer__sub">共 {{ groups.purchased.length }} 项已买到待结算</text>
      </view>
      <text class="footer__link" @click="allPurchasedSelected ? clearSelection() : selectAllPurchased()">{{
        allPurchasedSelected ? "取消全选" : "全选"
      }}</text>
      <view class="footer__btn footer__btn--primary" :class="{ 'footer__btn--disabled': !selectedIds.length }" @click="checkoutNow">
        <text class="footer__btn-text">去结算</text>
      </view>
      <view class="safe-bottom"></view>
    </view>
    <view class="footer" v-else-if="doneCount > 0">
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
import ShoppingCheckout from "@/components/ShoppingCheckout/index.vue"
import ShoppingItemActions from "@/components/ShoppingItemActions/index.vue"
import { groupItems, groupMeta } from "@/utils/shoppingGroups"
import { getFeatureCapabilities } from "@/utils/featureCapabilities"
import {
  listShoppingItem,
  addShoppingItem,
  updateShoppingItem,
  delShoppingItem,
  clearChecked
} from '@/api/px/life/shoppingList'

export default {
 components:{ShoppingCheckout,ShoppingItemActions},
  data() {
    return {
      listId: null,
      items: [],
      newItemName: '',
      // 结算选择:仅"已买到且未结算"可选,已结算不进入
      checkoutEnabled: false,
      selectedIds: [],
      showSettled: false
    }
  },
  computed: {
    doneCount() {
      return this.items.filter(i => i.checked).length
    },
    groups() {
      return groupItems(this.items)
    },
    // 已结算默认折叠
    visibleGroups() {
      const keys = ['needed', 'available', 'purchased']
      if (this.groups.settled.length) keys.push('settled')
      return Object.fromEntries(keys.filter(k => this.groups[k].length).map(k => [k, groupMeta[k]]))
    },
    allPurchasedSelected() {
      return this.groups.purchased.length > 0 && this.selectedIds.length === this.groups.purchased.length
    }
  },
  onLoad(options) {
    this.listId = options.id
    if (options.name) {
      uni.setNavigationBarTitle({ title: decodeURIComponent(options.name) })
    }
    this.loadItems()
    this.refreshCheckoutEnabled()
  },
  methods: {
    async loadItems() {
      const res = await listShoppingItem(this.listId)
      this.items = res.rows || res.data || []
      this.pruneSelection()
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
        // 与履约状态投影保持一致,分组立即生效
        item.fulfillmentState = item.checked ? 'purchased' : 'needed'
        this.pruneSelection()
      } catch (e) {
        uni.showToast({ title: '操作失败', icon: 'none' })
      }
    },
    toggleSelect(id) {
      this.selectedIds = this.selectedIds.includes(id)
        ? this.selectedIds.filter(x => x !== id)
        : [...this.selectedIds, id]
    },
    selectAllPurchased() {
      this.selectedIds = this.groups.purchased.map(i => i.id)
    },
    clearSelection() {
      this.selectedIds = []
    },
    pruneSelection() {
      const valid = new Set(this.groups.purchased.map(i => i.id))
      this.selectedIds = this.selectedIds.filter(id => valid.has(id))
    },
    checkoutNow() {
      if (!this.selectedIds.length) return
      this.$refs.checkout?.openWith(this.selectedIds)
    },
    async refreshCheckoutEnabled() {
      try {
        const capabilities = await getFeatureCapabilities()
        this.checkoutEnabled = !!capabilities.shoppingPlanV2
      } catch (e) {
        this.checkoutEnabled = false
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
/* 分组头 */
.group-head {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 20rpx 8rpx 10rpx;
}
.group-title {
  font-size: 26rpx;
  font-weight: 600;
  color: $text-primary;
}
.group-count {
  min-width: 34rpx;
  padding: 0 10rpx;
  font-size: 20rpx;
  line-height: 32rpx;
  text-align: center;
  color: $text-secondary;
  background: $bg-page;
  border-radius: 999rpx;
}
.group-toggle {
  font-size: 22rpx;
  color: #4f86f7;
}
.group-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
}
.group-dot--needed {
  background: #4f86f7;
}
.group-dot--available {
  background: #2fbf71;
}
.group-dot--purchased {
  background: #f5a623;
}
.group-dot--settled {
  background: #94a3b8;
}

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
    display: block;
    font-size: $font-body;
    font-weight: 600;
    color: $text-primary;
  }

  &__sub {
    display: block;
    margin-top: 4rpx;
    font-size: $font-caption;
    color: $text-tertiary;
  }

  &__link {
    font-size: $font-caption;
    color: #4f86f7;
    margin-right: $spacing-md;
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

  &__btn--primary {
    background: #4f86f7;
  }

  &__btn--disabled {
    opacity: 0.5;
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
