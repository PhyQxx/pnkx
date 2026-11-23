<!--
 * @File: exchange/index
 * @Author: PHY
 * @Description: 汇率换算（工具箱·免登录）
 * 后端 /tools/exrate/list 代理三方牌价（Redis 缓存 1h + 离线兜底），前端双向换算
-->
<template>
  <view class="ex-page">
    <!-- 换算卡片 -->
    <view class="calc-card">
      <!-- 金额 -->
      <view class="amount-row">
        <text class="amount-label">金额</text>
        <input
          class="amount-input"
          type="digit"
          :value="amount"
          placeholder="0"
          @input="onAmountInput"
        />
      </view>
      <!-- 币种选择 -->
      <view class="currency-row">
        <view class="currency-col">
          <text class="currency-label">从</text>
          <picker
            class="currency-picker"
            :range="currencyLabels"
            @change="onFromChange"
          >
            <view class="currency-value">
              <text class="currency-code">{{ fromCurrency }}</text>
              <text class="currency-name">{{ currencyName(fromCurrency) }}</text>
              <text class="currency-arrow">▾</text>
            </view>
          </picker>
        </view>
        <view class="swap-btn" @tap="swapCurrency">
          <text class="swap-icon">⇌</text>
        </view>
        <view class="currency-col">
          <text class="currency-label">到</text>
          <picker
            class="currency-picker"
            :range="currencyLabels"
            @change="onToChange"
          >
            <view class="currency-value">
              <text class="currency-code">{{ toCurrency }}</text>
              <text class="currency-name">{{ currencyName(toCurrency) }}</text>
              <text class="currency-arrow">▾</text>
            </view>
          </picker>
        </view>
      </view>
    </view>

    <!-- 结果 -->
    <view class="result-card" v-if="rateInfo">
      <view class="result-hero">
        <text class="result-value">{{ convertedText }}</text>
        <text class="result-unit">{{ toCurrency }} · {{ currencyName(toCurrency) }}</text>
      </view>
      <view class="result-row">
        <view class="result-cell">
          <text class="result-cell__value">1 {{ fromCurrency }} = {{ rateText }} {{ toCurrency }}</text>
          <text class="result-cell__label">参考汇率</text>
        </view>
      </view>
      <view class="result-meta">
        <text class="result-meta__time">更新于 {{ rateInfo.updateTime }}</text>
        <text class="result-meta__stale" v-if="rateInfo.stale">汇率非实时，仅供参考</text>
      </view>
    </view>

    <!-- 加载/错误状态 -->
    <view class="state-view" v-if="loading">
      <text class="state-text">汇率加载中…</text>
    </view>
    <view class="state-view" v-if="errorMsg">
      <text class="state-text state-text--error">{{ errorMsg }}</text>
      <button class="retry-btn" @tap="loadRates">重试</button>
    </view>

    <view class="hint-note">
      <text>数据经 pnkx 服务端缓存（1 小时），仅供换算参考，非交易依据。</text>
    </view>

    <view class="bottom-safe"></view>
  </view>
</template>

<script>
import { getExchangeRates } from '../api/exchange'
import { convertAmount, formatRate } from '@/utils/tools/exchange'

/** 默认金额 */
const DEFAULT_AMOUNT = '100'

