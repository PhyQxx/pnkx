<template>
    <div class="search-page">
        <!-- 搜索框 -->
        <van-search
            v-model="keyword"
            class="search-bar"
            placeholder="搜索待办、账本、日记、笔记…"
            clearable
            @search="search"
            @clear="clear"
        />

        <!-- 类型筛选 -->
        <div class="filters-scroll">
            <div class="filters-scroll__inner">
                <span
                    v-for="item in types"
                    :key="item.value"
                    class="filter-chip"
                    :class="{ 'filter-chip--active': type === item.value }"
                    @click="changeType(item.value)"
                >{{ item.label }}</span>
            </div>
        </div>

        <!-- 高级筛选：日期范围 + 标签 -->
        <div class="advanced-filters">
            <span class="advanced-filter" @click="showRangePicker = true">
                {{ dateRanges[dateRangeIndex].label }}
                <van-icon name="arrow-down" />
            </span>
            <van-field
                v-model="tag"
                class="tag-input"
                placeholder="标签"
                clearable
                :border="false"
                @keyup.enter="search"
                @clear="search"
            />
            <span class="advanced-filter advanced-filter--action" @click="search">搜索</span>
        </div>

        <!-- 状态 -->
        <div v-if="loading" class="search-state">搜索中…</div>
        <div v-else-if="searched && !results.length" class="m-empty">
            <span class="search-empty__emoji">🔎</span>
            <span>没有找到相关内容</span>
        </div>
        <div v-else-if="!searched" class="m-empty">
            <span class="search-empty__emoji">🔍</span>
            <span>输入关键词，搜索你的生活记录</span>
        </div>

        <!-- 分组结果 -->
        <div v-if="!loading && results.length" class="results">
            <div v-for="group in resultGroups" :key="group.type" class="result-group">
                <div class="result-group__header">
                    <span class="result-group__icon">{{ icon(group.type) }}</span>
                    <span class="result-group__name">{{ typeName(group.type) }}</span>
                    <span class="result-group__count">{{ group.items.length }}条</span>
                </div>
                <div class="result-group__list m-card">
                    <div
                        v-for="item in group.items"
                        :key="item.type + '-' + item.id"
                        class="result"
                        @click="open(item)"
                    >
                        <div class="result__body">
                            <div class="result__title">{{ item.title }}</div>
                            <div class="result__sub">{{ item.subtitle || typeName(item.type) }}</div>
                            <div v-if="item.createdAt || tagText(item)" class="result__meta">
                                <span v-if="item.createdAt">{{ item.createdAt }}</span>
                                <span v-if="tagText(item)"># {{ tagText(item) }}</span>
                            </div>
                        </div>
                        <van-icon name="arrow" class="result__arrow" />
                    </div>
                </div>
            </div>

            <!-- AI 总结 -->
            <div v-if="!summary" class="ai-summary" @click="summarize">✨ AI 总结搜索结果</div>
            <div v-if="summary" class="m-card summary-card">
                <div class="summary-card__title">✨ AI 总结</div>
                <div class="m-rich-content summary-card__content" v-html="summaryHtml"></div>
            </div>
        </div>

        <!-- 日期范围选择 -->
        <van-popup v-model:show="showRangePicker" position="bottom" round teleport="body">
            <van-picker
                :columns="rangeColumns"
                :model-value="[dateRanges[dateRangeIndex].label]"
                title="时间范围"
                @confirm="onRangeConfirm"
                @cancel="showRangePicker = false"
            />
        </van-popup>
    </div>
</template>

<script>
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { globalSearch, summarizeSearch } from '@/api/px/life/search'
import { uniappPathToMobile } from '@/mobile/router/map'
import { showToast, mLoading, closeToast, mError } from '@/mobile/utils/ui'

