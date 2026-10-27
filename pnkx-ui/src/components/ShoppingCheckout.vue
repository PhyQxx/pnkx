<template>
    <section class="checkout-panel">
        <div class="checkout-bar">
            <div>
                <strong>本次采购</strong>
                <p>{{ eligible.length }} 项已买到、待结算</p>
            </div>
            <div class="bar-actions">
                <el-button :disabled="saving" @click="loadBatches">我的采购批次</el-button
                ><el-button type="primary" :disabled="!eligible.length || saving" @click="open()"
                    >选择商品并结算</el-button
                >
            </div>
        </div>
        <el-alert v-if="success" :title="success" type="success" show-icon :closable="false" />
        <div v-if="batchesOpen" class="batches">
            <div class="batch-head">
                <strong>我的采购批次</strong><el-button text @click="batchesOpen = false">收起</el-button>
            </div>
            <p v-if="batchLoading">正在加载…</p>
            <el-alert v-if="batchError" :title="batchError" type="error" :closable="false" />
            <p v-if="!batchLoading && !batches.length && !batchError">暂无已结算批次</p>
            <div v-for="b in batches" :key="b.id" class="batch">
                <span>批次 #{{ b.id }} · {{ b.create_time }}</span
                ><el-tag :type="b.record_id ? 'success' : 'info'">{{ b.record_id ? '已记账' : '未记账' }}</el-tag
                ><el-button v-if="!b.record_id" link type="primary" :disabled="saving" @click="open(b.id)"
                    >补记支出</el-button
                >
            </div>
        </div>
        <el-dialog
            v-model="visible"
            :title="batchId ? '补记采购支出' : '核对本次采购'"
            width="min(620px,94vw)"
            top="6vh"
            append-to-body
            :close-on-click-modal="!saving"
            :close-on-press-escape="!saving"
            :show-close="!saving"
        >
            <div class="checkout-body">
                <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />
                <template v-if="!batchId"
                    ><div class="selection-head">
                        <strong>已选 {{ selectedCount }} 项</strong
                        ><el-button link type="primary" :disabled="saving" @click="toggleAll">{{
                            allSelected ? '取消全选' : '全选（最多200项）'
                        }}</el-button>
                    </div>
                    <p class="hint">只显示已买到且未结算的商品。未勾选的商品可下次结算。</p>
                    <div class="items">
                        <label v-for="item in eligible" :key="item.id" class="item"
                            ><el-checkbox
                                :model-value="form.purchasedItemIds.includes(item.id)"
                                :disabled="saving"
                                @change="toggleItem(item.id)"
                                >{{ item.name }}</el-checkbox
                            ><span>{{ item.quantity || '未填写数量' }}</span></label
                        >
                    </div></template
                >
                <div class="expense-heading">
                    <div>
                        <strong>{{ batchId ? `采购批次 #${batchId}` : '同时记录支出' }}</strong>
                        <p class="hint">
                            {{ batchId ? '为已有批次补记一笔支出' : '可选；不记账也能完成结算，之后可补记' }}
                        </p>
                    </div>
                    <el-switch
                        v-if="!batchId"
                        v-model="form.recordExpense"
                        :disabled="saving"
                        aria-label="同时记录支出"
                    />
                </div>
                <template v-if="form.recordExpense"
                    ><el-alert v-if="financeError" :title="financeError" type="warning" :closable="false" /><el-button
                        v-if="financeError"
                        :loading="financeLoading"
                        @click="loadFinance"
                        >重试加载账户与分类</el-button
                    >
                    <p v-if="financeLoading" class="hint">正在加载账户与分类…</p>
                    <el-form label-position="top" :disabled="saving"
                        ><div class="finance-grid">
                            <el-form-item label="实际总额（元）"
                                ><el-input
                                    v-model="form.totalAmount"
                                    inputmode="decimal"
                                    placeholder="如 38.50" /></el-form-item
                            ><el-form-item label="扣费日期"
                                ><el-date-picker
                                    v-model="form.paidAt"
                                    type="date"
                                    value-format="YYYY-MM-DD"
                                    style="width: 100%" /></el-form-item
                            ><el-form-item label="扣费账户"
                                ><el-select v-model="form.accountId" placeholder="请选择账户" style="width: 100%"
                                    ><el-option
                                        v-for="a in accounts"
                                        :key="a.id"
                                        :value="a.id"
                                        :label="a.accountName" /></el-select></el-form-item
                            ><el-form-item label="支出分类"
                                ><el-select
                                    v-model="form.classificationId"
                                    placeholder="请选择支出分类"
                                    style="width: 100%"
                                    ><el-option
                                        v-for="c in categories"
                                        :key="c.id"
                                        :value="c.id"
                                        :label="c.typeName" /></el-select
                            ></el-form-item></div
                    ></el-form>
                    <p class="hint">账户与支出记录归你本人，不向其他清单成员展示。</p></template
                >
            </div>
            <template #footer
                ><div class="checkout-footer">
                    <span>{{ batchId ? '为已有采购补记支出' : `本次结算 ${selectedCount} 项` }}</span
                    ><el-button :disabled="saving" @click="close">返回</el-button
                    ><el-button type="primary" :disabled="!batchId && !selectedCount" :loading="saving" @click="save">{{
                        batchId ? '确认记账' : form.recordExpense ? '结算并记账' : '确认结算'
                    }}</el-button>
                </div></template
            ></el-dialog
        >
    </section>
