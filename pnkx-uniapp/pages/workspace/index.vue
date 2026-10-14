<template>
  <view class="workspace subpage-shell">
    <!-- 搜索 -->
    <view class="ws-search">
      <uni-icons type="search" size="18" :color="$primary" />
      <input class="ws-search__input" v-model="query.q" confirm-type="search" placeholder="搜索笔记、剪藏、文件、待办…" @confirm="search(1)" />
      <text v-if="query.q" class="ws-search__clear" @click="query.q = ''; search(1)">×</text>
    </view>

    <!-- 视图 Segment：收件箱 / 全部 / 继续阅读 -->
    <view class="ws-seg">
      <view class="ws-seg__item" :class="{ on: seg === 'inbox' }" @click="setSeg('inbox')">收件箱</view>
      <view class="ws-seg__item" :class="{ on: seg === 'all' }" @click="setSeg('all')">全部</view>
      <view class="ws-seg__item" :class="{ on: seg === 'read' }" @click="setSeg('read')">继续阅读</view>
      <text class="ws-seg__count">共 {{ total }} 条</text>
    </view>

    <!-- 类型 chips + 筛选入口 -->
    <scroll-view scroll-x class="ws-chips" show-scrollbar="false">
      <view class="ws-chips__inner">
        <text v-for="(label, type) in types" :key="type" class="ws-chip" :class="{ on: query.type === type }" @click="query.type = type; search(1)">{{ label }}</text>
        <text class="ws-chip ws-chip--filter" :class="{ on: activeFilterCount > 0 }" @click="openFilter">筛选{{ activeFilterCount ? ' · ' + activeFilterCount : '' }}⌄</text>
      </view>
    </scroll-view>

    <!-- 标签 chips（长按管理） -->
    <scroll-view v-if="tags.length" scroll-x class="ws-chips ws-chips--tags" show-scrollbar="false">
      <view class="ws-chips__inner">
        <text v-for="tag in tags" :key="tag.id" class="ws-chip ws-chip--tag" :class="{ on: query.tagIds.includes(tag.id) }" @click="toggleTag(tag.id)" @longpress="manageTag(tag)"># {{ tag.name }}</text>
      </view>
    </scroll-view>

    <!-- 批量操作条 -->
    <view v-if="selected.length" class="ws-batch">
      <text class="ws-batch__count">已选 {{ selected.length }} 条</text>
      <view class="ws-batch__acts">
        <text class="ws-batch__btn" @click="batchInbox">{{ query.inbox ? '移出收件箱' : '加入收件箱' }}</text>
        <text class="ws-batch__btn ws-batch__btn--pri" @click="openBatch">加标签</text>
        <text class="ws-batch__btn" @click="selected = []">取消</text>
      </view>
    </view>

    <!-- 骨架屏 -->
    <view v-if="loading && !rows.length" class="ws-skel">
      <view v-for="n in 4" :key="n" class="ws-skel__card">
        <view class="ws-skel__badge" />
        <view class="ws-skel__lines"><view class="ws-skel__line" style="width: 70%" /><view class="ws-skel__line" style="width: 45%" /><view class="ws-skel__line ws-skel__line--thin" style="width: 30%" /></view>
      </view>
    </view>

    <!-- 空态 -->
    <view v-else-if="!rows.length" class="ws-empty">
      <text class="ws-empty__glyph">🗂</text>
      <text class="ws-empty__text">没有符合条件的资料</text>
      <text class="ws-empty__hint">换个类型或标签试试，或先去「整理建议」清理待处理项</text>
    </view>

    <!-- 资源卡片流 -->
    <view v-else class="ws-list">
      <view v-for="row in rows" :key="key(row)" class="ws-card" :class="{ 'ws-card--sel': isSelected(row) }" @click="open(row)" @longpress="toggle(row)">
        <view class="ws-badge" :class="'ws-badge--' + row.type">{{ badgeGlyph(row.type) }}</view>
        <view class="ws-card__main">
          <view class="ws-card__titlerow">
            <text class="ws-card__title">{{ row.title || '（无标题）' }}</text>
            <view class="ws-check" :class="{ on: isSelected(row) }"><text v-if="isSelected(row)">✓</text></view>
          </view>
          <text v-if="row.subtitle" class="ws-card__snippet">{{ row.subtitle }}</text>
          <view v-if="row.tags && row.tags.length" class="ws-card__tags"><text v-for="tag in row.tags" :key="tag" class="ws-card__tag">{{ tag }}</text></view>
          <view class="ws-card__meta">
            <text>{{ types[row.type] }}</text><text class="ws-card__dot">·</text><text>{{ row.updatedAt }}</text>
            <view v-if="row.inbox" class="ws-card__inboxdot" />
          </view>
          <view class="ws-card__acts">
            <text @click.stop="inbox(row)">{{ query.inbox ? '移出收件箱' : '收件箱' }}</text>
            <text v-if="row.type === 'webArticle'" @click.stop="openConversion(row)">转笔记</text>
            <text @click.stop="editRelations(row)">标签</text>
            <text v-if="row.type === 'note'" @click.stop="recovery(row.id)">历史</text>
          </view>
          <resource-links :type="row.type" :id="row.id" />
        </view>
      </view>
      <view class="ws-more">
        <text v-if="loading">加载中…</text>
        <text v-else-if="hasMore">上拉加载更多</text>
        <text v-else>没有更多了</text>
      </view>
    </view>

    <!-- 底部工具条（tab 常驻） -->
    <view class="ws-toolbar">
      <view class="ws-toolbar__item" @click="organize"><text class="ws-toolbar__icon">✦</text><text class="ws-toolbar__label">整理</text></view>
      <view class="ws-toolbar__item" @click="ask"><text class="ws-toolbar__icon">?</text><text class="ws-toolbar__label">问答</text></view>
      <view class="ws-toolbar__item" @click="recovery()"><text class="ws-toolbar__icon">↺</text><text class="ws-toolbar__label">回收站</text></view>
      <view class="ws-toolbar__item" @click="files"><text class="ws-toolbar__icon">▤</text><text class="ws-toolbar__label">文件库</text></view>
      <view class="ws-toolbar__item" @click="series"><text class="ws-toolbar__icon">⟳</text><text class="ws-toolbar__label">系列</text></view>
    </view>

    <!-- 筛选抽屉 -->
    <uni-popup ref="filterPopup" type="bottom">
      <view class="ws-sheet">
        <view class="ws-sheet__grab" />
        <text class="ws-sheet__title">筛选</text>
        <view v-if="query.type === 'webArticle'" class="ws-sheet__row">
          <picker class="ws-sheet__pick" :range="['全部阅读状态', '未读', '已读']" @change="query.readFlag = Number($event.detail.value) === 0 ? undefined : Number($event.detail.value) === 2; search(1)"><text>阅读状态：{{ query.readFlag === undefined ? '全部' : query.readFlag ? '已读' : '未读' }}</text></picker>
          <picker class="ws-sheet__pick" :range="['全部收藏状态', '未收藏', '已收藏']" @change="query.starFlag = Number($event.detail.value) === 0 ? undefined : Number($event.detail.value) === 2; search(1)"><text>收藏：{{ query.starFlag === undefined ? '全部' : query.starFlag ? '已收藏' : '未收藏' }}</text></picker>
        </view>
        <view class="ws-sheet__row">
          <picker class="ws-sheet__pick" :range="['满足全部标签', '满足任一标签']" @change="query.tagMode = Number($event.detail.value) === 0 ? 'all' : 'any'; search(1)"><text>{{ query.tagMode === 'all' ? '满足全部标签' : '满足任一标签' }}</text></picker>
        </view>
        <view class="ws-sheet__row ws-sheet__row--switch">
          <label class="ws-switch"><switch :checked="query.includeDescendants" @change="query.includeDescendants = $event.detail.value; search(1)" /><text>含子标签</text></label>
          <label class="ws-switch"><switch :checked="query.untagged" @change="query.untagged = $event.detail.value; search(1)" /><text>只看未整理</text></label>
        </view>
        <view class="ws-sheet__row">
          <picker class="ws-sheet__pick" mode="date" @change="query.startDate = $event.detail.value; search(1)"><text>{{ query.startDate || '开始日期' }}</text></picker>
          <picker class="ws-sheet__pick" mode="date" @change="query.endDate = $event.detail.value; search(1)"><text>{{ query.endDate || '结束日期' }}</text></picker>
        </view>
        <view class="ws-sheet__row ws-sheet__row--end">
          <text class="ws-sheet__link" @click="manageTag()">＋ 新标签</text>
          <text class="ws-sheet__link" @click="importLegacy">{{ importing ? '迁移中…' : '迁移已有标签' }}</text>
          <text class="ws-sheet__link ws-sheet__link--pri" @click="clearFilter">清除筛选</text>
        </view>
      </view>
    </uni-popup>

    <!-- 转笔记/待办 -->
    <uni-popup ref="convertPopup" type="bottom">
      <view class="ws-sheet">
        <view class="ws-sheet__grab" />
        <text class="ws-sheet__title">检查内容后创建</text>
        <picker class="ws-sheet__pick" :range="['笔记', '待办']" @change="conversion.targetType = Number($event.detail.value) === 0 ? 'note' : 'todo'"><text>创建为：{{ conversion.targetType === 'note' ? '笔记' : '待办' }}</text></picker>
        <input class="ws-sheet__input" v-model="conversion.title" maxlength="200" placeholder="标题" />
        <textarea class="ws-sheet__textarea" v-model="conversion.content" maxlength="100000" placeholder="正文；待办说明最多500字" />
        <button class="ws-sheet__btn" :loading="saving" @click="convert">确认创建并关联来源</button>
      </view>
    </uni-popup>

    <!-- 标签管理 -->
    <uni-popup ref="tagPopup" type="bottom">
      <view class="ws-sheet">
        <view class="ws-sheet__grab" />
        <text class="ws-sheet__title">{{ tagForm.id ? '管理标签' : '新建标签' }}</text>
        <input class="ws-sheet__input" v-model="tagForm.name" maxlength="100" placeholder="标签名称" />
        <picker class="ws-sheet__pick" :range="parentOptions" range-key="name" @change="tagForm.parentId = parentOptions[$event.detail.value].id"><text>父标签：{{ parentName }}</text></picker>
        <button class="ws-sheet__btn" :loading="saving" @click="save">保存</button>
        <template v-if="tagForm.id">
          <picker class="ws-sheet__pick" :range="mergeOptions" range-key="name" @change="mergeTarget = mergeOptions[$event.detail.value].id"><text>合并到：{{ mergeName }}</text></picker>
          <view class="ws-sheet__row ws-sheet__row--end">
            <button class="ws-sheet__btn ws-sheet__btn--ghost" :disabled="!mergeTarget" @click="merge">确认合并</button>
            <button class="ws-sheet__btn ws-sheet__btn--ghost" @click="remove">删除标签</button>
          </view>
        </template>
      </view>
    </uni-popup>

    <!-- 打标签（单条 / 批量） -->
    <uni-popup ref="relationPopup" type="bottom">
      <view class="ws-sheet">
        <view class="ws-sheet__grab" />
        <text class="ws-sheet__title">{{ editingRow ? '资料标签' : '批量添加标签' }}</text>
        <checkbox-group class="ws-sheet__tags" @change="selectedTags = $event.detail.value.map(Number)">
          <label v-for="tag in tags" :key="tag.id" class="ws-chip ws-chip--tag"><checkbox :value="String(tag.id)" :checked="selectedTags.includes(tag.id)" />{{ tag.name }}</label>
        </checkbox-group>
        <text v-if="batchResult" class="ws-sheet__result">{{ batchResult }}</text>
        <button class="ws-sheet__btn" :loading="saving" @click="applyTags">保存</button>
      </view>
    </uni-popup>
  </view>
