<template>
  <view class="chat-plan-card">
    <text v-if="busy" class="sub">正在同步计划…</text
    ><text v-if="error" class="error">{{ error }}</text>
    <template v-if="plan">
      <view class="heading"
        ><text>{{ plan.title }}</text
        ><text>{{ label(plan.status) }}</text></view
      >
      <text class="sub">{{ hint(plan.status) }}</text>
      <view v-for="(step, index) in plan.steps" :key="index" class="card-step">
        <text>{{ tool(step.tool_code) }}</text>
        <view v-for="(field, i) in fields(step.input_json)" :key="i" class="field"
          ><text class="sub">{{ field.label }}</text
          ><text>{{ field.value }}</text></view
        >
        <text v-if="step.error_msg" class="error">{{ step.error_msg }}</text>
      </view>
      <plan-draft-editor
        :plan="plan"
        :disabled="locked"
        @editing="editing = $event"
        @updated="revised"
      />
      <view v-if="!editing" class="card-actions">
        <button v-if="plan.replacement_id" :disabled="locked" @click="load(plan.replacement_id)">
          查看修改后的预览
        </button>
        <button
          v-if="plan.status === 'awaiting_confirmation'"
          type="primary"
          :disabled="locked"
          @click="run('confirm')"
        >
          确认执行以上内容
        </button>
        <button
          v-if="['failed', 'partially_succeeded'].includes(plan.status)"
          :disabled="locked"
          @click="run('retry')"
        >
          重试未完成步骤
        </button>
        <button
          v-if="plan.status === 'awaiting_confirmation'"
          :disabled="locked"
          @click="run('cancel')"
        >
          取消计划
        </button>
      </view>
    </template>
    <button v-if="!editing" :disabled="locked" @click="load(selectedId || planId)">刷新状态</button>
  </view>
</template>
<script>
import request from "@/utils/request";
import PlanDraftEditor from "./PlanDraftEditor.vue";
import { planLabels, toolLabels, planFields, planHint } from "../utils/operationPlanPresentation";
export default {
  components: { PlanDraftEditor },
  props: { planId: String, disabled: Boolean },
  data() {
    return {
      plan: null,
      busy: false,
      editing: false,
      error: "",
      requestVersion: 0,
      selectedId: null,
    };
  },
  computed: {
    locked() {
      return this.busy || this.disabled || this.editing;
    },
  },
  watch: {
    planId: {
      immediate: true,
      handler(id) {
        this.load(id);
      },
    },
  },
  beforeUnmount() {
    this.requestVersion++;
  },
  methods: {
    label: (status) => planLabels[status] || status,
    tool: (code) => toolLabels[code] || code,
    fields: planFields,
    hint: planHint,
    async load(id) {
      if (!id || this.editing || this.busy) return;
      const version = ++this.requestVersion;
      this.selectedId = id;
      this.busy = true;
      this.error = "";
      this.plan = null;
      try {
        const r = await request({ url: `/operation-plans/${id}` });
        if (version === this.requestVersion) this.plan = r.data;
      } catch (e) {
        if (version === this.requestVersion) this.error = "预览加载失败，请重试。";
      } finally {
        if (version === this.requestVersion) this.busy = false;
      }
    },
    revised(plan) {
      this.plan = plan;
      this.selectedId = plan.id;
      this.editing = false;
    },
    async run(action) {
      if (this.locked || !this.plan) return;
      this.busy = true;
      this.error = "";
      const version = ++this.requestVersion;
      try {
        const r = await request({
          url: `/operation-plans/${this.plan.id}/${action}`,
          method: "post",
        });
        if (version === this.requestVersion) this.plan = r.data;
      } catch (e) {
        if (version === this.requestVersion)
          this.error = e?.message || "操作未完成，请刷新状态后重试。";
      } finally {
        if (version === this.requestVersion) this.busy = false;
      }
    },
  },
};
</script>
<style scoped>
.chat-plan-card {
  margin-top: 24rpx;
  padding: 24rpx;
  border: 1px solid #cbd5e1;
  border-radius: 20rpx;
  background: #f8fafc;
  color: #334155;
}
.heading {
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
  font-weight: 600;
}
.sub {
  display: block;
  color: #64748b;
  font-size: 24rpx;
  line-height: 1.6;
}
.field {
  margin: 16rpx 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.card-step {
  margin-top: 24rpx;
}
.error {
  display: block;
  color: #b91c1c;
}
.card-actions button,
button {
  margin-top: 16rpx;
  font-size: 26rpx;
  min-height: 88rpx;
}
</style>