</template>
<script setup>
import request from '@/utils/request';
import { listAccount } from '@/api/px/life/bookkeeping/account';
import { listClassification } from '@/api/px/life/bookkeeping/classification';
import { useShoppingCheckout } from '../utils/useShoppingCheckout';
const props = defineProps({
        items: { type: Array, default: () => [] },
        listId: { type: [Number, String], default: null }
    }),
    emit = defineEmits(['changed']);
const {
    visible,
    saving,
    batchId,
    batches,
    batchesOpen,
    batchLoading,
    batchError,
    form,
    accounts,
    categories,
    financeLoading,
    financeError,
    error,
    success,
    eligible,
    selectedCount,
    allSelected,
    loadBatches,
    loadFinance,
    open,
    close,
    toggleAll,
    toggleItem,
    save
} = useShoppingCheckout(props, emit, { request, listAccount, listClassification });
// 供页面底部工具栏带页内预选打开结算核对
function openWith(ids) {
    return open(null, ids);
}
defineExpose({ openWith });
</script>
<style scoped>
.checkout-panel {
    margin: 16px 0;
}
.checkout-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
    padding: 18px 20px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 12px;
    background: var(--el-fill-color-lighter);
}
.checkout-bar p {
    margin: 6px 0 0;
    font-size: 13px;
    color: var(--el-text-color-regular);
}
.bar-actions {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
}
.bar-actions :deep(.el-button) {
    margin: 0;
}
.batches {
    padding: 16px;
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 10px;
    margin-top: 12px;
}
.batch-head,
.batch,
.selection-head,
.expense-heading {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
}
.batch {
    padding: 12px 0;
    border-top: 1px solid var(--el-border-color-lighter);
    flex-wrap: wrap;
    font-size: 13px;
}
.checkout-body {
    max-height: 65vh;
    overflow: auto;
    padding-right: 5px;
}
.hint {
    color: var(--el-text-color-regular);
    font-size: 13px;
    line-height: 1.65;
}
.selection-head {
    margin-top: 12px;
}
.items {
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 10px;
    max-height: 240px;
    overflow: auto;
}
.item {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    padding: 12px 16px;
    border-bottom: 1px solid var(--el-border-color-lighter);
}
.item:last-child {
    border: 0;
}
.item span {
    font-size: 13px;
    color: var(--el-text-color-regular);
    flex-shrink: 0;
}
.item :deep(.el-checkbox) {
    height: auto;
    min-height: 24px;
    min-width: 0;
}
.item :deep(.el-checkbox__label) {
    white-space: normal;
    overflow-wrap: anywhere;
}
.expense-heading {
    padding: 18px 0;
    margin-top: 8px;
}
.expense-heading p {
    margin: 6px 0 0;
}
.finance-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0 16px;
}
.checkout-footer {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 8px;
    flex-wrap: wrap;
}
.checkout-footer > span {
    margin-right: auto;
    color: var(--el-text-color-regular);
    font-size: 13px;
}
.checkout-footer :deep(.el-button) {
    margin: 0;
}
@media (max-width: 600px) {
    .checkout-bar {
        align-items: flex-start;
        flex-direction: column;
    }
    .finance-grid {
        grid-template-columns: 1fr;
    }
    .item {
        padding: 12px;
    }
    .checkout-footer :deep(.el-button) {
        min-height: 40px;
    }
}
</style>
