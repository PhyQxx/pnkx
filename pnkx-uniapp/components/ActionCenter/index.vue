<template>
  <view class="actions">
    <view class="head"><text>今天要处理</text><button size="mini" @click="load">刷新</button></view>
    <picker :range="spaceLabels" :value="spaceIndex" @change="switchSpace"
      ><view class="item">{{ spaceLabels[spaceIndex] }} ▾</view></picker
    >
    <view class="head"
      ><button v-for="key in pinned" :key="key" size="mini" @click="go(key)">
        {{ labels[key] }}
      </button></view
    >
    <button size="mini" @click="editing = !editing">管理常用入口</button>
    <view v-if="editing"
      ><checkbox-group @change="pinsChanged"
        ><label v-for="(label, key) in labels" :key="key"
          ><checkbox :value="key" :checked="pinned.includes(key)" />{{ label }}</label
        ></checkbox-group
      ><button size="mini" @click="savePins">保存</button></view
    >
    <text v-if="error">{{ error }}</text>
    <view v-for="todo in visibleTodos" :key="todo.id" class="item"
      ><text>{{ todo.content }} · {{ todo.planEndTime }}</text
      ><button v-if="data.canEdit" size="mini" @click="complete(todo)">完成</button></view
    >
    <text v-if="data.todoCount === 0">暂无到期或逾期待办</text>
    <button v-if="data.todoCount > 5" size="mini" @click="expanded = !expanded">
      {{ expanded ? "收起" : "查看全部（" + data.todoCount + "）" }}
    </button>
    <view v-if="expanded && data.todoCount > 20" class="head"
      ><button size="mini" :disabled="page === 1" @click="turn(-1)">上一页</button
      ><text>{{ page }}</text
      ><button size="mini" :disabled="page * 20 >= data.todoCount" @click="turn(1)">
        下一页
      </button></view
    >
    <feature-gate feature="chargeInstanceV2"
      ><view v-for="f in data.generationFailures || []" :key="'failure' + f.id" class="item"
        ><text
          >出账失败：{{ f.source_name }} · {{ f.due_date }}，请核对账户与分类；自动重试不早于
          {{ f.next_retry_at || "下一轮调度" }}</text
        ><button size="mini" @click="retryCharge(f, 'retry')">重试</button
        ><button size="mini" @click="retryCharge(f, 'dismiss')">忽略记录</button></view
      ></feature-gate
    ><view v-for="m in data.meals || []" :key="'meal' + m.id" class="item" @click="go('meal')"
      >{{ m.title }} · {{ m.servings }}份</view
    >
    <feature-gate feature="reminderEventV2"
      ><view v-for="e in visibleEvents" :key="'event' + e.id" class="item"
        ><text>{{ e.title }} · {{ states[e.state] || e.state }}</text
        ><view class="head"
          ><button v-if="!e.readAt" size="mini" @click="act(e, 'read')">已读</button
          ><button size="mini" @click="act(e, 'snooze')">一小时后</button
          ><button size="mini" @click="act(e, 'dismiss')">忽略本次</button></view
        ></view
      > </feature-gate
    ><template v-if="spaceIndex === 0"
      ><button size="mini" @click="details = !details">
        {{ details ? "收起账单与执行计划" : "查看账单与执行计划" }}</button
      ><template v-if="details"
        ><feature-gate feature="chargeInstanceV2"><charge-instances /></feature-gate
        ><feature-gate feature="operationPlanV2"><operation-plans /></feature-gate></template
    ></template>
  </view>
</template>
<script>
import request from "@/utils/request";
import ChargeInstances from "@/components/ChargeInstances/index.vue";
import OperationPlans from "@/components/OperationPlans/index.vue";
const paths = {
  todo: "/pages_life/todo/index",
  meal: "/pages_life/mealPlan/index",
  shopping: "/pages_life/shoppingList/index",
  bookkeeping: "/pages_life/bookkeeping/index",
  diary: "/pages_life/diary/index",
  report: "/pages_life/report/index",
};
export default {
  components: { ChargeInstances, OperationPlans },
  data() {
    return {
      data: {},
      error: "",
      spaces: [],
      spaceIndex: 0,
      page: 1,
      expanded: false,
      details: false,
      pinned: [],
      prefRevision: 0,
      editing: false,
      labels: {
        todo: "待办",
        meal: "本周吃饭",
        shopping: "购物",
        bookkeeping: "记账",
        diary: "日记",
        report: "月末复盘",
      },
      states: { open: "待处理", snoozed: "稍后提醒", resolved: "已处理", cancelled: "已结束" },
    };
  },
  computed: {
    spaceLabels() {
      return ["私人空间", ...this.spaces.map((g) => g.group_name)];
    },
    visibleTodos() {
      return (this.data.todos || []).slice(0, this.expanded ? 20 : 5);
    },
    visibleEvents() {
      return (this.data.reminders || [])
        .filter((e) => ["open", "snoozed"].includes(e.state))
        .slice(0, 5);
    },
  },
  async mounted() {
    this.load();
    const results = await Promise.allSettled([
      request({ url: "/life/action-center/spaces" }),
      request({ url: "/life/action-center/preferences" }),
    ]);
    if (results[0].status === "fulfilled") this.spaces = results[0].value.data || [];
    if (results[1].status === "fulfilled") {
      this.pinned = JSON.parse(results[1].value.data.entries_json);
      this.prefRevision = results[1].value.data.revision;
    }
  },
  methods: {
    go(key) {
      uni.navigateTo({ url: paths[key] });
    },
    pinsChanged(e) {
      this.pinned = e.detail.value;
    },
    async savePins() {
      const r = await request({
        url: "/life/action-center/preferences",
        method: "put",
        data: { entries: this.pinned, expectedRevision: this.prefRevision },
      });
      this.prefRevision = r.data.revision;
      this.editing = false;
    },
    switchSpace(e) {
      this.spaceIndex = Number(e.detail.value);
      this.data = {};
      this.page = 1;
      this.load();
    },
    turn(delta) {
      this.page += delta;
      this.load();
    },
    async load() {
      try {
        const r = await request({
          url: "/life/action-center",
          method: "get",
          params: {
            scope: this.spaceIndex === 0 ? "personal" : "group",
            groupId: this.spaceIndex === 0 ? undefined : this.spaces[this.spaceIndex - 1].id,
            page: this.page,
          },
        });
        this.data = r.data || {};
        this.error =
          this.data.partialErrors && this.data.partialErrors.length ? "部分内容暂时无法加载" : "";
      } catch (e) {
        this.data = {};
        this.error = "行动中心加载失败，请联网重试";
      }
    },
    async retryCharge(f, action) {
      await request({ url: `/charge/instances/failures/${f.id}/${action}`, method: "post" });
      await this.load();
    },
    async complete(t) {
      await request({
        url: "/admin/toDo",
        method: "put",
        data: { id: t.id, status: true, kanbanStatus: 2 },
      });
      await this.load();
    },
    async act(e, action) {
      await request({
        url: `/reminder/events/${e.id}/${action}`,
        method: "post",
        data: {
          requestId: `reminder_${Date.now()}_${Math.random().toString(36).slice(2)}`,
          expectedRevision: e.revision,
          until: action === "snooze" ? new Date(Date.now() + 3600000).toISOString() : null,
        },
      });
      await this.load();
    },
  },
};
</script>
<style scoped>
.actions {
  padding: 24rpx;
  margin: 24rpx;
  background: white;
  border-radius: 16rpx;
  color: #253045;
}
.head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8rpx;
}
.item {
  padding: 20rpx 0;
  border-bottom: 1px solid #eee;
}
</style>