</template>

<script>
import ResourceLinks from '@/components/resource-links/index.vue'
import { batchInbox as updateBatchInbox, getResource, setInbox, convertArticle, listResources, listTags, saveTag, deleteTag, mergeTags, getResourceTags, setResourceTags, batchTags, backfillTags } from '@/api/px/life/workspace'
import uniPopup from '@/uni_modules/uni-popup/components/uni-popup/uni-popup.vue'
export default {
  components: { uniPopup, ResourceLinks },
  data() { return { query: { q: '', type: 'all', tagIds: [], tagMode: 'all', includeDescendants: true, untagged: false, readFlag: undefined, starFlag: undefined, inbox: undefined, recent: false, startDate: '', endDate: '' },
    types: { all: '全部', file: '文件', note: '笔记', webArticle: '剪藏', todo: '待办' }, badges: { all: '全', file: '文', note: '笔', webArticle: '剪', todo: '办' }, tags: [], rows: [], total: 0, page: 1, hasMore: false, loading: false,
    conversion: {}, conversionId: null, selected: [], sequence: 0, tagForm: {}, mergeTarget: null, selectedTags: [], editingRow: null, batchResult: '', saving: false, importing: false, stopped: false } },
  computed: {
    seg() { return this.query.inbox ? 'inbox' : this.query.recent ? 'read' : 'all' },
    activeFilterCount() { let n = 0; if (this.query.tagIds.length) n++; if (!this.query.includeDescendants) n++; if (this.query.untagged) n++; if (this.query.tagMode !== 'all') n++; if (this.query.startDate) n++; if (this.query.endDate) n++; if (this.query.readFlag !== undefined) n++; if (this.query.starFlag !== undefined) n++; return n },
    parentOptions() { return [{ id: 0, name: '根标签' }, ...this.tags.filter(tag => tag.id !== this.tagForm.id)] },
    parentName() { return this.parentOptions.find(tag => tag.id === this.tagForm.parentId)?.name || '根标签' },
    mergeOptions() { return this.tags.filter(tag => tag.id !== this.tagForm.id) },
    mergeName() { return this.tags.find(tag => tag.id === this.mergeTarget)?.name || '请选择目标标签' }
  },
  onLoad(options) { if (options.untagged === 'true') this.query.untagged = true; if (options.recent === 'true') this.query.recent = true },
  onShow() { try { const f = uni.getStorageSync('ws:filter'); if (f) { const o = JSON.parse(f); this.query.untagged = !!(o.untagged || this.query.untagged); if (o.recent) { this.query.recent = true; this.query.inbox = undefined; } uni.removeStorageSync('ws:filter'); } } catch (e) {} this.refreshTags(); this.search(1) },
  onUnload() { this.sequence++; this.stopped = true },
  onReachBottom() { if (this.hasMore && !this.loading) this.search(this.page + 1, true) },
  methods: {
    badgeGlyph(type) { return this.badges[type] || '资' },
    setSeg(seg) { if (seg === 'inbox') { this.query.inbox = true; this.query.recent = false } else if (seg === 'read') { this.query.inbox = undefined; this.query.recent = true } else { this.query.inbox = undefined; this.query.recent = false } this.search(1) },
    openFilter() { this.$refs.filterPopup.open() },
    clearFilter() { this.query = { ...this.query, tagIds: [], tagMode: 'all', includeDescendants: true, untagged: false, readFlag: undefined, starFlag: undefined, startDate: '', endDate: '' }; this.$refs.filterPopup.close(); this.search(1) },
    isSelected(row) { return this.selected.some(item => this.key(item) === this.key(row)) },
    ask() { uni.navigateTo({ url: '/pages_life/workspace/ask' }) },
    async batchInbox() { const result = await updateBatchInbox(this.selected, !this.query.inbox); const failed = result.data.filter(item => item.status !== 'success').length; uni.showToast({ title: `成功 ${result.data.length - failed}，失败 ${failed}`, icon: 'none' }); await this.search(1) },
    files() { uni.navigateTo({ url: '/pages_life/workspace/files' }) },
    series() { uni.navigateTo({ url: '/pages_life/workspace/series' }) },
    organize() { uni.navigateTo({ url: '/pages_life/workspace/organize' }) },
    recovery(id) { uni.navigateTo({ url: '/pages_life/workspace/recovery' + (id ? '?id=' + id : '') }) },
    key(row) { return row.type + ':' + row.id },
    async refreshTags() { this.tags = (await listTags()).data },
    async search(page = 1, append = false) {
      const sequence = ++this.sequence; this.loading = true
      try { const res = await listResources({ ...this.query, pageNum: page, pageSize: 20 }); if (sequence !== this.sequence) return
        this.rows = append ? this.rows.concat(res.data.rows) : res.data.rows
        this.total = res.data.total; this.page = page; this.hasMore = res.data.hasMore
        if (!append) this.selected = []
      } finally { if (sequence === this.sequence) this.loading = false }
    },
    toggleTag(id) { const index = this.query.tagIds.indexOf(id); if (index < 0) this.query.tagIds.push(id); else this.query.tagIds.splice(index, 1); this.search(1) },
    toggle(row) { const index = this.selected.findIndex(item => this.key(item) === this.key(row)); if (index < 0) this.selected.push(row); else this.selected.splice(index, 1); uni.vibrateShort && uni.vibrateShort() },
    open(row) { uni.navigateTo({ url: `/pages_life/workspace/resource?type=${row.type}&id=${row.id}` }) },
    async inbox(row) { await setInbox(row, !this.query.inbox); if (this.query.inbox) await this.search(1); uni.showToast({ title: '收件箱已更新', icon: 'none' }) },
    async openConversion(row) {
      const source = (await getResource(row)).data; this.conversionId = row.id
      this.conversion = { title: source.title || '', content: source.content || '', targetType: 'note', requestId: Date.now().toString(36) + '-' + Math.random().toString(36).slice(2) + '-' + Math.random().toString(36).slice(2) }
      this.$refs.convertPopup.open()
    },
    async convert() { if (this.saving) return; this.saving = true; try { const res = await convertArticle(this.conversionId, this.conversion); this.$refs.convertPopup.close(); this.open(res.data) } finally { this.saving = false } },
    manageTag(tag) { this.tagForm = tag ? { ...tag } : { name: '', parentId: 0, color: '#409EFF' }; this.mergeTarget = null; this.$refs.tagPopup.open() },
    async save() { if (this.saving) return; this.saving = true; try { await saveTag(this.tagForm); this.$refs.tagPopup.close(); await this.refreshTags(); await this.search(1) } finally { this.saving = false } },
    async remove() { await this.$modal.confirm('资料内容会保留，子标签移到上一级。', '删除标签'); await deleteTag(this.tagForm.id); this.query.tagIds = []; this.$refs.tagPopup.close(); await this.refreshTags(); await this.search(1) },
    async merge() { await this.$modal.confirm('源标签会删除，关联资料合并到目标标签。', '合并标签'); await mergeTags(this.tagForm.id, this.mergeTarget); this.query.tagIds = []; this.$refs.tagPopup.close(); await this.refreshTags(); await this.search(1) },
    async editRelations(row) { this.selectedTags = (await getResourceTags(row)).data; this.editingRow = row; this.batchResult = ''; this.$refs.relationPopup.open() },
    openBatch() { if (!this.selected.length) return; this.selectedTags = []; this.editingRow = null; this.batchResult = ''; this.$refs.relationPopup.open() },
    async applyTags() {
      if (this.saving) return; this.saving = true
      try { if (this.editingRow) { await setResourceTags(this.editingRow, this.selectedTags); this.$refs.relationPopup.close() }
        else { const res = await batchTags(this.selected, this.selectedTags); const count = res.data.filter(row => row.status === 'success').length; this.batchResult = `成功 ${count} 条，失败 ${res.data.length - count} 条` }
        await this.search(this.page)
      } finally { this.saving = false }
    },
    async importLegacy() {
      if (this.importing) return; this.importing = true
      try { for (const type of ['todo', 'webArticle']) { let done = false; while (!done && !this.stopped) done = (await backfillTags(type)).data.done }
        await this.refreshTags(); await this.search(1); uni.showToast({ title: '标签迁移完成', icon: 'none' })
      } finally { this.importing = false }
    }
  }
}
</script>

