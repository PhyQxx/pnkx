<!--
 * @File: mortgage/index
 * @Author: PHY
 * @Description: 房贷计算器（工具箱·免登录）
 * 支持商贷/公积金利率切换、等额本息/本金对比，参数变更实时重算
-->
<template>
  <view class="mortgage-page">
    <!-- 贷款信息 -->
    <view class="section-title">
      <view class="section-accent"></view>
      <text class="section-text">贷款信息</text>
    </view>

    <view class="form-card">
      <!-- 房款总额 -->
      <view class="field">
        <text class="field-label">房款总额</text>
        <view class="field-value">
          <input
            class="field-input"
            type="digit"
            :value="houseTotal"
            placeholder="0"
            @input="onTotalInput"
          />
          <text class="field-unit">万</text>
        </view>
      </view>
      <!-- 首付比例 -->
      <view class="field">
        <text class="field-label">首付比例</text>
        <view class="seg">
          <view
            v-for="(item, index) in downPayments"
            :key="item.value"
            class="seg-item"
            :class="{ 'seg-item--active': index === downIndex }"
            @tap="onDownChange(index)"
          >
            <text>{{ item.label }}</text>
          </view>
        </view>
      </view>
      <!-- 贷款类型 -->
      <view class="field">
        <text class="field-label">贷款类型</text>
        <view class="seg">
          <view
            v-for="(item, index) in loanTypes"
            :key="item.label"
            class="seg-item"
            :class="{ 'seg-item--active': index === loanTypeIndex }"
            @tap="onTypeChange(index)"
          >
            <text>{{ item.label }}</text>
          </view>
        </view>
      </view>
      <!-- 贷款年限 -->
      <view class="field">
        <text class="field-label">贷款年限</text>
        <view class="seg">
          <view
            v-for="(item, index) in yearOptions"
            :key="item.value"
            class="seg-item"
            :class="{ 'seg-item--active': index === yearIndex }"
            @tap="onYearChange(index)"
          >
            <text>{{ item.label }}</text>
          </view>
        </view>
      </view>
      <!-- 年利率 -->
      <view class="field">
        <text class="field-label">年利率</text>
        <view class="field-value">
          <input
            class="field-input"
            type="digit"
            :value="rate"
            placeholder="0"
            @input="onRateInput"
          />
          <text class="field-unit">%</text>
        </view>
      </view>
      <!-- 还款方式 -->
      <view class="field">
        <text class="field-label">还款方式</text>
        <view class="seg seg--solid">
          <view
            v-for="(item, index) in methods"
            :key="item.value"
            class="seg-item"
            :class="{ 'seg-item--solid-active': index === methodIndex }"
            @tap="onMethodChange(index)"
          >
            <text>{{ item.label }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 计算结果 -->
    <view class="section-title">
      <view class="section-accent"></view>
      <text class="section-text">计算结果</text>
    </view>

    <view class="result-card" v-if="result">
      <!-- 月供 -->
      <view class="result-hero">
        <text class="result-hero__label">{{ result.monthLabel }}</text>
        <text class="result-hero__value">¥ {{ result.monthText }}</text>
        <text class="result-hero__sub">{{ result.monthSub }}</text>
      </view>
      <!-- 三格汇总 -->
      <view class="result-row">
        <view class="result-cell">
          <text class="result-cell__value">{{ loanAmountText }}</text>
          <text class="result-cell__label">贷款总额</text>
        </view>
        <view class="result-cell">
          <text class="result-cell__value">{{ result.totalInterestText }}</text>
          <text class="result-cell__label">支付利息</text>
        </view>
        <view class="result-cell">
          <text class="result-cell__value">{{ result.totalPaymentText }}</text>
          <text class="result-cell__label">还款总额</text>
        </view>
      </view>
      <!-- 本息占比 -->
      <view class="ratio-bar">
        <view class="ratio-bar__principal" :style="{ width: result.principalPercent + '%' }"></view>
        <view class="ratio-bar__interest" :style="{ width: result.interestPercent + '%' }"></view>
      </view>
      <view class="ratio-legend">
        <text class="ratio-legend__item">
          <text class="ratio-dot ratio-dot--p"></text>本金 {{ result.principalPercent }}%
        </text>
        <text class="ratio-legend__item">
          <text class="ratio-dot ratio-dot--i"></text>利息 {{ result.interestPercent }}%
        </text>
      </view>
    </view>

    <view class="hint-note">
      <text class="hint-note__line">计算结果仅供参考，实际月供以银行审批为准。</text>
      <text class="hint-note__line">切「公积金」自动切换利率，可手动修改。</text>
    </view>

    <view class="bottom-safe"></view>
  </view>
</template>

<script>
import { calcEqualPayment, calcEqualPrincipal, formatMoney } from '@/utils/tools/loan'

/** 首付比例选项（%） */
const DOWN_PAYMENTS = [
  { label: '20%', value: 20 },
  { label: '30%', value: 30 },
  { label: '35%', value: 35 }
]
/** 贷款类型选项（切换时回填默认利率） */
const LOAN_TYPES = [
  { label: '商业贷款', value: '3.60' },
  { label: '公积金', value: '2.85' }
]
/** 年限选项（年） */
const YEAR_OPTIONS = [
  { label: '20年', value: 20 },
  { label: '25年', value: 25 },
  { label: '30年', value: 30 }
]
/** 还款方式 */
const METHODS = [
  { label: '等额本息', value: 'ac' },
  { label: '等额本金', value: 'ab' }
]

/** 房款总额下限（万） */
const HOUSE_TOTAL_MIN = 10
/** 房款总额上限（万） */
const HOUSE_TOTAL_MAX = 5000
/** 输入防抖（ms） */
const INPUT_DEBOUNCE = 200

export default {
  name: 'MortgageCalculator',
  data() {
    return {
      // 表单选项
      downPayments: DOWN_PAYMENTS,
      loanTypes: LOAN_TYPES,
      yearOptions: YEAR_OPTIONS,
      methods: METHODS,
      // 输入值
      houseTotal: '120',
      rate: LOAN_TYPES[0].value,
      downIndex: 1,
      loanTypeIndex: 0,
      yearIndex: 2,
      methodIndex: 0,
      // 计算结果（视图模型）
      result: null,
      // 输入防抖定时器
      debounceTimer: null
    }
  },
  computed: {
    /**
     * 贷款总额（万）= 房款总额 × (1 − 首付比例)
     */
    loanAmountWan() {
      const total = Math.min(
        HOUSE_TOTAL_MAX,
        Math.max(HOUSE_TOTAL_MIN, parseFloat(this.houseTotal) || 0)
      )
      return total * (1 - DOWN_PAYMENTS[this.downIndex].value / 100)
    },
    /**
     * 贷款总额展示（万，去掉多余小数）
     */
    loanAmountText() {
      const v = this.loanAmountWan
      return (v % 1 === 0 ? v.toFixed(0) : v.toFixed(1)) + '万'
    }
  },
  onLoad() {
    this.calculate()
  },
  onUnload() {
    if (this.debounceTimer) clearTimeout(this.debounceTimer)
  },
  methods: {
    /**
     * 房款总额输入（防抖重算）
     */
    onTotalInput(e) {
      this.houseTotal = e.detail.value
      this.calculateDebounced()
    },
    /**
     * 年利率输入（防抖重算）
     */
    onRateInput(e) {
      this.rate = e.detail.value
      this.calculateDebounced()
    },
    /**
     * 首付比例切换
     * @param {number} index 选项下标
     */
    onDownChange(index) {
      this.downIndex = index
      this.calculate()
    },
    /**
     * 贷款类型切换：回填默认利率后重算
     * @param {number} index 选项下标
     */
    onTypeChange(index) {
      this.loanTypeIndex = index
      this.rate = LOAN_TYPES[index].value
      this.calculate()
    },
    /**
     * 年限切换
     * @param {number} index 选项下标
     */
    onYearChange(index) {
      this.yearIndex = index
      this.calculate()
    },
    /**
     * 还款方式切换
     * @param {number} index 选项下标
     */
    onMethodChange(index) {
      this.methodIndex = index
      this.calculate()
    },
    /**
     * 文本输入防抖，避免每次按键都全量重算
     */
    calculateDebounced() {
      if (this.debounceTimer) clearTimeout(this.debounceTimer)
      this.debounceTimer = setTimeout(() => {
        this.calculate()
      }, INPUT_DEBOUNCE)
    },
    /**
     * 执行计算并生成视图模型
     */
    calculate() {
      const principal = this.loanAmountWan * 10000
      const rate = Math.min(24, Math.max(0.01, parseFloat(this.rate) || 0))
      const months = YEAR_OPTIONS[this.yearIndex].value * 12
      const method = METHODS[this.methodIndex].value
      const isAn = method === 'ac'

      const calc = isAn
        ? calcEqualPayment(principal, rate, months)
        : calcEqualPrincipal(principal, rate, months)

      const interestPercent = Math.round(
        calc.totalInterest / (principal + calc.totalInterest) * 100
      )
      const monthText = isAn
        ? formatMoney(calc.monthlyPayment)
        : formatMoney(calc.firstMonth)

      this.result = {
        monthLabel: isAn ? '月供（等额本息）' : '首月月供（等额本金）',
        monthText,
        monthSub: isAn
          ? `固定月供 · 共 ${months} 期`
          : `首月月供 · 每月递减 ${formatMoney(calc.monthlyDecrease)}`,
        totalInterestText: this.toWanText(calc.totalInterest),
        totalPaymentText: this.toWanText(calc.totalPayment),
        principalPercent: 100 - interestPercent,
        interestPercent
      }
    },
    /**
     * 金额（元）转「万」展示文本
     * @param {number} yuan 金额（元）
     * @returns {string} 如 53.48万 / 137.5万
     */
    toWanText(yuan) {
      const wan = yuan / 10000
      const text = wan >= 100 ? wan.toFixed(1) : wan.toFixed(2)
      return parseFloat(text) + '万'
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

.mortgage-page {
  min-height: 100vh;
  background-color: $bg-page;
  padding: $spacing-md $page-padding 0;
  box-sizing: border-box;
}

/* 区块标题 */
.section-title {
  display: flex;
  align-items: center;
  margin: $spacing-md 0;

  .section-accent {
    width: 6rpx;
    height: 28rpx;
    border-radius: $radius-full;
    margin-right: $spacing-sm;
    flex-shrink: 0;
    background-color: $wallpaper;
  }

  .section-text {
    font-size: $font-body;
    font-weight: $font-weight-semibold;
    color: $text-primary;
  }
}

/* 表单卡片 */
.form-card {
  background: $bg-card;
  border-radius: $radius-xl;
  box-shadow: $shadow-card;
  overflow: hidden;
}

.field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22rpx 28rpx;

  & + .field {
    border-top: 1rpx solid $border-light;
  }

  .field-label {
    font-size: $font-body;
    color: $text-secondary;
    flex-shrink: 0;
  }

  .field-value {
    display: flex;
    align-items: center;
  }

  .field-input {
    width: 220rpx;
    text-align: right;
    font-size: 32rpx;
    font-weight: $font-weight-semibold;
    color: $text-primary;
  }

  .field-unit {
    margin-left: $spacing-2xs;
    font-size: $font-caption;
    color: $text-tertiary;
  }
}

/* 分段选择器 */
.seg {
  display: flex;
  background: $gray-100;
  border-radius: $radius-md;
  padding: 6rpx;

  &-item {
    flex: 1;
    text-align: center;
    padding: 12rpx 0;
    border-radius: $radius-sm;
    font-size: $font-caption;
    color: $text-tertiary;
    transition: all $duration-fast $ease-default;

    &--active {
      background: $bg-card;
      color: #0284C7;
      font-weight: $font-weight-semibold;
      box-shadow: 0 2rpx 8rpx rgba(15, 23, 42, 0.12);
    }
  }

  /* 实色变体（还款方式） */
  &--solid {
    .seg-item--solid-active {
      background: $wallpaper;
      color: $text-inverse;
      box-shadow: 0 4rpx 12rpx rgba(56, 189, 248, 0.35);
    }
  }
}

/* 结果卡片 */
.result-card {
  background: $bg-card;
  border-radius: $radius-xl;
  box-shadow: $shadow-card;
  overflow: hidden;
}

.result-hero {
  text-align: center;
  padding: 36rpx 32rpx 32rpx;

  &__label {
    font-size: $font-caption;
    color: $text-secondary;
  }

  &__value {
    display: block;
    font-size: 64rpx;
    font-weight: $font-weight-bold;
    color: #0EA5E9;
    margin: 8rpx 0;
    font-variant-numeric: tabular-nums;
  }

  &__sub {
    font-size: $font-small;
    color: $text-tertiary;
  }
}

.result-row {
  display: flex;
  border-top: 1rpx solid $border-light;
}

.result-cell {
  flex: 1;
  text-align: center;
  padding: 22rpx 8rpx;

  & + .result-cell {
    border-left: 1rpx solid $border-light;
  }

  &__value {
    display: block;
    font-size: $font-h3;
    font-weight: $font-weight-semibold;
    color: $text-primary;
    font-variant-numeric: tabular-nums;
  }

  &__label {
    display: block;
    margin-top: 4rpx;
    font-size: $font-mini;
    color: $text-tertiary;
  }
}

/* 本息占比 */
.ratio-bar {
  display: flex;
  height: 20rpx;
  border-radius: $radius-full;
  overflow: hidden;
  margin: $spacing-md $page-padding 0;

  &__principal {
    background: #0EA5E9;
    transition: width $duration-normal $ease-default;
  }

  &__interest {
    background: $warning;
    transition: width $duration-normal $ease-default;
  }
}

.ratio-legend {
  display: flex;
  justify-content: center;
  padding: 16rpx 0 28rpx;

  &__item {
    font-size: $font-mini;
    color: $text-secondary;
  }

  &__item + &__item {
    margin-left: 48rpx;
  }
}

.ratio-dot {
  display: inline-block;
  width: 16rpx;
  height: 16rpx;
  border-radius: 6rpx;
  margin-right: 8rpx;

  &--p {
    background: #0EA5E9;
  }

  &--i {
    background: $warning;
  }
}

/* 底部说明 */
.hint-note {
  padding: $spacing-md 40rpx;
  text-align: center;
  font-size: $font-mini;
  color: $text-tertiary;
  line-height: $line-height-relaxed;

  &__line {
    display: block;
  }
}

.bottom-safe {
  height: calc(env(safe-area-inset-bottom) + 40rpx);
}
</style>
