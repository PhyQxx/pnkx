<template>
  <view class="sync-page subpage-shell">
    <view class="summary">
      <view><text class="summary__value">{{ stats.pending }}</text><text>待同步</text></view>
      <view><text class="summary__value">{{ stats.syncing }}</text><text>同步中</text></view>
      <view><text class="summary__value summary__value--danger">{{ stats.conflict }}</text><text>需处理</text></view>
    </view>
    <button class="sync-now" :loading="syncing" @click="syncNow">立即同步</button>
    <view v-if="conflicts.length" class="conflicts">
      <text class="section-title">同步冲突</text>
      <view v-for="item in conflicts" :key="item.id" class="conflict-card">
        <text class="conflict-card__title">{{ moduleName(item.table_name) }} · {{ item.method }}</text>
        <text class="conflict-card__error">{{ item.error_msg || '同步失败，请选择处理方式' }}</text>
        <text class="conflict-card__time">{{ item.created_at }}</text>
        <view class="conflict-card__actions">
          <text @click="retry(item)">重试</text>
          <text @click="edit(item)">手工合并</text>
          <text @click="useServer(item)">使用服务端</text>
          <text class="danger" @click="discard(item)">丢弃</text>
        </view>
      </view>
    </view>
    <view v-else class="empty">没有需要处理的同步冲突</view>

    <uni-popup ref="mergePopup" type="bottom">
      <view class="merge-popup">
        <text class="section-title">编辑待同步数据</text>
        <textarea v-model="mergePayload" class="merge-popup__input" maxlength="-1" />
        <button class="sync-now" @click="saveMerge">保存并重试</button>
      </view>
    </uni-popup>
  </view>
</template>

<script>
import offlineQueue from '@/utils/offlineQueue'
import syncScheduler from '@/utils/syncScheduler'

export default {
  data() {
    return {
      stats: { pending: 0, syncing: 0, conflict: 0 },
      conflicts: [],
      syncing: false,
      editing: null,
      mergePayload: ''
    }
  },
  onShow() { this.load() },
  methods: {
    async load() {
      this.stats = await offlineQueue.getStats()
      this.conflicts = await offlineQueue.getConflictTasks()
    },
    moduleName(table) {
      const names = { px_diary: '日记', px_todo: '待办', px_bookkeeping_record: '账单', px_note: '笔记', px_commemoration_day: '纪念日' }
      return names[table] || table
    },
    async syncNow() {
      this.syncing = true
      try { await syncScheduler.triggerSync('manual'); await this.load() } finally { this.syncing = false }
    },
    async retry(item) { await offlineQueue.retry(item.id); await this.syncNow() },
    edit(item) {
      this.editing = item
      try { this.mergePayload = JSON.stringify(JSON.parse(item.payload || '{}'), null, 2) } catch (e) { this.mergePayload = '{}' }
      this.$refs.mergePopup.open()
    },
    async saveMerge() {
      try {
        const payload = JSON.parse(this.mergePayload)
        await offlineQueue.replacePayload(this.editing.id, payload)
        this.$refs.mergePopup.close()
        await this.syncNow()
      } catch (e) { uni.showToast({ title: 'JSON 格式错误', icon: 'none' }) }
    },
    async useServer(item) {
      await offlineQueue.discard(item.id, true)
      await syncScheduler.resetCursors([item.table_name])
      await syncScheduler.pullServerChanges()
      await this.load()
    },
    discard(item) {
      uni.showModal({ title: '丢弃本地变更', content: '此操作不可撤销，确定继续？', success: async res => {
        if (!res.confirm) return
        await offlineQueue.discard(item.id)
        await this.load()
      } })
    }
  }
}
</script>

<style lang="scss" scoped>
.sync-page { min-height: 100vh; padding: 28rpx $page-padding; background: $bg-page; }
.summary { display: flex; justify-content: space-around; padding: 30rpx; background: $bg-card; border-radius: $radius-xl; box-shadow: $shadow-card; }
.summary view { display: flex; flex-direction: column; align-items: center; gap: 8rpx; color: $text-tertiary; font-size: $font-caption; }
.summary__value { color: $primary; font-size: 42rpx; font-weight: 700; }
.summary__value--danger { color: $danger; }
.sync-now { margin: 24rpx 0; color: #fff; background: $primary; border-radius: $radius-xl; }
.section-title { display: block; margin-bottom: 18rpx; color: $text-primary; font-size: $font-h3; font-weight: 700; }
.conflict-card { margin-bottom: 18rpx; padding: 24rpx; background: $bg-card; border-radius: $radius-xl; box-shadow: $shadow-card; }
.conflict-card__title, .conflict-card__error, .conflict-card__time { display: block; }
.conflict-card__title { font-weight: 600; color: $text-primary; }
.conflict-card__error { margin-top: 10rpx; color: $danger; font-size: $font-caption; }
.conflict-card__time { margin-top: 8rpx; color: $text-tertiary; font-size: $font-mini; }
.conflict-card__actions { display: flex; justify-content: space-between; margin-top: 22rpx; color: $primary; font-size: $font-caption; }
.conflict-card__actions .danger { color: $danger; }
.empty { padding: 100rpx 0; text-align: center; color: $text-tertiary; }
.merge-popup { padding: 30rpx $page-padding calc(30rpx + env(safe-area-inset-bottom)); background: $bg-card; border-radius: $radius-xl $radius-xl 0 0; }
.merge-popup__input { box-sizing: border-box; width: 100%; min-height: 400rpx; padding: 20rpx; background: $bg-page; border-radius: $radius-lg; font-family: monospace; font-size: 24rpx; }
</style>