<style scoped lang="scss">
/* ===== 资料中心主页（一级 Tab）—— 对齐 PRODUCT_UIUE_REDESIGN §3.2 / 移动端原型 ===== */
.workspace { padding: 24rpx 28rpx 240rpx; min-height: 100vh; }

/* 搜索条 */
.ws-search { display: flex; align-items: center; gap: 16rpx; height: 84rpx; padding: 0 26rpx; border-radius: 26rpx;
  background: $frost-white; border: 1rpx solid $border-color-light; box-shadow: $shadow-sm;
  &__input { flex: 1; font-size: 28rpx; color: $text-primary; } 
  &__clear { font-size: 36rpx; color: $text-tertiary; padding: 0 8rpx; } }

/* Segment */
.ws-seg { display: flex; align-items: center; gap: 8rpx; margin-top: 24rpx; padding: 8rpx; border-radius: 24rpx; background: $bg-card-hover;
  &__item { flex: 1; text-align: center; padding: 16rpx 0; border-radius: 18rpx; font-size: 26rpx; font-weight: 600; color: $text-secondary;
    transition: all $duration-fast $ease-out;
    &.on { background: $frost-white; color: $primary; box-shadow: $shadow-sm; } }
  &__count { flex: none; padding: 0 20rpx; font-size: 22rpx; color: $text-tertiary; } }

