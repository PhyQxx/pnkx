<template>
  <view class="sub-page">
    <!-- 预测汇总 -->
    <view class="sub-summary" v-if="forecast.monthlyTotal !== undefined">
      <view class="sub-summary__card sub-summary__card--month">
        <text class="sub-summary__label">月度支出预测</text>
        <text class="sub-summary__value">¥{{ forecast.monthlyTotal }}</text>
        <text class="sub-summary__sub">{{ forecast.count }} 项订阅</text>
      </view>
      <view class="sub-summary__card sub-summary__card--year">
        <text class="sub-summary__label">年度支出预测</text>
        <text class="sub-summary__value">¥{{ forecast.yearlyTotal }}</text>
      </view>
    </view>

    <!-- 订阅列表 -->
    <view class="sub-list">
      <view class="sub-list__title">
        <text>我的订阅</text>
        <text class="sub-list__count">{{ list.length }}项</text>
      </view>

      <view v-if="loading" class="sub-loading">加载中…</view>

      <view v-else-if="list.length === 0" class="sub-empty">
        <text class="sub-empty__emoji">📭</text>
        <text class="sub-empty__text">暂无订阅</text>
        <text class="sub-empty__hint">点击下方按钮添加</text>
      </view>

      <view v-else>
        <view
          class="sub-item"
          v-for="item in list"
          :key="item.id"
          :class="{ 'sub-item--disabled': !item.enabled }"
        >
          <view class="sub-item__icon" :style="{ background: iconBg(item) }">
            <text class="sub-item__emoji">{{ item.logo || '💳' }}</text>
          </view>
          <view class="sub-item__body">
            <view class="sub-item__top">
              <text class="sub-item__name">{{ item.name }}</text>
              <text class="sub-item__amount">¥{{ item.amount }}</text>
            </view>
            <view class="sub-item__meta">
              <text class="sub-item__cycle">{{ cycleLabel(item) }}</text>
              <text v-if="item.nextPaymentDate" class="sub-item__next">
                下次：{{ formatTime(item.nextPaymentDate) }}
              </text>
            </view>
          </view>
          <view class="sub-item__actions">
            <view class="sub-item__toggle" @click="toggleEnabled(item)">
              <text class="sub-item__toggle-text">{{ item.enabled ? '停用' : '启用' }}</text>
            </view>
            <view class="sub-item__del" @click="handleDelete(item)">
              <text class="sub-item__del-icon">×</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 新增按钮 -->
    <view class="sub-fab" @click="showAdd = true">
      <text class="sub-fab__text">+</text>
    </view>

    <!-- 新增弹框 -->
    <uni-popup ref="addPopup" type="bottom" v-if="showAdd" @change="onPopupChange">
      <view class="sub-popup">
        <text class="sub-popup__title">添加订阅</text>
        <view class="sub-popup__form">
          <input class="sub-popup__input" v-model="form.name" placeholder="订阅名称（如 Netflix）" />
          <input class="sub-popup__input" v-model.number="form.amount" type="digit" placeholder="金额" />
          <view class="sub-popup__row">
            <picker class="sub-popup__picker" :range="cycleOptions" range-key="label" @change="onCycleChange">
              <view class="sub-popup__picker-text">{{ cycleLabel(form) || '选择周期' }}</view>
            </picker>
            <input class="sub-popup__input sub-popup__input--half" v-model.number="form.cycleInterval" type="number" placeholder="间隔" />
          </view>
          <picker class="sub-popup__picker" mode="date" @change="onDateChange">
            <view class="sub-popup__picker-text">{{ form.nextPaymentDate || '选择下次扣款日期' }}</view>
          </picker>
        </view>
        <view class="sub-popup__actions">
          <view class="sub-popup__btn sub-popup__btn--ghost" @click="showAdd = false">取消</view>
          <view class="sub-popup__btn sub-popup__btn--primary" @click="handleAdd">添加</view>
        </view>
      </view>
    </uni-popup>

    <view class="safe-bottom"></view>
  </view>
</template>

<script>
import { listSubscription, addSubscription, updateSubscription, delSubscription, forecast } from '@/api/px/life/subscription'

