<template>
  <view class="checkout-panel"
    ><view class="bar"
      ><text class="heading">本次采购</text
      ><text class="hint">{{ eligible.length }} 项已买到、待结算</text
      ><view class="bar-actions"
        ><button size="mini" :disabled="saving" @click="loadBatches">我的采购批次</button
        ><button size="mini" class="primary" :disabled="!eligible.length || saving" @click="open()">
          选择商品并结算
        </button></view
      ></view
    ><text v-if="success" class="success">{{ success }}</text>
    <view v-if="batchesOpen" class="batches"
      ><view class="section-head"
        ><text class="heading">我的采购批次</text
        ><button size="mini" @click="batchesOpen = false">收起</button></view
      ><text v-if="batchLoading" class="hint">正在加载…</text
      ><text v-if="batchError" class="error">{{ batchError }}</text
      ><text v-if="!batchLoading && !batches.length && !batchError" class="hint"
        >暂无已结算批次</text
      ><view v-for="b in batches" :key="b.id" class="batch"
        ><text>批次 #{{ b.id }} · {{ b.record_id ? "已记账" : "未记账" }}</text
        ><button v-if="!b.record_id" size="mini" :disabled="saving" @click="open(b.id)">
          补记支出
        </button></view
      ></view
    >
    <view v-if="visible" class="checkout"
      ><text class="heading">{{ batchId ? "补记采购支出" : "核对本次采购" }}</text
      ><text v-if="error" class="error">{{ error }}</text
      ><template v-if="!batchId"
        ><view class="section-head"
          ><text>已选 {{ selectedCount }} 项</text
          ><button size="mini" :disabled="saving" @click="toggleAll">
            {{ allSelected ? "取消全选" : "全选（最多200项）" }}
          </button></view
        ><text class="hint">仅显示已买到且未结算的商品，未选择的可下次结算。</text
        ><view class="items"
          ><view v-for="item in eligible" :key="item.id" class="item" @click="toggleItem(item.id)"
            ><checkbox
              :checked="form.purchasedItemIds.includes(item.id)"
              :disabled="saving"
              :aria-label="item.name"
            /><text class="item-name">{{ item.name }}</text
            ><text class="quantity">{{ item.quantity || "未填写数量" }}</text></view
          ></view
        ></template
      >
      <view class="section-head"
        ><view
          ><text class="heading">{{ batchId ? `采购批次 #${batchId}` : "同时记录支出" }}</text
          ><text class="hint">{{
            batchId ? "为已有批次补记一笔支出" : "可选，之后也可以补记"
          }}</text></view
        ><switch
          v-if="!batchId"
          :checked="form.recordExpense"
          :disabled="saving"
          @change="form.recordExpense = $event.detail.value"
      /></view>
      <template v-if="form.recordExpense"
        ><text v-if="financeLoading" class="hint">正在加载账户与分类…</text
        ><text v-if="financeError" class="error">{{ financeError }}</text
        ><button
          v-if="financeError"
          size="mini"
          :disabled="saving"
          :loading="financeLoading"
          @click="loadFinance"
        >
          重试加载</button
        ><view class="field"
          ><text>实际总额（元）</text
          ><input
            type="digit"
            v-model="form.totalAmount"
            :disabled="saving"
            placeholder="如 38.50" /></view
        ><view class="field"
          ><text>扣费日期</text
          ><picker
            mode="date"
            :value="form.paidAt"
            :disabled="saving"
            @change="form.paidAt = $event.detail.value"
            ><view>{{ form.paidAt }}　›</view></picker
          ></view
        ><view class="field"
          ><text>扣费账户</text
          ><picker
            :range="accounts"
            range-key="accountName"
            :disabled="saving"
            @change="selectAccount"
            ><view>{{ accountLabel }}　›</view></picker
          ></view
        ><view class="field"
          ><text>支出分类</text
          ><picker
            :range="categories"
            range-key="typeName"
            :disabled="saving"
            @change="selectCategory"
            ><view>{{ categoryLabel }}　›</view></picker
          ></view
        ><text class="hint">账户与支出归你本人，不向其他清单成员展示。</text></template
      >
      <view class="footer"
        ><text>{{ batchId ? "为已有采购补记支出" : `本次结算 ${selectedCount} 项` }}</text
        ><button
          class="primary"
          :disabled="saving || (!batchId && !selectedCount)"
          :loading="saving"
          @click="save"
        >
          {{ batchId ? "确认记账" : form.recordExpense ? "结算并记账" : "确认结算" }}</button
        ><button :disabled="saving" @click="close">返回</button></view
      ></view
    ></view
  >
