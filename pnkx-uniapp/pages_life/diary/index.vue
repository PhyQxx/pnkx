<template>
  <view class="diary-page">
    <view class="fixed-header">
      <view class="overview-panel">
        <view class="overview-main">
          <text class="overview-title">我的日记</text>
          <text class="overview-subtitle">{{ headerDateText }}</text>
        </view>
        <view class="overview-actions">
          <view class="overview-count" @click="goAnalysis">
            <text class="count-number">{{ totalDiaryCount }}</text>
            <text class="count-label">篇记录</text>
          </view>
          <view class="analysis-btn" @click="goAnalysis">
            <uni-icons type="chart" size="18" color="#A78BFA" />
            <text class="analysis-btn-text">心情分析</text>
          </view>
        </view>
      </view>

      <view class="stats-row">
        <view class="stat-item">
          <text class="stat-value">{{ monthDiaryCount }}</text>
          <text class="stat-label">本月</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ weekDiaryCount }}</text>
          <text class="stat-label">本周</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ currentStreak }}</text>
          <text class="stat-label">连续</text>
        </view>
      </view>

      <view class="search-bar">
        <uni-search-bar
          v-model="searchKeyword"
          placeholder="搜索标题或内容"
          @confirm="handleSearch"
          @clear="handleSearch"
          @input="onSearchInput"
          radius="100"
          bgColor="#FFFFFF"
          :focus="false"
          :show-action="false"
        />
      </view>

      <view class="view-switch">
        <view
          class="switch-item"
          :class="{ active: viewMode === 'timeline' }"
          @click="switchView('timeline')"
        >
          <uni-icons type="list" size="17" :color="viewMode === 'timeline' ? '#A78BFA' : '#9BA8B7'" />
          <text>时间线</text>
        </view>
        <view
          class="switch-item"
          :class="{ active: viewMode === 'calendar' }"
          @click="switchView('calendar')"
        >
          <uni-icons type="calendar" size="17" :color="viewMode === 'calendar' ? '#A78BFA' : '#9BA8B7'" />
          <text>日历</text>
        </view>
      </view>
    </view>

    <scroll-view
      class="content-scroll"
      scroll-y
      @scrolltolower="loadMore"
      lower-threshold="50"
      refresher-enabled
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onRefresh"
    >
      <view v-if="viewMode === 'calendar'" class="calendar-view">
        <view class="calendar-card">
          <uni-calendar
            :selected="selectedDates"
            :insert="true"
            :lunar="false"
            :startDate="calendarStartDate"
            :endDate="calendarEndDate"
            @monthSwitch="onMonthSwitch"
            @change="onDateClick"
          />
        </view>

        <view v-if="selectedDateDiaries.length > 0" class="selected-date-diaries">
          <view class="section-title">
            <text>{{ formatDateFull(selectedDate) }}</text>
            <text class="section-count">{{ selectedDateDiaries.length }}篇</text>
          </view>
          <DiaryCard
            v-for="diary in selectedDateDiaries"
            :key="diary.id"
            :diary="diary"
          />
        </view>

        <view v-else-if="selectedDate" class="empty-state compact">
          <view class="empty-icon">
            <uni-icons type="calendar" size="40" color="#A78BFA" />
          </view>
          <text class="empty-text">这一天还没有记录</text>
          <text class="empty-tip">写一篇，让日历多一个亮点</text>
        </view>
      </view>

      <view v-else class="timeline-view">
        <view v-if="diaryList.length === 0 && !loading" class="empty-state">
          <view class="empty-icon">
            <uni-icons type="compose" size="42" color="#A78BFA" />
          </view>
          <text class="empty-text">{{ emptyTitle }}</text>
          <text class="empty-tip">{{ emptyTip }}</text>
        </view>

        <view v-else class="diary-list">
          <DiaryCard
            v-for="diary in diaryList"
            :key="diary.id"
            :diary="diary"
          />
        </view>
      </view>

      <view v-if="viewMode === 'timeline' && diaryList.length > 0" class="load-more">
        <uni-load-more :status="loadMoreStatus" />
      </view>
    </scroll-view>

    <view class="fab-button" @click="handleAdd">
      <uni-icons type="plus" size="24" color="#FFFFFF" />
    </view>
  </view>
</template>

<script>
import { listDiary, delDiary } from '@/api/px/life/diary'
import { formatDate as _formatDate, formatDateFull as _formatDateFull } from '@/utils/pnkx'
import DiaryCard from './components/DiaryCard.vue'

