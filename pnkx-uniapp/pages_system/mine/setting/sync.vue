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
        <text class="conflict-card__error">{{ conflictInfo(item).message || item.error_msg || '同步失败，请选择处理方式' }}</text>
        <text class="conflict-card__time">{{ item.created_at }}</text>
        <view v-if="item.table_name === 'px_note'">
          <text selectable>本地草稿：{{ notePayload(item).title }}
{{ notePayload(item).content }}</text>
          <text v-if="conflictInfo(item).current" selectable>服务端：{{ conflictInfo(item).current.title }}
{{ conflictInfo(item).current.content }}</text>
        </view>
        <view class="conflict-card__actions">
          <text v-if="item.table_name !== 'px_note' || !conflictInfo(item).current" @click="retry(item)">重试</text>
          <text v-if="item.table_name === 'px_note'" @click="saveNoteCopy(item)">另存副本</text>
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
        <view v-if="editing && editing.table_name === 'px_note'">
          <text>请对照服务端内容编辑，保存会校验当前服务端修订号。</text>
          <input v-model="mergeNote.title" placeholder="笔记标题" />
          <textarea v-model="mergeNote.content" class="merge-popup__input" maxlength="-1" />
        </view>
        <textarea v-else v-model="mergePayload" class="merge-popup__input" maxlength="-1" />
        <button class="sync-now" @click="saveMerge">保存并重试</button>
      </view>
    </uni-popup>
  </view>
</template>

<script>
import offlineQueue from '@/utils/offlineQueue'
import request from '@/utils/request'
import syncScheduler from '@/utils/syncScheduler'

export default {
  data() {
    return {
      stats: { pending: 0, syncing: 0, conflict: 0 },
      conflicts: [],
      syncing: false,
      editing: null,
      mergePayload: '',
      mergeNote: {},
      copying: false
    }
  },
  onShow() { this.load() },
  methods: {
    conflictInfo(item) {
      try { return JSON.parse(item.error_msg || '{}') } catch { return {} }
    },
    notePayload(item) {
      try { return JSON.parse(item.payload || '{}') } catch { return {} }
    },
    async saveNoteCopy(item) {
      if (this.copying) return
      this.copying = true
      const draft = this.notePayload(item)
      try {
        await request({ url: '/note', method: 'post', offline: false, data: {
          title: (draft.title || '未命名笔记') + '（冲突副本）', content: draft.content, richText: draft.richText, folder: 0
        } })
        uni.showToast({ title: '副本已保存到根目录', icon: 'none' })
      } finally { this.copying = false }
    },
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
      this.mergeNote = { ...this.notePayload(item) }
      if (item.table_name === 'px_note') {
        const current = this.conflictInfo(item).current
        if (!current) {
          uni.showToast({ title: '缺少服务端版本，请保留草稿或另存副本', icon: 'none' })
          return
        }
        this.mergeNote.revision = current.revision
      }
      try { this.mergePayload = JSON.stringify(JSON.parse(item.payload || '{}'), null, 2) } catch (e) { this.mergePayload = '{}' }
      this.$refs.mergePopup.open()
    },
    async saveMerge() {
      try {
        const payload = this.editing.table_name === 'px_note' ? { ...this.mergeNote } : JSON.parse(this.mergePayload)
        if (this.editing.table_name === 'px_note') {
          const text = (payload.content || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
          payload.richText = text.split('\n').map(line => '<p>' + (line || '<br>') + '</p>').join('')
        }
        await offlineQueue.replacePayload(this.editing.id, payload)
        this.$refs.mergePopup.close()
        await this.syncNow()
      } catch (e) { uni.showToast({ title: '合并保存失败，请检查内容后重试', icon: 'none' }) }
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
        await syncScheduler.resetCursors([item.table_name])
        await syncScheduler.pullServerChanges()
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
