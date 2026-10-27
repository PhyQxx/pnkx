<template>
  <view class="actions">
    <view class="head"
      ><view class="head__left"
        ><text class="head__title">今天要处理</text
        ><text class="head__date">{{ todayLabel }}</text></view
      ><view class="head__right"
        ><text v-if="summary.todos" class="chip chip--danger">待办 {{ summary.todos }}</text
        ><text v-if="summary.failures" class="chip chip--warning">出账失败 {{ summary.failures }}</text
        ><text v-if="summary.reminders" class="chip chip--primary">提醒 {{ summary.reminders }}</text
        ><button size="mini" :loading="loading" class="refresh-btn" @click="load">刷新</button></view
      ></view
    >
    <view class="toolbar"
      ><picker :range="spaceLabels" :value="spaceIndex" @change="switchSpace"
        ><view class="space-picker">{{ spaceLabels[spaceIndex] }} ▾</view></picker
      ></view
    >
    <view class="pins"
      ><button v-for="key in pinned" :key="key" size="mini" class="pin" @click="go(key)">
        {{ labels[key] }}
      </button>
      <button size="mini" class="pin pin--ghost" :disabled="!preferencesReady || savingPins" @click="editPins">
        管理入口
      </button>
    </view>
    <view v-if="editing" class="pins-editor"
      ><checkbox-group @change="pinsChanged"
        ><label v-for="(label, key) in labels" :key="key"
          ><checkbox :value="key" :checked="draftPinned.includes(key)" :disabled="savingPins" />{{
            label
          }}</label
        ></checkbox-group
      ><text v-if="pinsError">{{ pinsError }}</text
      ><button size="mini" :loading="savingPins" @click="savePins">保存顺序</button
      ><button size="mini" :disabled="savingPins" @click="editing = false">取消</button
      ><text class="hint">按勾选顺序排列，保存后生效</text></view
    >
    <text v-if="loading" class="loading-note">正在加载当前空间…</text>
    <text v-if="error" class="error-note">{{ error }}</text>

    <view class="section section--danger">
      <view class="section__head"
        ><view class="dot dot--danger"></view><text class="section__title">到期与逾期待办</text
        ><text v-if="data.todoCount != null" class="count">{{ data.todoCount }}</text></view
      >
      <view
        v-for="(todo, index) in visibleTodos"
        :key="todo.id"
        class="item"
        :class="{ 'item--overdue': isOverdue(todo) }"
        :style="{ '--i': index }"
        ><view class="item__main"
          ><text class="item__title">{{ todo.content }}</text
          ><view class="item__meta"
            ><text class="chip chip--plain">{{ shortTime(todo.planEndTime) }}</text
            ><text v-if="isOverdue(todo)" class="tag tag--danger">已逾期</text></view
          ></view
        ><button
          v-if="data.canEdit"
          class="item__action"
          :disabled="loading || busyItems['todo:' + todo.id]"
          size="mini"
          @click="complete(todo)"
        >
          完成
        </button></view
      >
      <text v-if="data.todoCount === 0" class="empty">暂无到期或逾期待办</text>
      <button v-if="data.todoCount > 5" size="mini" class="more-btn" @click="expanded = !expanded">
        {{ expanded ? "收起" : "查看全部（" + data.todoCount + "）" }}
      </button>
      <view v-if="expanded && data.todoCount > 20" class="pager"
        ><button size="mini" :disabled="page === 1" @click="turn(-1)">上一页</button
        ><text>{{ page }}</text
        ><button size="mini" :disabled="page * 20 >= data.todoCount" @click="turn(1)">
          下一页
        </button></view
      >
    </view>

    <feature-gate feature="chargeInstanceV2"
      ><view class="section section--warning">
        <view class="section__head"
          ><view class="dot dot--warning"></view><text class="section__title">出账失败</text
          ><text v-if="(data.generationFailures || []).length" class="count">{{
            data.generationFailures.length
          }}</text></view
        >
        <view v-for="f in data.generationFailures || []" :key="'failure' + f.id" class="item item--warning"
          ><view class="item__main"
            ><text class="item__title">{{ f.source_name }} · {{ f.due_date }}</text
            ><view class="item__meta"
              ><text class="chip chip--plain">已尝试 {{ f.attempt_count }} 次</text
              ><text class="chip chip--plain">下次不早于 {{ shortTime(f.next_retry_at) || "下一轮调度" }}</text></view
            ></view
          ><view class="item__actions"
            ><button
              size="mini"
              class="item__action"
              :disabled="busyItems['charge:' + f.id]"
              @click="retryCharge(f, 'retry')"
            >
              重试</button
            ><button size="mini" :disabled="busyItems['charge:' + f.id]" @click="retryCharge(f, 'dismiss')">
              忽略
            </button></view
          ></view
        >
      </view></feature-gate
    >

    <view class="section section--success">
      <view class="section__head"
        ><view class="dot dot--success"></view><text class="section__title">今日餐饮</text
        ><text v-if="(data.meals || []).length" class="count">{{ data.meals.length }}</text></view
      >
      <view v-for="m in data.meals || []" :key="'meal' + m.id" class="item" @click="go('meal')"
        ><view class="item__main"
          ><text class="item__title">{{ m.title }}</text
          ><view class="item__meta"
            ><text class="chip chip--plain">{{ m.servings }} 份</text></view
          ></view
        ><text class="link">查看计划</text></view
      >
    </view>

    <feature-gate feature="reminderEventV2"
      ><view class="section section--primary">
        <view class="section__head"
          ><view class="dot dot--primary"></view><text class="section__title">提醒事项</text
          ><text v-if="visibleEvents.length" class="count">{{ visibleEvents.length }}</text></view
        >
        <view v-for="e in visibleEvents" :key="'event' + e.id" class="item"
          ><view class="item__main"
            ><text class="item__title">{{ e.title }}</text
            ><view class="item__meta"
              ><text class="tag" :class="e.state === 'open' ? 'tag--primary' : 'tag--plain'">{{
                states[e.state] || e.state
              }}</text
              ><text class="chip chip--plain">{{ e.readAt ? "已读" : "未读" }}</text></view
            ></view
          ><view class="item__actions"
            ><button
              v-if="!e.readAt"
              size="mini"
              :disabled="busyItems['reminder:' + e.id]"
              @click="act(e, 'read')"
            >
              已读</button
            ><button size="mini" :disabled="busyItems['reminder:' + e.id]" @click="act(e, 'snooze')">
              一小时后</button
            ><button size="mini" :disabled="busyItems['reminder:' + e.id]" @click="act(e, 'dismiss')">
              忽略本次
            </button></view
          ></view
        >
      </view></feature-gate
    >

    <template v-if="spaceIndex === 0"
      ><button size="mini" class="more-btn" @click="details = !details">
        {{ details ? "收起账单与执行计划" : "查看账单与执行计划" }}</button
      ><template v-if="details"
        ><feature-gate feature="chargeInstanceV2"><charge-instances /></feature-gate
        ><feature-gate feature="operationPlanV2"><operation-plans /></feature-gate></template
    ></template>
  </view>