/* chips */
.ws-chips { margin-top: 22rpx; white-space: nowrap;
  &--tags { margin-top: 14rpx; } }
.ws-chips__inner { display: inline-flex; gap: 14rpx; padding: 4rpx 2rpx; }
.ws-chip { flex: none; padding: 12rpx 26rpx; border-radius: 100rpx; font-size: 24rpx; font-weight: 600; color: $text-secondary;
  background: $frost-white; border: 1rpx solid $border-color-light;
  transition: all $duration-fast $ease-spring;
  &:active { transform: scale(0.92); }
  &.on { background: $primary; border-color: $primary; color: $theme-white; transform: scale(1.04); }
  &--filter { color: $primary; }
  &--tag { font-weight: 500; } }

/* 批量条 */
.ws-batch { display: flex; align-items: center; justify-content: space-between; margin-top: 20rpx; padding: 18rpx 26rpx; border-radius: 22rpx;
  background: $primary-lighter; border: 1rpx solid rgba(79, 134, 247, 0.35);
  &__count { font-size: 24rpx; font-weight: 700; color: $primary-dark; }
  &__acts { display: flex; gap: 22rpx; }
  &__btn { font-size: 24rpx; font-weight: 600; color: $primary-dark;
    &--pri { font-weight: 700; } } }

