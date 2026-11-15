<template>
    <div class="charges">
        <div class="charges__bar">
            <van-dropdown-menu active-color="var(--pnkx-primary)">
                <van-dropdown-item v-model="filterStatus" :options="filterOptions" @change="onFilterChange" />
            </van-dropdown-menu>
            <van-button size="small" plain round icon="replay" @click="load">刷新</van-button>
        </div>

        <div v-if="error" class="charges__error">{{ error }}</div>

        <div v-for="row in rows" :key="row.id" class="charges__item">
            <div class="charges__item-head">
                <div class="charges__item-name">{{ row.sourceName }}</div>
                <van-tag :type="statusTagType(row.status)">{{ labels[row.status] || row.status }}</van-tag>
            </div>
            <div class="charges__item-line">
                <span>{{ row.dueDate }}</span>
                <span>预计 ¥{{ row.expectedAmount }}</span>
                <span v-if="row.actualAmount != null">实际 ¥{{ row.actualAmount }}</span>
            </div>
            <div class="charges__item-actions">
                <van-button size="small" plain round @click="inspect(row)">详情</van-button>
                <template v-if="['pending', 'payment_failed'].indexOf(row.status) >= 0">
                    <van-button size="small" type="primary" round @click="open(row)">确认入账</van-button>
                    <van-button size="small" plain round @click="askResolve(row, 'skip')">跳过本期</van-button>
                    <van-button size="small" plain round @click="askResolve(row, 'payment-failed')">扣费失败</van-button>
                </template>
                <span v-else class="charges__reason">{{ row.reason || '已处理' }}</span>
            </div>
        </div>

        <div v-if="!loading && !rows.length && !error" class="charges__empty">暂无账期，规则到期后生成</div>

        <div class="charges__pager">
            <van-button size="small" plain round :disabled="page === 1" @click="page--; load()">上一页</van-button>
            <span class="charges__page-num">第 {{ page }} 页</span>
            <van-button size="small" plain round :disabled="rows.length < 20" @click="page++; load()">下一页</van-button>
        </div>

        <!-- 详情弹层 -->
        <van-popup :show="!!detail" position="bottom" round teleport="body" @update:show="detail = null">
            <div v-if="detail" class="charges__detail">
                <div class="charges__detail-title">账期详情</div>
                <van-cell title="来源" :value="detail.instance.sourceName" />
                <van-cell title="到期日" :value="detail.instance.dueDate" />
                <van-cell title="状态" :value="labels[detail.instance.status] || detail.instance.status" />
                <van-cell v-if="detail.instance.recordId" title="关联账单" :value="String(detail.instance.recordId)" />
                <div class="charges__detail-sub">处理记录</div>
                <div v-for="(h, index) in detail.history" :key="index" class="charges__history">
                    {{ h.create_time }} · {{ historyLabel(h) }}
                </div>
                <div v-if="!(detail.history || []).length" class="charges__history">暂无处理记录</div>
                <div class="charges__detail-actions">
                    <van-button block round @click="detail = null">收起详情</van-button>
                </div>
            </div>
        </van-popup>

        <!-- 确认入账弹层 -->
        <van-popup :show="!!selected" position="bottom" round teleport="body" @update:show="selected = null">
            <div v-if="selected" class="charges__editor">
                <div class="charges__detail-title">确认实际扣费</div>
                <van-cell-group inset :border="false">
                    <van-field
                        v-model="form.actualAmount"
                        type="number"
                        label="实际金额"
                        placeholder="实际金额"
                    />
                    <van-field
                        :model-value="form.paidAt"
                        label="扣费日期"
                        placeholder="选择日期"
                        readonly
                        is-link
                        @click="showDatePicker = true"
                    />
                    <van-field
                        :model-value="accountName"
                        label="账户"
                        placeholder="请选择"
                        readonly
                        is-link
                        @click="showAccountPicker = true"
                    />
                    <van-field
                        v-if="selected.classificationId !== 0"
                        :model-value="categoryName"
                        label="分类"
                        placeholder="请选择"
                        readonly
                        is-link
                        @click="showCategoryPicker = true"
                    />
                </van-cell-group>
                <div class="charges__detail-actions">
                    <van-button block round plain @click="selected = null">取消</van-button>
                    <van-button block round type="primary" :loading="saving" @click="confirm">确认入账</van-button>
                </div>
            </div>
        </van-popup>

        <!-- 扣费日期选择 -->
        <van-popup :show="showDatePicker" position="bottom" round teleport="body" @update:show="showDatePicker = $event">
            <van-date-picker
                :model-value="datePickerValue"
                title="选择日期"
                @confirm="onDateConfirm"
                @cancel="showDatePicker = false"
            />
        </van-popup>

        <!-- 账户选择 -->
        <van-popup :show="showAccountPicker" position="bottom" round teleport="body" @update:show="showAccountPicker = $event">
            <van-picker
                title="选择账户"
                :columns="accountColumns"
                @confirm="onAccountConfirm"
                @cancel="showAccountPicker = false"
            />
        </van-popup>

        <!-- 分类选择 -->
        <van-popup :show="showCategoryPicker" position="bottom" round teleport="body" @update:show="showCategoryPicker = $event">
            <van-picker
                title="选择分类"
                :columns="categoryColumns"
                @confirm="onCategoryConfirm"
                @cancel="showCategoryPicker = false"
            />
        </van-popup>

        <!-- 跳过/扣费失败原因 -->
        <van-dialog
            v-model:show="showReason"
            title="填写本期处理原因"
            show-cancel-button
            @confirm="resolve"
        >
            <div class="charges__reason-input">
                <van-field v-model="reasonText" placeholder="请输入原因" />
            </div>
        </van-dialog>
    </div>
