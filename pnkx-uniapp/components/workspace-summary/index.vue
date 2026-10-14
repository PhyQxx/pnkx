<template><view class="summary"><view class="heading" @click="openHub"><text>我的资料</text><text>资料中心 →</text></view><text v-if="count !== null" @click="openInbox">{{ displayCount }} 条未整理资料</text><view v-for="row in recent" :key="row.type + ':' + row.id" class="recent" @click="open(row)">继续阅读：{{ row.title }}</view><text v-if="error">{{ error }}</text></view></template>
<script>
import { listResources } from '@/api/px/life/workspace'
export default {
  data() { return { count: null, displayCount: 0, recent: [], error: '' } },
  beforeDestroy() { clearInterval(this._t) },
  mounted() { this.reload() },
  methods: {
    async reload() { try { const [inbox, recent] = await Promise.all([listResources({ untagged: true, pageSize: 1 }),listResources({ recent: true, pageSize: 3 })]); this.count = inbox.data.total; this.tweenCount(); this.recent = recent.data.rows; this.error = '' } catch { this.error = '资料暂时无法加载' } },
    openHub() { uni.switchTab({ url: '/pages/workspace/index' }) }, openInbox() { try { uni.setStorageSync('ws:filter', JSON.stringify({ untagged: true })) } catch (e) {} uni.switchTab({ url: '/pages/workspace/index' }) },
    tweenCount() {
      const to = this.count || 0
      const from = this.displayCount || 0
      clearInterval(this._t)
      const steps = 20
      let i = 0
      this._t = setInterval(() => {
        i++
        const p = i / steps
        const eased = 1 - Math.pow(1 - p, 3)
        this.displayCount = Math.round(from + (to - from) * eased)
        if (i >= steps) clearInterval(this._t)
      }, 20)
    },
    open(row) { uni.navigateTo({ url: `/pages_life/workspace/resource?type=${row.type}&id=${row.id}` }) }
  }
}
</script>
<style scoped lang="scss">.summary { background: $bg-card; border-radius: 24rpx; padding: 26rpx; margin-bottom: 24rpx; }.heading { display: flex; justify-content: space-between; font-weight: 600; margin-bottom: 18rpx; }.recent { margin-top: 16rpx; color: $primary; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }</style>
