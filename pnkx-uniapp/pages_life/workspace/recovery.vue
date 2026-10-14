<template>
  <view class="page ws-page subpage-shell">
    <view class="heading">{{ id ? '笔记历史' : '回收站' }}</view>
    <view v-if="id">
      <text>最多保留最近 50 个内容版本。还原会保留当前内容。</text>
      <picker :range="history.map(row => '版本 ' + row.revision + ' · ' + row.createTime)" @change="compare(history[$event.detail.value].revision)"><button>选择历史版本</button></picker>
      <view v-if="snapshot"><view class="heading">历史版本 {{ snapshot.revision }} · {{ snapshot.title }}</view><text class="content">{{ snapshot.content }}</text><view class="heading">当前版本 {{ current.revision }} · {{ current.title }}</view><text class="content">{{ current.content }}</text><button :disabled="saving" @click="restore">还原所选版本</button></view>
      <text v-if="!history.length">暂无历史版本</text><text>{{ message }}</text>
    </view>
    <view v-else><text>恢复到原目录；目录已删除的笔记恢复到根目录。</text><view v-for="row in rows" :key="row.type + row.id" class="row"><text>{{ row.type === 'note' ? '笔记' : row.type === 'file' ? '文件/目录' : '剪藏' }} · {{ row.title }}</text><button size="mini" :disabled="saving" @click="recover(row)">恢复</button></view><text v-if="!rows.length">回收站为空</text><view class="row"><button :disabled="page === 1" @click="loadTrash(page - 1)">上一页</button><button :disabled="!hasMore" @click="loadTrash(page + 1)">下一页</button></view></view>
  </view>
</template>
<script>
import { noteHistory, noteVersion, restoreVersion, getResource, listTrash, restoreTrash } from '@/api/px/life/workspace'
export default {
  data() { return { id: null, history: [], snapshot: null, current: {}, rows: [], page: 1, hasMore: false, saving: false, message: '', sequence: 0, initSequence: 0, trashSequence: 0, gone: false } },
  onLoad(query) { this.id = query.id; this.init() },
  onUnload() { this.gone = true; this.sequence++; this.initSequence++; this.trashSequence++ },
  methods: {
    async init() {
      const sequence = ++this.initSequence, id = this.id
      this.sequence++; this.trashSequence++; this.history = []; this.snapshot = null; this.current = {}
      if (id) { const result = await noteHistory(id); if (!this.gone && sequence === this.initSequence) this.history = result.data }
      else await this.loadTrash(1)
    },
    async compare(version) {
      const sequence = ++this.sequence, id = this.id
      this.snapshot = null; this.current = {}; this.message = ''
      const [old,now] = await Promise.all([noteVersion(id,version),getResource({ type: 'note', id })])
      if (this.gone || id !== this.id || sequence !== this.sequence) return
      this.snapshot = old.data; this.current = now.data
    },
    restore() {
      if (!this.snapshot || this.saving || this.gone) return
      const id = this.id, version = this.snapshot.revision, revision = this.current.revision, sequence = this.sequence
      uni.showModal({ title: '还原版本', content: '还原历史内容？当前内容会保留在历史中。', success: async result => {
        if (!result.confirm || this.gone || id !== this.id || sequence !== this.sequence || this.saving) return
        this.saving = true
        try {
          await restoreVersion(id,version,revision)
          if (this.gone || id !== this.id || sequence !== this.sequence) return
          await this.init()
          if (!this.gone) this.message = '已还原，请重新打开笔记继续编辑'
        } catch { if (!this.gone) this.message = '还原未完成，请重新选择版本比较后再试' }
        finally { if (!this.gone) this.saving = false }
      } })
    },
    async loadTrash(page) {
      const sequence = ++this.trashSequence
      const result = await listTrash({ pageNum: page, pageSize: 20 })
      if (this.gone || this.id || sequence !== this.trashSequence) return
      this.rows = result.data.rows; this.hasMore = result.data.hasMore; this.page = page
    },
    async recover(row) { this.saving = true; try { await restoreTrash(row); await this.loadTrash(this.page); uni.showToast({ title: '已恢复' }) } finally { this.saving = false } }
  }
}
</script>
<style scoped>
@import '@/static/scss/workspace-common.scss';.page { padding: 32rpx; }.heading { font-size: 36rpx; font-weight: 600; padding: 24rpx 0; }.content { display: block; white-space: pre-wrap; overflow-wrap: anywhere; padding: 20rpx; background: $bg-card-hover; }.row { display: flex; justify-content: space-between; align-items: center; gap: 16rpx; padding: 20rpx 0; }</style>
