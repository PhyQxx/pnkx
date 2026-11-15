<template>
    <div class="m-article-page">
        <!-- 搜索栏 -->
        <van-search
            v-model="searchKeyword"
            class="m-article-page__search"
            placeholder="搜索标题、摘要、链接..."
            shape="round"
            :clearable="true"
            @search="handleSearch"
            @clear="handleSearch"
            @update:model-value="onSearchInput"
        />

        <!-- 状态 + 分类筛选 -->
        <div class="m-article-tabs">
            <div class="m-article-tabs__scroll">
                <span
                    v-for="tab in statusTabs"
                    :key="tab.value"
                    class="m-article-tabs__item"
                    :class="{ active: activeStatus === tab.value }"
                    @click="switchStatus(tab.value)"
                >{{ tab.label }}</span>
                <span class="m-article-tabs__sep">|</span>
                <span
                    v-for="item in categories"
                    :key="item"
                    class="m-article-tabs__item"
                    :class="{ active: activeCategory === item }"
                    @click="switchCategory(item)"
                >{{ item }}</span>
            </div>
        </div>

        <!-- 标签筛选（有标签数据才显示） -->
        <div v-if="tagOptions.length" class="m-article-tabs m-article-tabs--tag">
            <div class="m-article-tabs__scroll">
                <span
                    class="m-article-tabs__item m-article-tabs__item--tag"
                    :class="{ active: !activeTag }"
                    @click="switchTag('')"
                >全部标签</span>
                <span
                    v-for="tag in tagOptions"
                    :key="tag"
                    class="m-article-tabs__item m-article-tabs__item--tag"
                    :class="{ active: activeTag === tag }"
                    @click="switchTag(tag)"
                ># {{ tag }}</span>
            </div>
        </div>

        <!-- 收藏列表 -->
        <van-pull-refresh v-model="isRefreshing" class="m-pull m-article-pull" @refresh="onRefresh">
            <van-list
                v-model:loading="loading"
                :finished="loadFinished"
                finished-text="没有更多了"
                @load="loadMore"
            >
                <div v-if="articleList.length === 0 && !loading" class="m-empty">
                    <van-icon name="link-o" size="42" color="var(--pnkx-primary)" />
                    <span>还没有收藏</span>
                    <span class="m-text-muted">点击右下角 + 粘贴网址，自动抓取正文并 AI 整理</span>
                </div>

                <van-swipe-cell v-for="item in articleList" :key="item.id" class="m-article-card">
                    <div class="m-article-card__body" @click="openDetail(item)">
                        <div class="m-article-card__head">
                            <span class="m-article-card__title" :class="{ unread: item.readFlag === '0' }">
                                {{ item.title || item.url }}
                            </span>
                            <van-icon
                                :name="item.starFlag === '1' ? 'star' : 'star-o'"
                                size="18"
                                class="m-article-card__star"
                                :class="{ stared: item.starFlag === '1' }"
                                @click.stop="toggleStar(item)"
                            />
                        </div>
                        <div v-if="item.summary" class="m-article-card__summary">{{ item.summary }}</div>
                        <div class="m-article-card__foot">
                            <div class="m-article-card__meta">
                                <span v-if="item.category" class="m-article-card__category">{{ item.category }}</span>
                                <span class="m-article-card__domain">{{ item.domain || '' }}</span>
                                <span>{{ formatTime(item.createTime) }}</span>
                            </div>
                            <div class="m-article-card__status">
                                <span v-if="item.fetchStatus === '0'" class="pending">抓取中…</span>
                                <span v-else-if="item.fetchStatus === '2'" class="error">抓取失败</span>
                                <span v-else-if="item.aiStatus === '0'" class="pending">AI整理中…</span>
                                <span v-else-if="item.aiStatus === '2'" class="error">整理失败</span>
                                <van-icon v-else name="checked" size="13" class="ok" />
                            </div>
                        </div>
                        <div v-if="item.tags" class="m-article-card__tags">
                            <span v-for="tag in splitTags(item.tags)" :key="tag" class="m-article-card__tag">{{ tag }}</span>
                        </div>
                    </div>
                    <template #right>
                        <van-button square type="primary" text="编辑" class="m-article-card__action" @click="handleEdit(item)" />
                        <van-button square type="danger" text="删除" class="m-article-card__action" @click="handleDelete(item)" />
                    </template>
                </van-swipe-cell>
            </van-list>
        </van-pull-refresh>

        <!-- FAB：收藏网址 / RSS 订阅 -->
        <button class="m-fab m-fab--no-tabbar" @click="showFabActions = true">
            <van-icon name="plus" />
        </button>
        <van-action-sheet
            v-model:show="showFabActions"
            :actions="fabActions"
            cancel-text="取消"
            close-on-click-action
            @select="onFabSelect"
        />

        <!-- 收藏网址弹窗 -->
        <van-popup v-model:show="showAddPopup" position="bottom" round teleport="body">
            <div class="m-article-add">
                <div class="m-article-add__title">收藏网址</div>
                <van-field
                    v-model="addUrl"
                    placeholder="粘贴网址链接，如 https://example.com/article"
                    clearable
                    class="m-article-add__field"
                />
                <div class="m-article-add__actions">
                    <van-button block round plain @click="showAddPopup = false">取消</van-button>
                    <van-button block round type="primary" :loading="adding" @click="handleAdd">收藏</van-button>
                </div>
            </div>
        </van-popup>
    </div>
