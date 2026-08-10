<!--
 * @File: CommemorationDayCard
 * @Author: PHY
 * @Date: 2025/03/10
 * @Description: 纪念日卡片组件
-->
<template>
  <uni-swipe-action>
    <uni-swipe-action-item :right-options="swipeOptions" @click="handleSwipeClick">
      <view class="commemoration-card" @click="handleClick">
        <view class="card-left">
          <view class="icon-wrapper">
            <svg-icon :icon-class="item.icon || '纪念日'" size="48px" />
          </view>
        </view>

        <view class="card-middle">
          <view class="name">{{ item.name }}</view>
          <view class="date">{{ formatDateDisplay }}</view>
        </view>

        <view class="card-right">
          <view class="count-down">
            <text class="count-number">{{ countDays }}</text>
            <text class="count-unit">{{ countUnit }}</text>
          </view>
          <view class="count-label">{{ countLabel }}</view>
        </view>
      </view>
    </uni-swipe-action-item>
  </uni-swipe-action>
</template>

<script>
export default {
  name: 'CommemorationDayCard',
  props: {
    item: {
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
            backgroundColor: '#FB923C'
          }
        },
        {
          text: '删除',
          style: {
            backgroundColor: '#FF6B6B'
          }
        }
      ],
      nowTime: new Date()
    }
  },
  computed: {
    formatDateDisplay() {
      if (!this.item.date) return ''
      const d = new Date(this.item.date)
      if (this.item.repeat) {
        return this.$parseTime(d, '{y}年{m}月{d}日')
      }
      return this.$parseTime(d, '{y}年{m}月{d}日')
    },
    countDays() {
      if (!this.item.date) return 0
      const targetDate = this.getNextOccurrence()
      const diff = targetDate.getTime() - this.nowTime.getTime()
      return Math.ceil(diff / (1000 * 60 * 60 * 24))
    },
    countUnit() {
      return '天'
    },
    countLabel() {
      if (this.item.repeat) {
        return '还有'
      }
      if (this.countDays >= 0) {
        return '还有'
      }
      return '已经'
    }
  },
  methods: {
    getNextOccurrence() {
      const itemDate = new Date(this.item.date)
      const now = new Date()

      if (this.item.repeat) {
        let nextDate = new Date(now.getFullYear(), itemDate.getMonth(), itemDate.getDate())
        if (nextDate.getTime() < now.getTime()) {
          nextDate = new Date(now.getFullYear() + 1, itemDate.getMonth(), itemDate.getDate())
        }
        return nextDate
      }

      return itemDate
    },
    handleClick() {
      this.handleCardClick(this.item)
    },
    handleSwipeClick(e) {
      const index = e.content.index
      if (index === 0) {
        this.handleEdit(this.item)
      } else if (index === 1) {
        this.handleDelete(this.item)
      }
    },
    updateTime() {
      this.nowTime = new Date()
    },
    handleCardClick(item) {
      uni.navigateTo({
        url: `/pages_life/commemorationDay/add?id=${item.id}`
      })
    },

    handleEdit(item) {
      uni.navigateTo({
        url: `/pages_life/commemorationDay/add?id=${item.id}`
      })
    },

    handleDelete(item) {
      uni.showModal({
        title: '确认删除',
        content: `确定要删除"${item.name}"吗？`,
        success: async (res) => {
          if (res.confirm) {
            try {
              const response = await delDay(item.id)
              if (response.code === 200) {
                uni.showToast({
                  title: '删除成功',
                  icon: 'success'
                })
                this.refreshList()
              }
            } catch (error) {
              console.error('删除纪念日失败:', error)
              uni.showToast({
                title: '删除失败',
                icon: 'none'
              })
            }
          }
        }
      })
    }
  },
  mounted() {
    this.timer = setInterval(() => {
      this.updateTime()
    }, 60000)
  },
  beforeDestroy() {
    if (this.timer) {
      clearInterval(this.timer)
    }
  }
}
</script>

<style lang="scss" scoped>
.commemoration-card {
  display: flex;
  align-items: center;
  background-color: $bg-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  margin-bottom: $spacing-md;
  box-shadow: $shadow-card;
  transition: transform $duration-fast $ease-default;

  &:active {
    transform: scale(0.98);
  }

  .card-left {
    margin-right: $spacing-md;

    .icon-wrapper {
      width: 96rpx;
      height: 96rpx;
      background: linear-gradient(135deg, rgba($commemoration, 0.12) 0%, rgba($commemoration, 0.06) 100%);
      border-radius: $radius-full;
      display: flex;
      align-items: center;
      justify-content: center;

      ::v-deep .svg-icon {
        width: 48rpx;
        height: 48rpx;
      }
    }
  }

  .card-middle {
    flex: 1;
    min-width: 0;

    .name {
      font-size: $font-h2;
      font-weight: $font-weight-semibold;
      color: $text-primary;
      margin-bottom: $spacing-xs;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .date {
      font-size: $font-caption;
      color: $text-tertiary;
    }
  }

  .card-right {
    text-align: right;
    margin-left: $spacing-md;

    .count-down {
      display: flex;
      align-items: baseline;
      justify-content: flex-end;

      .count-number {
        font-size: 48rpx;
        font-weight: $font-weight-bold;
        color: $commemoration;
      }

      .count-unit {
        font-size: $font-caption;
        color: $commemoration;
        margin-left: $spacing-2xs;
      }
    }

    .count-label {
      font-size: $font-caption;
      color: $text-tertiary;
      margin-top: $spacing-2xs;
    }
  }
}
</style>
