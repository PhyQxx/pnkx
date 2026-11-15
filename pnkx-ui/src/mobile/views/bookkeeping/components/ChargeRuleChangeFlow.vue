<template>
    <van-popup
        :show="visible"
        position="bottom"
        round
        :close-on-click-overlay="!busy"
        teleport="body"
        @update:show="onPopupShow"
    >
        <div v-if="task" class="flow">
            <div class="flow__header">
                <div class="flow__title">{{ task.kind === 'backfill' ? '补齐暂停期间账期' : '调整账期规则' }}</div>
                <van-icon name="cross" class="flow__close" @click="cancel" />
            </div>

            <div class="flow__steps">
                <div
                    v-for="(name, i) in ['变化摘要', '影响账期', '确认执行']"
                    :key="name"
                    class="flow__step"
                    :class="{ 'flow__step--on': i === step, 'flow__step--done': i < step }"
                >
                    <div class="flow__step-dot">{{ i < step ? '✓' : i + 1 }}</div>
                    <div class="flow__step-name">{{ name }}</div>
                </div>
            </div>

            <div class="flow__body">
                <!-- 第 1 步：变化摘要 -->
                <template v-if="step === 0">
                    <template v-if="task.kind === 'change'">
                        <div v-for="d in diffs" :key="d.field" class="flow__diff">
                            <span class="flow__diff-label">{{ d.label }}</span>
                            <span class="flow__diff-before">{{ d.before }}</span>
                            <span class="flow__diff-arrow">→</span>
                            <span class="flow__diff-after">{{ d.after }}</span>
                        </div>
                        <div v-if="!diffs.length" class="flow__hint">没有检测到字段变化。</div>
                        <div v-if="(task.preview.duplicates || []).length" class="flow__warn">
                            存在同金额规则：{{ task.preview.duplicates.map(d => d.name).join('、') }}，请核对是否重复并保留主规则。
                        </div>
                    </template>
                    <template v-else>
                        <div class="flow__lead">将补齐以下 {{ task.preview.periods.length }} 个暂停期间账期：</div>
                        <div class="flow__periods">
                            <span v-for="p in task.preview.periods" :key="p" class="flow__period">{{ p }}</span>
                        </div>
                        <div class="flow__hint">每期预计 {{ task.preview.amountPerPeriod }} 元，只生成待确认账期，不直接增加支出。</div>
                    </template>
                </template>

                <!-- 第 2 步：影响账期 -->
                <template v-else-if="step === 1">
                    <template v-if="task.kind === 'change'">
                        <div class="flow__lead">
                            该规则当前有 {{ affected.length }} 个未入账账期，将保留原金额快照，不受本次调整影响；之后的新账期按新规则生成。
                        </div>
                        <div v-if="affectedLoading" class="flow__hint">正在加载账期…</div>
                        <div v-if="affectedError" class="flow__warn">{{ affectedError }}</div>
                        <van-button
                            v-if="affectedError"
                            size="small"
                            plain
                            round
                            class="flow__retry"
                            @click="loadAffected"
                        >
                            重试加载
                        </van-button>
                        <div v-for="i in affected" :key="i.id" class="flow__instance">
                            <span class="flow__instance-date">{{ i.dueDate }}</span>
                            <span class="flow__instance-amount">¥{{ i.expectedAmount }}</span>
                            <span class="flow__instance-status">{{ statusLabels[i.status] || i.status }}</span>
                        </div>
                    </template>
                    <template v-else>
                        <div class="flow__lead">补齐后这些账期会出现在「周期账单」中，等待逐期确认入账：</div>
                        <div class="flow__periods">
                            <span v-for="p in task.preview.periods" :key="p" class="flow__period flow__period--warn">{{ p }}</span>
                        </div>
                        <div class="flow__hint">规则保持暂停；剩余超过 100 期的部分可再次预览补齐。</div>
                    </template>
                </template>

                <!-- 第 3 步：确认执行 -->
                <template v-else>
                    <div class="flow__lead">{{ task.kind === 'change' ? '确认后立即生效：' : '确认后生成待确认账期：' }}</div>
                    <div class="flow__notice">{{ task.preview.notice }}</div>
                    <div class="flow__hint">预览有效期约 15 分钟；来源若被再次修改，需要重新预览。</div>
                    <div v-if="commitError" class="flow__warn">{{ commitError }}</div>
                    <van-button
                        v-if="commitError"
                        size="small"
                        plain
                        round
                        class="flow__retry"
                        @click="refreshPreview"
                    >
                        重新预览
                    </van-button>
                </template>
            </div>

            <div class="flow__footer">
                <van-button size="small" round :disabled="busy" @click="cancel">取消</van-button>
                <van-button v-if="step > 0" size="small" round :disabled="busy" @click="step--">上一步</van-button>
                <van-button
                    v-if="step < 2"
                    size="small"
                    type="primary"
                    round
                    :disabled="step === 0 && task.kind === 'change' && !diffs.length"
                    @click="nextStep"
                >
                    下一步
                </van-button>
                <van-button
                    v-else
                    size="small"
                    type="primary"
                    round
                    :loading="committing"
                    @click="confirmCommit"
                >
                    确认执行
                </van-button>
            </div>
        </div>
    </van-popup>
