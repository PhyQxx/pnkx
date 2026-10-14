<template>
  <view class="links">
    <text class="heading" @click="expanded = !expanded; expanded && reload()">关联资料与反链 {{ expanded ? '⌃' : '⌄' }}</text>
    <view v-if="expanded">
      <text class="action" @click="picker = !picker; picker && search(1)">＋关联资料</text>
      <view v-for="link in links" :key="link.targetType + ':' + link.targetId + ':' + link.relationType" class="row">
        <text @click="!link.unavailable && open(link.targetType, link.targetId)">{{ link.title || '未命名资料' }}</text>
        <text v-if="link.relationType === 'related'" class="action" @click="remove(link)">移除</text><text v-else>正文引用</text>
      </view>
      <text>被引用于（{{ backlinks.length }}）</text>
      <view v-for="link in backlinks" :key="link.sourceType + ':' + link.sourceId + ':' + link.relationType" class="row" @click="open(link.sourceType, link.sourceId)">{{ link.title || '未命名资料' }}</view>
      <view v-if="picker"><uni-search-bar v-model="keyword" placeholder="搜索关联资料" @confirm="search(1)" />
        <view v-for="row in candidates" :key="row.type + ':' + row.id" class="row"><text>{{ row.title }}</text><text class="action" @click="add(row)">关联</text></view>
        <view class="row"><button :disabled="page === 1" @click="search(page - 1)">上一页</button><button :disabled="!hasMore" @click="search(page + 1)">下一页</button></view>
      </view>
    </view>
  </view>
</template>
<script>
import { getLinks, getBacklinks, addLink, removeLink, listResources } from '@/api/px/life/workspace'
export default {
  props: { type: { type: String, required: true }, id: { type: [String, Number], required: true } },
  data() { return { expanded: false, links: [], backlinks: [], picker: false, keyword: '', candidates: [], page: 1, hasMore: false, searchSequence: 0, sequence: 0 } },
  watch: { id() { this.sequence++; this.searchSequence++; this.links = []; this.backlinks = []; if (this.expanded) this.reload() } },
  methods: {
    source() { return { type: this.type, id: this.id } },
    async reload() { const sequence = ++this.sequence; const [out, incoming] = await Promise.all([getLinks(this.source()), getBacklinks(this.source())]); if (sequence !== this.sequence) return; this.links = out.data; this.backlinks = incoming.data },
    async search(page) { const sequence = ++this.searchSequence; const res = await listResources({ q: this.keyword, pageNum: page, pageSize: 10 }); if (sequence !== this.searchSequence) return; this.candidates = res.data.rows.filter(row => row.type !== this.type || String(row.id) !== String(this.id)); this.page = page; this.hasMore = res.data.hasMore },
    async add(row) { await addLink(this.source(),row); this.picker = false; await this.reload() },
    async remove(link) { await removeLink(this.source(),link); await this.reload() },
    open(type,id) { uni.navigateTo({ url: `/pages_life/workspace/resource?type=${type}&id=${id}` }) }
  }
}
</script>
<style scoped lang="scss">
.links { margin-top: 24rpx; padding: 20rpx; border-top: 1px solid $gray-100; }.heading { display: block; font-weight: 600; padding: 12rpx 0; }.row { display: flex; justify-content: space-between; gap: 18rpx; padding: 16rpx 0; }.action { color: $primary; }
</style>
