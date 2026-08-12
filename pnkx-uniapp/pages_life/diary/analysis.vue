<!--
 * @File: analysis
 * @Author: PHY
 * @Description: diary mood analysis page
-->
<template>
  <view class="diary-analysis subpage-shell">
    <!-- Header -->
    <view class="header">
      <view class="header-title">
        <text class="header-title-text">心情分析</text>
        <text class="header-subtitle">{{ analysisSubtitle }}</text>
      </view>
      <view class="range-switch">
        <view class="switch-item" :class="{ active: isAll === false }" @click="switchRange(false)">
          <text>本月</text>
        </view>
        <view class="switch-item" :class="{ active: isAll === true }" @click="switchRange(true)">
          <text>全部</text>
        </view>
      </view>
    </view>
    <scroll-view class="content-scroll" scroll-y>
      <!-- Stats -->
      <view class="stats-card">
        <view class="stats-item">
          <text class="stats-value">{{ analysisData.total || 0 }}</text>
          <text class="stats-label">日记总数</text>
        </view>
        <view class="stats-divider"></view>
        <view class="stats-item">
          <text class="stats-value">{{ moodDistribution.length }}</text>
          <text class="stats-label">心情种类</text>
        </view>
        <view class="stats-divider"></view>
        <view class="stats-item">
          <text class="stats-value">{{ dominantMood }}</text>
          <text class="stats-label">主导心情</text>
        </view>
      </view>
      <view class="section-card" v-if="moodDistribution.length > 0">
        <text class="section-title">心情分布</text>
        <view class="mood-chart">
          <view v-for="(item, index) in moodDistribution" :key="index" class="mood-bar-row">
            <text class="mood-label">{{ item.name }}</text>
            <view class="mood-bar-track">
              <view class="mood-bar-fill" :style="{ width: getBarWidth(item.value) + '%', backgroundColor: getMoodColor(item.name) }"></view>
            </view>
            <text class="mood-count">{{ item.value }}</text>
          </view>
        </view>
      </view>
      <view class="section-card" v-if="analysisData.timeline && analysisData.timeline.length > 0">
        <text class="section-title">心情趋势</text>
        <view class="trend-chart">
          <view class="trend-bars">
            <view v-for="(item, index) in analysisData.timeline" :key="index" class="trend-item">
              <view class="trend-bar" :style="{ backgroundColor: getMoodColor(item.mood) }">
                <text class="trend-bar-label">{{ item.mood ? item.mood.charAt(0) : '' }}</text>
              </view>
              <text class="trend-date">{{ formatShortDate(item.date) }}</text>
            </view>
          </view>
        </view>
      </view>
      <view class="section-card" v-if="analysisData.timeline && analysisData.timeline.length > 0">
        <text class="section-title">日记记录</text>
        <view class="timeline-list">
          <view v-for="(item, index) in analysisData.timeline" :key="index" class="timeline-item">
            <view class="timeline-dot" :style="{ backgroundColor: getMoodColor(item.mood) }"></view>
            <view class="timeline-content">
              <view class="timeline-header">
                <text class="timeline-date">{{ item.date }}</text>
                <view class="timeline-mood-tag" :style="{ backgroundColor: getMoodColor(item.mood) + '20', color: getMoodColor(item.mood) }">{{ item.mood }}</view>
                <view v-if="item.weather" class="timeline-weather-tag">{{ item.weather }}</view>
              </view>
              <text class="timeline-title">{{ item.title }}</text>
              <text v-if="item.contentPreview" class="timeline-preview">{{ item.contentPreview }}</text>
            </view>
          </view>
        </view>
      </view>


      <!-- AI Analysis -->
      <view class="section-card">
        <view class="ai-header">
          <text class="section-title">AI 心情洞察</text>
          <view class="ai-run-btn" @click="runAiAnalysis">
            <text>{{ isStreaming ? '分析中...' : (aiResult ? '重新分析' : '开始分析') }}</text>
          </view>
        </view>
        <view v-if="isStreaming && !aiResult" class="ai-loading">
          <view class="typing-indicator">
            <view class="dot"></view>
            <view class="dot"></view>
            <view class="dot"></view>
          </view>
          <text class="ai-loading-text">AI正在分析你的日记...</text>
        </view>
        <view v-if="aiResult" class="ai-result">
          <mp-html :content="aiResult" :tag-style="htmlTagStyle" />
          <text v-if="isStreaming" class="typing-cursor">|</text>
        </view>
        <view v-if="!isStreaming && !aiResult" class="ai-empty">
          <text class="ai-empty-text">点击上方按钮，让AI分析你的心情变化</text>
        </view>
      </view>

      <view style="height: 60rpx;"></view>
    </scroll-view>
  </view>
