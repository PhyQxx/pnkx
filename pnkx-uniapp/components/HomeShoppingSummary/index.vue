<template>
  <view class="shopping-card">
    <view class="shopping-header">
      <view class="shopping-heading"><view class="shopping-icon"><uni-icons type="cart" size="20" color="#409eff" /></view><text class="shopping-title">购物清单</text><text v-if="!loading && !error" class="shopping-count">待购 {{ groups.needed.length }}</text></view>
      <button class="add-button" :disabled="loading || error || busy" @click="openAdd">＋ 添加</button>
    </view>
    <view v-if="loading" class="shopping-empty">正在加载购物清单…</view>
    <view v-else-if="error" class="shopping-empty"><text>购物清单加载失败</text><button class="text-button" @click="reload">点击重试</button></view>
    <template v-else>
      <view class="shopping-filter">
        <picker :range="filterOptions" range-key="name" :value="filterIndex" @change="changeFilter"><view class="picker-label">{{ filterOptions[filterIndex].name }} <text>⌄</text></view></picker>
        <text class="shopping-hint">需要的，顺手记下来</text>
      </view>
      <view v-for="item in groups.needed.slice(0, 4)" :key="item.id" class="shopping-row">
        <button class="check-button" :disabled="busy" :aria-label="'标记' + item.name + '已买到'" @click="markBought(item)"><view class="check-square" /></button>
        <view class="shopping-copy" @click="openList(item.listId)"><text class="item-name">{{ item.name }}</text><view class="item-meta"><text>{{ listName(item.listId) }}</text><text v-if="item.addedFromMeal" class="meal-source">来自餐饮</text></view></view>
        <text class="item-quantity">× {{ item.quantity || 1 }}</text>
      </view>
      <view v-if="!groups.needed.length" class="shopping-empty">{{ lists.length ? '暂时没有要买的东西，想到什么随时添加' : '还没有购物清单，点击添加创建一份吧' }}</view>
      <view v-if="undoItem" class="undo-row"><text>已将「{{ undoItem.name }}」标记为买到</text><button class="text-button" :disabled="busy" @click="undo">撤销</button></view>
      <button class="view-all" @click="viewAll">{{ groups.needed.length > 4 ? '还有 ' + (groups.needed.length - 4) + ' 项待购 · ' : '' }}查看全部 ›</button>
      <view v-if="groups.purchased.length" class="shopping-footer"><text>✓　已买到 {{ groups.purchased.length }} 项，待处理</text><button class="text-button" @click="viewAll">去处理 ›</button></view>
    </template>
    <uni-popup ref="addPopup" type="bottom" background-color="#fff" border-radius="24rpx 24rpx 0 0">
      <view class="add-sheet">
        <view class="sheet-header"><text>添加待购物品</text><button class="text-button" :disabled="busy" @click="$refs.addPopup.close()">取消</button></view>
        <text class="field-label">物品名称</text><input v-model="form.name" class="field-input" maxlength="80" placeholder="例如：牛奶" />
        <text class="field-label">数量</text><uni-number-box v-model="form.quantity" :min="1" :max="999" />
        <text class="field-label">加入清单</text>
        <picker :range="lists" range-key="name" :value="addListIndex" @change="addListIndex = Number($event.detail.value)"><view class="field-input">{{ lists[addListIndex] ? lists[addListIndex].name : '请选择清单' }}　⌄</view></picker>
        <button class="submit-button" :loading="busy" :disabled="busy" @click="add">添加到清单</button>
      </view>
    </uni-popup>
  </view>
</template>

<script>
import { listShoppingList, listShoppingItem, addShoppingItem, updateShoppingItem } from '@/api/px/life/shoppingList'
import { groupItems } from '@/utils/shoppingGroups'
import uniPopup from '@/uni_modules/uni-popup/components/uni-popup/uni-popup.vue'

