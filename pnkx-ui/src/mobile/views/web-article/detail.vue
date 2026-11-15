<template>
    <div class="m-article-detail">
        <van-pull-refresh v-model="isRefreshing" class="m-pull m-article-detail__pull" @refresh="loadArticle">
            <div class="m-page" style="min-height: calc(100vh - 46px)">
                <!-- 头部信息 -->
                <div class="m-card article-head">
                    <div class="article-head__title">{{ article.title || article.url || '加载中…' }}</div>
                    <div class="article-head__meta">
                        <span v-if="article.category" class="article-head__category">{{ article.category }}</span>
                        <span class="article-head__domain">{{ article.domain }}</span>
                        <span v-if="article.publishTime">{{ formatTime(article.publishTime) }}</span>
                    </div>
                    <div v-if="tagList.length" class="article-head__tags">
                        <span v-for="tag in tagList" :key="tag" class="article-head__tag">{{ tag }}</span>
                    </div>
                </div>

                <!-- AI 摘要 -->
                <div v-if="article.summary" class="m-card article-summary">
                    <div class="article-summary__head">
                        <van-icon name="edit" size="14" />
                        <span class="article-summary__title">AI 摘要</span>
                    </div>
                    <div class="article-summary__text">{{ article.summary }}</div>
                </div>

                <!-- 抓取失败提示 -->
                <div v-if="article.fetchStatus === '2'" class="m-card article-error">
                    <div class="article-error__title">正文抓取失败</div>
                    <div class="article-error__text">{{ article.fetchError || '该网页可能需要登录或做了反爬限制' }}</div>
                </div>

                <!-- 正文快照（超大 HTML 降级为纯文本，避免渲染卡顿） -->
                <div v-if="usePlainText && !renderHtml" class="m-card article-content">
                    <div class="article-content__plain-tip">
                        <span>正文较长，已切换为纯文本模式</span>
                        <span class="article-content__plain-action" @click="renderHtml = true">渲染网页版</span>
                    </div>
                    <div class="article-content__plain-text">{{ article.contentText }}</div>
                </div>
                <div
                    v-else-if="article.contentHtml"
                    class="m-card article-content"
                >
                    <div
                        class="m-rich-content article-content__html"
                        @click="onContentClick"
                        v-html="sanitizedHtml"
                    ></div>
                </div>
                <div v-else-if="article.fetchStatus !== '2' && loaded" class="m-card article-error">
                    <div class="article-error__text">正文抓取中，或该页面无可提取正文，可稍后下拉刷新或直接打开原文</div>
                </div>

                <div class="article-bottom-space"></div>
            </div>
        </van-pull-refresh>

        <!-- 底部操作栏 -->
        <div class="article-action-bar">
            <div class="article-action-bar__item" @click="toggleStar">
                <van-icon
                    :name="article.starFlag === '1' ? 'star' : 'star-o'"
                    size="20"
                    :class="{ 'article-action-bar__star--on': article.starFlag === '1' }"
                />
                <span class="article-action-bar__label">星标</span>
            </div>
            <div class="article-action-bar__item" @click="copyLink">
                <van-icon name="link-o" size="20" />
                <span class="article-action-bar__label">复制链接</span>
            </div>
            <div class="article-action-bar__item" @click="openEdit">
                <van-icon name="edit" size="20" />
                <span class="article-action-bar__label">编辑</span>
            </div>
            <div class="article-action-bar__item" @click="toggleRead">
                <van-icon
                    name="checked"
                    size="20"
                    :class="{ 'article-action-bar__read--on': article.readFlag === '1' }"
                />
                <span class="article-action-bar__label">{{ article.readFlag === '1' ? '已读' : '未读' }}</span>
            </div>
            <div class="article-action-bar__primary" @click="openOrigin">
                <van-icon name="share" size="14" />
                <span>打开原文</span>
            </div>
        </div>
    </div>
</template>

<script>
import DOMPurify from 'dompurify'
import { getArticle, markStar, markRead } from '@/api/px/app/webArticle'
import { showToast } from '@/mobile/utils/ui'

