<template>
  <view class="life-manager subpage-shell">
    <!-- Header -->
    <view class="lm-header">
      <view class="lm-header__bg">
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
          <svg-icon icon-class="date" size="42rpx" />
        </view>
        <view class="lm-card__body">
          <text class="lm-card__title">家庭日历</text>
          <text class="lm-card__desc">待办 / 纪念日 / 经期聚合</text>
        </view>
        <text class="lm-card__arrow">›</text>
      </view>

      <view class="lm-card" @click="navigate('/pages_life/recipe/index')">
        <view class="lm-card__icon lm-card__icon--recipe">
          <svg-icon icon-class="c-huacai" size="42rpx" />
        </view>
        <view class="lm-card__body">
          <text class="lm-card__title">菜谱库</text>
          <text class="lm-card__desc">管理你的拿手菜</text>
        </view>
        <text class="lm-card__arrow">›</text>
      </view>

      <view class="lm-card" @click="navigate('/pages_life/mealPlan/index')">
        <view class="lm-card__icon lm-card__icon--meal">
          <svg-icon icon-class="qingdan" size="42rpx" />
        </view>
        <view class="lm-card__body">
          <text class="lm-card__title">膳食计划</text>
          <text class="lm-card__desc">规划每周三餐</text>
        </view>
        <text class="lm-card__arrow">›</text>
      </view>

      <view class="lm-card" @click="navigate('/pages_life/shoppingList/index')">
        <view class="lm-card__icon lm-card__icon--shopping">
          <svg-icon icon-class="shopping" size="42rpx" />
        </view>
        <view class="lm-card__body">
          <text class="lm-card__title">购物清单</text>
          <text class="lm-card__desc">买菜购物不遗漏</text>
        </view>
        <text class="lm-card__arrow">›</text>
      </view>

      <view class="lm-card" @click="navigate('/pages_life/subscription/index')">
        <view class="lm-card__icon lm-card__icon--sub">
          <svg-icon icon-class="finance" size="42rpx" />
        </view>
        <view class="lm-card__body">
          <text class="lm-card__title">订阅管理</text>
          <text class="lm-card__desc">周期出账 + 支出预测</text>
        </view>
        <text class="lm-card__arrow">›</text>
      </view>

      <view class="lm-card" @click="navigate('/pages_life/report/index')">
        <view class="lm-card__icon lm-card__icon--report">
          <svg-icon icon-class="chart" size="42rpx" />
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
      <svg-icon icon-class="ai" size="30rpx" />
      <text class="lm-tip__text">从菜谱加入膳食计划，一键生成购物清单</text>
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
  background: transparent;
  padding: 0 $page-padding $spacing-xl;
}

/* Header */
.lm-header {
  position: relative;
  margin: 24rpx 0 28rpx;
  padding: 34rpx 30rpx;
  background: rgba(255, 255, 255, 0.8);
  border: 1rpx solid rgba(255, 255, 255, 0.94);
  border-radius: $radius-2xl;
  box-shadow: $shadow-card;
  backdrop-filter: blur(28rpx);
  overflow: hidden;

  &__bg {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    pointer-events: none;
  }

  &__content {
    position: relative;
    z-index: $z-base;
    display: flex;
    flex-direction: column;
  }

  &__title {
    font-size: $font-h1;
    color: $text-primary;
    font-weight: $font-weight-bold;
    line-height: $line-height-tight;
  }

  &__desc {
    font-size: $font-caption;
    color: $text-secondary;
    margin-top: $spacing-xs;
  }
}

/* Grid */
.lm-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
  margin: 0;
  position: relative;
  z-index: $z-card;
}

.lm-card {
  position: relative;
  min-height: 208rpx;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  background: rgba(255, 255, 255, 0.86);
  border: 1rpx solid rgba(255, 255, 255, 0.94);
  border-radius: $radius-2xl;
  box-shadow: $shadow-card;
  padding: 26rpx;
  margin: 0;
  backdrop-filter: blur(24rpx);

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
    margin-right: 0;

    &--recipe {
      background: rgba(238, 231, 255, 0.74);
    }

    &--meal {
      background: rgba(221, 248, 255, 0.78);
    }

    &--shopping {
      background: rgba(221, 234, 255, 0.82);
    }

    &--calendar {
      background: rgba(238, 231, 255, 0.8);
    }

    &--sub {
      background: rgba(221, 248, 255, 0.76);
    }

    &--report {
      background: rgba(221, 234, 255, 0.82);
    }
  }

  &__body {
    flex: 1;
    min-width: 0;
    margin-top: 20rpx;
  }

  &__title {
    font-size: $font-h3;
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
    position: absolute;
    top: 26rpx;
    right: 24rpx;
    font-size: 34rpx;
    color: $text-tertiary;
    margin-left: $spacing-sm;
  }
}

/* Tip */
.lm-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  margin: 28rpx 0 0;
  padding: $spacing-md;
  color: $primary;
  background: rgba(255, 255, 255, 0.72);
  border: 1rpx solid rgba(255, 255, 255, 0.9);
  border-radius: $radius-xl;
  box-shadow: $shadow-sm;
  text-align: center;

  &__text {
    font-size: $font-caption;
    color: $text-secondary;
  }
}
</style>
