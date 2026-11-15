<template>
    <div class="m-book-detail">
        <!-- 头部信息卡 -->
        <div class="book-hero" :class="'hero-' + coverScheme">
            <div class="book-hero__row">
                <div class="book-hero__cover">
                    <van-icon name="bookmark" size="40" class="book-hero__cover-icon" />
                    <span class="book-hero__spine"></span>
                </div>
                <div class="book-hero__meta">
                    <div class="book-hero__title">{{ book.title }}</div>
                    <div class="book-hero__author">{{ book.author || '未知作者' }}</div>
                    <div class="book-hero__tags">
                        <span class="book-hero__tag">{{ statusLabel }}</span>
                        <span class="book-hero__tag">{{ book.chapterCount || 0 }} 章</span>
                    </div>
                </div>
                <button class="book-hero__edit" @click="handleEdit">
                    <van-icon name="edit" size="16" />
                </button>
            </div>
            <div v-if="book.description" class="book-hero__desc">{{ book.description }}</div>
        </div>

        <!-- 继续阅读按钮 -->
        <div class="m-page" style="padding-bottom: 0">
            <div class="book-continue" @click="handleContinue">
                <van-icon name="eye-o" size="16" />
                <span class="book-continue__text">{{ continueLabel }}</span>
                <van-icon name="arrow" size="14" />
            </div>
        </div>

        <!-- 章节目录 -->
        <div class="book-catalog">
            <div class="book-catalog__header">
                <div class="m-section-title" style="margin: 0 0 10px">目录</div>
                <van-search
                    v-model="chapterKeyword"
                    placeholder="搜索章节"
                    shape="round"
                    :clearable="true"
                    class="book-catalog__search"
                    @search="searchChapters"
                    @clear="searchChapters"
                />
            </div>

            <van-list
                v-model:loading="chapterLoading"
                :finished="chapterFinished"
                finished-text="没有更多了"
                @load="loadMoreChapters"
            >
                <div v-if="chapterList.length === 0 && !chapterLoading" class="m-empty">
                    <van-icon name="bookmark-o" size="36" color="var(--pnkx-text-muted)" />
                    <span>暂无章节</span>
                </div>
                <div
                    v-for="ch in chapterList"
                    :key="ch.id"
                    class="book-chapter"
                    :class="{ 'book-chapter--current': isCurrentChapter(ch.id) }"
                    @click="handleReadChapter(ch)"
                >
                    <span class="book-chapter__no">第{{ ch.chapterNo }}章</span>
                    <span class="book-chapter__name">{{ ch.chapterName }}</span>
                    <span v-if="ch.wordCount" class="book-chapter__words">{{ formatWordCount(ch.wordCount) }}</span>
                    <van-icon v-if="isCurrentChapter(ch.id)" name="success" size="14" class="book-chapter__check" />
                </div>
            </van-list>
        </div>
    </div>
</template>

<script>
import { getBook, listChapters } from '@/api/px/app/book'
import { showToast } from '@/mobile/utils/ui'

export default {
    name: 'MBookDetail',
    data() {
        return {
            bookId: null,
            book: {},
            chapterKeyword: '',
            chapterList: [],
            chapterLoading: false,
            chapterFetching: false,
            chapterFinished: false,
            chapterPageNum: 1,
            chapterPageSize: 30,
            chapterTotal: 0
        }
    },
    computed: {
        coverScheme() {
            return String((this.book.id || 0) % 5)
        },
        statusLabel() {
            const map = { reading: '在读', finished: '已读完', shelved: '书架' }
            return map[this.book.status] || '在读'
        },
        continueLabel() {
            if (this.book.lastReadChapterId && this.book.lastReadChapterName) {
                return '继续阅读 · ' + this.book.lastReadChapterName
            }
            return '开始阅读'
        }
    },
    mounted() {
        this.bookId = this.$route.query.id
        // 缺少 id 参数（非法入口），直接返回
        if (!this.bookId || this.bookId === 'undefined') {
            showToast('参数错误')
            setTimeout(() => this.$router.back(), 800)
            return
        }
        this.loadBook()
        // 章节列表由 van-list 自动触发首次 load
    },
    methods: {
        async loadBook() {
            if (!this.bookId || this.bookId === 'undefined') return
            try {
                const res = await getBook(this.bookId)
                if (res.code === 200 && res.data) {
                    this.book = res.data
                }
            } catch (e) {
                console.error('加载书籍失败:', e)
            }
        },

        async loadChapters(refresh = false) {
            if (!this.bookId || this.bookId === 'undefined') {
                this.chapterLoading = false
                return
            }
            if (this.chapterFetching) {
                this.chapterLoading = false
                return
            }
            this.chapterFetching = true
            this.chapterLoading = true
            if (refresh) {
                this.chapterPageNum = 1
                this.chapterList = []
                this.chapterFinished = false
            }
            try {
                const params = {
                    bookId: this.bookId,
                    pageNum: this.chapterPageNum,
                    pageSize: this.chapterPageSize
                }
                if (this.chapterKeyword) params.chapterName = this.chapterKeyword
                const res = await listChapters(params)
                if (res.code === 200) {
                    const list = res.rows || []
                    this.chapterTotal = res.total || 0
                    this.chapterList = refresh ? list : [...this.chapterList, ...list]
                    this.chapterFinished = this.chapterList.length >= this.chapterTotal
                }
            } catch (e) {
                console.error('加载章节失败:', e)
            } finally {
                this.chapterFetching = false
                this.chapterLoading = false
            }
        },

        loadMoreChapters() {
            this.chapterPageNum = Math.floor(this.chapterList.length / this.chapterPageSize) + 1
            this.loadChapters(this.chapterList.length === 0)
        },

        searchChapters() {
            this.loadChapters(true)
        },

        isCurrentChapter(id) {
            return this.book.lastReadChapterId === id
        },

        formatWordCount(n) {
            if (!n) return ''
            if (n >= 10000) return (n / 10000).toFixed(1) + '万字'
            return n + '字'
        },

        goReader(chapterId) {
            const path = `/m/book/reader?chapterId=${chapterId}`
            if (this.$router.resolve(path).matched.length === 0) {
                showToast('功能即将上线')
                return
            }
            this.$router.push(path)
        },

        handleContinue() {
            const id = this.book.lastReadChapterId || this.book.firstChapterId
            if (!id) {
                showToast('暂无可读章节')
                return
            }
            this.goReader(id)
        },

        handleReadChapter(ch) {
            this.goReader(ch.id)
        },

        handleEdit() {
            const path = `/m/book/edit?id=${this.bookId}`
            if (this.$router.resolve(path).matched.length === 0) {
                showToast('功能即将上线')
                return
            }
            this.$router.push(path)
        }
    }
}
</script>

