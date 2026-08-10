<!--
 * @File: TodoCard
 * @Author: PHY
 * @Date: 2025/03/09
 * @Description: 待办卡片组件
-->
<template>
  <uni-swipe-action>
    <uni-swipe-action-item :right-options="swipeOptions" @click="handleSwipeClick">
      <view class="todo-card" :class="{ completed: isCompleted, overdue: isOverdue }" @click="handleClick">
        <view class="card-header">
          <view class="status-section" @click.stop="handleToggleStatus">
            <uni-icons :type="statusIcon" :color="statusColor" size="22" />
          </view>
          <view class="header-info">
            <text class="status-text">{{ statusText }}</text>
            <text class="date-range">{{ dateRange }}</text>
          </view>
          <view v-if="isOverdue && !isCompleted" class="overdue-badge">已逾期</view>
        </view>

        <view class="card-content" :class="{ completed: isCompleted }">
          {{ contentPreview }}
        </view>

        <view class="card-footer" v-if="labels.length > 0 || todo.performerName">
          <view class="labels-section">
            <view
              v-for="(label, index) in labels"
              :key="index"
              class="label-chip"
            >
              {{ label }}
            </view>
          </view>
          <view class="performer-section" v-if="todo.performerName">
            <uni-icons type="person" size="14" color="#9BA8B7" />
            <text class="performer-text">{{ todo.performerName }}</text>
          </view>
        </view>
      </view>
    </uni-swipe-action-item>
  </uni-swipe-action>
</template>

<script>
export default {
  name: 'TodoCard',
  props: {
    todo: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      swipeOptions: [
        {
          text: '编辑',
          style: {
            backgroundColor: '#34D399'
          }
        },
        {
          text: '删除',
          style: {
            backgroundColor: '#FF6B6B'
          }
        }
      ]
    }
  },
  computed: {
    statusIcon() {
      return this.isCompleted ? 'checkbox-filled' : 'circle'
    },
    statusColor() {
      return this.isCompleted ? '#52C41A' : '#D9D9D9'
    },
    statusText() {
      return this.isCompleted ? '已完成' : '进行中'
    },
    isCompleted() {
      const status = this.todo ? this.todo.status : false
      return status === true || status === 1 || status === '1' || status === 'true' || status === '已完成'
    },
    isOverdue() {
      if (this.isCompleted || !this.todo.planEndTime) return false
      const endTime = new Date(this.todo.planEndTime).getTime()
      if (Number.isNaN(endTime)) return false
      return endTime < Date.now()
    },
    dateRange() {
      const start = this.formatDateShort(this.todo.planStartTime)
      const end = this.formatDateShort(this.todo.planEndTime)
      if (!start && !end) {
        return '未设置时间'
      }
      if (!start) {
        return `截止 ${end}`
      }
      if (!end) {
        return `${start} 开始`
      }
      if (start === end) {
        return start
      }
      return `${start} ~ ${end}`
    },
    labels() {
      return this.todo.label ? this.todo.label.split(',').filter(l => l.trim()) : []
    },
    contentPreview() {
      const content = this.todo.content || ''
      return content.length > 80 ? content.substring(0, 80) + '...' : content
    }
  },
  methods: {
    formatDateShort(dateStr) {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${month}-${day}`
    },

    handleClick() {
      this.$emit('click', this.todo)
    },

    handleToggleStatus() {
      this.$emit('toggle-status', this.todo)
    },

    handleSwipeClick(e) {
      const index = e.content.index
      if (index === 0) {
        this.$emit('edit', this.todo)
      } else if (index === 1) {
        this.$emit('delete', this.todo)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.todo-card {
  position: relative;
  overflow: hidden;
  background-color: $bg-card;
  border-radius: $radius-lg;
  padding: 26rpx;
  margin-bottom: $spacing-md;
  border: 1rpx solid rgba($todo, 0.12);
  box-shadow: $shadow-card;
  transition: all 0.3s;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 28rpx;
    width: 8rpx;
    height: 76rpx;
    border-radius: 0 $radius-full $radius-full 0;
    background-color: $todo;
  }

  &.completed {
    border-color: $border-light;

    &::before {
      background-color: $text-disabled;
    }
  }

  &.overdue {
    border-color: rgba($danger, 0.2);

    &::before {
      background-color: $danger;
    }
  }

  &:active {
    transform: scale(0.98);
  }

  .card-header {
    display: flex;
    align-items: center;
    gap: 18rpx;
    margin-bottom: 18rpx;

    .status-section {
      width: 56rpx;
      height: 56rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: $radius-full;
      background-color: rgba($todo, 0.1);
      transition: all 0.2s;
      flex-shrink: 0;

      &:active {
        background-color: rgba($todo, 0.18);
      }
    }

    .header-info {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 6rpx;

      .status-text {
        font-size: $font-small;
        color: $text-tertiary;
        line-height: 1.2;
      }

      .date-range {
        font-size: $font-caption;
        font-weight: $font-weight-semibold;
        color: $text-secondary;
        line-height: 1.2;
      }
    }

    .overdue-badge {
      flex-shrink: 0;
      height: 44rpx;
      padding: 0 16rpx;
      border-radius: $radius-full;
      background-color: rgba($danger, 0.1);
      color: $danger;
      font-size: $font-mini;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  .card-content {
    padding-left: 74rpx;
    margin-bottom: 20rpx;
    font-size: $font-h3;
    line-height: $line-height-relaxed;
    color: $text-primary;
    word-break: break-all;
    font-weight: $font-weight-medium;

    &.completed {
      color: $text-tertiary;
      text-decoration: line-through;
      font-weight: $font-weight-normal;
    }
  }

  .card-footer {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 12rpx;
    padding-left: 74rpx;

    .labels-section {
      display: flex;
      flex-wrap: wrap;
      gap: 8rpx;
      flex: 1;
      min-width: 0;

      .label-chip {
        max-width: 220rpx;
        height: 42rpx;
        padding: 0 14rpx;
        border-radius: $radius-full;
        background-color: rgba($todo, 0.1);
        color: $todo;
        font-size: $font-mini;
        line-height: 42rpx;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .performer-section {
      display: flex;
      align-items: center;
      gap: 6rpx;
      flex-shrink: 0;
      max-width: 190rpx;

      .performer-text {
        min-width: 0;
        font-size: $font-small;
        color: $text-tertiary;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }
}
</style>
