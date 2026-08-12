<template>
  <view class="record-list subpage-shell">
    <view class="header">
      <view class="month-selector">
        <view class="arrow" @click="changeMonth(-1)">
          <view class="arrow__btn">
            <uni-icons type="left" size="18" color="#ffffff"/>
          </view>
        </view>
        <view class="month-text">{{ currentMonthStr }}</view>
        <view class="arrow" @click="changeMonth(1)">
          <view class="arrow__btn">
            <uni-icons type="right" size="18" color="#ffffff"/>
          </view>
        </view>
      </view>
      <view class="summary">
        <view class="summary-item">
          <text class="label">支出</text>
          <text class="value expenditure">{{ moneyFilter(expenditure) }}</text>
        </view>
        <view class="summary-item">
          <text class="label">收入</text>
          <text class="value income">{{ moneyFilter(income) }}</text>
        </view>
      </view>
    </view>

    <scroll-view
      class="list-content"
      scroll-y
      @scrolltolower="loadMore"
      :refresher-enabled="true"
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onRefresh"
    >
      <view v-if="!recordList || recordList.length === 0" class="empty-state">
        <view class="empty-state__icon">
          <svg-icon icon-class="qingdan" size="120rpx"/>
        </view>
        <text class="empty-state__title">暂无记录</text>
        <text class="empty-state__hint">点击下方按钮开始记账</text>
        <view class="empty-state__btn" @click="handleAdd">
          <uni-icons type="plus" size="16" color="#4F86F7"/>
          <text>记一笔</text>
        </view>
      </view>

      <view v-for="day in recordList" :key="day.date" class="day-group">
        <view class="day-header">
          <view class="day-left">
            <text class="day-number">{{ getDayNumber(day.date) }}</text>
            <text class="day-week">{{ getWeekDay(day.date) }}</text>
          </view>
          <view class="day-right">
            <view v-if="day.expenditure > 0" class="day-summary">
              <uni-icons type="minus" size="12" color="#4F86F7"/>
              <text class="expenditure">{{ moneyFilter(day.expenditure) }}</text>
            </view>
            <view v-if="day.income > 0" class="day-summary">
              <uni-icons type="plus" size="12" color="#FF6B6B"/>
              <text class="income">{{ moneyFilter(day.income) }}</text>
            </view>
          </view>
          <view class="day-spending-bar">
            <view class="day-spending-bar__expense" :style="{ width: getExpenseRatio(day) + '%' }"></view>
            <view class="day-spending-bar__income" :style="{ width: getIncomeRatio(day) + '%' }"></view>
          </view>
        </view>

        <uni-swipe-action ref="swipeAction">
          <uni-swipe-action-item
            v-for="record in day.records"
            :key="record.id"
            :right-options="getSwipeOptions(record)"
            @click="handleSwipeClick(record, $event)"
          >
            <view class="record-item" :style="{ borderLeftColor: getCategoryColor(record) }" @click="handleEdit(record)">
              <view class="record-icon">
                <svg-icon :icon-class="getRecordIcon(record)" size="48rpx"/>
              </view>
              <view class="record-info">
                <view class="record-name">
                  <text v-if="isTransfer(record)" class="transfer-text">
                    {{ getAccountName(record) }} → {{ getOtherAccountName(record) }}
                  </text>
                  <text v-else>{{ getTypeName(record) }}</text>
                </view>
                <text class="record-remark">{{ record.remark || '无备注' }}</text>
              </view>
              <view class="record-amount" :class="record.typeObject && record.typeObject.typeDifference === '1' ? 'expenditure' : (record.typeObject && record.typeObject.typeDifference === '0' ? 'income' : 'transfer')">
                {{ getAmountPrefix(record) }}{{ moneyFilter(record.money) }}
              </view>
            </view>
          </uni-swipe-action-item>
        </uni-swipe-action>
      </view>

      <uni-load-more :status="loadStatus"/>
    </scroll-view>

    <view class="fab-btn" @click="handleAdd">
      <uni-icons type="plus" size="28" color="#fff"/>
    </view>
  </view>
</template>

<script>
import { listRecord, delRecord } from "@/api/px/life/bookkeeping/record";

