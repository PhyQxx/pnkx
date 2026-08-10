<template>
  <view class="bk-page">
    <!-- Header -->
    <view class="bk-header">
      <view class="bk-header__bg">
        <view class="bk-header__wave"></view>
      </view>
      <view class="bk-header__content">
        <view class="bk-header__month">
          <text class="bk-header__month-num">{{ currentMonth }}</text>
          <text class="bk-header__month-unit">月</text>
          <text class="bk-header__month-label">本月账单</text>
        </view>
        <view class="bk-header__summary">
          <view class="bk-header__summary-item">
            <view class="bk-header__dot bk-header__dot--expense"></view>
            <text class="bk-header__summary-label">支出</text>
            <text class="bk-header__summary-value bk-header__summary-value--expense">{{ moneyFilter(expenditure) }}</text>
          </view>
          <view class="bk-header__summary-item">
            <view class="bk-header__dot bk-header__dot--income"></view>
            <text class="bk-header__summary-label">收入</text>
            <text class="bk-header__summary-value bk-header__summary-value--income">{{ moneyFilter(income) }}</text>
          </view>
          <view class="bk-header__summary-item">
            <view class="bk-header__dot bk-header__dot--balance"></view>
            <text class="bk-header__summary-label">结余</text>
            <text class="bk-header__summary-value bk-header__summary-value--balance">{{ moneyFilter(income - expenditure) }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- Menu Cards -->
    <view class="bk-menu">
      <view
        class="bk-menu__card"
        v-for="menu in menuList"
        :key="menu.id"
        @click="handleMenuClick(menu)"
      >
        <view class="bk-menu__icon" :style="{ background: menu.gradient }">
          <uni-icons :type="menu.icon" size="24" color="#ffffff" />
        </view>
        <view class="bk-menu__text">
          <text class="bk-menu__name">{{ menu.name }}</text>
          <text class="bk-menu__desc">{{ menu.desc }}</text>
        </view>
        <view class="bk-menu__arrow">
          <uni-icons type="right" size="16" color="#9BA8B7" />
        </view>
      </view>
    </view>

    <!-- Recent Records -->
    <view class="bk-recent" v-if="recentRecords.length > 0">
      <view class="bk-recent__header">
        <text class="bk-recent__title">最近记录</text>
        <text class="bk-recent__link" @click="handleMenuClick({ path: '/pages_life/bookkeeping/record/index' })">查看全部</text>
      </view>
      <view class="bk-recent__list">
        <view
          class="bk-recent__item"
          v-for="record in recentRecords"
          :key="record.id"
          @click="handleEditRecord(record)"
        >
          <view class="bk-recent__icon-wrap">
            <svg-icon :icon-class="getRecordIcon(record)" size="36rpx" />
          </view>
          <view class="bk-recent__info">
            <text class="bk-recent__type">{{ getTypeName(record) }}</text>
            <text class="bk-recent__time">{{ formatRecentTime(record.payTime) }}</text>
          </view>
          <text class="bk-recent__amount" :class="'bk-recent__amount--' + getAmountClass(record)">
            {{ getAmountPrefix(record) }}{{ moneyFilter(record.money) }}
          </text>        </view>
      </view>
    </view>

    <!-- FAB -->
    <view class="bk-fab" @click="handleQuickRecord">
      <view class="bk-fab__btn">
        <uni-icons type="compose" size="28" color="#ffffff" />
      </view>
      <text class="bk-fab__label">记一笔</text>
    </view>
  </view>
</template>

<script>
import { listRecord } from "@/api/px/life/bookkeeping/record";
import { moneyFilter } from "@/utils/filters";

export default {
  name: "BookkeepingIndex",
  filters: { moneyFilter },
  data() {
    return {
      currentMonth: new Date().getMonth() + 1,
      expenditure: 0,
      income: 0,
      recentRecords: [],
      menuList: [
        {
          id: 1,
          name: '记一笔',
          desc: '快速记账',
          icon: 'compose',
          theme: 'primary',
          gradient: 'linear-gradient(135deg, #6C9EFF, #4A7ADB)',
          path: '/pages_life/bookkeeping/record/add'
        },
        {
          id: 2,
          name: '账目清单',
          desc: '查看流水',
          icon: 'list',
          theme: 'success',
          gradient: 'linear-gradient(135deg, #4ADE80, #22C55E)',
          path: '/pages_life/bookkeeping/record/index'
        },
        {
          id: 3,
          name: '账户管理',
          desc: '资产统计',
          icon: 'wallet',
          theme: 'warning',
          gradient: 'linear-gradient(135deg, #FBBF24, #F59E0B)',
          path: '/pages_life/bookkeeping/account/index'
        },
        {
          id: 4,
          name: '统计分析',
          desc: '图表报表',
          icon: 'bars',
          theme: 'purple',
          gradient: 'linear-gradient(135deg, #A78BFA, #8B5CF6)',
          path: '/pages_life/bookkeeping/statistics/index'
        }
      ]
    };
  },
  onLoad() {
    this.getMonthSummary();
  },
  onShow() {
    this.getMonthSummary();
  },
  methods: {
    getMonthSummary() {
      const now = new Date();
      const year = now.getFullYear();
      const month = String(now.getMonth() + 1).padStart(2, '0');
      const payTime = `${year}-${month}`;

      listRecord({ payTime }).then(res => {
        this.expenditure = this.$arraySum(
          res.rows.filter(r => r.typeObject && r.typeObject.typeDifference === '1'),
          'money'
        );
        this.income = this.$arraySum(
          res.rows.filter(r => r.typeObject && r.typeObject.typeDifference === '0'),
          'money'
        );
        this.recentRecords = (res.rows || []).slice(0, 5);
      });
    },
    handleMenuClick(menu) {
      this.$tab.navigateTo(menu.path);
    },
    handleQuickRecord() {
      this.$tab.navigateTo('/pages_life/bookkeeping/record/add');
    },
    getRecordIcon(record) {
      return (record.typeObject && record.typeObject.typeIcon) || 'moren';
    },
    getTypeName(record) {
      return record.typeObject && record.typeObject.typeName;
    },
    getAmountClass(record) {
      const diff = record.typeObject && record.typeObject.typeDifference;
      if (diff === '1') return 'expenditure';
      if (diff === '0') return 'income';
      return 'transfer';
    },
    getAmountPrefix(record) {
      const diff = record.typeObject && record.typeObject.typeDifference;
      return diff === '1' ? '-' : '+';
    },
    formatRecentTime(payTime) {
      if (!payTime) return '';
      const d = new Date(payTime);
      const now = new Date();
      const diffDays = Math.floor((now.getTime() - d.getTime()) / (1000 * 60 * 60 * 24));
      if (diffDays === 0) return '今天';
      if (diffDays === 1) return '昨天';
      if (diffDays < 7) return diffDays + '天前';
      return `${d.getMonth() + 1}/${d.getDate()}`;
    },
    handleEditRecord(record) {
      this.$tab.navigateTo(`/pages_life/bookkeeping/record/add?recordId=${record.id}`);
    }
  }
};
</script>

<style lang="scss" scoped>
.bk-page {
  min-height: 100vh;
  background-color: $bg-page;
  padding-bottom: 200rpx;
}

/* ============================
   Header
   ============================ */
.bk-header {
  position: relative;
  padding: $spacing-2xl $page-padding;
  padding-bottom: $spacing-3xl;

  &__bg {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 360rpx;
    background: linear-gradient(135deg, $primary 0%, $primary-dark 100%);
    border-radius: 0 0 $radius-2xl $radius-2xl;
    overflow: hidden;
  }

  &__wave {
    position: absolute;
    bottom: -2rpx;
    left: -10%;
    width: 120%;
    height: 48rpx;
    background: $bg-page;
    border-radius: 50% 50% 0 0;
  }

  &__content {
    position: relative;
    z-index: $z-card;
  }

  &__month {
    display: flex;
    align-items: baseline;
    margin-bottom: $spacing-lg;
  }

  &__month-num {
    font-size: $font-display;
    font-weight: $font-weight-bold;
    color: $text-inverse;
    line-height: $line-height-tight;
  }

  &__month-unit {
    font-size: $font-h2;
    font-weight: $font-weight-medium;
    color: rgba(255, 255, 255, 0.9);
    margin-left: $spacing-2xs;
    margin-right: $spacing-sm;
  }

  &__month-label {
    font-size: $font-body;
    color: rgba(255, 255, 255, 0.75);
  }

  &__summary {
    display: flex;
    background: rgba(255, 255, 255, 0.15);
    border-radius: $spacing-md;
    padding: $spacing-lg 0;
    backdrop-filter: blur(10px);
  }

  &__summary-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    border-right: 1rpx solid rgba(255, 255, 255, 0.2);

    &:last-child {
      border-right: none;
    }
  }

  &__dot {
    width: 12rpx;
    height: 12rpx;
    border-radius: $radius-full;
    margin-bottom: $spacing-xs;

    &--expense {
      background: $danger-light;
    }

    &--income {
      background: $success-light;
    }

    &--balance {
      background: $warning-light;
    }
  }

  &__summary-label {
    font-size: $font-caption;
    color: rgba(255, 255, 255, 0.8);
    margin-bottom: $spacing-xs;
  }

  &__summary-value {
    font-size: $font-h3;
    font-weight: $font-weight-semibold;

    &--expense {
      color: #FEF2F2;
    }

    &--income {
      color: #ECFDF5;
    }

    &--balance {
      color: #FFFBEB;
    }
  }
}

