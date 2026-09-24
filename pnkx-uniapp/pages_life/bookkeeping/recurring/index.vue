<template>
  <view class="recurring-page subpage-shell">
    <view class="intro">
      <text class="intro__title">固定收支，自动入账</text>
      <text class="intro__text">房租、工资、会员等规则会由系统每日检查，到期后自动生成账目。</text>
    </view>

    <view v-if="!loading && rules.length === 0" class="empty-card">暂无周期规则</view>
    <view v-for="rule in rules" :key="rule.id" class="rule-card" @click="edit(rule)">
      <view class="rule-card__top">
        <view>
          <text class="rule-card__name">{{ rule.name }}</text>
          <text class="rule-card__cycle">{{ cycleText(rule) }}</text>
        </view>
        <text class="rule-card__money">¥{{ Number(rule.money || 0).toFixed(2) }}</text>
      </view>
      <view class="rule-card__meta">
        <text>{{ rule.typeDifference === '2' ? '转账' : (rule.typeName || '-') }} · {{ rule.accountName || '-' }}</text>
        <text>下次 {{ dateText(rule.nextRunDate) }}</text>
      </view>
      <view class="rule-card__actions" @click.stop>
        <switch :checked="Boolean(rule.enabled)" color="#4F86F7" @change="toggle(rule, $event)" />
        <view class="delete-link" @click="remove(rule)">删除</view>
      </view>
    </view>

    <view class="fab" @click="create"><uni-icons type="plus" size="28" color="#fff" /></view>

    <uni-popup ref="editor" type="bottom">
      <scroll-view scroll-y class="editor">
        <view class="editor__header">
          <text @click="$refs.editor.close()">取消</text>
          <text class="editor__title">{{ form.id ? '编辑规则' : '新增规则' }}</text>
          <text class="editor__save" @click="save">保存</text>
        </view>
        <view class="field"><text class="field__label">名称</text><input v-model="form.name" class="field__input" placeholder="如：房租" /></view>
        <picker :range="typeLabels" :value="typeIndex" @change="onTypeChange">
          <view class="field"><text class="field__label">类型</text><text>{{ typeLabels[typeIndex] }}</text></view>
        </picker>
        <picker :range="frequencyLabels" :value="frequencyIndex" @change="onFrequencyChange">
          <view class="field"><text class="field__label">频率</text><text>{{ frequencyLabels[frequencyIndex] }}</text></view>
        </picker>
        <picker :range="dayLabels" :value="dayIndex" @change="onDayChange">
          <view class="field"><text class="field__label">执行日</text><text>{{ dayLabels[dayIndex] }}</text></view>
        </picker>
        <picker v-if="form.typeDifference !== '2'" :range="categoryLabels" :value="categoryIndex" @change="onCategoryChange">
          <view class="field"><text class="field__label">分类</text><text>{{ categoryLabels[categoryIndex] || '请选择' }}</text></view>
        </picker>
        <picker :range="accountLabels" :value="accountIndex" @change="onAccountChange">
          <view class="field"><text class="field__label">{{ form.typeDifference === '2' ? '转出账户' : '账户' }}</text><text>{{ accountLabels[accountIndex] || '请选择' }}</text></view>
        </picker>
        <picker v-if="form.typeDifference === '2'" :range="accountLabels" :value="otherAccountIndex" @change="onOtherAccountChange">
          <view class="field"><text class="field__label">转入账户</text><text>{{ accountLabels[otherAccountIndex] || '请选择' }}</text></view>
        </picker>
        <view class="field"><text class="field__label">金额</text><input v-model="form.money" type="digit" class="field__input" placeholder="0.00" /></view>
        <view class="field"><text class="field__label">备注</text><input v-model="form.remark" class="field__input" placeholder="选填" /></view>
      </scroll-view>
    </uni-popup>
  </view>
</template>

<script>
import { addRecurring, delRecurring, listRecurring, toggleRecurring, updateRecurring } from '@/api/px/life/bookkeeping/recurring'
import { getClassificationList } from '@/api/px/life/bookkeeping/classification'
import { getAccountList } from '@/api/px/life/bookkeeping/account'

