<template>
    <div class="plan-card" aria-label="操作确认卡片">
        <div v-if="busy" class="plan-card__busy">正在同步计划…</div>
        <div v-if="error" class="plan-card__error">{{ error }}</div>
        <template v-if="plan">
            <div class="plan-card__head">
                <span class="plan-card__title">{{ plan.title }}</span>
                <span class="plan-card__badge" :class="'is-' + tone(plan.status)">{{ label(plan.status) }}</span>
            </div>
            <div class="plan-card__hint">{{ hint(plan.status) }}</div>
            <div v-for="(step, index) in plan.steps" :key="index" class="plan-card__step">
                <div class="plan-card__step-title">{{ index + 1 }}. {{ tool(step.tool_code) }}</div>
                <div v-for="(field, i) in fields(step.input_json)" :key="i" class="plan-card__field">
                    <span class="plan-card__field-label">{{ field.label }}</span>
                    <span class="plan-card__field-value">{{ field.value }}</span>
                </div>
                <div v-if="step.error_msg" class="plan-card__error" role="alert">{{ step.error_msg }}</div>
            </div>
            <plan-draft-editor
                :plan="plan"
                :disabled="locked"
                @editing="editing = $event"
                @updated="revised"
            />
            <div v-if="!editing" class="plan-card__actions">
                <van-button
                    v-if="plan.replacement_id"
                    size="small"
                    plain
                    :disabled="locked"
                    @click="load(plan.replacement_id)"
                >
                    查看修改后的预览
                </van-button>
                <van-button
                    v-if="plan.status === 'awaiting_confirmation'"
                    size="small"
                    type="primary"
                    :disabled="locked"
                    @click="run('confirm')"
                >
                    确认执行以上内容
                </van-button>
                <van-button
                    v-if="['failed', 'partially_succeeded'].includes(plan.status)"
                    size="small"
                    type="primary"
                    plain
                    :disabled="locked"
                    @click="run('retry')"
                >
                    重试未完成步骤
                </van-button>
                <van-button
                    v-if="plan.status === 'awaiting_confirmation'"
                    size="small"
                    plain
                    :disabled="locked"
                    @click="run('cancel')"
                >
                    取消计划
                </van-button>
            </div>
        </template>
        <van-button
            v-if="!editing"
            size="small"
            plain
            class="plan-card__refresh"
            :disabled="locked"
            @click="load(selectedId || planId)"
        >
            刷新状态
        </van-button>
    </div>
</template>

<script>
import request from '@/utils/request'
import PlanDraftEditor from './PlanDraftEditor.vue'
import { planLabels, toolLabels, planFields, planHint, planTone } from '@/utils/operationPlanPresentation'

export default {
    name: 'MChatPlanCard',
    components: { PlanDraftEditor },
    props: { planId: String, disabled: Boolean },
    data() {
        return { plan: null, busy: false, editing: false, error: '', requestVersion: 0, selectedId: null }
    },
    computed: {
        locked() {
            return this.busy || this.disabled || this.editing
        }
    },
    watch: {
        planId: {
            immediate: true,
            handler(id) {
                this.load(id)
            }
        }
    },
    beforeUnmount() {
        this.requestVersion++
    },
    methods: {
        label: (status) => planLabels[status] || status,
        tool: (code) => toolLabels[code] || code,
        fields: planFields,
        hint: planHint,
        tone: planTone,
        async load(id) {
            if (!id || this.editing || this.busy) return
            const version = ++this.requestVersion
            this.selectedId = id
            this.busy = true
            this.error = ''
            this.plan = null
            try {
                const r = await request({ url: `/operation-plans/${id}` })
                if (version === this.requestVersion) this.plan = r.data
            } catch (e) {
                if (version === this.requestVersion) this.error = '预览加载失败，请重试。'
            } finally {
                if (version === this.requestVersion) this.busy = false
            }
        },
        revised(plan) {
            this.plan = plan
            this.selectedId = plan.id
            this.editing = false
        },
        async run(action) {
            if (this.locked || !this.plan) return
            this.busy = true
            this.error = ''
            const version = ++this.requestVersion
            try {
                const r = await request({ url: `/operation-plans/${this.plan.id}/${action}`, method: 'post' })
                if (version === this.requestVersion) this.plan = r.data
            } catch (e) {
                if (version === this.requestVersion)
                    this.error = (e && e.message) || '操作未完成，请刷新状态后重试。'
            } finally {
                if (version === this.requestVersion) this.busy = false
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.plan-card {
    margin-top: 10px;
    padding: 12px;
    border: 1px solid var(--pnkx-border);
    border-radius: 12px;
    background: var(--pnkx-surface-muted);
    color: var(--pnkx-text);
    font-size: 13px;
}

.plan-card__busy {
    color: var(--pnkx-text-muted);
}

.plan-card__error {
    color: var(--pnkx-danger);
    background: var(--pnkx-danger-soft);
    border-radius: 8px;
    padding: 8px 10px;
    margin: 8px 0;
    line-height: 1.6;
}

.plan-card__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
}

.plan-card__title {
    font-weight: 600;
    font-size: 14px;
    word-break: break-word;
}

.plan-card__badge {
    flex-shrink: 0;
    font-size: 11px;
    border-radius: 6px;
    padding: 2px 8px;
    background: var(--pnkx-surface);
    color: var(--pnkx-text-secondary);
    border: 1px solid var(--pnkx-border-soft);

    &.is-primary {
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
        border-color: transparent;
    }

    &.is-success {
        background: var(--pnkx-success-soft);
        color: var(--pnkx-success);
        border-color: transparent;
    }

    &.is-danger {
        background: var(--pnkx-danger-soft);
        color: var(--pnkx-danger);
        border-color: transparent;
    }
}

.plan-card__hint {
    margin-top: 4px;
    color: var(--pnkx-text-secondary);
    line-height: 1.6;
}

.plan-card__step {
    margin-top: 12px;
    padding: 10px;
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 10px;
    background: var(--pnkx-surface);
}

.plan-card__step-title {
    font-weight: 600;
}

.plan-card__field {
    display: flex;
    gap: 10px;
    margin-top: 8px;
    line-height: 1.6;
}

.plan-card__field-label {
    flex: 0 0 76px;
    color: var(--pnkx-text-muted);
}

.plan-card__field-value {
    flex: 1;
    min-width: 0;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
}

.plan-card__actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 10px;
}

.plan-card__refresh {
    margin-top: 10px;
}
</style>
