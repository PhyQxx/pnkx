<template>
  <view class="automation-page subpage-shell">
    <scroll-view scroll-x class="templates">
      <view class="templates__inner">
        <view v-for="item in templates" :key="item.code" class="template" @click="useTemplate(item)">
          <text class="template__name">{{ item.name }}</text>
          <text class="template__desc">{{ item.description }}</text>
        </view>
      </view>
    </scroll-view>

    <view v-if="!rules.length" class="empty">选择上方模板创建第一条自动化规则</view>
    <view v-for="rule in rules" :key="rule.id" class="rule-card">
      <view class="rule-card__top">
        <view><text class="rule-card__name">{{ rule.name }}</text><text class="rule-card__meta">{{ triggerLabel(rule.triggerType) }} → {{ actionLabel(rule.actionType) }}</text></view>
        <switch :checked="rule.enabled" color="#6C63FF" @change="toggle(rule, $event.detail.value)" />
      </view>
      <view class="rule-card__actions">
        <text @click="run(rule, true)">试运行</text><text @click="run(rule, false)">立即执行</text>
        <text @click="openHistory(rule)">记录</text><text @click="edit(rule)">编辑</text><text class="danger" @click="remove(rule)">删除</text>
      </view>
    </view>

    <view class="fab" @click="createBlank">＋</view>

    <uni-popup ref="editor" type="bottom">
      <view class="editor">
        <text class="editor__title">{{ form.id ? '编辑规则' : '新建规则' }}</text>
        <uni-easyinput v-model="form.name" placeholder="规则名称" />
        <picker :range="triggerOptions" range-key="label" @change="form.triggerType = triggerOptions[$event.detail.value].value">
          <view class="picker-row">触发器：{{ triggerLabel(form.triggerType) }}</view>
        </picker>
        <picker :range="actionOptions" range-key="label" @change="form.actionType = actionOptions[$event.detail.value].value">
          <view class="picker-row">动作：{{ actionLabel(form.actionType) }}</view>
        </picker>
        <template v-if="form.triggerType === 'schedule'">
          <uni-datetime-picker v-model="form.nextRunTime" type="datetime" />
          <textarea v-model="form.triggerConfig" class="json-input" :placeholder="placeholders.triggerConfig" />
        </template>
        <textarea v-model="form.conditionJson" class="json-input" :placeholder="placeholders.conditionJson" />
        <textarea v-model="form.actionConfig" class="json-input" :placeholder="placeholders.actionConfig" />
        <button type="primary" @click="submit">保存规则</button>
      </view>
    </uni-popup>

    <uni-popup ref="history" type="bottom">
      <view class="history">
        <text class="editor__title">执行记录</text>
        <view v-for="item in executions" :key="item.id" class="execution">
          <view><text :class="['status', item.status]">{{ statusLabel(item.status) }}</text><text class="execution__time">{{ item.createTime }}</text></view>
          <text v-if="item.errorMsg" class="execution__error">{{ item.errorMsg }}</text>
          <text v-if="item.status === 'failed'" class="retry" @click="retry(item)">重放</text>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script>
import { listRules, listTemplates, saveRule, deleteRule, runRule, dryRunRule, listExecutions, retryExecution } from '@/api/px/life/automation'