/* ============================
   Menu Cards
   ============================ */
.bk-menu {
  padding: 0 $page-padding;
  margin-top: -$spacing-md;

  &__card {
    display: flex;
    align-items: center;
    background: $bg-card;
    border-radius: $spacing-md;
    padding: $spacing-lg $spacing-lg;
    margin-bottom: $spacing-sm;
    box-shadow: $shadow-card;
    transition: transform $duration-fast $ease-default;

    &:active {
      transform: scale(0.96);
    }
  }

  &__icon {
    width: 80rpx;
    height: 80rpx;
    border-radius: $spacing-md;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &__text {
    flex: 1;
    margin-left: $spacing-md;
    display: flex;
    flex-direction: column;
  }

  &__name {
    font-size: $font-body;
    font-weight: $font-weight-medium;
    color: $text-primary;
    line-height: $line-height-normal;
  }

  &__desc {
    font-size: $font-caption;
    color: $text-tertiary;
    line-height: $line-height-normal;
    margin-top: $spacing-2xs;
  }

  &__arrow {
    flex-shrink: 0;
    margin-left: $spacing-sm;
  }
}

/* ============================
   Recent Records
   ============================ */
.bk-recent {
  margin-top: $spacing-lg;
  padding: 0 $page-padding;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: $spacing-md;
  }

  &__title {
    font-size: $font-body;
    font-weight: $font-weight-semibold;
    color: $text-primary;
  }

  &__link {
    font-size: $font-caption;
    color: $primary;
  }

  &__list {
    background: $bg-card;
    border-radius: $spacing-md;
    box-shadow: $shadow-card;
    overflow: hidden;
  }

  &__item {
    display: flex;
    align-items: center;
    padding: $spacing-md $spacing-lg;
    border-bottom: 1rpx solid $border-light;

    &:last-child {
      border-bottom: none;
    }

    &:active {
      background: $bg-hover;
    }
  }

  &__icon-wrap {
    width: 64rpx;
    height: 64rpx;
    border-radius: $radius-full;
    background: $bg-page;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &__info {
    flex: 1;
    margin-left: $spacing-md;
    display: flex;
    flex-direction: column;
  }

  &__type {
    font-size: $font-body;
    font-weight: $font-weight-medium;
    color: $text-primary;
    line-height: $line-height-normal;
  }

  &__time {
    font-size: $font-caption;
    color: $text-tertiary;
    line-height: $line-height-normal;
    margin-top: $spacing-2xs;
  }

  &__amount {
    flex-shrink: 0;
    font-size: $font-body;
    font-weight: $font-weight-semibold;
    margin-left: $spacing-sm;

    &--expenditure {
      color: $danger;
    }

    &--income {
      color: $success-dark;
    }

    &--transfer {
      color: $primary;
    }
  }
}

/* ============================
   FAB
   ============================ */
.bk-fab {
  position: fixed;
  bottom: 120rpx;
  right: $page-padding;
  z-index: $z-fixed;
  display: flex;
  flex-direction: column;
  align-items: center;

  &__btn {
    width: 112rpx;
    height: 112rpx;
    border-radius: $radius-full;
    background: linear-gradient(135deg, $primary 0%, $primary-dark 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: $shadow-md;
    transition: transform $duration-fast $ease-default;

    &:active {
      transform: scale(0.96);
    }
  }

  &__label {
    margin-top: $spacing-xs;
    font-size: $font-caption;
    color: $text-secondary;
    font-weight: $font-weight-medium;
  }
}
</style>