export default {
    name: 'MSearch',
    data() {
        return {
            keyword: '',
            type: 'all',
            tag: '',
            results: [],
            summary: '',
            loading: false,
            searched: false,
            dateRangeIndex: 0,
            dateRanges: [
                { label: '全部时间', days: 0 },
                { label: '最近7天', days: 7 },
                { label: '最近30天', days: 30 },
                { label: '最近一年', days: 365 }
            ],
            types: [
                { label: '全部', value: 'all' },
                { label: '文章', value: 'article' },
                { label: '待办', value: 'todo' },
                { label: '账本', value: 'bookkeeping' },
                { label: '日记', value: 'diary' },
                { label: '笔记', value: 'note' },
                { label: '书籍', value: 'book' },
                { label: '菜谱', value: 'recipe' },
                { label: '纪念日', value: 'commemoration' },
                { label: '订阅', value: 'subscription' },
                { label: '购物', value: 'shopping' }
            ],
            showRangePicker: false
        }
    },
    computed: {
        rangeColumns() {
            return this.dateRanges.map(r => r.label)
        },
        resultGroups() {
            const groups = []
            const byType = {}
            this.results.forEach(item => {
                if (!byType[item.type]) {
                    byType[item.type] = []
                    groups.push({ type: item.type, items: byType[item.type] })
                }
                byType[item.type].push(item)
            })
            return groups
        },
        summaryHtml() {
            if (!this.summary) return ''
            return DOMPurify.sanitize(marked.parse(this.summary), {
                USE_PROFILES: { html: true },
                FORBID_TAGS: ['script', 'style', 'iframe', 'object', 'embed'],
                FORBID_ATTR: ['onerror', 'onload', 'onclick', 'onmouseover']
            })
        }
    },
    methods: {
        async search() {
            const q = this.keyword.trim()
            if (!q) {
                this.clear()
                return
            }
            this.loading = true
            try {
                const response = await globalSearch(q, this.type, this.searchFilters())
                this.results = response.data || []
                this.summary = ''
                this.searched = true
            } catch (e) {
                showToast('搜索失败')
            } finally {
                this.loading = false
            }
        },
        clear() {
            this.results = []
            this.summary = ''
            this.searched = false
        },
        changeType(type) {
            this.type = type
            if (this.keyword.trim()) this.search()
        },
        onRangeConfirm({ selectedOptions, selectedIndexes }) {
            this.dateRangeIndex = selectedIndexes[0] != null ? selectedIndexes[0] : this.dateRanges.findIndex(r => r.label === selectedOptions[0])
            this.showRangePicker = false
            if (this.keyword.trim()) this.search()
        },
        searchFilters() {
            const selected = this.dateRanges[this.dateRangeIndex]
            const filters = { tag: this.tag.trim() || undefined }
            if (!selected.days) return filters
            const end = new Date()
            const start = new Date(end.getTime() - (selected.days - 1) * 86400000)
            return { ...filters, startDate: this.formatDate(start), endDate: this.formatDate(end) }
        },
        formatDate(d) {
            const m = (d.getMonth() + 1).toString().padStart(2, '0')
            const day = d.getDate().toString().padStart(2, '0')
            return d.getFullYear() + '-' + m + '-' + day
        },
        async summarize() {
            mLoading('AI 正在整理')
            try {
                const response = await summarizeSearch({
                    q: this.keyword.trim(),
                    type: this.type,
                    ...this.searchFilters()
                })
                this.summary = response.data || ''
            } catch (e) {
                mError('摘要生成失败')
            } finally {
                closeToast()
            }
        },
        tagText(item) {
            const tags = item.tags || []
            return tags.join(' / ')
        },
        open(item) {
            if (!item.appRoute) return
            const [raw, query] = String(item.appRoute).split('?')
            const base = uniappPathToMobile(raw)
            if (!base) {
                showToast('功能即将上线')
                return
            }
            const path = query ? base + '?' + query : base
            if (this.$router.resolve(path).matched.length > 0) {
                this.$router.push(path)
            } else {
                showToast('功能即将上线')
            }
        },
        typeName(type) {
            const found = this.types.find(item => item.value === type)
            return found ? found.label : type
        },
        icon(type) {
            const icons = {
                article: '📝', todo: '✅', bookkeeping: '💰', diary: '📔', note: '📌',
                book: '📚', recipe: '🍳', commemoration: '🎉', subscription: '💳', shopping: '🛒'
            }
            return icons[type] || '🔎'
        }
    }
}
</script>

<style lang="scss" scoped>
.search-page {
    min-height: calc(100vh - 46px);
    background: var(--pnkx-bg);
    padding: 0 12px 24px;
}

.search-bar {
    padding: 8px 0;
    margin: 0 -12px;
    background: var(--pnkx-bg);

    :deep(.van-search__content) {
        background: var(--pnkx-surface);
        border: 1px solid var(--pnkx-border-soft);
        border-radius: 999px;
    }
}

.filters-scroll {
    width: 100%;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    white-space: nowrap;
    margin-bottom: 10px;

    &__inner {
        display: inline-flex;
        gap: 8px;
    }
}

.filter-chip {
    padding: 5px 14px;
    border-radius: 999px;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    color: var(--pnkx-text-secondary);
    font-size: 12px;
    flex-shrink: 0;

    &--active {
        color: #fff;
        background: var(--pnkx-primary);
        border-color: var(--pnkx-primary);
    }
}

.advanced-filters {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
}

.advanced-filter {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 7px 12px;
    border-radius: 8px;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    color: var(--pnkx-text-secondary);
    font-size: 12px;
    flex-shrink: 0;

    &--action {
        color: var(--pnkx-primary);
        background: var(--pnkx-primary-soft);
        border-color: transparent;
    }
}

.tag-input {
    flex: 1;
    padding: 0;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 8px;

    :deep(.van-field__control) {
        font-size: 12px;
        padding: 7px 10px;
    }
}

.search-state {
    padding: 60px 0;
    text-align: center;
    color: var(--pnkx-text-muted);
    font-size: 13px;
}

.search-empty__emoji {
    font-size: 40px;
    opacity: 0.6;
}

.result-group {
    margin-bottom: 14px;

    &__header {
        display: flex;
        align-items: center;
        gap: 6px;
        margin-bottom: 8px;
    }

    &__icon {
        font-size: 16px;
    }

    &__name {
        font-size: 14px;
        font-weight: 600;
        color: var(--pnkx-text);
        flex: 1;
    }

    &__count {
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }

    &__list {
        margin-bottom: 0;
    }
}

.result {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 0;
    border-bottom: 1px solid var(--pnkx-border-soft);

    &:last-child {
        border-bottom: none;
    }

    &:active {
        opacity: 0.7;
    }

    &__body {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
        gap: 3px;
    }

    &__title {
        color: var(--pnkx-text);
        font-size: 14px;
        font-weight: 600;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__sub {
        color: var(--pnkx-text-secondary);
        font-size: 12px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__meta {
        display: flex;
        gap: 8px;
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }

    &__arrow {
        color: var(--pnkx-text-muted);
        flex-shrink: 0;
    }
}

.ai-summary {
    margin: 16px 0;
    padding: 12px;
    border-radius: 12px;
    background: var(--pnkx-primary-soft);
    color: var(--pnkx-primary);
    text-align: center;
    font-weight: 600;
    font-size: 13px;

    &:active {
        opacity: 0.8;
    }
}

.summary-card {
    &__title {
        font-size: 13px;
        font-weight: 600;
        color: var(--pnkx-primary);
        margin-bottom: 6px;
    }

    &__content {
        font-size: 13px;
        color: var(--pnkx-text);

        :deep(p) {
            margin: 4px 0;
        }
    }
}
</style>
