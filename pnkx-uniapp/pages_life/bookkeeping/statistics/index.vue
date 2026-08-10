<template>
  <view class="statistics">
    <view class="header">
      <view class="month-selector">
        <view class="month-selector__arrow" @click="changeMonth(-1)">
          <view class="month-selector__btn">
            <uni-icons type="left" size="16" color="#ffffff"/>
          </view>
        </view>
        <text class="month-selector__text">{{ currentMonthStr }}</text>
        <view class="month-selector__arrow" @click="changeMonth(1)">
          <view class="month-selector__btn">
            <uni-icons type="right" size="16" color="#ffffff"/>
          </view>
        </view>
      </view>
      <view class="summary">
        <view class="summary__item">
          <text class="summary__label">收入</text>
          <text class="summary__value summary__value--income">{{ moneyFilter(income) }}</text>
        </view>
        <view class="summary__item">
          <text class="summary__label">支出</text>
          <text class="summary__value summary__value--expense">{{ moneyFilter(expenditure) }}</text>
        </view>
      </view>
    </view>

    <view class="trend-cards" v-if="prevIncome !== null">
      <view class="trend-card">
        <text class="trend-card__label">较上月支出</text>
        <view class="trend-card__value" :class="expenseTrend > 0 ? 'trend-card__value--up' : 'trend-card__value--down'">
          <uni-icons :type="expenseTrend > 0 ? 'up' : 'down'" size="12" :color="expenseTrend > 0 ? '#FF6B6B' : '#4ADE80'"/>
          <text>{{ Math.abs(expenseTrend) }}%</text>
        </view>
      </view>
      <view class="trend-card">
        <text class="trend-card__label">较上月收入</text>
        <view class="trend-card__value" :class="incomeTrend > 0 ? 'trend-card__value--up' : 'trend-card__value--down'">
          <uni-icons :type="incomeTrend > 0 ? 'up' : 'down'" size="12" :color="incomeTrend > 0 ? '#4ADE80' : '#FF6B6B'"/>
          <text>{{ Math.abs(incomeTrend) }}%</text>
        </view>
      </view>
    </view>

    <view class="chart-section">
      <view class="chart-tabs">
        <view
          v-for="tab in chartTabs"
          :key="tab.value"
          class="chart-tabs__item"
          :class="{ 'chart-tabs__item--active': activeTab === tab.value }"
          @click="activeTab = tab.value"
        >
          {{ tab.label }}
        </view>
      </view>

      <view v-if="activeTab === 'line'" class="chart-content">
        <view class="bar-chart">
          <view class="bar-chart__grid">
            <view class="bar-chart__grid-line" v-for="i in 4" :key="i"></view>
          </view>
          <view class="bar-chart__bars">
            <view class="bar-chart__item" v-for="(item, index) in lineData" :key="index">
              <view class="bar-chart__pair">
                <view class="bar-chart__bar bar-chart__bar--income" :style="{ height: getBarHeight(item.inflowMoney) }"></view>
                <view class="bar-chart__bar bar-chart__bar--expense" :style="{ height: getBarHeight(item.flowOutMoney) }"></view>
              </view>
              <text class="bar-chart__label">{{ item.date }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-if="activeTab === 'pie'" class="chart-content">
        <view class="donut-chart" v-if="pieData.length > 0">
          <view class="donut-chart__ring" :style="pieGradientStyle">
            <view class="donut-chart__center">
              <text class="donut-chart__total">{{ moneyFilter(totalExpense) }}</text>
              <text class="donut-chart__label">总支出</text>
            </view>
          </view>
        </view>
        <view class="pie-legend">
          <view class="pie-legend__item" v-for="(item, index) in pieData" :key="index">
            <view class="pie-legend__dot" :style="{ backgroundColor: colors[index % colors.length] }"></view>
            <text class="pie-legend__name">{{ item.name }}</text>
            <text class="pie-legend__value">{{ moneyFilter(item.value) }}</text>
            <text class="pie-legend__percent">{{ getPercent(item.value) }}%</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getLineChart, getPrimaryStatistics } from "@/api/px/life/bookkeeping/statistics";
import { listRecord } from "@/api/px/life/bookkeeping/record";
import { moneyFilter } from "@/utils/filters";