</template>

<script>
import request from '@/utils/request'
import { showToast } from '@/mobile/utils/ui'
import {
    setRuleFlowPresenter,
    formatDiffs,
    CHANGE_PREVIEW_URL,
    BACKFILL_PREVIEW_URL,
    CHANGE_COMMIT_URL
} from '@/utils/ruleChangeFlow'

export default {
    name: 'BkChargeRuleChangeFlow',
    data() {
        return {
            visible: false,
            step: 0,
            task: null,
            affected: [],
            affectedLoading: false,
            affectedError: '',
            committing: false,
            commitError: '',
            statusLabels: { pending: '待确认', payment_failed: '扣费失败', planned: '按计划' },
            revoke: null,
            resolver: null
        }
    },
    computed: {
        diffs() {
            return formatDiffs((this.task && this.task.preview && this.task.preview.changes) || [])
        },
        busy() {
            return this.affectedLoading || this.committing
        }
    },
    mounted() {
        this.revoke = setRuleFlowPresenter(task => this.open(task))
    },
    beforeUnmount() {
        if (this.revoke) this.revoke()
        if (this.resolver) this.resolver({ status: 'cancelled' })
    },
    methods: {
        open(task) {
            return new Promise(resolve => {
                this.task = task
                this.step = 0
                this.affected = []
                this.affectedError = ''
                this.commitError = ''
                this.resolver = resolve
                this.visible = true
                if (task.kind === 'change') this.loadAffected()
            })
        },
        nextStep() {
            this.step++
            if (this.step === 2) this.commitError = ''
        },
        onPopupShow(value) {
            if (!value && !this.busy) this.cancel()
        },
        async loadAffected() {
            this.affectedLoading = true
            this.affectedError = ''
            try {
                const r = await request({
                    url: '/charge/instances',
                    method: 'get',
                    params: {
                        sourceType: this.task.sourceType,
                        sourceId: this.task.sourceId,
                        pageNum: 1,
                        pageSize: 100
                    }
                })
                const rows = r.rows || (r.data && r.data.rows) || []
                this.affected = rows.filter(i => ['pending', 'payment_failed'].includes(i.status))
            } catch (e) {
                this.affectedError = '账期加载失败，可重试；不影响后续确认。'
                this.affected = []
            } finally {
                this.affectedLoading = false
            }
        },
        async confirmCommit() {
            if (this.committing) return
            this.committing = true
            this.commitError = ''
            try {
                const r = await request({
                    url: CHANGE_COMMIT_URL,
                    method: 'post',
                    data: { previewId: this.task.preview.previewId }
                })
                this.visible = false
                showToast(this.task.kind === 'change' ? '规则已更新' : '账期已补齐')
                if (this.resolver) this.resolver({ status: 'committed', result: r })
                this.resolver = null
            } catch (e) {
                this.commitError = (e && e.message) || '提交失败，可重试同一预览或重新预览。'
            } finally {
                this.committing = false
            }
        },
        async refreshPreview() {
            const url = this.task.kind === 'change' ? CHANGE_PREVIEW_URL : BACKFILL_PREVIEW_URL
            try {
                const r = await request({ url, method: 'post', data: this.task.input })
                this.task.preview = r.data
                this.commitError = ''
                this.step = 0
            } catch (e) {
                this.commitError = '重新预览失败：' + ((e && e.message) || '请稍后重试')
            }
        },
        cancel() {
            if (this.busy) return
            this.visible = false
            if (this.resolver) this.resolver({ status: 'cancelled' })
            this.resolver = null
        }
    }
}
</script>