export default {
    name: 'MWebArticleDetail',
    data() {
        return {
            articleId: null,
            article: {},
            loaded: false,
            renderHtml: false,
            isRefreshing: false
        }
    },
    computed: {
        tagList() {
            if (!this.article.tags) return []
            return this.article.tags.split(',').map(t => t.trim()).filter(Boolean)
        },
        // 超大 HTML（约 30 万字符）降级为纯文本阅读，防止 v-html 渲染卡顿
        usePlainText() {
            const html = this.article.contentHtml || ''
            return html.length > 300000 && (this.article.contentText || '').length > 0
        },
        sanitizedHtml() {
            return DOMPurify.sanitize(this.article.contentHtml || '', {
                USE_PROFILES: { html: true },
                FORBID_TAGS: ['script', 'style', 'iframe', 'object', 'embed'],
                FORBID_ATTR: ['onerror', 'onload', 'onclick', 'onmouseover']
            })
        }
    },
    mounted() {
        this.articleId = this.$route.query.id
        if (!this.articleId || this.articleId === 'undefined') {
            showToast('参数错误')
            setTimeout(() => this.$router.back(), 800)
            return
        }
        this.loadArticle()
    },
    methods: {
        async loadArticle() {
            if (!this.articleId) return
            try {
                const res = await getArticle(this.articleId)
                this.article = res.data || {}
                this.loaded = true
                // 打开详情视为已读
                if (this.article.readFlag === '0') {
                    this.article.readFlag = '1'
                    markRead(this.articleId, '1').catch(() => {})
                }
            } catch (e) { /* 统一提示 */ } finally {
                this.isRefreshing = false
            }
        },

        formatTime(time) {
            if (!time) return ''
            return String(time).slice(0, 10)
        },

        async toggleStar() {
            const target = this.article.starFlag === '1' ? '0' : '1'
            try {
                await markStar(this.articleId, target)
                this.article.starFlag = target
                showToast(target === '1' ? '已星标' : '已取消')
            } catch (e) { /* 统一提示 */ }
        },

        async toggleRead() {
            const target = this.article.readFlag === '1' ? '0' : '1'
            try {
                await markRead(this.articleId, target)
                this.article.readFlag = target
            } catch (e) { /* 统一提示 */ }
        },

        copyLink() {
            if (!this.article.url) return
            const done = () => showToast('链接已复制')
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(this.article.url).then(done).catch(() => this.copyFallback(done))
            } else {
                this.copyFallback(done)
            }
        },

        copyFallback(done) {
            try {
                const input = document.createElement('textarea')
                input.value = this.article.url
                document.body.appendChild(input)
                input.select()
                document.execCommand('copy')
                input.remove()
                done()
            } catch (e) {
                showToast('复制失败')
            }
        },

        openEdit() {
            const path = `/m/web-article/edit?id=${this.articleId}`
            if (this.$router.resolve(path).matched.length === 0) {
                showToast('功能即将上线')
                return
            }
            this.$router.push(path)
        },

        openOrigin() {
            if (!this.article.url) return
            window.open(this.article.url, '_blank', 'noopener')
        },

        // 正文内链接：拦截后统一新窗口打开
        onContentClick(e) {
            const anchor = e.target.closest && e.target.closest('a')
            if (anchor && anchor.href) {
                e.preventDefault()
                window.open(anchor.href, '_blank', 'noopener')
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.m-article-detail {
    min-height: calc(100vh - 46px);
    background: var(--pnkx-bg);
}

.article-head {
    display: flex;
    flex-direction: column;
    gap: 7px;

    &__title {
        font-size: 17px;
        font-weight: 700;
        color: var(--pnkx-text);
        line-height: 1.4;
        word-break: break-all;
    }

    &__meta {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 6px;
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }

    &__category {
        padding: 1px 8px;
        border-radius: 4px;
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
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
}

.article-summary {
    background: var(--pnkx-primary-soft);
    border-color: var(--pnkx-primary-soft);

    &__head {
        display: flex;
        align-items: center;
        gap: 5px;
        color: var(--pnkx-primary);
        margin-bottom: 6px;
    }

    &__title {
        font-size: 13px;
        font-weight: 600;
        color: var(--pnkx-primary);
    }

    &__text {
        font-size: 13px;
        color: var(--pnkx-text-secondary);
        line-height: 1.7;
        word-break: break-all;
    }
}

.article-error {
    background: var(--pnkx-danger-soft);
    border-color: var(--pnkx-danger-soft);

    &__title {
        font-size: 13px;
        font-weight: 600;
        color: var(--pnkx-danger);
        margin-bottom: 4px;
    }

    &__text {
        font-size: 13px;
        color: var(--pnkx-text-secondary);
        line-height: 1.6;
    }
}

.article-content {
    &__html {
        color: var(--pnkx-text);
    }

    &__plain-tip {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 8px;
        padding-bottom: 8px;
        border-bottom: 1px solid var(--pnkx-border-soft);
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }

    &__plain-action {
        color: var(--pnkx-primary);
    }

    &__plain-text {
        font-size: 14px;
        line-height: 1.7;
        color: var(--pnkx-text);
        white-space: pre-wrap;
        word-break: break-all;
    }
}

.article-bottom-space {
    height: calc(76px + env(safe-area-inset-bottom));
}

.article-action-bar {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 99;
    display: flex;
    align-items: center;
    padding: 8px 14px calc(8px + env(safe-area-inset-bottom));
    gap: 18px;
    background: var(--pnkx-surface);
    border-top: 1px solid var(--pnkx-border-soft);

    &__item {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 2px;
        color: var(--pnkx-text-muted);

        &:active { opacity: 0.7; }
    }

    &__label {
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }

    &__star--on {
        color: var(--pnkx-warning);
    }

    &__read--on {
        color: var(--pnkx-primary);
    }

    &__primary {
        margin-left: auto;
        display: flex;
        align-items: center;
        gap: 4px;
        background: var(--pnkx-primary);
        color: #fff;
        border-radius: 999px;
        padding: 9px 18px;
        font-size: 13px;
        font-weight: 600;
        box-shadow: 0 6px 16px rgba(91, 141, 239, 0.35);

        &:active { transform: scale(0.95); }
    }
}
</style>