export default {
  name: 'ExchangeRate',
  data() {
    return {
      // 输入金额
      amount: DEFAULT_AMOUNT,
      // 币种选择
      fromCurrency: 'CNY',
      toCurrency: 'USD',
      // 牌价数据（接口返回）
      rateInfo: null,
      loading: false,
      errorMsg: ''
    }
  },
  computed: {
    /**
     * 币种下拉标签列表
     */
    currencyLabels() {
      if (!this.rateInfo || !this.rateInfo.currencyNames) return []
      return Object.keys(this.rateInfo.currencyNames)
        .map(code => `${code} · ${this.rateInfo.currencyNames[code]}`)
    },
    /**
     * 换算结果文本
     */
    convertedText() {
      const amount = parseFloat(this.amount)
      if (!this.rateInfo || !this.rateInfo.rates || !(amount >= 0)) return '—'
      try {
        const converted = convertAmount({
          amount,
          rates: this.rateInfo.rates,
          from: this.fromCurrency,
          to: this.toCurrency
        })
        return formatRate(converted)
      } catch (e) {
        return '—'
      }
    },
    /**
     * 1 源币种 → 目标币种汇率文本
     */
    rateText() {
      if (!this.rateInfo || !this.rateInfo.rates) return '—'
      try {
        const rate = convertAmount({
          amount: 1,
          rates: this.rateInfo.rates,
          from: this.fromCurrency,
          to: this.toCurrency
        })
        return formatRate(rate)
      } catch (e) {
        return '—'
      }
    }
  },
  onLoad() {
    this.loadRates()
  },
  methods: {
    /**
     * 币种中文名
     * @param {string} code 币种代码
     * @returns {string}
     */
    currencyName(code) {
      if (this.rateInfo && this.rateInfo.currencyNames) {
        return this.rateInfo.currencyNames[code] || code
      }
      return code
    },
    /**
     * 金额输入
     */
    onAmountInput(e) {
      this.amount = e.detail.value
    },
    /**
     * 源币种变更
     * @param {Object} e picker change 事件
     */
    onFromChange(e) {
      const code = this.currencyLabels[e.detail.value]
      if (code) this.fromCurrency = code.split(' ')[0]
    },
    /**
     * 目标币种变更
     * @param {Object} e picker change 事件
     */
    onToChange(e) {
      const code = this.currencyLabels[e.detail.value]
      if (code) this.toCurrency = code.split(' ')[0]
    },
    /**
     * 交换源/目标币种
     */
    swapCurrency() {
      const temp = this.fromCurrency
      this.fromCurrency = this.toCurrency
      this.toCurrency = temp
    },
    /**
     * 加载牌价（基准固定 CNY，换算在前端交叉完成）
     */
    async loadRates() {
      if (this.loading) return
      this.loading = true
      this.errorMsg = ''
      try {
        const res = await getExchangeRates('CNY')
        if (res && res.data) {
          this.rateInfo = res.data
        } else {
          this.errorMsg = '汇率数据为空，请稍后再试'
        }
      } catch (e) {
        this.errorMsg = '汇率加载失败，请检查网络后重试'
      } finally {
        this.loading = false
      }
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

.ex-page {
  min-height: 100vh;
  background-color: $bg-page;
  padding: $spacing-lg $page-padding 0;
  box-sizing: border-box;
}

/* 换算卡片 */
.calc-card {
  background: $bg-card;
  border-radius: $radius-xl;
  box-shadow: $shadow-card;
  overflow: hidden;
}

.amount-row {
  display: flex;
  align-items: center;
  padding: 28rpx 32rpx;
  border-bottom: 1rpx solid $border-light;

  .amount-label {
    font-size: $font-body;
    color: $text-secondary;
    flex-shrink: 0;
  }

  .amount-input {
    flex: 1;
    text-align: right;
    font-size: 48rpx;
    font-weight: $font-weight-bold;
    color: $text-primary;
  }
}

.currency-row {
  display: flex;
  align-items: center;
  padding: 24rpx 20rpx;
}

.currency-col {
  flex: 1;
  min-width: 0;

  .currency-label {
    font-size: $font-mini;
    color: $text-tertiary;
    padding-left: 8rpx;
  }

  .currency-picker {
    display: block;
  }

  .currency-value {
    display: flex;
    align-items: center;
    background: $gray-100;
    border-radius: $radius-lg;
    padding: 18rpx 20rpx;
    margin-top: 8rpx;

    .currency-code {
      font-size: $font-h3;
      font-weight: $font-weight-semibold;
      color: $text-primary;
    }

    .currency-name {
      flex: 1;
      margin-left: 12rpx;
      font-size: $font-mini;
      color: $text-tertiary;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .currency-arrow {
      font-size: $font-caption;
      color: $text-tertiary;
    }
  }
}

.swap-btn {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: rgba(56, 189, 248, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 16rpx;
  flex-shrink: 0;

  .swap-icon {
    font-size: 36rpx;
    color: #0284C7;
  }
}

/* 结果卡片 */
.result-card {
  margin-top: $spacing-md;
  background: $bg-card;
  border-radius: $radius-xl;
  box-shadow: $shadow-card;
  overflow: hidden;
}

.result-hero {
  text-align: center;
  padding: 36rpx 32rpx 24rpx;

  .result-value {
    display: block;
    font-size: 64rpx;
    font-weight: $font-weight-bold;
    color: #0EA5E9;
    font-variant-numeric: tabular-nums;
  }

  .result-unit {
    display: block;
    margin-top: 4rpx;
    font-size: $font-caption;
    color: $text-tertiary;
  }
}

.result-row {
  border-top: 1rpx solid $border-light;
}

.result-cell {
  text-align: center;
  padding: 22rpx 16rpx;

  &__value {
    display: block;
    font-size: $font-body;
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

.result-meta {
  display: flex;
  justify-content: center;
  gap: 24rpx;
  padding: 0 32rpx 24rpx;

  &__time {
    font-size: $font-mini;
    color: $text-tertiary;
  }

  &__stale {
    font-size: $font-mini;
    color: $warning-dark;
  }
}

/* 加载/错误状态 */
.state-view {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: $spacing-xl 0;

  .state-text {
    font-size: $font-caption;
    color: $text-tertiary;

    &--error {
      color: $danger;
    }
  }

  .retry-btn {
    margin-top: $spacing-md;
    height: 68rpx;
    line-height: 68rpx;
    padding: 0 48rpx;
    border-radius: $radius-lg;
    background: rgba(56, 189, 248, 0.1);
    color: #0284C7;
    font-size: $font-caption;
    font-weight: $font-weight-semibold;

    &::after {
      border: none;
    }
  }
}

/* 底部说明 */
.hint-note {
  padding: $spacing-md 40rpx;
  text-align: center;
  font-size: $font-mini;
  color: $text-tertiary;
  line-height: $line-height-relaxed;
}

.bottom-safe {
  height: calc(env(safe-area-inset-bottom) + 40rpx);
}
</style>