export default {
  name: "StatisticsIndex",
  data() {
    return {
      currentDate: new Date(),
      income: 0,
      expenditure: 0,
      prevIncome: null,
      prevExpenditure: null,
      activeTab: 'line',
      activeRange: 'month',
      timeRanges: [
        { label: '月', value: 'month' }
      ],
      chartTabs: [
        { label: '趋势图', value: 'line' },
        { label: '分类图', value: 'pie' }
      ],
      lineData: [],
      pieData: [],
      colors: ['#6C9EFF', '#FF6B6B', '#4ADE80', '#FBBF24', '#A78BFA', '#F472B6', '#34D399', '#60A5FA']
    };
  },
  computed: {
    currentMonthStr() {
      const date = this.currentDate;
      return `${date.getFullYear()}年${date.getMonth() + 1}月`;
    },
    pieGradientStyle() {
      const total = this.pieData.reduce((sum, item) => sum + (item.value || 0), 0);
      if (total === 0) return {};
      let stops = '';
      let cumulative = 0;
      this.pieData.forEach((item, i) => {
        const percent = ((item.value || 0) / total) * 100;
        const color = this.colors[i % this.colors.length];
        stops += `${color} ${cumulative}% ${cumulative + percent}%, `;
        cumulative += percent;
      });
      return { background: `conic-gradient(${stops.slice(0, -2)})` };
    },
    totalExpense() {
      return this.pieData.reduce((sum, item) => sum + (item.value || 0), 0);
    },
    expenseTrend() {
      if (this.prevExpenditure === null || this.prevExpenditure === 0) return 0;
      return Math.round(((this.expenditure - this.prevExpenditure) / this.prevExpenditure) * 100);
    },
    incomeTrend() {
      if (this.prevIncome === null || this.prevIncome === 0) return 0;
      return Math.round(((this.income - this.prevIncome) / this.prevIncome) * 100);
    }
  },
  onLoad() {
    this.loadData();
  },
  onShow() {
    this.loadData();
  },
  methods: {
    async loadData() {
      await Promise.all([
        this.getLineChart(),
        this.getPieChart(),
        this.loadPrevMonth()
      ]);
    },
    changeMonth(delta) {
      const newDate = new Date(this.currentDate);
      newDate.setMonth(newDate.getMonth() + delta);
      this.currentDate = newDate;
      this.loadData();
    },
    async loadPrevMonth() {
      try {
        const prevDate = new Date(this.currentDate);
        prevDate.setMonth(prevDate.getMonth() - 1);
        const year = prevDate.getFullYear();
        const month = String(prevDate.getMonth() + 1).padStart(2, '0');
        const payTime = `${year}-${month}`;
        const res = await listRecord({ payTime });
        const rows = res.rows || [];
        this.prevExpenditure = rows.reduce((sum, r) => {
          return sum + (r.typeObject && r.typeObject.typeDifference === '1' ? r.money : 0);
        }, 0);
        this.prevIncome = rows.reduce((sum, r) => {
          return sum + (r.typeObject && r.typeObject.typeDifference === '0' ? r.money : 0);
        }, 0);
      } catch (e) {
        this.prevExpenditure = null;
        this.prevIncome = null;
      }
    },
    async getLineChart() {
      try {
        const payTime = this.$parseTime(this.currentDate);
        const res = await getLineChart({ payTime });
        this.lineData = res.data || [];

        this.income = 0;
        this.expenditure = 0;
        this.lineData.forEach(item => {
          this.income += item.inflowMoney || 0;
          this.expenditure += item.flowOutMoney || 0;
        });
      } catch (e) {
        console.error('获取折线图失败', e);
      }
    },
    async getPieChart() {
      try {
        const params = {
          date: this.$parseTime(this.currentDate),
          dimension: 'primaryFlowOut',
          typeDifference: '1'
        };
        const res = await getPrimaryStatistics(params);
        this.pieData = res.data || [];
      } catch (e) {
        console.error('获取饼图失败', e);
      }
    },
    getBarHeight(value) {
      const maxValue = Math.max(
        ...this.lineData.map(d => Math.max(d.inflowMoney || 0, d.flowOutMoney || 1))
      );
      if (maxValue === 0) return '0rpx';
      const height = ((value || 0) / maxValue) * 240;
      return `${Math.max(height, 8)}rpx`;
    },
    getPercent(value) {
      const total = this.pieData.reduce((sum, item) => sum + (item.value || 0), 0);
      if (total === 0) return '0';
      return ((value / total) * 100).toFixed(1);
    }
  },
  filters: {
    moneyFilter
  }
};
</script>

