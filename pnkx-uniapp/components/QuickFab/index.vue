<template>
  <view class="qfab">
    <view v-if="expanded" class="qfab__overlay" @click="expanded = false" />
    <view class="qfab__wrap">
      <view v-if="expanded" class="qfab__menu">
        <view v-for="item in items" :key="item.key" class="qfab__item" @click="pick(item.key)">
          <text class="qfab__label">{{ item.label }}</text>
          <view class="qfab__icon" :class="'qfab__icon--' + item.key">
            <text v-if="item.key === 'ai'" class="qfab__icon-text">AI</text>
            <svg-icon v-else :icon-class="item.icon" size="34rpx" class-name="qfab__icon-text" />
          </view>
        </view>
      </view>
      <view class="qfab__btn" :class="{ active: expanded }" @click="expanded = !expanded">
        <text class="qfab__btn-text">{{ expanded ? '×' : '+' }}</text>
      </view>
    </view>
  </view>
</template>

<script>
/**
 * 快捷录入 FAB（PRODUCT_UIUE_REDESIGN §3.2 / §7.3）
 * 展开动效：主按钮 45° 旋转，子项自下而上弹出；点击后 emit('action', key)。
 * 今天页等高频页复用；AI 入口已由全局 AI 悬浮球承载时可通过 items 定制移除。
 */
export default {
  name: 'QuickFab',
  props: {
    items: {
      type: Array,
      default: () => [
        { key: 'ai', label: 'AI助手', icon: '' },
        { key: 'diary', label: '写日记', icon: 'edit' },
        { key: 'todo', label: '记待办', icon: 'daiban' },
        { key: 'bookkeeping', label: '记一笔', icon: 'jizhang' }
      ]
    }
  },
  data() { return { expanded: false } },
  methods: {
    pick(key) {
      this.expanded = false
      this.$emit('action', key)
    }
  }
}
</script>

<style scoped lang="scss">
.qfab__overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; z-index: 999998; }
.qfab__wrap {
  position: fixed; right: 30rpx; bottom: 180rpx; z-index: 999999;
  display: flex; flex-direction: column; align-items: flex-end;
}
.qfab__menu { display: flex; flex-direction: column; align-items: flex-end; margin-bottom: 20rpx; }
.qfab__item {
  display: flex; align-items: center; margin-bottom: 20rpx;
  animation: qfab-pop 0.3s $ease-spring both;
  &:nth-child(2) { animation-delay: 0.04s; }
  &:nth-child(3) { animation-delay: 0.08s; }
  &:nth-child(4) { animation-delay: 0.12s; }
  &:active { opacity: 0.7; }
}
@keyframes qfab-pop { from { opacity: 0; transform: translateY(24rpx) scale(0.6); } to { opacity: 1; transform: none; } }
.qfab__label {
  font-size: 26rpx; color: $text-primary; background: $theme-white;
  padding: 10rpx 20rpx; border-radius: 10rpx;
  box-shadow: $shadow-md; margin-right: 16rpx; white-space: nowrap;
}
.qfab__icon {
  width: 80rpx; height: 80rpx; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  background: var(--glass-strong);
  border: 1rpx solid var(--glass-strong);
  color: $primary;
  box-shadow: $shadow-md;
}
.qfab__icon-text { color: $primary; font-size: 28rpx; font-weight: bold; }
.qfab__btn {
  width: 100rpx; height: 100rpx; border-radius: 50%;
  background: linear-gradient(135deg, $primary-light 0%, $primary 55%, $diary 100%);
  box-shadow: 0 12rpx 30rpx rgba(79, 134, 247, 0.28);
  display: flex; align-items: center; justify-content: center;
  transition: transform 0.3s $ease-spring;
  &.active { transform: rotate(45deg); }
  &:active { transform: scale(0.95); }
}
.qfab__btn-text { color: $theme-white; font-size: 48rpx; font-weight: bold; line-height: 1; }
</style>
