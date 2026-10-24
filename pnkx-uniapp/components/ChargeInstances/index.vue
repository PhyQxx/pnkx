<template>
  <view class="charges">
    <picker
      :range="['全部账期', '待确认', '扣费失败', '已入账']"
      @change="
        filterStatus = ['', 'pending', 'payment_failed', 'posted'][Number($event.detail.value)];
        page = 1;
        load();
      "
      ><view>筛选账期：{{ labels[filterStatus] || "全部" }} ▾</view></picker
    >
    <view v-if="detail" class="charge"
      ><text
        >{{ detail.instance.sourceName }} · {{ detail.instance.dueDate }} ·
        {{ labels[detail.instance.status] }}</text
      ><view v-if="detail.instance.recordId">关联账单：{{ detail.instance.recordId }}</view
      ><view v-for="(h, index) in detail.history" :key="index"
        >{{ h.create_time }} · {{ historyLabel(h) }}</view
      ><button size="mini" @click="detail = null">收起详情</button></view
    >
    <view class="heading"
      ><text>周期账单</text><button size="mini" @click="load">刷新</button></view
    >
    <text v-if="error">{{ error }}</text>
    <view v-for="row in rows" :key="row.id" class="charge">
      <text>{{ row.sourceName }} · {{ row.dueDate }}</text
      ><button size="mini" @click="inspect(row)">详情</button>
      <view
        >预计 ¥{{ row.expectedAmount }} · {{ labels[row.status] || row.status
        }}<text v-if="row.actualAmount != null"> · 实际 ¥{{ row.actualAmount }}</text></view
      >
      <view v-if="['pending', 'payment_failed'].indexOf(row.status) >= 0" class="heading">
        <button size="mini" @click="open(row)">确认入账</button
        ><button size="mini" @click="resolve(row, 'skip')">跳过本期</button
        ><button size="mini" @click="resolve(row, 'payment-failed')">扣费失败</button>
      </view>
      <text v-else>{{ row.reason || "已处理" }}</text>
    </view>
    <text v-if="!loading && !rows.length && !error">暂无账期，规则到期后生成</text>
    <view class="heading"
      ><button
        size="mini"
        :disabled="page === 1"
        @click="
          page--;
          load();
        "
      >
        上一页</button
      ><text>第 {{ page }} 页</text
      ><button
        size="mini"
        :disabled="rows.length < 20"
        @click="
          page++;
          load();
        "
      >
        下一页
      </button></view
    >
    <view v-if="selected" class="editor">
      <text>确认实际扣费（联网提交）</text>
      <input type="digit" v-model="form.actualAmount" placeholder="实际金额" />
      <picker mode="date" :value="form.paidAt" @change="form.paidAt = $event.detail.value"
        ><view>扣费日期：{{ form.paidAt }}</view></picker
      >
      <picker
        :range="accounts"
        range-key="accountName"
        @change="form.accountId = accounts[$event.detail.value].id"
        ><view>账户：{{ accountName }}</view></picker
      >
      <picker
        v-if="selected.classificationId !== 0"
        :range="categories"
        range-key="typeName"
        @change="form.classificationId = categories[$event.detail.value].id"
        ><view>分类：{{ categoryName }}</view></picker
      >
      <button :loading="saving" :disabled="saving" @click="confirm">确认入账</button
      ><button @click="selected = null">取消</button>
    </view>
  </view>
</template>
<script>
import request from "@/utils/request";
import { listAccount } from "@/api/px/life/bookkeeping/account";
import { listClassification } from "@/api/px/life/bookkeeping/classification";
const key = () => `charge_${Date.now()}_${Math.random().toString(36).slice(2)}`;
export default {
  props: { sourceType: { type: String, default: null } },
  data() {
    return {
      detail: null,
      filterStatus: "",
      rows: [],
      page: 1,
      loading: false,
      error: "",
      saving: false,
      selected: null,
      form: {},
      accounts: [],
      categories: [],
      labels: {
        pending: "待确认",
        posted: "已入账",
        voided: "账单已作废",
        payment_failed: "扣费失败",
        skipped: "已跳过",
        cancelled: "已取消",
      },
    };
  },
  computed: {
    accountName() {
      const a = this.accounts.find((a) => a.id === this.form.accountId);
      return a ? a.accountName : "请选择";
    },
    categoryName() {
      const c = this.categories.find((c) => c.id === this.form.classificationId);
      return c ? c.typeName : "请选择";
    },
  },
  mounted() {
    this.load();
  },
  methods: {
    async inspect(row) {
      const r = await request({ url: `/charge/instances/${row.id}` });
      this.detail = r.data;
    },
    historyLabel(h) {
      try {
        const r = JSON.parse(h.result_json);
        return (this.labels[r.status] || r.status) + (r.reason ? " · " + r.reason : "");
      } catch (e) {
        return "处理记录";
      }
    },
    async load() {
      this.loading = true;
      this.error = "";
      try {
        const r = await request({
          url: "/charge/instances",
          method: "get",
          params: {
            pageNum: this.page,
            pageSize: 20,
            sourceType: this.sourceType,
            status: this.filterStatus || undefined,
          },
        });
        this.rows = r.data || [];
      } catch (e) {
        this.error = "账期加载失败，请联网后重试";
      } finally {
        this.loading = false;
      }
    },
    async open(row) {
      this.selected = row;
      this.form = {
        requestId: key(),
        expectedRevision: row.revision,
        actualAmount: String(row.expectedAmount),
        paidAt: row.dueDate,
        accountId: row.accountId,
        classificationId: row.classificationId,
      };
      try {
        const [a, c] = await Promise.all([
          listAccount({ pageSize: 100 }),
          listClassification({ pageSize: 100 }),
        ]);
        this.accounts = a.rows || [];
        this.categories = c.rows || [];
      } catch (e) {
        this.error = "账户或分类加载失败，请重试";
      }
    },
    async confirm() {
      if (this.saving) return;
      this.saving = true;
      try {
        await request({
          url: `/charge/instances/${this.selected.id}/confirm`,
          method: "post",
          data: this.form,
        });
        this.selected = null;
        uni.showToast({ title: "已入账" });
        await this.load();
      } finally {
        this.saving = false;
      }
    },
    resolve(row, action) {
      uni.showModal({
        title: "填写本期处理原因",
        editable: true,
        success: async (r) => {
          if (!r.confirm || !r.content) return;
          try {
            await request({
              url: `/charge/instances/${row.id}/${action}`,
              method: "post",
              data: { requestId: key(), expectedRevision: row.revision, reason: r.content },
            });
            await this.load();
          } catch (e) {
            this.error = "处理失败，请刷新后重试";
          }
        },
      });
    },
  },
};
</script>
<style scoped>
.charges {
  padding: 24rpx;
  background: #fff;
  border-radius: 16rpx;
  margin: 24rpx 0;
  color: #253045;
}
.heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin: 16rpx 0;
}
.charge {
  padding: 20rpx 0;
  border-bottom: 1px solid #eee;
}
.editor {
  padding: 24rpx;
  border: 1px solid #aab;
  border-radius: 12rpx;
}
.editor input,
.editor picker {
  padding: 20rpx 0;
}
</style>
