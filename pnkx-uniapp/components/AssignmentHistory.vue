<template>
  <view
    ><button size="mini" @click="open">分工与转派记录</button>
    <view v-if="visible" class="assignment">
      <button size="mini" @click="visible = false">收起</button
      ><text v-if="error">{{ error }}</text>
      <text>当前负责人：{{ data.assignee || "未分配" }}</text>
      <view v-if="data.canTransfer">
        <picker
          :range="candidateLabels"
          @change="target = String(data.candidates[Number($event.detail.value)].userId)"
          ><view>新负责人：{{ target || "点击选择" }}</view></picker
        >
        <textarea v-model="reason" maxlength="500" placeholder="填写转派原因（必填）" />
        <button size="mini" :disabled="busy || !target || !reason.trim()" @click="transfer">
          确认转派
        </button>
      </view>
      <text v-if="!(data.history || []).length">暂无分工变更记录</text>
      <view v-for="h in data.history || []" :key="h.id" class="entry"
        ><text
          >{{ h.previous_assignee || "未分配" }} → {{ h.assignee || "未分配" }} · 操作人
          {{ h.operator_id }}</text
        ><text>{{ h.reason }}</text
        ><text>{{ h.create_time }}</text></view
      >
      <button
        size="mini"
        :disabled="page === 1"
        @click="
          page--;
          load();
        "
      >
        上一页</button
      ><button
        size="mini"
        :disabled="(data.history || []).length < 30"
        @click="
          page++;
          load();
        "
      >
        下一页
      </button>
    </view></view
  >
</template>
<script>
import request from "@/utils/request";
export default {
  props: {
    type: { type: String, required: true },
    sourceId: { type: [String, Number], required: true },
  },
  emits: ["changed"],
  data: () => ({
    visible: false,
    busy: false,
    error: "",
    data: {},
    target: "",
    reason: "",
    page: 1,
    attempt: null,
  }),
  computed: {
    candidateLabels() {
      return (this.data.candidates || []).map((c) => "成员 " + c.userId);
    },
  },
  methods: {
    async load() {
      try {
        this.data = (
          await request({
            url: `/life/assignments/${this.type}/${this.sourceId}`,
            params: { pageNum: this.page },
          })
        ).data;
        this.error = "";
      } catch (_) {
        this.data = {};
        this.error = "分工记录加载失败，请收起后重试";
      }
    },
    async open() {
      this.page = 1;
      this.visible = true;
      this.target = "";
      this.reason = "";
      this.attempt = null;
      await this.load();
    },
    async transfer() {
      const body = {
          assignee: this.target,
          expectedAssignee: this.data.assignee ?? null,
          reason: this.reason,
        },
        hash = JSON.stringify(body);
      if (!this.attempt || this.attempt.hash !== hash)
        this.attempt = {
          hash,
          body: {
            ...body,
            requestId: `assign_${Date.now()}_${Math.random().toString(36).slice(2)}`,
          },
        };
      this.busy = true;
      try {
        this.data = (
          await request({
            url: `/life/assignments/${this.type}/${this.sourceId}/transfer`,
            method: "post",
            data: this.attempt.body,
          })
        ).data;
        this.reason = "";
        this.target = "";
        this.attempt = null;
        this.page = 1;
        this.$emit("changed", this.data.assignee);
      } catch (_) {
        this.error = "转派未确认成功，可重试；负责人变化时请重新打开核对";
      } finally {
        this.busy = false;
      }
    },
  },
};
</script>
<style scoped>
.assignment {
  padding: 20rpx;
  background: #f5f7fa;
  color: #303133;
}
.assignment text {
  display: block;
  word-break: break-all;
}
.assignment textarea {
  width: 100%;
  margin: 20rpx 0;
}
.entry {
  padding: 16rpx 0;
  border-bottom: 1px solid #ddd;
}
</style>
