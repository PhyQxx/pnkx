<template>
    <div class="checkout-panel">
        <!-- 采购入口 -->
        <div class="checkout-panel__bar">
            <div class="checkout-panel__heading">本次采购</div>
            <div class="checkout-panel__hint">{{ eligible.length }} 项已买到、待结算</div>
            <div class="checkout-panel__bar-actions">
                <van-button size="small" plain round :disabled="saving" @click="loadBatches">我的采购批次</van-button>
                <van-button
                    size="small"
                    type="primary"
                    round
                    :disabled="!eligible.length || saving"
                    @click="open()"
                >
                    选择商品并结算
                </van-button>
            </div>
        </div>

        <div v-if="success" class="checkout-panel__success">{{ success }}</div>

        <!-- 我的采购批次 -->
        <div v-if="batchesOpen" class="checkout-panel__section">
            <div class="checkout-panel__section-head">
                <span class="checkout-panel__heading">我的采购批次</span>
                <van-button size="small" plain round @click="batchesOpen = false">收起</van-button>
            </div>
            <div v-if="batchLoading" class="checkout-panel__hint">正在加载…</div>
            <div v-if="batchError" class="checkout-panel__error">{{ batchError }}</div>
            <div v-if="!batchLoading && !batches.length && !batchError" class="checkout-panel__hint">暂无已结算批次</div>
            <div v-for="b in batches" :key="b.id" class="checkout-panel__batch">
                <span>批次 #{{ b.id }} · {{ b.record_id ? '已记账' : '未记账' }}</span>
                <van-button
                    v-if="!b.record_id"
                    size="small"
                    type="primary"
                    round
                    :disabled="saving"
                    @click="open(b.id)"
                >
                    补记支出
                </van-button>
            </div>
        </div>

        <!-- 结算核对弹层 -->
        <van-popup
            :show="visible"
            position="bottom"
            round
            teleport="body"
            :close-on-click-overlay="!saving"
            @update:show="onPopupShow"
        >
            <div class="checkout">
                <div class="checkout__title">{{ batchId ? '补记采购支出' : '核对本次采购' }}</div>
                <div v-if="error" class="checkout-panel__error">{{ error }}</div>

                <!-- 商品多选（补记支出时不需要） -->
                <template v-if="!batchId">
                    <div class="checkout__section-head">
                        <span>已选 {{ selectedCount }} 项</span>
                        <van-button size="small" plain round :disabled="saving" @click="toggleAll">
                            {{ allSelected ? '取消全选' : '全选（最多200项）' }}
                        </van-button>
                    </div>
                    <div class="checkout__hint">仅显示已买到且未结算的商品，未选择的可下次结算。</div>
                    <div class="checkout__items">
                        <div
                            v-for="item in eligible"
                            :key="item.id"
                            class="checkout__item"
                            @click="toggleItem(item.id)"
                        >
                            <van-checkbox
                                :model-value="form.purchasedItemIds.includes(item.id)"
                                :disabled="saving"
                                shape="square"
                            />
                            <span class="checkout__item-name">{{ item.name }}</span>
                            <span class="checkout__item-qty">{{ item.quantity || '未填写数量' }}</span>
                        </div>
                    </div>
                </template>

                <!-- 同时记录支出 -->
                <div class="checkout__section-head">
                    <div>
                        <div class="checkout-panel__heading">{{ batchId ? `采购批次 #${batchId}` : '同时记录支出' }}</div>
                        <div class="checkout__hint">{{ batchId ? '为已有批次补记一笔支出' : '可选，之后也可以补记' }}</div>
                    </div>
                    <van-switch
                        v-if="!batchId"
                        :model-value="form.recordExpense"
                        :disabled="saving"
                        size="20px"
                        @update:model-value="form.recordExpense = $event"
                    />
                </div>

                <template v-if="form.recordExpense">
                    <div v-if="financeLoading" class="checkout__hint">正在加载账户与分类…</div>
                    <div v-if="financeError" class="checkout-panel__error">{{ financeError }}</div>
                    <van-button
                        v-if="financeError"
                        size="small"
                        plain
                        round
                        :loading="financeLoading"
                        class="checkout__retry"
                        @click="loadFinance"
                    >
                        重试加载
                    </van-button>
                    <van-cell-group inset :border="false">
                        <van-field
                            v-model="form.totalAmount"
                            type="number"
                            label="实际总额"
                            placeholder="如 38.50（元）"
                            :disabled="saving"
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
                            :model-value="accountLabel"
                            label="扣费账户"
                            placeholder="请选择"
                            readonly
                            is-link
                            @click="showAccountPicker = true"
                        />
                        <van-field
                            :model-value="categoryLabel"
                            label="支出分类"
                            placeholder="请选择"
                            readonly
                            is-link
                            @click="showCategoryPicker = true"
                        />
                    </van-cell-group>
                    <div class="checkout__hint">账户与支出归你本人，不向其他清单成员展示。</div>
                </template>

                <div class="checkout__footer">
                    <div class="checkout__hint">
                        {{ batchId ? '为已有采购补记支出' : `本次结算 ${selectedCount} 项` }}
                    </div>
                    <div class="checkout__footer-actions">
                        <van-button block round plain :disabled="saving" @click="close">返回</van-button>
                        <van-button
                            block
                            round
                            type="primary"
                            :disabled="saving || (!batchId && !selectedCount)"
                            :loading="saving"
                            @click="save"
                        >
                            {{ batchId ? '确认记账' : (form.recordExpense ? '结算并记账' : '确认结算') }}
                        </van-button>
                    </div>
                </div>
            </div>
        </van-popup>

        <!-- 扣费日期选择 -->
        <van-popup :show="showDatePicker" position="bottom" round teleport="body" @update:show="showDatePicker = $event">
            <van-date-picker
                :model-value="paidAtParts"
                title="选择扣费日期"
                @confirm="onDateConfirm"
                @cancel="showDatePicker = false"
            />
        </van-popup>

        <!-- 账户选择 -->
        <van-popup :show="showAccountPicker" position="bottom" round teleport="body" @update:show="showAccountPicker = $event">
            <van-picker
                title="选择扣费账户"
                :columns="accountColumns"
                @confirm="onAccountConfirm"
                @cancel="showAccountPicker = false"
            />
        </van-popup>

        <!-- 支出分类选择 -->
        <van-popup :show="showCategoryPicker" position="bottom" round teleport="body" @update:show="showCategoryPicker = $event">
            <van-picker
                title="选择支出分类"
                :columns="categoryColumns"
                @confirm="onCategoryConfirm"
                @cancel="showCategoryPicker = false"
            />
        </van-popup>
    </div>
