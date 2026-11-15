<template>
    <van-swipe-cell class="m-book-card">
        <div class="m-book-card__body" @click="$emit('click', item)">
            <!-- 封面占位（按 id 取模分配 5 色方案） -->
            <div class="m-book-card__cover" :class="'scheme-' + coverScheme">
                <van-icon name="bookmark" size="24" class="m-book-card__cover-icon" />
                <span class="m-book-card__spine"></span>
            </div>

            <!-- 信息 -->
            <div class="m-book-card__info">
                <div class="m-book-card__top">
                    <span class="m-book-card__title">{{ item.title }}</span>
                    <span class="m-book-card__status" :class="'status-' + (item.status || 'reading')">{{ statusLabel }}</span>
                </div>
                <div class="m-book-card__author">{{ item.author || '未知作者' }}</div>
                <div class="m-book-card__meta">
                    <span class="m-book-card__meta-item">
                        <van-icon name="orders-o" size="12" />
                        {{ item.chapterCount || 0 }}章
                    </span>
                    <span v-if="lastReadText" class="m-book-card__meta-item m-book-card__meta-read">
                        <van-icon name="eye-o" size="12" />
                        {{ lastReadText }}
                    </span>
                </div>
            </div>

            <van-icon name="arrow" size="15" class="m-book-card__arrow" />
        </div>

        <template #right>
            <van-button square type="primary" text="编辑" class="m-book-card__action" @click="$emit('edit', item)" />
            <van-button square type="danger" text="删除" class="m-book-card__action" @click="$emit('delete', item)" />
        </template>
    </van-swipe-cell>
</template>

<script>
export default {
    name: 'MBookCard',
    props: {
        item: {
            type: Object,
            required: true
        }
    },
    computed: {
        coverScheme() {
            // 根据书籍 id 取模分配封面色调，让书架更有层次
            return String((this.item.id || 0) % 5)
        },
        statusLabel() {
            const map = { reading: '在读', finished: '已读完', shelved: '书架' }
            return map[this.item.status] || '在读'
        },
        lastReadText() {
            if (!this.item.lastReadChapterId) return ''
            return this.item.lastReadChapterName || '已阅读'
        }
    }
}
</script>

<style lang="scss" scoped>
.m-book-card {
    display: block;
    margin-bottom: 10px;

    :deep(.van-swipe-cell__wrapper) {
        border-radius: 12px;
        overflow: hidden;
    }
}

.m-book-card__body {
    display: flex;
    align-items: center;
    padding: 12px;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 12px;

    &:active {
        opacity: 0.85;
    }
}

.m-book-card__cover {
    position: relative;
    width: 54px;
    height: 74px;
    border-radius: 6px;
    flex-shrink: 0;
    margin-right: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    box-shadow: 0 2px 6px rgba(15, 23, 42, 0.16);

    &.scheme-0 { background: var(--pnkx-primary); }
    &.scheme-1 { background: var(--pnkx-danger); }
    &.scheme-2 { background: var(--pnkx-success); }
    &.scheme-3 { background: var(--pnkx-warning); }
    &.scheme-4 { background: var(--pnkx-info); }
}

.m-book-card__cover-icon {
    color: rgba(255, 255, 255, 0.9);
}

.m-book-card__spine {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 4px;
    background: rgba(0, 0, 0, 0.15);
}

.m-book-card__info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
}

.m-book-card__top {
    display: flex;
    align-items: center;
    margin-bottom: 3px;
}

.m-book-card__title {
    flex: 1;
    min-width: 0;
    font-size: 15px;
    font-weight: 600;
    color: var(--pnkx-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin-right: 8px;
}

.m-book-card__status {
    flex-shrink: 0;
    font-size: 11px;
    padding: 1px 8px;
    border-radius: 999px;

    &.status-reading {
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
    }

    &.status-finished {
        background: var(--pnkx-success-soft);
        color: var(--pnkx-success);
    }

    &.status-shelved {
        background: var(--pnkx-surface-muted);
        color: var(--pnkx-text-secondary);
    }
}

.m-book-card__author {
    font-size: 12px;
    color: var(--pnkx-text-muted);
    margin-bottom: 5px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.m-book-card__meta {
    display: flex;
    align-items: center;
    gap: 12px;
}

.m-book-card__meta-item {
    display: flex;
    align-items: center;
    gap: 3px;
    min-width: 0;
    font-size: 12px;
    color: var(--pnkx-text-muted);
}

.m-book-card__meta-read {
    flex: 1;
    overflow: hidden;

    span {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }
}

.m-book-card__arrow {
    flex-shrink: 0;
    margin-left: 6px;
    color: var(--pnkx-text-muted);
}

.m-book-card__action {
    height: 100%;
}
</style>
