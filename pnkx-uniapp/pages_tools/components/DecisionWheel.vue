<!--
 * @File: DecisionWheel
 * @Author: PHY
 * @Description: 决策转盘组件（今天吃什么等场景通用）
 * 采用 CSS conic-gradient 绘制扇区（H5 / 小程序 / App webview 均支持），
 * 旋转结果通过随机目标扇区反推旋转角度，保证指针指向与结果一致
 *
 * 用法：
 *   <DecisionWheel ref="wheel" :options="foods" @result="onResult" />
 *   this.$refs.wheel.spin() 触发旋转；@result 回调 { index, name }
-->
<template>
  <view class="wheel-box" :style="{ width: sizeRpx + 'rpx', height: sizeRpx + 'rpx' }">
    <!-- 指针 -->
    <view class="wheel-pointer"></view>
    <!-- 转盘盘面 -->
    <view
      class="wheel"
      :style="wheelStyle"
    >
      <view
        v-for="(seg, index) in segments"
        :key="index + '-' + seg.name"
        class="wheel-label"
        :style="{ transform: 'rotate(' + seg.angle + 'deg)' }"
      >
        <text class="wheel-label__text">{{ seg.name }}</text>
      </view>
    </view>
    <!-- 中心按钮 -->
    <view class="wheel-hub" @tap="spin">
      <text class="wheel-hub__text">{{ spinning ? '…' : '转' }}</text>
    </view>
  </view>
</template>

<script>
/** 默认扇区配色（对齐 uni.scss 功能色） */
const DEFAULT_COLORS = [
  '#6C9EFF', '#A78BFA', '#34D399', '#FB923C', '#F472B6',
  '#38BDF8', '#FBBF24', '#4ADE80', '#F87171', '#C084FC'
]

/** 旋转圈数（多转几圈更有仪式感） */
const SPIN_TURNS = 5
/** 旋转动画时长（ms），结束后回传结果 */
const SPIN_DURATION = 4200
/** 结果回传兜底延时（部分端无 transitionend，统一用定时器） */
const SPIN_TIMER = 4350

export default {
  name: 'DecisionWheel',
  props: {
    /** 选项列表（2–10 个） */
    options: {
      type: Array,
      default: () => []
    },
    /** 转盘尺寸（rpx） */
    size: {
      type: Number,
      default: 600
    },
    /** 自定义扇区配色 */
    colors: {
      type: Array,
      default: () => DEFAULT_COLORS
    }
  },
  data() {
    return {
      // 累计旋转角度（跨次累加，保证每次都向前转）
      rotation: 0,
      spinning: false
    }
  },
  computed: {
    /**
     * 转盘尺寸（rpx 数值，供 style 绑定）
     */
    sizeRpx() {
      return this.size
    },
    /**
     * 单个扇区角度
     */
    segmentAngle() {
      return this.options.length > 0 ? 360 / this.options.length : 360
    },
    /**
     * 扇区视图模型（含中心角，用于标签定位）
     */
    segments() {
      const angle = this.segmentAngle
      return this.options.map((name, index) => ({
        name,
        angle: index * angle + angle / 2,
        color: this.colors[index % this.colors.length]
      }))
    },
    /**
     * 盘面样式：conic-gradient 背景 + 旋转角度
     */
    wheelStyle() {
      const angle = this.segmentAngle
      const stops = this.options
        .map((name, index) => {
          const color = this.colors[index % this.colors.length]
          return `${color} ${index * angle}deg ${(index + 1) * angle}deg`
        })
        .join(', ')
      return {
        background: `conic-gradient(${stops})`,
        transform: `rotate(${this.rotation}deg)`,
        transition: this.spinning
          ? `transform ${SPIN_DURATION / 1000}s cubic-bezier(0.12, 0.62, 0.06, 1)`
          : 'none'
      }
    }
  },
  methods: {
    /**
     * 开始旋转
     * 随机选定目标扇区后，反推需要旋转的角度，保证指针（12 点方向）停在目标扇区内
     */
    spin() {
      if (this.spinning) return
      if (!this.options || this.options.length < 2) {
        uni.showToast({ title: '至少需要 2 个选项', icon: 'none' })
        return
      }
      this.spinning = true
      this.$emit('start')

      const count = this.options.length
      const angle = this.segmentAngle
      // 随机目标扇区
      const target = Math.floor(Math.random() * count)
      // 目标扇区中心角
      const center = target * angle + angle / 2
      // 扇区内随机落点（避免停在边界）
      const jitter = (Math.random() - 0.5) * angle * 0.6
      // 需要的旋转角 ≡ (360 − center − jitter) (mod 360)
      const current = ((this.rotation % 360) + 360) % 360
      const delta = ((360 - center - jitter) - current + 720) % 360
      this.rotation += SPIN_TURNS * 360 + delta

      // 动画结束后回传结果（定时器兜底，兼容无 transitionend 的端）
      setTimeout(() => {
        this.spinning = false
        this.$emit('result', { index: target, name: this.options[target] })
      }, SPIN_TIMER)
    }
  }
}
</script>

<style lang="scss" scoped>
.wheel-box {
  position: relative;
}

/* 盘面 */
.wheel {
  position: absolute;
  top: 24rpx;
  left: 24rpx;
  right: 24rpx;
  bottom: 24rpx;
  border-radius: 50%;
  box-shadow: 0 20rpx 60rpx rgba(15, 23, 42, 0.18);
}

/* 扇区文字：从盘沿向圆心排布 */
.wheel-label {
  position: absolute;
  left: 50%;
  top: 2%;
  width: 80rpx;
  height: 48%;
  margin-left: -40rpx;
  transform-origin: 50% 100%;
  display: flex;
  justify-content: center;

  &__text {
    writing-mode: vertical-rl;
    font-size: 30rpx;
    font-weight: $font-weight-semibold;
    color: #FFFFFF;
    letter-spacing: 4rpx;
    text-shadow: 0 2rpx 6rpx rgba(0, 0, 0, 0.18);
  }
}

/* 指针 */
.wheel-pointer {
  position: absolute;
  top: 8rpx;
  left: 50%;
  transform: translateX(-50%);
  z-index: 5;
  width: 0;
  height: 0;
  border: 22rpx solid transparent;
  border-top: 40rpx solid $commemoration;
}

/* 中心按钮 */
.wheel-hub {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 4;
  width: 130rpx;
  height: 130rpx;
  border-radius: 50%;
  background: #FFFFFF;
  border: 8rpx solid $commemoration;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(251, 146, 60, 0.35);

  &__text {
    font-size: 32rpx;
    font-weight: $font-weight-bold;
    color: $commemoration;
  }
}
</style>
