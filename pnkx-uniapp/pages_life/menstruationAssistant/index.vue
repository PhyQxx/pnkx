<!--
  * @File: index
  * @Author: PHY
  * @Date: 2025/03/10
  * @Description: 姨妈助手主页
-->
<template>
  <view class="period-page">
    <view class="period-header">
      <view class="period-hero">
        <view class="period-hero__top">
          <view>
            <text class="period-hero__eyebrow">姨妈助手</text>
            <text class="period-hero__title">{{ heroTitle }}</text>
          </view>
          <view class="period-hero__setting" @click="goToSettings">
            <uni-icons type="gear" size="22" color="#BE185D" />
          </view>
        </view>

        <view class="period-hero__body">
          <view class="period-hero__status" :style="statusStyle">
            {{ currentStateLabel }}
          </view>
          <view class="period-hero__summary">
            <text class="period-hero__summary-value">{{ nextPeriodText }}</text>
            <text class="period-hero__summary-label">{{ nextPeriodLabel }}</text>
          </view>
        </view>
      </view>

      <view class="period-insight" v-if="pregnancyWeeks">
        <view class="period-insight__icon">
          <uni-icons type="heart-filled" size="18" color="#FFFFFF" />
        </view>
        <view class="period-insight__content">
          <text class="period-insight__title">距离最近一次记录 {{ pregnancyWeeks[2] }}</text>
          <text class="period-insight__text">约 {{ pregnancyWeeks[0] }} 周 {{ pregnancyWeeks[1] }} 天，可结合身体感受复盘。</text>
        </view>
      </view>

      <view class="period-actions">
        <view class="period-action" @click="handleAddRecord">
          <view class="period-action__icon primary">
            <uni-icons type="plus" size="20" color="#FFFFFF" />
          </view>
          <text>记录一下</text>
        </view>
        <view class="period-action" @click="goToStatistics">
          <view class="period-action__icon blue">
            <uni-icons type="bars" size="20" color="#FFFFFF" />
          </view>
          <text>统计</text>
        </view>
        <view class="period-action" @click="goToSettings">
          <view class="period-action__icon pale">
            <uni-icons type="gear" size="20" color="#BE185D" />
          </view>
          <text>设置</text>
        </view>
      </view>

      <view class="view-switch">
        <view
          class="view-switch__item"
          :class="{ active: viewMode === 'calendar' }"
          @click="switchView('calendar')"
        >
          <uni-icons type="calendar" size="17" :color="viewMode === 'calendar' ? '#BE185D' : '#9BA8B7'" />
          <text>日历</text>
        </view>
        <view
          class="view-switch__item"
          :class="{ active: viewMode === 'timeline' }"
          @click="switchView('timeline')"
        >
          <uni-icons type="list" size="17" :color="viewMode === 'timeline' ? '#BE185D' : '#9BA8B7'" />
          <text>列表</text>
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
            :insert="true"
            :lunar="false"
            :selected="calendarSelected"
            :startDate="calendarStartDate"
            :endDate="calendarEndDate"
            @monthSwitch="onMonthSwitch"
            @change="onDateClick"
          />
        </view>

        <view class="section-header" v-if="selectedDate">
          <view>
            <text class="section-header__title">{{ formatDateFull(selectedDate) }}</text>
            <text class="section-header__subtitle">{{ selectedDateRecords.length }} 条记录</text>
          </view>
          <view class="section-header__add" @click="handleAddRecord">
            <uni-icons type="plus" size="16" color="#BE185D" />
          </view>
        </view>

        <view v-if="selectedDateRecords.length > 0" class="record-list">
          <RecordCard
            v-for="record in selectedDateRecords"
            :key="record.id"
            :record="record"
            @click="handleRecordClick"
            @edit="handleRecordEdit"
            @delete="handleRecordDelete"
          />
        </view>

        <view v-else-if="selectedDate" class="empty-state">
          <view class="empty-state__icon">
            <uni-icons type="calendar" size="40" color="#F472B6" />
          </view>
          <text class="empty-state__title">这一天还没有记录</text>
          <text class="empty-state__text">添加经期、体温或检查信息，后续回看会更清楚。</text>
        </view>
      </view>

      <view v-else class="timeline-view">
        <view v-if="timelineRecords.length === 0" class="empty-state">
          <view class="empty-state__icon">
            <uni-icons type="calendar" size="40" color="#F472B6" />
          </view>
          <text class="empty-state__title">暂无记录</text>
          <text class="empty-state__text">从今天开始记录，周期趋势会慢慢变得可参考。</text>
        </view>
        <view v-else class="record-list">
          <RecordCard
            v-for="record in timelineRecords"
            :key="record.id"
            :record="record"
            @click="handleRecordClick"
            @edit="handleRecordEdit"
            @delete="handleRecordDelete"
          />
        </view>
      </view>

      <view v-if="viewMode === 'timeline' && timelineRecords.length > 0" class="load-more">
        <uni-load-more :status="loadMoreStatus" />
      </view>
    </scroll-view>

    <view class="fab-button" @click="handleAddRecord">
      <uni-icons type="plus" size="24" color="#FFFFFF" />
    </view>
  </view>