export default {
  name: "RecordList",
  data() {
    return {
      currentMonth: new Date(),
      recordList: [],
      expenditure: 0,
      income: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        payTime: ''
      },
      total: 0,
      loadStatus: 'more',
      isRefreshing: false,
      categoryColors: ['#4F86F7', '#FF6B6B', '#4ADE80', '#FBBF24', '#A78BFA', '#F472B6', '#34D399', '#60A5FA'],
      swipeOptions: [
        { text: '编辑', style: { backgroundColor: '#4F86F7' } },
        { text: '删除', style: { backgroundColor: '#FF6B6B' } }
      ]
    };
  },
  computed: {
    currentMonthStr() {
      const date = this.currentMonth;
      return `${date.getFullYear()}年${date.getMonth() + 1}月`;
    }
  },
  onLoad() {
    this.getRecordList();
  },
  onShow() {
    this.refreshList();
  },
  methods: {
    async getRecordList() {
      this.loadStatus = 'loading';
      const year = this.currentMonth.getFullYear();
      const month = String(this.currentMonth.getMonth() + 1).padStart(2, '0');
      this.queryParams.payTime = `${year}-${month}`;

      try {
        const res = await listRecord(this.queryParams);
        this.total = res.total || 0;

        const groupedRecords = this.groupByDate(res.rows || []);

        if (this.queryParams.pageNum === 1) {
          this.recordList = groupedRecords;
        } else {
          this.recordList = [...this.recordList, ...groupedRecords];
        }

        this.calculateSummary(res.rows);

        this.loadStatus = this.recordList.length >= this.total ? 'noMore' : 'more';
      } catch (e) {
        this.loadStatus = 'more';
      }
    },
    groupByDate(records) {
      const grouped = {};

      records.forEach(record => {
        const date = this.$parseTime(record.payTime, '{y}-{m}-{d}');
        if (!grouped[date]) {
          grouped[date] = {
            date,
            records: [],
            expenditure: 0,
            income: 0
          };
        }
        grouped[date].records.push(record);

        const typeDiff = record.typeObject && record.typeObject.typeDifference;
        if (typeDiff === '1') {
          grouped[date].expenditure += record.money;
        } else if (typeDiff === '0') {
          grouped[date].income += record.money;
        }
      });

      return Object.values(grouped).sort((a, b) =>
        new Date(b.date).getTime() - new Date(a.date).getTime()
      );
    },
    calculateSummary(records) {
      this.expenditure = this.$arraySum(
        records.filter(r => r.typeObject && r.typeObject.typeDifference === '1'),
        'money'
      );
      this.income = this.$arraySum(
        records.filter(r => r.typeObject && r.typeObject.typeDifference === '0'),
        'money'
      );
    },
    changeMonth(delta) {
      const newMonth = new Date(this.currentMonth);
      newMonth.setMonth(newMonth.getMonth() + delta);
      this.currentMonth = newMonth;
      this.queryParams.pageNum = 1;
      this.getRecordList();
    },
    getDayNumber(date) {
      return new Date(date).getDate();
    },
    getWeekDay(date) {
      const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
      return weekDays[new Date(date).getDay()];
    },
    amountClass(record) {
      const diff = record.typeObject && record.typeObject.typeDifference;
      if (diff === '1') return 'expenditure';
      if (diff === '0') return 'income';
      return 'transfer';
    },
    handleEdit(record) {
      if (record.typeObject && record.typeObject.typeDifference === '3') {
        uni.showToast({ title: '余额变更不可编辑', icon: 'none' });
        return;
      }
      this.$tab.navigateTo(`/pages_life/bookkeeping/record/add?recordId=${record.id}`);
    },
    getRecordIcon(record) {
      if (this.isTransfer(record)) return 'zhuanzhang';
      return (record.typeObject && record.typeObject.typeIcon) || '默认';
    },
    isTransfer(record) {
      // 转账记录 type=0 导致 typeObject 为 null，靠 otherAccount 兜底识别
      return !!record.otherAccount || (record.typeObject && record.typeObject.typeDifference === '2');
    },
    getAccountName(record) {
      return record.accountObject && record.accountObject.accountName;
    },
    getOtherAccountName(record) {
      return record.otherAccountObject && record.otherAccountObject.accountName;
    },
    getTypeName(record) {
      return record.typeObject && record.typeObject.typeName;
    },
    getAmountPrefix(record) {
      if (this.isTransfer(record)) return '';
      const diff = record.typeObject && record.typeObject.typeDifference;
      return diff === '1' ? '-' : '+';
    },
    getSwipeOptions(record) {
      return this.swipeOptions;
    },
    getCategoryColor(record) {
      const name = record.typeObject && record.typeObject.typeName || '';
      let hash = 0;
      for (let i = 0; i < name.length; i++) {
        hash = name.charCodeAt(i) + ((hash << 5) - hash);
      }
      return this.categoryColors[Math.abs(hash) % this.categoryColors.length];
    },
    getExpenseRatio(day) {
      const total = day.expenditure + day.income;
      if (total === 0) return 0;
      return Math.round((day.expenditure / total) * 100);
    },
    getIncomeRatio(day) {
      const total = day.expenditure + day.income;
      if (total === 0) return 0;
      return Math.round((day.income / total) * 100);
    },
    handleAdd() {
      this.$tab.navigateTo('/pages_life/bookkeeping/record/add');
    },
    handleSwipeClick(record, e) {
      const index = e.content.index;
      if (index === 0) {
        this.handleEdit(record);
      } else if (index === 1) {
        uni.showModal({
          title: '删除确认',
          content: '确定删除该记录吗？',
          confirmColor: '#FF6B6B',
          success: async (res) => {
            if (res.confirm) {
              await delRecord(record.id);
              uni.showToast({ title: '删除成功', icon: 'success' });
              this.refreshList();
            }
          }
        });
      }
    },
    async loadMore() {
      if (this.loadStatus !== 'more') return;
      this.queryParams.pageNum++;
      await this.getRecordList();
    },
    async onRefresh() {
      this.isRefreshing = true;
      await this.refreshList();
      this.isRefreshing = false;
    },
    async refreshList() {
      this.queryParams.pageNum = 1;
      await this.getRecordList();
    }
  }
};
</script>