export default {
  name: 'DiaryIndex',
  components: {
    DiaryCard
  },
  data() {
    return {
      searchKeyword: '',
      searchTimer: null,
      viewMode: 'timeline',
      selectedDate: '',
      calendarStartDate: '',
      calendarEndDate: '',
      selectedDates: [],
      diaryList: [],
      selectedDateDiaries: [],
      loading: false,
      isRefreshing: false,
      loadMoreStatus: 'more',
      pageNum: 1,
      pageSize: 10,
      total: 0,
      allDatesMap: {}
    }
  },
  computed: {
    headerDateText() {
      const now = new Date()
      const weekList = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      return `${now.getMonth() + 1}月${now.getDate()}日 ${weekList[now.getDay()]}`
    },
    totalDiaryCount() {
      return this.total || this.diaryList.length
    },
    monthDiaryCount() {
      const now = new Date()
      const currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
      return this.diaryList.filter(item => item.date && item.date.substring(0, 7) === currentMonth).length
    },
    weekDiaryCount() {
      const now = new Date()
      const day = now.getDay() || 7
      const start = new Date(now)
      start.setHours(0, 0, 0, 0)
      start.setDate(now.getDate() - day + 1)
      const end = new Date(start)
      end.setDate(start.getDate() + 7)

      return this.diaryList.filter(item => {
        if (!item.date) return false
        const date = new Date(item.date)
        return date >= start && date < end
      }).length
    },
    currentStreak() {
      const dateSet = {}
      this.diaryList.forEach(item => {
        if (item.date) dateSet[item.date.substring(0, 10)] = true
      })

      let streak = 0
      const cursor = new Date()
      while (dateSet[this.formatDate(cursor)]) {
        streak++
        cursor.setDate(cursor.getDate() - 1)
      }
      return streak
    },
    emptyTitle() {
      return this.searchKeyword ? '没有匹配的日记' : '还没有日记'
    },
    emptyTip() {
      return this.searchKeyword ? '换个关键词再找找' : '点右下角写下今天的第一段'
    }
  },
  onLoad() {
    this.initCalendarDates()
    this.loadDiaryList()
  },
  onShow() {
    this.refreshDiaryList()
  },
  methods: {
    initCalendarDates() {
      const now = new Date()
      const year = now.getFullYear()

      this.calendarStartDate = `${year}-01-01`
      this.calendarEndDate = `${year + 1}-12-31`
      this.selectedDate = this.formatDate(now)
    },

    formatDate(date) {
      return _formatDate(date)
    },

    formatDateFull(date) {
      return _formatDateFull(date, true)
    },

    switchView(mode) {
      this.viewMode = mode
      if (mode === 'calendar') {
        this.loadSelectedDateDiaries()
      }
    },

    async loadDiaryList(refresh = false) {
      if (this.loading) return

      this.loading = true

      if (refresh) {
        this.pageNum = 1
        this.diaryList = []
        this.allDatesMap = {}
      }

      try {
        const params = {
          pageNum: this.pageNum,
          pageSize: this.pageSize
        }

        if (this.searchKeyword) {
          params.title = this.searchKeyword
          params.content = this.searchKeyword
        }

        const response = await listDiary(params)

        if (response.code === 200) {
          const newDiaries = response.rows || []
          this.total = response.total || 0

          if (refresh) {
            this.diaryList = newDiaries
          } else {
            this.diaryList = [...this.diaryList, ...newDiaries]
          }

          this.buildSelectedDates()
          this.loadMoreStatus = this.diaryList.length >= this.total ? 'noMore' : 'more'
        } else {
          this.loadMoreStatus = 'more'
        }
      } catch (error) {
        console.error('加载日记列表失败:', error)
        this.loadMoreStatus = 'more'
        uni.showToast({
          title: '加载失败',
          icon: 'none'
        })
      } finally {
        this.loading = false
        this.isRefreshing = false
      }
    },

    buildSelectedDates() {
      this.allDatesMap = {}
      this.selectedDates = []

      this.diaryList.forEach(diary => {
        const date = diary.date ? diary.date.substring(0, 10) : ''
        if (date) {
          if (!this.allDatesMap[date]) {
            this.allDatesMap[date] = []
            this.selectedDates.push({ date: date, info: '记' })
          }
          this.allDatesMap[date].push(diary)
        }
      })

      this.loadSelectedDateDiaries()
    },

    loadSelectedDateDiaries() {
      this.selectedDateDiaries = this.allDatesMap[this.selectedDate] || []
    },

    loadMore() {
      if (this.viewMode !== 'timeline') return
      if (this.loadMoreStatus !== 'more' || this.loading) return
      this.pageNum++
      this.loadMoreStatus = 'loading'
      this.loadDiaryList()
    },

    onRefresh() {
      this.isRefreshing = true
      this.refreshDiaryList()
    },

    refreshDiaryList() {
      this.loadDiaryList(true)
    },

    handleSearch() {
      this.loadDiaryList(true)
    },

    onSearchInput(value) {
      const keyword = typeof value === 'string' ? value : value && value.value
      if (!keyword) {
        this.handleSearch()
        return
      }

      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
      }
      this.searchTimer = setTimeout(() => {
        this.handleSearch()
      }, 500)
    },

    onMonthSwitch(e) {
    },

    onDateClick(e) {
      this.selectedDate = e.fulldate
      this.loadSelectedDateDiaries()
    },

    handleAdd() {
      uni.navigateTo({
        url: '/pages_life/diary/edit'
      })
    },
    goAnalysis() {
      uni.navigateTo({
        url: '/pages_life/diary/analysis'
      })
    },
  }
}
</script>

