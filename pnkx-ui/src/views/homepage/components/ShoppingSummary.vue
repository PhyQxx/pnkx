<template>
  <section class="shopping-summary" :class="{ compact }" v-loading="loading">
    <div class="summary-head">
      <div class="summary-title"><span class="shopping-icon"><svg-icon icon-class="shopping" /></span><h3>购物清单</h3><el-tag size="small" round>待购 {{ groups.needed.length }}</el-tag></div>
      <div><el-button type="primary" size="small" :disabled="loading || !!error" @click="openAdd">＋ 添加</el-button><el-button link type="primary" @click="openManager">查看全部 ›</el-button></div>
    </div>
    <template v-if="!error">
      <div class="summary-filter"><el-select v-model="selected" placeholder="全部清单" aria-label="筛选购物清单"><el-option label="全部清单" :value="ALL_LISTS" /><el-option v-for="list in lists" :key="list.id" :label="list.name" :value="list.id" /></el-select><span>需要的，顺手记下来</span></div>
      <div class="shopping-grid"><div v-for="item in visibleItems" :key="item.id" class="shopping-row">
        <el-checkbox :model-value="false" :disabled="busy" :aria-label="'标记' + item.name + '已买到'" @change="markBought(item)" />
        <div class="item-copy"><div class="item-name" :title="item.name">{{ item.name }}</div><div class="item-source">{{ listName(item.listId) }}<span v-if="item.addedFromMeal">来自餐饮</span></div></div><span class="item-quantity">× {{ item.quantity }}</span>
      </div>
      </div>
      <el-empty v-if="!loading && !groups.needed.length" description="暂时没有要买的东西，想到什么随时添加" :image-size="60" />
      <div class="shopping-pager"><span>共 {{ groups.needed.length }} 项待购</span><el-button link :disabled="page === 1" @click="page--">‹</el-button><span>{{ page }} / {{ pages }}</span><el-button link :disabled="page >= pages" @click="page++">›</el-button></div>
      <div v-if="undoItem" class="undo-row">已将「{{ undoItem.name }}」标记为买到 <el-button link type="primary" :disabled="busy" @click="undo">撤销</el-button></div>
      <div v-if="groups.purchased.length" class="summary-footer"><span>✓　已买到 {{ groups.purchased.length }} 项，待处理</span><el-button link type="primary" @click="openManager">去处理 ›</el-button></div>
    </template>
    <div v-else class="load-error">购物清单加载失败 <el-button link type="primary" @click="load">重试</el-button></div>
    <el-dialog v-model="addVisible" title="添加待购物品" width="420px" append-to-body>
      <el-form label-position="top" @submit.prevent="add">
        <el-form-item label="物品名称"><el-input v-model="form.name" maxlength="80" placeholder="例如：牛奶" /></el-form-item>
        <el-form-item label="数量"><el-input-number v-model="form.quantity" :min="1" :max="999" /></el-form-item>
        <el-form-item label="加入清单"><el-select v-model="form.listId" style="width:100%"><el-option v-for="list in lists" :key="list.id" :label="list.name" :value="list.id" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="addVisible = false">取消</el-button><el-button type="primary" :loading="busy" @click="add">添加</el-button></template>
    </el-dialog>
    <el-dialog v-model="managerVisible" title="购物清单" width="88%" top="5vh" append-to-body destroy-on-close @closed="load">
      <shopping-center v-if="managerVisible" :initial-list-id="selected === ALL_LISTS ? undefined : selected" />
    </el-dialog>
  </section>