export default {
  data() {
    return {
      list: [],
      loading: true,
      forecast: {},
      showAdd: false,
      form: { name: '', amount: 0, cycle: 'monthly', cycleInterval: 1, nextPaymentDate: '' },
      cycleOptions: [
        { label: '每月', value: 'monthly' },
        { label: '每年', value: 'yearly' },
        { label: '每周', value: 'weekly' },
        { label: '每天', value: 'daily' }
      ]
    }
  },
  onLoad() {
    this.loadData()
  },
  onShow() {
    if (!this.loading) this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const [listRes, fcRes] = await Promise.all([
          listSubscription({ pageNum: 1, pageSize: 100 }),
          forecast()
        ])
        this.list = listRes.rows || []
        this.forecast = fcRes.data || {}
      } catch (e) {
        uni.showToast({ title: '加载失败', icon: 'none' })
      }
      this.loading = false
    },
    cycleLabel(item) {
      const interval = item.cycleInterval || 1
      const map = { monthly: '月', yearly: '年', weekly: '周', daily: '天' }
      const unit = map[item.cycle] || item.cycle
      return interval === 1 ? '每' + unit : '每' + interval + unit
    },
    formatTime(t) {
      if (!t) return ''
      return String(t).replace('T', ' ').substring(0, 10)
    },
    iconBg(item) {
      const bgs = ['rgba(91,158,238,0.12)', 'rgba(52,211,153,0.12)', 'rgba(251,191,36,0.12)', 'rgba(255,159,67,0.12)']
      return bgs[(item.id || 0) % bgs.length]
    },
    async toggleEnabled(item) {
      try {
        await updateSubscription({ id: item.id, enabled: !item.enabled })
        item.enabled = !item.enabled
        this.loadData()
      } catch (e) {
        uni.showToast({ title: '操作失败', icon: 'none' })
      }
    },
    handleDelete(item) {
      uni.showModal({
        title: '提示',
        content: '确认删除"' + item.name + '"？',
        success: async (res) => {
          if (!res.confirm) return
          await delSubscription(item.id)
          this.loadData()
          uni.showToast({ title: '已删除', icon: 'none' })
        }
      })
    },
    onCycleChange(e) {
      this.form.cycle = this.cycleOptions[e.detail.value].value
    },
    onDateChange(e) {
      this.form.nextPaymentDate = e.detail.value
    },
    async handleAdd() {
      if (!this.form.name.trim()) {
        uni.showToast({ title: '请输入名称', icon: 'none' })
        return
      }
      try {
        await addSubscription({
          ...this.form,
          name: this.form.name.trim(),
          enabled: true
        })
        this.showAdd = false
        this.form = { name: '', amount: 0, cycle: 'monthly', cycleInterval: 1, nextPaymentDate: '' }
        this.loadData()
        uni.showToast({ title: '已添加', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: '添加失败', icon: 'none' })
      }
    },
    onPopupChange(e) {
      if (!e.show) this.showAdd = false
    }
  }
}
</script>

<style lang="scss" scoped>
.sub-page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 160rpx;
}

/* Summary */
.sub-summary {
  display: flex;
  gap: $spacing-sm;
  padding: $spacing-md $page-padding;
}

.sub-summary__card {
  flex: 1;
  border-radius: $radius-lg;
  padding: $spacing-md;
  box-shadow: $shadow-card;

  &--month {
    background: linear-gradient(135deg, $primary, $primary-dark);
    color: #fff;
  }

  &--year {
    background: $bg-card;
  }

  .sub-summary__label {
    font-size: $font-caption;
    opacity: 0.85;
    display: block;
  }

  .sub-summary__value {
    font-size: $font-h2;
    font-weight: $font-weight-bold;
    display: block;
    margin: $spacing-xs 0;
  }

  .sub-summary__sub {
    font-size: $font-mini;
    opacity: 0.75;
  }
}

