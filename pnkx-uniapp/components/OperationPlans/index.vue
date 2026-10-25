<template>
  <view class="plans"
    ><view class="head"
      ><view
        ><text class="heading">操作计划</text
        ><text class="sub">先确认，再执行。每一步都有结果。</text></view
      ><button size="mini" :loading="loading" :disabled="busy || draftEditing" @click="load">
        刷新
      </button></view
    >
    <view class="filters"
      ><button
        v-for="f in filters"
        :key="f.key"
        size="mini"
        :class="{ selected: filter === f.key }"
        @click="filter = f.key"
      >
        {{ f.label }} {{ count(f.key) }}
      </button></view
    >
    <input class="search" v-model="query" placeholder="搜索计划名称" aria-label="搜索计划名称" />
    <text v-if="error" class="error">{{ error }}</text
    ><text v-if="loading && !plans.length" class="empty">正在加载计划…</text>
    <view v-for="p in filtered" :key="p.id" class="row"
      ><view class="title-line"
        ><text class="title">{{ p.title }}</text
        ><text :class="['badge', tone(p.status)]">{{ labels[p.status] || p.status }}</text></view
      ><text class="sub">{{ hint(p.status) }}</text
      ><button
        size="mini"
        class="view-button"
        :disabled="busy || draftEditing"
        @click="inspect(p.id)"
      >
        {{ actionLabel(p.status) }}
      </button></view
    >
    <text v-if="!loading && !filtered.length && !error" class="empty">{{
      plans.length ? "没有匹配的计划，试试其他筛选或关键词" : "暂无操作计划，可在 AI 对话中创建草稿"
    }}</text
    ><text class="list-note">最近加载 {{ plans.length }} 个计划 · 待处理含待确认和执行失败</text>
    <view v-if="opened" class="detail"
      ><view class="head"
        ><text class="heading">计划详情</text
        ><button size="mini" :disabled="busy || draftEditing" @click="closeDetail">
          收起
        </button></view
      ><text v-if="detailLoading" class="empty">正在加载详情…</text
      ><text v-if="detailError" class="error">{{ detailError }}</text
      ><button v-if="detailError && !detail" size="mini" @click="inspect(selectedId)">
        重新加载
      </button>
      <template v-if="detail && !detailLoading"
        ><text class="title">{{ detail.title }}</text
        ><text :class="['badge', tone(detail.status)]">{{
          labels[detail.status] || detail.status
        }}</text
        ><text class="sub">{{ hint(detail.status) }}</text
        ><text class="progress">{{ completed }} / {{ detail.steps.length }} 步已完成</text
        ><view v-for="s in detail.steps" :key="s.step_number" class="step"
          ><view class="title-line"
            ><text class="title">{{ s.step_number }}. {{ tools[s.tool_code] || s.tool_code }}</text
            ><text :class="['badge', tone(s.status)]">{{
              labels[s.status] || s.status
            }}</text></view
          ><view v-for="(f, i) in fields(s.input_json)" :key="i" class="field"
            ><text class="field-label">{{ f.label }}</text
            ><text class="field-value" selectable>{{ f.value }}</text></view
          ><text v-if="result(s).sourceId" class="sub">已创建记录 #{{ result(s).sourceId }}</text
          ><text v-if="s.error_msg" class="error">{{ s.error_msg }}</text></view
        >
        <PlanDraftEditor
          :plan="detail"
          :disabled="busy"
          @editing="draftEditing = $event"
          @updated="acceptRevision"
        /><view v-if="!draftEditing" class="actions"
          ><button
            v-if="detail.status === 'awaiting_confirmation'"
            class="primary"
            :disabled="busy || draftEditing"
            :loading="busy"
            @click="run('confirm')"
          >
            确认并执行</button
          ><button
            v-if="['failed', 'partially_succeeded'].includes(detail.status)"
            class="primary"
            :disabled="busy || draftEditing"
            :loading="busy"
            @click="run('retry')"
          >
            重试未完成步骤</button
          ><template v-if="detail.status === 'running'"
            ><button :disabled="busy || draftEditing" @click="inspect(detail.id)">刷新进度</button
            ><button :disabled="busy || draftEditing" :loading="busy" @click="run('retry')">
              尝试恢复执行
            </button></template
          ><button
            v-if="['succeeded', 'partially_succeeded'].includes(detail.status)"
            :disabled="busy || draftEditing"
            @click="undo"
          >
            预览撤销</button
          ><button
            v-if="['awaiting_confirmation', 'failed'].includes(detail.status)"
            :disabled="busy || draftEditing"
            @click="run('cancel')"
          >
            取消计划
          </button></view
        ></template
      ></view
    >
  </view>
