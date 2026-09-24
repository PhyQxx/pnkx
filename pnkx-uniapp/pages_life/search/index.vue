<template>
  <view class="search-page subpage-shell">
    <uni-search-bar v-model="keyword" placeholder="搜索待办、账本、日记、笔记…" focus @confirm="search" @clear="clear" />
    <scroll-view scroll-x class="filters">
      <view class="filters__inner">
        <text v-for="item in types" :key="item.value" class="filter" :class="{ active: type === item.value }" @click="changeType(item.value)">{{ item.label }}</text>
      </view>
    </scroll-view>
    <view class="advanced-filters">
      <picker :range="dateRanges" range-key="label" @change="changeDateRange">
        <view class="advanced-filter">{{ dateRanges[dateRangeIndex].label }}⌄</view>
      </picker>
      <uni-easyinput v-model="tag" class="tag-input" placeholder="标签" :clearable="true" @confirm="search" @clear="search" />
    </view>
    <view v-if="loading" class="state">搜索中…</view>
    <view v-else-if="searched && !results.length" class="state">没有找到相关内容</view>
    <view v-else class="results">
      <view v-for="item in results" :key="item.type + '-' + item.id" class="result" @click="open(item)">
        <view class="result__icon">{{ icon(item.type) }}</view>
        <view class="result__body">
          <text class="result__title">{{ item.title }}</text>
          <text class="result__sub">{{ item.subtitle || typeName(item.type) }}</text>
        </view>
        <uni-icons type="right" size="16" color="#94A3B8" />
      </view>
      <view v-if="results.length" class="ai-summary" @click="summarize">✨ AI 总结搜索结果</view>
      <view v-if="summary" class="summary-card">{{ summary }}</view>
    </view>
  </view>
</template>

<script>
import { globalSearch, summarizeSearch } from '@/api/px/life/search'

export default {
  data() {
    return {
      keyword: '', type: 'all', tag: '', results: [], summary: '', loading: false, searched: false,
      dateRangeIndex: 0,
      dateRanges: [
        { label: '全部时间', days: 0 }, { label: '最近7天', days: 7 },
        { label: '最近30天', days: 30 }, { label: '最近一年', days: 365 }
      ],
      types: [
        { label: '全部', value: 'all' }, { label: '文章', value: 'article' }, { label: '待办', value: 'todo' },
        { label: '账本', value: 'bookkeeping' }, { label: '日记', value: 'diary' },
        { label: '笔记', value: 'note' }, { label: '书籍', value: 'book' },
        { label: '菜谱', value: 'recipe' }, { label: '纪念日', value: 'commemoration' },
        { label: '订阅', value: 'subscription' }, { label: '购物', value: 'shopping' }
      ]
    }
  },
  methods: {
    async search() {
      const q = this.keyword.trim()
      if (!q) return this.clear()
      this.loading = true
      try {
        const response = await globalSearch(q, this.type, this.searchFilters())
        this.results = response.data || []
        this.summary = ''
        this.searched = true
      } finally { this.loading = false }
    },
    clear() { this.results = []; this.summary = ''; this.searched = false },
    changeType(type) { this.type = type; if (this.keyword.trim()) this.search() },
    changeDateRange(event) { this.dateRangeIndex = Number(event.detail.value); if (this.keyword.trim()) this.search() },
    searchFilters() {
      const selected = this.dateRanges[this.dateRangeIndex]
      if (!selected.days) return { tag: this.tag.trim() || undefined }
      const end = new Date()
      const start = new Date(end.getTime() - (selected.days - 1) * 86400000)
      const format = d => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
      return { startDate: format(start), endDate: format(end), tag: this.tag.trim() || undefined }
    },
    async summarize() {
      uni.showLoading({ title: 'AI 正在整理' })
      try {
        const response = await summarizeSearch({ q: this.keyword.trim(), type: this.type, ...this.searchFilters() })
        this.summary = response.data || ''
      } catch (e) {
        uni.showToast({ title: '摘要生成失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    },
    open(item) { if (item.appRoute) uni.navigateTo({ url: item.appRoute }) },
    typeName(type) { const found = this.types.find(item => item.value === type); return found?.label || type },
    icon(type) {
      const icons = { article: '📝', todo: '✅', bookkeeping: '💰', diary: '📔', note: '📌', book: '📚', recipe: '🍳', commemoration: '🎉', subscription: '💳', shopping: '🛒' }
      return icons[type] || '🔎'
    }
  }
}
</script>

<style lang="scss" scoped>
.search-page { min-height: 100vh; padding: 20rpx $page-padding; background: $bg-page; }
.filters { width: 100%; white-space: nowrap; margin: 12rpx 0 24rpx; }
.filters__inner { display: inline-flex; gap: 14rpx; }
.advanced-filters { display: flex; align-items: center; gap: 16rpx; margin-bottom: 22rpx; }
.advanced-filter { padding: 16rpx 22rpx; border-radius: $radius-md; background: $bg-card; color: $text-secondary; font-size: $font-caption; }
.tag-input { flex: 1; }
.filter { padding: 12rpx 24rpx; border-radius: $radius-full; background: $bg-card; color: $text-secondary; font-size: $font-caption; }
.filter.active { color: #fff; background: $primary; }
.result { display: flex; align-items: center; gap: 20rpx; margin-bottom: 14rpx; padding: 24rpx; background: $bg-card; border-radius: $radius-xl; box-shadow: $shadow-card; }
.result__icon { font-size: 38rpx; }
.result__body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 6rpx; }
.result__title { color: $text-primary; font-size: $font-body; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.result__sub { color: $text-tertiary; font-size: $font-caption; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.state { padding: 120rpx 0; text-align: center; color: $text-tertiary; }
.ai-summary { margin: 24rpx 0; padding: 22rpx; border-radius: $radius-lg; background: rgba($primary, 0.1); color: $primary; text-align: center; font-weight: 600; }
.summary-card { padding: 28rpx; border-radius: $radius-lg; background: $bg-card; color: $text-primary; font-size: $font-caption; line-height: 1.7; white-space: pre-wrap; }
</style>
