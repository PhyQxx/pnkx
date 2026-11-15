<template>
    <div class="plans">
        <div class="plans__head">
            <div>
                <div class="plans__heading">操作计划</div>
                <div class="plans__sub">先确认，再执行。每一步都有结果。</div>
            </div>
            <van-button size="small" plain :loading="loading" :disabled="busy || draftEditing" @click="load">
                刷新
            </van-button>
        </div>

        <div class="plans__filters">
            <button
                v-for="f in filters"
                :key="f.key"
                type="button"
                class="plans__filter"
                :class="{ 'is-selected': filter === f.key }"
                @click="filter = f.key"
            >
                {{ f.label }} {{ count(f.key) }}
            </button>
        </div>

        <van-field
            v-model="query"
            class="plans__search"
            placeholder="搜索计划名称"
            clearable
            aria-label="搜索计划名称"
        />

        <div v-if="error" class="plans__error">{{ error }}</div>
        <div v-if="loading && !plans.length" class="plans__empty">正在加载计划…</div>

        <div v-for="p in filtered" :key="p.id" class="plans__row">
            <div class="plans__title-line">
                <span class="plans__title">{{ p.title }}</span>
                <span class="plans__badge" :class="'is-' + tone(p.status)">{{ labels[p.status] || p.status }}</span>
            </div>
            <div class="plans__sub">{{ hint(p.status) }}</div>
            <van-button
                size="small"
                plain
                type="primary"
                class="plans__view-button"
                :disabled="busy || draftEditing"
                @click="inspect(p.id)"
            >
                {{ actionLabel(p.status) }}
            </van-button>
        </div>

        <div v-if="!loading && !filtered.length && !error" class="plans__empty">
            {{ plans.length ? '没有匹配的计划，试试其他筛选或关键词' : '暂无操作计划，可在 AI 对话中创建草稿' }}
        </div>
        <div class="plans__list-note">最近加载 {{ plans.length }} 个计划 · 待处理含待确认和执行失败</div>

        <!-- 计划详情 -->
        <div v-if="opened" class="plans__detail">
            <div class="plans__head">
                <div class="plans__heading">计划详情</div>
                <van-button size="small" plain :disabled="busy || draftEditing" @click="closeDetail">
                    收起
                </van-button>
            </div>
            <div v-if="detailLoading" class="plans__empty">正在加载详情…</div>
            <div v-if="detailError" class="plans__error">{{ detailError }}</div>
            <van-button v-if="detailError && !detail" size="small" plain @click="inspect(selectedId)">
                重新加载
            </van-button>
            <template v-if="detail && !detailLoading">
                <div class="plans__title-line">
                    <span class="plans__title">{{ detail.title }}</span>
                    <span class="plans__badge" :class="'is-' + tone(detail.status)">
                        {{ labels[detail.status] || detail.status }}
                    </span>
                </div>
                <div class="plans__sub">{{ hint(detail.status) }}</div>
                <div v-if="detail.replacement_id" class="plans__replacement">
                    <div class="plans__sub">此草稿已修改，请打开新预览核对后确认。</div>
                    <van-button size="small" plain :disabled="busy || draftEditing" @click="inspect(detail.replacement_id)">
                        查看修改后的预览
                    </van-button>
                </div>
                <div class="plans__progress">{{ completed }} / {{ detail.steps.length }} 步已完成</div>
                <div v-for="s in detail.steps" :key="s.step_number" class="plans__step">
                    <div class="plans__title-line">
                        <span class="plans__title">{{ s.step_number }}. {{ tools[s.tool_code] || s.tool_code }}</span>
                        <span class="plans__badge" :class="'is-' + tone(s.status)">
                            {{ labels[s.status] || s.status }}
                        </span>
                    </div>
                    <div v-for="(f, i) in fields(s.input_json)" :key="i" class="plans__field">
                        <span class="plans__field-label">{{ f.label }}</span>
                        <span class="plans__field-value">{{ f.value }}</span>
                    </div>
                    <div v-if="result(s).sourceId" class="plans__sub">已创建记录 #{{ result(s).sourceId }}</div>
                    <div v-if="s.error_msg" class="plans__error">{{ s.error_msg }}</div>
                </div>

                <plan-draft-editor
                    :plan="detail"
                    :disabled="busy"
                    @editing="draftEditing = $event"
                    @updated="acceptRevision"
                />
                <div v-if="!draftEditing" class="plans__actions">
                    <van-button
                        v-if="detail.status === 'awaiting_confirmation'"
                        block
                        type="primary"
                        :disabled="busy || draftEditing"
                        :loading="busy"
                        @click="run('confirm')"
                    >
                        确认并执行
                    </van-button>
                    <van-button
                        v-if="['failed', 'partially_succeeded'].includes(detail.status)"
                        block
                        type="primary"
                        plain
                        :disabled="busy || draftEditing"
                        :loading="busy"
                        @click="run('retry')"
                    >
                        重试未完成步骤
                    </van-button>
                    <template v-if="detail.status === 'running'">
                        <van-button block plain :disabled="busy || draftEditing" @click="inspect(detail.id)">
                            刷新进度
                        </van-button>
                        <van-button block plain :loading="busy" :disabled="busy || draftEditing" @click="run('retry')">
                            尝试恢复执行
                        </van-button>
                    </template>
                    <van-button
                        v-if="['succeeded', 'partially_succeeded'].includes(detail.status)"
                        block
                        plain
                        :disabled="busy || draftEditing"
                        @click="undo"
                    >
                        预览撤销
                    </van-button>
                    <van-button
                        v-if="['awaiting_confirmation', 'failed'].includes(detail.status)"
                        block
                        plain
                        type="danger"
                        :disabled="busy || draftEditing"
                        @click="run('cancel')"
                    >
                        取消计划
                    </van-button>
                </div>
            </template>
        </div>
    </div>
