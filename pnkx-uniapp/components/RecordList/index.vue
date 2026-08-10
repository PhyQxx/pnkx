<template>
  <view class="record-list">
    <no-data v-if="!recordList || recordList.length < 1"/>
    <uni-card v-else v-for="day in recordList" :key="day.id" :title="day.accountName"
              :extra="moneyFilter(day.balance)"
              margin="0"
              class="record-card">
      <template v-slot:title>
        <view class="date">
          <view class="left">
            <span>{{ new Date(day.payTime).getDate() }}</span>
            <span>日 / 周{{ $parseTime(new Date(day.payTime), '{a}') }}</span>
          </view>
          <view class="right">
						<span
              v-if="(day.recordList || []).filter(item => {return item.typeObject && item.typeObject.typeDifference === '0'}).length && $arraySum((day.recordList || []).filter(item => {return item.typeObject && item.typeObject.typeDifference === '0'}), 'money')">
							<svg-icon icon-class="加号"/>
							<span>{{
                  moneyFilter($arraySum((day.recordList || []).filter(item => {
                    return item.typeObject && item.typeObject.typeDifference === '0'
                  }), 'money'))
                }}</span>
						</span>
            <span class="income"
                  v-if="(day.recordList || []).filter(item => {return item.typeObject && item.typeObject.typeDifference === '1'}).length && $arraySum((day.recordList || []).filter(item => {return item.typeObject && item.typeObject.typeDifference === '1'}), 'money')">
							<svg-icon icon-class="减少"/>
							<span>{{
                  moneyFilter($arraySum((day.recordList || []).filter(item => {
                    return item.typeObject && item.typeObject.typeDifference === '1'
                  }), 'money'))
                }}</span>
						</span>
          </view>
        </view>
      </template>
      <view v-for="record in (day.recordList || [])" @click="handleEdit" :key="record.id"
            :data-record="record" :class="{
        'blue': record.typeObject && record.typeObject.typeDifference === '1',
        'red': record.typeObject && record.typeObject.typeDifference === '0',
        'gray': record.typeObject && record.typeObject.typeDifference === '2'
      }" class="record-day">
        <svg-icon class="business-icon" :icon-class="(record.typeObject && record.typeObject.typeIcon) || 'moren'"/>
        <view class="content">
          <view v-if="record.typeObject && record.typeObject.typeDifference === '2'"
                class="transfer">

            <span>{{ record.accountObject && record.accountObject.accountName }}</span>
            <svg-icon class="business-icon" icon-class="右箭头"/>
            <span>{{ record.otherAccountObject && record.otherAccountObject.accountName }}</span>
          </view>
          <view v-else class="transfer">{{ record.typeObject && record.typeObject.typeName }}</view>
          <view class="remark">
            {{ record.remark ? record.remark : '' }}
          </view>
          <view class="money">
            {{ moneyFilter(record.money) }}
          </view>
        </view>
      </view>
    </uni-card>
  </view>
</template>

<script>

export default {
  name: "RecordList",
  props: {
    recordList: {
      type: Array,
      default: () => []
    }
  },
  methods: {
    /**
     * 颜色
     * @param record
     * @returns {string}
     */
    moneyStyle(record) {
      if (record.typeObject && record.typeObject.typeDifference === '1') {
        return 'blue'
      }
      if (record.typeObject && record.typeObject.typeDifference === '0') {
        return 'red'
      }
      if (record.typeObject && record.typeObject.typeDifference === '2') {
        return 'gray'
      }
    },
    /**
     * 跳转编辑
     */
    handleEdit(e) {
      const record = e.currentTarget.dataset.record
      if (record?.typeObject?.typeDifference === '3') {
        uni.showToast({
          title: '抱歉，余额变更不可编辑',
          icon: 'none'
        })
      } else {
        uni.navigateTo({
          url: '/pages_life/bookkeeping/record/add?recordId=' + record.id
        })
      }
    },
  }
}
</script>

<style scoped lang="scss">
.record-list {
  .record-card {

    margin: 1rem 0 !important;
    padding: 0 !important;

    :deep(.uni-card) {
      margin: 0 !important;
    }

    .date {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .transfer {
      width: 60vw !important;
    }

    .blue {
      .money {
        color: $primary !important;
      }
    }

    .red {
      .money {
        color: $danger !important;
      }
    }

    .gray {
      .money {
        color: $text-tertiary !important;
      }
    }

    .date {
      display: flex;
      align-items: center;
      padding: 1rem 1rem 0.5rem 1rem;
      border-bottom: 1px solid $primary;

      .left {
        span:nth-child(1) {
          font-size: 1.2rem;
          font-weight: bold;
        }

        span:nth-child(2) {
          color: $text-tertiary;
          font-size: 0.8rem;
        }
      }

      .right {
        font-size: 0.9rem;
        color: $text-tertiary;

        .income {
          margin-left: 0.5rem;
        }

        span {
          .svg-icon {
            margin-right: 0.2rem;
          }
        }
      }

    }

    .record-day {
      padding: 0.5rem;
      display: flex;
      align-items: center;
      justify-content: space-between;
      border-bottom: 1px solid $border-color;

      &:last-child {
        border-bottom: 0;
      }

      .content {
        width: 75vw;
        display: flex;
        align-items: center;
        justify-content: space-between;

        .content {
          margin-left: 0.5rem;

          .remark {
            font-size: 0.8rem;
            color: $text-tertiary;
          }
        }

        .transfer {
        }

        .remark {
          flex: 1;
          white-space: nowrap;
        }

        .money {
          width: 5rem;
          text-align: right;
        }
      }

      .van-cell__value {
        font-weight: bold;
        color: $primary;
      }

    }
  }
}
</style>