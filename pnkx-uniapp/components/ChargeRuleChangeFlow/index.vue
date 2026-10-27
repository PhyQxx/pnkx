<template>
  <view v-if="visible" class="flow-mask" @click="dismiss">
    <view class="flow" @click.stop>
      <view class="flow__header">
        <text class="flow__title">{{ task && task.kind === 'backfill' ? '补齐暂停期间账期' : '调整账期规则' }}</text>
        <text class="flow__close" @click="cancel">×</text>
      </view>

      <view class="flow__steps">
        <view v-for="(name, i) in ['变化摘要', '影响账期', '确认执行']" :key="name" class="step" :class="{ 'step--on': i === step, 'step--done': i < step }">
          <view class="step__dot"><text>{{ i < step ? '✓' : i + 1 }}</text></view>
          <text class="step__name">{{ name }}</text>
        </view>
      </view>

      <scroll-view scroll-y class="flow__body">
        <!-- 第 1 步:变化摘要 -->
        <template v-if="step === 0">
          <view v-if="task && task.kind === 'change'">
            <view v-for="d in diffs" :key="d.field" class="diff">
              <text class="diff__label">{{ d.label }}</text>
              <text class="diff__before">{{ d.before }}</text>
              <text class="diff__arrow">→</text>
              <text class="diff__after">{{ d.after }}</text>
            </view>
            <text v-if="!diffs.length" class="hint">没有检测到字段变化。</text>
            <view v-if="task && (task.preview.duplicates || []).length" class="warn">
              <text>存在同金额规则：{{ task.preview.duplicates.map((d) => d.name).join('、') }}，请核对是否重复并保留主规则。</text>
            </view>
          </view>
          <view v-else-if="task">
            <text class="lead">将补齐以下 {{ task.preview.periods.length }} 个暂停期间账期：</text>
            <view class="periods">
              <text v-for="p in task.preview.periods" :key="p" class="period">{{ p }}</text>
            </view>
            <text class="hint">每期预计 {{ task.preview.amountPerPeriod }} 元，只生成待确认账期，不直接增加支出。</text>
          </view>
        </template>

        <!-- 第 2 步:影响账期 -->
        <template v-else-if="step === 1">
          <view v-if="task && task.kind === 'change'">
            <text class="lead">该规则当前有 {{ affected.length }} 个未入账账期，将保留原金额快照，不受本次调整影响；之后的新账期按新规则生成。</text>
            <text v-if="affectedLoading" class="hint">正在加载账期…</text>
            <view v-if="affectedError" class="warn">
              <text>{{ affectedError }}</text>
            </view>
            <button v-if="affectedError" size="mini" class="retry" @click="loadAffected">重试加载</button>
            <view v-for="i in affected" :key="i.id" class="instance">
              <text class="instance__date">{{ i.dueDate }}</text>
              <text class="instance__amount">{{ i.expectedAmount }}</text>
              <text class="instance__status">{{ statusLabels[i.status] || i.status }}</text>
            </view>
          </view>
          <view v-else-if="task">
            <text class="lead">补齐后这些账期会出现在「周期账单」中，等待逐期确认入账：</text>
            <view class="periods">
              <text v-for="p in task.preview.periods" :key="p" class="period period--warn">{{ p }}</text>
            </view>
            <text class="hint">规则保持暂停；剩余超过 100 期的部分可再次预览补齐。</text>
          </view>
        </template>

        <!-- 第 3 步:确认执行 -->
        <template v-else>
          <text class="lead">{{ task && task.kind === 'change' ? '确认后立即生效：' : '确认后生成待确认账期：' }}</text>
          <view class="notice"><text>{{ task && task.preview.notice }}</text></view>
          <text class="hint">预览有效期约 15 分钟；来源若被再次修改，需要重新预览。</text>
          <view v-if="commitError" class="warn"><text>{{ commitError }}</text></view>
          <button v-if="commitError" size="mini" class="retry" @click="refreshPreview">重新预览</button>
        </template>
      </scroll-view>

      <view class="flow__footer">
        <button size="mini" class="btn" :disabled="busy" @click="cancel">取消</button>
        <button v-if="step > 0" size="mini" class="btn" :disabled="busy" @click="step--">上一步</button>
        <button
          v-if="step < 2"
          size="mini"
          class="btn btn--primary"
          :disabled="step === 0 && task && task.kind === 'change' && !diffs.length"
          @click="step++; step === 2 && (commitError = '')"
        >
          下一步
        </button>
        <button v-else size="mini" class="btn btn--primary" :loading="committing" @click="confirmCommit">确认执行</button>
      </view>
      <view class="safe-bottom"></view>
    </view>
  </view>