<style lang="scss" scoped>
.statistics {
  min-height: 100vh;
  background: $bg-page;

  .header {
    background: linear-gradient(135deg, $primary 0%, $primary-dark 100%);
    padding: $spacing-lg;
    color: $text-inverse;

    .month-selector {
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: $spacing-lg;

      &__arrow {
        padding: $spacing-sm;
      }

      &__btn {
        width: 56rpx;
        height: 56rpx;
        border-radius: $radius-full;
        background: rgba(255, 255, 255, 0.2);
        display: flex;
        align-items: center;
        justify-content: center;
        transition: transform $duration-fast $ease-default;

        &:active {
          transform: scale(0.9);
        }
      }

      &__text {
        font-size: $font-h1;
        font-weight: $font-weight-semibold;
        margin: 0 $spacing-lg;
        color: $text-inverse;
      }
    }

    .summary {
      display: flex;
      justify-content: space-around;
      padding: $section-gap;
      background: rgba(255, 255, 255, 0.15);
      border-radius: $radius-lg;

      &__item {
        display: flex;
        flex-direction: column;
        align-items: center;
      }

      &__label {
        font-size: $font-caption;
        opacity: 0.8;
        margin-bottom: $spacing-xs;
      }

      &__value {
        font-size: $font-h1;
        font-weight: $font-weight-semibold;

        &--income {
          color: #ECFDF5;
        }

        &--expense {
          color: #FEF2F2;
        }
      }
    }
  }

  .trend-cards {
    display: flex;
    gap: $section-gap;
    padding: $section-gap;
    margin: $section-gap;
    margin-top: -#{$spacing-md};
    border-radius: $radius-lg;

    .trend-card {
      flex: 1;
      background: $bg-card;
      border-radius: $radius-lg;
      padding: $spacing-md $spacing-lg;
      box-shadow: $shadow-card;

      &__label {
        font-size: $font-caption;
        color: $text-tertiary;
        display: block;
        margin-bottom: $spacing-xs;
      }

      &__value {
        display: flex;
        align-items: center;
        gap: $spacing-2xs;
        font-size: $font-h2;
        font-weight: $font-weight-semibold;

        &--up {
          color: $danger;
        }

        &--down {
          color: $success;
        }
      }
    }
  }

  .chart-section {
    background: $bg-card;
    margin: $section-gap;
    border-radius: $radius-lg;
    padding: $section-gap;
    box-shadow: $shadow-card;

    .chart-tabs {
      display: flex;
      margin-bottom: $spacing-lg;
      border-bottom: 1rpx solid $border-color;

      &__item {
        flex: 1;
        text-align: center;
        padding: $spacing-sm;
        font-size: $font-body;
        color: $text-secondary;
        position: relative;
        transition: color $duration-fast $ease-default;

        &--active {
          color: $primary-dark;
          font-weight: $font-weight-medium;

          &::after {
            content: '';
            position: absolute;
            bottom: 0;
            left: 50%;
            transform: translateX(-50%);
            width: 48rpx;
            height: 4rpx;
            background: $primary;
            border-radius: $spacing-2xs;
          }
        }
      }
    }

    .chart-content {
      min-height: 400rpx;
    }

    .bar-chart {
      position: relative;
      height: 320rpx;

      &__grid {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 40rpx;
        display: flex;
        flex-direction: column;
        justify-content: space-between;

        &-line {
          height: 1rpx;
          background: $border-light;
        }
      }

      &__bars {
        position: relative;
        z-index: $z-base;
        display: flex;
        justify-content: space-around;
        align-items: flex-end;
        height: 280rpx;
        padding: 0 $spacing-sm;
      }

      &__item {
        display: flex;
        flex-direction: column;
        align-items: center;
        flex: 1;
      }

      &__pair {
        display: flex;
        gap: 6rpx;
        align-items: flex-end;
      }

      &__bar {
        width: 28rpx;
        border-radius: 14rpx 14rpx 0 0;
        min-height: 8rpx;
        transition: height $duration-normal $ease-spring;

        &--income {
          background: $success;
        }

        &--expense {
          background: $primary;
        }
      }

      &__label {
        font-size: $font-mini;
        color: $text-tertiary;
        margin-top: $spacing-xs;
        text-align: center;
      }
    }

    .donut-chart {
      display: flex;
      justify-content: center;
      padding: $spacing-xl 0;

      &__ring {
        width: 300rpx;
        height: 300rpx;
        border-radius: $radius-full;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      &__center {
        width: 180rpx;
        height: 180rpx;
        border-radius: $radius-full;
        background: $bg-card;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
      }

      &__total {
        font-size: $font-h2;
        font-weight: $font-weight-semibold;
        color: $text-primary;
      }

      &__label {
        font-size: $font-mini;
        color: $text-tertiary;
      }
    }

    .pie-legend {
      padding: 0 $spacing-sm;

      &__item {
        display: flex;
        align-items: center;
        padding: $spacing-sm 0;
        border-bottom: 1rpx solid $border-light;

        &:last-child {
          border-bottom: none;
        }
      }

      &__dot {
        width: 24rpx;
        height: 24rpx;
        border-radius: $radius-sm;
        margin-right: $spacing-sm;
        flex-shrink: 0;
      }

      &__name {
        flex: 1;
        font-size: $font-body;
        color: $text-primary;
      }

      &__value {
        font-size: $font-caption;
        color: $text-secondary;
        margin-right: $spacing-sm;
      }

      &__percent {
        font-size: $font-body;
        font-weight: $font-weight-semibold;
        color: $text-primary;
        min-width: 80rpx;
        text-align: right;
      }
    }
  }
}
</style>