</template>

<script>
import {
    listChargeInstance,
    getChargeInstance,
    confirmChargeInstance,
    resolveChargeInstance
} from '@/api/px/life/bookkeeping/chargeInstance'
import { listAccount } from '@/api/px/life/bookkeeping/account'
import { listClassification } from '@/api/px/life/bookkeeping/classification'
import { showToast } from '@/mobile/utils/ui'

const genKey = () => `charge_${Date.now()}_${Math.random().toString(36).slice(2)}`

export default {
    name: 'BkChargeInstances',
    props: {
        sourceType: { type: String, default: null }
    },
    data() {
        return {
            detail: null,
            filterStatus: '',
            rows: [],
            page: 1,
            loading: false,
            error: '',
            saving: false,
            selected: null,
            form: {},
            accounts: [],
            categories: [],
            showDatePicker: false,
            showAccountPicker: false,
            showCategoryPicker: false,
            showReason: false,
            reasonText: '',
            resolveTarget: null,
            resolveAction: '',
            filterOptions: [
                { text: '全部账期', value: '' },
                { text: '待确认', value: 'pending' },
                { text: '扣费失败', value: 'payment_failed' },
                { text: '已入账', value: 'posted' }
            ],
            labels: {
                pending: '待确认',
                posted: '已入账',
                voided: '账单已作废',
                payment_failed: '扣费失败',
                skipped: '已跳过',
                cancelled: '已取消'
            }
        }
    },
    computed: {
        accountName() {
            const a = this.accounts.find(x => x.id === this.form.accountId)
            return a ? a.accountName : '请选择'
        },
        categoryName() {
            const c = this.categories.find(x => x.id === this.form.classificationId)
            return c ? c.typeName : '请选择'
        },
        accountColumns() {
            return this.accounts.map(a => ({ text: a.accountName, value: a.id }))
        },
        categoryColumns() {
            return this.categories.map(c => ({ text: c.typeName, value: c.id }))
        },
        datePickerValue() {
            const d = this.form.paidAt ? new Date(this.form.paidAt) : new Date()
            return [String(d.getFullYear()), String(d.getMonth() + 1).padStart(2, '0'), String(d.getDate()).padStart(2, '0')]
        }
    },
    mounted() {
        this.load()
    },
    methods: {
        statusTagType(status) {
            if (status === 'posted') return 'success'
            if (status === 'payment_failed') return 'danger'
            if (status === 'pending') return 'warning'
            return 'default'
        },
        historyLabel(h) {
            try {
                const r = JSON.parse(h.result_json)
                return (this.labels[r.status] || r.status) + (r.reason ? ' · ' + r.reason : '')
            } catch (e) {
                return '处理记录'
            }
        },
        onFilterChange() {
            this.page = 1
            this.load()
        },
        async load() {
            this.loading = true
            this.error = ''
            try {
                const r = await listChargeInstance({
                    pageNum: this.page,
                    pageSize: 20,
                    sourceType: this.sourceType,
                    status: this.filterStatus || undefined
                })
                this.rows = r.data || []
            } catch (e) {
                this.error = '账期加载失败，请联网后重试'
            } finally {
                this.loading = false
            }
        },
        async inspect(row) {
            const r = await getChargeInstance(row.id)
            this.detail = r.data
        },
        async open(row) {
            this.selected = row
            this.form = {
                requestId: genKey(),
                expectedRevision: row.revision,
                actualAmount: String(row.expectedAmount),
                paidAt: row.dueDate,
                accountId: row.accountId,
                classificationId: row.classificationId
            }
            try {
                const [a, c] = await Promise.all([
                    listAccount({ pageSize: 100 }),
                    listClassification({ pageSize: 100 })
                ])
                this.accounts = a.rows || []
                this.categories = c.rows || []
            } catch (e) {
                this.error = '账户或分类加载失败，请重试'
            }
        },
        onDateConfirm({ selectedValues }) {
            this.form.paidAt = selectedValues.join('-')
            this.showDatePicker = false
        },
        onAccountConfirm({ selectedOptions }) {
            if (selectedOptions[0]) this.form.accountId = selectedOptions[0].value
            this.showAccountPicker = false
        },
        onCategoryConfirm({ selectedOptions }) {
            if (selectedOptions[0]) this.form.classificationId = selectedOptions[0].value
            this.showCategoryPicker = false
        },
        async confirm() {
            if (this.saving) return
            this.saving = true
            try {
                await confirmChargeInstance(this.selected.id, this.form)
                this.selected = null
                showToast('已入账')
                await this.load()
            } finally {
                this.saving = false
            }
        },
        askResolve(row, action) {
            this.resolveTarget = row
            this.resolveAction = action
            this.reasonText = ''
            this.showReason = true
        },
        async resolve() {
            const row = this.resolveTarget
            if (!row || !this.reasonText) return
            try {
                await resolveChargeInstance(row.id, this.resolveAction, {
                    requestId: genKey(),
                    expectedRevision: row.revision,
                    reason: this.reasonText
                })
                await this.load()
            } catch (e) {
                this.error = '处理失败，请刷新后重试'
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.charges {
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 12px;
    padding: 12px;
    margin-bottom: 12px;

    &__bar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 8px;

        :deep(.van-dropdown-menu__bar) {
            box-shadow: none;
            background: transparent;
            border-radius: 8px;
        }
    }

    &__error {
        font-size: 12px;
        color: var(--pnkx-danger);
        padding: 6px 0;
    }

    &__item {
        padding: 12px 0;
        border-bottom: 1px solid var(--pnkx-border-soft);

        &:last-of-type {
            border-bottom: none;
        }
    }

    &__item-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
    }

    &__item-name {
        font-size: 14px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__item-line {
        display: flex;
        flex-wrap: wrap;
        gap: 4px 12px;
        margin-top: 4px;
        font-size: 12px;
        color: var(--pnkx-text-secondary);
    }

    &__item-actions {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 6px;
        margin-top: 8px;
    }

    &__reason {
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }

    &__empty {
        padding: 20px 0;
        text-align: center;
        font-size: 13px;
        color: var(--pnkx-text-muted);
    }

    &__pager {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
        margin-top: 8px;
    }

    &__page-num {
        font-size: 12px;
        color: var(--pnkx-text-secondary);
    }

    &__detail,
    &__editor {
        padding: 16px 0 calc(16px + env(safe-area-inset-bottom));
        max-height: 75vh;
        overflow-y: auto;
    }

    &__detail-title {
        padding: 0 16px 10px;
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__detail-sub {
        padding: 10px 16px 4px;
        font-size: 13px;
        font-weight: 500;
        color: var(--pnkx-text-secondary);
    }

    &__history {
        padding: 4px 16px;
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }

    &__detail-actions {
        display: flex;
        gap: 10px;
        padding: 12px 16px 0;
    }

    &__reason-input {
        padding: 8px 4px;
    }
}
</style>
