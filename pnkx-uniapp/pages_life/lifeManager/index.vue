<template>
  <view class="life-manager">
    <!-- Header -->
    <view class="lm-header">
      <view class="lm-header__bg">
        <view class="lm-header__decor lm-header__decor--circle"></view>
        <view class="lm-header__decor lm-header__decor--dot"></view>
      </view>
      <view class="lm-header__content">
        <text class="lm-header__title">生活管家</text>
        <text class="lm-header__desc">日历 · 菜谱 · 膳食 · 购物 · 订阅 · 报告</text>
      </view>
    </view>

    <!-- Entries（与 PC 端生活管家 6 项一致）-->
    <view class="lm-grid">
      <view class="lm-card" @click="navigate('/pages_life/calendar/index')">
        <view class="lm-card__icon lm-card__icon--calendar">
          <text class="lm-card__emoji">📅</text>
        </view>
        <view class="lm-card__body">
          <text class="lm-card__title">家庭日历</text>
          <text class="lm-card__desc">待办 / 纪念日 / 经期聚合</text>
        </view>
        <text class="lm-card__arrow">›</text>
      </view>

      <view class="lm-card" @click="navigate('/pages_life/recipe/index')">
        <view class="lm-card__icon lm-card__icon--recipe">
          <text class="lm-card__emoji">🍳</text>
        </view>
        <view class="lm-card__body">
          <text class="lm-card__title">菜谱库</text>
          <text class="lm-card__desc">管理你的拿手菜</text>
        </view>
        <text class="lm-card__arrow">›</text>
      </view>

      <view class="lm-card" @click="navigate('/pages_life/mealPlan/index')">
        <view class="lm-card__icon lm-card__icon--meal">
          <text class="lm-card__emoji">🥗</text>
        </view>
        <view class="lm-card__body">
          <text class="lm-card__title">膳食计划</text>
          <text class="lm-card__desc">规划每周三餐</text>
        </view>
        <text class="lm-card__arrow">›</text>
      </view>

      <view class="lm-card" @click="navigate('/pages_life/shoppingList/index')">
        <view class="lm-card__icon lm-card__icon--shopping">
          <text class="lm-card__emoji">🛒</text>
        </view>
        <view class="lm-card__body">
          <text class="lm-card__title">购物清单</text>
          <text class="lm-card__desc">买菜购物不遗漏</text>
        </view>
        <text class="lm-card__arrow">›</text>
      </view>

      <view class="lm-card" @click="navigate('/pages_life/subscription/index')">
        <view class="lm-card__icon lm-card__icon--sub">
          <text class="lm-card__emoji">💳</text>
        </view>
        <view class="lm-card__body">
          <text class="lm-card__title">订阅管理</text>
          <text class="lm-card__desc">周期出账 + 支出预测</text>
        </view>
        <text class="lm-card__arrow">›</text>
      </view>

      <view class="lm-card" @click="navigate('/pages_life/report/index')">
        <view class="lm-card__icon lm-card__icon--report">
          <text class="lm-card__emoji">📊</text>
        </view>
        <view class="lm-card__body">
          <text class="lm-card__title">生活报告</text>
          <text class="lm-card__desc">AI 周报 / 月报</text>
        </view>
        <text class="lm-card__arrow">›</text>
      </view>
    </view>

    <!-- Workflow hint -->
    <view class="lm-tip">
      <text class="lm-tip__text">💡 从菜谱加入膳食计划，一键生成购物清单</text>
    </view>
  </view>
</template>

<script>
export default {
  methods: {
    navigate(url) {
      uni.navigateTo({ url })
    }
  }
}
</script>

<style lang="scss" scoped>
.life-manager {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: $spacing-xl;
}

/* Header */
.lm-header {
  position: relative;
  background: linear-gradient(135deg, #34D399 0%, #22C55E 100%);
  padding: $spacing-xl $page-padding $spacing-2xl;
  overflow: hidden;

  &__bg {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    pointer-events: none;
  }

  &__decor {
    position: absolute;
    border-radius: 50%;
    opacity: 0.1;
    background: #fff;

    &--circle {
      width: 300rpx;
      height: 300rpx;
      top: -100rpx;
      right: -60rpx;
    }

    &--dot {
      width: 120rpx;
      height: 120rpx;
      bottom: 20rpx;
      left: 60rpx;
    }
  }

  &__content {
    position: relative;
    z-index: $z-base;
    display: flex;
    flex-direction: column;
  }

  &__title {
    font-size: $font-h1;
    color: #fff;
    font-weight: $font-weight-bold;
    line-height: $line-height-tight;
  }

  &__desc {
    font-size: $font-caption;
    color: rgba(255, 255, 255, 0.8);
    margin-top: $spacing-xs;
  }
}

/* Grid */
.lm-grid {
  margin: -$spacing-lg $page-padding 0;
  position: relative;
  z-index: $z-card;
}

.lm-card {
  display: flex;
  align-items: center;
  background: $bg-card;
  border-radius: $radius-lg;
  box-shadow: $shadow-card;
  padding: $spacing-md;
  margin-bottom: $spacing-sm;

  &:active {
    transform: scale(0.98);
  }

  &__icon {
    width: 88rpx;
    height: 88rpx;
    border-radius: $radius-xl;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    margin-right: $spacing-md;

    &--recipe {
      background: rgba(251, 146, 60, 0.12);
    }

    &--meal {
      background: rgba(52, 211, 153, 0.12);
    }

    &--shopping {
      background: rgba(91, 158, 238, 0.12);
    }

    &--calendar {
      background: rgba(108, 99, 255, 0.12);
    }

    &--sub {
      background: rgba(251, 191, 36, 0.12);
    }

    &--report {
      background: rgba(255, 159, 67, 0.12);
    }
  }

  &__emoji {
    font-size: 44rpx;
  }

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__title {
    font-size: $font-body;
    font-weight: $font-weight-semibold;
    color: $text-primary;
    display: block;
  }

  &__desc {
    font-size: $font-caption;
    color: $text-tertiary;
    margin-top: 4rpx;
    display: block;
  }

  &__arrow {
    font-size: 40rpx;
    color: $text-tertiary;
    margin-left: $spacing-sm;
  }
}

/* Tip */
.lm-tip {
  margin: $spacing-lg $page-padding 0;
  padding: $spacing-md;
  background: rgba(52, 211, 153, 0.08);
  border-radius: $radius-md;
  text-align: center;

  &__text {
    font-size: $font-caption;
    color: $text-secondary;
  }
}
</style>
