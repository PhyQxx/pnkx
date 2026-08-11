<template>
  <view class="calendar-page">
    <!-- 月份导航 -->
    <view class="cal-header">
      <view class="cal-header__nav" @click="shiftMonth(-1)">‹</view>
      <text class="cal-header__title">{{ year }}年{{ month + 1 }}月</text>
      <view class="cal-header__nav" @click="shiftMonth(1)">›</view>
    </view>

    <!-- 星期 -->
    <view class="cal-weekdays">
      <text v-for="(w, i) in weekdays" :key="i" class="cal-weekday">{{ w }}</text>
    </view>

    <!-- 日期网格 -->
    <view class="cal-grid">
      <view
        v-for="(day, i) in calendarDays"
        :key="i"
        class="cal-day"
        :class="{
          'cal-day--other': !day.currentMonth,
          'cal-day--today': day.isToday,
          'cal-day--selected': day.dateStr === selectedDate
        }"
        @click="selectDay(day)"
      >
        <text class="cal-day__num">{{ day.day }}</text>
        <view v-if="day.eventCount > 0" class="cal-day__dots">
          <view
            v-for="(color, ci) in day.eventColors"
            :key="ci"
            class="cal-day__dot"
            :style="{ background: colorOf(color) }"
          ></view>
        </view>
      </view>
    </view>

    <!-- 选中日的事件 -->
    <view class="cal-events">
      <view class="cal-events__title">
        {{ selectedDateLabel }}
        <text v-if="dayEvents.length > 0" class="cal-events__count">{{ dayEvents.length }}项</text>
      </view>
      <view v-if="dayEvents.length > 0" class="cal-events__list">
        <view
          class="cal-event"
          v-for="(ev, i) in dayEvents"
          :key="i"
          @click="navigateToSource(ev)"
        >
          <view class="cal-event__bar" :style="{ background: colorOf(ev.color) }"></view>
          <view class="cal-event__body">
            <text class="cal-event__title">{{ ev.title }}</text>
            <text class="cal-event__type">{{ typeLabel(ev.sourceType) }}</text>
          </view>
          <text class="cal-event__arrow">›</text>
        </view>
      </view>
      <view v-else class="cal-events__empty">
        <text class="cal-events__empty-text">这天没有安排</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getMonthEvents } from '@/api/px/life/calendar'

const WEEKDAYS = ['日', '一', '二', '三', '四', '五', '六']
const COLOR_MAP = {
  todo: '#6C63FF',
  commemoration: '#FF9F43',
  menstruation: '#FD6697',
  bookkeeping: '#5B9EEE'
}

