<template>
    <div class="m-book-reader" :class="'theme-' + theme">
        <!-- 顶部：返回 + 章节名（沉浸式自绘栏） -->
        <div class="reader-topbar">
            <button class="reader-topbar__back" @click="goBack">
                <van-icon name="arrow-left" size="18" />
            </button>
            <span class="reader-topbar__title">{{ chapter.chapterName || '阅读' }}</span>
            <span class="reader-topbar__back reader-topbar__back--placeholder"></span>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="reader-loading">
            <van-loading size="28" :color="mutedColor" />
            <span class="reader-loading__text">正在加载...</span>
        </div>

        <template v-else>
            <!-- 正文 -->
            <div ref="scrollEl" class="reader-scroll">
                <div class="reader-inner" :style="{ fontSize: fontSizes[fontIndex] + 'px', lineHeight: '1.9' }">
                    <div class="reader-inner__title">{{ chapter.chapterName }}</div>
                    <div class="reader-inner__meta">
                        <span>第 {{ chapter.chapterNo }} 章</span>
                        <span v-if="chapter.wordCount"> · {{ formatWordCount(chapter.wordCount) }}</span>
                    </div>
                    <div class="reader-inner__content">{{ chapter.content || '（本章暂无内容）' }}</div>

                    <div class="reader-end">
                        <div class="reader-end__nav">
                            <span class="reader-end__btn" :class="{ disabled: !previous }" @click="goPrevious">
                                <van-icon name="arrow-left" size="14" />
                                上一章
                            </span>
                            <span class="reader-end__btn" @click="openCatalog">
                                <van-icon name="bars" size="14" />
                                目录
                            </span>
                            <span class="reader-end__btn" :class="{ disabled: !next }" @click="goNext">
                                下一章
                                <van-icon name="arrow" size="14" />
                            </span>
                        </div>
                        <span v-if="!next" class="reader-end__tip">— 全书完 —</span>
                    </div>
                </div>
            </div>

            <!-- 底部工具栏 -->
            <div class="reader-toolbar">
                <span class="reader-toolbar__btn" :class="{ disabled: !previous }" @click="goPrevious">
                    <van-icon name="arrow-left" size="20" />
                </span>
                <span class="reader-toolbar__btn" @click="openCatalog">
                    <van-icon name="bars" size="20" />
                </span>
                <span class="reader-toolbar__btn" @click="toggleSetting">
                    <van-icon name="setting-o" size="20" />
                </span>
                <span class="reader-toolbar__btn" :class="{ disabled: !next }" @click="goNext">
                    <van-icon name="arrow" size="20" />
                </span>
            </div>

            <!-- 设置面板 -->
            <div v-if="showSetting" class="reader-setting">
                <div class="reader-setting__group">
                    <span class="reader-setting__label">字号</span>
                    <div class="reader-setting__controls">
                        <span class="reader-setting__size" :class="{ active: fontIndex > 0 }" @click="changeFont(-1)">
                            <van-icon name="minus" size="13" />
                        </span>
                        <span class="reader-setting__current">A</span>
                        <span
                            class="reader-setting__size"
                            :class="{ active: fontIndex < fontSizes.length - 1 }"
                            @click="changeFont(1)"
                        >
                            <van-icon name="plus" size="13" />
                        </span>
                    </div>
                </div>
                <div class="reader-setting__group">
                    <span class="reader-setting__label">背景</span>
                    <div class="reader-setting__controls">
                        <span
                            v-for="t in themes"
                            :key="t.value"
                            class="reader-setting__theme"
                            :class="['dot-' + t.value, { selected: theme === t.value }]"
                            @click="theme = t.value"
                        ></span>
                    </div>
                </div>
            </div>
        </template>
    </div>
</template>

<script>
import { getReaderData, updateReadingProgress } from '@/api/px/app/book'
import { showToast } from '@/mobile/utils/ui'

const SETTING_KEY = 'book_reader_setting'

