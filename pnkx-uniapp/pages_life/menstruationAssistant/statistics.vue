<!--
  * @File: statistics
  * @Author: PHY
  * @Date: 2025/03/10
  * @Description: 经期统计页
-->
<template>
  <view class="statistics-page">
    <view class="stats-hero">
      <text class="stats-hero__eyebrow">周期统计</text>
      <text class="stats-hero__title">{{ stats.avgCycleText }} 天</text>
      <text class="stats-hero__text">平均周期，基于已记录的开始日期计算</text>
    </view>

    <view class="stats-grid">
      <view class="stat-card">
        <text class="stat-card__value">{{ stats.avgCycleText }}</text>
        <text class="stat-card__label">平均周期</text>
      </view>
      <view class="stat-card blue">
        <text class="stat-card__value">{{ stats.avgDurationText }}</text>
        <text class="stat-card__label">经期时长</text>
      </view>
      <view class="stat-card green">
        <text class="stat-card__value">{{ stats.totalRecords }}</text>
        <text class="stat-card__label">记录总数</text>
      </view>
      <view class="stat-card orange">
        <text class="stat-card__value">{{ stats.totalCycles }}</text>
        <text class="stat-card__label">经期次数</text>
      </view>
    </view>

    <view class="trend-section">
      <view class="section-header">
        <view>
          <text class="section-header__title">周期趋势</text>
          <text class="section-header__sub">最近记录的间隔概览</text>
        </view>
      </view>

      <view v-if="cycleTrend.length > 0" class="trend-list">
        <view v-for="(item, index) in cycleTrend" :key="index" class="trend-item">
          <view class="trend-item__meta">
            <text class="trend-item__date">{{ item.date }}</text>
            <text class="trend-item__days">{{ item.daysText }}</text>
          </view>
          <view class="trend-item__bar">
            <view class="trend-item__bar-fill" :style="item.style"></view>
          </view>
        </view>
      </view>

      <view v-else class="empty-state compact">
        <view class="empty-state__icon">
          <uni-icons type="bars" size="34" color="#F472B6" />
        </view>
        <text class="empty-state__title">趋势还不够清晰</text>
        <text class="empty-state__text">至少记录两次经期开始后，就能计算周期间隔。</text>
      </view>
    </view>

    <view class="recent-section">
      <view class="section-header">
        <view>
          <text class="section-header__title">最近记录</text>
          <text class="section-header__sub">点击可继续编辑</text>
        </view>
      </view>

      <view v-if="recentRecords.length === 0" class="empty-state compact">
        <view class="empty-state__icon">
          <uni-icons type="calendar" size="34" color="#F472B6" />
        </view>
        <text class="empty-state__title">暂无记录</text>
      </view>

      <view v-else class="record-list">
        <view
          v-for="record in recentRecords"
          :key="record.id"
          class="record-item"
          @click="handleRecordClick(record)"
        >
          <view class="record-item__date">
            <text class="record-item__day">{{ record.dayText }}</text>
            <text class="record-item__month">{{ record.monthText }}</text>
          </view>
          <view class="record-item__main">
            <text class="record-item__type" :class="record.typeClass">{{ record.typeText }}</text>
            <text class="record-item__desc">{{ record.descText }}</text>
          </view>
          <uni-icons type="right" size="16" color="#CBD5E1" />
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getPxMenstruationRecordList } from '@/api/px/life/menstruationRecord'

