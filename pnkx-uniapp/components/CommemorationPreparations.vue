<template>
  <view v-if="items.length"
    ><view v-for="i in items" :key="i.id" class="cp-item"
      ><view class="cp-item__main"
        ><text class="cp-item__title">{{ i.name }}</text
        ><view class="cp-item__meta"
          ><text class="cp-chip">{{ i.occurrenceDate }}</text
          ><text class="cp-chip">{{ i.daysUntil === 0 ? "今天" : i.daysUntil + "天后" }}</text
          ><text class="cp-tag" :class="{ 'cp-tag--ready': ['accepted', 'completed'].includes(i.preparationStatus) }">{{
            labels[i.preparationStatus] || "尚未准备"
          }}</text></view
        ></view
      ><view v-if="canEdit && !['accepted', 'completed', 'removed'].includes(i.preparationStatus)" class="cp-item__actions"
        ><button size="mini" class="cp-btn" @click="open(i)">
          {{ i.preparationStatus === "dismissed" ? "重新准备" : "创建待办" }}</button
        ><button v-if="i.preparationStatus !== 'dismissed'" size="mini" @click="dismiss(i)">
          忽略本次
        </button></view
      ></view
    >
    <view v-if="selected" class="cp-editor"
      ><text>准备 {{ selected.name }}，将创建你负责的待办</text
      ><textarea v-model="form.content" maxlength="2000" placeholder="具体准备事项" /><picker
        mode="date"
        :value="form.dueDate"
        :end="selected.occurrenceDate"
        @change="form.dueDate = $event.detail.value"
        ><view>完成日期 {{ form.dueDate }}</view></picker
      ><text v-if="error">{{ error }}</text
      ><button :loading="busy" @click="confirm">预览并确认</button
      ><button @click="selected = null">取消</button></view
    >
  </view>
</template>
<script>
import request from "@/utils/request";
export default {
  props: { items: { type: Array, default: () => [] }, canEdit: { type: Boolean, default: false } },
  emits: ["changed"],
  data: () => ({
    selected: null,
    form: {},
    busy: false,
    error: "",
    preview: null,
    labels: {
      draft: "待确认",
      accepted: "准备中",
      completed: "准备完成",
      dismissed: "已忽略本次",
      removed: "准备待办已移除",
    },
  }),
  methods: {
    open(i) {
      this.selected = i;
      this.form = {
        content: `准备${i.name}`,
        dueDate: i.suggestedDueDate,
        occurrenceDate: i.occurrenceDate,
      };
      this.preview = null;
      this.error = "";
    },
    async confirm() {
      if (this.busy) return;
      this.busy = true;
      try {
        const signature = JSON.stringify(this.form);
        if (!this.preview || this.preview.signature !== signature) {
          const r = await request({
            url: `/life/action-center/commemorations/${this.selected.id}/preview`,
            method: "post",
            data: this.form,
          });
          this.preview = { signature, ...r.data };
        }
        const ok = await new Promise((resolve) =>
          uni.showModal({
            title: "确认创建准备待办",
            content: `${this.preview.content}，完成日期：${this.preview.dueDate}`,
            success: (r) => resolve(r.confirm),
            fail: () => resolve(false),
          })
        );
        if (!ok) return;
        await request({
          url: "/life/action-center/commemorations/commit",
          method: "post",
          data: { previewId: this.preview.previewId },
        });
        this.selected = null;
        this.$emit("changed");
      } catch (_) {
        this.error = "未确认成功，可重试；日期或来源变化时请收起并刷新再预览";
      } finally {
        this.busy = false;
      }
    },
    async dismiss(i) {
      await request({
        url: `/life/action-center/commemorations/${i.id}/dismiss`,
        method: "post",
        data: { occurrenceDate: i.occurrenceDate },
      });
      this.$emit("changed");
    },
  },
};
</script>
<style scoped>
.cp-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 20rpx;
  margin-bottom: 14rpx;
  background: #f6f2ff;
  border-radius: 16rpx;
  animation: cp-in 0.25s ease both;
}
.cp-item__main {
  flex: 1;
  min-width: 0;
}
.cp-item__title {
  font-size: 28rpx;
  font-weight: 500;
  color: #253045;
}
.cp-item__meta {
  display: flex;
  align-items: center;
  gap: 10rpx;
  flex-wrap: wrap;
  margin-top: 8rpx;
}
.cp-item__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.cp-btn {
  color: #9a6bff !important;
  background: #f1ebff !important;
}
.cp-chip {
  padding: 2rpx 16rpx;
  font-size: 22rpx;
  color: #7d8ba1;
  background: #eef2f8;
  border-radius: 999rpx;
}
.cp-tag {
  padding: 2rpx 14rpx;
  font-size: 22rpx;
  color: #9a6bff;
  background: #f1ebff;
  border-radius: 8rpx;
}
.cp-tag--ready {
  color: #2fbf71;
  background: #e8f8f0;
}
.cp-editor {
  padding: 20rpx;
  margin-top: 12rpx;
  background: #f8fafc;
  border-radius: 16rpx;
}
.cp-editor text {
  display: block;
}
.cp-editor textarea {
  width: 100%;
  margin: 12rpx 0;
}
@keyframes cp-in {
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
