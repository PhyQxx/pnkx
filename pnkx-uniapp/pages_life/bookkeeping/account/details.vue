<template>
  <view class="details subpage-shell">
    <view class="header-card">
      <view class="account-icon">
        <svg-icon :icon-class="account.accountIcon || '默认'" size="56rpx"/>
      </view>
      <view class="account-info">
        <view class="account-name">{{ account.accountName }}</view>
        <view class="account-balance">余额：{{ moneyFilter(account.balance) }}</view>
        <view class="account-remark">{{ account.remark || '暂无备注' }}</view>
      </view>
    </view>

    <view class="info-list">
      <uni-list>
        <uni-list-item title="账户类型" :rightText="accountTypeName"></uni-list-item>
        <uni-list-item title="创建时间" :rightText="$parseTime(account.createTime, '{y}-{m}-{d}') || '未知'"></uni-list-item>
      </uni-list>
    </view>

    <view class="detail-transactions" v-if="recentTransactions.length > 0">
      <view class="detail-transactions__header">
        <text class="detail-transactions__title">近期流水</text>
      </view>
      <view
        v-for="txn in recentTransactions"
        :key="txn.id"
        class="detail-txn"
        @click="$tab.navigateTo(`/pages_life/bookkeeping/record/add?recordId=${txn.id}`)"
      >
        <view class="detail-txn__icon">
          <svg-icon :icon-class="(txn.typeObject && txn.typeObject.typeIcon) || 'moren'" size="36rpx"/>
        </view>
        <view class="detail-txn__info">
          <text class="detail-txn__name">{{ txn.typeObject && txn.typeObject.typeName || '未知' }}</text>
          <text class="detail-txn__time">{{ $parseTime(txn.payTime, '{m}-{d}') }}</text>
        </view>
        <text
          class="detail-txn__amount"
          :class="txn.typeObject && txn.typeObject.typeDifference === '1' ? 'detail-txn__amount--expense' : 'detail-txn__amount--income'"
        >
          {{ txn.typeObject && txn.typeObject.typeDifference === '1' ? '-' : '+' }}{{ moneyFilter(txn.money) }}
        </text>
      </view>
    </view>

    <view class="actions">
      <button class="btn-edit" @click="handleEdit">编辑</button>
      <button class="btn-delete" @click="handleDelete">删除</button>
    </view>
  </view>
</template>

<script>
import { getAccount, delAccount } from "@/api/px/life/bookkeeping/account";
import { getDicts } from "@/api/system/dict/data";
import { listRecord } from "@/api/px/life/bookkeeping/record";
import { moneyFilter } from "@/utils/filters";

export default {
  name: "AccountDetails",
  data() {
    return {
      accountId: null,
      account: {},
      accountTypeName: '',
      recentTransactions: []
    };
  },
  onLoad(options) {
    if (options.accountId) {
      this.accountId = options.accountId;
      this.getAccountInfo();
    }
  },
  onNavigationBarButtonTap() {
    this.handleEdit();
  },
  methods: {
    async getAccountInfo() {
      try {
        const res = await getAccount(this.accountId);
        this.account = res.data || {};
        try {
          const dictRes = await getDicts('px_bookkeeping_account_type');
          const typeInfo = dictRes.data.find(d => d.dictValue === this.account.accountType);
          this.accountTypeName = typeInfo ? typeInfo.dictLabel : '未知';
        } catch (dictError) {
          this.accountTypeName = this.account.accountType || '未知';
        }
        const txnRes = await listRecord({ account: this.accountId, pageNum: 1, pageSize: 5 });
        this.recentTransactions = txnRes.rows || [];
      } catch (e) {
        uni.showToast({ title: '获取账户信息失败', icon: 'none' });
      }
    },
    handleEdit() {
      this.$tab.navigateTo(`/pages_life/bookkeeping/account/maintenance?accountId=${this.accountId}`);
    },
    handleDelete() {
      uni.showModal({
        title: '删除确认',
        content: '删除该账户将同时删除相关的流水记录，是否继续？',
        confirmColor: '#FF6B6B',
        success: async (res) => {
          if (res.confirm) {
            try {
              await delAccount(this.accountId);
              uni.showToast({ title: '删除成功', icon: 'success' });
              uni.navigateBack();
            } catch (e) {
              uni.showToast({ title: '删除失败', icon: 'none' });
            }
          }
        }
      });
    }
  },
  filters: {
    moneyFilter
  }
};
</script>

