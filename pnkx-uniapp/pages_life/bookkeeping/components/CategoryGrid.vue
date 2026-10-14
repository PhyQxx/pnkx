<template>
  <view class="bk-cat">
    <view class="bk-cat__grid">
      <view
        v-for="(category, index) in categories"
        :key="category.id"
        class="bk-cat__item"
        :class="{ selected: selectedKey === category.id || selectedKey === category.typeName }"
        @click="$emit('select', category)"
      >
        <view class="bk-cat__icon" :style="{ backgroundColor: color(index) }">
          <svg-icon :icon-class="category.typeIcon" size="32rpx" />
        </view>
        <view class="bk-cat__label">{{ category.typeName }}</view>
        <view v-if="category.children && category.children.length > 0" class="bk-cat__arrow"></view>
      </view>
    </view>
  </view>
</template>

<script>
/** 分类图标宫格（一级分类），自 add.vue 拆出；色板由页面传入保持配置一致 */
export default {
  name: 'CategoryGrid',
  props: {
    categories: { type: Array, default: () => [] },
    colors: { type: Array, default: () => [] },
    selectedKey: { type: [String, Number], default: '' }
  },
  methods: {
    color(index) { return this.colors.length ? this.colors[index % this.colors.length] : '' }
  }
}
</script>

<style scoped lang="scss">
.bk-cat {
  margin: 20rpx $page-padding 0;
  padding: 22rpx 20rpx;
  background: rgba(255, 255, 255, 0.86);
  border: 1rpx solid rgba(255, 255, 255, 0.94);
  border-radius: $radius-2xl;
  box-shadow: $shadow-card;
  backdrop-filter: blur(24rpx);
  flex-shrink: 0;

  &__grid {
    display: flex;
    flex-wrap: wrap;
    gap: 10rpx;
    justify-content: center;
  }

  &__item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: calc(25% - 8rpx);
    padding: 10rpx 4rpx;
    border-radius: $radius-md;
    background: transparent;
    position: relative;
    transition: all $duration-normal $ease-spring;

    &:active { transform: scale(0.95); }

    &.selected {
      background: rgba(79, 134, 247, 0.1);
      box-shadow: inset 0 0 0 1rpx rgba(79, 134, 247, 0.18);
      .bk-cat__label {
        color: $primary-dark;
        font-weight: $font-weight-medium;
      }
    }
  }

  &__icon {
    width: 70rpx;
    height: 70rpx;
    border-radius: $radius-full;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: $spacing-2xs;
    box-shadow: none;
  }

  &__label {
    font-size: $font-small;
    color: $text-primary;
    text-align: center;
    transition: color $duration-fast $ease-default;
  }

  &__arrow {
    position: absolute;
    top: $spacing-2xs;
    right: $spacing-2xs;
    width: $spacing-md;
    height: $spacing-md;
    border-radius: $radius-full;
    background-color: $gray-300;
    display: flex;
    align-items: center;
    justify-content: center;

    &::after {
      content: '';
      width: 0;
      height: 0;
      border-left: $spacing-2xs solid transparent;
      border-right: $spacing-2xs solid transparent;
      border-top: $spacing-xs solid $bg-card;
      margin-top: $spacing-2xs;
    }
  }
}
</style>

@media screen and (max-height: 760px) {
  .bk-cat {
    margin-top: 10rpx;
    padding: 12rpx 14rpx;
  }
  .bk-cat__grid { gap: 4rpx 8rpx; }
  .bk-cat__item { padding: 4rpx 2rpx; }
  .bk-cat__icon { width: 58rpx; height: 58rpx; }
  .bk-cat__label { font-size: 20rpx; }
}
