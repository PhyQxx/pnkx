<template>
  <view class="budget-page subpage-shell">
    <view class="month-nav">
      <view class="month-nav__button" @click="changeMonth(-1)"><uni-icons type="left" size="18" color="#4F86F7" /></view>
      <text class="month-nav__label">{{ monthLabel }}</text>
      <view class="month-nav__button" :class="{ disabled: isCurrentMonth }" @click="changeMonth(1)">
        <uni-icons type="right" size="18" :color="isCurrentMonth ? '#C7D2E1' : '#4F86F7'" />
      </view>
    </view>

    <view v-if="!loading && budgets.length === 0" class="empty-card">
      <text class="empty-card__title">本月还没有预算</text>
      <text class="empty-card__hint">设置总预算或分类预算，系统会在接近上限时提醒你</text>
    </view>

    <view v-for="item in budgets" :key="item.id" class="budget-card" :class="{ exceeded: item.exceeded }">
      <view class="budget-card__header">
        <view>
          <text class="budget-card__name">{{ item.typeName || '总预算' }}</text>
          <text v-if="item.exceeded" class="budget-card__tag">已超支</text>
        </view>
        <text class="budget-card__amount">¥{{ money(item.used) }} / ¥{{ money(item.amount) }}</text>
      </view>
      <view class="progress"><view class="progress__value" :class="progressClass(item)" :style="{ width: progressWidth(item) }" /></view>
      <view class="budget-card__footer">
        <text>{{ item.exceeded ? '超出' : '剩余' }} ¥{{ money(Math.abs(Number(item.remaining || 0))) }}</text>
        <view class="delete-link" @click="remove(item)">删除</view>
      </view>
    </view>

    <view class="fab" @click="openEditor"><uni-icons type="plus" size="28" color="#fff" /></view>

    <uni-popup ref="editor" type="bottom">
      <view class="editor">
        <view class="editor__header">
          <text @click="$refs.editor.close()">取消</text>
          <text class="editor__title">设置预算</text>
          <text class="editor__save" @click="save">保存</text>
        </view>
        <view class="field"><text class="field__label">月份</text><text>{{ month }}</text></view>
        <picker :range="categoryLabels" :value="categoryIndex" @change="onCategoryChange">
          <view class="field"><text class="field__label">预算类型</text><text>{{ categoryLabels[categoryIndex] }}</text></view>
        </picker>
        <view class="field">
          <text class="field__label">预算金额</text>
          <input v-model="form.amount" type="digit" class="field__input" placeholder="请输入金额" />
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script>
import { delBudget, getBudgetStatus, saveBudget } from '@/api/px/life/bookkeeping/budget'
import { getClassificationList } from '@/api/px/life/bookkeeping/classification'

export default {
  data() {
    return {
      currentDate: new Date(), loading: false, budgets: [], categories: [], categoryIndex: 0,
      form: { typeId: 0, amount: '' }
    }
  },
  computed: {
    month() {
      return this.currentDate.getFullYear() + '-' + String(this.currentDate.getMonth() + 1).padStart(2, '0')
    },
    monthLabel() { return this.currentDate.getFullYear() + '年' + (this.currentDate.getMonth() + 1) + '月' },
    isCurrentMonth() {
      const now = new Date()
      return this.currentDate.getFullYear() === now.getFullYear() && this.currentDate.getMonth() === now.getMonth()
    },
    categoryLabels() { return ['总预算'].concat(this.categories.map(item => item.typeName)) }
  },
  onLoad() { this.loadCategories(); this.load() },
  onShow() { this.load() },
  methods: {
    async load() {
      this.loading = true
      try { const res = await getBudgetStatus(this.month); this.budgets = res.data || [] } finally { this.loading = false }
    },
    async loadCategories() {
      const res = await getClassificationList({ typeDifference: '1' })
      const roots = res.data || []
      this.categories = roots.reduce((all, item) => all.concat(item.children || (item.typeLevel === '1' ? [item] : [])), [])
    },
    changeMonth(delta) {
      if (delta > 0 && this.isCurrentMonth) return
      this.currentDate = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth() + delta, 1)
      this.load()
    },
    openEditor() {
      this.categoryIndex = 0
      this.form = { typeId: 0, amount: '' }
      this.$refs.editor.open()
    },
    onCategoryChange(e) {
      this.categoryIndex = Number(e.detail.value)
      this.form.typeId = this.categoryIndex === 0 ? 0 : this.categories[this.categoryIndex - 1].id
    },
    async save() {
      const amount = Number(this.form.amount)
      if (!amount || amount <= 0) return uni.showToast({ title: '请输入有效金额', icon: 'none' })
      await saveBudget({ month: this.month, typeId: this.form.typeId, amount })
      this.$refs.editor.close()
      uni.showToast({ title: '预算已保存', icon: 'success' })
      this.load()
    },
    remove(item) {
      uni.showModal({ title: '删除预算', content: '确定删除“' + (item.typeName || '总预算') + '”吗？', success: async res => {
        if (!res.confirm) return
        await delBudget(item.id); this.load()
      } })
    },
    money(value) { return Number(value || 0).toFixed(2) },
    progressWidth(item) { return Math.min(Number(item.percent || 0), 100) + '%' },
    progressClass(item) { return item.exceeded ? 'danger' : (Number(item.percent || 0) >= 80 ? 'warning' : '') }
  }
}
</script>

