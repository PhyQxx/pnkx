<template>
    <div class="app-container book-page">
        <el-form :model="queryParams" inline class="search-bar" @submit.prevent>
            <el-form-item label="书名">
                <el-input v-model="queryParams.title" placeholder="请输入书名" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="作者">
                <el-input v-model="queryParams.author" placeholder="请输入作者" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item>
                <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
                <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
            </el-form-item>
        </el-form>

        <div class="table-toolbar">
            <div>
                <h2>我的书城</h2>
                <span class="sub-title">收藏书籍，按章节沉浸阅读</span>
            </div>
            <div class="toolbar-actions">
                <el-button :icon="UploadFilled" @click="openImportDialog">批量导入TXT</el-button>
                <el-button
                    :icon="Download"
                    :disabled="selectedBooks.length === 0"
                    :loading="batchExporting"
                    @click="handleBatchExport"
                >批量导出{{ selectedBooks.length ? `(${selectedBooks.length})` : '' }}</el-button>
                <el-button type="primary" :icon="Plus" @click="handleAdd">新增书籍</el-button>
            </div>
        </div>

        <el-table v-loading="loading" :data="bookList" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="48" align="center" />
            <el-table-column label="书名" prop="title" min-width="220">
                <template #default="scope">
                    <el-link type="primary" :underline="false" @click="openChapters(scope.row)">
                        {{ scope.row.title }}
                    </el-link>
                </template>
            </el-table-column>
            <el-table-column label="作者" prop="author" min-width="140" show-overflow-tooltip />
            <el-table-column label="章节" prop="chapterCount" width="90" align="center" />
            <el-table-column label="阅读进度" min-width="190">
                <template #default="scope">
                    <el-link
                        v-if="scope.row.lastReadChapterId"
                        type="primary"
                        :underline="false"
                        @click="continueReading(scope.row)"
                    >
                        上次读到：{{ scope.row.lastReadChapterName }}
                    </el-link>
                    <el-link
                        v-else-if="scope.row.firstChapterId"
                        type="primary"
                        :underline="false"
                        @click="continueReading(scope.row)"
                    >开始阅读</el-link>
                    <span v-else class="not-read">暂无章节</span>
                </template>
            </el-table-column>
            <el-table-column label="状态" prop="status" width="100" align="center">
                <template #default="scope">
                    <el-tag :type="statusMap[scope.row.status]?.type || 'info'">
                        {{ statusMap[scope.row.status]?.label || '未设置' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="简介" prop="description" min-width="220" show-overflow-tooltip />
            <el-table-column label="更新时间" prop="updateTime" width="170">
                <template #default="scope">{{ scope.row.updateTime || scope.row.createTime || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="380" fixed="right">
                <template #default="scope">
                    <el-button
                        v-if="scope.row.lastReadChapterId || scope.row.firstChapterId"
                        link
                        :type="scope.row.lastReadChapterId ? 'success' : 'primary'"
                        :icon="Reading"
                        @click="continueReading(scope.row)"
                    >{{ scope.row.lastReadChapterId ? '继续阅读' : '开始阅读' }}</el-button>
                    <el-button link type="primary" :icon="Reading" @click="openChapters(scope.row)">章节</el-button>
                    <el-button link type="primary" :icon="Download" @click="handleExport(scope.row)">导出</el-button>
                    <el-button link type="primary" :icon="Edit" @click="handleEdit(scope.row)">编辑</el-button>
                    <el-button link type="danger" :icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <pagination
            v-show="total > 0"
            :total="total"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            @pagination="getList"
        />

        <el-dialog v-model="dialog.visible" :title="dialog.title" width="560px" append-to-body>
            <el-form ref="bookForm" :model="form" :rules="rules" label-width="76px">
                <el-form-item label="书名" prop="title">
                    <el-input v-model="form.title" maxlength="200" show-word-limit placeholder="请输入书名" />
                </el-form-item>
                <el-form-item label="作者" prop="author">
                    <el-input v-model="form.author" maxlength="100" placeholder="请输入作者" />
                </el-form-item>
                <el-form-item label="状态" prop="status">
                    <el-select v-model="form.status" style="width: 100%">
                        <el-option label="在读" value="reading" />
                        <el-option label="已读完" value="finished" />
                        <el-option label="已搁置" value="shelved" />
                    </el-select>
                </el-form-item>
                <el-form-item label="简介" prop="description">
                    <el-input v-model="form.description" type="textarea" :rows="4" maxlength="1000" show-word-limit />
                </el-form-item>
                <el-form-item label="备注" prop="remark">
                    <el-input v-model="form.remark" maxlength="255" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialog.visible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="importDialog.visible" title="批量导入TXT书籍" width="960px" top="3vh" append-to-body>
            <el-upload
                ref="txtUpload"
                v-model:file-list="importDialog.uploadFiles"
                drag
                multiple
                action="#"
                accept=".txt,text/plain"
                :auto-upload="false"
                :limit="20"
                :on-change="handleTxtFileChange"
                :on-remove="handleTxtFileRemove"
                :on-exceed="handleTxtFileExceed"
            >
                <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                <div class="el-upload__text">拖入多个 TXT 文件，或<em>点击选择</em></div>
                <template #tip>
                    <div class="el-upload__tip">每次最多20本；单个不超过20MB、总大小不超过80MB；支持 UTF-8、GB18030、UTF-16</div>
                </template>
            </el-upload>

            <div v-if="importDialog.items.length" class="txt-preview">
                <div class="batch-import-summary">
                    已选择 {{ importDialog.items.length }} 本，合计 {{ importTotalChapters }} 章、{{ importTotalWords }} 字
                    <span v-if="previewingCount">；正在识别 {{ previewingCount }} 个文件…</span>
                </div>
                <el-table :data="importDialog.items" max-height="520" border size="small">
                    <el-table-column type="expand">
                        <template #default="scope">
                            <div v-if="scope.row.preview" class="chapter-preview-wrap">
                                <el-table :data="scope.row.preview.chapters" max-height="240" size="small" border>
                                    <el-table-column label="序号" prop="chapterNo" width="70" align="center" />
                                    <el-table-column label="识别到的章节名" prop="chapterName" min-width="300" show-overflow-tooltip />
                                    <el-table-column label="字数" prop="wordCount" width="90" align="right" />
                                </el-table>
                                <div v-if="scope.row.preview.chapterCount > scope.row.preview.chapters.length" class="preview-more">
                                    这里只预览前 {{ scope.row.preview.chapters.length }} 章，确认后导入全部章节
                                </div>
                            </div>
                            <el-alert v-else-if="scope.row.error" :title="scope.row.error" type="error" :closable="false" />
                            <div v-else class="preview-loading"><el-icon class="is-loading"><Loading /></el-icon> 正在识别…</div>
                        </template>
                    </el-table-column>
                    <el-table-column label="文件" prop="file.name" min-width="150" show-overflow-tooltip />
                    <el-table-column label="书名" min-width="190">
                        <template #default="scope">
                            <el-input v-model="scope.row.title" maxlength="200" placeholder="书名" />
                        </template>
                    </el-table-column>
                    <el-table-column label="作者" min-width="130">
                        <template #default="scope">
                            <el-input v-model="scope.row.author" maxlength="100" placeholder="可选" />
                        </template>
                    </el-table-column>
                    <el-table-column label="状态" width="110">
                        <template #default="scope">
                            <el-select v-model="scope.row.status">
                                <el-option label="在读" value="reading" />
                                <el-option label="已读完" value="finished" />
                                <el-option label="已搁置" value="shelved" />
                            </el-select>
                        </template>
                    </el-table-column>
                    <el-table-column label="编码" width="95" align="center">
                        <template #default="scope">{{ scope.row.preview?.encoding || '-' }}</template>
                    </el-table-column>
                    <el-table-column label="章节" width="75" align="center">
                        <template #default="scope">
                            <el-icon v-if="scope.row.previewing" class="is-loading"><Loading /></el-icon>
                            <span v-else-if="scope.row.error" class="import-error">失败</span>
                            <span v-else>{{ scope.row.preview?.chapterCount || 0 }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="70" align="center">
                        <template #default="scope">
                            <el-button link type="danger" @click="removeImportItem(scope.row)">移除</el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </div>
            <template #footer>
                <el-button @click="importDialog.visible = false">取消</el-button>
                <el-button
                    type="primary"
                    :disabled="!canSubmitTxtImport"
                    :loading="importDialog.importing"
                    @click="submitTxtImport"
                >确认导入{{ importDialog.items.length ? `(${importDialog.items.length})` : '' }}</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { computed, reactive, ref, getCurrentInstance } from 'vue'
import { Delete, Download, Edit, Loading, Plus, Reading, Refresh, Search, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
    addBook,
    deleteBooks,
    exportBookTxt,
    exportBooksTxt,
    getBook,
    importBooksTxt,
    listBooks,
    previewBookTxt,
    updateBook
} from '@/api/px/app/book'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const saving = ref(false)
const batchExporting = ref(false)
const total = ref(0)
const bookList = ref([])
const selectedBooks = ref([])
const bookForm = ref()
const txtUpload = ref()
const queryParams = reactive({ pageNum: 1, pageSize: 10, title: '', author: '' })
const dialog = reactive({ visible: false, title: '' })
const importDialog = reactive({
    visible: false,
    importing: false,
    uploadFiles: [],
    items: []
})
const form = reactive({ id: undefined, title: '', author: '', description: '', status: 'reading', remark: '' })
const statusMap = {
    reading: { label: '在读', type: 'primary' },
    finished: { label: '已读完', type: 'success' },
    shelved: { label: '已搁置', type: 'info' }
}
const rules = { title: [{ required: true, message: '书名不能为空', trigger: 'blur' }] }
const previewingCount = computed(() => importDialog.items.filter(item => item.previewing).length)
const importTotalChapters = computed(() => importDialog.items.reduce((sum, item) => sum + (item.preview?.chapterCount || 0), 0))
const importTotalWords = computed(() => importDialog.items.reduce((sum, item) => sum + (item.preview?.totalWordCount || 0), 0))
const canSubmitTxtImport = computed(() => importDialog.items.length > 0
    && !importDialog.importing
    && importDialog.items.every(item => item.preview && !item.previewing && !item.error && item.title.trim()))

function getList() {
    loading.value = true
    listBooks(queryParams).then(res => {
        bookList.value = res.rows || []
        total.value = res.total || 0
    }).finally(() => { loading.value = false })
}

function resetForm() {
    Object.assign(form, { id: undefined, title: '', author: '', description: '', status: 'reading', remark: '' })
    bookForm.value?.clearValidate()
}

function handleQuery() {
    queryParams.pageNum = 1
    getList()
}

function resetQuery() {
    Object.assign(queryParams, { pageNum: 1, title: '', author: '' })
    getList()
}

function handleAdd() {
    resetForm()
    dialog.title = '新增书籍'
    dialog.visible = true
}

function openImportDialog() {
    resetTxtImport()
    importDialog.visible = true
    txtUpload.value?.clearFiles()
}

function resetTxtImport() {
    importDialog.importing = false
    importDialog.uploadFiles = []
    importDialog.items = []
}

function handleTxtFileChange(uploadFile) {
    const file = uploadFile.raw
    if (!file || importDialog.items.some(item => item.uid === uploadFile.uid)) return
    if (!file.name.toLowerCase().endsWith('.txt')) {
        ElMessage.warning('仅支持 TXT 文件')
        importDialog.uploadFiles = importDialog.uploadFiles.filter(item => item.uid !== uploadFile.uid)
        return
    }
    if (file.size > 20 * 1024 * 1024) {
        ElMessage.warning('TXT 文件不能超过 20MB')
        importDialog.uploadFiles = importDialog.uploadFiles.filter(item => item.uid !== uploadFile.uid)
        return
    }
    const totalSize = importDialog.items.reduce((sum, item) => sum + item.file.size, 0) + file.size
    if (totalSize > 80 * 1024 * 1024) {
        ElMessage.warning('批量导入文件总大小不能超过 80MB')
        importDialog.uploadFiles = importDialog.uploadFiles.filter(item => item.uid !== uploadFile.uid)
        return
    }
    const item = reactive({
        uid: uploadFile.uid,
        file,
        title: file.name.replace(/\.txt$/i, ''),
        author: '',
        status: 'reading',
        preview: null,
        previewing: true,
        error: ''
    })
    importDialog.items.push(item)
    previewBookTxt(file).then(res => {
        item.preview = res.data
        item.title = res.data?.suggestedTitle || item.title
    }).catch(error => {
        item.error = error?.message || '文件解析失败'
    }).finally(() => { item.previewing = false })
}

function handleTxtFileRemove(uploadFile) {
    importDialog.items = importDialog.items.filter(item => item.uid !== uploadFile.uid)
}

function handleTxtFileExceed() {
    ElMessage.warning('每次最多选择 20 个 TXT 文件')
}

function removeImportItem(item) {
    importDialog.items = importDialog.items.filter(current => current.uid !== item.uid)
    importDialog.uploadFiles = importDialog.uploadFiles.filter(file => file.uid !== item.uid)
}

function submitTxtImport() {
    if (!canSubmitTxtImport.value) return
    importDialog.importing = true
    importBooksTxt(importDialog.items.map(item => ({
        file: item.file,
        title: item.title.trim(),
        author: item.author.trim(),
        status: item.status
    }))).then(res => {
        importDialog.visible = false
        getList()
        ElMessage.success(`成功导入 ${res.bookCount || importDialog.items.length} 本，共 ${res.chapterCount || importTotalChapters.value} 章`)
    }).finally(() => { importDialog.importing = false })
}

function handleSelectionChange(selection) {
    selectedBooks.value = selection
}

function handleBatchExport() {
    if (!selectedBooks.value.length) return
    if (selectedBooks.value.length > 100) {
        ElMessage.warning('每次最多批量导出 100 本书')
        return
    }
    proxy.$confirm(`确认将选中的 ${selectedBooks.value.length} 本书打包导出为 ZIP 吗？`, '批量导出', {
        confirmButtonText: '导出',
        cancelButtonText: '取消',
        type: 'info'
    }).then(() => {
        batchExporting.value = true
        return exportBooksTxt(selectedBooks.value)
    }).then(() => {
        ElMessage.success('批量导出完成')
    }).catch(error => {
        if (error !== 'cancel' && error !== 'close') ElMessage.error('批量导出失败')
    }).finally(() => { batchExporting.value = false })
}

function handleExport(row) {
    exportBookTxt(row).then(() => {
        ElMessage.success('TXT 已导出')
    }).catch(() => {
        ElMessage.error('导出失败')
    })
}

function handleEdit(row) {
    getBook(row.id).then(res => {
        resetForm()
        Object.assign(form, res.data || {})
        dialog.title = '编辑书籍'
        dialog.visible = true
    })
}

function submitForm() {
    bookForm.value.validate(valid => {
        if (!valid) return
        saving.value = true
        const action = form.id ? updateBook(form) : addBook(form)
        action.then(() => {
            dialog.visible = false
            getList()
            ElMessage.success('保存成功')
        }).finally(() => { saving.value = false })
    })
}

function handleDelete(row) {
    proxy.$confirm(`确认删除《${row.title}》及其全部章节吗？`, '删除提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => deleteBooks(row.id)).then(() => {
        getList()
        ElMessage.success('删除成功')
    }).catch(() => {})
}

function openChapters(row) {
    proxy.$router.push({ name: 'MyBookChapter', query: { bookId: row.id } })
}

function continueReading(row) {
    const chapterId = row.lastReadChapterId || row.firstChapterId
    if (!chapterId) return
    proxy.$router.push({ name: 'MyBookReader', params: { chapterId } })
}

getList()
</script>

<style lang="scss" scoped>
.book-page {
    .search-bar { padding: 16px 18px 0; background: var(--pnkx-surface, #fff); border-radius: 10px; }
    .table-toolbar { display: flex; justify-content: space-between; align-items: center; margin: 18px 0 14px; }
    .toolbar-actions { display: flex; }
    h2 { display: inline; margin: 0 12px 0 0; font-size: 20px; }
    .sub-title { color: var(--pnkx-text-secondary, #909399); font-size: 13px; }
    .not-read { color: var(--pnkx-text-placeholder, #a8abb2); }
    .txt-preview { margin-top: 18px; }
    .batch-import-summary { margin-bottom: 10px; color: var(--pnkx-text-secondary, #606266); }
    .chapter-preview-wrap { padding: 4px 18px 8px 48px; }
    .preview-loading { padding: 28px 0 12px; text-align: center; color: var(--pnkx-text-secondary, #606266); }
    .preview-more { margin-top: 8px; color: var(--pnkx-text-secondary, #909399); font-size: 12px; text-align: right; }
    .import-error { color: var(--el-color-danger); }
}
</style>
