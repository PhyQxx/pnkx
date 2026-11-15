<template>
    <div class="report-actions">
        <div class="report-actions__head">
            <div class="m-section-title report-actions__title">报告行动与复盘</div>
            <van-button size="small" round plain type="primary" :loading="loading" @click="snapshot">生成事实快照</van-button>
        </div>
        <div class="report-actions__tip">事实来自实际账单；快照不会随历史账单修改而改变。采纳行动前请核对内容。</div>

        <!-- 快照列表 -->
        <div v-for="i in rows" :key="i.id" class="insight">
            <div class="insight__fact">{{ i.fact_text }}</div>

            <!-- 可选行动草稿 -->
            <div v-if="i.status === 'proposed'" class="insight__candidates">
                <div class="insight__hint">可选行动：根据事实快照生成的规则草稿，可修改后再确认。</div>
                <div v-for="c in i.candidates || []" :key="c.id" class="candidate">
                    <div class="candidate__title">{{ c.title }}</div>
                    <div class="candidate__basis">{{ c.basis }}</div>
                    <van-button size="small" plain round hairline type="primary" @click="open(i, c)">选择并编辑</van-button>
                </div>
            </div>

            <!-- 行动复盘 -->
            <div v-if="i.action_status" class="insight__review">
                行动复盘：{{ statusLabels[i.action_status] || i.action_status }}
                <template v-if="i.target"> · {{ i.target.content }}</template>
            </div>
            <div v-if="i.execution_status" class="insight__review">计划状态：{{ statusLabels[i.execution_status] || i.execution_status }}</div>

            <div class="insight__meta">生成于 {{ i.create_time }} · {{ statusText(i) }}</div>

            <div class="insight__ops">
                <van-button size="small" plain round hairline @click="sources(i)">来源明细</van-button>
                <van-button v-if="i.status === 'proposed'" size="small" plain round hairline type="primary" @click="open(i)">创建行动</van-button>
                <van-button v-if="i.status === 'proposed' && !i.plan_id" size="small" plain round hairline type="danger" @click="dismiss(i)">忽略</van-button>
            </div>
        </div>

        <div v-if="!rows.length && !loading" class="m-empty">
            <span class="insights-empty__emoji">🧾</span>
            <span>生成事实快照后，可将复盘结论转成具体行动</span>
        </div>

        <!-- 创建行动表单 -->
        <van-popup
            v-model:show="showForm"
            position="bottom"
            round
            teleport="body"
            class="action-popup"
        >
            <div class="action-form">
                <div class="action-form__title">预览后续行动</div>
                <van-cell-group :border="false" class="m-cell-card">
                    <van-field
                        v-model="form.content"
                        type="textarea"
                        rows="2"
                        autosize
                        label="行动内容"
                        placeholder="具体行动，例如：安排下周三顿晚饭"
                    />
                    <van-cell
                        title="完成日期"
                        :value="form.dueDate || '请选择'"
                        is-link
                        @click="showDatePicker = true"
                    />
                </van-cell-group>
                <div class="action-form__hint">确认后创建一条待办，不会自动调整预算或更改账单。</div>
                <div v-if="actionError" class="action-form__error">{{ actionError }}</div>
                <div class="action-form__ops">
                    <van-button round block plain @click="showForm = false">取消</van-button>
                    <van-button round block type="primary" :loading="saving" @click="accept">预览并确认</van-button>
                </div>
            </div>
        </van-popup>

        <!-- 日期选择 -->
        <van-popup v-model:show="showDatePicker" position="bottom" round teleport="body">
            <van-date-picker
                :model-value="datePickValue"
                title="选择完成日期"
                @confirm="onDateConfirm"
                @cancel="showDatePicker = false"
            />
        </van-popup>

        <!-- 来源明细 -->
        <van-popup
            v-model:show="showSources"
            position="bottom"
            round
            teleport="body"
            class="source-popup"
        >
            <div class="source-panel">
                <div class="source-panel__title">当前来源明细（可能与快照不同）</div>
                <div v-if="sourceRows.length > 0" class="source-panel__list">
                    <div v-for="r in sourceRows" :key="r.id" class="source-row">
                        <span class="source-row__time">{{ r.pay_time }}</span>
                        <span class="source-row__type">{{ r.type_name }}</span>
                        <span class="source-row__money">¥{{ r.money }}</span>
                    </div>
                </div>
                <div v-else class="m-empty">暂无来源数据</div>
                <div class="source-panel__ops">
                    <van-button size="small" plain round :disabled="sourcePage === 1" @click="changeSourcePage(-1)">上一页</van-button>
                    <van-button size="small" plain round :disabled="sourceRows.length < 30" @click="changeSourcePage(1)">下一页</van-button>
                </div>
            </div>
        </van-popup>
    </div>