</template>
<script setup>
import { computed, ref, onMounted, onActivated, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { listShoppingList, listShoppingItem, addShoppingItem, updateShoppingItem } from '@/api/px/life/shopping'
import { groupItems } from '@/utils/shoppingGroups'
import ShoppingCenter from '@/views/px/life/shopping/index.vue'

const props = defineProps({ compact: Boolean, pageSize: { type: Number, default: 6 } })
const page = ref(1)
const ALL_LISTS = '__all_lists__'
const lists = ref([]), items = ref([]), selected = ref(ALL_LISTS), loading = ref(false), error = ref(false)
const busy = ref(false), addVisible = ref(false), managerVisible = ref(false), undoItem = ref(null)
const form = ref({ name: '', quantity: 1, listId: undefined })
const groups = computed(() => groupItems(items.value.filter(item => selected.value === ALL_LISTS || String(item.listId) === String(selected.value))))
const pages = computed(() => Math.max(1, Math.ceil(groups.value.needed.length / props.pageSize)))
const visibleItems = computed(() => groups.value.needed.slice((page.value - 1) * props.pageSize, page.value * props.pageSize))
watch(selected, () => { page.value = 1 })
watch(pages, value => { page.value = Math.min(page.value, value) })
const listName = id => lists.value.find(list => String(list.id) === String(id))?.name || '购物清单'
// Explicit pagination prevents the homepage counts from silently showing only the first page.
async function allPages(api, params = {}) {
  const result = []
  for (let pageNum = 1; ; pageNum++) {
    const response = await api({ ...params, pageNum, pageSize: 100 })
    const rows = response.rows || response.data || []
    result.push(...rows)
    if (!rows.length || (response.total != null ? result.length >= response.total : rows.length < 100)) return result
  }
}
async function load() {
  if (loading.value) return
  loading.value = true
  error.value = false
  try {
    const nextLists = await allPages(listShoppingList)
    const nextItems = await Promise.all(nextLists.map(list => allPages(listShoppingItem, { listId: list.id })))
    lists.value = nextLists
    items.value = nextItems.flat()
    if (selected.value !== ALL_LISTS && !nextLists.some(list => String(list.id) === String(selected.value))) selected.value = ALL_LISTS
    undoItem.value = null
  } catch { error.value = true } finally { loading.value = false }
}
async function markBought(item) {
  if (busy.value) return
  busy.value = true
  try {
    await updateShoppingItem({ id: item.id, checked: true })
    item.checked = true
    item.fulfillmentState = 'purchased'
    undoItem.value = item
  } catch { ElMessage.error('标记失败，请重试') } finally { busy.value = false }
}
async function undo() {
  if (busy.value || !undoItem.value) return
  busy.value = true
  try {
    await updateShoppingItem({ id: undoItem.value.id, checked: false })
    undoItem.value.checked = false
    undoItem.value.fulfillmentState = 'needed'
    undoItem.value = null
  } catch { ElMessage.error('撤销失败，请刷新清单后重试') } finally { busy.value = false }
}
function openManager() { undoItem.value = null; managerVisible.value = true }
function openAdd() {
  if (!lists.value.length) { ElMessage.info('请先创建一份购物清单'); openManager(); return }
  form.value = { name: '', quantity: 1, listId: selected.value === ALL_LISTS ? lists.value[0].id : selected.value }
  addVisible.value = true
}
async function add() {
  if (busy.value) return
  const name = form.value.name.trim()
  if (!name || !form.value.listId || !form.value.quantity) { ElMessage.warning('请填写物品名称、数量和清单'); return }
  busy.value = true
  try {
    await addShoppingItem({ ...form.value, name, checked: false })
    selected.value = form.value.listId
    addVisible.value = false
    ElMessage.success('已添加')
    await load()
  } catch { ElMessage.error('添加失败，请重试') } finally { busy.value = false }
}
onMounted(load)
onActivated(load)
</script>
<style scoped lang="scss">
.shopping-summary { background:var(--pnkx-surface); border:1px solid var(--pnkx-border); border-radius:14px; overflow:hidden; }
.summary-head,.summary-title,.summary-filter,.shopping-row,.summary-footer { display:flex; align-items:center; }
.summary-head { padding:18px 22px 14px; justify-content:space-between; gap:12px; flex-wrap:wrap; }
.summary-title { gap:10px; h3 { font-size:15px; margin:0; } }
.shopping-icon { display:grid; place-items:center; width:34px; height:34px; border-radius:10px; background:var(--pnkx-primary-soft); color:var(--pnkx-primary); }
.summary-filter { padding:0 22px 14px; justify-content:space-between; border-bottom:1px solid var(--pnkx-border-soft); .el-select { width:160px; } span { font-size:12px; color:var(--pnkx-text-muted); } }
.shopping-row { margin:0 22px; min-height:68px; gap:14px; border-bottom:1px solid var(--pnkx-border-soft); }
.item-copy { flex:1; min-width:0; padding:10px 0; }
.item-name { overflow-wrap:anywhere; font-weight:500; font-size:14px; }
.item-source { margin-top:6px; color:var(--pnkx-text-muted); font-size:12px; span { margin-left:10px; color:var(--pnkx-warning); } }
.item-quantity { color:var(--pnkx-text-secondary); font-size:13px; white-space:nowrap; }
.summary-more { width:100%; padding:15px; background:none; border:0; color:var(--pnkx-primary); cursor:pointer; }
.summary-footer { padding:14px 22px; justify-content:space-between; background:var(--pnkx-primary-soft); color:var(--pnkx-text-secondary); font-size:12px; }
.undo-row,.load-error { padding:16px 22px; color:var(--pnkx-text-secondary); font-size:13px; }

.shopping-pager { display:flex; justify-content:flex-end; align-items:center; gap:12px; padding:5px 16px; font-size:12px; color:var(--pnkx-text-muted); }
.compact { height:100%; min-height:0; display:flex; flex-direction:column; }
.compact .summary-head { padding:10px 16px; flex:none; }
.compact .summary-filter { padding:0 16px 8px; flex:none; }
.compact .shopping-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); align-content:start; flex:1; min-height:0; }
.compact .shopping-row { margin:0 16px; min-height:0; height:58px; gap:9px; }
.compact .item-name { white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.compact .item-source { font-size:11px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.compact .summary-footer,.compact .undo-row { padding:5px 16px; min-height:28px; font-size:12px; flex:none; }
.compact :deep(.el-empty) { padding:5px; flex:1; }
.compact :deep(.el-empty__image) { display:none; }
.compact :deep(.el-empty__description) { margin:0; }
</style>
