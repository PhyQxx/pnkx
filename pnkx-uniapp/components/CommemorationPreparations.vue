<template>
  <view v-if="items.length"
    ><text>未来30天纪念日准备</text>
    <view v-for="i in items" :key="i.id" class="preparation"
      ><text
        >{{ i.name }} · {{ i.occurrenceDate }} ·
        {{ i.daysUntil === 0 ? "今天" : i.daysUntil + "天后" }}</text
      ><text>{{ labels[i.preparationStatus] || "尚未准备" }}</text>
      <view v-if="canEdit && !['accepted', 'completed', 'removed'].includes(i.preparationStatus)"
        ><button size="mini" @click="open(i)">
          {{ i.preparationStatus === "dismissed" ? "重新准备" : "创建准备待办" }}</button
        ><button v-if="i.preparationStatus !== 'dismissed'" size="mini" @click="dismiss(i)">
          忽略本次
        </button></view
      ></view
    >
    <view v-if="selected" class="preparation"
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
.preparation {
  padding: 20rpx 0;
  border-bottom: 1px solid #ddd;
}
.preparation text {
  display: block;
}
.preparation textarea {
  width: 100%;
  margin: 12rpx 0;
}
</style>