const emptyForm = () => ({ name: '', templateCode: '', triggerType: 'manual', triggerConfig: '{}', conditionJson: '{}', actionType: 'create_todo', actionConfig: '{}', enabled: true })
export default {
  data() {
    return {
      rules: [], templates: [], executions: [], form: emptyForm(),
      // WXML 属性值里不能出现字面量 { } 和 "，示例文案必须走绑定
      placeholders: {
        triggerConfig: '触发配置 JSON，例如 {"intervalMinutes":1440}',
        conditionJson: '条件 JSON，例如 {"daysBefore":7}',
        actionConfig: '动作配置 JSON，例如 {"content":"准备礼物"}'
      },
      triggerOptions: [{ label: '手动', value: 'manual' }, { label: '定时', value: 'schedule' }, { label: '订阅到期', value: 'subscription_due' }, { label: '待办完成', value: 'todo_completed' }],
      actionOptions: [{ label: '订阅扣费', value: 'subscription_charge' }, { label: '餐饮转购物', value: 'meal_to_shopping' }, { label: '创建待办', value: 'create_todo' }, { label: '创建阅读待办', value: 'create_reading_todo' }, { label: '待办转日记', value: 'todo_to_diary' }]
    }
  },
  onLoad() { this.load() },
  methods: {
    async load() { const [r, t] = await Promise.all([listRules(), listTemplates()]); this.rules = r.data || []; this.templates = t.data || [] },
    triggerLabel(value) { return this.triggerOptions.find(i => i.value === value)?.label || value },
    actionLabel(value) { return this.actionOptions.find(i => i.value === value)?.label || value },
    createBlank() { this.form = emptyForm(); this.$refs.editor.open() },
    useTemplate(item) { this.form = { ...emptyForm(), name: item.name, templateCode: item.code, triggerType: item.triggerType, actionType: item.actionType }; this.$refs.editor.open() },
    edit(rule) { this.form = { ...rule, conditionJson: rule.conditionJson || '{}', actionConfig: rule.actionConfig || '{}', triggerConfig: rule.triggerConfig || '{}' }; this.$refs.editor.open() },
    async submit() {
      if (!this.form.name.trim()) return uni.showToast({ title: '请输入规则名称', icon: 'none' })
      try { JSON.parse(this.form.conditionJson || '{}'); JSON.parse(this.form.actionConfig || '{}'); JSON.parse(this.form.triggerConfig || '{}') } catch (e) { return uni.showToast({ title: 'JSON 配置格式错误', icon: 'none' }) }
      if (this.form.triggerType === 'schedule' && !this.form.nextRunTime) return uni.showToast({ title: '请选择首次运行时间', icon: 'none' })
      await saveRule(this.form); this.$refs.editor.close(); uni.showToast({ title: '已保存', icon: 'success' }); this.load()
    },
    async toggle(rule, enabled) { await saveRule({ ...rule, enabled }); this.load() },
    async run(rule, dry) { const res = await (dry ? dryRunRule(rule.id) : runRule(rule.id)); const ok = ['success', 'preview'].includes(res.data?.status); uni.showToast({ title: ok ? (dry ? '试运行完成' : '执行成功') : '执行失败', icon: ok ? 'success' : 'none' }) },
    remove(rule) { uni.showModal({ title: '删除规则', content: `确定删除“${rule.name}”吗？`, success: async e => { if (e.confirm) { await deleteRule(rule.id); this.load() } } }) },
    async openHistory(rule) { const res = await listExecutions(rule.id); this.executions = res.data || []; this.$refs.history.open() },
    async retry(item) { await retryExecution(item.id); const res = await listExecutions(item.ruleId); this.executions = res.data || [] },
    statusLabel(status) { return { success: '成功', failed: '失败', preview: '试运行', running: '执行中' }[status] || status }
  }
}
</script>

<style lang="scss" scoped>
.automation-page { min-height: 100vh; padding: 24rpx $page-padding 160rpx; background: $bg-page; }
.templates { white-space: nowrap; margin-bottom: 24rpx; }
.templates__inner { display: inline-flex; gap: 16rpx; }
.template { width: 300rpx; padding: 24rpx; border-radius: $radius-lg; background: linear-gradient(135deg, rgba($primary,.15), $bg-card); white-space: normal; }
.template__name,.template__desc { display:block; }.template__name { color:$text-primary;font-weight:600 }.template__desc { margin-top:8rpx;color:$text-tertiary;font-size:$font-mini; }
.rule-card { margin-bottom:16rpx;padding:26rpx;border-radius:$radius-lg;background:$bg-card;box-shadow:$shadow-card; }
.rule-card__top { display:flex;justify-content:space-between;align-items:center }.rule-card__name,.rule-card__meta { display:block }.rule-card__name { color:$text-primary;font-weight:600 }.rule-card__meta { margin-top:8rpx;color:$text-tertiary;font-size:$font-mini }
.rule-card__actions { display:flex;gap:24rpx;margin-top:22rpx;color:$primary;font-size:$font-mini }.danger,.execution__error { color:#EF4444 }
.empty { padding:120rpx 0;text-align:center;color:$text-tertiary }.fab { position:fixed;right:40rpx;bottom:60rpx;width:96rpx;height:96rpx;border-radius:50%;background:$primary;color:#fff;font-size:50rpx;line-height:96rpx;text-align:center;box-shadow:$shadow-lg }
.editor,.history { max-height:75vh;padding:34rpx;border-radius:28rpx 28rpx 0 0;background:$bg-card }.editor { display:flex;flex-direction:column;gap:20rpx }.editor__title { color:$text-primary;font-size:$font-h2;font-weight:700 }.picker-row,.json-input { padding:20rpx;border-radius:$radius-md;background:$bg-page;color:$text-secondary }.json-input { width:auto;min-height:120rpx;font-family:monospace }
.execution { position:relative;padding:20rpx 0;border-bottom:1px solid $gray-100 }.execution__time { margin-left:16rpx;color:$text-tertiary;font-size:$font-mini }.status { color:$text-secondary }.status.success { color:#10B981 }.status.failed { color:#EF4444 }.retry { position:absolute;right:0;top:20rpx;color:$primary }
</style>
