<template>
  <uni-swipe-action>
    <uni-swipe-action-item :right-options="swipeOptions" @click="handleSwipeClick">
      <view class="diary-card" @click="handleClick">
        <view class="date-rail">
          <text class="date-day">{{ dayText }}</text>
          <text class="date-month">{{ monthText }}</text>
        </view>

        <view class="card-main">
          <view class="card-top">
            <view class="title-group">
              <text class="card-title">{{ diary.title || '无标题日记' }}</text>
              <text class="date-full">{{ fullDateText }}</text>
            </view>

            <view v-if="diary.mood || diary.weather" class="mood-weather">
              <view v-if="diary.mood" class="icon-chip">
                <svg-icon :icon-class="diary.mood" size="32rpx" />
              </view>
              <view v-if="diary.weather" class="icon-chip">
                <svg-icon :icon-class="diary.weather" size="32rpx" />
              </view>
            </view>
          </view>

          <text class="card-content">{{ contentPreview }}</text>

          <view class="card-footer">
            <view class="meta-pill">
              <uni-icons type="compose" size="13" color="#A78BFA" />
              <text>{{ wordCount }}字</text>
            </view>
            <view class="read-more">
              <text>查看</text>
              <uni-icons type="right" size="13" color="#9BA8B7" />
            </view>
          </view>
        </view>
      </view>
    </uni-swipe-action-item>
  </uni-swipe-action>
</template>

<script>
export default {
  name: 'DiaryCard',
  props: {
    diary: {
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
            backgroundColor: '#A78BFA'
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
    cleanContent() {
      const content = this.diary.content || this.diary.richText || ''
      return String(content)
        .replace(/<[^>]+>/g, '')
        .replace(/&nbsp;/g, ' ')
        .replace(/\s+/g, ' ')
        .trim()
    },
    contentPreview() {
      if (!this.cleanContent) return '今天还没有写下正文，点开补上这一页。'
      return this.cleanContent.length > 96 ? `${this.cleanContent.substring(0, 96)}...` : this.cleanContent
    },
    wordCount() {
      return this.cleanContent.length
    },
    dateValue() {
      if (!this.diary.date) return null
      const date = new Date(this.diary.date)
      return Number.isNaN(date.getTime()) ? null : date
    },
    dayText() {
      if (!this.dateValue) return '--'
      return String(this.dateValue.getDate()).padStart(2, '0')
    },
    monthText() {
      if (!this.dateValue) return '未知'
      return `${this.dateValue.getMonth() + 1}月`
    },
    fullDateText() {
      if (!this.dateValue) return '未选择日期'
      const weekList = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      return `${this.dateValue.getFullYear()}年${this.dateValue.getMonth() + 1}月${this.dateValue.getDate()}日 ${weekList[this.dateValue.getDay()]}`
    }
  },
  methods: {
    handleClick() {
      uni.navigateTo({
        url: `/pages_life/diary/edit?id=${this.diary.id}`
      })
    },
    handleSwipeClick(e) {
      const index = e.content.index
      if (index === 0) {
        this.handleDiaryEdit(this.diary)
      } else if (index === 1) {
        this.handleDiaryDelete(this.diary)
      }
    },
    handleDiaryDelete(diary) {
      uni.showModal({
        title: '删除确认',
        content: `确定删除「${diary.title || '无标题日记'}」吗？`,
        success: async (res) => {
          if (res.confirm) {
            try {
              const response = await delDiary(diary.id)
              if (response.code === 200) {
                uni.showToast({
                  title: '删除成功',
                  icon: 'success'
                })
                this.refreshDiaryList()
              }
            } catch (error) {
              console.error('删除日记失败:', error)
              uni.showToast({
                title: '删除失败',
                icon: 'none'
              })
            }
          }
        }
      })
    },
    handleDiaryEdit(diary) {
      uni.navigateTo({
        url: `/pages_life/diary/edit?id=${diary.id}`
      })
    },
  }
}
</script>

<style lang="scss" scoped>
.diary-card {
  display: flex;
  gap: $spacing-md;
  padding: 26rpx;
  margin-bottom: $spacing-md;
  background-color: $bg-card;
  border-radius: $radius-lg;
  border: 1rpx solid rgba($diary, 0.12);
  box-shadow: 0 8rpx 22rpx rgba(74, 85, 104, 0.06);
  transition: transform $duration-fast $ease-default;

  &:active {
    transform: scale(0.985);
  }

  .date-rail {
    width: 92rpx;
    min-width: 92rpx;
    height: 112rpx;
    border-radius: $radius-md;
    background: linear-gradient(180deg, rgba($diary, 0.16) 0%, rgba($diary, 0.06) 100%);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;

    .date-day {
      font-size: 38rpx;
      line-height: 1;
      font-weight: $font-weight-bold;
      color: $diary;
    }

    .date-month {
      margin-top: 8rpx;
      font-size: $font-mini;
      color: $text-secondary;
    }
  }

  .card-main {
    flex: 1;
    min-width: 0;
  }

  .card-top {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: $spacing-sm;
    margin-bottom: $spacing-sm;

    .title-group {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 8rpx;

      .card-title {
        font-size: $font-h2;
        font-weight: $font-weight-semibold;
        color: $text-primary;
        line-height: 1.25;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .date-full {
        font-size: $font-small;
        color: $text-tertiary;
        line-height: 1.2;
      }
    }

    .mood-weather {
      display: flex;
      gap: 8rpx;
      flex-shrink: 0;

      .icon-chip {
        width: 48rpx;
        height: 48rpx;
        border-radius: $radius-full;
        background-color: $gray-50;
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }
  }

  .card-content {
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
    font-size: $font-caption;
    color: $text-secondary;
    line-height: 1.65;
  }

  .card-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: $spacing-md;

    .meta-pill,
    .read-more {
      display: flex;
      align-items: center;
      gap: 6rpx;
      font-size: $font-mini;
      color: $text-tertiary;
      line-height: 1;
    }

    .meta-pill {
      height: 42rpx;
      padding: 0 14rpx;
      border-radius: $radius-full;
      background-color: rgba($diary, 0.08);
      color: $diary;
    }
  }
}
</style>
