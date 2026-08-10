<!--
  * @File: RecordCard
  * @Author: PHY
  * @Date: 2025/03/10
  * @Description: 姨妈助手记录卡片组件
-->
<template>
  <view class="record-card" @click="handleClick">
    <view class="record-card__stripe" :class="typeClass"></view>

    <view class="record-card__main">
      <view class="record-card__header">
        <view class="record-card__date-block">
          <text class="record-card__day">{{ dayText }}</text>
          <view class="record-card__date-meta">
            <text class="record-card__month">{{ monthText }}</text>
            <text class="record-card__weekday">{{ weekdayText }}</text>
          </view>
        </view>

        <view class="record-card__tags">
          <view v-if="record.type" class="record-card__tag" :class="typeClass">
            {{ typeText }}
          </view>
          <view v-if="record.makeLove" class="record-card__tag soft">
            同房
          </view>
          <view v-if="record.items" class="record-card__tag check">
            检查
          </view>
        </view>
      </view>

      <view class="record-card__metrics" v-if="record.temperature || record.weight">
        <view v-if="record.temperature" class="record-card__metric">
          <text class="record-card__metric-label">体温</text>
          <text class="record-card__metric-value">{{ record.temperature }}℃</text>
        </view>
        <view v-if="record.weight" class="record-card__metric">
          <text class="record-card__metric-label">体重</text>
          <text class="record-card__metric-value">{{ record.weight }}kg</text>
        </view>
      </view>

      <view v-if="record.items || record.results || record.remark" class="record-card__notes">
        <view v-if="record.items" class="record-card__note">
          <text class="record-card__note-label">检查项目</text>
          <text class="record-card__note-content">{{ record.items }}</text>
        </view>
        <view v-if="record.results" class="record-card__note">
          <text class="record-card__note-label">检查结果</text>
          <text class="record-card__note-content">{{ record.results }}</text>
        </view>
        <view v-if="record.remark" class="record-card__note">
          <text class="record-card__note-label">备注</text>
          <text class="record-card__note-content">{{ record.remark }}</text>
        </view>
      </view>

      <view class="record-card__footer">
        <view class="record-card__action" @click.stop="handleEdit">
          <uni-icons type="compose" size="16" color="#F472B6" />
          <text>编辑</text>
        </view>
        <view class="record-card__action delete" @click.stop="handleDelete">
          <uni-icons type="trash" size="16" color="#FF6B6B" />
          <text>删除</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'RecordCard',
  props: {
    record: {
      type: Object,
      required: true
    }
  },
  computed: {
    recordDate() {
      if (!this.record.date) return ''
      return this.record.date.substring(0, 10)
    },
    dayText() {
      if (!this.recordDate) return '--'
      return this.recordDate.substring(8, 10)
    },
    monthText() {
      if (!this.recordDate) return ''
      const date = new Date(this.recordDate)
      return `${date.getMonth() + 1}月`
    },
    weekdayText() {
      if (!this.recordDate) return ''
      const weekMap = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      return weekMap[new Date(this.recordDate).getDay()]
    },
    typeText() {
      if (this.record.type === '0') return '经期开始'
      if (this.record.type === '1') return '经期结束'
      return '普通记录'
    },
    typeClass() {
      if (this.record.type === '0') return 'start'
      if (this.record.type === '1') return 'end'
      return 'normal'
    }
  },
  methods: {
    handleClick() {
      this.$emit('click', this.record)
    },
    handleEdit() {
      this.$emit('edit', this.record)
    },
    handleDelete() {
      this.$emit('delete', this.record)
    }
  }
}
</script>

<style lang="scss" scoped>
.record-card {
  position: relative;
  display: flex;
  overflow: hidden;
  background: #FFFFFF;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 8rpx 24rpx rgba(244, 114, 182, 0.08);

  &__stripe {
    width: 8rpx;
    flex-shrink: 0;
    background: #D1D8E0;

    &.start {
      background: #F472B6;
    }

    &.end {
      background: #6C9EFF;
    }
  }

  &__main {
    flex: 1;
    min-width: 0;
    padding: 24rpx;
  }

  &__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 20rpx;
  }

  &__date-block {
    display: flex;
    align-items: center;
    gap: 14rpx;
    min-width: 0;
  }

  &__day {
    font-size: 44rpx;
    line-height: 1;
    font-weight: 700;
    color: #1A202C;
  }

  &__date-meta {
    display: flex;
    flex-direction: column;
    gap: 4rpx;
  }

  &__month {
    font-size: 24rpx;
    color: #4A5568;
    font-weight: 600;
  }

  &__weekday {
    font-size: 22rpx;
    color: #9BA8B7;
  }

  &__tags {
    display: flex;
    justify-content: flex-end;
    flex-wrap: wrap;
    gap: 10rpx;
    max-width: 360rpx;
  }

  &__tag {
    padding: 6rpx 14rpx;
    border-radius: 999rpx;
    font-size: 22rpx;
    line-height: 1.2;
    color: #6B7B8D;
    background: #F4F6F9;

    &.start {
      color: #DB2777;
      background: #FCE7F3;
    }

    &.end {
      color: #2563EB;
      background: #DBEAFE;
    }

    &.soft {
      color: #BE185D;
      background: #FFF1F2;
    }

    &.check {
      color: #2563EB;
      background: #EEF6FF;
    }
  }

  &__metrics {
    display: flex;
    gap: 16rpx;
    margin-top: 22rpx;
  }

  &__metric {
    flex: 1;
    min-width: 0;
    padding: 18rpx 20rpx;
    background: #FAFBFC;
    border-radius: 12rpx;
  }

  &__metric-label {
    display: block;
    font-size: 22rpx;
    color: #9BA8B7;
    margin-bottom: 6rpx;
  }

  &__metric-value {
    display: block;
    font-size: 30rpx;
    font-weight: 700;
    color: #2D3748;
  }

  &__notes {
    margin-top: 20rpx;
  }

  &__note {
    display: flex;
    align-items: flex-start;
    gap: 14rpx;
    margin-bottom: 10rpx;

    &:last-child {
      margin-bottom: 0;
    }
  }

  &__note-label {
    flex-shrink: 0;
    width: 112rpx;
    font-size: 24rpx;
    color: #9BA8B7;
  }

  &__note-content {
    flex: 1;
    min-width: 0;
    font-size: 26rpx;
    line-height: 1.55;
    color: #2D3748;
  }

  &__footer {
    display: flex;
    justify-content: flex-end;
    gap: 14rpx;
    margin-top: 22rpx;
  }

  &__action {
    display: flex;
    align-items: center;
    gap: 8rpx;
    min-height: 56rpx;
    padding: 0 20rpx;
    border-radius: 999rpx;
    background: #FFF5FA;

    text {
      font-size: 24rpx;
      color: #BE185D;
    }

    &.delete {
      background: #FFF5F5;

      text {
        color: #EF4444;
      }
    }

    &:active {
      opacity: 0.78;
    }
  }
}
</style>