</template>
<script>
import { createActionMutations } from "@/utils/actionCenterMutations";
import request from "@/utils/request";
import { createLatestLoader, parsePinned } from "@/utils/actionCenterState";
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
      loading: false,
      busyItems: {},
      mutations: null,
      loader: null,
      draftPinned: [],
      savingPins: false,
      preferencesReady: false,
      pinsError: "",
      error: "",
      spaces: [],
      spaceIndex: 0,
      page: 1,
      expanded: false,
      details: false,
      pinned: [],
      prefRevision: 0,
      editing: false,
      todayLabel: new Date().toLocaleDateString("zh-CN", {
        month: "long",
        day: "numeric",
        weekday: "long",
      }),
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
    summary() {
      return {
        todos: this.data.todoCount || 0,
        failures: (this.data.generationFailures || []).length,
        reminders: this.visibleEvents.length,
      };
    },
  },
  created() {
    this.mutations = createActionMutations({
      request,
      refresh: () => this.load(),
      onBusy: (key, value) => {
        this.busyItems = { ...this.busyItems, [key]: value };
      },
      onError: (message) => {
        this.error = message;
      },
      makeId: () => `reminder_${Date.now()}_${Math.random().toString(36).slice(2)}`,
    });
    this.loader = createLatestLoader({
      fetch: (params) => request({ url: "/life/action-center", method: "get", params }),
      onStart: () => {
        this.loading = true;
        this.error = "";
        this.data = {};
      },
      onData: (r) => {
        this.data = r.data || {};
        this.error = this.data.partialErrors?.length ? "部分内容暂时无法加载，请刷新重试" : "";
      },
      onError: () => {
        this.error = "行动中心加载失败，请联网重试";
      },
      onSettled: () => {
        this.loading = false;
      },
    });
  },
  beforeUnmount() {
    this.loader.invalidate();
    this.mutations.dispose();
  },
  async mounted() {
    this.load();
    const results = await Promise.allSettled([
      request({ url: "/life/action-center/spaces" }),
      request({ url: "/life/action-center/preferences" }),
    ]);
    if (results[0].status === "fulfilled") this.spaces = results[0].value.data || [];
    if (results[1].status === "fulfilled") {
      this.pinned = parsePinned(results[1].value.data.entries_json, this.labels);
      this.prefRevision = results[1].value.data.revision;
    }
    this.preferencesReady = true;
  },
  methods: {
    isOverdue(todo) {
      if (!todo || !todo.planEndTime) return false;
      return new Date(todo.planEndTime.replace(/-/g, "/")).getTime() < Date.now();
    },
    shortTime(value) {
      return value ? String(value).slice(5, 16) : "";
    },
    go(key) {
      uni.navigateTo({ url: paths[key] });
    },
    pinsChanged(e) {
      this.draftPinned = e.detail.value.slice(0, 6);
    },
    editPins() {
      if (this.savingPins || !this.preferencesReady) return;
      this.draftPinned = [...this.pinned];
      this.pinsError = "";
      this.editing = !this.editing;
    },
    async savePins() {
      if (this.savingPins) return;
      this.savingPins = true;
      this.pinsError = "";
      const entries = [...this.draftPinned];
      try {
        const r = await request({
          url: "/life/action-center/preferences",
          method: "put",
          data: { entries, expectedRevision: this.prefRevision },
        });
        this.prefRevision = r.data.revision;
        this.pinned = entries;
        this.editing = false;
      } catch {
        this.pinsError = "保存失败，选择已保留；如在其他设备修改过，请刷新页面后重试";
      } finally {
        this.savingPins = false;
      }
    },
    switchSpace(e) {
      this.mutations.contextChanged();
      this.spaceIndex = Number(e.detail.value);
      this.data = {};
      this.page = 1;
      this.expanded = false;
      this.load();
    },
    turn(delta) {
      this.page += delta;
      this.load();
    },
    load() {
      return this.loader.run({
        scope: this.spaceIndex === 0 ? "personal" : "group",
        groupId: this.spaceIndex === 0 ? undefined : this.spaces[this.spaceIndex - 1].id,
        page: this.page,
      });
    },
    retryCharge(f, action) {
      return this.mutations.run(`charge:${f.id}`, action, () => ({
        url: `/charge/instances/failures/${f.id}/${action}`,
        method: "post",
      }));
    },
    complete(t) {
      return this.mutations.run(`todo:${t.id}`, "complete", () => ({
        url: "/admin/toDo",
        method: "put",
        data: { id: t.id, status: true, kanbanStatus: 2 },
      }));
    },
    act(e, action) {
      return this.mutations.run(`reminder:${e.id}`, `${action}:${e.revision}`, ({ id, now }) => ({
        url: `/reminder/events/${e.id}/${action}`,
        method: "post",
        data: {
          requestId: id,
          expectedRevision: e.revision,
          until: action === "snooze" ? new Date(now + 3600000).toISOString() : null,
        },
      }));
    },
  },
};
</script>
<style scoped>
.actions {
  padding: 28rpx;
  margin: 24rpx;
  background: white;
  border-radius: 24rpx;
  color: #253045;
  box-shadow: 0 4rpx 20rpx rgba(37, 48, 69, 0.06);
  animation: actions-in 0.3s ease both;
}
.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12rpx;
  margin-bottom: 18rpx;
}
.head__left {
  display: flex;
  flex-direction: column;
}
.head__title {
  font-size: 36rpx;
  font-weight: 700;
  color: #123b70;
}
.head__date {
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #7d8ba1;
}
.head__right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8rpx;
  flex-wrap: wrap;
}
.refresh-btn {
  margin: 0;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}