</template>

<script>
import { listArticles, addArticle, markStar, deleteArticles, listTags } from '@/api/px/app/webArticle'
import { mConfirm, mSuccess, showToast } from '@/mobile/utils/ui'

export default {
    name: 'MWebArticle',
    data() {
        return {
            searchKeyword: '',
            activeStatus: 'all',
            activeCategory: '',
            activeTag: '',
            categories: ['技术', '资讯', '工具', '生活', '学习', '其他'],
            tagOptions: [],
            statusTabs: [
                { label: '全部', value: 'all' },
                { label: '未读', value: 'unread' },
                { label: '星标', value: 'star' }
            ],
            articleList: [],
            loading: false,
            fetching: false,
            loadFinished: false,
            isRefreshing: false,
            pageNum: 1,
            pageSize: 20,
            total: 0,
            showFabActions: false,
            fabActions: [
                { name: '收藏网址', subname: '粘贴链接，自动抓取正文并 AI 整理', key: 'add' },
                { name: 'RSS 订阅', key: 'rss' }
            ],
            showAddPopup: false,
            addUrl: '',
            adding: false,
            pollTimer: null
        }
    },
    mounted() {
        this.loadTags()
        // 列表由 van-list 自动触发首次 load
    },
    beforeUnmount() {
        this.stopPoll()
    },
    deactivated() {
        this.stopPoll()
    },
    methods: {
        async loadList(refresh = false) {
            if (this.fetching) {
                this.isRefreshing = false
                this.loading = false
                return
            }
            this.fetching = true
            this.loading = true
            if (refresh) {
                this.pageNum = 1
                this.loadFinished = false
            }
            const query = {
                pageNum: this.pageNum,
                pageSize: this.pageSize,
                keyword: this.searchKeyword || undefined
            }
            if (this.activeStatus === 'unread') query.readFlag = '0'
            if (this.activeStatus === 'star') query.starFlag = '1'
            if (this.activeCategory) query.category = this.activeCategory
            if (this.activeTag) query.tag = this.activeTag
            try {
                const res = await listArticles(query)
                const rows = res.rows || []
                this.articleList = refresh ? rows : this.articleList.concat(rows)
                this.total = res.total || 0
                this.loadFinished = this.articleList.length >= this.total
                this.schedulePoll()
            } finally {
                this.fetching = false
                this.loading = false
                this.isRefreshing = false
            }
        },

        loadMore() {
            this.pageNum = Math.floor(this.articleList.length / this.pageSize) + 1
            this.loadList(this.articleList.length === 0)
        },

        onRefresh() {
            this.loadList(true)
        },

        handleSearch() {
            this.loadList(true)
        },

        onSearchInput(value) {
            if (!value) this.handleSearch()
        },

        switchStatus(value) {
            if (this.activeStatus === value) return
            this.activeStatus = value
            this.loadList(true)
        },

        switchCategory(value) {
            this.activeCategory = this.activeCategory === value ? '' : value
            this.loadList(true)
        },

        switchTag(value) {
            this.activeTag = this.activeTag === value ? '' : value
            this.loadList(true)
        },

        async loadTags() {
            try {
                const res = await listTags()
                this.tagOptions = res.data || []
            } catch (e) { /* ignore */ }
        },

        // 抓取/AI 分析进行中时轮询刷新
        schedulePoll() {
            this.stopPoll()
            const pending = this.articleList.some(
                item => item.fetchStatus === '0' || item.aiStatus === '0'
            )
            if (pending) {
                this.pollTimer = setInterval(() => this.loadList(true), 5000)
            }
        },

        stopPoll() {
            if (this.pollTimer) {
                clearInterval(this.pollTimer)
                this.pollTimer = null
            }
        },

        splitTags(tags) {
            if (!tags) return []
            return tags.split(',').map(t => t.trim()).filter(Boolean)
        },

        formatTime(time) {
            if (!time) return ''
            return String(time).slice(5, 16)
        },

        go(path) {
            // 路由分批注册，跳转前先校验
            if (this.$router.resolve(path).matched.length === 0) {
                showToast('功能即将上线')
                return
            }
            this.$router.push(path)
        },

        openDetail(item) {
            this.go(`/m/web-article/detail?id=${item.id}`)
        },

        handleEdit(item) {
            this.go(`/m/web-article/edit?id=${item.id}`)
        },

        async handleDelete(item) {
            try {
                await mConfirm('删除确认', `删除「${item.title || item.url}」？`)
            } catch (e) {
                return
            }
            try {
                await deleteArticles(item.id)
                showToast('已删除')
                this.loadList(true)
            } catch (e) { /* 统一提示 */ }
        },

        async toggleStar(item) {
            const target = item.starFlag === '1' ? '0' : '1'
            try {
                await markStar(item.id, target)
                item.starFlag = target
                showToast(target === '1' ? '已星标' : '已取消')
            } catch (e) { /* 统一提示 */ }
        },

        onFabSelect(action) {
            this.showFabActions = false
            if (action.key === 'rss') {
                this.openRssManager()
            } else if (action.key === 'add') {
                this.addUrl = ''
                this.showAddPopup = true
            }
        },

        openRssManager() {
            this.go('/m/web-article/rss')
        },

        async handleAdd() {
            const link = (this.addUrl || '').trim()
            if (!link) {
                showToast('请输入网址')
                return
            }
            this.adding = true
            try {
                await addArticle({ url: link })
                this.showAddPopup = false
                mSuccess('已收藏，后台整理中')
                this.loadList(true)
            } catch (e) { /* 统一提示 */ } finally {
                this.adding = false
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.m-article-page {
    min-height: calc(100vh - 46px);
    background: var(--pnkx-bg);
    padding-bottom: calc(80px + env(safe-area-inset-bottom));
}

.m-article-page__search {
    background: var(--pnkx-surface);
}

.m-article-tabs {
    background: var(--pnkx-surface);
    padding: 6px 12px 10px;

    &--tag {
        padding-top: 0;
        border-top: 1px solid var(--pnkx-border-soft);
    }

    &__scroll {
        display: flex;
        align-items: center;
        gap: 8px;
        overflow-x: auto;
        white-space: nowrap;

        &::-webkit-scrollbar {
            display: none;
        }
    }

    &__item {
        display: inline-flex;
        align-items: center;
        padding: 4px 14px;
        border-radius: 999px;
        background: var(--pnkx-surface-muted);
        font-size: 13px;
        color: var(--pnkx-text-secondary);
        flex-shrink: 0;

        &--tag {
            font-size: 12px;
            padding: 3px 12px;
        }

        &.active {
            background: var(--pnkx-primary);
            color: #fff;
            font-weight: 600;
        }
    }

    &__sep {
        color: var(--pnkx-border);
        flex-shrink: 0;
    }
}

.m-article-pull {
    padding: 12px;
}

.m-article-card {
    display: block;
    margin-bottom: 10px;

    :deep(.van-swipe-cell__wrapper) {
        border-radius: 12px;
        overflow: hidden;
    }

    &__body {
        background: var(--pnkx-surface);
        border: 1px solid var(--pnkx-border-soft);
        border-radius: 12px;
        padding: 12px;
        display: flex;
        flex-direction: column;
        gap: 7px;

        &:active { opacity: 0.85; }
    }

    &__head {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 8px;
    }

    &__title {
        flex: 1;
        min-width: 0;
        font-size: 15px;
        font-weight: 500;
        color: var(--pnkx-text);
        line-height: 1.45;
        display: -webkit-box;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 2;
        overflow: hidden;
        word-break: break-all;

        &.unread {
            font-weight: 700;
        }
    }

    &__star {
        flex-shrink: 0;
        color: var(--pnkx-text-muted);
        padding: 2px;

        &.stared {
            color: var(--pnkx-warning);
        }
    }

    &__summary {
        font-size: 12px;
        color: var(--pnkx-text-secondary);
        line-height: 1.6;
        display: -webkit-box;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 2;
        overflow: hidden;
    }

    &__foot {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
    }

    &__meta {
        display: flex;
        align-items: center;
        gap: 6px;
        min-width: 0;
        font-size: 11px;
        color: var(--pnkx-text-muted);
        overflow: hidden;
    }

    &__category {
        flex-shrink: 0;
        padding: 1px 8px;
        border-radius: 4px;
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
    }

    &__domain {
        max-width: 130px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__status {
        flex-shrink: 0;
        font-size: 11px;

        .pending { color: var(--pnkx-warning); }
        .error { color: var(--pnkx-danger); }
        .ok { color: var(--pnkx-success); }
    }

    &__tags {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
    }

    &__tag {
        font-size: 11px;
        color: var(--pnkx-text-muted);
        background: var(--pnkx-surface-muted);
        padding: 1px 8px;
        border-radius: 4px;
    }

    &__action {
        height: 100%;
    }
}

.m-article-add {
    padding: 16px 16px calc(16px + env(safe-area-inset-bottom));

    &__title {
        font-size: 16px;
        font-weight: 600;
        color: var(--pnkx-text);
        text-align: center;
        margin-bottom: 12px;
    }

    &__field {
        border: 1px solid var(--pnkx-border-soft);
        border-radius: 8px;
        margin-bottom: 14px;
    }

    &__actions {
        display: flex;
        gap: 10px;

        .van-button {
            flex: 1;
        }
    }
}
</style>