</template>

<script>
import {
    listShoppingBatches,
    checkoutShoppingList,
    recordShoppingBatchExpense
} from '@/api/px/life/shopping'
import { listAccount } from '@/api/px/life/bookkeeping/account'
import { listClassification } from '@/api/px/life/bookkeeping/classification'
import { eligibleItems, businessToday, checkoutError, checkoutPayload } from '@/utils/checkoutModel'

const genKey = () => `purchase_${Date.now()}_${Math.random().toString(36).slice(2)}`
const pad = value => String(value).padStart(2, '0')

export default {
    name: 'MShoppingCheckout',
    props: {
        items: { type: Array, default: () => [] },
        listId: { type: [String, Number], default: null }
    },
    emits: ['changed'],
    data() {
        return {
            visible: false,
            saving: false,
            batchId: null,
            batches: [],
            batchesOpen: false,
            batchLoading: false,
            batchError: '',
            form: { purchasedItemIds: [] },
            accounts: [],
            categories: [],
            financeLoading: false,
            financeError: '',
            error: '',
            success: '',
            // 幂等键缓存：同内容重试复用同一 requestId
            attempt: null,
            showDatePicker: false,
            showAccountPicker: false,
            showCategoryPicker: false,
            batchRequest: 0,
            financeRequest: 0,
            generation: 0
        }
    },
    computed: {
        eligible() {
            return eligibleItems(this.items)
        },
        selectedCount() {
            return (this.form.purchasedItemIds || []).length
        },
        allSelected() {
            return this.eligible.length > 0 && this.eligible.every(i => this.form.purchasedItemIds.includes(i.id))
        },
        accountLabel() {
            const account = this.accounts.find(a => a.id === this.form.accountId)
            return account ? account.accountName : '请选择'
        },
        categoryLabel() {
            const category = this.categories.find(c => c.id === this.form.classificationId)
            return category ? category.typeName : '请选择'
        },
        accountColumns() {
            return this.accounts.map(a => ({ text: a.accountName, value: a.id }))
        },
        categoryColumns() {
            return this.categories.map(c => ({ text: c.typeName, value: c.id }))
        },
        paidAtParts() {
            const value = this.form.paidAt || businessToday()
            const [year, month, day] = value.split('-')
            return [year, pad(Number(month)), pad(Number(day))]
        }
    },
    watch: {
        // 打开同时记账时加载账户与支出分类
        'form.recordExpense'(active) {
            if (active) this.loadFinance()
        },
        form: {
            deep: true,
            handler() {
                this.error = ''
            }
        },
        listId() {
            this.reset()
        }
    },
    beforeUnmount() {
        this.generation++
        this.batchRequest++
        this.financeRequest++
    },
    methods: {
        reset() {
            this.generation++
            this.batchRequest++
            this.financeRequest++
            this.visible = false
            this.batches = []
            this.batchesOpen = false
            this.accounts = []
            this.categories = []
            this.error = ''
            this.success = ''
            this.batchError = ''
            this.financeError = ''
            this.financeLoading = false
            this.batchLoading = false
        },
        async loadBatches() {
            this.batchesOpen = true
            const token = ++this.batchRequest
            this.batchLoading = true
            this.batchError = ''
            try {
                const r = await listShoppingBatches(this.listId)
                if (token === this.batchRequest) this.batches = r.data || []
            } catch (e) {
                if (token === this.batchRequest) this.batchError = '采购批次加载失败，请重试'
            } finally {
                if (token === this.batchRequest) this.batchLoading = false
            }
        },
        async loadFinance() {
            const token = ++this.financeRequest
            this.financeLoading = true
            this.financeError = ''
            try {
                const [a, c] = await Promise.all([
                    listAccount({ pageSize: 100 }),
                    listClassification({ pageSize: 100, typeDifference: '1' })
                ])
                if (token !== this.financeRequest) return
                this.accounts = a.rows || []
                this.categories = c.rows || []
                if (!this.accounts.length || !this.categories.length) {
                    this.financeError = '请先在记账模块创建账户和支出分类，或关闭同时记账'
                }
            } catch (e) {
                if (token === this.financeRequest) this.financeError = '账户或分类加载失败，可以重试或关闭同时记账'
            } finally {
                if (token === this.financeRequest) this.financeLoading = false
            }
        },
        open(id = null, preselected = null) {
            if (this.saving) return
            this.generation++
            this.batchId = id
            this.error = ''
            this.success = ''
            this.financeError = ''
            const eligibleIds = this.eligible.slice(0, 200).map(i => i.id)
            // 页内预选：仅接受当前仍"已买到且未结算"的条目，非法或超量部分忽略
            let initial = eligibleIds
            if (!id && Array.isArray(preselected) && preselected.length) {
                const allowed = new Set(eligibleIds.map(String))
                initial = preselected.map(String).filter(v => allowed.has(v)).map(Number)
            }
            this.form = {
                purchasedItemIds: initial,
                recordExpense: !!id,
                totalAmount: '',
                paidAt: businessToday(),
                accountId: null,
                classificationId: null
            }
            this.attempt = null
            this.visible = true
        },
        // 供页面底部工具栏带页内预选打开结算核对
        openWith(ids) {
            return this.open(null, ids)
        },
        onPopupShow(value) {
            if (!value && !this.saving) this.close()
        },
        close() {
            if (!this.saving) {
                this.visible = false
                this.generation++
                this.financeRequest++
            }
        },
        toggleAll() {
            if (this.saving) return
            this.form.purchasedItemIds = this.allSelected
                ? []
                : this.eligible.slice(0, 200).map(i => i.id)
        },
        toggleItem(id) {
            if (this.saving) return
            const selected = this.form.purchasedItemIds
            this.form.purchasedItemIds = selected.includes(id)
                ? selected.filter(x => x !== id)
                : [...selected, id]
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
        async save() {
            if (this.saving) return
            this.error = checkoutError(this.form, this.batchId, this.items)
            if (this.error) return
            if (this.form.recordExpense && (this.financeLoading || this.financeError)) {
                this.error = '账户和分类尚未就绪，请先重试加载'
                return
            }
            const payload = checkoutPayload(this.form, this.batchId)
            // 同内容重试复用同一幂等键
            const fingerprint = JSON.stringify(payload)
            if (!this.attempt || this.attempt.fingerprint !== fingerprint) {
                this.attempt = { fingerprint, requestId: genKey() }
            }
            payload.requestId = this.attempt.requestId
            const token = this.generation
            this.saving = true
            try {
                if (this.batchId) {
                    await recordShoppingBatchExpense(this.batchId, payload)
                } else {
                    await checkoutShoppingList(this.listId, payload)
                }
                if (token !== this.generation) return
                this.visible = false
                this.success = this.batchId ? '本批次支出已记录' : '本次采购已结算，未选择的商品保持原状'
                this.$emit('changed')
                await this.loadBatches()
            } catch (e) {
                if (token === this.generation) {
                    this.error = (e && e.message) || '提交未确认成功，请保留当前内容重试，重复请求不会重复结算'
                }
            } finally {
                this.saving = false
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.checkout-panel {
    margin: 12px 0;

    &__bar,
    &__section {
        background: var(--pnkx-surface);
        border: 1px solid var(--pnkx-border-soft);
        border-radius: 12px;
        padding: 14px;
        margin-bottom: 10px;
    }

    &__bar {
        background: var(--pnkx-surface-muted);
    }

    &__heading {
        font-size: 14px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__hint {
        font-size: 12px;
        color: var(--pnkx-text-secondary);
        line-height: 1.65;
        margin: 6px 0;
    }

    &__bar-actions,
    &__section-head {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;
        margin-top: 10px;
    }

    &__section-head {
        margin-top: 4px;
    }

    &__success,
    &__error {
        padding: 10px 12px;
        margin-bottom: 10px;
        font-size: 12px;
        line-height: 1.6;
        border-radius: 8px;
    }

    &__success {
        background: var(--pnkx-success-soft);
        color: var(--pnkx-success);
    }

    &__error {
        background: var(--pnkx-danger-soft);
        color: var(--pnkx-danger);
    }

    &__batch {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 8px;
        padding: 10px 0;
        border-bottom: 1px solid var(--pnkx-border-soft);
        font-size: 13px;
        color: var(--pnkx-text);

        &:last-child {
            border-bottom: none;
        }
    }
}

.checkout {
    max-height: 82vh;
    overflow-y: auto;
    padding: 16px 0 calc(16px + env(safe-area-inset-bottom));

    &__title {
        padding: 0 16px 10px;
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__section-head {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;
        padding: 12px 16px;
    }

    &__hint {
        padding: 0 16px;
        font-size: 12px;
        color: var(--pnkx-text-secondary);
        line-height: 1.65;
    }

    &__items {
        margin: 8px 16px;
        border: 1px solid var(--pnkx-border-soft);
        border-radius: 8px;
        max-height: 240px;
        overflow-y: auto;
    }

    &__item {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 10px 12px;
        border-bottom: 1px solid var(--pnkx-border-soft);
        min-height: 40px;

        &:last-child {
            border-bottom: none;
        }
    }

    &__item-name {
        flex: 1;
        font-size: 13px;
        color: var(--pnkx-text);
        word-break: break-word;
    }

    &__item-qty {
        color: var(--pnkx-text-secondary);
        font-size: 12px;
        max-width: 90px;
        word-break: break-word;
    }

    &__retry {
        margin: 8px 16px;
    }

    &__footer {
        margin-top: 12px;
    }

    &__footer-actions {
        display: flex;
        gap: 10px;
        padding: 10px 16px 0;

        .van-button {
            flex: 1;
        }
    }
}
</style>
