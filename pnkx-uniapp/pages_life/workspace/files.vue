<template>
 <view class="page ws-page subpage-shell"><view class="heading">个人文件</view><text>{{ size(used) }} / {{ size(quota) }}，回收站仍占空间</text><view class="toolbar"><button size="mini" @click="openFolder(0)">根目录</button><button size="mini" @click="newFolder">新建目录</button><button size="mini" @click="recovery">回收站</button></view>
 <!-- #ifdef H5 --><input type="file" @change="uploadWeb"/><!-- #endif -->
 <!-- #ifndef H5 --><button :disabled="saving" @click="chooseImage">上传图片（20MB 内）</button><!-- #endif -->
 <view v-for="row in rows" :key="row.id" class="card"><text class="title" @click="row.isFolder ? openFolder(row.id) : preview(row)">{{ row.isFolder ? '📁' : '📄' }} {{ row.displayName }}</text><text>{{ row.isFolder ? '' : size(row.byteSize) }}</text><button size="mini" @click="edit(row)">改名/移动</button><button size="mini" @click="remove(row)">移入回收站</button><resource-links v-if="!row.isFolder" type="file" :id="row.id"/></view><button v-if="hasMore" @click="load(nextAfter)">加载更多</button>
 <view v-if="form" class="card"><input v-model="form.displayName" placeholder="名称" maxlength="200"/><picker :range="folderLabels" @change="form.parentId = Number($event.detail.value) === 0 ? 0 : folders[Number($event.detail.value)-1].id"><button>选择目标目录 #{{ form.parentId }}</button></picker><button :disabled="saving" @click="save">保存</button><button @click="form = null">取消</button></view><view v-if="text !== null" class="card"><button size="mini" @click="text = null">关闭预览</button><text class="content">{{ text }}</text></view>
 </view>
</template>
<script>
import ResourceLinks from '@/components/resource-links/index.vue'
import { listFiles, listFileFolders, getFile, createFolder, editFile, deleteFile, fileText } from '@/api/px/life/workspace'
import upload from '@/utils/upload'
import config from '@/config'
import { getToken } from '@/utils/auth'
export default {
 components: { ResourceLinks },data() { return { rows: [], folders: [], parent: 0, used: 0, quota: 0, nextAfter: 0, hasMore: false, form: null, saving: false, text: null, sequence: 0 } },computed: { folderLabels() { return ['根目录',...this.folders.map(f => f.displayName + ' (#' + f.id + ')')] } },
 async onLoad(query) { this.folders = (await listFileFolders()).data; if (query.id) { const row = (await getFile(query.id)).data; this.parent = row.isFolder ? row.id : row.parentId; if (!row.isFolder) await this.preview(row) } await this.load(0) },
 methods: {
 size(value) { return (Number(value || 0)/1024/1024).toFixed(2) + ' MB' },
 async load(after = 0) { const sequence = ++this.sequence; const result = await listFiles(this.parent,after); if (sequence !== this.sequence) return; this.rows = after ? [...this.rows,...result.data.rows] : result.data.rows; Object.assign(this,{ used: result.data.usedBytes, quota: result.data.quotaBytes, nextAfter: result.data.nextAfter, hasMore: result.data.hasMore }) },
 async openFolder(parent) { this.parent = parent; await this.load(0) },
 newFolder() { uni.showModal({ title: '新建目录', editable: true, placeholderText: '名称', success: async result => { if (result.confirm) { await createFolder(this.parent,result.content); this.folders = (await listFileFolders()).data; await this.load(0) } } }) },
 recovery() { uni.navigateTo({ url: '/pages_life/workspace/recovery' }) },
 edit(row) { this.form = { ...row } },
 async save() { this.saving = true; try { await editFile(this.form.id,this.form); this.form = null; this.folders = (await listFileFolders()).data; await this.load(0) } finally { this.saving = false } },
 remove(row) { uni.showModal({ title: '移入回收站', content: '非空目录须先处理其中项目，是否继续？', success: async result => { if (result.confirm) { await deleteFile(row); this.folders = (await listFileFolders()).data; await this.load(0) } } }) },
 chooseImage() { uni.chooseImage({ count: 1, success: result => this.uploadPath(result.tempFilePaths[0]) }) },
 async uploadPath(filePath,displayName) { this.saving = true; try { await upload({ url: '/workspace/files/upload', filePath, formData: { parent: this.parent, ...(displayName ? { displayName } : {}) }, timeout: 120000 }); await this.load(0) } finally { this.saving = false } },
 // #ifdef H5
 async uploadWeb(event) { const file = event.target.files?.[0]; if (!file) return; const url = URL.createObjectURL(file); try { await this.uploadPath(url,file.name) } finally { URL.revokeObjectURL(url); event.target.value = '' } },
 // #endif
 async preview(row) {
  if (row.mime === 'text/plain') { this.text = (await fileText(row.id)).data; return }
  uni.downloadFile({ url: config.baseUrl + `/workspace/files/${row.id}/content?preview=true`, header: { Authorization: 'Bearer ' + getToken() }, success: result => {
   if (result.statusCode !== 200) { uni.showToast({ title: '文件不可访问', icon: 'none' }); return }
   if (row.mime.startsWith('image/')) uni.previewImage({ urls: [result.tempFilePath] });
   else {
    // #ifdef H5
    const anchor = document.createElement('a'); anchor.href = result.tempFilePath; anchor.download = row.displayName; anchor.click();
    // #endif
    // #ifndef H5
    if (row.mime === 'application/pdf') uni.openDocument({ filePath: result.tempFilePath, fileType: 'pdf', showMenu: true }); else uni.showToast({ title: '此格式请在 PC 下载打开', icon: 'none' });
    // #endif
   }
  }, fail: () => uni.showToast({ title: '下载失败，请重试', icon: 'none' }) })
 }
 }
}
</script>
<style scoped>
@import '@/static/scss/workspace-common.scss';.page { padding: 32rpx; }.heading { font-size: 36rpx; font-weight: 600; margin-bottom: 20rpx; }.toolbar { display: flex; gap: 10rpx; margin: 20rpx 0; }.card { border: 1px solid $border-color; border-radius: 20rpx; padding: 24rpx; margin: 20rpx 0; }.title { display: block; font-weight: 600; margin-bottom: 16rpx; }.content { display: block; white-space: pre-wrap; overflow-wrap: anywhere; }</style>