/* 骨架屏 */
.ws-skel { display: flex; flex-direction: column; gap: 20rpx; margin-top: 24rpx;
  &__card { display: flex; gap: 20rpx; padding: 26rpx; border-radius: 24rpx; background: $bg-card; border: 1rpx solid $border-color-light; }
  &__badge { width: 76rpx; height: 76rpx; border-radius: 22rpx; flex: none; background: $bg-card-hover; position: relative; overflow: hidden;
    &::after { content: ''; position: absolute; inset: 0; transform: translateX(-100%);
      background: linear-gradient(90deg, transparent, $frost-white, transparent); animation: ws-shimmer 1.2s ease-in-out infinite; } }
  &__lines { flex: 1; display: flex; flex-direction: column; gap: 14rpx; justify-content: center; }
  &__line { height: 26rpx; border-radius: 8rpx; background: $bg-card-hover; position: relative; overflow: hidden;
    &--thin { height: 18rpx; }
    &::after { content: ''; position: absolute; inset: 0; transform: translateX(-100%);
      background: linear-gradient(90deg, transparent, $frost-white, transparent); animation: ws-shimmer 1.2s ease-in-out infinite; } } }
@keyframes ws-shimmer { to { transform: translateX(100%); } }

/* 空态 */
.ws-empty { display: flex; flex-direction: column; align-items: center; padding: 120rpx 40rpx;
  &__glyph { font-size: 80rpx; animation: ws-float 3s ease-in-out infinite; }
  &__text { margin-top: 24rpx; font-size: 28rpx; font-weight: 600; color: $text-secondary; }
  &__hint { margin-top: 12rpx; font-size: 22rpx; color: $text-tertiary; text-align: center; } }