</template>

<script>
import {
    listLifeReportInsights,
    createInsightSnapshot,
    previewInsightAction,
    acceptInsightAction,
    dismissInsight,
    listInsightSources,
    getOperationPlan
} from '@/api/px/life/report'
import { mConfirm, mSuccess, mError } from '@/mobile/utils/ui'

export default {
    name: 'MReportActions',
    props: {
        period: {
            type: String,
            default: 'month'
        }
    },
    data() {
        return {
            statusLabels: {
                awaiting_confirmation: '待确认',
                running: '执行中',
                succeeded: '已执行',
                failed: '失败',
                partially_succeeded: '部分完成',
                accepted: '待办进行中',
                completed: '待办已完成',
                cancelled: '已取消或来源移除',
                compensated: '已撤销'
            },
            rows: [],
            loading: false,
            saving: false,
            actionError: '',
            showForm: false,
            showDatePicker: false,
            selected: null,
            form: { content: '', dueDate: '' },
            showSources: false,
            sourceRows: [],
            sourcePage: 1,
            sourceInsight: null
        }
    },
    computed: {
        datePickValue() {
            const value = this.form.dueDate || this.parseTime(new Date(), '{y}-{m}-{d}')
            const parts = value.split('-')
            return [Number(parts[0]), Number(parts[1]), Number(parts[2])]
        }
    },
    mounted() {
        this.load()
    },
    methods: {
        async load() {
            try {
                const r = await listLifeReportInsights()
                this.rows = r.data || []
            } catch (e) {
                this.rows = []
            }
        },
        async snapshot() {
            this.loading = true
            try {
                await createInsightSnapshot({ period: this.period })
                await this.load()
                mSuccess('快照已生成')
            } catch (e) {
                mError('生成失败')
            } finally {
                this.loading = false
            }
        },
        statusText(i) {
            return i.status === 'accepted' ? '已采纳' : (i.status === 'dismissed' ? '已忽略' : '待决策')
        },
        async open(insight, candidate) {
            this.selected = insight
            this.actionError = ''
            this.form = candidate
                ? { content: candidate.content, dueDate: candidate.dueDate, candidateId: candidate.id }
                : { content: '', dueDate: '' }
            if (!candidate && insight.plan_id) {
                const p = await getOperationPlan(insight.plan_id)
                const args = JSON.parse(p.data.steps[0].input_json)
                this.form = { ...args, candidateId: insight.selected_candidate }
            }
            this.showForm = true
        },
        onDateConfirm({ selectedValues }) {
            const [year, month, day] = selectedValues
            this.form.dueDate = year + '-' + String(month).padStart(2, '0') + '-' + String(day).padStart(2, '0')
            this.showDatePicker = false
        },
        async accept() {
            if (this.saving) return
            if (!this.form.content) {
                this.actionError = '请填写具体行动内容'
                return
            }
            this.saving = true
            try {
                const p = await previewInsightAction(this.selected.id, this.form)
                const args = JSON.parse(p.data.steps[0].input_json)
                let ok = true
                try {
                    await mConfirm('确认创建待办', args.content + '，完成日期：' + args.dueDate)
                } catch (e) {
                    ok = false
                }
                if (!ok) return
                const result = await acceptInsightAction(this.selected.id, { expectedPlanId: p.data.id })
                if (result.data.status !== 'succeeded') {
                    this.actionError = '行动尚未执行成功，请在操作计划中查看失败原因并重试'
                    await this.load()
                    return
                }
                this.showForm = false
                mSuccess('已创建待办')
                await this.load()
            } catch (e) {
                mError('操作失败')
            } finally {
                this.saving = false
            }
        },
        async sources(insight, reset = true) {
            this.sourceInsight = insight
            if (reset) this.sourcePage = 1
            const r = await listInsightSources(insight.id, this.sourcePage)
            this.sourceRows = r.data || []
            this.showSources = true
        },
        changeSourcePage(delta) {
            this.sourcePage += delta
            this.sources(this.sourceInsight, false)
        },
        async dismiss(insight) {
            await dismissInsight(insight.id)
            mSuccess('已忽略')
            await this.load()
        }
    }
}
</script>