<style lang="scss" scoped>
.flow {
    max-height: 82vh;
    display: flex;
    flex-direction: column;
    padding-bottom: env(safe-area-inset-bottom);

    &__header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 14px 16px 6px;
    }

    &__title {
        font-size: 16px;
        font-weight: 700;
        color: var(--pnkx-text);
    }

    &__close {
        font-size: 18px;
        color: var(--pnkx-text-muted);
        padding: 2px 4px;
    }

    &__steps {
        display: flex;
        justify-content: space-around;
        padding: 8px 16px 12px;
        border-bottom: 1px solid var(--pnkx-border-soft);
    }

    &__step {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 4px;
    }

    &__step-dot {
        width: 22px;
        height: 22px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 11px;
        color: var(--pnkx-text-muted);
        background: var(--pnkx-surface-muted);
    }

    &__step--on &__step-dot {
        color: #fff;
        background: var(--pnkx-primary);
    }

    &__step--done &__step-dot {
        color: #fff;
        background: var(--pnkx-success);
    }

    &__step-name {
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }

    &__step--on &__step-name {
        color: var(--pnkx-primary);
        font-weight: 600;
    }

    &__body {
        flex: 1;
        min-height: 0;
        max-height: 52vh;
        overflow-y: auto;
        padding: 12px 16px;
    }

    &__lead {
        font-size: 13px;
        color: var(--pnkx-text);
        margin-bottom: 8px;
        line-height: 1.6;
    }

    &__hint {
        font-size: 12px;
        color: var(--pnkx-text-muted);
        margin: 8px 0;
        line-height: 1.6;
    }

    &__diff {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 0;
        border-bottom: 1px solid var(--pnkx-border-soft);

        &:last-of-type {
            border-bottom: none;
        }
    }

    &__diff-label {
        width: 64px;
        font-size: 13px;
        color: var(--pnkx-text);
        font-weight: 600;
        flex-shrink: 0;
    }

    &__diff-before {
        flex: 1;
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }

    &__diff-arrow {
        color: var(--pnkx-text-muted);
    }

    &__diff-after {
        flex: 1;
        font-size: 12px;
        color: var(--pnkx-primary);
        text-align: right;
    }

    &__periods {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
        margin: 6px 0;
    }

    &__period {
        padding: 2px 10px;
        font-size: 12px;
        color: var(--pnkx-primary);
        background: var(--pnkx-primary-soft);
        border-radius: 999px;
    }

    &__period--warn {
        color: var(--pnkx-warning);
        background: var(--pnkx-warning-soft);
    }

    &__instance {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 8px 0;
        border-bottom: 1px solid var(--pnkx-border-soft);

        &:last-of-type {
            border-bottom: none;
        }
    }

    &__instance-date {
        flex: 1;
        font-size: 13px;
        color: var(--pnkx-text);
    }

    &__instance-amount {
        font-size: 13px;
        color: var(--pnkx-text);
    }

    &__instance-status {
        font-size: 11px;
        color: var(--pnkx-text-muted);
        background: var(--pnkx-surface-muted);
        border-radius: 4px;
        padding: 1px 6px;
    }

    &__warn {
        padding: 8px 10px;
        background: var(--pnkx-warning-soft);
        border-radius: 8px;
        font-size: 12px;
        color: var(--pnkx-warning);
        margin: 8px 0;
        line-height: 1.6;
    }

    &__notice {
        padding: 8px 10px;
        background: var(--pnkx-surface-muted);
        border-radius: 8px;
        font-size: 13px;
        color: var(--pnkx-text);
        margin-bottom: 6px;
        line-height: 1.6;
    }

    &__retry {
        margin: 6px 0;
    }

    &__footer {
        display: flex;
        gap: 8px;
        padding: 10px 16px;
        border-top: 1px solid var(--pnkx-border-soft);

        .van-button {
            flex: 1;
        }
    }
}
</style>