<style lang="scss" scoped>
.budget-page { min-height: 100vh; padding: $page-padding; padding-bottom: 160rpx; background: $bg-page; box-sizing: border-box; }
.month-nav { display: flex; align-items: center; justify-content: center; gap: $spacing-xl; margin: $spacing-md 0 $spacing-xl; }
.month-nav__button { width: 64rpx; height: 64rpx; display: flex; align-items: center; justify-content: center; border-radius: $radius-full; background: $bg-card; box-shadow: $shadow-xs; }
.month-nav__button.disabled { opacity: .55; }
.month-nav__label { font-size: $font-h2; font-weight: $font-weight-semibold; color: $text-primary; }
.empty-card, .budget-card { background: $bg-card; border-radius: $radius-xl; padding: $spacing-lg; margin-bottom: $spacing-md; box-shadow: $shadow-card; }
.empty-card { text-align: center; padding: 80rpx $spacing-lg; }
.empty-card__title, .empty-card__hint { display: block; }
.empty-card__title { color: $text-primary; font-size: $font-h3; }
.empty-card__hint { margin-top: $spacing-sm; color: $text-tertiary; font-size: $font-caption; line-height: 1.7; }
.budget-card__header, .budget-card__footer { display: flex; justify-content: space-between; align-items: center; }
.budget-card__name { font-size: $font-body; font-weight: $font-weight-semibold; color: $text-primary; }
.budget-card__tag { margin-left: $spacing-xs; padding: 4rpx 10rpx; border-radius: $radius-full; color: #fff; background: $danger; font-size: 20rpx; }
.budget-card__amount { font-size: $font-caption; color: $text-secondary; }
.progress { height: 14rpx; margin: $spacing-md 0; overflow: hidden; border-radius: $radius-full; background: $border-light; }
.progress__value { height: 100%; border-radius: inherit; background: $success; }
.progress__value.warning { background: $warning; }.progress__value.danger { background: $danger; }
.budget-card__footer { color: $text-tertiary; font-size: $font-caption; }.delete-link { color: $danger; padding: $spacing-xs; }
.fab { position: fixed; right: 40rpx; bottom: 60rpx; width: 104rpx; height: 104rpx; display: flex; align-items: center; justify-content: center; border-radius: $radius-full; background: $primary; box-shadow: $shadow-lg; }
.editor { padding: $spacing-lg $page-padding calc($spacing-xl + env(safe-area-inset-bottom)); border-radius: $radius-2xl $radius-2xl 0 0; background: $bg-card; }
.editor__header, .field { display: flex; align-items: center; justify-content: space-between; }.editor__header { margin-bottom: $spacing-lg; color: $text-secondary; }
.editor__title { color: $text-primary; font-size: $font-h3; font-weight: $font-weight-semibold; }.editor__save { color: $primary; }
.field { min-height: 92rpx; border-bottom: 1rpx solid $border-light; color: $text-primary; }.field__label { color: $text-secondary; }.field__input { flex: 1; text-align: right; }
</style>