<style lang="scss" scoped>
.report-actions {
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 12px;
    padding: 14px;
    margin-bottom: 12px;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);

    &__head {
        display: flex;
        align-items: center;
        justify-content: space-between;
    }

    &__title {
        margin: 0;
        flex: 1;
        min-width: 0;
    }

    &__tip {
        margin-top: 8px;
        font-size: 11px;
        color: var(--pnkx-info);
        background: var(--pnkx-info-soft);
        border-radius: 8px;
        padding: 6px 10px;
        line-height: 1.5;
    }
}

.insight {
    padding: 12px 0;
    border-bottom: 1px solid var(--pnkx-border-soft);

    &:last-of-type {
        border-bottom: none;
    }

    &__fact {
        font-size: 13px;
        color: var(--pnkx-text);
        line-height: 1.6;
    }

    &__hint {
        font-size: 11px;
        color: var(--pnkx-text-muted);
        margin-bottom: 6px;
    }

    &__candidates {
        margin-top: 8px;
    }

    &__review {
        margin-top: 6px;
        font-size: 12px;
        color: var(--pnkx-text-secondary);
        line-height: 1.5;
    }

    &__meta {
        margin-top: 6px;
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }

    &__ops {
        display: flex;
        gap: 8px;
        margin-top: 8px;
    }
}

.candidate {
    padding: 10px;
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 8px;
    margin-bottom: 8px;

    &__title {
        font-size: 13px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__basis {
        font-size: 12px;
        color: var(--pnkx-text-secondary);
        margin: 4px 0 8px;
        line-height: 1.5;
    }
}

.insights-empty__emoji {
    font-size: 36px;
    opacity: 0.6;
}

.action-form {
    padding: 16px 12px calc(16px + env(safe-area-inset-bottom));

    &__title {
        font-size: 16px;
        font-weight: 600;
        color: var(--pnkx-text);
        margin-bottom: 12px;
        text-align: center;
    }

    &__hint {
        font-size: 12px;
        color: var(--pnkx-text-muted);
        line-height: 1.5;
        margin: 10px 2px 0;
    }

    &__error {
        margin-top: 8px;
        font-size: 12px;
        color: var(--pnkx-warning);
    }

    &__ops {
        display: flex;
        gap: 10px;
        margin-top: 14px;

        .van-button {
            flex: 1;
        }
    }
}

.source-panel {
    padding: 16px 12px calc(16px + env(safe-area-inset-bottom));
    max-height: 60vh;
    overflow-y: auto;

    &__title {
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
        margin-bottom: 12px;
        text-align: center;
    }

    &__list {
        border: 1px solid var(--pnkx-border-soft);
        border-radius: 8px;
        overflow: hidden;
    }

    &__ops {
        display: flex;
        justify-content: center;
        gap: 10px;
        margin-top: 12px;
    }
}

.source-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 10px;
    font-size: 12px;
    border-bottom: 1px solid var(--pnkx-border-soft);

    &:last-child {
        border-bottom: none;
    }

    &__time {
        color: var(--pnkx-text-muted);
    }

    &__type {
        flex: 1;
        margin: 0 8px;
        color: var(--pnkx-text);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__money {
        color: var(--pnkx-danger);
    }
}
</style>