async function allPages(api) {
  const result = []
  for (let pageNum = 1; ; pageNum++) {
    const response = await api({ pageNum, pageSize: 100 })
    const rows = response.rows || response.data || []
    result.push(...rows)
    if (!rows.length || (response.total != null ? result.length >= response.total : rows.length < 100)) return result
  }
}
export default {
  components: { uniPopup },
  data() {
    return { lists: [], items: [], selected: '__all__', loading: false, error: false, busy: false, undoItem: null, addListIndex: 0, form: { name: '', quantity: 1 }, alive: true }
  },
  computed: {
    filterOptions() { return [{ id: '__all__', name: '全部清单' }, ...this.lists] },
    filterIndex() { return Math.max(0, this.filterOptions.findIndex(item => String(item.id) === String(this.selected))) },
    groups() { return groupItems(this.items.filter(item => this.selected === '__all__' || String(item.listId) === String(this.selected))) }
  },
  mounted() { this.reload() },
  beforeUnmount() { this.alive = false },
  methods: {
    async reload() {
      if (this.loading || this.busy || !this.$store.getters.token) return
      this.loading = true
      this.error = false
      try {
        const lists = await allPages(params => listShoppingList(params))
        const items = await Promise.all(lists.map(list => allPages(params => listShoppingItem(list.id, params))))
        if (!this.alive) return
        this.lists = lists
        this.items = items.flat()
        this.undoItem = null
        if (!lists.some(list => String(list.id) === String(this.selected))) this.selected = '__all__'
      } catch { if (this.alive) this.error = true } finally { this.loading = false }
    },
    listName(id) { return this.lists.find(list => String(list.id) === String(id))?.name || '购物清单' },
    changeFilter(event) { this.selected = this.filterOptions[Number(event.detail.value)].id },
    openList(id) {
      uni.navigateTo({ url: '/pages_life/shoppingList/detail?id=' + encodeURIComponent(id) + '&name=' + encodeURIComponent(this.listName(id)) })
    },
    viewAll() {
      if (this.selected !== '__all__') this.openList(this.selected)
      else uni.navigateTo({ url: '/pages_life/shoppingList/index' })
    },
    async markBought(item) {
      if (this.busy) return
      this.busy = true
      try {
        await updateShoppingItem({ id: item.id, checked: true })
        item.checked = true
        item.fulfillmentState = 'purchased'
        this.undoItem = item
      } catch { uni.showToast({ title: '标记失败，请重试', icon: 'none' }) } finally { this.busy = false }
    },
    async undo() {
      if (this.busy || !this.undoItem) return
      this.busy = true
      try {
        await updateShoppingItem({ id: this.undoItem.id, checked: false })
        this.undoItem.checked = false
        this.undoItem.fulfillmentState = 'needed'
        this.undoItem = null
      } catch { uni.showToast({ title: '撤销失败，请刷新后重试', icon: 'none' }) } finally { this.busy = false }
    },
    openAdd() {
      if (!this.lists.length) { uni.navigateTo({ url: '/pages_life/shoppingList/index' }); return }
      this.form = { name: '', quantity: 1 }
      this.addListIndex = Math.max(0, this.lists.findIndex(list => String(list.id) === String(this.selected)))
      this.$refs.addPopup.open()
    },
    async add() {
      if (this.busy) return
      const name = this.form.name.trim(), list = this.lists[this.addListIndex]
      if (!name || !list || !this.form.quantity) { uni.showToast({ title: '请填写物品名称、数量和清单', icon: 'none' }); return }
      this.busy = true
      try {
        await addShoppingItem({ listId: list.id, name, quantity: this.form.quantity, checked: false })
        this.selected = list.id
        this.$refs.addPopup.close()
        uni.showToast({ title: '已添加', icon: 'success' })
      } catch { uni.showToast({ title: '添加失败，请重试', icon: 'none' }); return } finally { this.busy = false }
      await this.reload()
    }
  }
}
</script>

