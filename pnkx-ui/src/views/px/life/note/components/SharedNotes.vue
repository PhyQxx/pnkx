<template>
  <section class="shared-notes" v-loading="busy">
    <header>
      <h2>{{ token ? '分享的笔记' : '共享给我的' }}</h2>
      <el-button @click="reload" :disabled="busy">刷新</el-button>
      <el-button v-if="path.length" @click="back" :disabled="busy || saving">返回上级</el-button>
    </header>
    <p v-if="error" role="alert">{{ error }}</p>
    <template v-if="!current && !error">
      <el-empty v-if="!grants.length && !busy" description="暂无共享给你的笔记" />
      <el-button v-for="g in grants" :key="g.id" class="resource" @click="openGrant(g)">
        {{ g.resource_type === 'folder' ? '📁' : '📄' }} {{ g.title }} · {{ g.permission === 'edit' ? '可编辑' : '只读' }}
      </el-button>
    </template>
    <template v-if="current">
      <p class="breadcrumbs">{{ path.map(p => p.title).join(' / ') }}</p>
      <template v-if="current.type === 'folder'">
        <h3>{{ current.title }}</h3>
        <el-empty v-if="!current.children.length" description="文件夹为空" />
        <el-button v-for="item in current.children" :key="`${item.type}-${item.id}`" class="resource" @click="navigate(item)">
          {{ item.type === 'folder' ? '📁' : '📄' }} {{ item.title }}
        </el-button>
      </template>
      <template v-else>
        <template v-if="current.editable">
          <el-input v-model="current.title" aria-label="笔记标题" />
          <Editor :key="current.id" v-model="current.richText" :min-height="300" />
          <el-button type="primary" :loading="saving" @click="save">保存修改</el-button>
        </template>
        <template v-else>
          <h3>{{ current.title }}</h3>
          <article v-if="current.richText" v-html="safeHtml" />
          <pre v-else>{{ current.content }}</pre>
        </template>
      </template>
    </template>
  </section>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import DOMPurify from 'dompurify'
import { ElMessage, ElMessageBox } from 'element-plus'
import Editor from '@/components/Editor'
import { receivedShares, browseShare, updateNote } from '@/api/px/life/note'
const props = defineProps({ token: { type: String, default: '' } })
const grants = ref([]), current = ref(null), path = ref([]), shareId = ref(null)
const busy = ref(false), saving = ref(false), error = ref(''), saved = ref('')
const safeHtml = computed(() => DOMPurify.sanitize(current.value?.richText || ''))
const signature = () => JSON.stringify([current.value?.title, current.value?.richText])
async function canLeave() {
  if (current.value?.editable && signature() !== saved.value) {
    try { await ElMessageBox.confirm('有尚未保存的修改，确定离开？', '提示'); return true } catch { return false }
  }
  return true
}
async function load(item, nextPath) {
  busy.value = true; error.value = ''; current.value = null
  try {
    const data = (await browseShare(shareId.value, props.token, item ? { type: item.type, id: item.id } : {})).data
    if (data.richText) data.richText = DOMPurify.sanitize(data.richText)
    // Older notes may only have plain content.
    if (data.editable && !data.richText) data.richText = DOMPurify.sanitize((data.content || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/\n/g, '<br>'))
    current.value = data; saved.value = signature()
    path.value = nextPath ?? [{ type: data.type, id: data.id, title: data.title }]
  } catch (e) { error.value = e.message || '共享已失效或无权访问' }
  finally { busy.value = false }
}
async function openGrant(g) { shareId.value = g.id; await load(null) }
async function navigate(item) { if (await canLeave()) await load(item, [...path.value, item]) }
async function back() {
  if (!(await canLeave())) return
  if (path.value.length > 1) { const next = path.value.slice(0, -1); await load(next[next.length - 1], next) }
  else if (!props.token) { path.value = []; current.value = null; await reload() }
  else await load(null)
}
async function reload() {
  if (!(await canLeave())) return
  if (props.token || (shareId.value && path.value.length)) { await load(path.value.at(-1), path.value.length ? path.value : undefined); return }
  busy.value = true; error.value = ''
  try { grants.value = (await receivedShares()).data }
  catch (e) { error.value = e.message || '加载共享失败' }
  finally { busy.value = false }
}
async function save() {
  saving.value = true
  try {
    const div = document.createElement('div'); div.innerHTML = DOMPurify.sanitize(current.value.richText || '')
    await updateNote({ id: current.value.id, title: current.value.title, richText: current.value.richText, content: div.textContent })
    saved.value = signature(); ElMessage.success('已保存')
  } catch (e) { error.value = e.message || '保存失败，请确认共享是否仍有效' }
  finally { saving.value = false }
}
onMounted(reload)
</script>
<style scoped>
.shared-notes { padding: 24px; overflow: auto; height: 100%; box-sizing: border-box; }
header { display: flex; gap: 12px; align-items: center; }
header h2 { flex: 1; }
.resource { display: flex; margin: 12px 0; max-width: 100%; white-space: normal; height: auto; min-height: 40px; }
.breadcrumbs { color: var(--text-secondary); }
article { overflow-wrap: anywhere; line-height: 1.7; }
article :deep(img) { max-width: 100%; }
pre { white-space: pre-wrap; overflow-wrap: anywhere; }
</style>
