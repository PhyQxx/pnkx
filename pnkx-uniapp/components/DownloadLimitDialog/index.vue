<!--
 * @Component: DownloadLimitDialog 下载次数用完引导分享弹窗
 * @Author: PHY
 * @Description: 壁纸下载次数用完时弹出的引导弹窗。
 *   - 展示「今日下载次数已用完」提示
 *   - 提供「分享小程序 +N 次下载」按钮（open-type="share"），
 *     点击即唤起微信分享面板；分享成功后由父页面调用后端发放奖励。
 *   - 提供关闭按钮
 *   - 今日分享获奖次数用完时，按钮禁用并提示「明天再来」
 *
 *   用法：
 *     <download-limit-dialog
 *       :show="showLimitDialog"
 *       :reward="rewardCount"
 *       :share-times-used="shareTimesUsed"
 *       :share-times-limit="shareTimesLimit"
 *       :reward-claiming="rewardClaiming"
 *       @share="onShareReward"
 *       @close="showLimitDialog = false"
 *     />
 *   说明：open-type="share" 按钮点击后会同时触发本组件的 @click（用于发奖）
 *   与当前页面的微信分享面板。父页面在 onShareAppMessage 中配置分享内容。
-->
<template>
  <view v-if="show" class="dld-mask" @click.stop="handleMaskClick">
    <view class="dld-panel" @click.stop>
      <!-- 关闭按钮 -->
      <view class="dld-close" @click="handleClose">
        <view class="iconfont icon-close"></view>
      </view>

      <!-- 图标 -->
      <view class="dld-icon-wrap">
        <view class="iconfont icon-download dld-icon"></view>
      </view>

      <!-- 标题与文案 -->
      <text class="dld-title">今日下载次数已用完</text>
      <text class="dld-desc">{{ descText }}</text>

      <!-- 分享得次数按钮 -->
      <button
        v-if="canShare"
        class="dld-share-btn"
        open-type="share"
        hover-class="dld-share-btn-active"
        :disabled="rewardClaiming"
        @click="handleShare"
      >
        <view class="iconfont icon-fenxiang dld-share-icon"></view>
        <text>{{ rewardClaiming ? '领取中...' : `分享小程序 +${reward} 次下载` }}</text>
      </button>
      <view v-else class="dld-share-disabled">
        今日分享奖励次数已用完，明天再来吧～
      </view>

      <!-- 知道了 -->
      <view class="dld-cancel" @click="handleClose">
        <text>{{ canShare ? '稍后再说' : '知道了' }}</text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'DownloadLimitDialog',
  props: {
    // 是否显示
    show: {
      type: Boolean,
      default: false
    },
    // 每次分享奖励的下载次数
    reward: {
      type: Number,
      default: 0
    },
    // 今日已分享获奖次数
    shareTimesUsed: {
      type: Number,
      default: 0
    },
    // 每日最多可分享获奖次数（0 表示不限）
    shareTimesLimit: {
      type: Number,
      default: 0
    },
    // 是否正在领取奖励（防止重复点击）
    rewardClaiming: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    // 今日是否还能通过分享获得奖励
    canShare() {
      if (this.shareTimesLimit <= 0) return true
      return this.shareTimesUsed < this.shareTimesLimit
    },
    descText() {
      if (this.canShare) {
        return `分享小程序给好友，即可获得 ${this.reward} 次额外下载次数`
      }
      return '分享奖励次数也已用完，明天再来吧'
    }
  },
  methods: {
    /**
     * 点击分享按钮：open-type=share 会唤起分享面板，
     * 同时触发 @click，由父页面调用后端发放奖励。
     */
    handleShare() {
      if (this.rewardClaiming) return
      this.$emit('share')
    },
    handleClose() {
      this.$emit('close')
    },
    handleMaskClick() {
      // 点击遮罩不关闭，避免误触；仅通过按钮关闭
    }
  }
}
</script>

<style lang="scss" scoped>
.dld-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}

.dld-panel {
  position: relative;
  width: 580rpx;
  background: $bg-card;
  border-radius: $radius-2xl;
  padding: $spacing-2xl $spacing-xl $spacing-lg;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: $shadow-lg;
}

.dld-close {
  position: absolute;
  top: $spacing-sm;
  right: $spacing-sm;
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;

  .icon-close {
    font-size: 28rpx;
    color: $text-tertiary;
  }
}

.dld-icon-wrap {
  width: 120rpx;
  height: 120rpx;
  border-radius: $radius-full;
  background: rgba(#0EA5E9, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: $spacing-md;

  .dld-icon {
    font-size: 56rpx;
    color: #0EA5E9;
  }
}

.dld-title {
  font-size: $font-h3;
  font-weight: $font-weight-bold;
  color: $text-primary;
  margin-bottom: $spacing-sm;
}

.dld-desc {
  font-size: $font-body;
  color: $text-secondary;
  text-align: center;
  line-height: 1.5;
  margin-bottom: $spacing-xl;
}

.dld-share-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: $spacing-md;
  border-radius: $radius-lg;
  background: linear-gradient(135deg, #0EA5E9 0%, #38BDF8 100%);
  color: #fff;
  font-size: $font-body;
  font-weight: $font-weight-semibold;

  &::after {
    border: none;
  }

  &[disabled] {
    opacity: 0.7;
    color: #fff;
  }

  .dld-share-icon {
    font-size: 32rpx;
    margin-right: $spacing-xs;
  }
}

.dld-share-btn-active {
  opacity: 0.85;
  transform: scale(0.98);
}

.dld-share-disabled {
  width: 100%;
  text-align: center;
  padding: $spacing-md;
  border-radius: $radius-lg;
  background: $gray-100;
  font-size: $font-caption;
  color: $text-tertiary;
}

.dld-cancel {
  margin-top: $spacing-md;
  padding: $spacing-sm $spacing-xl;

  text {
    font-size: $font-body;
    color: $text-tertiary;
  }
}
</style>
