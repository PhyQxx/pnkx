<template>
  <view v-if="schema.length" class="draft-editor"
    ><button v-if="!editing" size="mini" :disabled="disabled" @click="begin">修改草稿</button
    ><template v-else
      ><text>保存后生成新预览，旧草稿失效。请再次核对后确认。</text
      ><view v-for="f in schema" :key="f.key" class="field"
        ><text>{{ f.label }}</text
        ><switch
          v-if="f.boolean"
          :checked="values[f.key]"
          :disabled="saving"
          @change="values[f.key] = $event.detail.value" /><textarea
          v-else-if="f.multiline"
          v-model="values[f.key]"
          :disabled="saving"
          :maxlength="-1" /><input
          v-else
          v-model="values[f.key]"
          :disabled="saving"
          :maxlength="-1" /></view
      ><text v-if="error">{{ error }}</text
      ><button :loading="saving" :disabled="saving" @click="save">保存并重新预览</button
      ><button :disabled="saving" @click="finish">取消修改</button></template
    ></view
  >
</template>
<script>
import request from "@/utils/request";
import { editableFields, draftValues, revisionBody } from "@/utils/planEditing";
export default {
  props: { plan: Object, disabled: Boolean },
  emits: ["updated", "editing"],
  data() {
    return { editing: false, saving: false, values: {}, error: "" };
  },
  computed: {
    schema() {
      return editableFields(this.plan);
    },
  },
  watch: {
    "plan.id"() {
      this.finish();
    },
  },
  methods: {
    begin() {
      this.values = draftValues(this.plan);
      this.editing = true;
      this.error = "";
      this.$emit("editing", true);
    },
    finish() {
      this.editing = false;
      this.$emit("editing", false);
    },
    async save() {
      if (this.saving) return;
      this.saving = true;
      this.error = "";
      try {
        const r = await request({
          url: `/operation-plans/${this.plan.id}/revise`,
          method: "post",
          data: revisionBody(this.plan, this.values),
        });
        this.finish();
        this.$emit("updated", r.data);
      } catch (e) {
        this.error = e?.message || "保存失败，草稿已保留，请重试";
      } finally {
        this.saving = false;
      }
    },
  },
};
</script>
<style scoped>
.draft-editor {
  padding: 24rpx;
  background: #f8fafc;
  border-radius: 14rpx;
  margin: 20rpx 0;
  font-size: 26rpx;
}
.field {
  margin: 20rpx 0;
}
.field text {
  display: block;
  margin-bottom: 12rpx;
}
.field input,
.field textarea {
  background: #fff;
  padding: 16rpx;
  border: 1px solid #cbd5e1;
  border-radius: 8rpx;
  width: auto;
}
.field textarea {
  width: calc(100% - 34rpx);
}
button {
  margin-top: 16rpx;
}
</style>
