<template>
  <view class="bk-page subpage-shell">
    <view class="bk-total">
      <view class="bk-total__label">总资产 (CNY)</view>
      <view class="bk-total__number">{{ moneyFilter(totalAssets) }}</view>
      <view class="bk-total__breakdown" v-if="assetBreakdown.length > 0">
        <view
          v-for="item in assetBreakdown"
          :key="item.label"
          class="bk-total__legend"
        >
          <view class="bk-total__dot" :style="{ background: item.color }"></view>
          <text class="bk-total__legend-text">{{ item.label }}</text>
        </view>
      </view>
    </view>

    <view class="bk-list">
      <view class="bk-part" v-for="item in accountInfoList" :key="item.dictValue">
        <view class="bk-part__header">
          <view class="bk-part__accent" :style="{ background: getTypeGradient(item.dictValue) }"></view>
          <text class="bk-part__label">{{ item.dictLabel }}</text>
          <text class="bk-part__balance">资产：{{ moneyFilter($arraySum(item.accountList, 'balance')) }}</text>
        </view>
        <view
          v-for="account in item.accountList"
          :key="account.id"
          class="bk-card"
          @click="handleDetail(account.id)"
        >
          <view class="bk-card__icon" :style="{ background: getTypeGradient(item.dictValue) }">
            <svg-icon :icon-class="account.accountIcon || 'moren'" size="40rpx"/>
          </view>
          <view class="bk-card__info">
            <text class="bk-card__name">{{ account.accountName }}</text>
            <text class="bk-card__remark">{{ account.remark || '暂无备注' }}</text>
          </view>
          <view class="bk-card__balance">{{ moneyFilter(account.balance) }}</view>
        </view>
      </view>
    </view>

    <view class="bk-fab" @click="handleAdd">
      <uni-icons type="plus" size="28" color="#fff"/>
    </view>
  </view>
</template>

<script>
import { listAccount } from "@/api/px/life/bookkeeping/account";
import { getDicts } from "@/api/system/dict/data";
import { moneyFilter } from "@/utils/filters";

export default {
  name: "AccountIndex",
  data() {
    return {
      totalAssets: 0,
      accountInfoList: []
    };
  },
  computed: {
    assetBreakdown() {
      const colors = ['#4F86F7', '#4ADE80', '#FBBF24', '#A78BFA'];
      return this.accountInfoList.map((item, index) => ({
        label: item.dictLabel,
        color: colors[index % colors.length],
        total: this.$arraySum(item.accountList, 'balance')
      }));
    }
  },
  onLoad() {
    this.getAccountList();
  },
  onShow() {
    this.getAccountList();
  },
  onNavigationBarButtonTap() {
    this.handleAdd();
  },
  methods: {
    async getAccountList() {
      try {
        const res = await listAccount();
        const accountList = res.rows || [];

        const dictRes = await getDicts('px_bookkeeping_account_type');
        const accountInfo = dictRes.data.map(r => ({
          dictValue: r.dictValue,
          dictLabel: r.dictLabel,
          accountList: []
        }));

        this.totalAssets = 0;
        accountList.forEach(item => {
          this.totalAssets += parseInt(item.balance) || 0;
          const group = accountInfo.find(a => a.dictValue === item.accountType);
          if (group) {
            group.accountList.push(item);
          }
        });

        this.accountInfoList = accountInfo;
      } catch (e) {
        console.error('获取账户列表失败', e);
      }
    },
    handleDetail(id) {
      this.$tab.navigateTo(`/pages_life/bookkeeping/account/details?accountId=${id}`);
    },
    handleAdd() {
      this.$tab.navigateTo('/pages_life/bookkeeping/account/maintenance');
    },
    getTypeGradient(dictValue) {
      const gradients = {
        '1': 'linear-gradient(135deg, #4F86F7, #4A7ADB)',
        '2': 'linear-gradient(135deg, #4ADE80, #22C55E)',
        '3': 'linear-gradient(135deg, #FBBF24, #F59E0B)',
        '4': 'linear-gradient(135deg, #A78BFA, #8B5CF6)',
      };
      return gradients[dictValue] || 'linear-gradient(135deg, #4F86F7, #4A7ADB)';
    }
  },
  filters: {
    moneyFilter
  }
};
</script>

<style lang="scss" scoped>
.bk-page {
  min-height: 100vh;
  background-color: $bg-page;
  padding: $section-gap;

  .bk-total {
    padding: $spacing-lg;
    display: flex;
    flex-direction: column;
    justify-content: flex-end;
    height: 240rpx;
    background: linear-gradient(135deg, $primary 0%, $primary-dark 100%);
    color: $text-inverse;
    font-size: $font-body;
    font-weight: $font-weight-medium;
    border-radius: $radius-lg;
    margin-bottom: $section-gap;
    box-shadow: $shadow-md;

    &__number {
      font-size: $font-h1;
      font-weight: $font-weight-semibold;
      margin-top: $spacing-xs;
    }

    &__breakdown {
      display: flex;
      flex-wrap: wrap;
      gap: $spacing-sm;
      margin-top: $spacing-sm;
    }

    &__legend {
      display: flex;
      align-items: center;
      gap: $spacing-2xs;
    }

    &__dot {
      width: 16rpx;
      height: 16rpx;
      border-radius: $radius-full;
    }

    &__legend-text {
      font-size: $font-small;
      color: rgba(255, 255, 255, 0.85);
    }
  }

  .bk-list {
    .bk-part {
      margin-bottom: $section-gap;

      &__header {
        display: flex;
        align-items: center;
        padding: $spacing-sm $spacing-md;
        background: $bg-card;
        border-radius: $radius-lg $radius-lg 0 0;
      }

      &__accent {
        width: 6rpx;
        height: 32rpx;
        border-radius: $spacing-2xs;
        margin-right: $spacing-sm;
      }

      &__label {
        font-size: $font-body;
        font-weight: $font-weight-medium;
        color: $text-primary;
      }

      &__balance {
        margin-left: auto;
        font-size: $font-caption;
        color: $text-tertiary;
      }
    }
  }

  .bk-card {
    display: flex;
    align-items: center;
    padding: $spacing-md $spacing-lg;
    background: $bg-card;
    border-bottom: 1rpx solid $border-light;
    transition: transform $duration-fast $ease-spring;

    &:last-child {
      border-bottom: none;
      border-radius: 0 0 $radius-lg $radius-lg;
    }

    &:active {
      transform: scale(0.96);
    }

    &__icon {
      width: 72rpx;
      height: 72rpx;
      border-radius: $radius-full;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: $spacing-md;
      color: $text-inverse;
      flex-shrink: 0;
    }

    &__info {
      flex: 1;
      min-width: 0;
    }

    &__name {
      font-size: $font-body;
      font-weight: $font-weight-medium;
      color: $text-primary;
      display: block;
    }

    &__remark {
      font-size: $font-caption;
      color: $text-tertiary;
      display: block;
      margin-top: $spacing-2xs;
    }

    &__balance {
      font-size: $font-h3;
      font-weight: $font-weight-semibold;
      color: $primary;
      flex-shrink: 0;
      margin-left: $spacing-md;
    }
  }

  .bk-fab {
    position: fixed;
    right: $spacing-lg;
    bottom: 120rpx;
    width: 100rpx;
    height: 100rpx;
    border-radius: $radius-full;
    background: linear-gradient(135deg, $primary 0%, $primary-dark 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: $shadow-md;
    transition: transform $duration-fast $ease-spring;

    &:active {
      transform: scale(0.92);
    }
  }
}
</style>
