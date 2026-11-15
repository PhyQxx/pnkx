<template>
    <div class="record-card" @click="$emit('click', record)">
        <div class="record-card__stripe" :class="typeClass"></div>

        <div class="record-card__main">
            <div class="record-card__header">
                <div class="record-card__date-block">
                    <span class="record-card__day">{{ dayText }}</span>
                    <div class="record-card__date-meta">
                        <span class="record-card__month">{{ monthText }}</span>
                        <span class="record-card__weekday">{{ weekdayText }}</span>
                    </div>
                </div>

                <div class="record-card__tags">
                    <span v-if="record.type" class="record-card__tag" :class="typeClass">{{ typeText }}</span>
                    <span v-if="record.makeLove" class="record-card__tag soft">同房</span>
                    <span v-if="record.items" class="record-card__tag check">检查</span>
                </div>
            </div>

            <div v-if="record.temperature || record.weight" class="record-card__metrics">
                <div v-if="record.temperature" class="record-card__metric">
                    <span class="record-card__metric-label">体温</span>
                    <span class="record-card__metric-value">{{ record.temperature }}℃</span>
                </div>
                <div v-if="record.weight" class="record-card__metric">
                    <span class="record-card__metric-label">体重</span>
                    <span class="record-card__metric-value">{{ record.weight }}kg</span>
                </div>
            </div>

            <div v-if="record.items || record.results || record.remark" class="record-card__notes">
                <div v-if="record.items" class="record-card__note">
                    <span class="record-card__note-label">检查项目</span>
                    <span class="record-card__note-content">{{ record.items }}</span>
                </div>
                <div v-if="record.results" class="record-card__note">
                    <span class="record-card__note-label">检查结果</span>
                    <span class="record-card__note-content">{{ record.results }}</span>
                </div>
                <div v-if="record.remark" class="record-card__note">
                    <span class="record-card__note-label">备注</span>
                    <span class="record-card__note-content">{{ record.remark }}</span>
                </div>
            </div>

            <div class="record-card__footer">
                <button class="record-card__action" @click.stop="$emit('edit', record)">
                    <van-icon name="edit" size="14" />
                    <span>编辑</span>
                </button>
                <button class="record-card__action record-card__action--delete" @click.stop="$emit('delete', record)">
                    <van-icon name="delete-o" size="14" />
                    <span>删除</span>
                </button>
            </div>
        </div>
    </div>
</template>

<script>
export default {
    name: 'MMenstruationRecordCard',
    props: {
        record: {
            type: Object,
            required: true
        }
    },
    computed: {
        recordDate() {
            if (!this.record.date) return ''
            return String(this.record.date).substring(0, 10)
        },
        dayText() {
            if (!this.recordDate) return '--'
            return this.recordDate.substring(8, 10)
        },
        monthText() {
            if (!this.recordDate) return ''
            const date = new Date(this.recordDate.replace(/-/g, '/'))
            return `${date.getMonth() + 1}月`
        },
        weekdayText() {
            if (!this.recordDate) return ''
            const weekMap = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
            return weekMap[new Date(this.recordDate.replace(/-/g, '/')).getDay()]
        },
        typeText() {
            if (this.record.type === '0') return '经期开始'
            if (this.record.type === '1') return '经期结束'
            return '普通记录'
        },
        typeClass() {
            if (this.record.type === '0') return 'start'
            if (this.record.type === '1') return 'end'
            return 'normal'
        }
    }
}
</script>

<style lang="scss" scoped>
.record-card {
    position: relative;
    display: flex;
    overflow: hidden;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 12px;
    margin-bottom: 12px;

    &__stripe {
        width: 4px;
        flex-shrink: 0;
        background: var(--pnkx-border);

        &.start {
            background: var(--pnkx-danger);
        }

        &.end {
            background: var(--pnkx-info);
        }
    }

    &__main {
        flex: 1;
        min-width: 0;
        padding: 12px;
    }

    &__header {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 10px;
    }

    &__date-block {
        display: flex;
        align-items: center;
        gap: 7px;
        min-width: 0;
    }

    &__day {
        font-size: 22px;
        line-height: 1;
        font-weight: 700;
        color: var(--pnkx-text);
    }

    &__date-meta {
        display: flex;
        flex-direction: column;
        gap: 2px;
    }

    &__month {
        font-size: 12px;
        color: var(--pnkx-text-secondary);
        font-weight: 600;
    }

    &__weekday {
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }

    &__tags {
        display: flex;
        justify-content: flex-end;
        flex-wrap: wrap;
        gap: 5px;
        max-width: 180px;
    }

    &__tag {
        padding: 3px 7px;
        border-radius: 999px;
        font-size: 11px;
        line-height: 1.2;
        color: var(--pnkx-text-secondary);
        background: var(--pnkx-surface-muted);

        &.start {
            color: var(--pnkx-danger);
            background: var(--pnkx-danger-soft);
        }

        &.end {
            color: var(--pnkx-info);
            background: var(--pnkx-info-soft);
        }

        &.soft {
            color: var(--pnkx-danger);
            background: var(--pnkx-danger-soft);
        }

        &.check {
            color: var(--pnkx-info);
            background: var(--pnkx-info-soft);
        }
    }

    &__metrics {
        display: flex;
        gap: 8px;
        margin-top: 11px;
    }

    &__metric {
        flex: 1;
        min-width: 0;
        padding: 9px 10px;
        background: var(--pnkx-surface-muted);
        border-radius: 8px;
    }

    &__metric-label {
        display: block;
        font-size: 11px;
        color: var(--pnkx-text-muted);
        margin-bottom: 3px;
    }

    &__metric-value {
        display: block;
        font-size: 15px;
        font-weight: 700;
        color: var(--pnkx-text);
    }

    &__notes {
        margin-top: 10px;
    }

    &__note {
        display: flex;
        align-items: flex-start;
        gap: 7px;
        margin-bottom: 5px;

        &:last-child {
            margin-bottom: 0;
        }
    }

    &__note-label {
        flex-shrink: 0;
        width: 56px;
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }

    &__note-content {
        flex: 1;
        min-width: 0;
        font-size: 13px;
        line-height: 1.55;
        color: var(--pnkx-text);
        word-break: break-word;
    }

    &__footer {
        display: flex;
        justify-content: flex-end;
        gap: 8px;
        margin-top: 11px;
    }

    &__action {
        display: flex;
        align-items: center;
        gap: 4px;
        min-height: 28px;
        padding: 0 10px;
        border: none;
        border-radius: 999px;
        background: var(--pnkx-danger-soft);
        color: var(--pnkx-danger);
        font-size: 12px;

        &--delete {
            background: var(--pnkx-danger-soft);
            color: var(--pnkx-danger);
        }

        &:active {
            opacity: 0.78;
        }
    }
}
</style>