<style lang="scss" scoped>
.m-book-detail {
    min-height: calc(100vh - 46px);
    background: var(--pnkx-bg);
    padding-bottom: 24px;
}

/* 头部信息卡 */
.book-hero {
    position: relative;
    padding: 16px 14px 14px;
    overflow: hidden;

    &.hero-0 { background: linear-gradient(135deg, var(--pnkx-primary), var(--pnkx-info)); }
    &.hero-1 { background: linear-gradient(135deg, var(--pnkx-danger), var(--pnkx-warning)); }
    &.hero-2 { background: linear-gradient(135deg, var(--pnkx-success), var(--pnkx-info)); }
    &.hero-3 { background: linear-gradient(135deg, var(--pnkx-warning), var(--pnkx-danger)); }
    &.hero-4 { background: linear-gradient(135deg, var(--pnkx-info), var(--pnkx-primary)); }

    &__row {
        position: relative;
        display: flex;
        align-items: center;
    }

    &__cover {
        position: relative;
        width: 70px;
        height: 95px;
        border-radius: 6px;
        background: rgba(255, 255, 255, 0.18);
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 14px;
        flex-shrink: 0;
        overflow: hidden;
        box-shadow: 0 4px 12px rgba(15, 23, 42, 0.2);
    }

    &__cover-icon {
        color: rgba(255, 255, 255, 0.9);
    }

    &__spine {
        position: absolute;
        left: 0;
        top: 0;
        bottom: 0;
        width: 5px;
        background: rgba(0, 0, 0, 0.15);
    }

    &__meta {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
    }

    &__title {
        font-size: 19px;
        font-weight: 700;
        color: #fff;
        line-height: 1.3;
        margin-bottom: 3px;
        word-break: break-all;
    }

    &__author {
        font-size: 13px;
        color: rgba(255, 255, 255, 0.85);
        margin-bottom: 7px;
    }

    &__tags {
        display: flex;
        gap: 6px;
    }

    &__tag {
        font-size: 11px;
        padding: 1px 9px;
        border-radius: 999px;
        background: rgba(255, 255, 255, 0.26);
        color: #fff;
    }

    &__edit {
        flex-shrink: 0;
        width: 30px;
        height: 30px;
        border-radius: 50%;
        border: none;
        background: rgba(255, 255, 255, 0.24);
        color: #fff;
        display: flex;
        align-items: center;
        justify-content: center;

        &:active { opacity: 0.8; }
    }

    &__desc {
        position: relative;
        margin-top: 12px;
        padding: 10px;
        background: rgba(255, 255, 255, 0.16);
        border-radius: 8px;
        font-size: 12px;
        color: rgba(255, 255, 255, 0.95);
        line-height: 1.7;
        word-break: break-all;
    }
}

/* 继续阅读 */
.book-continue {
    display: flex;
    align-items: center;
    gap: 6px;
    height: 44px;
    border-radius: 10px;
    background: var(--pnkx-primary);
    color: #fff;
    padding: 0 14px;
    box-shadow: 0 4px 12px rgba(91, 141, 239, 0.35);

    &__text {
        flex: 1;
        text-align: center;
        font-size: 14px;
        font-weight: 600;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &:active { opacity: 0.85; }
}

/* 目录 */
.book-catalog {
    margin-top: 12px;
    background: var(--pnkx-surface);
    border-radius: 12px 12px 0 0;
    padding: 14px 12px 20px;

    &__header {
        border-bottom: 1px solid var(--pnkx-border-soft);
        padding-bottom: 10px;
    }

    &__search {
        padding: 0;
        background: transparent;
    }

    :deep(.van-search__content) {
        background: var(--pnkx-surface-muted);
    }
}

.book-chapter {
    display: flex;
    align-items: center;
    padding: 13px 0;
    border-bottom: 1px solid var(--pnkx-border-soft);

    &:last-child {
        border-bottom: none;
    }

    &:active {
        opacity: 0.75;
    }

    &--current {
        .book-chapter__name {
            color: var(--pnkx-primary);
            font-weight: 600;
        }

        .book-chapter__no {
            color: var(--pnkx-primary);
        }
    }

    &__no {
        font-size: 12px;
        color: var(--pnkx-text-muted);
        width: 64px;
        flex-shrink: 0;
    }

    &__name {
        flex: 1;
        min-width: 0;
        font-size: 14px;
        color: var(--pnkx-text);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        margin-right: 8px;
    }

    &__words {
        font-size: 11px;
        color: var(--pnkx-text-muted);
        flex-shrink: 0;
        margin-right: 4px;
    }

    &__check {
        color: var(--pnkx-primary);
        flex-shrink: 0;
    }
}
</style>
