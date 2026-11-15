<template>
    <van-swipe-cell class="note-swipe">
        <div class="note-card" @click="$emit('click', note)">
            <div class="note-card__main">
                <div class="note-card__mark">{{ titleInitial }}</div>
                <div class="note-card__body">
                    <div class="note-card__title">{{ note.title || '无标题' }}</div>
                    <div class="note-card__content">{{ contentPreview }}</div>
                </div>
            </div>
            <div class="note-card__footer">
                <span class="note-card__time">
                    <van-icon name="clock-o" size="12" />
                    {{ updateTime }}
                </span>
                <span v-if="note.folderName" class="note-card__folder">
                    <van-icon name="folder-o" size="11" />
                    {{ note.folderName }}
                </span>
            </div>
        </div>
        <template #right>
            <van-button square type="primary" text="编辑" class="note-swipe__btn" @click="$emit('edit', note)" />
            <van-button square type="danger" text="删除" class="note-swipe__btn" @click="$emit('delete', note)" />
        </template>
    </van-swipe-cell>
</template>

<script>
export default {
    name: 'MNoteCard',
    props: {
        note: {
            type: Object,
            required: true
        }
    },
    computed: {
        contentPreview() {
            const content = this.note.content || this.note.richText || ''
            const text = String(content).replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ')
            if (!text.trim()) return '暂无内容'
            return text.length > 60 ? text.substring(0, 60) + '...' : text
        },
        updateTime() {
            return this.$parseTime(this.note.updateTime || this.note.createTime, '{m}-{d} {h}:{i}')
        },
        titleInitial() {
            const title = this.note.title || '记'
            return title.substring(0, 1)
        }
    }
}
</script>

<style lang="scss" scoped>
.note-swipe {
    display: block;
    margin-bottom: 12px;

    &__btn {
        height: 100%;
    }
}

.note-card {
    padding: 13px;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 12px;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);

    &:active {
        opacity: 0.85;
    }

    &__main {
        display: flex;
        align-items: flex-start;
    }

    &__mark {
        width: 36px;
        height: 36px;
        flex-shrink: 0;
        margin-right: 10px;
        border-radius: 10px;
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
        font-size: 15px;
        font-weight: 600;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    &__body {
        flex: 1;
        min-width: 0;
    }

    &__title {
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
        line-height: 1.35;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__content {
        margin-top: 5px;
        font-size: 12px;
        color: var(--pnkx-text-secondary);
        line-height: 1.6;
        display: -webkit-box;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 2;
        overflow: hidden;
    }

    &__footer {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-top: 10px;
        padding-top: 9px;
        border-top: 1px solid var(--pnkx-border-soft);
    }

    &__time {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }

    &__folder {
        display: flex;
        align-items: center;
        gap: 3px;
        max-width: 55%;
        padding: 3px 9px;
        border-radius: 999px;
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
        font-size: 11px;
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
    }
}
</style>