</template>
<script>
import request from "@/utils/request";
import { listAccount } from "@/api/px/life/bookkeeping/account";
import { listClassification } from "@/api/px/life/bookkeeping/classification";
import { useShoppingCheckout } from "@/utils/useShoppingCheckout";
export default {
  props: {
    items: { type: Array, default: () => [] },
    listId: { type: [String, Number], default: null },
  },
  emits: ["changed"],
  setup(props, { emit, expose }) {
    const state = useShoppingCheckout(props, emit, { request, listAccount, listClassification });
    expose({
      // 供页面底部工具栏带页内预选打开结算核对
      openWith(ids) {
        return state.open(null, ids);
      },
    });
    return {
      ...state,
      selectAccount(e) {
        state.form.value.accountId = state.accounts.value[Number(e.detail.value)]?.id;
      },
      selectCategory(e) {
        state.form.value.classificationId = state.categories.value[Number(e.detail.value)]?.id;
      },
    };
  },
};
</script>
<style scoped>
.checkout-panel {
  margin: 24rpx 0;
  color: #1e293b;
}
.bar,
.checkout,
.batches {
  padding: 28rpx;
  border: 1px solid #e2e8f0;
  border-radius: 18rpx;
  background: #fff;
  margin: 16rpx 0;
}
.bar {
  background: #f8fafc;
}
.heading {
  font-size: 29rpx;
  font-weight: 600;
  display: block;
}
.hint {
  font-size: 24rpx;
  color: #475569;
  display: block;
  line-height: 1.65;
  margin: 12rpx 0;
}
.bar-actions,
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12rpx;
  flex-wrap: wrap;
  margin: 20rpx 0;
}
.bar-actions button,
.section-head button {
  margin: 0;
}
.primary {
  background: #0284c7;
  color: white;
}
.items {
  border: 1px solid #e2e8f0;
  border-radius: 12rpx;
  max-height: 500rpx;
  overflow: auto;
}
.item {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 22rpx 16rpx;
  border-bottom: 1px solid #e2e8f0;
  min-height: 70rpx;
}
.item:last-child {
  border: 0;
}
.item-name {
  flex: 1;
  font-size: 27rpx;
  word-break: break-word;
}
.quantity {
  color: #475569;
  font-size: 24rpx;
  max-width: 160rpx;
  word-break: break-word;
}
.field {
  margin: 22rpx 0;
  font-size: 26rpx;
}
.field input,
.field picker {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 20rpx;
  border-radius: 10rpx;
  margin-top: 12rpx;
}
.field input {
  height: 44rpx;
}
.error,
.success {
  display: block;
  padding: 18rpx;
  margin: 16rpx 0;
  font-size: 25rpx;
  line-height: 1.6;
  border-radius: 10rpx;
}
.error {
  background: #fff1f2;
  color: #9f1239;
}
.success {
  background: #f0fdf4;
  color: #166534;
}
.footer {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-top: 24rpx;
  font-size: 26rpx;
}
.footer button {
  width: 100%;
  margin: 0;
  font-size: 28rpx;
  min-height: 88rpx;
}
.batch {
  padding: 20rpx 0;
  border-bottom: 1px solid #e2e8f0;
  font-size: 26rpx;
}
.batch button {
  margin: 12rpx 0 0;
}
button::after {
  border-color: #e2e8f0;
}
</style>
