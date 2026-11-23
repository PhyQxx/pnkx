<!--
 * @File: tax/index
 * @Author: PHY
 * @Description: 个税计算器（工具箱·免登录）
 * 年度累计预扣简化估算 + 年终奖单独/并入对比；房贷利息与住房租金扣除互斥
-->
<template>
  <view class="tax-page">
    <!-- 收入信息 -->
    <view class="section-title">
      <view class="section-accent"></view>
      <text class="section-text">收入信息</text>
    </view>

    <view class="form-card">
      <view class="field">
        <text class="field-label">税前月薪</text>
        <view class="field-value">
          <input
            class="field-input"
            type="digit"
            :value="salary"
            placeholder="0"
            @input="onSalaryInput"
          />
          <text class="field-unit">元</text>
        </view>
      </view>
      <view class="field">
        <text class="field-label">年终奖</text>
        <view class="field-value">
          <input
            class="field-input"
            type="digit"
            :value="bonus"
            placeholder="0"
            @input="onBonusInput"
          />
          <text class="field-unit">元</text>
        </view>
      </view>
      <view class="field">
        <text class="field-label">五险一金（个人）</text>
        <view class="seg">
          <view
            v-for="(item, index) in insuranceOptions"
            :key="item"
            class="seg-item"
            :class="{ 'seg-item--active': index === insuranceIndex }"
            @tap="onInsuranceChange(index)"
          >
            <text>{{ item }}%</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 专项附加扣除 -->
    <view class="section-title">
      <view class="section-accent"></view>
      <text class="section-text">专项附加扣除</text>
      <text class="section-extra">点选生效 · 月额度</text>
    </view>

    <view class="form-card">
      <view class="chip-grid">
        <view
          v-for="item in deductions"
          :key="item.key"
          class="dchip"
          :class="{ 'dchip--on': selectedMap[item.key], 'dchip--blocked': isBlocked(item.key) }"
          @tap="onDeductionTap(item)"
        >
          <text class="dchip__name">{{ item.name }}</text>
          <text class="dchip__amount">{{ item.amount }}</text>
        </view>
      </view>
    </view>

    <!-- 计算结果 -->
    <view class="section-title">
      <view class="section-accent"></view>
      <text class="section-text">计算结果</text>
    </view>

    <view class="result-card" v-if="result">
      <view class="result-hero">
        <text class="result-hero__label">每月应缴个税（年终奖按月平摊）</text>
        <text class="result-hero__value">¥ {{ result.monthTaxText }}</text>
        <text class="result-hero__sub">适用税率 {{ result.tierRateText }} · 速算扣除 {{ result.tierQuickText }}</text>
      </view>

      <view class="result-row">
        <view class="result-cell">
          <text class="result-cell__value">¥ {{ result.monthlyNetText }}</text>
          <text class="result-cell__label">月到手</text>
        </view>
        <view class="result-cell">
          <text class="result-cell__value">¥ {{ result.bonusNetText }}</text>
          <text class="result-cell__label">年终奖到手</text>
        </view>
        <view class="result-cell">
          <text class="result-cell__value">{{ result.effectiveRateText }}</text>
          <text class="result-cell__label">实际税负率</text>
        </view>
      </view>
      <view class="result-row">
        <view class="result-cell">
          <text class="result-cell__value">¥ {{ result.annualTaxableText }}</text>
          <text class="result-cell__label">年应纳税所得额</text>
        </view>
        <view class="result-cell">
          <text class="result-cell__value">¥ {{ result.annualTaxText }}</text>
          <text class="result-cell__label">全年工资个税</text>
        </view>
        <view class="result-cell">
          <text class="result-cell__value">¥ {{ result.bonusTaxText }}</text>
          <text class="result-cell__label">年终奖个税</text>
        </view>
      </view>

      <!-- 年终奖方式提示 -->
      <view class="bonus-tip" v-if="result.bonusTip">
        <text class="bonus-tip__text">{{ result.bonusTip }}</text>
      </view>
    </view>

    <!-- 税率表 -->
    <view class="form-card tier-card">
      <view class="tier-table">
        <view class="tier-row tier-row--head">
          <text class="tier-col tier-col--range">年应纳税所得额</text>
          <text class="tier-col tier-col--rate">税率</text>
          <text class="tier-col tier-col--quick">速算扣除</text>
        </view>
        <view
          v-for="(tier, index) in tiers"
          :key="index"
          class="tier-row"
          :class="{ 'tier-row--current': result && result.tierIndex === index }"
        >
          <text class="tier-col tier-col--range">{{ tier.rangeText }}</text>
          <text class="tier-col tier-col--rate">{{ tier.rateText }}</text>
          <text class="tier-col tier-col--quick">{{ tier.quickText }}</text>
        </view>
      </view>
    </view>

    <view class="hint-note">
      <text>按年度累计预扣预缴制简化估算；房贷利息与住房租金不可同时扣除。</text>
    </view>

    <view class="bottom-safe"></view>
  </view>
