<!--
 * @File: what-to-eat/index
 * @Author: PHY
 * @Description: 今天吃什么 · 决策转盘（工具箱·免登录）
 * 选项本地持久化（2–10 个），转盘随机决定，结果弹窗支持「再转一次」
-->
<template>
  <view class="wte-page">
    <!-- 转盘 -->
    <view class="wheel-zone">
      <DecisionWheel
        ref="wheel"
        :options="options"
        :size="600"
        @start="onWheelStart"
        @result="onWheelResult"
      />
    </view>

    <!-- 开始按钮 -->
    <button class="spin-btn" :disabled="spinning" @tap="spin">
      {{ spinning ? '命运转盘转动中…' : '🍽️ 今晚吃啥，转一下！' }}
    </button>

    <!-- 我的选项 -->
    <view class="section-title">
      <view class="section-accent"></view>
      <text class="section-text">我的选项</text>
      <text class="section-extra">2–10 个</text>
    </view>

    <view class="opt-bar">
      <input
        class="opt-input"
        v-model="inputOption"
        maxlength="5"
        placeholder="输入店名 / 菜名"
        confirm-type="done"
        @confirm="addOption"
      />
      <button class="opt-add" @tap="addOption">添加</button>
    </view>
    <view class="opt-list">
      <view v-for="(item, index) in options" :key="item + '-' + index" class="opt-chip">
        <text class="opt-chip__text">{{ foodEmoji(item) }} {{ item }}</text>
        <text class="opt-chip__remove" @tap="removeOption(index)">✕</text>
      </view>
    </view>

    <view class="share-hint">
      <text>小程序内可「转发给 TA」—— 让对方帮你按下去 😈</text>
    </view>

    <view class="bottom-safe"></view>

    <!-- 结果弹窗 -->
    <view class="popup-mask" v-if="showPopup" @tap="closePopup">
      <view class="popup-card" @tap.stop>
        <text class="popup-card__food">{{ result.emoji }}</text>
        <text class="popup-card__hint">命运安排，今晚就吃</text>
        <text class="popup-card__name">{{ result.name }}</text>
        <view class="popup-card__btns">
          <button class="popup-btn popup-btn--ghost" @tap="spinAgain">再转一次</button>
          <button class="popup-btn popup-btn--primary" @tap="confirmResult">就它了！</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import DecisionWheel from '../components/DecisionWheel.vue'

/** 选项本地存储 key */
const STORAGE_KEY = 'tools_wte_options'

/** 选项数量限制 */
const OPTIONS_MIN = 2
const OPTIONS_MAX = 10

/** 默认选项 */
const DEFAULT_OPTIONS = ['火锅', '烧烤', '日料', '麻辣烫', '拉面', '炒菜', '汉堡', '饺子']

/** 关键词 → emoji 映射（模糊匹配，按顺序取第一个命中） */
const FOOD_EMOJI_MAP = [
  ['火锅', '🍲'], ['烧烤', '🍢'], ['日料', '🍣'], ['麻辣烫', '🍜'], ['拉面', '🍜'],
  ['炒菜', '🥘'], ['汉堡', '🍔'], ['饺子', '🥟'], ['粥', '🥣'], ['面', '🍜'],
  ['饭', '🍛'], ['串', '🍢'], ['鸡', '🍗'], ['蟹', '🦀'], ['虾', '🍤'], ['菜', '🥗']
]

