<template>
  <view
    ><button size="mini" :disabled="!eligible.length" @click="open()">
      结束采购（{{ eligible.length }} 项）</button
    ><button size="mini" @click="loadBatches">我的采购批次</button
    ><view v-for="b in batches" :key="b.id"
      ><text>批次 {{ b.id }} · {{ b.record_id ? "已记账" : "未记账" }}</text
      ><button v-if="!b.record_id" size="mini" @click="open(b.id)">补记支出</button></view
    ><view v-if="visible" class="checkout"
      ><text>{{ batchId ? "为采购批次补记支出" : "只结算已买到且尚未结算的条目" }}</text
      ><view v-if="!batchId"
        ><text>同时记一笔支出</text
        ><switch
          :checked="form.recordExpense"
          @change="form.recordExpense = $event.detail.value" /></view
      ><view v-if="form.recordExpense"
        ><input type="digit" v-model="form.totalAmount" placeholder="实际总额" /><picker
          mode="date"
          @change="form.paidAt = $event.detail.value"
          ><view>扣费日期 {{ form.paidAt }}</view></picker
        ><picker :range="accounts" range-key="accountName" @change="selectAccount"
          ><view>账户 {{ accountLabel }}</view></picker
        ><picker :range="categories" range-key="typeName" @change="selectCategory"
          ><view>分类 {{ categoryLabel }}</view></picker
        ></view
      ><button :disabled="saving" :loading="saving" @click="save">确认结算</button
      ><button @click="visible = false">取消</button></view
    ></view
  >
</template>
<script>
import request from "@/utils/request";
import { listAccount } from "@/api/px/life/bookkeeping/account";
import { listClassification } from "@/api/px/life/bookkeeping/classification";
export default {
  props: {
    items: { type: Array, default: () => [] },
    listId: { type: [String, Number], default: null },
  },
  data() {
    return {
      batchId: null,
      batches: [],
      visible: false,
      saving: false,
      form: {},
      accounts: [],
      categories: [],
      accountLabel: "请选择",
      categoryLabel: "请选择",
    };
  },
  computed: {
    eligible() {
      return this.items.filter((i) => i.fulfillmentState === "purchased" && !i.batchId);
    },
  },
  methods: {
    async loadBatches() {
      const r = await request({ url: `/shoppingList/${this.listId}/batches` });
      this.batches = r.data || [];
    },
    async open(id = null) {
      this.batchId = id;
      const d = new Date();
      this.form = {
        requestId: `purchase_${Date.now()}_${Math.random().toString(36).slice(2)}`,
        purchasedItemIds: this.eligible.map((i) => i.id),
        recordExpense: !!id,
        accountId: null,
        classificationId: null,
        totalAmount: "",
        paidAt: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(
          d.getDate()
        ).padStart(2, "0")}`,
      };
      this.visible = true;
      const [a, c] = await Promise.all([
        listAccount({ pageSize: 100 }),
        listClassification({ pageSize: 100, typeDifference: "1" }),
      ]);
      this.accounts = a.rows || [];
      this.categories = c.rows || [];
    },
    selectAccount(e) {
      const a = this.accounts[e.detail.value];
      this.form.accountId = a.id;
      this.accountLabel = a.accountName;
    },
    selectCategory(e) {
      const c = this.categories[e.detail.value];
      this.form.classificationId = c.id;
      this.categoryLabel = c.typeName;
    },
    async save() {
      if (this.saving) return;
      this.saving = true;
      try {
        await request({
          url: this.batchId
            ? `/shopping-batches/${this.batchId}/record-expense`
            : `/shoppingList/${this.listId}/checkout`,
          method: "post",
          data: this.form,
        });
        this.visible = false;
        await this.loadBatches();
        this.$emit("changed");
      } finally {
        this.saving = false;
      }
    },
  },
};
</script>
<style scoped>
.checkout {
  padding: 24rpx;
  border: 1px solid #ccd;
  background: white;
  color: #253045;
}
.checkout input,
.checkout picker {
  padding: 20rpx 0;
}
</style>