</template>
<script>
import request from "@/utils/request";
import PlanDraftEditor from "@/components/PlanDraftEditor.vue";
import {
  planLabels,
  toolLabels,
  planFilters,
  matchesPlan,
  planTone,
  planHint,
  planActionLabel,
  planFields,
  parsePlanValue,
} from "@/utils/operationPlanPresentation";
export default {
  components: { PlanDraftEditor },
  data() {
    return {
      plans: [],
      detail: null,
      filter: "all",
      query: "",
      opened: false,
      loading: false,
      detailLoading: false,
      error: "",
      detailError: "",
      busy: false,
      draftEditing: false,
      selectedId: "",
      listRequest: 0,
      detailRequest: 0,
      labels: planLabels,
      tools: toolLabels,
      filters: planFilters,
    };
  },
  computed: {
    filtered() {
      return this.plans.filter((p) => matchesPlan(p, this.filter, this.query));
    },
    completed() {
      return (this.detail?.steps || []).filter((s) => s.status === "succeeded").length;
    },
  },
  mounted() {
    this.load();
  },
  beforeUnmount() {
    this.listRequest++;
    this.detailRequest++;
  },
  methods: {
    acceptRevision(plan) {
      this.detail = plan;
      this.selectedId = plan.id;
      this.draftEditing = false;
      this.load();
    },
    tone: planTone,
    hint: planHint,
    actionLabel: planActionLabel,
    fields: planFields,
    result(s) {
      return parsePlanValue(s.result_json);
    },
    count(key) {
      return this.plans.filter((p) => matchesPlan(p, key)).length;
    },
    closeDetail() {
      if (this.busy || this.draftEditing) return;
      this.detailRequest++;
      this.opened = false;
      this.detail = null;
    },
    async load() {
      const token = ++this.listRequest;
      this.loading = true;
      try {
        const r = await request({ url: "/operation-plans", method: "get" });
        if (token === this.listRequest) {
          this.plans = r.data || [];
          this.error = "";
        }
      } catch {
        if (token === this.listRequest) this.error = "加载失败，已有结果保留，请刷新重试";
      } finally {
        if (token === this.listRequest) this.loading = false;
      }
    },
    async inspect(id) {
      if (this.busy || this.draftEditing) return;
      const token = ++this.detailRequest;
      this.selectedId = id;
      this.opened = true;
      this.detail = null;
      this.detailError = "";
      this.detailLoading = true;
      try {
        const r = await request({ url: `/operation-plans/${id}`, method: "get" });
        if (token === this.detailRequest) this.detail = r.data;
      } catch {
        if (token === this.detailRequest) this.detailError = "详情加载失败，请重试";
      } finally {
        if (token === this.detailRequest) this.detailLoading = false;
      }
    },
    async run(action) {
      if (this.busy || !this.detail) return;
      this.busy = true;
      this.detailError = "";
      try {
        const r = await request({
          url: `/operation-plans/${this.detail.id}/${action}`,
          method: "post",
        });
        this.detail = r.data;
        await this.load();
      } catch (e) {
        this.detailError = e?.message || "操作未完成，请刷新详情核对后重试";
      } finally {
        this.busy = false;
      }
    },
    async undo() {
      if (this.busy || !this.detail) return;
      this.busy = true;
      this.detailError = "";
      const id = this.detail.id;
      try {
        const r = await request({
          url: `/operation-plans/${id}/compensation-preview`,
          method: "post",
        });
        const res = await new Promise((resolve, reject) =>
          uni.showModal({
            title: "确认撤销",
            content: `将移除 ${r.data.compensation.length} 个本计划创建且未修改的对象。`,
            success: resolve,
            fail: reject,
          })
        );
        this.busy = false;
        if (res.confirm && this.detail?.id === id) await this.run("compensate");
      } catch (e) {
        this.detailError = e?.message || "当前无法撤销，请在原模块核对内容";
      } finally {
        this.busy = false;
      }
    },
  },
};
</script>
<style scoped>
.plans {
  background: #fff;
  padding: 28rpx;
  border: 1px solid #e2e8f0;
  border-radius: 20rpx;
  margin: 20rpx;
  color: #1e293b;
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}
.head button {
  flex-shrink: 0;
  margin: 0;
}
.heading {
  font-size: 32rpx;
  font-weight: 650;
  display: block;
}
.sub {
  display: block;
  font-size: 24rpx;
  line-height: 1.65;
  color: #475569;
  margin-top: 10rpx;
}
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  margin: 24rpx 0 16rpx;
}
.filters button {
  margin: 0;
  background: #f1f5f9;
  color: #475569;
  font-size: 23rpx;
}
.filters button.selected {
  background: #e0f2fe;
  color: #075985;
}
.search {
  font-size: 26rpx;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  margin-bottom: 12rpx;
}
.row {
  padding: 26rpx 0;
  border-bottom: 1px solid #e2e8f0;
}
.title-line {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-wrap: wrap;
}
.title {
  font-size: 28rpx;
  font-weight: 600;
  word-break: break-word;
}
.badge {
  display: inline-block;
  font-size: 22rpx;
  border-radius: 8rpx;
  padding: 5rpx 12rpx;
  background: #f1f5f9;
  color: #475569;
}
.badge.primary {
  background: #e0f2fe;
  color: #075985;
}
.badge.success {
  background: #f0fdf4;
  color: #166534;
}
.badge.danger {
  background: #fff1f2;
  color: #9f1239;
}
.view-button {
  display: block;
  width: fit-content;
  margin: 18rpx 0 0 auto;
  color: #0369a1;
  background: #f0f9ff;
}
.empty {
  display: block;
  padding: 40rpx 8rpx;
  font-size: 26rpx;
  color: #475569;
}
.list-note {
  display: block;
  font-size: 22rpx;
  color: #64748b;
  margin-top: 20rpx;
  line-height: 1.6;
}
.detail {
  margin-top: 28rpx;
  border-top: 2px solid #e0f2fe;
  padding-top: 24rpx;
}
.detail > .title {
  display: block;
  margin: 20rpx 0 10rpx;
}
.progress {
  display: block;
  font-size: 24rpx;
  color: #475569;
  margin: 16rpx 0;
}
.step {
  padding: 22rpx;
  border: 1px solid #e2e8f0;
  border-radius: 14rpx;
  margin: 20rpx 0;
}
.field {
  display: flex;
  gap: 16rpx;
  margin-top: 18rpx;
  font-size: 26rpx;
  line-height: 1.6;
}
.field-label {
  flex: 0 0 120rpx;
  color: #475569;
}
.field-value {
  min-width: 0;
  white-space: pre-wrap;
  word-break: break-word;
}
.error {
  display: block;
  color: #9f1239;
  background: #fff1f2;
  padding: 16rpx;
  border-radius: 10rpx;
  margin: 16rpx 0;
  font-size: 25rpx;
  line-height: 1.6;
}
.actions {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-top: 24rpx;
}
.actions button {
  width: 100%;
  margin: 0;
  font-size: 28rpx;
  min-height: 88rpx;
}
.actions .primary {
  background: #0284c7;
  color: white;
}
button::after {
  border-color: #e2e8f0;
}
</style>