</template>

<script>
import request from '@/utils/request'
import { mConfirm } from '@/mobile/utils/ui'
import PlanDraftEditor from './PlanDraftEditor.vue'
import {
    planLabels,
    toolLabels,
    planFilters,
    matchesPlan,
    planTone,
    planHint,
    planActionLabel,
    planFields,
    parsePlanValue
} from '@/utils/operationPlanPresentation'

export default {
    name: 'MOperationPlans',
    components: { PlanDraftEditor },
    data() {
        return {
            plans: [],
            detail: null,
            filter: 'all',
            query: '',
            opened: false,
            loading: false,
            detailLoading: false,
            error: '',
            detailError: '',
            busy: false,
            draftEditing: false,
            selectedId: '',
            listRequest: 0,
            detailRequest: 0,
            labels: planLabels,
            tools: toolLabels,
            filters: planFilters
        }
    },
    computed: {
        filtered() {
            return this.plans.filter((p) => matchesPlan(p, this.filter, this.query))
        },
        completed() {
            return (this.detail?.steps || []).filter((s) => s.status === 'succeeded').length
        }
    },
    mounted() {
        this.load()
    },
    beforeUnmount() {
        this.listRequest++
        this.detailRequest++
    },
    methods: {
        acceptRevision(plan) {
            this.detail = plan
            this.selectedId = plan.id
            this.draftEditing = false
            this.load()
        },
        tone: planTone,
        hint: planHint,
        actionLabel: planActionLabel,
        fields: planFields,
        result(s) {
            return parsePlanValue(s.result_json)
        },
        count(key) {
            return this.plans.filter((p) => matchesPlan(p, key)).length
        },
        closeDetail() {
            if (this.busy || this.draftEditing) return
            this.detailRequest++
            this.opened = false
            this.detail = null
        },
        async load() {
            const token = ++this.listRequest
            this.loading = true
            try {
                const r = await request({ url: '/operation-plans', method: 'get' })
                if (token === this.listRequest) {
                    this.plans = r.data || []
                    this.error = ''
                }
            } catch (e) {
                if (token === this.listRequest) this.error = '加载失败，已有结果保留，请刷新重试'
            } finally {
                if (token === this.listRequest) this.loading = false
            }
        },
        async inspect(id) {
            if (this.busy || this.draftEditing) return
            const token = ++this.detailRequest
            this.selectedId = id
            this.opened = true
            this.detail = null
            this.detailError = ''
            this.detailLoading = true
            try {
                const r = await request({ url: `/operation-plans/${id}`, method: 'get' })
                if (token === this.detailRequest) this.detail = r.data
            } catch (e) {
                if (token === this.detailRequest) this.detailError = '详情加载失败，请重试'
            } finally {
                if (token === this.detailRequest) this.detailLoading = false
            }
        },
        async run(action) {
            if (this.busy || !this.detail) return
            this.busy = true
            this.detailError = ''
            try {
                const r = await request({
                    url: `/operation-plans/${this.detail.id}/${action}`,
                    method: 'post'
                })
                this.detail = r.data
                await this.load()
            } catch (e) {
                this.detailError = (e && e.message) || '操作未完成，请刷新详情核对后重试'
            } finally {
                this.busy = false
            }
        },
        async undo() {
            if (this.busy || !this.detail) return
            this.busy = true
            this.detailError = ''
            const id = this.detail.id
            let count = 0
            try {
                const r = await request({ url: `/operation-plans/${id}/compensation-preview`, method: 'post' })
                count = (r.data && r.data.compensation ? r.data.compensation.length : 0) || 0
            } catch (e) {
                this.detailError = (e && e.message) || '当前无法撤销，请在原模块核对内容'
                this.busy = false
                return
            }
            this.busy = false
            try {
                await mConfirm('确认撤销', `将移除 ${count} 个本计划创建且未修改的对象。`)
            } catch (e) {
                return
            }
            if (this.detail?.id === id) await this.run('compensate')
        }
    }
}
</script>

