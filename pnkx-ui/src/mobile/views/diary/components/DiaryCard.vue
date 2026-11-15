<template>
    <van-swipe-cell class="diary-swipe">
        <div class="diary-card" @click="$emit('click', diary)">
            <div class="diary-card__rail">
                <span class="diary-card__day">{{ dayText }}</span>
                <span class="diary-card__month">{{ monthText }}</span>
            </div>

            <div class="diary-card__main">
                <div class="diary-card__top">
                    <div class="diary-card__titles">
                        <div class="diary-card__title">{{ diary.title || '无标题日记' }}</div>
                        <div class="diary-card__date">{{ fullDateText }}</div>
                    </div>
                    <div v-if="diary.mood || diary.weather" class="diary-card__icons">
                        <span v-if="moodIcon" class="icon-chip">
                            <img :src="moodIcon" alt="">
                        </span>
                        <span v-if="weatherIcon" class="icon-chip">
                            <img :src="weatherIcon" alt="">
                        </span>
                    </div>
                </div>

                <div class="diary-card__content">{{ contentPreview }}</div>

                <div class="diary-card__footer">
                    <span class="meta-pill">
                        <van-icon name="edit" size="11" />
                        {{ wordCount }}字
                    </span>
                    <span class="read-more">
                        查看
                        <van-icon name="arrow" size="11" />
                    </span>
                </div>
            </div>
        </div>
        <template #right>
            <van-button square type="primary" text="编辑" class="diary-swipe__btn" @click="$emit('edit', diary)" />
            <van-button square type="danger" text="删除" class="diary-swipe__btn" @click="$emit('delete', diary)" />
        </template>
    </van-swipe-cell>
</template>

<script>
import { iconUrl } from '@/mobile/utils/icons'

const WEEK_LIST = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

export default {
    name: 'MDiaryCard',
    // 声明 emits，避免父级 @click 等监听被当作原生透传绑定到根元素（会和 $emit 双触发且拿到原生 Event）
    emits: ['click', 'edit', 'delete'],
    props: {
        diary: {
            type: Object,
            required: true
        }
    },
    computed: {
        moodIcon() {
            return this.diary.mood ? iconUrl(this.diary.mood) : ''
        },
        weatherIcon() {
            return this.diary.weather ? iconUrl(this.diary.weather) : ''
        },
        cleanContent() {
            const content = this.diary.content || this.diary.richText || ''
            return String(content)
                .replace(/<[^>]+>/g, '')
                .replace(/&nbsp;/g, ' ')
                .replace(/\s+/g, ' ')
                .trim()
        },
        contentPreview() {
            if (!this.cleanContent) return '今天还没有写下正文，点开补上这一页。'
            return this.cleanContent.length > 96 ? `${this.cleanContent.substring(0, 96)}...` : this.cleanContent
        },
        wordCount() {
            return this.cleanContent.length
        },
        dateValue() {
            if (!this.diary.date) return null
            const date = new Date(String(this.diary.date).replace(/-/g, '/'))
            return Number.isNaN(date.getTime()) ? null : date
        },
        dayText() {
            if (!this.dateValue) return '--'
            return String(this.dateValue.getDate()).padStart(2, '0')
        },
        monthText() {
            if (!this.dateValue) return '未知'
            return `${this.dateValue.getMonth() + 1}月`
        },
        fullDateText() {
            if (!this.dateValue) return '未选择日期'
            return `${this.dateValue.getFullYear()}年${this.dateValue.getMonth() + 1}月${this.dateValue.getDate()}日 ${WEEK_LIST[this.dateValue.getDay()]}`
        }
    }
}
</script>

<style lang="scss" scoped>
.diary-swipe {
    margin-bottom: 12px;

    &__btn {
        height: 100%;
    }
}

.diary-card {
    display: flex;
    gap: 12px;
    padding: 13px;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 12px;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);

    &:active {
        opacity: 0.85;
    }

    &__rail {
        width: 46px;
        min-width: 46px;
        height: 56px;
        border-radius: 10px;
        background: var(--pnkx-primary-soft);
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
    }

    &__day {
        font-size: 19px;
        line-height: 1;
        font-weight: 700;
        color: var(--pnkx-primary);
    }

    &__month {
        margin-top: 4px;
        font-size: 10px;
        color: var(--pnkx-text-secondary);
    }

    &__main {
        flex: 1;
        min-width: 0;
    }

    &__top {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 8px;
        margin-bottom: 4px;
    }

    &__titles {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
        gap: 4px;
    }

    &__title {
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
        line-height: 1.25;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__date {
        font-size: 11px;
        color: var(--pnkx-text-muted);
        line-height: 1.2;
    }

    &__icons {
        display: flex;
        gap: 4px;
        flex-shrink: 0;

        .icon-chip {
            width: 24px;
            height: 24px;
            border-radius: 50%;
            background: var(--pnkx-surface-muted);
            display: flex;
            align-items: center;
            justify-content: center;
            overflow: hidden;

            img {
                width: 18px;
                height: 18px;
                object-fit: contain;
            }
        }
    }

    &__content {
        display: -webkit-box;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 2;
        overflow: hidden;
        font-size: 12px;
        color: var(--pnkx-text-secondary);
        line-height: 1.65;
    }

    &__footer {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-top: 8px;
    }

    .meta-pill,
    .read-more {
        display: flex;
        align-items: center;
        gap: 3px;
        font-size: 10px;
        color: var(--pnkx-text-muted);
        line-height: 1;
    }

    .meta-pill {
        height: 21px;
        padding: 0 7px;
        border-radius: 999px;
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
    }
}
</style>