</template>

<script>
import { getDiaryAnalysisData } from '@/api/px/life/diaryAnalysis'
import config from '@/config'
import { getToken } from '@/utils/auth'

const baseUrl = config.baseUrl

function createStreamRequest(url, params) {
  const token = getToken()
  const xhr = new XMLHttpRequest()
  let _resolve, _reject, _onChunk
  let lastIndex = 0

  const completionPromise = new Promise((resolve, reject) => {
    _resolve = resolve
    _reject = reject
  })

  const paramStr = Object.keys(params).map(k => k + '=' + encodeURIComponent(params[k])).join('&')
  xhr.open('GET', url + (paramStr ? '?' + paramStr : ''))
  if (token) xhr.setRequestHeader('Authorization', 'Bearer ' + token)
  xhr.timeout = 120000

  xhr.onprogress = () => {
    const newData = xhr.responseText.substring(lastIndex)
    lastIndex = xhr.responseText.length
    if (_onChunk && newData) _onChunk(newData)
  }

  xhr.onload = () => _resolve()
  xhr.onerror = () => _reject(new Error('Network error'))
  xhr.ontimeout = () => _reject(new Error('Timeout'))

  xhr.send()

  return {
    onChunk(callback) { _onChunk = callback },
    completionPromise
  }
}

export default {
  name: 'DiaryAnalysis',
  data() {
    return {
      isAll: false,
      analysisData: {
        total: 0,
        moodDistribution: [],
        timeline: []
      },
      aiResult: '',
      isStreaming: false,
      htmlTagStyle: {
        h1: 'font-size:32rpx;font-weight:bold;color:#123B70;margin:20rpx 0 10rpx;',
        h2: 'font-size:30rpx;font-weight:bold;color:#123B70;margin:20rpx 0 10rpx;',
        h3: 'font-size:28rpx;font-weight:bold;color:#A78BFA;margin:16rpx 0 8rpx;padding-left:12rpx;border-left:6rpx solid #A78BFA;',
        p: 'margin:8rpx 0;line-height:1.7;',
        blockquote: 'border-left:6rpx solid #A78BFA;padding:10rpx 16rpx;margin:10rpx 0;background:#f8f9ff;color:#4F627B;border-radius:0 8rpx 8rpx 0;',
        ul: 'padding-left:28rpx;margin:6rpx 0;list-style-type:disc;',
        ol: 'padding-left:36rpx;margin:6rpx 0;',
        li: 'margin:6rpx 0;line-height:1.7;',
        strong: 'font-weight:bold;color:#123B70;',
        em: 'font-style:italic;color:#4F627B;',
      }
    }
  },
  computed: {
    moodDistribution() {
      return this.analysisData.moodDistribution || []
    },
    dominantMood() {
      if (this.moodDistribution.length === 0) return '-'
      const sorted = [...this.moodDistribution].sort((a, b) => (b.value || 0) - (a.value || 0))
      return sorted[0].name
    },
    analysisSubtitle() {
      return this.isAll ? '全部日记' : '本月日记'
    }
  },
  onLoad() {
    this.loadData()
  },
  methods: {
    switchRange(isAll) {
      this.isAll = isAll
      this.aiResult = ''
      this.loadData()
    },
    async loadData() {
      try {
        const res = await getDiaryAnalysisData({ isAll: this.isAll })
        if (res.code === 200) {
          this.analysisData = res.data || { total: 0, moodDistribution: [], timeline: [] }
        }
      } catch (error) {
        console.error('加载分析数据失败:', error)
      }
    },
    getMoodColor(mood) {
      if (!mood) return '#8EA0B8'
      const colorMap = {
        '开心': '#4ADE80', '快乐': '#4ADE80', '高兴': '#4ADE80',
        '难过': '#4F86F7', '伤心': '#4F86F7', '悲伤': '#4F86F7',
        '平静': '#8EA0B8', '淡定': '#8EA0B8', '普通': '#8EA0B8',
        '焦虑': '#FBBF24', '担忧': '#FBBF24', '紧张': '#FBBF24',
        '愤怒': '#FF6B6B', '生气': '#FF6B6B', '烦躁': '#FF6B6B',
      }
      return colorMap[mood] || '#A78BFA'
    },
    getBarWidth(value) {
      const maxVal = Math.max(...this.moodDistribution.map(m => m.value || 0), 1)
      return Math.max((value / maxVal) * 100, 4)
    },
    formatShortDate(dateStr) {
      if (!dateStr) return ''
      const parts = dateStr.split('-')
      if (parts.length >= 3) return parts[1] + '/' + parts[2]
      return dateStr.substring(5, 10)
    },
    async runAiAnalysis() {
      if (this.isStreaming) return
      this.isStreaming = true
      this.aiResult = ''
      try {
        const stream = createStreamRequest(baseUrl + '/diary/analysis/stream', { isAll: this.isAll })
        await this.readStreamResponse(stream)
      } catch (error) {
        console.error('AI分析失败:', error)
        if (!this.aiResult) this.aiResult = '<p>分析失败，请稍后重试。</p>'
      } finally {
        this.isStreaming = false
      }
    },
    async readStreamResponse({ onChunk, completionPromise }) {
      let fullContent = ''
      let eventLines = []
      onChunk((chunk) => {
          const lines = chunk.split('\n')
        for (const line of lines) {
          if (line.startsWith('data:')) {
            eventLines.push(line.substring(5))
          } else if (line === '' && eventLines.length > 0) {
            const data = eventLines.join('\n')
            eventLines = []
            if (data === '[DONE]') continue
            fullContent += data
            this.aiResult = this.formatMarkdown(fullContent)
          }
        }
      })
      try { await completionPromise } catch (e) {}
      if (eventLines.length > 0) {
        const data = eventLines.join('\n').trim()
        if (data && data !== '[DONE]') fullContent += data
      }
      this.aiResult = this.formatMarkdown(fullContent || '') || '<p>AI 暂时无法分析，请稍后再试。</p>'
    },
    formatMarkdown(content) {
      if (!content) return ''
      let html = content
      html = html.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      html = html.replace(/\*\*([^*]+?)\*\*/g, '<strong></strong>')
        html = html.replace(/\*([^*\n]+?)\*/g, '<em></em>')
      const lines = html.split(/\r?\n/)
      const result = []
      for (const line of lines) {
        const trimmed = line.trim()
        if (trimmed.startsWith('### ')) result.push('<h3>' + trimmed.substring(4) + '</h3>')
        else if (trimmed.startsWith('## ')) result.push('<h2>' + trimmed.substring(3) + '</h2>')
        else if (trimmed.startsWith('# ')) result.push('<h1>' + trimmed.substring(2) + '</h1>')
        else if (/^[*\-] /.test(trimmed)) result.push('<p>&#8226; ' + trimmed.substring(2) + '</p>')
        else if (trimmed) result.push('<p>' + trimmed + '</p>')
      }
      return result.join('')
    }
  }
}
</script>