export default {
    name: 'MBookReader',
    data() {
        return {
            chapterId: null,
            chapter: {},
            previous: null,
            next: null,
            loading: true,
            showSetting: false,
            fontIndex: 1,
            fontSizes: [15, 17, 19, 21],
            theme: 'paper',
            themes: [
                { value: 'paper' },
                { value: 'yellow' },
                { value: 'green' },
                { value: 'dark' }
            ],
            mutedColor: '#8EA0B8'
        }
    },
    watch: {
        theme() {
            this.saveSetting()
        },
        fontIndex() {
            this.saveSetting()
        }
    },
    mounted() {
        this.chapterId = this.$route.query.chapterId
        this.restoreSetting()
        if (!this.chapterId || this.chapterId === 'undefined') {
            showToast('参数错误')
            setTimeout(() => this.goBack(), 800)
            return
        }
        this.loadChapter()
    },
    beforeUnmount() {
        this.saveSetting()
    },
    methods: {
        async loadChapter() {
            if (!this.chapterId) return
            this.loading = true
            try {
                const res = await getReaderData(this.chapterId)
                if (res.code === 200 && res.data) {
                    this.chapter = res.data.chapter || {}
                    this.previous = res.data.previous || null
                    this.next = res.data.next || null
                    // 记录阅读进度
                    this.saveProgressQuiet()
                }
            } catch (e) {
                console.error('加载章节失败:', e)
                showToast('加载失败')
            } finally {
                this.loading = false
                this.$nextTick(() => {
                    if (this.$refs.scrollEl) this.$refs.scrollEl.scrollTop = 0
                })
            }
        },

        saveProgressQuiet() {
            updateReadingProgress(this.chapterId).catch(() => {})
        },

        goPrevious() {
            if (!this.previous) return
            this.chapterId = this.previous.id
            this.loadChapter()
        },

        goNext() {
            if (!this.next) return
            this.chapterId = this.next.id
            this.loadChapter()
        },

        openCatalog() {
            // 目录在详情页，返回即达
            this.goBack()
        },

        goBack() {
            if (window.history.state && window.history.state.back) {
                this.$router.back()
            } else {
                this.$router.replace('/m/book')
            }
        },

        toggleSetting() {
            this.showSetting = !this.showSetting
        },

        changeFont(delta) {
            const next = this.fontIndex + delta
            if (next >= 0 && next < this.fontSizes.length) {
                this.fontIndex = next
            }
        },

        formatWordCount(n) {
            if (!n) return ''
            if (n >= 10000) return (n / 10000).toFixed(1) + '万字'
            return n + '字'
        },

        saveSetting() {
            try {
                localStorage.setItem(SETTING_KEY, JSON.stringify({
                    fontIndex: this.fontIndex,
                    theme: this.theme
                }))
            } catch (e) { /* ignore */ }
        },

        restoreSetting() {
            try {
                const raw = localStorage.getItem(SETTING_KEY)
                if (raw) {
                    const s = JSON.parse(raw)
                    if (s && typeof s.fontIndex === 'number') this.fontIndex = s.fontIndex
                    if (s && s.theme) this.theme = s.theme
                }
            } catch (e) { /* ignore */ }
        }
    }
}
</script>

<style lang="scss" scoped>
/*
 * 阅读器为沉浸式全屏页（路由 meta.plain，自绘顶栏）。
 * 主题背景为功能性护眼底色（纸张/米黄/护眼绿/夜间），与设计令牌体系无关。
 */