</template>

<script>
import {
  listMenstruationRecord,
  getPxMenstruationRecordList,
  getLastStartDate,
  delMenstruationRecord
} from '@/api/px/life/menstruationRecord'
import RecordCard from './components/RecordCard.vue'

export default {
  name: 'MenstruationAssistantIndex',
  components: {
    RecordCard
  },
  data() {
    return {
      viewMode: 'calendar',
      selectedDate: '',
      calendarStartDate: '',
      calendarEndDate: '',
      calendarSelected: [],
      menstruationRecords: [],
      selectedDateRecords: [],
      allRecordsMap: {},
      timelineRecords: [],
      currentState: '',
      stateOptions: [],
      pregnancyWeeks: null,
      cycle: 28,
      duration: 5,
      loading: false,
      isRefreshing: false,
      loadMoreStatus: 'more',
      pageNum: 1,
      pageSize: 10,
      total: 0,
      currentMonth: ''
    }
  },
  computed: {
    currentStateLabel() {
      const state = this.stateOptions.find(item => item.dictValue === this.currentState)
      if (state) return state.dictLabel
      const fallback = {
        whyl: '未怀孕',
        ymq: '姨妈期',
        aqq: '安全期',
        plq: '排卵期'
      }
      return fallback[this.currentState] || '待记录'
    },
    statusColor() {
      const colorMap = {
        whyl: '#BE185D',
        ymq: '#BE185D',
        aqq: '#2563EB',
        plq: '#DB2777'
      }
      return colorMap[this.currentState] || '#6B7B8D'
    },
    statusBgColor() {
      const colorMap = {
        whyl: '#FCE7F3',
        ymq: '#FCE7F3',
        aqq: '#DBEAFE',
        plq: '#FFF1F2'
      }
      return colorMap[this.currentState] || '#F4F6F9'
    },
    statusStyle() {
      return `color: ${this.statusColor}; background-color: ${this.statusBgColor}`
    },
    heroTitle() {
      if (this.currentState === 'ymq') return '照顾好现在的自己'
      if (this.currentState === 'plq') return '关注排卵窗口'
      if (this.currentState === 'aqq') return '今天是平稳期'
      return '记录身体节律'
    },
    lastStartRecord() {
      const records = [...this.menstruationRecords, ...this.timelineRecords]
        .filter(item => item.type === '0' && item.date)
        .sort((a, b) => new Date(b.date) - new Date(a.date))
      return records[0] || null
    },
    nextPeriodDate() {
      if (!this.lastStartRecord) return ''
      const date = new Date(this.lastStartRecord.date)
      date.setDate(date.getDate() + this.cycle)
      return this.formatDate(date)
    },
    nextPeriodText() {
      if (!this.nextPeriodDate) return '--'
      const days = this.getDaysBetween(new Date(), new Date(this.nextPeriodDate))
      if (days > 0) return `${days} 天`
      if (days === 0) return '今天'
      return `已过 ${Math.abs(days)} 天`
    },
    nextPeriodLabel() {
      return this.nextPeriodDate ? `预计下次 ${this.formatMonthDay(this.nextPeriodDate)}` : '记录一次开始日期后预测'
    }
  },
  onLoad() {
    this.$getConfigKey('ymdqzt').then(response => {
      this.currentState = response.msg
    })
    this.initCalendar()
    this.loadSettings()
    this.loadStateOptions()
    this.loadLastStartDate()
    this.loadMenstruationRecords()
  },
  onShow() {
    this.refreshData()
  },
  methods: {
    initCalendar() {
      const now = new Date()
      const year = now.getFullYear()
      this.calendarStartDate = `${year - 1}-01-01`
      this.calendarEndDate = `${year + 1}-12-31`
      this.selectedDate = this.formatDate(now)
      this.currentMonth = this.formatMonth(now)
    },

    formatDate(date) {
      if (!date) return ''
      const d = new Date(date)
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    },

    formatMonth(date) {
      if (!date) return ''
      const d = new Date(date)
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
    },

    formatMonthDay(date) {
      if (!date) return ''
      const d = new Date(date)
      return `${d.getMonth() + 1}月${d.getDate()}日`
    },

    formatDateFull(date) {
      if (!date) return ''
      const d = new Date(date)
      const weekMap = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${weekMap[d.getDay()]}`
    },

    getDaysBetween(date1, date2) {
      const d1 = new Date(this.formatDate(date1))
      const d2 = new Date(this.formatDate(date2))
      return Math.floor((d2 - d1) / (1000 * 60 * 60 * 24))
    },

    loadSettings() {
      const cycle = uni.getStorageSync('menstruation_cycle')
      const duration = uni.getStorageSync('menstruation_duration')
      if (cycle) this.cycle = parseInt(cycle)
      if (duration) this.duration = parseInt(duration)
    },

    async loadStateOptions() {
      try {
        const response = await this.getDicts('px_life_menstruation')
        if (response.code === 200) {
          this.stateOptions = response.data || []
        }
      } catch (error) {
        console.error('加载状态字典失败', error)
      }
    },

    async loadLastStartDate() {
      try {
        const response = await getLastStartDate()
        if (response.code === 200 && response.data) {
          const lastDate = new Date(response.data.date)
          const today = new Date()
          const days = Math.floor((today - lastDate) / (1000 * 60 * 60 * 24))
          if (days >= 0) {
            this.pregnancyWeeks = [
              Math.floor(days / 7),
              days % 7,
              `${days} 天`
            ]
          }
        }
      } catch (error) {
        console.error('加载最后经期时间失败', error)
      }
    },

    switchView(mode) {
      this.viewMode = mode
      if (mode === 'calendar') {
        this.loadMenstruationRecords()
      } else {
        this.loadTimelineRecords(true)
      }
    },

    async loadMenstruationRecords() {
      if (this.loading) return
      this.loading = true

      try {
        const response = await listMenstruationRecord({ date: this.currentMonth })

        if (response.code === 200) {
          const records = response.rows || []
          this.menstruationRecords = records.map(item => ({
            ...item,
            date: item.date ? item.date.substring(0, 10) : ''
          }))
          this.buildCalendarSelected()
          this.buildRecordsMap()

          if (this.selectedDate) {
            this.loadSelectedDateRecords()
          }
        }
      } catch (error) {
        console.error('加载经期记录失败:', error)
        uni.showToast({
          title: '加载失败',
          icon: 'none'
        })
      } finally {
        this.loading = false
        this.isRefreshing = false
      }
    },

    buildCalendarSelected() {
      this.calendarSelected = []
      const startRecords = this.menstruationRecords.filter(item => item.type === '0')
      const endRecords = this.menstruationRecords.filter(item => item.type === '1')

      startRecords.forEach(record => {
        this.calendarSelected.push({
          date: record.date,
          info: '开始',
          data: record
        })
      })

      endRecords.forEach(record => {
        this.calendarSelected.push({
          date: record.date,
          info: '结束',
          data: record
        })
      })

      this.menstruationRecords.forEach(record => {
        if (record.makeLove) {
          this.calendarSelected.push({
            date: record.date,
            info: '同房',
            data: record
          })
        }
        if (record.items) {
          this.calendarSelected.push({
            date: record.date,
            info: '检查',
            data: record
          })
        }
      })

      if (startRecords.length > 0) {
        const lastStart = new Date(startRecords[0].date)
        const today = new Date()
        const nextStart = new Date(lastStart)
        nextStart.setDate(nextStart.getDate() + this.cycle)

        if (nextStart > today) {
          for (let i = 0; i < this.duration; i++) {
            const predictDate = new Date(nextStart)
            predictDate.setDate(predictDate.getDate() + i)
            this.calendarSelected.push({
              date: this.formatDate(predictDate),
              info: '预测'
            })
          }
        }
      }
    },

    buildRecordsMap() {
      this.allRecordsMap = {}
      this.menstruationRecords.forEach(record => {
        if (record.date) {
          if (!this.allRecordsMap[record.date]) {
            this.allRecordsMap[record.date] = []
          }
          this.allRecordsMap[record.date].push(record)
        }
      })
    },

    loadSelectedDateRecords() {
      this.selectedDateRecords = this.allRecordsMap[this.selectedDate] || []
    },

    async loadTimelineRecords(refresh = false) {
      if (this.loading) return
      this.loading = true

      if (refresh) {
        this.pageNum = 1
        this.timelineRecords = []
      }

      try {
        const response = await getPxMenstruationRecordList({
          pageNum: this.pageNum,
          pageSize: this.pageSize
        })

        if (response.code === 200) {
          const newRecords = response.rows || []
          this.total = response.total || 0

          const formattedRecords = newRecords.map(item => ({
            ...item,
            date: item.date ? item.date.substring(0, 10) : ''
          }))

          this.timelineRecords = refresh
            ? formattedRecords
            : [...this.timelineRecords, ...formattedRecords]

          this.loadMoreStatus = this.timelineRecords.length >= this.total ? 'noMore' : 'more'
        } else {
          this.loadMoreStatus = 'more'
        }
      } catch (error) {
        console.error('加载时间线记录失败', error)
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

    loadMore() {
      if (this.viewMode !== 'timeline' || this.loadMoreStatus !== 'more') return
      this.pageNum++
      this.loadMoreStatus = 'loading'
      this.loadTimelineRecords()
    },

    onRefresh() {
      this.isRefreshing = true
      this.refreshData()
    },

    refreshData() {
      if (this.viewMode === 'calendar') {
        this.loadMenstruationRecords()
      } else {
        this.loadTimelineRecords(true)
      }
    },

    onMonthSwitch(e) {
      this.currentMonth = `${e.year}-${String(e.month).padStart(2, '0')}`
      this.loadMenstruationRecords()
    },

    onDateClick(e) {
      this.selectedDate = e.fulldate
      this.loadSelectedDateRecords()
    },

    handleAddRecord() {
      const url = this.selectedDate
        ? `/pages_life/menstruationAssistant/record?date=${this.selectedDate}`
        : '/pages_life/menstruationAssistant/record'
      uni.navigateTo({ url })
    },

    handleRecordClick(record) {
      uni.navigateTo({
        url: `/pages_life/menstruationAssistant/record?id=${record.id}`
      })
    },

    handleRecordEdit(record) {
      uni.navigateTo({
        url: `/pages_life/menstruationAssistant/record?id=${record.id}`
      })
    },

    handleRecordDelete(record) {
      uni.showModal({
        title: '确认删除',
        content: '确定要删除这条记录吗？',
        success: async (res) => {
          if (res.confirm) {
            try {
              const response = await delMenstruationRecord(record.id)
              if (response.code === 200) {
                uni.showToast({
                  title: '删除成功',
                  icon: 'success'
                })
                this.refreshData()
              }
            } catch (error) {
              console.error('删除记录失败:', error)
              uni.showToast({
                title: '删除失败',
                icon: 'none'
              })
            }
          }
        }
      })
    },

    goToSettings() {
      uni.navigateTo({
        url: '/pages_life/menstruationAssistant/settings'
      })
    },

    goToStatistics() {
      uni.navigateTo({
        url: '/pages_life/menstruationAssistant/statistics'
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.period-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #F7F8FB;
}

.period-header {
  position: sticky;
  top: 0;
  z-index: 100;
  padding: 24rpx 30rpx 18rpx;
  background: #F7F8FB;
}

.period-hero {
  padding: 30rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #FFF1F6 0%, #FFFFFF 54%, #EEF6FF 100%);
  box-shadow: 0 12rpx 32rpx rgba(244, 114, 182, 0.12);

  &__top {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 24rpx;
  }

  &__eyebrow {
    display: block;
    font-size: 24rpx;
    color: #BE185D;
    font-weight: 600;
    margin-bottom: 8rpx;
  }

  &__title {
    display: block;
    font-size: 40rpx;
    line-height: 1.2;
    font-weight: 700;
    color: #1A202C;
  }

  &__setting {
    width: 64rpx;
    height: 64rpx;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(255, 255, 255, 0.82);
  }

  &__body {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 24rpx;
    margin-top: 36rpx;
  }

  &__status {
    padding: 10rpx 20rpx;
    border-radius: 999rpx;
    font-size: 24rpx;
    font-weight: 600;
  }

  &__summary {
    text-align: right;
  }

  &__summary-value {
    display: block;
    font-size: 46rpx;
    line-height: 1.1;
    font-weight: 800;
    color: #BE185D;
  }

  &__summary-label {
    display: block;
    margin-top: 8rpx;
    font-size: 23rpx;
    color: #6B7B8D;
  }
}

.period-insight {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 18rpx 22rpx;
  margin-top: 18rpx;
  border-radius: 16rpx;
  background: #FFFFFF;

  &__icon {
    width: 44rpx;
    height: 44rpx;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #F472B6;
    flex-shrink: 0;
  }

  &__content {
    flex: 1;
    min-width: 0;
  }

  &__title {
    display: block;
    font-size: 26rpx;
    font-weight: 600;
    color: #2D3748;
  }

  &__text {
    display: block;
    margin-top: 4rpx;
    font-size: 23rpx;
    color: #9BA8B7;
  }
}

.period-actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
  margin-top: 18rpx;
}

.period-action {
  display: flex;
  align-items: center;
  gap: 12rpx;
  min-height: 76rpx;
  padding: 0 18rpx;
  border-radius: 16rpx;
  background: #FFFFFF;

  &__icon {
    width: 42rpx;
    height: 42rpx;
    border-radius: 14rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    &.primary {
      background: #F472B6;
    }

    &.blue {
      background: #6C9EFF;
    }

    &.pale {
      background: #FCE7F3;
    }
  }

  text {
    font-size: 25rpx;
    color: #2D3748;
    font-weight: 600;
  }
}

.view-switch {
  display: flex;
  padding: 8rpx;
  gap: 8rpx;
  margin-top: 18rpx;
  background: #FFFFFF;
  border-radius: 18rpx;

  &__item {
    flex: 1;
    height: 64rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8rpx;
    border-radius: 14rpx;
    color: #9BA8B7;
    font-size: 26rpx;
    font-weight: 600;

    &.active {
      color: #BE185D;
      background: #FCE7F3;
    }
  }
}

.content-scroll {
  flex: 1;
  height: 0;
  padding: 0 30rpx 160rpx;
}

.calendar-card {
  overflow: hidden;
  background: #FFFFFF;
  border-radius: 18rpx;
  box-shadow: 0 8rpx 24rpx rgba(108, 158, 255, 0.08);

  ::v-deep .uni-calendar {
    background: transparent;
  }
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 28rpx 0 18rpx;

  &__title {
    display: block;
    font-size: 30rpx;
    font-weight: 700;
    color: #1A202C;
  }

  &__subtitle {
    display: block;
    margin-top: 6rpx;
    font-size: 23rpx;
    color: #9BA8B7;
  }

  &__add {
    width: 56rpx;
    height: 56rpx;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #FCE7F3;
  }
}

.record-list {
  padding-bottom: 10rpx;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 96rpx 40rpx;
  margin-top: 24rpx;
  background: #FFFFFF;
  border-radius: 18rpx;

  &__icon {
    width: 96rpx;
    height: 96rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: #FFF1F6;
    margin-bottom: 22rpx;
  }

  &__title {
    font-size: 30rpx;
    font-weight: 700;
    color: #2D3748;
  }

  &__text {
    max-width: 460rpx;
    margin-top: 10rpx;
    font-size: 24rpx;
    line-height: 1.5;
    color: #9BA8B7;
    text-align: center;
  }
}

.load-more {
  padding: 20rpx 0;
}

.fab-button {
  position: fixed;
  right: 40rpx;
  bottom: 100rpx;
  width: 104rpx;
  height: 104rpx;
  background: linear-gradient(135deg, #F472B6 0%, #DB2777 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 14rpx 30rpx rgba(219, 39, 119, 0.28);
  z-index: 999;

  &:active {
    transform: scale(0.96);
  }
}
</style>