@keyframes ws-float { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-10rpx); } }

/* 卡片流 */
.ws-list { display: flex; flex-direction: column; gap: 20rpx; margin-top: 24rpx; }
.ws-card { display: flex; gap: 20rpx; padding: 26rpx; border-radius: 26rpx; background: $frost-white;
  border: 1rpx solid $border-color-light; box-shadow: $shadow-sm;
  transition: transform $duration-fast $ease-spring, border-color $duration-fast $ease-out;
  &--sel { border-color: $primary; background: $primary-lighter; } }
.ws-badge { width: 76rpx; height: 76rpx; border-radius: 22rpx; flex: none; display: flex; align-items: center; justify-content: center;
  font-size: 28rpx; font-weight: 700;
  &--note { background: rgba(167, 139, 250, 0.16); color: $diary; }
  &--webArticle { background: rgba(79, 134, 247, 0.14); color: $primary; }
  &--file { background: rgba(52, 211, 153, 0.14); color: $todo; }
  &--todo { background: rgba(251, 191, 36, 0.18); color: $warning-dark; } }
.ws-card__main { flex: 1; min-width: 0; }
.ws-card__titlerow { display: flex; align-items: flex-start; gap: 14rpx; }
.ws-card__title { flex: 1; font-size: 28rpx; font-weight: 700; color: $text-primary; overflow-wrap: anywhere; }
.ws-check { width: 40rpx; height: 40rpx; border-radius: 14rpx; border: 2rpx solid $border-color; flex: none;
  display: flex; align-items: center; justify-content: center; font-size: 24rpx; color: $theme-white;
  transition: all $duration-fast $ease-spring;
  &.on { background: $primary; border-color: $primary; } }
