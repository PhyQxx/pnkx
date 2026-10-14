<template>
  <view class="page ws-page subpage-shell"><view class="title">整理建议</view><text>扫描自己的资料。重复候选仅作核查提示，不会自动删除。正文重复比较限每批 100 条。</text><button :disabled="scanning" @click="scan">扫描资料</button><button v-if="scanning" @click="stopped = true">停止扫描</button><text>{{ message }}</text>
    <view v-for="row in rows" :key="row.id" class="card"><text class="title" @click="open(row)">{{ row.title || '未命名资料' }}</text><text>{{ row.reason }}</text><text v-if="row.proposedTitle">新标题：{{ row.proposedTitle }}</text><button size="mini" :disabled="saving" @click="apply(row,false)">{{ row.proposedTitle ? '应用标题' : '加入收件箱' }}</button><button size="mini" :disabled="saving" @click="apply(row,true)">忽略</button><text>{{ results[row.id] }}</text></view>
    <text v-if="!rows.length">暂无待处理建议</text><button v-if="hasMore" @click="load(nextAfter)">下一批建议</button><button @click="load(0)">刷新</button>
  </view>
</template>
<script>
import { scanOrganize, listOrganize, applyOrganize } from '@/api/px/life/workspace'
export default {
  data() { return { rows: [], nextAfter: 0, hasMore: false, scanning: false, stopped: false, saving: false, message: '', results: {} } },
  onLoad() { this.load(0) }, onUnload() { this.stopped = true },
  methods: {
    async load(after) { const result = await listOrganize(after); this.rows = result.data.rows; this.nextAfter = result.data.nextAfter; this.hasMore = result.data.hasMore },
    async scan() { this.scanning = true; this.stopped = false; let checked = 0; try { for (const type of ['note','webArticle','todo']) { let after = 0; while (!this.stopped) { const result = await scanOrganize(type,after); checked += result.data.checked; this.message = `已检查 ${checked} 条`; after = result.data.lastId; if (result.data.done) break } } await this.load(0) } finally { this.scanning = false } },
    async apply(row,ignore) { this.saving = true; try { const result = await applyOrganize(row.id,ignore); this.results = { ...this.results, [row.id]: ({ applied: '已应用', ignored: '已忽略', stale: '资料已变化，请重新扫描' })[result.data] || result.data } } finally { this.saving = false } },
    open(row) { uni.navigateTo({ url: `/pages_life/workspace/resource?type=${row.resourceType}&id=${row.resourceId}` }) }
  }
}
</script>
<style scoped>
@import '@/static/scss/workspace-common.scss';.page { padding: 32rpx; }.title { display: block; font-size: 34rpx; font-weight: 600; padding: 20rpx 0; }.card { margin-top: 24rpx; padding: 24rpx; border: 1px solid $border-color; border-radius: 20rpx; }.card text { display: block; margin-bottom: 16rpx; }</style>