</template>
<script>
import request from "@/utils/request";
import {
  setRuleFlowPresenter,
  formatDiffs,
  CHANGE_PREVIEW_URL,
  BACKFILL_PREVIEW_URL,
  CHANGE_COMMIT_URL,
} from "@/utils/ruleChangeFlow";

export default {
  data() {
    return {
      visible: false,
      step: 0,
      task: null,
      affected: [],
      affectedLoading: false,
      affectedError: "",
      committing: false,
      commitError: "",
      statusLabels: { pending: "待确认", payment_failed: "扣费失败", planned: "按计划" },
      revoke: null,
      resolver: null,
    };
  },
  computed: {
    diffs() {
      return formatDiffs((this.task && this.task.preview && this.task.preview.changes) || []);
    },
    busy() {
      return this.affectedLoading || this.committing;
    },
  },
  mounted() {
    this.revoke = setRuleFlowPresenter((task) => this.open(task));
  },
  beforeUnmount() {
    if (this.revoke) this.revoke();
    if (this.resolver) this.resolver({ status: "cancelled" });
  },
  methods: {
    open(task) {
      return new Promise((resolve) => {
        this.task = task;
        this.step = 0;
        this.affected = [];
        this.affectedError = "";
        this.commitError = "";
        this.resolver = resolve;
        this.visible = true;
        if (task.kind === "change") this.loadAffected();
      });
    },
    async loadAffected() {
      this.affectedLoading = true;
      this.affectedError = "";
      try {
        const r = await request({
          url: "/charge/instances",
          method: "get",
          params: {
            sourceType: this.task.sourceType,
            sourceId: this.task.sourceId,
            pageNum: 1,
            pageSize: 100,
          },
        });
        const rows = r.rows || (r.data && r.data.rows) || [];
        this.affected = rows.filter((i) => ["pending", "payment_failed"].includes(i.status));
      } catch (e) {
        this.affectedError = "账期加载失败，可重试；不影响后续确认。";
        this.affected = [];
      } finally {
        this.affectedLoading = false;
      }
    },
    async confirmCommit() {
      if (this.committing) return;
      this.committing = true;
      this.commitError = "";
      try {
        const r = await request({
          url: CHANGE_COMMIT_URL,
          method: "post",
          data: { previewId: this.task.preview.previewId },
        });
        this.visible = false;
        uni.showToast({ title: this.task.kind === "change" ? "规则已更新" : "账期已补齐", icon: "none" });
        this.resolver && this.resolver({ status: "committed", result: r });
        this.resolver = null;
      } catch (e) {
        this.commitError = (e && e.message) || "提交失败，可重试同一预览或重新预览。";
      } finally {
        this.committing = false;
      }
    },
    async refreshPreview() {
      const url = this.task.kind === "change" ? CHANGE_PREVIEW_URL : BACKFILL_PREVIEW_URL;
      try {
        const r = await request({ url, method: "post", data: this.task.input });
        this.task.preview = r.data;
        this.commitError = "";
        this.step = 0;
      } catch (e) {
        this.commitError = "重新预览失败：" + ((e && e.message) || "请稍后重试");
      }
    },
    dismiss() {
      if (!this.busy) this.cancel();
    },
    cancel() {
      if (this.busy) return;
      this.visible = false;
      this.resolver && this.resolver({ status: "cancelled" });
      this.resolver = null;
    },
  },
};
</script>
<style scoped>
.flow-mask {
  position: fixed;
  inset: 0;
  z-index: 999;
  display: flex;
  align-items: flex-end;
  background: rgba(15, 23, 42, 0.45);
}
.flow {
  width: 100%;
  max-height: 82vh;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 28rpx 28rpx 0 0;
  animation: flow-in 0.24s ease;
}
@keyframes flow-in {
  from {
    transform: translateY(40rpx);
    opacity: 0;
  }
  to {
    transform: none;
    opacity: 1;
  }
}
.flow__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 26rpx 30rpx 10rpx;
}
.flow__title {
  font-size: 32rpx;
  font-weight: 700;
  color: #123b70;
}
.flow__close {
  font-size: 44rpx;
  line-height: 44rpx;
  color: #94a3b8;
  padding: 0 10rpx;
}
.flow__steps {
  display: flex;
  justify-content: space-around;
  padding: 16rpx 30rpx 20rpx;
  border-bottom: 1rpx solid #eef2f8;
}
.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}
.step__dot {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  color: #7d8ba1;
  background: #eef2f8;
}
.step--on .step__dot {
  color: #fff;
  background: #4f86f7;
}
.step--done .step__dot {
  color: #fff;
  background: #2fbf71;
}
.step__name {
  font-size: 22rpx;
  color: #7d8ba1;
}
.step--on .step__name {
  color: #2f6bff;
  font-weight: 600;
}
.flow__body {
  flex: 1;
  min-height: 0;
  padding: 24rpx 30rpx;
  box-sizing: border-box;
}
.lead {
  display: block;
  font-size: 28rpx;
  color: #253045;
  margin-bottom: 16rpx;
}
.hint {
  display: block;
  font-size: 24rpx;
  color: #7d8ba1;
  margin: 12rpx 0;
}
.diff {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f1f5fb;
}
.diff__label {
  width: 140rpx;
  font-size: 26rpx;
  color: #253045;
  font-weight: 600;
}
.diff__before {
  flex: 1;
  font-size: 24rpx;
  color: #7d8ba1;
}
.diff__arrow {
  color: #94a3b8;
}
.diff__after {
  flex: 1;
  font-size: 24rpx;
  color: #2f6bff;
  text-align: right;
}
.periods {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin: 8rpx 0;
}
.period {
  padding: 4rpx 18rpx;
  font-size: 24rpx;
  color: #2f6bff;
  background: #eaf1ff;
  border-radius: 999rpx;
}
.period--warn {
  color: #b97a0d;
  background: #fdf6e7;
}
.instance {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 14rpx 0;
  border-bottom: 1rpx solid #f1f5fb;
}
.instance__date {
  flex: 1;
  font-size: 26rpx;
  color: #253045;
}
.instance__amount {
  font-size: 26rpx;
  color: #253045;
}
.instance__status {
  font-size: 22rpx;
  color: #7d8ba1;
  background: #eef2f8;
  border-radius: 8rpx;
  padding: 2rpx 12rpx;
}
.warn {
  padding: 16rpx 20rpx;
  background: #fdf6e7;
  border-radius: 12rpx;
  font-size: 24rpx;
  color: #b97a0d;
  margin: 12rpx 0;
}
.notice {
  padding: 16rpx 20rpx;
  background: #f5f8fc;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #253045;
  margin-bottom: 8rpx;
}
.retry {
  margin: 8rpx 0;
}
.flow__footer {
  display: flex;
  gap: 16rpx;
  padding: 16rpx 30rpx;
  border-top: 1rpx solid #eef2f8;
}
.btn {
  flex: 1;
  margin: 0;
  color: #253045;
  background: #f1f5fb;
  border-radius: 999rpx;
}
.btn--primary {
  color: #fff !important;
  background: #4f86f7 !important;
}
.safe-bottom {
  height: env(safe-area-inset-bottom);
  width: 100%;
}
</style>
