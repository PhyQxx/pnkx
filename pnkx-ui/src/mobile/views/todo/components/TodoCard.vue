<template>
    <van-swipe-cell class="todo-swipe">
        <div class="todo-card" :class="{ 'is-completed': isCompleted, 'is-overdue': isOverdue }" @click="$emit('click', todo)">
            <div class="todo-card__header">
                <div class="todo-card__status" @click.stop="$emit('toggle-status', todo)">
                    <van-icon :name="isCompleted ? 'checked' : 'circle'" :color="isCompleted ? 'var(--pnkx-success)' : 'var(--pnkx-border)'" size="22" />
                </div>
                <div class="todo-card__info">
                    <span class="todo-card__status-text">{{ statusText }}</span>
                    <span class="todo-card__date">{{ dateRange }}</span>
                </div>
                <span v-if="isOverdue && !isCompleted" class="todo-card__overdue">已逾期</span>
            </div>

            <div class="todo-card__content" :class="{ 'is-completed': isCompleted }">{{ contentPreview }}</div>

            <div v-if="labels.length > 0 || todo.performerName" class="todo-card__footer">
                <div class="todo-card__labels">
                    <span v-for="(label, index) in labels" :key="index" class="todo-card__label">{{ label }}</span>
                </div>
                <div v-if="todo.performerName" class="todo-card__performer">
                    <van-icon name="manager-o" size="12" />
                    <span class="todo-card__performer-text">{{ todo.performerName }}</span>
                </div>
            </div>
        </div>
        <template #right>
            <van-button square type="primary" text="编辑" class="todo-swipe__btn" @click="$emit('edit', todo)" />
            <van-button square type="danger" text="删除" class="todo-swipe__btn" @click="$emit('delete', todo)" />
        </template>
    </van-swipe-cell>
</template>

<script>
export default {
    name: 'MTodoCard',
    // 声明 emits，避免父级 @click 等监听被当作原生透传绑定到根元素（会和 $emit 双触发且拿到原生 Event）
    emits: ['click', 'edit', 'delete', 'toggle-status'],
    props: {
        todo: {
            type: Object,
            required: true
        }
    },
    computed: {
        isCompleted() {
            const status = this.todo ? this.todo.status : false
            return status === true || status === 1 || status === '1' || status === 'true' || status === '已完成'
        },
        statusText() {
            return this.isCompleted ? '已完成' : '进行中'
        },
        isOverdue() {
            if (this.isCompleted || !this.todo.planEndTime) return false
            const endTime = new Date(String(this.todo.planEndTime).replace(/-/g, '/')).getTime()
            if (Number.isNaN(endTime)) return false
            return endTime < Date.now()
        },
        dateRange() {
            const start = this.formatDateShort(this.todo.planStartTime)
            const end = this.formatDateShort(this.todo.planEndTime)
            if (!start && !end) return '未设置时间'
            if (!start) return `截止 ${end}`
            if (!end) return `${start} 开始`
            if (start === end) return start
            return `${start} ~ ${end}`
        },
        labels() {
            return this.todo.label ? this.todo.label.split(',').filter(l => l.trim()) : []
        },
        contentPreview() {
            const content = this.todo.content || ''
            return content.length > 80 ? content.substring(0, 80) + '...' : content
        }
    },
    methods: {
        formatDateShort(dateStr) {
            if (!dateStr) return ''
            const date = new Date(String(dateStr).replace(/-/g, '/'))
            if (Number.isNaN(date.getTime())) return ''
            const month = String(date.getMonth() + 1).padStart(2, '0')
            const day = String(date.getDate()).padStart(2, '0')
            return `${month}-${day}`
        }
    }
}
</script>

<style lang="scss" scoped>
.todo-swipe {
    margin-bottom: 12px;

    &__btn {
        height: 100%;
    }
}

.todo-card {
    position: relative;
    overflow: hidden;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 12px;
    padding: 14px;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);

    &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 14px;
        width: 4px;
        height: 38px;
        border-radius: 0 4px 4px 0;
        background: var(--pnkx-primary);
    }

    &.is-completed::before {
        background: var(--pnkx-border);
    }

    &.is-overdue {
        border-color: var(--pnkx-danger-soft);

        &::before {
            background: var(--pnkx-danger);
        }
    }

    &:active {
        opacity: 0.85;
    }

    &__header {
        display: flex;
        align-items: center;
        gap: 9px;
        margin-bottom: 9px;
    }

    &__status {
        width: 28px;
        height: 28px;
        flex-shrink: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 50%;
        background: var(--pnkx-primary-soft);
    }

    &__info {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
        gap: 3px;
    }

    &__status-text {
        font-size: 11px;
        color: var(--pnkx-text-muted);
        line-height: 1.2;
    }

    &__date {
        font-size: 12px;
        font-weight: 600;
        color: var(--pnkx-text-secondary);
        line-height: 1.2;
    }

    &__overdue {
        flex-shrink: 0;
        height: 20px;
        padding: 0 8px;
        border-radius: 999px;
        background: var(--pnkx-danger-soft);
        color: var(--pnkx-danger);
        font-size: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    &__content {
        padding-left: 37px;
        margin-bottom: 10px;
        font-size: 14px;
        line-height: 1.6;
        color: var(--pnkx-text);
        word-break: break-all;
        font-weight: 500;

        &.is-completed {
            color: var(--pnkx-text-muted);
            text-decoration: line-through;
            font-weight: 400;
        }
    }

    &__footer {
        display: flex;
        align-items: flex-end;
        justify-content: space-between;
        gap: 6px;
        padding-left: 37px;
    }

    &__labels {
        display: flex;
        flex-wrap: wrap;
        gap: 4px;
        flex: 1;
        min-width: 0;
    }

    &__label {
        max-width: 110px;
        height: 21px;
        padding: 0 7px;
        border-radius: 999px;
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
        font-size: 10px;
        line-height: 21px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__performer {
        display: flex;
        align-items: center;
        gap: 3px;
        flex-shrink: 0;
        max-width: 96px;
        color: var(--pnkx-text-muted);
    }

    &__performer-text {
        min-width: 0;
        font-size: 11px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }
}
</style>
