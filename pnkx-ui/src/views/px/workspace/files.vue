<template>
 <section class="files" v-loading="loading"><header v-if="!embedded"><h1>个人文件</h1><router-link to="/resources/hub">资料中心</router-link> · <router-link to="/resources/recovery">回收站</router-link></header><p>已使用 {{ size(used) }} / {{ size(quota) }}；回收站仍占用空间。单文件上限 20 MB。</p>
 <div class="toolbar"><el-button @click="openFolder(0)">根目录</el-button><el-button v-if="parent" @click="openFolder(folders.find(f => String(f.id) === String(parent))?.parentId || 0)">上一级</el-button><span>{{ folders.find(f => String(f.id) === String(parent))?.displayName || '根目录' }}</span><el-button @click="newFolder">新建目录</el-button><label class="upload">上传文件 <input type="file" :disabled="saving" @change="upload" /></label></div>
 <article v-for="row in rows" :key="row.id"><el-button text @click="row.isFolder ? openFolder(row.id) : preview(row)">{{ row.isFolder ? '📁' : '📄' }} {{ row.displayName }}</el-button><small>{{ row.isFolder ? '' : size(row.byteSize) }}</small><el-button v-if="!row.isFolder" text @click="download(row)">下载</el-button><el-button text @click="edit(row)">改名/移动</el-button><el-button text type="danger" @click="remove(row)">移入回收站</el-button></article><el-empty v-if="!rows.length" description="目录为空"/><el-button v-if="hasMore" @click="load(nextAfter)">加载更多</el-button>
 <el-dialog v-model="editing" title="改名或移动"><el-input v-model="form.displayName" maxlength="200"/><el-select v-model="form.parentId"><el-option label="根目录" :value="0"/><el-option v-for="folder in folders.filter(f => f.id !== form.id)" :key="folder.id" :label="folder.displayName + ' (#' + folder.id + ')'" :value="folder.id"/></el-select><template #footer><el-button :loading="saving" @click="save">保存</el-button></template></el-dialog>
 <el-dialog v-model="previewing" :title="current?.displayName || '预览'" width="min(900px,95vw)" @close="clearPreview"><img v-if="current?.mime?.startsWith('image/')" :src="previewUrl" class="image"/><pre v-else-if="current?.mime === 'text/plain'">{{ previewText }}</pre><iframe v-else-if="current?.mime === 'application/pdf'" :src="previewUrl" sandbox="allow-same-origin" title="PDF 预览"/><p v-else>该格式暂不支持预览，请下载后打开。</p><resource-links v-if="current" type="file" :id="current.id"/></el-dialog>
 </section>
</template>
<script>
import ResourceLinks from '@/components/ResourceLinks'
import { listFiles, listFileFolders, getFile, createFolder, editFile, deleteFile, uploadPrivateFile, fileContent } from '@/api/workspace'
export default {
 name: 'PrivateFiles', components: { ResourceLinks }, props: { embedded: Boolean }, data() { return { rows: [], folders: [], parent: 0, used: 0, quota: 0, hasMore: false, nextAfter: 0, loading: false, saving: false, editing: false, form: {}, previewing: false, previewUrl: '', previewText: '', current: null, sequence: 0, previewSequence: 0, initSequence: 0 } },
 mounted() { this.init() }, beforeUnmount() { this.initSequence++; this.sequence++; this.clearPreview() }, watch: { '$route.query.id'() { this.init() } },
 methods: {
 size(value) { return (Number(value || 0)/1024/1024).toFixed(2) + ' MB' },
 async init() {
   const sequence = ++this.initSequence, id = this.$route.query.id
   this.sequence++; this.rows = []; this.clearPreview(); this.loading = true
   try {
     const folders = (await listFileFolders()).data
     if (sequence !== this.initSequence) return
     this.folders = folders
     if (id) {
       const row = (await getFile(id)).data
       if (sequence !== this.initSequence) return
       this.parent = row.isFolder ? row.id : row.parentId
       if (!row.isFolder) await this.preview(row)
     } else this.parent = 0
     if (sequence === this.initSequence) await this.load(0)
   } finally { if (sequence === this.initSequence) this.loading = false }
 },
 async load(after = 0) { const sequence = ++this.sequence; this.loading = true; try { const res = await listFiles(this.parent,after); if (sequence !== this.sequence) return; this.rows = after ? [...this.rows,...res.data.rows] : res.data.rows; Object.assign(this,{ hasMore: res.data.hasMore, nextAfter: res.data.nextAfter, used: res.data.usedBytes, quota: res.data.quotaBytes }) } finally { if (sequence === this.sequence) this.loading = false } },
 async openFolder(parent) { this.initSequence++; this.parent = parent; await this.load(0) },
 async newFolder() { const { value } = await this.$prompt('输入目录名称','新建目录'); await createFolder(this.parent,value); this.folders = (await listFileFolders()).data; await this.load(0) },
 async upload(event) { const file = event.target.files[0]; if (!file) return; this.saving = true; try { await uploadPrivateFile(this.parent,file); await this.load(0) } finally { this.saving = false; event.target.value = '' } },
 edit(row) { this.form = { ...row }; this.editing = true },
 async save() { this.saving = true; try { await editFile(this.form.id,this.form); this.editing = false; this.folders = (await listFileFolders()).data; await this.load(0) } finally { this.saving = false } },
 async remove(row) { await this.$confirm('将此项目移入回收站？非空目录须先处理其内容。','删除项目'); await deleteFile(row); this.folders = (await listFileFolders()).data; await this.load(0) },
 async blob(row,preview) { const blob = await fileContent(row.id,preview); if (blob.type.includes('application/json')) { const error = JSON.parse(await blob.text()); this.$message.error(error.msg || '文件不可访问'); throw new Error(error.msg || '文件不可访问') } return blob },
 resetPreview() { this.previewSequence++; if (this.previewUrl) URL.revokeObjectURL(this.previewUrl); this.previewUrl = ''; this.previewText = ''; this.current = null },
 clearPreview() { this.resetPreview(); this.previewing = false },
 async preview(row) {
   this.resetPreview(); const sequence = this.previewSequence
   const blob = await this.blob(row,true)
   if (sequence !== this.previewSequence) return
   const text = row.mime === 'text/plain' ? await blob.text() : ''
   if (sequence !== this.previewSequence) return
   this.current = row; this.previewText = text
   if (row.mime !== 'text/plain') this.previewUrl = URL.createObjectURL(blob)
   this.previewing = true
 },
 async download(row) { const blob = await this.blob(row,false); const url = URL.createObjectURL(blob); const anchor = document.createElement('a'); anchor.href = url; anchor.download = row.displayName; anchor.click(); setTimeout(() => URL.revokeObjectURL(url),1000) }
 }
}
</script>
<style scoped>.files { padding: 24px; }header,.toolbar,article { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }header h1 { margin-right: auto; }article { border-bottom: 1px solid #ddd; padding: 16px 0; }.upload { padding: 10px; border: 1px solid #ddd; border-radius: 8px; }.upload input { max-width: 220px; }.image { max-width: 100%; max-height: 65vh; }iframe { width: 100%; height: 65vh; border: none; }pre { white-space: pre-wrap; overflow-wrap: anywhere; max-height: 65vh; overflow: auto; }</style>
