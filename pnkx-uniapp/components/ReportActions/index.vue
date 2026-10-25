<template>
  <view class="insights"
    ><text>报告行动与复盘</text
    ><button size="mini" :loading="loading" @click="snapshot">生成当前周期事实快照</button
    ><view v-for="i in rows" :key="i.id" class="insight"
      ><text>{{ i.fact_text }}</text
      ><view v-if="i.status === 'proposed'"
        ><text>可选行动为规则草稿，可修改后再确认。</text
        ><view v-for="c in i.candidates || []" :key="c.id" class="candidate"
          ><text>{{ c.title }}：{{ c.basis }}</text
          ><button size="mini" @click="open(i, c)">选择并编辑</button></view
        ></view
      ><view v-if="i.action_status"
        >行动复盘：{{ statusLabels[i.action_status]
        }}<text v-if="i.target"> · {{ i.target.content }}</text></view
      ><view
        ><button size="mini" @click="sources(i)">来源明细</button
        ><button v-if="i.status === 'proposed'" size="mini" @click="open(i)">创建行动</button
        ><text v-else>{{ i.status === "accepted" ? "已采纳" : "已忽略" }}</text></view
      ></view
    ><view v-if="selected"
      ><input v-model="form.content" placeholder="具体行动内容" /><picker
        mode="date"
        @change="form.dueDate = $event.detail.value"
        ><view>完成日期 {{ form.dueDate || "请选择" }}</view></picker
      ><text v-if="actionError">{{ actionError }}</text
      ><button :loading="saving" @click="accept">预览并确认</button
      ><button @click="selected = null">取消</button></view
    ><view v-if="sourceRows"
      ><text>当前来源（可能与快照不同）</text
      ><view v-for="r in sourceRows" :key="r.id"
        >{{ r.pay_time }} · {{ r.type_name }} · ¥{{ r.money }}</view
      ><button
        size="mini"
        :disabled="sourcePage === 1"
        @click="
          sourcePage--;
          sources(sourceInsight, false);
        "
      >
        上一页</button
      ><button
        size="mini"
        :disabled="sourceRows.length < 30"
        @click="
          sourcePage++;
          sources(sourceInsight, false);
        "
      >
        下一页</button
      ><button size="mini" @click="sourceRows = null">收起</button></view
    ></view
  >
</template>
<script>
import request from "@/utils/request";
export default {
  props: { period: { type: String, default: "month" } },
  data() {
    return {
      statusLabels: {
        accepted: "待办进行中",
        completed: "待办已完成",
        cancelled: "已取消或来源移除",
      },
      rows: [],
      loading: false,
      saving: false,
      actionError: "",
      selected: null,
      form: { content: "", dueDate: "" },
      sourceRows: null,
      sourceInsight: null,
      sourcePage: 1,
    };
  },
  mounted() {
    this.load();
  },
  methods: {
    async load() {
      const r = await request({ url: "/lifeReport/insights", method: "get" });
      this.rows = r.data || [];
    },
    async snapshot() {
      this.loading = true;
      try {
        await request({
          url: "/lifeReport/insights",
          method: "post",
          data: { period: this.period },
        });
        await this.load();
      } finally {
        this.loading = false;
      }
    },
    async open(i, candidate) {
      this.selected = i;
      this.actionError = "";
      this.form = candidate
        ? { content: candidate.content, dueDate: candidate.dueDate, candidateId: candidate.id }
        : { content: "", dueDate: "" };
      if (!candidate && i.plan_id) {
        const p = await request({ url: `/operation-plans/${i.plan_id}` });
        this.form = {
          ...JSON.parse(p.data.steps[0].input_json),
          candidateId: i.selected_candidate,
        };
      }
    },
    async accept() {
      if (this.saving) return;
      this.saving = true;
      try {
        const p = await request({
          url: `/lifeReport/insights/${this.selected.id}/action-preview`,
          method: "post",
          data: this.form,
        });
        const a = JSON.parse(p.data.steps[0].input_json);
        const ok = await new Promise((resolve) =>
          uni.showModal({
            title: "确认创建待办",
            content: `${a.content}，完成日期：${a.dueDate}`,
            success: (r) => resolve(r.confirm),
            fail: () => resolve(false),
          })
        );
        if (!ok) return;
        const result = await request({
          url: `/lifeReport/insights/${this.selected.id}/accept`,
          method: "post",
          data: { expectedPlanId: p.data.id },
        });
        if (result.data.status !== "succeeded") {
          this.actionError = "行动尚未执行成功，请在操作计划中查看失败原因并重试";
          await this.load();
          return;
        }
        this.selected = null;
        await this.load();
      } finally {
        this.saving = false;
      }
    },
    async sources(i, reset = true) {
      this.sourceInsight = i;
      if (reset) this.sourcePage = 1;
      const r = await request({
        url: `/lifeReport/insights/${i.id}/sources`,
        method: "get",
        params: { pageNum: this.sourcePage },
      });
      this.sourceRows = r.data || [];
    },
  },
};
</script>
<style scoped>
.insights {
  padding: 24rpx;
  background: white;
  border-radius: 16rpx;
  margin: 20rpx 0;
  color: #253045;
}
.candidate {
  padding: 16rpx;
  border: 1px solid #ddd;
  margin: 12rpx 0;
}
.insight {
  padding: 20rpx 0;
  border-bottom: 1px solid #eee;
}
input,
picker {
  padding: 24rpx 0;
}
</style>