.ws-card__snippet { display: block; margin-top: 10rpx; font-size: 24rpx; color: $text-secondary; line-height: 1.6;
  max-height: 110rpx; overflow: hidden; overflow-wrap: anywhere; }
.ws-card__tags { display: flex; gap: 10rpx; flex-wrap: wrap; margin-top: 14rpx; }
.ws-card__tag { font-size: 20rpx; padding: 4rpx 16rpx; border-radius: 100rpx; background: $bg-card-hover; color: $text-secondary; font-weight: 600; }
.ws-card__meta { display: flex; align-items: center; gap: 10rpx; margin-top: 14rpx; font-size: 20rpx; color: $text-tertiary; }
.ws-card__inboxdot { width: 12rpx; height: 12rpx; border-radius: 50%; background: $primary; }
.ws-card__acts { display: flex; gap: 26rpx; margin-top: 14rpx; padding-top: 14rpx; border-top: 1rpx solid $border-color-light;
  text { font-size: 22rpx; font-weight: 600; color: $primary; } }
.ws-more { text-align: center; padding: 24rpx 0; font-size: 22rpx; color: $text-tertiary; }

/* 底部工具条 */
.ws-toolbar { position: fixed; left: 24rpx; right: 24rpx; bottom: calc(130rpx + env(safe-area-inset-bottom)); z-index: 90;
  display: flex; padding: 14rpx 10rpx; border-radius: 30rpx;
  background: $frost-white; border: 1rpx solid $border-color-light; box-shadow: $shadow-lg;
  backdrop-filter: blur(20rpx); }
.ws-toolbar__item { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4rpx;
  transition: transform $duration-fast $ease-spring;
  &:active { transform: scale(0.92); } }
.ws-toolbar__icon { font-size: 34rpx; line-height: 1.15; color: $text-primary; }
.ws-toolbar__label { font-size: 20rpx; font-weight: 600; color: $text-secondary; }

/* 弹层 sheet */
.ws-sheet { padding: 20rpx 32rpx calc(40rpx + env(safe-area-inset-bottom)); background: $frost-white; border-radius: 36rpx 36rpx 0 0; }
.ws-sheet__grab { width: 76rpx; height: 8rpx; border-radius: 6rpx; background: $border-color; margin: 4rpx auto 22rpx; }
.ws-sheet__title { display: block; font-size: 32rpx; font-weight: 700; color: $text-primary; margin-bottom: 24rpx; }
.ws-sheet__row { display: flex; align-items: center; gap: 18rpx; flex-wrap: wrap; margin: 18rpx 0;
  &--switch { gap: 40rpx; }
  &--end { justify-content: flex-end; } }
.ws-sheet__pick { padding: 20rpx 26rpx; border-radius: 18rpx; background: $bg-card-hover; font-size: 26rpx; color: $text-primary; }
.ws-switch { display: flex; align-items: center; gap: 12rpx; font-size: 26rpx; color: $text-primary; }
.ws-sheet__input { padding: 22rpx 4rpx; font-size: 28rpx; border-bottom: 1rpx solid $border-color-light; }
.ws-sheet__textarea { width: 100%; height: 300rpx; margin-top: 18rpx; padding: 22rpx; border-radius: 18rpx; background: $bg-card-hover; font-size: 26rpx; box-sizing: border-box; }
.ws-sheet__btn { margin-top: 26rpx; border-radius: 100rpx; background: $primary; color: $theme-white; font-size: 28rpx; font-weight: 700;
  &--ghost { background: $bg-card-hover; color: $text-secondary; font-weight: 600; } }
.ws-sheet__link { font-size: 24rpx; font-weight: 600; color: $text-secondary;
  &--pri { color: $primary; } }
.ws-sheet__tags { display: flex; flex-wrap: wrap; gap: 16rpx; margin: 8rpx 0 16rpx; }
.ws-sheet__result { display: block; margin-top: 12rpx; font-size: 24rpx; color: $text-secondary; }
</style>
