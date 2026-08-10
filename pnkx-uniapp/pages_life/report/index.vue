<template>
  <view class="life-report">
    <view class="header">
      <view class="period-selector">
        <view
          v-for="item in periods"
          :key="item.value"
          class="period-selector__item"
          :class="{ 'period-selector__item--active': currentPeriod === item.value }"
          @click="switchPeriod(item.value)"
        >
          {{ item.label }}
        </view>
      </view>
      <view class="date-range">
        <text class="date-range__text">{{ dateRangeText }}</text>
      </view>
    </view>

    <view class="report-tabs">
      <view
        v-for="tab in reportTypes"
        :key="tab.value"
        class="report-tabs__item"
        :class="{ 'report-tabs__item--active': currentType === tab.value }"
        @click="switchType(tab.value)"
      >
        {{ tab.label }}
      </view>
    </view>

    <view class="summary-cards">
      <view class="summary-card">
        <text class="summary-card__label">总支出</text>
        <text class="summary-card__value summary-card__value--expense">{{ expenseText }}</text>
        <text class="summary-card__sub">{{ reportData.bookkeeping.recordCount || 0 }}笔记录</text>
      </view>
      <view class="summary-card">
        <text class="summary-card__label">日记篇数</text>
        <text class="summary-card__value summary-card__value--diary">{{ reportData.diary.count || 0 }}</text>
        <text class="summary-card__sub">篇记录</text>
      </view>
      <view class="summary-card">
        <text class="summary-card__label">待办完成</text>
        <text class="summary-card__value summary-card__value--todo">{{ todoRate }}</text>
        <text class="summary-card__sub">{{ reportData.todo.done || 0 }}/{{ totalTodo }}</text>
      </view>
    </view>

    <view class="ai-report">
      <view class="ai-report__header">
        <view class="ai-report__icon">
          <text class="ai-report__icon-text">AI</text>
        </view>
        <text class="ai-report__title">智能分析报告</text>
        <view v-if="isStreaming" class="ai-report__loading">
          <view class="dot"></view>
          <view class="dot"></view>
          <view class="dot"></view>
        </view>
      </view>
      <view class="ai-report__content" v-if="aiReportHtml">
        <mp-html :content="aiReportHtml" :tag-style="htmlTagStyle" />
      </view>
      <view class="ai-report__empty" v-if="!aiReportHtml && !isStreaming">
        <text class="ai-report__empty-text">点击下方按钮生成 AI 报告</text>
      </view>
    </view>

    <view class="footer">
      <view class="generate-btn" :class="{ 'generate-btn--loading': isStreaming }" @click="generateReport">
        <text class="generate-btn__text">{{ isStreaming ? '生成中...' : '生成 AI 报告' }}</text>
      </view>
      <view class="safe-bottom"></view>
    </view>
  </view>
</template>
<script>
import { getLifeReportData } from '@/api/px/life/report'
import config from '@/config'
import { getToken } from '@/utils/auth'