<style lang="scss" scoped>
.diary-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, rgba($diary, 0.14) 0%, $bg-page 380rpx);

  .fixed-header {
    position: sticky;
    top: 0;
    z-index: $z-sticky;
    padding: 24rpx $page-padding 18rpx;
    background: linear-gradient(180deg, rgba($diary, 0.18) 0%, rgba(244, 246, 249, 0.96) 100%);
    box-shadow: 0 8rpx 24rpx rgba(74, 85, 104, 0.06);

    .overview-panel {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: $spacing-md;

      .overview-main {
        display: flex;
        flex-direction: column;
        gap: 8rpx;

        .overview-title {
          font-size: 40rpx;
          line-height: 1.2;
          font-weight: $font-weight-bold;
          color: $text-primary;
        }

        .overview-subtitle {
          font-size: $font-caption;
          color: $text-secondary;
        }
      }

      .overview-count {
        width: 128rpx;
        height: 128rpx;
        border-radius: $radius-full;
        background: linear-gradient(135deg, $diary 0%, #EC4899 100%);
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        box-shadow: 0 12rpx 28rpx rgba($diary, 0.3);

        .count-number {
          font-size: 34rpx;
          line-height: 1.1;
          font-weight: $font-weight-bold;
          color: $text-inverse;
        }

        .count-label {
          margin-top: 4rpx;
          font-size: $font-mini;
          color: rgba(255, 255, 255, 0.88);
        }
      }
    }

    .stats-row {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 12rpx;
      margin-bottom: 16rpx;

      .stat-item {
        padding: 16rpx 10rpx;
        border-radius: $radius-md;
        border: 1rpx solid rgba($diary, 0.14);
        background-color: rgba(255, 255, 255, 0.78);
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 4rpx;

        .stat-value {
          font-size: 32rpx;
          line-height: 1.1;
          font-weight: $font-weight-bold;
          color: $text-primary;
        }

        .stat-label {
          font-size: $font-mini;
          color: $text-tertiary;
        }
      }
    }

    .search-bar {
      margin: 0 -16rpx;
    }

    .view-switch {
      display: flex;
      gap: 6rpx;
      padding: 6rpx;
      margin-top: 14rpx;
      border-radius: $radius-full;
      border: 1rpx solid rgba($diary, 0.14);
      background-color: rgba(255, 255, 255, 0.82);

      .switch-item {
        flex: 1;
        height: 64rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 10rpx;
        border-radius: $radius-full;
        font-size: $font-caption;
        color: $text-tertiary;
        transition: all $duration-normal $ease-default;

        &.active {
          color: $diary;
          background-color: #FFFFFF;
          box-shadow: $shadow-sm;
          font-weight: $font-weight-semibold;
        }
      }
    }
  }

  .content-scroll {
    flex: 1;
    height: 0;
    padding: 22rpx $page-padding 160rpx;
  }

  .calendar-view {
    .calendar-card {
      overflow: hidden;
      margin-bottom: $spacing-lg;
      border-radius: $radius-lg;
      background-color: $bg-card;
      box-shadow: $shadow-card;

      ::v-deep .uni-calendar {
        background-color: transparent;
        padding: 18rpx;
      }
    }
  }

  .diary-list {
    padding-bottom: $spacing-md;
  }

  .selected-date-diaries {
    margin-top: $spacing-lg;
  }

  .section-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: $spacing-md;
    font-size: $font-body;
    font-weight: $font-weight-semibold;
    color: $text-primary;

    .section-count {
      font-size: $font-caption;
      font-weight: $font-weight-normal;
      color: $text-tertiary;
    }
  }

  .empty-state {
    min-height: 430rpx;
    padding: $spacing-3xl 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;

    &.compact {
      min-height: 360rpx;
    }

    .empty-icon {
      width: 132rpx;
      height: 132rpx;
      border-radius: $radius-full;
      background-color: rgba($diary, 0.12);
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .empty-text {
      margin-top: $spacing-lg;
      font-size: $font-body;
      font-weight: $font-weight-semibold;
      color: $text-secondary;
    }

    .empty-tip {
      margin-top: $spacing-xs;
      font-size: $font-caption;
      color: $text-tertiary;
    }
  }

  .load-more {
    padding: $spacing-md 0;
  }

  .fab-button {
    position: fixed;
    right: $page-padding;
    bottom: 92rpx;
    z-index: $z-fixed;
    width: 104rpx;
    height: 104rpx;
    border-radius: $radius-full;
    background: linear-gradient(135deg, $diary 0%, #EC4899 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 12rpx 28rpx rgba($diary, 0.38);
    transition: transform $duration-fast $ease-spring;

    &:active {
      transform: scale(0.92);
    }
  }
}
</style>
