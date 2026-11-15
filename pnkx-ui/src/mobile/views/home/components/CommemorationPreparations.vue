<template>
    <div v-if="items.length" class="cp">
        <div v-for="item in items" :key="item.id" class="cp-item">
            <div class="cp-item__main">
                <div class="cp-item__title">{{ item.name }}</div>
                <div class="cp-item__meta">
                    <span class="cp-chip">{{ item.occurrenceDate }}</span>
                    <span class="cp-chip">{{ item.daysUntil === 0 ? '今天' : item.daysUntil + '天后' }}</span>
                    <span class="cp-tag" :class="{ 'cp-tag--ready': ['accepted', 'completed'].includes(item.preparationStatus) }">
                        {{ labels[item.preparationStatus] || '尚未准备' }}
                    </span>
                </div>
            </div>
            <div v-if="canEdit && !['accepted', 'completed', 'removed'].includes(item.preparationStatus)" class="cp-item__actions">
                <van-button size="small" plain type="primary" @click="open(item)">
                    {{ item.preparationStatus === 'dismissed' ? '重新准备' : '创建待办' }}
                </van-button>
                <van-button v-if="item.preparationStatus !== 'dismissed'" size="small" plain @click="dismiss(item)">忽略本次</van-button>
            </div>
        </div>

        <div v-if="selected" class="cp-editor">
            <div class="cp-editor__tip">准备 {{ selected.name }}，将创建你负责的待办</div>
            <van-field
                v-model="form.content"
                type="textarea"
                rows="2"
                autosize
                maxlength="2000"
                placeholder="具体准备事项"
                class="cp-editor__field"
            />
            <button class="cp-editor__date" @click="calendarVisible = true">完成日期 {{ form.dueDate }}</button>
            <div v-if="error" class="cp-editor__error">{{ error }}</div>
            <div class="cp-editor__actions">
                <van-button size="small" type="primary" :loading="busy" :disabled="busy" @click="confirm">预览并确认</van-button>
                <van-button size="small" @click="selected = null">取消</van-button>
            </div>
        </div>

        <van-calendar
            v-model:show="calendarVisible"
            :min-date="calendarMinDate"
            :max-date="calendarMaxDate"
            @confirm="onDateConfirm"
        />
    </div>
</template>

<script>
import {
    commitCommemorationPreparation,
    dismissCommemorationPreparation,
    previewCommemorationPreparation
} from '@/api/px/life/actionCenter'
import { mConfirm } from '@/mobile/utils/ui'

export default {
    name: 'MHomeCommemorationPreparations',
    props: {
        items: { type: Array, default: () => [] },
        canEdit: { type: Boolean, default: false }
    },
    emits: ['changed'],
    data() {
        return {
            selected: null,
            form: {},
            busy: false,
            error: '',
            preview: null,
            calendarVisible: false,
            labels: {
                draft: '待确认',
                accepted: '准备中',
                completed: '准备完成',
                dismissed: '已忽略本次',
                removed: '准备待办已移除'
            }
        }
    },
    computed: {
        calendarMinDate() {
            const base = this.selected && this.selected.occurrenceDate
                ? new Date(String(this.selected.occurrenceDate).replace(/-/g, '/'))
                : new Date()
            return new Date(base.getTime() - 90 * 24 * 3600 * 1000)
        },
        calendarMaxDate() {
            if (!this.selected || !this.selected.occurrenceDate) return new Date(new Date().getFullYear() + 1, 11, 31)
            return new Date(String(this.selected.occurrenceDate).replace(/-/g, '/'))
        }
    },
    methods: {
        open(item) {
            this.selected = item
            this.form = {
                content: `准备${item.name}`,
                dueDate: item.suggestedDueDate,
                occurrenceDate: item.occurrenceDate
            }
            this.preview = null
            this.error = ''
        },
        onDateConfirm(date) {
            this.form.dueDate = this.$parseTime(date, '{y}-{m}-{d}')
            this.calendarVisible = false
        },
        async confirm() {
            if (this.busy) return
            this.busy = true
            try {
                const signature = JSON.stringify(this.form)
                if (!this.preview || this.preview.signature !== signature) {
                    const response = await previewCommemorationPreparation(this.selected.id, this.form)
                    this.preview = { signature, ...response.data }
                }
                let ok = false
                try {
                    await mConfirm('确认创建准备待办', `${this.preview.content}，完成日期：${this.preview.dueDate}`)
                    ok = true
                } catch (e) {
                    ok = false
                }
                if (!ok) return
                await commitCommemorationPreparation({ previewId: this.preview.previewId })
                this.selected = null
                this.$emit('changed')
            } catch (e) {
                this.error = '未确认成功，可重试；日期或来源变化时请收起并刷新再预览'
            } finally {
                this.busy = false
            }
        },
        async dismiss(item) {
            try {
                await dismissCommemorationPreparation(item.id, { occurrenceDate: item.occurrenceDate })
            } catch (e) {
                // 忽略失败也刷新，保持与服务端一致
            }
            this.$emit('changed')
        }
    }
}
</script>

<style lang="scss" scoped>
.cp-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    padding: 10px;
    margin-bottom: 8px;
    background: var(--pnkx-info-soft);
    border-radius: 10px;

    &__main {
        flex: 1;
        min-width: 0;
    }

    &__title {
        font-size: 14px;
        font-weight: 500;
        color: var(--pnkx-text);
    }

    &__meta {
        display: flex;
        align-items: center;
        gap: 6px;
        flex-wrap: wrap;
        margin-top: 5px;
    }

    &__actions {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        justify-content: flex-end;
        gap: 6px;
    }
}

.cp-chip {
    padding: 1px 8px;
    font-size: 11px;
    color: var(--pnkx-text-secondary);
    background: var(--pnkx-surface-muted);
    border-radius: 999px;
}

.cp-tag {
    padding: 1px 7px;
    font-size: 11px;
    color: var(--pnkx-info);
    background: var(--pnkx-surface);
    border-radius: 5px;

    &--ready {
        color: var(--pnkx-success);
        background: var(--pnkx-success-soft);
    }
}

.cp-editor {
    padding: 12px;
    margin-top: 6px;
    background: var(--pnkx-surface-muted);
    border-radius: 10px;

    &__tip {
        font-size: 12px;
        color: var(--pnkx-text-secondary);
    }

    &__field {
        margin-top: 10px;
        border: 1px solid var(--pnkx-border);
        border-radius: 8px;
        padding: 4px 8px;
    }

    &__date {
        display: block;
        margin-top: 10px;
        font-size: 13px;
        color: var(--pnkx-primary);
        background: transparent;
        border: 0;
        padding: 0;
    }

    &__error {
        margin-top: 8px;
        font-size: 12px;
        color: var(--pnkx-danger);
    }

    &__actions {
        display: flex;
        gap: 8px;
        margin-top: 12px;
    }
}
</style>