export default {
  name: 'WhatToEat',
  components: { DecisionWheel },
  data() {
    return {
      // 选项列表
      options: [...DEFAULT_OPTIONS],
      // 输入中的新选项
      inputOption: '',
      // 转盘状态
      spinning: false,
      // 结果弹窗
      showPopup: false,
      result: { name: '', emoji: '🍽️' }
    }
  },
  onLoad() {
    this.loadOptions()
  },
  // 开启「转发给朋友」（需同时存在，朋友圈分享才会生效）
  onShareAppMessage() {
    return {
      title: '今晚吃啥？转盘帮我决定 👉',
      path: '/pages_tools/what-to-eat/index'
    }
  },
  // 开启「分享到朋友圈」
  onShareTimeline() {
    return {
      title: '今晚吃啥？转盘帮我决定 👉'
    }
  },
  methods: {
    /**
     * 从本地缓存加载选项（未配置时用默认值）
     */
    loadOptions() {
      try {
        const cached = uni.getStorageSync(STORAGE_KEY)
        if (Array.isArray(cached) && cached.length >= OPTIONS_MIN && cached.length <= OPTIONS_MAX) {
          this.options = cached
        }
      } catch (e) {
        console.warn('读取转盘选项缓存失败，使用默认选项', e)
      }
    },
    /**
     * 选项持久化
     */
    persistOptions() {
      try {
        uni.setStorageSync(STORAGE_KEY, this.options)
      } catch (e) {
        console.warn('保存转盘选项缓存失败', e)
      }
    },
    /**
     * 关键词匹配食物 emoji（无命中时用默认）
     * @param {string} name 选项名称
     * @returns {string} emoji
     */
    foodEmoji(name) {
      const hit = FOOD_EMOJI_MAP.find(([keyword]) => name.includes(keyword))
      return hit ? hit[1] : '🍽️'
    },
    /**
     * 添加选项（去重、上限校验）
     */
    addOption() {
      const value = (this.inputOption || '').trim()
      if (!value) {
        this.$modal.msg('先输入一个选项')
        return
      }
      if (this.spinning) {
        this.$modal.msg('转盘转动中，稍后再改')
        return
      }
      if (this.options.length >= OPTIONS_MAX) {
        this.$modal.msg(`最多 ${OPTIONS_MAX} 个选项`)
        return
      }
      if (this.options.includes(value)) {
        this.$modal.msg('已经有这个选项啦')
        return
      }
      this.options.push(value)
      this.inputOption = ''
      this.persistOptions()
    },
    /**
     * 删除选项（保留下限）
     * @param {number} index 选项下标
     */
    removeOption(index) {
      if (this.options.length <= OPTIONS_MIN) {
        this.$modal.msg(`至少保留 ${OPTIONS_MIN} 个选项哦`)
        return
      }
      if (this.spinning) {
        this.$modal.msg('转盘转动中，稍后再改')
        return
      }
      this.options.splice(index, 1)
      this.persistOptions()
    },
    /**
     * 开始旋转
     */
    spin() {
      if (this.spinning) return
      if (this.options.length < OPTIONS_MIN) {
        this.$modal.msg(`至少需要 ${OPTIONS_MIN} 个选项`)
        return
      }
      this.spinning = true
      this.$refs.wheel.spin()
    },
    /**
     * 转盘开始事件
     */
    onWheelStart() {
      this.spinning = true
    },
    /**
     * 转盘结果事件
     * @param {{index:number, name:string}} payload 结果
     */
    onWheelResult(payload) {
      this.spinning = false
      this.result = {
        name: payload.name,
        emoji: this.foodEmoji(payload.name)
      }
      this.showPopup = true
    },
    /**
     * 关闭结果弹窗
     */
    closePopup() {
      this.showPopup = false
    },
    /**
     * 再转一次
     */
    spinAgain() {
      this.showPopup = false
      setTimeout(() => this.spin(), 250)
    },
    /**
     * 确认结果
     */
    confirmResult() {
      this.showPopup = false
      this.$modal.msgSuccess(`今晚就吃「${this.result.name}」！`)
    }
  }
}
</script>

<style lang="scss" scoped>
/* #ifndef APP-NVUE */
page {
  background-color: $bg-page;
}
/* #endif */

.wte-page {
  min-height: 100vh;
  background-color: $bg-page;
  padding: $spacing-lg $page-padding 0;
  box-sizing: border-box;
}

/* 转盘区域 */
.wheel-zone {
  display: flex;
  justify-content: center;
  padding: $spacing-sm 0;
}