<style lang="scss" scoped>
.details {
  min-height: 100vh;
  background: $bg-page;
  padding: $section-gap;

  .header-card {
    background: $bg-card;
    border-radius: $radius-lg;
    padding: $spacing-lg;
    display: flex;
    align-items: center;
    margin-bottom: $section-gap;
    box-shadow: $shadow-card;

    .account-icon {
      width: 120rpx;
      height: 120rpx;
      border-radius: $radius-full;
      background: linear-gradient(135deg, $primary 0%, $primary-dark 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: $section-gap;
      flex-shrink: 0;
    }

    .account-info {
      flex: 1;
      min-width: 0;

      .account-name {
        font-size: $font-h2;
        font-weight: $font-weight-semibold;
        color: $text-primary;
        margin-bottom: $spacing-xs;
      }

      .account-balance {
        font-size: $font-body;
        color: $primary;
        font-weight: $font-weight-medium;
        margin-bottom: $spacing-2xs;
      }

      .account-remark {
        font-size: $font-caption;
        color: $text-tertiary;
      }
    }
  }

  .info-list {
    background: $bg-card;
    border-radius: $radius-lg;
    overflow: hidden;
    margin-bottom: $section-gap;
    box-shadow: $shadow-card;

    ::v-deep .uni-list-item {
      padding: $section-gap $spacing-lg;
    }
  }

  .detail-transactions {
    background: $bg-card;
    border-radius: $radius-lg;
    overflow: hidden;
    margin-bottom: $section-gap;
    box-shadow: $shadow-card;

    &__header {
      padding: $spacing-md $spacing-lg;
      border-bottom: 1rpx solid $border-light;
    }

    &__title {
      font-size: $font-body;
      font-weight: $font-weight-medium;
      color: $text-primary;
    }
  }

  .detail-txn {
    display: flex;
    align-items: center;
    padding: $spacing-md $spacing-lg;
    border-bottom: 1rpx solid $border-light;

    &:last-child {
      border-bottom: none;
    }

    &__icon {
      width: 64rpx;
      height: 64rpx;
      border-radius: $radius-full;
      background: $gray-50;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: $spacing-md;
      color: $primary;
      flex-shrink: 0;
    }

    &__info {
      flex: 1;
      min-width: 0;
    }

    &__name {
      font-size: $font-body;
      color: $text-primary;
      display: block;
    }

    &__time {
      font-size: $font-caption;
      color: $text-tertiary;
      display: block;
      margin-top: $spacing-2xs;
    }

    &__amount {
      font-size: $font-h3;
      font-weight: $font-weight-semibold;
      flex-shrink: 0;
      margin-left: $spacing-md;

      &--expense {
        color: $danger;
      }

      &--income {
        color: $success;
      }
    }
  }

  .actions {
    display: flex;
    gap: $section-gap;
    padding: 0 $section-gap;

    button {
      display: flex;
      align-items: center;
      justify-content: center;
      flex: 1;
      height: 88rpx;
      border-radius: $radius-lg;
      font-size: $font-body;
      border: none;
      transition: transform $duration-fast $ease-default;

      &:active {
        transform: scale(0.96);
      }
    }

    .btn-edit {
      background: linear-gradient(135deg, $primary 0%, $primary-dark 100%);
      color: $text-inverse;
    }

    .btn-delete {
      background: rgba($danger, 0.08);
      color: $danger;
    }
  }
}
</style>