export default {
  data() {
    return {
      loading: false, rules: [], categories: [], accounts: [],
      typeLabels: ['支出', '收入', '转账'], typeValues: ['1', '0', '2'], typeIndex: 0,
      frequencyLabels: ['每月', '每周'], frequencyValues: ['month', 'week'], frequencyIndex: 0,
      dayIndex: 0, categoryIndex: 0, accountIndex: 0, otherAccountIndex: 0,
      form: {}
    }
  },
  computed: {
    dayLabels() {
      if (this.form.frequency === 'week') return ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
      return Array.from({ length: 28 }, (_, i) => (i + 1) + '号')
    },
    categoryLabels() { return this.categories.map(item => item.typeName) },
    accountLabels() { return this.accounts.map(item => item.accountName) }
  },
  onLoad() { this.resetForm(); this.loadReferences(); this.load() },
  onShow() { this.load() },
  methods: {
    resetForm() {
      this.form = { id: null, name: '', typeDifference: '1', frequency: 'month', dayNumber: 1, type: null, account: null, otherAccount: null, money: '', remark: '' }
      this.typeIndex = 0; this.frequencyIndex = 0; this.dayIndex = 0; this.categoryIndex = 0; this.accountIndex = 0; this.otherAccountIndex = 0
    },
    async load() {
      this.loading = true
      try { const res = await listRecurring(); this.rules = res.data || [] } finally { this.loading = false }
    },
    async loadReferences() {
      const [classRes, accountRes] = await Promise.all([
        getClassificationList({ typeDifference: this.form.typeDifference === '2' ? '1' : this.form.typeDifference }),
        getAccountList({})
      ])
      this.categories = (classRes.data || []).reduce((all, item) => all.concat(item.children || (item.typeLevel === '1' ? [item] : [])), [])
      this.accounts = (accountRes.data || []).reduce((all, item) => all.concat(item.children || (item.id ? [item] : [])), [])
      if (!this.form.type && this.categories[0]) this.form.type = this.categories[0].id
      if (!this.form.account && this.accounts[0]) this.form.account = this.accounts[0].id
      if (!this.form.otherAccount && this.accounts[1]) { this.form.otherAccount = this.accounts[1].id; this.otherAccountIndex = 1 }
      this.syncPickerIndexes()
    },
    syncPickerIndexes() {
      this.typeIndex = Math.max(0, this.typeValues.indexOf(String(this.form.typeDifference)))
      this.frequencyIndex = Math.max(0, this.frequencyValues.indexOf(this.form.frequency))
      this.dayIndex = Math.max(0, Number(this.form.dayNumber || 1) - 1)
      this.categoryIndex = Math.max(0, this.categories.findIndex(item => item.id === this.form.type))
      this.accountIndex = Math.max(0, this.accounts.findIndex(item => item.id === this.form.account))
      this.otherAccountIndex = Math.max(0, this.accounts.findIndex(item => item.id === this.form.otherAccount))
    },
    create() { this.resetForm(); this.loadReferences().then(() => this.$refs.editor.open()) },
    edit(rule) {
      this.form = { id: rule.id, name: rule.name, typeDifference: String(rule.typeDifference), frequency: rule.frequency, dayNumber: rule.dayNumber, type: rule.type, account: rule.account, otherAccount: rule.otherAccount, money: String(rule.money || ''), remark: rule.remark || '' }
      this.loadReferences().then(() => this.$refs.editor.open())
    },
    async onTypeChange(e) {
      this.typeIndex = Number(e.detail.value); this.form.typeDifference = this.typeValues[this.typeIndex]
      this.form.type = null; await this.loadReferences()
    },
    onFrequencyChange(e) { this.frequencyIndex = Number(e.detail.value); this.form.frequency = this.frequencyValues[this.frequencyIndex]; this.form.dayNumber = 1; this.dayIndex = 0 },
    onDayChange(e) { this.dayIndex = Number(e.detail.value); this.form.dayNumber = this.dayIndex + 1 },
    onCategoryChange(e) { this.categoryIndex = Number(e.detail.value); this.form.type = this.categories[this.categoryIndex].id },
    onAccountChange(e) { this.accountIndex = Number(e.detail.value); this.form.account = this.accounts[this.accountIndex].id },
    onOtherAccountChange(e) { this.otherAccountIndex = Number(e.detail.value); this.form.otherAccount = this.accounts[this.otherAccountIndex].id },
    async save() {
      if (!this.form.name.trim()) return uni.showToast({ title: '请输入规则名称', icon: 'none' })
      if (!Number(this.form.money)) return uni.showToast({ title: '请输入有效金额', icon: 'none' })
      if (!this.form.account) return uni.showToast({ title: '请选择账户', icon: 'none' })
      if (this.form.typeDifference !== '2' && !this.form.type) return uni.showToast({ title: '请选择分类', icon: 'none' })
      if (this.form.typeDifference === '2' && (!this.form.otherAccount || this.form.otherAccount === this.form.account)) return uni.showToast({ title: '请选择不同的转入账户', icon: 'none' })
      const payload = { ...this.form, money: Number(this.form.money), type: this.form.typeDifference === '2' ? 0 : this.form.type }
      await (payload.id ? updateRecurring(payload) : addRecurring(payload))
      this.$refs.editor.close(); uni.showToast({ title: '已保存', icon: 'success' }); this.load()
    },
    async toggle(rule, e) { await toggleRecurring(rule.id, Boolean(e.detail.value)); this.load() },
    remove(rule) {
      uni.showModal({ title: '删除规则', content: '已生成的历史账目不会删除，是否继续？', success: async res => {
        if (!res.confirm) return
        await delRecurring(rule.id); this.load()
      } })
    },
    cycleText(rule) { return rule.frequency === 'week' ? '每周' + this.dayLabelsForWeek(rule.dayNumber) : '每月' + rule.dayNumber + '号' },
    dayLabelsForWeek(day) { return ['一', '二', '三', '四', '五', '六', '日'][day - 1] || '' },
    dateText(value) { return value ? String(value).slice(0, 10) : '-' }
  }
}
</script>