<style lang="scss" scoped>
.shopping-card { background:#fff; border:1rpx solid #e8eef6; border-radius:28rpx; box-shadow:0 6rpx 24rpx rgba(37,70,108,.03); }
.shopping-header,.shopping-heading,.shopping-filter,.shopping-row,.shopping-footer,.undo-row,.sheet-header { display:flex; align-items:center; }
.shopping-header { justify-content:space-between; padding:28rpx; gap:12rpx; }
.shopping-heading { gap:12rpx; }
.shopping-icon { width:58rpx; height:58rpx; border-radius:16rpx; background:#edf5ff; display:flex; align-items:center; justify-content:center; }
.shopping-title { font-size:30rpx; font-weight:600; color:#334155; }
.shopping-count { font-size:20rpx; padding:6rpx 12rpx; border-radius:24rpx; background:#edf5ff; color:#409eff; }
button { margin:0; padding:0; line-height:1.5; border-radius:12rpx; }
button::after { border:none; }
.add-button { background:#409eff; color:#fff; padding:12rpx 18rpx; font-size:24rpx; flex-shrink:0; }
.shopping-filter { justify-content:space-between; gap:12rpx; padding:0 28rpx 22rpx; border-bottom:1rpx solid #f0f3f8; }
.picker-label { font-size:24rpx; border:1rpx solid #dfe8f4; border-radius:12rpx; padding:10rpx 16rpx; color:#71849e; max-width:320rpx; overflow:hidden; white-space:nowrap; text-overflow:ellipsis; }
.shopping-hint { font-size:22rpx; color:#a1adbd; }
.shopping-row { margin:0 28rpx; min-height:128rpx; gap:18rpx; border-bottom:1rpx solid #f0f3f8; }
.check-button { width:72rpx; min-height:88rpx; display:flex; align-items:center; justify-content:center; background:transparent; flex-shrink:0; }
.check-square { width:36rpx; height:36rpx; border:2rpx solid #cbdbee; border-radius:10rpx; }
.shopping-copy { flex:1; min-width:0; padding:18rpx 0; }
.item-name { display:block; font-size:28rpx; color:#334155; overflow-wrap:anywhere; }
.item-meta { margin-top:10rpx; font-size:22rpx; color:#9baabd; }
.meal-source { color:#c29b58; margin-left:12rpx; }
.item-quantity { font-size:24rpx; color:#7e91ab; flex-shrink:0; }
.shopping-empty { padding:42rpx 28rpx; color:#96a5b8; font-size:24rpx; text-align:center; }
.text-button { background:transparent; color:#409eff; font-size:24rpx; padding:12rpx 0; flex-shrink:0; }
.view-all { background:transparent; text-align:center; padding:22rpx; color:#7f9dbc; font-size:24rpx; }
.shopping-footer { padding:12rpx 28rpx; justify-content:space-between; gap:10rpx; background:#f6faff; border-radius:0 0 28rpx 28rpx; color:#7d95b0; font-size:22rpx; }
.undo-row { padding:8rpx 28rpx; gap:16rpx; font-size:22rpx; color:#7e91ab; justify-content:space-between; }
.undo-row text { min-width:0; overflow-wrap:anywhere; }
.add-sheet { padding:28rpx 32rpx calc(32rpx + env(safe-area-inset-bottom)); }
.sheet-header { justify-content:space-between; font-size:32rpx; font-weight:600; color:#334155; }
.field-label { display:block; margin:24rpx 0 14rpx; font-size:24rpx; color:#7e91ab; }
.field-input { box-sizing:border-box; height:84rpx; line-height:48rpx; padding:18rpx; border:1rpx solid #dfe8f4; border-radius:12rpx; font-size:28rpx; color:#334155; }
.submit-button { margin-top:32rpx; padding:20rpx; background:#409eff; color:white; font-size:28rpx; }
</style>