<style lang="scss" scoped>
.diary-analysis {
  min-height: 100vh;
  background: $bg-page;
  display: flex;
  flex-direction: column;

  .header {
    background: linear-gradient(135deg, $diary 0%, #EC4899 100%);
    padding: $spacing-lg $page-padding;
    color: $text-inverse;

    .header-title {
      margin-bottom: $spacing-md;

      .header-title-text {
        font-size: $font-h1;
        font-weight: $font-weight-bold;
        display: block;
      }

      .header-subtitle {
        font-size: $font-caption;
        opacity: 0.8;
        margin-top: $spacing-xs;
        display: block;
      }
    }

    .range-switch {
      display: flex;
      gap: $spacing-xs;
      padding: $spacing-2xs;
      background: rgba(255, 255, 255, 0.2);
      border-radius: $radius-full;

      .switch-item {
        flex: 1;
        text-align: center;
        padding: $spacing-xs 0;
        border-radius: $radius-full;
        font-size: $font-caption;
        color: rgba(255, 255, 255, 0.7);
        transition: all $duration-fast $ease-default;

        &.active {
          background: rgba(255, 255, 255, 0.95);
          color: $diary;
          font-weight: $font-weight-semibold;
          box-shadow: $shadow-sm;
        }
      }
    }
  }

  .content-scroll {
    flex: 1;
    padding: $section-gap $page-padding;
  }

  .stats-card {
    display: flex;
    align-items: center;
    justify-content: space-around;
    background: $bg-card;
    border-radius: $radius-lg;
    padding: $spacing-lg;
    margin-bottom: $section-gap;
    box-shadow: $shadow-card;

    .stats-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: $spacing-xs;

      .stats-value {
        font-size: $font-h1;
        font-weight: $font-weight-bold;
        color: $diary;
      }

      .stats-label {
        font-size: $font-mini;
        color: $text-tertiary;
      }
    }

    .stats-divider {
      width: 1rpx;
      height: 60rpx;
      background: $border-light;
    }
  }

  .section-card {
    background: $bg-card;
    border-radius: $radius-lg;
    padding: $spacing-lg;
    margin-bottom: $section-gap;
    box-shadow: $shadow-card;

    .section-title {
      font-size: $font-h3;
      font-weight: $font-weight-semibold;
      color: $text-primary;
      margin-bottom: $spacing-md;
      display: block;
    }
  }

  .mood-chart {
    .mood-bar-row {
      display: flex;
      align-items: center;
      margin-bottom: $spacing-sm;

      .mood-label {
        width: 80rpx;
        font-size: $font-caption;
        color: $text-secondary;
        text-align: right;
        margin-right: $spacing-sm;
        flex-shrink: 0;
      }

      .mood-bar-track {
        flex: 1;
        height: 32rpx;
        background-color: $gray-100;
        border-radius: $radius-full;
        overflow: hidden;
      }

      .mood-bar-fill {
        height: 100%;
        border-radius: $radius-full;
        transition: width $duration-normal $ease-default;
        min-width: 16rpx;
      }

      .mood-count {
        width: 56rpx;
        font-size: $font-caption;
        color: $text-tertiary;
        text-align: right;
        margin-left: $spacing-sm;
        flex-shrink: 0;
      }
    }
  }

  .trend-chart {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;

    .trend-bars {
      display: flex;
      align-items: flex-end;
      gap: $spacing-xs;
      min-width: max-content;
      padding-bottom: $spacing-xs;
    }

    .trend-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      width: 48rpx;

      .trend-bar {
        width: 40rpx;
        height: 80rpx;
        border-radius: $radius-sm $radius-sm 0 0;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: $spacing-xs;

        .trend-bar-label {
          font-size: $font-mini;
          color: $text-inverse;
        }
      }

      .trend-date {
        font-size: 16rpx;
        color: $text-tertiary;
        transform: rotate(-45deg);
        transform-origin: center;
        white-space: nowrap;
      }
    }
  }

  .timeline-list {
    .timeline-item {
      display: flex;
      padding: $spacing-sm 0;
      position: relative;

      &:not(:last-child)::after {
        content: '';
        position: absolute;
        left: 11rpx;
        top: 40rpx;
        bottom: 0;
        width: 2rpx;
        background: $border-light;
      }

      .timeline-dot {
        width: 24rpx;
        height: 24rpx;
        border-radius: $radius-full;
        flex-shrink: 0;
        margin-right: $spacing-sm;
        margin-top: 6rpx;
      }

      .timeline-content {
        flex: 1;
        min-width: 0;

        .timeline-header {
          display: flex;
          align-items: center;
          gap: $spacing-xs;
          margin-bottom: $spacing-xs;
          flex-wrap: wrap;

          .timeline-date {
            font-size: $font-caption;
            color: $text-tertiary;
          }

          .timeline-mood-tag {
            font-size: $font-mini;
            padding: 2rpx 12rpx;
            border-radius: $radius-full;
            font-weight: $font-weight-medium;
          }

          .timeline-weather-tag {
            font-size: $font-mini;
            color: $text-tertiary;
            padding: 2rpx 12rpx;
            background: $gray-50;
            border-radius: $radius-full;
          }
        }

        .timeline-title {
          font-size: $font-body;
          color: $text-primary;
          font-weight: $font-weight-medium;
          display: block;
          margin-bottom: $spacing-2xs;
        }

        .timeline-preview {
          font-size: $font-caption;
          color: $text-secondary;
          display: block;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }
  }

  .ai-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: $spacing-md;

    .section-title {
      margin-bottom: 0;
    }

    .ai-run-btn {
      padding: $spacing-xs $spacing-lg;
      border-radius: $radius-full;
      background: linear-gradient(135deg, $diary 0%, #EC4899 100%);
      color: $text-inverse;
      font-size: $font-caption;
      font-weight: $font-weight-medium;

      &:active {
        transform: scale(0.95);
      }
    }
  }

  .ai-loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: $spacing-xl 0;

    .typing-indicator {
      display: flex;
      gap: $spacing-xs;
      margin-bottom: $spacing-sm;

      .dot {
        width: 16rpx;
        height: 16rpx;
        border-radius: $radius-full;
        background-color: $diary;
        animation: typing 1.4s infinite;

        &:nth-child(2) { animation-delay: 0.2s; }
        &:nth-child(3) { animation-delay: 0.4s; }
      }
    }

    .ai-loading-text {
      font-size: $font-caption;
      color: $text-tertiary;
    }
  }

  .ai-result {
    line-height: $line-height-relaxed;
  }

  .typing-cursor {
    font-weight: bold;
    color: $diary;
    animation: blink 1s infinite;
  }

  .ai-empty {
    padding: $spacing-lg 0;
    text-align: center;

    .ai-empty-text {
      font-size: $font-caption;
      color: $text-tertiary;
    }
  }
}

@keyframes typing {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}
</style>