export default {
  name: 'LifeReportIndex',
  data() {
    return {
      currentPeriod: 'week',
      currentType: 'summary',
      periods: [
        { label: '本周', value: 'week' },
        { label: '本月', value: 'month' }
      ],
      reportTypes: [
        { label: '综合', value: 'summary' },
        { label: '消费', value: 'expense' },
        { label: '心情', value: 'mood' }
      ],
      reportData: {
        bookkeeping: { totalExpense: 0, recordCount: 0 },
        diary: { count: 0, samples: [] },
        todo: { done: 0, undone: 0 }
      },
      aiReportHtml: '',
      isStreaming: false,
      xhr: null,
      htmlTagStyle: {
        h1: 'font-size:32rpx;font-weight:bold;color:#303133;margin:20rpx 0 10rpx;',
        h2: 'font-size:30rpx;font-weight:bold;color:#303133;margin:20rpx 0 10rpx;padding-bottom:8rpx;border-bottom:2rpx solid #e4e7ed;',
        h3: 'font-size:28rpx;font-weight:bold;color:#5A8DEE;margin:16rpx 0 8rpx;padding-left:12rpx;border-left:6rpx solid #5A8DEE;',
        h4: 'font-size:27rpx;font-weight:bold;color:#303133;margin:12rpx 0 6rpx;',
        p: 'margin:8rpx 0;line-height:1.7;',
        blockquote: 'border-left:6rpx solid #5A8DEE;padding:10rpx 16rpx;margin:10rpx 0;background:#f8f9ff;color:#606266;border-radius:0 8rpx 8rpx 0;',
        pre: 'background:#1e1e2e;border-radius:12rpx;padding:20rpx 24rpx;margin:16rpx 0;overflow-x:auto;',
        code: 'font-size:24rpx;font-family:Consolas,Monaco,monospace;',
        table: 'width:100%;border-collapse:collapse;font-size:24rpx;border:2rpx solid #e4e7ed;border-radius:8rpx;overflow:hidden;margin:16rpx 0;',
        th: 'background:#5A8DEE;color:#fff;padding:10rpx 16rpx;text-align:left;font-weight:bold;',
        td: 'padding:10rpx 16rpx;border-bottom:2rpx solid #f0f0f0;',
        ul: 'padding-left:28rpx;margin:6rpx 0;list-style-type:disc;',
        ol: 'padding-left:36rpx;margin:6rpx 0;',
        li: 'margin:6rpx 0;line-height:1.7;',
        hr: 'border:none;border-top:2rpx solid #e4e7ed;margin:16rpx 0;',
        strong: 'font-weight:bold;color:#303133;',
        em: 'font-style:italic;color:#606266;',
        a: 'color:#5A8DEE;text-decoration:none;'
      }
    }
  },
  computed: {
    dateRangeText() {
      var range = this.reportData.dateRange
      if (!range || !range.length) return ''
      return range[0] + ' ~ ' + range[range.length - 1]
    },
    expenseText() {
      var amount = this.reportData.bookkeeping.totalExpense || 0
      return '¥' + amount.toFixed(2)
    },
    totalTodo() {
      return (this.reportData.todo.done || 0) + (this.reportData.todo.undone || 0)
    },
    todoRate() {
      if (this.totalTodo === 0) return '0%'
      return Math.round((this.reportData.todo.done / this.totalTodo) * 100) + '%'
    }
  },
  onLoad() {
    this.loadReportData()
  },
  onUnload() {
    this.abortStream()
  },
  methods: {
    switchPeriod(period) {
      if (this.currentPeriod === period) return
      this.currentPeriod = period
      this.aiReportHtml = ''
      this.loadReportData()
    },
    switchType(type) {
      if (this.currentType === type) return
      this.currentType = type
      this.aiReportHtml = ''
    },
    async loadReportData() {
      try {
        var res = await getLifeReportData({
          period: this.currentPeriod,
          reportType: this.currentType
        })
        if (res.code === 200 && res.data) {
          this.reportData = {
            bookkeeping: res.data.bookkeeping || { totalExpense: 0, recordCount: 0 },
            diary: res.data.diary || { count: 0, samples: [] },
            todo: res.data.todo || { done: 0, undone: 0 },
            dateRange: res.data.dateRange || [],
            commemorationDay: res.data.commememorationDay || {},
            menstruation: res.data.menstruation || {}
          }
        }
      } catch (e) {
        console.error('获取报告数据失败', e)
      }
    },
    generateReport() {
      if (this.isStreaming) return
      this.aiReportHtml = ''
      this.createReportStream(this.currentPeriod, this.currentType)
    },
    createReportStream(period, reportType) {
      this.isStreaming = true
      var url = config.baseUrl + '/lifeReport/stream?period=' + encodeURIComponent(period) + '&reportType=' + encodeURIComponent(reportType)
      var xhr = new XMLHttpRequest()
      this.xhr = xhr
      var lastIndex = 0
      var fullContent = ''
      var eventLines = []
      var lastRenderTime = 0
      var RENDER_INTERVAL = 80
      var self = this
      xhr.open('GET', url, true)
      xhr.setRequestHeader('Authorization', 'Bearer ' + getToken())
      xhr.timeout = 120000
      xhr.onreadystatechange = function () {
        if (xhr.readyState >= 3) {
          var newData = xhr.responseText.substring(lastIndex)
          lastIndex = xhr.responseText.length
          if (!newData) return
          var lines = newData.split('\n')
          for (var i = 0; i < lines.length; i++) {
            var line = lines[i]
            if (line.indexOf('data:') === 0) {
              eventLines.push(line.substring(5))
            } else if (line === '' && eventLines.length > 0) {
              var data = eventLines.join('\n')
              eventLines = []
              if (data === '[DONE]') continue
              fullContent += data
              var now = Date.now()
              if (now - lastRenderTime >= RENDER_INTERVAL) {
                lastRenderTime = now
                self.aiReportHtml = self.formatMessage(fullContent)
              }
            }
          }
        }
      }
      xhr.onload = function () {
        if (eventLines.length > 0) {
          var remaining = eventLines.join('\n').trim()
          if (remaining && remaining !== '[DONE]') {
            fullContent += remaining
          }
        }
        self.aiReportHtml = self.formatMessage(fullContent) || '<p>报告生成失败，请稍后重试。</p>'
        self.isStreaming = false
      }
      xhr.onerror = function () {
        self.aiReportHtml = self.aiReportHtml || '<p>网络请求失败，请稍后重试。</p>'
        self.isStreaming = false
      }
      xhr.ontimeout = function () {
        self.aiReportHtml = self.aiReportHtml || '<p>请求超时，请稍后重试。</p>'
        self.isStreaming = false
      }
      xhr.send()
    },
    abortStream() {
      if (this.xhr) {
        this.xhr.abort()
        this.xhr = null
      }
      this.isStreaming = false
    },
    escapeHtml(str) {
      return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    },
    formatMessage(content) {
      if (!content) return ''
      // 0. 过滤指令性文本
      let html = content.replace(/\n?回复“确认”保存，回复“取消”放弃。/g, '')
                        .replace(/\[PENDING_CONFIRM\]/gi, '')
                        .trim()
      const codeBlocks = []
      const inlineCodes = []

      // 1. 提取代码块 → 占位符
      html = html.replace(/```(\w*)\n?([\s\S]*?)```/g, (_, lang, code) => {
        codeBlocks.push('<pre><code>' + this.escapeHtml(code.trim()) + '</code></pre>')
        return '%%CB' + (codeBlocks.length - 1) + '%%'
      })

      // 2. 提取行内代码 → 占位符
      html = html.replace(/`([^`\n]+)`/g, (_, code) => {
        inlineCodes.push('<code style="background:#f0f0f5;color:#e83e8c;padding:2rpx 10rpx;border-radius:6rpx;">' + this.escapeHtml(code) + '</code>')
        return '%%IC' + (inlineCodes.length - 1) + '%%'
      })

      // 3. 转义HTML
      html = html.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

      // 4. 先处理内联格式（加粗、斜体）
      html = html.replace(/\*\*([^*]+?)\*\*/g, '<strong>$1</strong>')
      html = html.replace(/\*([^*\n]+?)\*/g, '<em>$1</em>')

      // 5. 逐行处理块级元素
      const lines = html.split(/\r?\n/)
      const result = []
      let tableRows = []
      let tableHasHeader = false
      let ulItems = []
      let olItems = []

      for (const line of lines) {
        const trimmed = line.trim()

        // 收集中的表格
        if (tableRows.length > 0) {
          if (trimmed.startsWith('|')) {
            if (/^\|[\s\-:|]+\|$/.test(trimmed)) continue
            const cells = trimmed.split('|').slice(1, -1)
            const tag = tableHasHeader ? 'td' : 'th'
            tableHasHeader = true
            tableRows.push('<tr>' + cells.map(c => '<' + tag + '>' + c.trim() + '</' + tag + '>').join('') + '</tr>')
            continue
          }
          result.push('<table>' + tableRows.join('') + '</table>')
          tableRows = []
          tableHasHeader = false
        }

        // 收集中的无序列表
        if (ulItems.length > 0 && !/^[*\-] /.test(trimmed)) {
          result.push('<ul>' + ulItems.join('') + '</ul>')
          ulItems = []
        }

        // 收集中的有序列表
        if (olItems.length > 0 && !/^\d+\. /.test(trimmed)) {
          result.push('<ol>' + olItems.join('') + '</ol>')
          olItems = []
        }

        // 标题
        if (trimmed.startsWith('#### ')) {
          result.push('<h4>' + trimmed.substring(5) + '</h4>')
        } else if (trimmed.startsWith('### ')) {
          result.push('<h3>' + trimmed.substring(4) + '</h3>')
        } else if (trimmed.startsWith('## ')) {
          result.push('<h2>' + trimmed.substring(3) + '</h2>')
        } else if (trimmed.startsWith('# ')) {
          result.push('<h1>' + trimmed.substring(2) + '</h1>')
        }
        // 分割线
        else if (/^---+$/.test(trimmed)) {
          result.push('<hr/>')
        }
        // 引用
        else if (trimmed.startsWith('&gt; ')) {
          result.push('<blockquote><p>' + trimmed.substring(5) + '</p></blockquote>')
        }
        // 表格行
        else if (trimmed.startsWith('|')) {
          if (/^\|[\s\-:|]+\|$/.test(trimmed)) continue
          const cells = trimmed.split('|').slice(1, -1)
          tableRows.push('<tr>' + cells.map(c => '<th>' + c.trim() + '</th>').join('') + '</tr>')
          tableHasHeader = true
        }
        // 无序列表
        else if (/^[*\-] /.test(trimmed)) {
          ulItems.push('<li>' + trimmed.substring(2) + '</li>')
        }
        // 有序列表
        else if (/^\d+\. /.test(trimmed)) {
          olItems.push('<li>' + trimmed.replace(/^\d+\. /, '') + '</li>')
        }
        // 空行
        else if (!trimmed) {
          result.push('')
        }
        // 普通文本
        else {
          result.push(trimmed)
        }
      }

      // 收尾未闭合的块
      if (tableRows.length > 0) result.push('<table>' + tableRows.join('') + '</table>')
      if (ulItems.length > 0) result.push('<ul>' + ulItems.join('') + '</ul>')
      if (olItems.length > 0) result.push('<ol>' + olItems.join('') + '</ol>')

      html = result.join('<br/>')

      // 还原占位符
      inlineCodes.forEach((code, i) => {
        html = html.replace('%%IC' + i + '%%', code)
      })
      codeBlocks.forEach((block, i) => {
        html = html.replace('%%CB' + i + '%%', block)
      })

      return html
    },
  }
}
</script>

<style lang="scss" scoped>
.life-report {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 140rpx;
}

.header {
  background: linear-gradient(135deg, $primary 0%, $primary-dark 100%);
  padding: $spacing-lg;
  padding-top: $spacing-md;
  color: $text-inverse;
}

.period-selector {
  display: flex;
  background: rgba(255, 255, 255, 0.2);
  border-radius: $radius-full;
  padding: 4rpx;
  margin-bottom: $spacing-md;
}

.period-selector__item {
  flex: 1;
  text-align: center;
  padding: $spacing-sm 0;
  font-size: $font-body;
  color: rgba(255, 255, 255, 0.8);
  border-radius: $radius-full;
  transition: all $duration-fast $ease-default;
}

.period-selector__item--active {
  background: rgba(255, 255, 255, 0.95);
  color: $primary-dark;
  font-weight: $font-weight-semibold;
}

.date-range {
  text-align: center;
}

.date-range__text {
  font-size: $font-caption;
  opacity: 0.8;
}

.report-tabs {
  display: flex;
  background: $bg-card;
  padding: $spacing-md $spacing-lg;
  margin: $section-gap;
  margin-top: -24rpx;
  border-radius: $radius-lg;
  box-shadow: $shadow-card;
}

.report-tabs__item {
  flex: 1;
  text-align: center;
  padding: $spacing-sm;
  font-size: $font-body;
  color: $text-secondary;
  position: relative;
  transition: color $duration-fast $ease-default;
}

.report-tabs__item--active {
  color: $primary-dark;
  font-weight: $font-weight-medium;
}

.report-tabs__item--active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 48rpx;
  height: 4rpx;
  background: $primary;
  border-radius: 4rpx;
}

.summary-cards {
  display: flex;
  gap: $section-gap;
  padding: 0 $section-gap;
  margin-bottom: $section-gap;
}

.summary-card {
  flex: 1;
  background: $bg-card;
  border-radius: $radius-lg;
  padding: $spacing-md;
  box-shadow: $shadow-card;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.summary-card__label {
  font-size: $font-caption;
  color: $text-tertiary;
  margin-bottom: $spacing-xs;
}

.summary-card__value {
  font-size: $font-h2;
  font-weight: $font-weight-semibold;
  margin-bottom: 4rpx;
}

.summary-card__value--expense {
  color: $danger;
}

.summary-card__value--diary {
  color: $diary;
}

.summary-card__value--todo {
  color: $todo;
}

.summary-card__sub {
  font-size: $font-mini;
  color: $text-tertiary;
}

.ai-report {
  background: $bg-card;
  margin: 0 $section-gap;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  box-shadow: $shadow-card;
}

.ai-report__header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-lg;
  padding-bottom: $spacing-md;
  border-bottom: 1rpx solid $border-light;
}

.ai-report__icon {
  width: 56rpx;
  height: 56rpx;
  border-radius: $radius-full;
  background: linear-gradient(135deg, #5A8DEE, #6C63FF);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ai-report__icon-text {
  color: #fff;
  font-size: 22rpx;
  font-weight: bold;
}

.ai-report__title {
  font-size: $font-h3;
  font-weight: $font-weight-semibold;
  color: $text-primary;
  flex: 1;
}

.ai-report__loading {
  display: flex;
  gap: 8rpx;
  align-items: center;
}

.ai-report__loading .dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: $primary;
  animation: report-bounce 1.4s infinite ease-in-out;
}

.ai-report__loading .dot:nth-child(1) {
  animation-delay: 0s;
}

.ai-report__loading .dot:nth-child(2) {
  animation-delay: 0.2s;
}

.ai-report__loading .dot:nth-child(3) {
  animation-delay: 0.4s;
}

.ai-report__content {
  font-size: $font-body;
  line-height: $line-height-relaxed;
  color: $text-primary;
}

.ai-report__empty {
  padding: 48rpx 0;
  text-align: center;
}

.ai-report__empty-text {
  font-size: $font-caption;
  color: $text-tertiary;
}

@keyframes report-bounce {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-12rpx);
    opacity: 1;
  }
}

.footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: $bg-card;
  padding: $spacing-md $spacing-lg 0;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);
}

.generate-btn {
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, $primary, $primary-dark);
  border-radius: $radius-lg;
}

.generate-btn:active {
  opacity: 0.85;
}

.generate-btn--loading {
  opacity: 0.6;
  pointer-events: none;
}

.generate-btn__text {
  font-size: $font-h3;
  font-weight: $font-weight-semibold;
  color: $text-inverse;
}

.safe-bottom {
  height: env(safe-area-inset-bottom);
}
</style>