/* 开始按钮 */
.spin-btn {
  margin-top: $spacing-lg;
  height: 92rpx;
  line-height: 92rpx;
  border-radius: $radius-xl;
  background: linear-gradient(135deg, $commemoration, $card);
  color: $text-inverse;
  font-size: $font-h3;
  font-weight: $font-weight-semibold;
  box-shadow: 0 12rpx 36rpx rgba(244, 114, 182, 0.35);

  &::after {
    border: none;
  }

  &[disabled] {
    opacity: 0.6;
    color: $text-inverse;
    background: linear-gradient(135deg, $commemoration, $card);
  }
}

/* 区块标题 */
.section-title {
  display: flex;
  align-items: center;
  margin: $spacing-lg 0 $spacing-md;

  .section-accent {
    width: 6rpx;
    height: 28rpx;
    border-radius: $radius-full;
    margin-right: $spacing-sm;
    flex-shrink: 0;
    background-color: $menstruation;
  }

  .section-text {
    font-size: $font-body;
    font-weight: $font-weight-semibold;
    color: $text-primary;
  }

  .section-extra {
    margin-left: auto;
    font-size: $font-mini;
    color: $text-tertiary;
  }
}

/* 选项编辑 */
.opt-bar {
  display: flex;
  align-items: center;

  .opt-input {
    flex: 1;
    height: 76rpx;
    background: $bg-card;
    border-radius: $radius-lg;
    padding: 0 28rpx;
    font-size: $font-caption;
    color: $text-primary;
    box-shadow: $shadow-card;
  }

  .opt-add {
    height: 76rpx;
    line-height: 76rpx;
    padding: 0 32rpx;
    margin: 0 0 0 $spacing-sm;
    border-radius: $radius-lg;
    background: $commemoration;
    color: $text-inverse;
    font-size: $font-caption;
    font-weight: $font-weight-semibold;

    &::after {
      border: none;
    }
  }
}

.opt-list {
  display: flex;
  flex-wrap: wrap;
  padding: $spacing-xs 0 0;
}

.opt-chip {
  display: flex;
  align-items: center;
  background: $bg-card;
  border-radius: $radius-full;
  padding: 14rpx 24rpx;
  margin: 0 16rpx 16rpx 0;
  box-shadow: $shadow-card;

  &__text {
    font-size: $font-caption;
    color: $text-secondary;
  }

  &__remove {
    font-size: $font-caption;
    color: $text-disabled;
    padding: 0 4rpx 0 10rpx;
  }
}

/* 分享提示 */
.share-hint {
  text-align: center;
  font-size: $font-mini;
  color: $text-tertiary;
  padding: $spacing-sm 40rpx 0;
}

.bottom-safe {
  height: calc(env(safe-area-inset-bottom) + 40rpx);
}

/* 结果弹窗 */
.popup-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.5);
  z-index: $z-modal;
  display: flex;
  align-items: center;
  justify-content: center;
}

.popup-card {
  width: 560rpx;
  background: $bg-card;
  border-radius: $radius-2xl;
  padding: 48rpx 40rpx 36rpx;
  display: flex;
  flex-direction: column;
  align-items: center;

  &__food {
    font-size: 100rpx;
    line-height: 1.2;
  }

  &__hint {
    margin-top: 16rpx;
    font-size: $font-caption;
    color: $text-secondary;
  }

  &__name {
    margin-top: 4rpx;
    font-size: 44rpx;
    font-weight: $font-weight-bold;
    color: $commemoration;
  }

  &__btns {
    display: flex;
    margin-top: 36rpx;
    width: 100%;
  }
}

.popup-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: $radius-lg;
  font-size: $font-caption;
  font-weight: $font-weight-semibold;
  margin: 0;
  padding: 0;

  &::after {
    border: none;
  }

  &--ghost {
    background: rgba(56, 189, 248, 0.1);
    color: #0284C7;
    margin-right: 20rpx;
  }

  &--primary {
    background: linear-gradient(135deg, $commemoration, $card);
    color: $text-inverse;
  }
}
</style>
