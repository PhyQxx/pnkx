<template>
  <el-dialog
    :model-value="modelValue"
    title="剪藏网页"
    width="480px"
    custom-class="modern-dialog"
    :modal-append-to-body="true"
    @update:model-value="v => $emit('update:modelValue', v)"
  >
    <el-form label-position="top" class="modern-form" @submit.prevent>
      <el-form-item label="网页链接">
        <el-input v-model="form.url" placeholder="粘贴链接，自动抓取正文并 AI 生成摘要" @keyup.enter="submit" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选" />
      </el-form-item>
    </el-form>
    <div class="capture-hint">保存后将进入收件箱，稍后可在资料中心整理</div>
    <template #footer>
      <div class="dialog-footer">
        <el-button class="btn-cancel" @click="$emit('update:modelValue', false)">取消</el-button>
        <el-button type="primary" class="btn-confirm" :loading="saving" :disabled="!form.url.trim()" @click="submit">收藏</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import { addArticle } from '@/api/px/app/webArticle'

/**
 * 快捷剪藏弹窗（共用组件）。
 * 与网址收藏页同源 addArticle 接口（粘贴链接后台抓取 + AI 摘要）；成功 emit('saved')。
 */
export default {
  name: 'CaptureForm',
  props: {
    modelValue: { type: Boolean, default: false }
  },
  emits: ['update:modelValue', 'saved'],
  data() {
    return {
      saving: false,
      form: { url: '', remark: '' }
    }
  },
  watch: {
    modelValue(v) {
      if (v) this.form = { url: '', remark: '' }
    }
  },
  methods: {
    submit() {
      if (!this.form.url.trim() || this.saving) return
      this.saving = true
      addArticle({ url: this.form.url.trim(), remark: this.form.remark }).then(() => {
        this.$message.success('已收藏，正在后台抓取与 AI 分析')
        this.$emit('update:modelValue', false)
        this.$emit('saved')
      }).finally(() => {
        this.saving = false
      })
    }
  }
}
</script>

<style scoped>
.capture-hint { font-size: 12px; color: #94a3b8; margin-top: 4px; }
</style>