export default {
  name: 'MenstruationAssistantStatistics',
  data() {
    return {
      stats: {
        avgCycle: null,
        avgCycleText: '--',
        avgDuration: 5,
        avgDurationText: '5',
        totalRecords: 0,
        totalCycles: 0
      },
      recentRecords: [],
      allRecords: [],
      cycleTrend: []
    }
  },
  onLoad() {
    this.loadStatistics()
  },
  methods: {
    formatDate(date) {
      if (!date) return ''
      return date.substring(0, 10)
    },

    getDay(date) {
      const value = this.formatDate(date)
      return value ? value.substring(8, 10) : '--'
    },

    getMonth(date) {
      const value = this.formatDate(date)
      if (!value) return ''
      const d = new Date(value)
      return `${d.getMonth() + 1}月`
    },

    getTypeText(type) {
      if (type === '0') return '经期开始'
      if (type === '1') return '经期结束'
      return '普通记录'
    },

    getTypeClass(type) {
      if (type === '0') return 'start'
      if (type === '1') return 'end'
      return 'normal'
    },

    getRecordDesc(record) {
      const desc = []
      if (record.temperature) desc.push(`体温 ${record.temperature}℃`)
      if (record.weight) desc.push(`体重 ${record.weight}kg`)
      if (record.items) desc.push(record.items)
      if (record.makeLove) desc.push('同房')
      return desc.length ? desc.join(' · ') : '未填写更多信息'
    },

    normalizeRecordDisplay(record) {
      return {
        ...record,
        dayText: this.getDay(record.date),
        monthText: this.getMonth(record.date),
        typeText: this.getTypeText(record.type),
        typeClass: this.getTypeClass(record.type),
        descText: this.getRecordDesc(record)
      }
    },

    async loadStatistics() {
      try {
        uni.showLoading({ title: '加载中...' })

        const response = await getPxMenstruationRecordList({
          pageNum: 1,
          pageSize: 100
        })

        uni.hideLoading()

        if (response.code === 200) {
          this.allRecords = (response.rows || []).map(item => ({
            ...item,
            date: item.date ? item.date.substring(0, 10) : ''
          }))
          this.recentRecords = this.allRecords.slice(0, 10).map(this.normalizeRecordDisplay)
          this.calculateStats()
        }
      } catch (error) {
        uni.hideLoading()
        console.error('加载统计数据失败:', error)
        uni.showToast({
          title: '加载失败',
          icon: 'none'
        })
      }
    },

    calculateStats() {
      this.stats.totalRecords = this.allRecords.length

      const startRecords = this.allRecords
        .filter(r => r.type === '0')
        .sort((a, b) => new Date(b.date) - new Date(a.date))

      this.stats.totalCycles = startRecords.length

      const cycles = []
      for (let i = 0; i < startRecords.length - 1; i++) {
        const days = this.getDaysBetween(startRecords[i].date, startRecords[i + 1].date)
        if (days > 0 && days < 60) {
          cycles.push({
            date: this.formatDate(startRecords[i].date),
            days
          })
        }
      }

      if (cycles.length > 0) {
        const avgCycle = cycles.reduce((sum, item) => sum + item.days, 0) / cycles.length
        this.stats.avgCycle = Math.round(avgCycle)
        this.stats.avgCycleText = String(this.stats.avgCycle)
      } else {
        this.stats.avgCycle = null
        this.stats.avgCycleText = '--'
      }

      const duration = uni.getStorageSync('menstruation_duration')
      this.stats.avgDuration = duration ? parseInt(duration) : 5
      this.stats.avgDurationText = String(this.stats.avgDuration)

      const maxDays = Math.max(...cycles.map(item => item.days), 35)
      this.cycleTrend = cycles.slice(0, 6).map(item => {
        const percent = Math.max(12, Math.min(100, Math.round((item.days / maxDays) * 100)))
        return {
          ...item,
          daysText: `${item.days} 天`,
          style: `width: ${percent}%`
        }
      })
    },

    getDaysBetween(date1, date2) {
      const d1 = new Date(date1)
      const d2 = new Date(date2)
      return Math.floor((d1 - d2) / (1000 * 60 * 60 * 24))
    },

    handleRecordClick(record) {
      uni.navigateTo({
        url: `/pages_life/menstruationAssistant/record?id=${record.id}`
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.statistics-page {
  min-height: 100vh;
  padding: 24rpx 30rpx;
  background: #F7F8FB;
}

.stats-hero {
  padding: 30rpx;
  border-radius: 24rpx;
  margin-bottom: 20rpx;
  background: linear-gradient(135deg, #FFF1F6 0%, #FFFFFF 58%, #EEF6FF 100%);
  box-shadow: 0 12rpx 32rpx rgba(244, 114, 182, 0.1);

  &__eyebrow {
    display: block;
    font-size: 24rpx;
    color: #BE185D;
    font-weight: 600;
  }

  &__title {
    display: block;
    margin-top: 8rpx;
    font-size: 54rpx;
    line-height: 1.1;
    font-weight: 800;
    color: #1A202C;
  }

  &__text {
    display: block;
    margin-top: 10rpx;
    font-size: 25rpx;
    color: #6B7B8D;
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.stat-card {
  padding: 24rpx;
  border-radius: 18rpx;
  background: #FFFFFF;
  border-top: 6rpx solid #F472B6;

  &.blue {
    border-top-color: #6C9EFF;
  }

  &.green {
    border-top-color: #34D399;
  }

  &.orange {
    border-top-color: #FB923C;
  }

  &__value {
    display: block;
    font-size: 42rpx;
    line-height: 1.1;
    font-weight: 800;
    color: #1A202C;
  }

  &__label {
    display: block;
    margin-top: 10rpx;
    font-size: 24rpx;
    color: #6B7B8D;
  }
}

.trend-section,
.recent-section {
  padding: 26rpx;
  border-radius: 18rpx;
  background: #FFFFFF;
  margin-bottom: 20rpx;
}

.section-header {
  margin-bottom: 22rpx;

  &__title {
    display: block;
    font-size: 30rpx;
    font-weight: 700;
    color: #1A202C;
  }

  &__sub {
    display: block;
    margin-top: 6rpx;
    font-size: 23rpx;
    color: #9BA8B7;
  }
}

.trend-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.trend-item {
  &__meta {
    display: flex;
    justify-content: space-between;
    margin-bottom: 10rpx;
  }

  &__date {
    font-size: 24rpx;
    color: #6B7B8D;
  }

  &__days {
    font-size: 24rpx;
    color: #BE185D;
    font-weight: 700;
  }

  &__bar {
    height: 16rpx;
    overflow: hidden;
    border-radius: 999rpx;
    background: #F4F6F9;
  }

  &__bar-fill {
    height: 100%;
    border-radius: 999rpx;
    background: linear-gradient(90deg, #F9A8D4 0%, #F472B6 100%);
  }
}

.record-list {
  display: flex;
  flex-direction: column;
}

.record-item {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 18rpx 0;
  border-bottom: 1px solid #F4F6F9;

  &:last-child {
    border-bottom: none;
  }

  &__date {
    width: 70rpx;
    height: 70rpx;
    border-radius: 18rpx;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: #FFF1F6;
    flex-shrink: 0;
  }

  &__day {
    font-size: 28rpx;
    line-height: 1;
    font-weight: 800;
    color: #BE185D;
  }

  &__month {
    margin-top: 6rpx;
    font-size: 20rpx;
    color: #BE185D;
  }

  &__main {
    flex: 1;
    min-width: 0;
  }

  &__type {
    display: block;
    font-size: 27rpx;
    font-weight: 700;
    color: #6B7B8D;

    &.start {
      color: #BE185D;
    }

    &.end {
      color: #2563EB;
    }
  }

  &__desc {
    display: block;
    margin-top: 8rpx;
    font-size: 23rpx;
    color: #9BA8B7;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 70rpx 30rpx;

  &.compact {
    padding: 50rpx 20rpx;
  }

  &__icon {
    width: 84rpx;
    height: 84rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: #FFF1F6;
    margin-bottom: 20rpx;
  }

  &__title {
    font-size: 28rpx;
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
</style>