.space-picker {
  padding: 10rpx 24rpx;
  font-size: 26rpx;
  color: #123b70;
  background: #f1f5fb;
  border-radius: 999rpx;
}
.pins {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-wrap: wrap;
  margin-bottom: 16rpx;
}
.pin {
  margin: 0;
  padding: 0 26rpx;
  font-size: 24rpx;
  font-weight: 600;
  line-height: 52rpx;
  color: #2f6bff;
  background: #eaf1ff;
  border: none;
  border-radius: 999rpx;
}
.pin--ghost {
  color: #7d8ba1;
  background: #f1f5fb;
}
.pins-editor {
  padding: 20rpx;
  margin-bottom: 16rpx;
  background: #f8fafc;
  border-radius: 16rpx;
}
.pins-editor button {
  margin: 12rpx 12rpx 0 0;
}
.hint {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #94a3b8;
}
.loading-note,
.error-note {
  display: block;
  margin-bottom: 10rpx;
  font-size: 24rpx;
  color: #7d8ba1;
}
.error-note {
  color: #e5484d;
}
.section {
  padding: 20rpx 0 8rpx;
  border-top: 1rpx solid #eef2f8;
}
.section__head {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 14rpx;
}
.section__title {
  font-size: 30rpx;
  font-weight: 600;
  color: #253045;
}
.dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
}
.dot--danger {
  background: #e5484d;
}
.dot--warning {
  background: #f5a623;
}
.dot--success {
  background: #2fbf71;
}
.dot--primary {
  background: #4f86f7;
}
.count {
  min-width: 36rpx;
  padding: 0 12rpx;
  font-size: 22rpx;
  line-height: 34rpx;
  color: #7d8ba1;
  text-align: center;
  background: #f1f5fb;
  border-radius: 999rpx;
}
.item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 20rpx;
  margin-bottom: 14rpx;
  background: #f8fafc;
  border: 1rpx solid transparent;
  border-radius: 16rpx;
  animation: item-in 0.25s ease both;
  animation-delay: calc(var(--i, 0) * 30ms);
}
.item--overdue {
  background: #fdecec;
}
.item--overdue .item__title {
  color: #e5484d;
}
.item--warning {
  background: #fdf6e7;
}
.item__main {
  flex: 1;
  min-width: 0;
}
.item__title {
  font-size: 28rpx;
  font-weight: 500;
  color: #253045;
}
.item__meta {
  display: flex;
  align-items: center;
  gap: 10rpx;
  flex-wrap: wrap;
  margin-top: 8rpx;
}
.item__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.item__actions button {
  margin: 0 0 0 10rpx;
}
.item__action {
  color: #fff !important;
  background: #4f86f7 !important;
}
.chip {
  padding: 2rpx 16rpx;
  font-size: 22rpx;
  border-radius: 999rpx;
}
.chip--plain {
  color: #7d8ba1;
  background: #eef2f8;
}
.chip--danger {
  color: #e5484d;
  background: #fdecec;
}
.chip--warning {
  color: #b97a0d;
  background: #fdf6e7;
}
.chip--primary {
  color: #2f6bff;
  background: #eaf1ff;
}
.tag {
  padding: 2rpx 14rpx;
  font-size: 22rpx;
  border-radius: 8rpx;
}
.tag--danger {
  color: #fff;
  background: #e5484d;
}
.tag--primary {
  color: #2f6bff;
  background: #eaf1ff;
}
.tag--plain {
  color: #7d8ba1;
  background: #eef2f8;
}
.link {
  font-size: 24rpx;
  color: #4f86f7;
}
.empty {
  display: block;
  padding: 10rpx 0;
  font-size: 24rpx;
  color: #94a3b8;
}
.more-btn {
  margin: 8rpx 0;
}
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  padding: 10rpx 0;
}
.pager button {
  margin: 0;
}
@keyframes actions-in {
  from {
    opacity: 0;
    transform: translateY(24rpx);
  }
  to {
    opacity: 1;
    transform: none;
  }
}
@keyframes item-in {
  from {
    opacity: 0;
    transform: translateY(12rpx);
  }
  to {
    opacity: 1;
    transform: none;
  }
}
</style>