</template>

<script>
import {
  calcSalaryTax,
  calcBonusTax,
  ANNUAL_TIERS,
  SPECIAL_DEDUCTIONS
} from '@/utils/tools/tax'

/** 五险一金个人比例选项（%） */
const INSURANCE_OPTIONS = [10, 15, 20]

/** 输入上限（元），防误输入导致计算异常 */
const AMOUNT_MAX = 100000000

/** 输入防抖（ms） */
const INPUT_DEBOUNCE = 200

export default {
  name: 'TaxCalculator',
  data() {
    return {
      // 表单输入
      salary: '20000',
      bonus: '40000',
      insuranceOptions: INSURANCE_OPTIONS,
      insuranceIndex: 1,
      // 专项附加扣除项配置
      deductions: SPECIAL_DEDUCTIONS,
      // 扣除项选中状态（预定义 key 保证 Vue2 响应式）
      selectedMap: {
        child: false,
        elder: false,
        house: false,
        rent: false,
        edu: false
      },
      // 计算结果（视图模型）
      result: null,
      // 税率表视图模型
      tiers: [],
      // 输入防抖定时器
      debounceTimer: null
    }
  },
  onLoad() {
    this.buildTierTable()
    this.calculate()
  },
  onUnload() {
    if (this.debounceTimer) clearTimeout(this.debounceTimer)
  },
  methods: {
    /**
     * 月薪输入（防抖重算）
     */
    onSalaryInput(e) {
      this.salary = e.detail.value
      this.calculateDebounced()
    },
    /**
     * 年终奖输入（防抖重算）
     */
    onBonusInput(e) {
      this.bonus = e.detail.value
      this.calculateDebounced()
    },
    /**
     * 五险一金比例切换
     * @param {number} index 选项下标
     */
    onInsuranceChange(index) {
      this.insuranceIndex = index
      this.calculate()
    },
    /**
     * 扣除项是否被锁定（房贷利息与住房租金互斥：一方选中时另一方锁定）
     * @param {string} key 扣除项 key
     * @returns {boolean}
     */
    isBlocked(key) {
      if (key === 'house') return this.selectedMap.rent && !this.selectedMap.house
      if (key === 'rent') return this.selectedMap.house && !this.selectedMap.rent
      return false
    },
    /**
     * 扣除项点选
     * @param {{key:string, name:string, amount:number}} item 扣除项
     */
    onDeductionTap(item) {
      // 被锁定的一方不可点
      if (this.isBlocked(item.key)) {
        this.$modal.msg(`「${item.name}」与已选扣除项互斥，请先取消另一方`)
        return
      }
      this.selectedMap[item.key] = !this.selectedMap[item.key]
      this.calculate()
    },
    /**
     * 文本输入防抖
     */
    calculateDebounced() {
      if (this.debounceTimer) clearTimeout(this.debounceTimer)
      this.debounceTimer = setTimeout(() => {
        this.calculate()
      }, INPUT_DEBOUNCE)
    },
    /**
     * 构建税率表视图模型
     */
    buildTierTable() {
      this.tiers = ANNUAL_TIERS.map(tier => ({
        rangeText: tier.max === Infinity
          ? '超过 96 万'
          : `≤ ${tier.max >= 10000 ? tier.max / 10000 + ' 万' : tier.max + ' 元'}`,
        rateText: Math.round(tier.rate * 100) + '%',
        quickText: tier.quick === Infinity ? '-' : tier.quick.toLocaleString()
      }))
    },
    /**
     * 执行计算并生成视图模型
     */
    calculate() {
      const salary = Math.min(AMOUNT_MAX, Math.max(0, parseFloat(this.salary) || 0))
      const bonus = Math.min(AMOUNT_MAX, Math.max(0, parseFloat(this.bonus) || 0))
      const insuranceRate = INSURANCE_OPTIONS[this.insuranceIndex]
      const deductionTotal = SPECIAL_DEDUCTIONS
        .filter(d => this.selectedMap[d.key])
        .reduce((sum, d) => sum + d.amount, 0)

      const salaryResult = calcSalaryTax({
        monthlySalary: salary,
        insuranceRate,
        specialDeductions: deductionTotal
      })
      const bonusResult = calcBonusTax(bonus, salaryResult.annualTaxable)

      // 月均应缴 = 工资税月摊 + 年终奖单独计税税额月摊
      const monthTaxTotal = salaryResult.monthlyTax + bonusResult.separateTax / 12
      // 年终奖方式提示：并入更优时提醒
      let bonusTip = ''
      if (bonus > 0 && bonusResult.better === 'merged') {
        const save = Math.max(0, Math.round(bonusResult.separateTax - bonusResult.mergedIncrement))
        bonusTip = `年终奖并入综合所得更划算，可省税 ¥ ${save.toLocaleString()}`
      }

      this.result = {
        monthTaxText: Math.round(monthTaxTotal).toLocaleString(),
        tierRateText: Math.round(salaryResult.tier.rate * 100) + '%',
        tierQuickText: salaryResult.tier.quick.toLocaleString(),
        tierIndex: salaryResult.tierIndex,
        monthlyNetText: Math.round(salaryResult.monthlyNet).toLocaleString(),
        bonusNetText: Math.round(bonusResult.separateNet).toLocaleString(),
        effectiveRateText: (salaryResult.effectiveRate * 100).toFixed(1) + '%',
        annualTaxableText: Math.round(salaryResult.annualTaxable).toLocaleString(),
        annualTaxText: Math.round(salaryResult.annualTax).toLocaleString(),
        bonusTaxText: Math.round(bonusResult.separateTax).toLocaleString(),
        bonusTip
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

.tax-page {
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
    background-color: $warning;
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
    width: 240rpx;
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
}

/* 扣除项 chips */
.chip-grid {
  display: flex;
  flex-wrap: wrap;
  padding: 10rpx 12rpx 0;
}

.dchip {
  display: flex;
  align-items: center;
  border: 1rpx solid $border-color;
  background: $bg-card;
  border-radius: $radius-full;
  padding: 14rpx 24rpx;
  margin: 0 12rpx 16rpx 16rpx;
  transition: all $duration-fast $ease-default;

  &__name {
    font-size: $font-caption;
    color: $text-secondary;
    margin-right: 8rpx;
  }

  &__amount {
    font-size: $font-small;
    color: $text-tertiary;
  }

  &--on {
    border-color: #0EA5E9;
    background: rgba(56, 189, 248, 0.08);

    .dchip__name {
      color: #0284C7;
      font-weight: $font-weight-semibold;
    }

    .dchip__amount {
      color: #0284C7;
    }
  }

  &--blocked {
    opacity: 0.45;
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
    font-size: 60rpx;
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
    font-size: 28rpx;
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

/* 年终奖方式提示 */
.bonus-tip {
  border-top: 1rpx solid $border-light;
  padding: 20rpx 28rpx;
  background: rgba(74, 222, 128, 0.08);

  &__text {
    font-size: $font-caption;
    color: $success-dark;
  }
}

/* 税率表 */
.tier-card {
  margin-top: $spacing-md;
}

.tier-table {
  padding: 8rpx 28rpx 16rpx;
}

.tier-row {
  display: flex;
  align-items: center;
  padding: 12rpx 0;
  font-size: $font-small;
  color: $text-tertiary;

  & + .tier-row {
    border-top: 1rpx solid $border-light;
  }

  &--head {
    color: $text-tertiary;
    font-size: $font-mini;
  }

  &--current {
    background: rgba(56, 189, 248, 0.08);
    color: #0284C7;
    font-weight: $font-weight-semibold;
  }
}

.tier-col {
  &--range {
    flex: 1.8;
  }

  &--rate {
    flex: 0.8;
    text-align: center;
  }

  &--quick {
    flex: 1;
    text-align: right;
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