export default {
  data() {
    const today = new Date()
    return {
      year: today.getFullYear(),
      month: today.getMonth(),
      weekdays: WEEKDAYS,
      events: [],
      selectedDate: this.formatDate(today)
    }
  },
  computed: {
    calendarDays() {
      const firstDay = new Date(this.year, this.month, 1)
      const startWeekday = firstDay.getDay()
      const daysInMonth = new Date(this.year, this.month + 1, 0).getDate()
      const daysInPrev = new Date(this.year, this.month, 0).getDate()
      const todayStr = this.formatDate(new Date())
      const days = []
      // 上月填充
      for (let i = startWeekday - 1; i >= 0; i--) {
        const d = new Date(this.year, this.month - 1, daysInPrev - i)
        days.push(this.buildDay(d, false, todayStr))
      }
      // 本月
      for (let d = 1; d <= daysInMonth; d++) {
        const date = new Date(this.year, this.month, d)
        days.push(this.buildDay(date, true, todayStr))
      }
      // 下月填充至 42 格
      let nextDay = 1
      while (days.length < 42) {
        const d = new Date(this.year, this.month + 1, nextDay++)
        days.push(this.buildDay(d, false, todayStr))
      }
      return days
    },
    dayEvents() {
      return this.events.filter(e => (e.date || '').slice(0, 10) === this.selectedDate)
    },
    selectedDateLabel() {
      if (!this.selectedDate) return ''
      const d = new Date(this.selectedDate)
      return (d.getMonth() + 1) + '月' + d.getDate() + '日'
    }
  },
  onLoad() {
    this.loadEvents()
  },
  methods: {
    formatDate(d) {
      const m = (d.getMonth() + 1).toString().padStart(2, '0')
      const day = d.getDate().toString().padStart(2, '0')
      return d.getFullYear() + '-' + m + '-' + day
    },
    buildDay(date, currentMonth, todayStr) {
      const dateStr = this.formatDate(date)
      const dayEvents = this.events.filter(e => (e.date || '').slice(0, 10) === dateStr)
      return {
        day: date.getDate(),
        dateStr,
        currentMonth,
        isToday: dateStr === todayStr,
        eventCount: dayEvents.length,
        eventColors: [...new Set(dayEvents.map(e => e.color))].slice(0, 3)
      }
    },
    shiftMonth(delta) {
      let m = this.month + delta
      let y = this.year
      if (m < 0) { m = 11; y-- }
      else if (m > 11) { m = 0; y++ }
      this.year = y
      this.month = m
      this.loadEvents()
    },
    async loadEvents() {
      const start = new Date(this.year, this.month, 1)
      const end = new Date(this.year, this.month + 1, 0)
      try {
        const res = await getMonthEvents(this.formatDate(start), this.formatDate(end))
        this.events = res.data || []
      } catch (e) {
        uni.showToast({ title: '加载失败', icon: 'none' })
      }
    },
    selectDay(day) {
      this.selectedDate = day.dateStr
    },
    colorOf(color) {
      return COLOR_MAP[color] || '#909399'
    },
    typeLabel(type) {
      const map = { todo: '待办', commemoration: '纪念日', menstruation: '经期', bookkeeping: '记账' }
      return map[type] || type
    },
    navigateToSource(ev) {
      const map = {
        todo: '/pages_life/todo/index',
        commemoration: '/pages_life/commemorationDay/index',
        menstruation: '/pages_life/menstruationAssistant/index'
      }
      const path = map[ev.sourceType]
      if (path) {
        uni.navigateTo({ url: path })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.calendar-page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: $spacing-xl;
}

/* Header */
.cal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: $bg-card;
  padding: $spacing-md $page-padding;
  box-shadow: $shadow-xs;

  &__title {
    font-size: $font-h3;
    font-weight: $font-weight-semibold;
    color: $text-primary;
  }

  &__nav {
    width: 64rpx;
    height: 64rpx;
    border-radius: 50%;
    background: $bg-page;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 40rpx;
    color: $text-secondary;

    &:active { opacity: 0.6; }
  }
}

/* Weekdays */
.cal-weekdays {
  display: flex;
  padding: $spacing-sm $page-padding 0;
}

.cal-weekday {
  flex: 1;
  text-align: center;
  font-size: $font-mini;
  color: $text-tertiary;
}

/* Grid */
.cal-grid {
  display: flex;
  flex-wrap: wrap;
  padding: $spacing-sm $page-padding;
}

.cal-day {
  width: calc(100% / 7);
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;

  &__num {
    font-size: $font-body;
    color: $text-primary;
  }

  &--other &__num {
    color: $text-tertiary;
    opacity: 0.4;
  }

  &--today {
    .cal-day__num {
      width: 56rpx;
      height: 56rpx;
      line-height: 56rpx;
      text-align: center;
      border-radius: 50%;
      background: $primary;
      color: #fff;
      font-weight: $font-weight-bold;
    }
  }

  &--selected:not(.cal-day--today) {
    .cal-day__num {
      width: 56rpx;
      height: 56rpx;
      line-height: 56rpx;
      text-align: center;
      border-radius: 50%;
      background: rgba($primary, 0.12);
      color: $primary;
      font-weight: $font-weight-semibold;
    }
  }

  &__dots {
    position: absolute;
    bottom: 8rpx;
    display: flex;
    gap: 4rpx;
  }

  &__dot {
    width: 8rpx;
    height: 8rpx;
    border-radius: 50%;
  }
}

/* Events */
.cal-events {
  margin: $spacing-md $page-padding 0;
  background: $bg-card;
  border-radius: $radius-lg;
  box-shadow: $shadow-card;
  padding: $spacing-md;

  &__title {
    font-size: $font-body;
    font-weight: $font-weight-semibold;
    color: $text-primary;
    margin-bottom: $spacing-sm;
    display: flex;
    align-items: center;
    gap: $spacing-sm;
  }

  &__count {
    font-size: $font-mini;
    color: $primary;
    font-weight: normal;
  }

  &__empty {
    padding: $spacing-lg 0;
    text-align: center;
  }

  &__empty-text {
    font-size: $font-caption;
    color: $text-tertiary;
  }
}

.cal-event {
  display: flex;
  align-items: center;
  padding: $spacing-sm 0;
  border-bottom: 2rpx solid $gray-100;

  &:last-child { border-bottom: none; }
  &:active { opacity: 0.7; }

  &__bar {
    width: 6rpx;
    height: 48rpx;
    border-radius: $radius-full;
    margin-right: $spacing-sm;
    flex-shrink: 0;
  }

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__title {
    font-size: $font-body;
    color: $text-primary;
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__type {
    font-size: $font-mini;
    color: $text-tertiary;
  }

  &__arrow {
    font-size: 36rpx;
    color: $text-tertiary;
  }
}
</style>