.m-book-reader {
    height: 100vh;
    height: 100dvh;
    display: flex;
    flex-direction: column;
    position: relative;
    transition: background-color 0.25s ease;

    &.theme-paper { background-color: #F7F4ED; }
    &.theme-yellow { background-color: #FBF3E0; }
    &.theme-green { background-color: #E8F0E4; }

    &.theme-dark {
        background-color: #1A1A1A;

        .reader-topbar {
            background: rgba(38, 38, 38, 0.92);
            border-bottom-color: #333;

            &__back { color: #C8C8C8; }
            &__title { color: #E8E8E8; }
        }

        .reader-inner {
            color: #C8C8C8;

            &__title { color: #E8E8E8; }
            &__meta { color: #888; }
            &__content { color: #C0C0C0; }
        }

        .reader-end__nav { border-top-color: #333; }
        .reader-end__tip { color: #666; }
        .reader-toolbar { background-color: #262626; border-top-color: #333; }
        .reader-setting { background-color: #262626; border-top-color: #333; }
        .reader-setting__label { color: #C8C8C8; }
        .reader-setting__size { background-color: #333; }
        .reader-setting__current { color: #E8E8E8; }
    }
}

.reader-topbar {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    padding: calc(6px + env(safe-area-inset-top)) 10px 6px;
    background: rgba(255, 255, 255, 0.92);
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);

    &__back {
        width: 32px;
        height: 32px;
        border: none;
        background: transparent;
        color: #4A5568;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;

        &:active { opacity: 0.6; }

        &--placeholder {
            pointer-events: none;
        }
    }

    &__title {
        flex: 1;
        min-width: 0;
        text-align: center;
        font-size: 15px;
        font-weight: 600;
        color: #1A202C;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }
}

.reader-loading {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 12px;

    &__text {
        font-size: 12px;
        color: #8EA0B8;
    }
}

.reader-scroll {
    flex: 1;
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
}

.reader-inner {
    padding: 20px 16px 150px;
    color: #2D3748;

    &__title {
        font-size: 20px;
        font-weight: 700;
        color: #1A202C;
        text-align: center;
        margin-bottom: 6px;
        line-height: 1.35;
    }

    &__meta {
        text-align: center;
        font-size: 12px;
        color: #8EA0B8;
        margin-bottom: 20px;
    }

    &__content {
        font-size: inherit;
        white-space: pre-wrap;
        word-break: break-all;
        color: #2D3748;
    }
}

.reader-end {
    margin-top: 28px;
    text-align: center;

    &__nav {
        display: flex;
        align-items: center;
        justify-content: space-around;
        padding: 14px 0;
        border-top: 1px solid rgba(0, 0, 0, 0.08);
    }

    &__btn {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: #6366F1;

        &.disabled {
            opacity: 0.4;
        }

        &:active { opacity: 0.6; }
    }

    &__tip {
        display: block;
        margin-top: 10px;
        font-size: 12px;
        color: #8EA0B8;
    }
}

.reader-toolbar {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 10;
    display: flex;
    align-items: center;
    justify-content: space-around;
    padding: 6px 0 calc(6px + env(safe-area-inset-bottom));
    background-color: #fff;
    border-top: 1px solid rgba(0, 0, 0, 0.06);

    &__btn {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 8px 0;
        color: #4A5568;

        &.disabled {
            opacity: 0.4;
        }

        &:active { opacity: 0.6; }
    }
}

.reader-setting {
    position: fixed;
    left: 0;
    right: 0;
    bottom: calc(52px + env(safe-area-inset-bottom));
    z-index: 9;
    background-color: #fff;
    border-top: 1px solid rgba(0, 0, 0, 0.06);
    padding: 14px 16px;
    box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.06);
    display: flex;
    flex-direction: column;
    gap: 14px;

    &__group {
        display: flex;
        align-items: center;
        justify-content: space-between;
    }

    &__label {
        font-size: 12px;
        color: #718096;
    }

    &__controls {
        display: flex;
        align-items: center;
        gap: 12px;
    }

    &__size {
        width: 28px;
        height: 28px;
        border-radius: 50%;
        background-color: #EDF2F7;
        color: #4A5568;
        display: flex;
        align-items: center;
        justify-content: center;

        &.active {
            background-color: rgba(99, 102, 241, 0.14);
            color: #6366F1;
        }

        &:active { transform: scale(0.92); }
    }

    &__current {
        font-size: 15px;
        font-weight: 700;
        color: #1A202C;
        width: 20px;
        text-align: center;
    }

    &__theme {
        width: 28px;
        height: 28px;
        border-radius: 50%;
        border: 2px solid transparent;
        box-sizing: border-box;

        &.dot-paper { background-color: #F7F4ED; }
        &.dot-yellow { background-color: #FBF3E0; }
        &.dot-green { background-color: #E8F0E4; }
        &.dot-dark { background-color: #1A1A1A; }

        &.selected {
            border-color: #6366F1;
            box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.15);
        }

        &:active { transform: scale(0.92); }
    }
}
</style>