<style lang="scss" scoped>
.plans {
    background: var(--pnkx-surface);
    padding: 14px;
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 12px;
    color: var(--pnkx-text);
}

.plans__head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 8px;
}

.plans__heading {
    font-size: 16px;
    font-weight: 650;
}

.plans__sub {
    font-size: 12px;
    line-height: 1.65;
    color: var(--pnkx-text-secondary);
    margin-top: 4px;
}

.plans__filters {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin: 12px 0 8px;
}

.plans__filter {
    border: 1px solid var(--pnkx-border-soft);
    background: var(--pnkx-surface-muted);
    color: var(--pnkx-text-secondary);
    font-size: 12px;
    border-radius: 999px;
    padding: 4px 12px;

    &.is-selected {
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
        border-color: var(--pnkx-primary);
    }
}

.plans__search {
    background: var(--pnkx-surface-muted);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 8px;
    padding: 4px 4px;
    margin-bottom: 6px;
}

.plans__row {
    padding: 13px 0;
    border-bottom: 1px solid var(--pnkx-border-soft);

    &:last-of-type {
        border-bottom: none;
    }
}

.plans__title-line {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
}

.plans__title {
    font-size: 14px;
    font-weight: 600;
    word-break: break-word;
}

.plans__badge {
    display: inline-block;
    font-size: 11px;
    border-radius: 6px;
    padding: 2px 8px;
    background: var(--pnkx-surface-muted);
    color: var(--pnkx-text-secondary);

    &.is-primary {
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
    }

    &.is-success {
        background: var(--pnkx-success-soft);
        color: var(--pnkx-success);
    }

    &.is-danger {
        background: var(--pnkx-danger-soft);
        color: var(--pnkx-danger);
    }
}

.plans__view-button {
    display: block;
    margin: 8px 0 0 auto;
}

.plans__empty {
    padding: 20px 4px;
    font-size: 13px;
    color: var(--pnkx-text-secondary);
}

.plans__list-note {
    font-size: 11px;
    color: var(--pnkx-text-muted);
    margin-top: 10px;
    line-height: 1.6;
}

.plans__error {
    color: var(--pnkx-danger);
    background: var(--pnkx-danger-soft);
    padding: 8px 10px;
    border-radius: 8px;
    margin: 8px 0;
    font-size: 12px;
    line-height: 1.6;
}

.plans__detail {
    margin-top: 14px;
    border-top: 1px solid var(--pnkx-primary-soft);
    padding-top: 12px;
}

.plans__replacement {
    margin-top: 8px;
}

.plans__progress {
    font-size: 12px;
    color: var(--pnkx-text-secondary);
    margin: 8px 0;
}

.plans__step {
    padding: 11px;
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 10px;
    margin: 10px 0;
    background: var(--pnkx-surface-muted);
}

.plans__field {
    display: flex;
    gap: 10px;
    margin-top: 9px;
    font-size: 13px;
    line-height: 1.6;
}

.plans__field-label {
    flex: 0 0 60px;
    color: var(--pnkx-text-muted);
}

.plans__field-value {
    min-width: 0;
    white-space: pre-wrap;
    word-break: break-word;
}

.plans__actions {
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-top: 12px;
}
</style>