<style lang="scss" scoped>
.recurring-page { min-height: 100vh; padding: $page-padding; padding-bottom: 160rpx; background: $bg-page; box-sizing: border-box; }
.intro { padding: $spacing-xl; margin: $spacing-md 0 $spacing-xl; border-radius: $radius-xl; color: #fff; background: linear-gradient(135deg, $primary, $primary-dark); }
.intro__title, .intro__text { display: block; }.intro__title { font-size: $font-h2; font-weight: $font-weight-semibold; }.intro__text { margin-top: $spacing-sm; font-size: $font-caption; line-height: 1.7; opacity: .86; }
.empty-card, .rule-card { padding: $spacing-lg; margin-bottom: $spacing-md; border-radius: $radius-xl; background: $bg-card; box-shadow: $shadow-card; }.empty-card { text-align: center; color: $text-tertiary; }
.rule-card__top, .rule-card__meta, .rule-card__actions { display: flex; justify-content: space-between; align-items: center; }
.rule-card__name, .rule-card__cycle { display: block; }.rule-card__name { font-size: $font-body; font-weight: $font-weight-semibold; color: $text-primary; }.rule-card__cycle { margin-top: 6rpx; font-size: $font-caption; color: $text-tertiary; }
.rule-card__money { font-size: $font-h2; color: $primary; font-weight: $font-weight-semibold; }.rule-card__meta { margin-top: $spacing-md; color: $text-secondary; font-size: $font-caption; }
.rule-card__actions { margin-top: $spacing-md; padding-top: $spacing-sm; border-top: 1rpx solid $border-light; }.delete-link { color: $danger; padding: $spacing-xs; }
.fab { position: fixed; right: 40rpx; bottom: 60rpx; width: 104rpx; height: 104rpx; display: flex; align-items: center; justify-content: center; border-radius: $radius-full; background: $primary; box-shadow: $shadow-lg; }
.editor { max-height: 82vh; padding: $spacing-lg $page-padding calc($spacing-xl + env(safe-area-inset-bottom)); border-radius: $radius-2xl $radius-2xl 0 0; background: $bg-card; box-sizing: border-box; }
.editor__header, .field { display: flex; align-items: center; justify-content: space-between; }.editor__header { margin-bottom: $spacing-lg; color: $text-secondary; }.editor__title { font-size: $font-h3; font-weight: $font-weight-semibold; color: $text-primary; }.editor__save { color: $primary; }
.field { min-height: 92rpx; border-bottom: 1rpx solid $border-light; color: $text-primary; }.field__label { color: $text-secondary; }.field__input { flex: 1; text-align: right; }
</style>
