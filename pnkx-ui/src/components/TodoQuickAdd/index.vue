<template>
  <el-dialog
    :model-value="modelValue"
    title="新建待办"
    width="440px"
    custom-class="modern-dialog"
    :modal-append-to-body="true"
    @update:model-value="v => $emit('update:modelValue', v)"
  >
    <el-form label-position="top" class="modern-form" @submit.prevent>
      <el-form-item label="内容">
        <el-input
          v-model="form.content"
          placeholder="例如：下班后去买菜..."
          @keyup.enter="submit"
        />
      </el-form-item>
      <el-form-item label="计划时间">
        <el-date-picker
          v-model="form.time"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始"
          end-placeholder="结束"
          align="right"
          style="width: 100%"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button class="btn-cancel" @click="$emit('update:modelValue', false)">取消</el-button>
        <el-button type="primary" class="btn-confirm" :loading="loading" :disabled="!form.content.trim()" @click="submit">新建</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import { addDo } from '@/api/px/life/todo'

/**
 * 快捷新建待办弹窗（共用组件）。
 * 与待办事项页同源 addDo 接口；保存成功 emit('saved') 由调用方刷新列表。
 */
export default {
  name: 'TodoQuickAdd',
  props: {
    modelValue: { type: Boolean, default: false }
  },
  emits: ['update:modelValue', 'saved'],
  data() {
    return {
      loading: false,
      form: { content: '', time: [] }
    }
  },
  watch: {
    modelValue(v) {
      if (v) this.form = { content: '', time: [] }
    }
  },
  methods: {
    submit() {
      if (!this.form.content.trim() || this.loading) return
      this.loading = true
      addDo({
        content: this.form.content,
        planStartTime: this.form.time ? this.parseTime(this.form.time[0]) : '',
        planEndTime: this.form.time ? this.parseTime(this.form.time[1]) : ''
      }).then(res => {
        if (res.code === 200) {
          this.$notify.success('新增成功')
          this.$emit('update:modelValue', false)
          this.$emit('saved')
        }
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>
