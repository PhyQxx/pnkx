<template>
    <div class="m-rss-page">
        <div class="m-page">
            <div v-if="feedList.length === 0 && !loading" class="m-empty">
                <van-icon name="volume-o" size="42" color="var(--pnkx-primary)" />
                <span>还没有订阅</span>
                <span class="m-text-muted">添加 RSS 源，新文章自动收藏并 AI 整理</span>
            </div>

            <div v-else class="m-rss-list">
                <div v-for="feed in feedList" :key="feed.id" class="m-card m-rss-card">
                    <div class="m-rss-card__head">
                        <span class="m-rss-card__name">{{ feed.name }}</span>
                        <span class="m-rss-card__status" :class="feed.enabled === '1' ? 'on' : 'off'">
                            {{ feed.enabled === '1' ? '启用' : '停用' }}
                        </span>
                    </div>
                    <div class="m-rss-card__url">{{ feed.url }}</div>
                    <div class="m-rss-card__meta">
                        <span>{{ feed.lastFetchTime ? '最近抓取：' + formatTime(feed.lastFetchTime) : '未抓取' }}</span>
                        <span v-if="feed.lastFetchCount > 0" class="m-rss-card__added">+{{ feed.lastFetchCount }} 篇</span>
                    </div>
                    <div v-if="feed.fetchError" class="m-rss-card__error">
                        {{ (feed.consecutiveFailures >= 3 ? '连续失败已自动停用：' : '抓取失败：') + feed.fetchError }}
                    </div>
                    <div class="m-rss-card__actions">
                        <span class="m-rss-chip" @click="toggleEnabled(feed)">
                            {{ feed.enabled === '1' ? '停用' : '启用' }}
                        </span>
                        <span class="m-rss-chip primary" :class="{ disabled: feed.fetching }" @click="fetchNow(feed)">
                            {{ feed.fetching ? '抓取中…' : '立即抓取' }}
                        </span>
                        <span class="m-rss-chip danger" @click="removeFeed(feed)">删除</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- FAB 新增 -->
        <button class="m-fab m-fab--no-tabbar" @click="showAddPopup = true">
            <van-icon name="plus" />
        </button>

        <!-- 新增订阅弹窗 -->
        <van-popup v-model:show="showAddPopup" position="bottom" round teleport="body">
            <div class="m-rss-add">
                <div class="m-rss-add__title">新增 RSS 订阅</div>
                <van-field
                    v-model="addUrl"
                    placeholder="RSS/Atom 链接，如 https://sspai.com/feed"
                    clearable
                    class="m-rss-add__field"
                />
                <div class="m-rss-add__actions">
                    <van-button block round plain @click="showAddPopup = false">取消</van-button>
                    <van-button block round type="primary" :loading="adding" @click="handleAdd">添加</van-button>
                </div>
            </div>
        </van-popup>
    </div>
</template>

<script>
import { listFeeds, addFeed, updateFeed, deleteFeeds, fetchFeedNow } from '@/api/px/app/webArticle'
import { mConfirm, mError, mSuccess, showToast } from '@/mobile/utils/ui'

export default {
    name: 'MWebArticleRss',
    data() {
        return {
            feedList: [],
            loading: false,
            showAddPopup: false,
            addUrl: '',
            adding: false
        }
    },
    mounted() {
        this.loadFeeds()
    },
    methods: {
        async loadFeeds() {
            this.loading = true
            try {
                const res = await listFeeds({ pageNum: 1, pageSize: 100 })
                this.feedList = (res.rows || []).map(feed => ({ ...feed, fetching: false }))
            } finally {
                this.loading = false
            }
        },

        formatTime(time) {
            if (!time) return ''
            return String(time).slice(5, 16)
        },

        async handleAdd() {
            const link = (this.addUrl || '').trim()
            if (!link) {
                showToast('请输入 RSS 链接')
                return
            }
            this.adding = true
            try {
                await addFeed({ name: link.replace(/^https?:\/\//, '').slice(0, 40), url: link })
                this.showAddPopup = false
                this.addUrl = ''
                mSuccess('已添加')
                this.loadFeeds()
            } catch (e) { /* 统一提示 */ } finally {
                this.adding = false
            }
        },

        async toggleEnabled(feed) {
            try {
                await updateFeed({ id: feed.id, enabled: feed.enabled === '1' ? '0' : '1' })
                this.loadFeeds()
            } catch (e) { /* 统一提示 */ }
        },

        async fetchNow(feed) {
            if (feed.fetching) return
            feed.fetching = true
            try {
                const res = await fetchFeedNow(feed.id)
                showToast(`新收录 ${res.added ?? 0} 篇`)
                this.loadFeeds()
            } catch (e) { /* 统一提示 */ } finally {
                feed.fetching = false
            }
        },

        async removeFeed(feed) {
            try {
                await mConfirm('删除订阅', `删除「${feed.name}」？已收藏的文章不受影响`)
            } catch (e) {
                return
            }
            try {
                await deleteFeeds(feed.id)
                showToast('已删除')
                this.loadFeeds()
            } catch (e) {
                mError('删除失败')
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.m-rss-page {
    min-height: calc(100vh - 46px);
    background: var(--pnkx-bg);
    padding-bottom: calc(80px + env(safe-area-inset-bottom));
}

.m-rss-card {
    display: flex;
    flex-direction: column;
    gap: 5px;

    &__head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
    }

    &__name {
        flex: 1;
        min-width: 0;
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__status {
        flex-shrink: 0;
        font-size: 11px;
        padding: 1px 9px;
        border-radius: 999px;

        &.on {
            color: var(--pnkx-primary);
            background: var(--pnkx-primary-soft);
        }

        &.off {
            color: var(--pnkx-text-muted);
            background: var(--pnkx-surface-muted);
        }
    }

    &__url {
        font-size: 11px;
        color: var(--pnkx-text-muted);
        word-break: break-all;
    }

    &__meta {
        display: flex;
        align-items: center;
        gap: 10px;
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }

    &__added {
        color: var(--pnkx-success);
    }

    &__error {
        font-size: 11px;
        color: var(--pnkx-danger);
        word-break: break-all;
    }

    &__actions {
        display: flex;
        gap: 8px;
        margin-top: 5px;
    }
}

.m-rss-chip {
    padding: 4px 14px;
    border-radius: 999px;
    background: var(--pnkx-surface-muted);
    font-size: 12px;
    color: var(--pnkx-text-secondary);

    &:active { opacity: 0.7; }

    &.primary {
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
    }

    &.danger {
        background: var(--pnkx-danger-soft);
        color: var(--pnkx-danger);
    }

    &.disabled {
        opacity: 0.5;
        pointer-events: none;
    }
}

.m-rss-add {
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
