<template>
  <view class="bk-tabs">
    <view
      v-for="tab in tabs"
      :key="tab.value"
      class="bk-tabs__item"
      :class="{ active: active === tab.value }"
      @click="$emit('change', tab.value)"
    >
      {{ tab.label }}
    </view>
    <view class="bk-tabs__indicator" :style="indicatorStyle"></view>
  </view>
</template>

<script>
/** 记账类型标签（支出/收入/转账），自 add.vue 拆出；滑动指示条随选中项移动 */
export default {
  name: 'TypeTabs',
  props: {
    tabs: { type: Array, required: true },
    active: { type: String, default: '1' }
  },
  computed: {
    indicatorStyle() {
      const index = this.tabs.findIndex(tab => tab.value === this.active)
      const step = 100 / this.tabs.length
      return `left: ${index * step}%;`
    }
  }
}
</script>

<style scoped lang="scss">
.bk-tabs {
  display: flex;
  background: transparent;
  padding: 0 $spacing-sm;
  position: relative;
  flex: 1;

  &__item {
    flex: 1;
    text-align: center;
    padding: $spacing-sm 0;
    font-size: $font-body;
    color: $text-secondary;
    position: relative;
    transition: color $duration-normal $ease-default;

    &.active {
      color: $primary-dark;
      font-weight: $font-weight-medium;
    }
  }

  &__indicator {
    position: absolute;
    bottom: 0;
    left: 0;
    width: calc(100% / 3);
    height: $spacing-2xs;
    background-color: $primary;
    transition: left $duration-normal $ease-default;
  }
}
</style>

@media screen and (max-height: 760px) {
  .bk-tabs__item { padding: 10rpx 0; }
}