/* List */
.sub-list {
  margin: 0 $page-padding;
  background: $bg-card;
  border-radius: $radius-lg;
  box-shadow: $shadow-card;
  padding: $spacing-md;

  &__title {
    font-size: $font-body;
    font-weight: $font-weight-semibold;
    color: $text-primary;
    margin-bottom: $spacing-sm;
    display: flex;
    justify-content: space-between;
  }

  &__count {
    font-size: $font-caption;
    color: $text-tertiary;
    font-weight: normal;
  }
}

.sub-loading {
  padding: 80rpx 0;
  text-align: center;
  color: $text-tertiary;
  font-size: $font-caption;
}

.sub-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 0;

  &__emoji {
    font-size: 72rpx;
    opacity: 0.5;
    margin-bottom: $spacing-sm;
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

.sub-item {
  display: flex;
  align-items: center;
  padding: $spacing-sm 0;
  border-bottom: 2rpx solid $gray-100;

  &:last-child { border-bottom: none; }

  &--disabled { opacity: 0.5; }

  &__icon {
    width: 72rpx;
    height: 72rpx;
    border-radius: $radius-md;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: $spacing-md;
    flex-shrink: 0;
  }

  &__emoji { font-size: 36rpx; }

  &__body { flex: 1; min-width: 0; }

  &__top {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  &__name {
    font-size: $font-body;
    color: $text-primary;
    font-weight: $font-weight-medium;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
  }

  &__amount {
    font-size: $font-body;
    color: $danger;
    font-weight: $font-weight-semibold;
    margin-left: $spacing-sm;
  }

  &__meta {
    margin-top: 4rpx;
    display: flex;
    gap: $spacing-sm;
  }

  &__cycle, &__next {
    font-size: $font-mini;
    color: $text-tertiary;
  }

  &__actions {
    display: flex;
    align-items: center;
    margin-left: $spacing-sm;
    gap: $spacing-xs;
  }

  &__toggle {
    padding: $spacing-xs $spacing-sm;
    background: $bg-page;
    border-radius: $radius-md;
  }

  &__toggle-text {
    font-size: $font-mini;
    color: $primary;
  }

  &__del {
    width: 48rpx;
    height: 48rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__del-icon {
    font-size: 32rpx;
    color: $text-tertiary;
  }
}

/* FAB */
.sub-fab {
  position: fixed;
  right: 40rpx;
  bottom: 60rpx;
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: linear-gradient(360deg, $primary 0%, $primary-light 100%);
  box-shadow: 0 4rpx 16rpx rgba($primary, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99;

  &:active { transform: scale(0.95); }

  &__text {
    color: #fff;
    font-size: 56rpx;
    font-weight: 300;
    line-height: 1;
  }
}

/* Popup */
.sub-popup {
  background: $bg-card;
  border-radius: $radius-lg $radius-lg 0 0;
  padding: $spacing-lg $page-padding;
  padding-bottom: calc(#{$spacing-lg} + env(safe-area-inset-bottom));

  &__title {
    font-size: $font-h3;
    font-weight: $font-weight-semibold;
    text-align: center;
    display: block;
    margin-bottom: $spacing-md;
  }

  &__form > * { margin-bottom: $spacing-sm; }

  &__input {
    width: 100%;
    height: 80rpx;
    background: $bg-page;
    border-radius: $radius-md;
    padding: 0 $spacing-md;
    font-size: $font-body;
    box-sizing: border-box;
  }

  &__row { display: flex; gap: $spacing-sm; }

  &__input--half { flex: 1; }

  &__picker {
    flex: 1;
  }

  &__picker-text {
    height: 80rpx;
    line-height: 80rpx;
    background: $bg-page;
    border-radius: $radius-md;
    padding: 0 $spacing-md;
    font-size: $font-body;
    color: $text-primary;
  }

  &__actions {
    display: flex;
    gap: $spacing-sm;
    margin-top: $spacing-md;
  }

  &__btn {
    flex: 1;
    height: 80rpx;
    line-height: 80rpx;
    text-align: center;
    border-radius: $radius-md;
    font-size: $font-body;

    &--ghost {
      background: $bg-page;
      color: $text-secondary;
    }

    &--primary {
      background: $primary;
      color: #fff;
    }
  }
}

.safe-bottom { height: env(safe-area-inset-bottom); }
</style>