<style lang="scss" scoped>
.record-list {
  min-height: 100vh;
  background-color: $bg-page;
  display: flex;
  flex-direction: column;

  .header {
    background: linear-gradient(135deg, $primary 0%, $primary-dark 100%);
    padding: $spacing-lg;
    color: $text-inverse;
    flex-shrink: 0;

    .month-selector {
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: $spacing-lg;

      .arrow {
        padding: $spacing-sm;
        transition: transform $duration-fast $ease-default;

        &:active {
          transform: scale(0.9);
        }

        &__btn {
          width: 64rpx;
          height: 64rpx;
          border-radius: $radius-full;
          background: rgba(255, 255, 255, 0.2);
          display: flex;
          align-items: center;
          justify-content: center;
        }
      }

      .month-text {
        font-size: $font-h1;
        font-weight: $font-weight-semibold;
        margin: 0 $spacing-lg;
      }
    }

    .summary {
      display: flex;
      justify-content: space-around;
      padding: $section-gap;
      background: rgba(255, 255, 255, 0.15);
      border-radius: $radius-lg;

      .summary-item {
        display: flex;
        flex-direction: column;
        align-items: center;

        .label {
          font-size: $font-caption;
          opacity: 0.8;
          margin-bottom: $spacing-xs;
        }

        .value {
          font-size: $font-h1;
          font-weight: $font-weight-semibold;

          &.expenditure {
            color: #FEF2F2;
          }

          &.income {
            color: #ECFDF5;
          }
        }
      }
    }
  }

  .list-content {
    flex: 1;
    padding: $section-gap;
  }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 120rpx 0;

    &__icon {
      margin-bottom: $spacing-lg;
      opacity: 0.4;
    }

    &__title {
      font-size: $font-h3;
      color: $text-secondary;
      margin-bottom: $spacing-xs;
    }

    &__hint {
      font-size: $font-caption;
      color: $text-tertiary;
      margin-bottom: $spacing-xl;
    }

    &__btn {
      display: flex;
      align-items: center;
      gap: $spacing-xs;
      padding: $spacing-sm $spacing-xl;
      border-radius: $radius-xl;
      background: $bookkeeping-light;
      color: $primary;
      font-size: $font-body;
    }
  }

  .day-group {
    background: $bg-card;
    border-radius: $radius-lg;
    margin-bottom: $section-gap;
    overflow: hidden;
    box-shadow: $shadow-card;

    .day-header {
      display: flex;
      flex-direction: column;
      padding: $section-gap;
      border-bottom: 1rpx solid $border-light;

      .day-left {
        display: flex;
        align-items: baseline;
        gap: $spacing-xs;

        .day-number {
          font-size: $font-display;
          font-weight: $font-weight-semibold;
          color: $text-primary;
        }

        .day-week {
          font-size: $font-caption;
          color: $text-tertiary;
        }
      }

      .day-right {
        display: flex;
        gap: $section-gap;

        .day-summary {
          display: flex;
          align-items: center;
          gap: $spacing-2xs;
          font-size: $font-caption;

          .expenditure {
            color: $primary;
          }

          .income {
            color: $theme-bookkeeping-red;
          }
        }
      }

      .day-spending-bar {
        display: flex;
        height: 6rpx;
        background: rgba(0, 0, 0, 0.05);
        border-radius: $radius-full;
        overflow: hidden;
        margin-top: $spacing-xs;

        &__expense {
          background: $primary;
          border-radius: $radius-full;
          transition: width $duration-normal $ease-spring;
        }

        &__income {
          background: $success;
          border-radius: $radius-full;
          transition: width $duration-normal $ease-spring;
        }
      }
    }

    .record-item {
      display: flex;
      align-items: center;
      padding: $section-gap;
      border-bottom: 1rpx solid $border-light;
      border-left: 6rpx solid transparent;
      transition: background-color $duration-fast $ease-default;

      &:last-child {
        border-bottom: none;
      }

      &:active {
        background-color: $bg-hover;
      }

      .record-icon {
        width: 72rpx;
        height: 72rpx;
        border-radius: $radius-full;
        background: $gray-50;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 20rpx;
      }

      .record-info {
        flex: 1;
        min-width: 0;

        .record-name {
          font-size: $font-h3;
          color: $text-primary;
          margin-bottom: $spacing-xs;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;

          .transfer-text {
            font-size: $font-caption;
            color: $text-secondary;
          }
        }

        .record-remark {
          font-size: $font-caption;
          color: $text-tertiary;
        }
      }

      .record-amount {
        font-size: $font-h2;
        font-weight: $font-weight-semibold;

        &.expenditure {
          color: $primary;
        }

        &.income {
          color: $theme-bookkeeping-red;
        }

        &.transfer {
          color: $text-tertiary;
        }
      }
    }
  }

  .fab-btn {
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
