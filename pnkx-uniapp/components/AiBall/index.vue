<template>
  <view class="ai-ball" :class="[side === 'left' ? 'ai-ball--left' : '', { breathing }]" :style="posStyle" @click="open" hover-class="ai-ball--press" :hover-stay-time="120">
    <text class="ai-ball__glyph">✦</text>
  </view>
</template>

<script>
/**
 * 全局 AI 悬浮球（PRODUCT_UIUE_REDESIGN §3.3）
 * 常驻 今天/生活/我的 三个 Tab；资料 Tab 由底部工具条承载问答，不放球（避免双悬浮）。
 * 点击进入统一 AI 助手会话页。
 */
export default {
  name: 'AiBall',
  props: {
    // 左右侧（首页右下已有快捷录入 FAB，AI 球放左侧避免双悬浮重叠）
    side: { type: String, default: 'right' },
    // 距底部偏移（默认避开原生 tabBar + 安全区）
    bottom: { type: String, default: '200rpx' },
    // 是否显示呼吸动效（首页等安静场景可关）
    breathing: { type: Boolean, default: true }
  },
  computed: {
    posStyle() {
      return { bottom: this.bottom, [this.side === 'left' ? 'left' : 'right']: '30rpx' }
    }
  },
  methods: {
    open() {
      uni.navigateTo({ url: '/pages_life/ai/chat', animationType: 'slide-in-bottom' })
    }
  }
}
</script>

<style lang="scss" scoped>
.ai-ball {
  position: fixed;
  right: 30rpx;
  bottom: 200rpx;
  z-index: 90;
  width: 92rpx;
  height: 92rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, $diary, $primary);
  box-shadow: 0 12rpx 32rpx rgba(79, 134, 247, 0.42);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.22s $ease-spring;
  &--left {
    right: auto;
    left: 30rpx;
  }
  /* 呼吸光环：低频、克制 */
  &.breathing::after {
    content: '';
    position: absolute;
    inset: -8rpx;
    border-radius: 50%;
    border: 2rpx solid rgba(167, 139, 250, 0.5);
    animation: ai-ball-ping 2.8s $ease-out infinite;
    pointer-events: none;
  }
  &--press {
    transform: scale(0.88);
  }
  &__glyph {
    color: $theme-white;
    font-size: 40rpx;
    line-height: 1;
  }
}
@keyframes ai-ball-ping {
  0% { transform: scale(0.9); opacity: 0.7; }
  70%, 100% { transform: scale(1.45); opacity: 0; }
}
</style>
