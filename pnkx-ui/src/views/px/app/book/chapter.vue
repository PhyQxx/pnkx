<template>
    <div class="app-container chapter-page">
        <el-page-header @back="backToBooks">
            <template #content>
                <span class="page-title">《{{ book.title || '我的书城' }}》章节</span>
            </template>
            <template #extra>
                <el-button :icon="Plus" @click="handleBatchAdd">批量新增</el-button>
                <el-button type="primary" :icon="Plus" @click="handleAdd">新增章节</el-button>
            </template>
        </el-page-header>

        <el-form :model="queryParams" inline class="chapter-search" @submit.prevent>
            <el-form-item label="章节名">
                <el-input v-model="queryParams.chapterName" clearable placeholder="搜索章节" @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item>
                <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
                <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
            </el-form-item>
        </el-form>

        <el-table v-loading="loading" :data="chapterList" class="chapter-table">
            <el-table-column label="序号" prop="chapterNo" width="100" align="center" />
            <el-table-column label="章节名" prop="chapterName" min-width="300">
                <template #default="scope">
                    <el-link type="primary" :underline="false" @click="openReader(scope.row)">
                        {{ scope.row.chapterName }}
                    </el-link>
                    <el-tag
                        v-if="book.lastReadChapterId === scope.row.id"
                        size="small"
                        type="success"
                        effect="plain"
                        class="reading-progress-tag"
                    >上次读到</el-tag>
                </template>
            </el-table-column>
            <el-table-column label="文章字数" prop="wordCount" width="110" align="center">
                <template #default="scope">{{ scope.row.wordCount || 0 }}</template>
            </el-table-column>
            <el-table-column label="更新时间" prop="updateTime" width="180">
                <template #default="scope">{{ scope.row.updateTime || scope.row.createTime || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="230" fixed="right">
                <template #default="scope">
                    <el-button link type="primary" :icon="View" @click="openReader(scope.row)">详情</el-button>
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

        <el-dialog v-model="dialog.visible" :title="dialog.title" width="760px" append-to-body>
            <el-form ref="chapterForm" :model="form" :rules="rules" label-width="88px">
                <el-row :gutter="16">
                    <el-col :span="17">
                        <el-form-item label="章节名" prop="chapterName">
                            <el-input v-model="form.chapterName" maxlength="255" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="7">
                        <el-form-item label="序号" prop="chapterNo">
                            <el-input-number v-model="form.chapterNo" :min="1" :max="999999" controls-position="right" />
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-form-item label="文章内容" prop="content">
                    <el-input v-model="form.content" type="textarea" :rows="16" placeholder="可直接粘贴 HTML 或普通文字" />
                </el-form-item>
                <el-form-item label="HTML处理">
                    <div class="html-tools">
                        <el-switch v-model="form.convertHtml" active-text="保存时转换为纯文本" />
                        <el-button v-if="form.convertHtml" link type="primary" @click="previewConvert">立即转换预览</el-button>
                        <span class="hint">会去除标签，保留段落换行与正文内容</span>
                    </div>
                </el-form-item>
                <el-form-item label="保存设置">
                    <el-switch v-model="dialog.closeAfterSave" active-text="保存成功后关闭弹窗" />
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="chapter-dialog-footer">
                    <div v-if="form.id" class="chapter-switcher">
                        <el-button
                            :icon="ArrowLeft"
                            :disabled="!dialog.previous"
                            :loading="dialog.navigating"
                            @click="navigateEdit(dialog.previous)"
                        >上一章</el-button>
                        <el-button
                            :disabled="!dialog.next"
                            :loading="dialog.navigating"
                            @click="navigateEdit(dialog.next)"
                        >
                            下一章<el-icon class="el-icon--right"><ArrowRight /></el-icon>
                        </el-button>
                    </div>
                    <div class="dialog-actions">
                        <el-button @click="dialog.visible = false">取消</el-button>
                        <el-button
                            type="primary"
                            :disabled="savingNext"
                            :loading="saving"
                            @click="submitForm()"
                        >保存</el-button>
                        <el-button
                            v-if="form.id"
                            type="primary"
                            plain
                            :disabled="!dialog.next || saving"
                            :loading="savingNext"
                            @click="submitForm('next')"
                        >保存&下一章</el-button>
                    </div>
                </div>
            </template>
        </el-dialog>

        <el-dialog v-model="batchDialog.visible" title="批量新增章节" width="900px" top="1vh" append-to-body>
            <div class="batch-toolbar">
                <span>共 {{ batchDialog.items.length }} 个章节，最多一次新增 100 个</span>
                <span class="hint">点击标签栏右侧“+”新增一章，关闭标签可移除该章</span>
            </div>
            <div class="quick-generate">
                <div class="quick-generate-title">
                    <span>快捷生成章节</span>
                    <span class="hint">每行一个章节名，将生成连续序号的空内容章节</span>
                </div>
                <div class="quick-generate-body">
                    <el-input
                        v-model="batchDialog.quickTitles"
                        type="textarea"
                        :rows="4"
                        resize="none"
                        placeholder="仲夏夜之梦&#10;同学会&#10;野百合也有春天&#10;吾爱吾师"
                    />
                    <el-button type="primary" plain @click="quickGenerateChapters">生成章节</el-button>
                </div>
            </div>
            <el-tabs
                v-model="batchDialog.activeKey"
                type="card"
                editable
                class="batch-tabs"
                @tab-add="appendBatchItem"
                @tab-remove="removeBatchItem"
            >
                <el-tab-pane
                    v-for="(item, index) in batchDialog.items"
                    :key="item.clientKey"
                    :name="item.clientKey"
                >
                    <template #label>
                        <span class="batch-tab-label">
                            {{ item.chapterName?.trim() || `第 ${item.chapterNo || index + 1} 章` }}
                        </span>
                    </template>
                    <div class="batch-tab-content">
                        <el-form label-width="82px">
                            <el-row :gutter="16">
                                <el-col :xs="24" :sm="7">
                                    <el-form-item label="章节序号" required>
                                        <el-input-number
                                            v-model="item.chapterNo"
                                            :min="1"
                                            :max="999999"
                                            controls-position="right"
                                            class="batch-no"
                                        />
                                    </el-form-item>
                                </el-col>
                                <el-col :xs="24" :sm="17">
                                    <el-form-item label="章节名称" required>
                                        <el-input
                                            v-model="item.chapterName"
                                            maxlength="255"
                                            placeholder="请输入章节名"
                                        />
                                    </el-form-item>
                                </el-col>
                            </el-row>
                            <el-form-item label="文章内容">
                                <el-input
                                    v-model="item.content"
                                    type="textarea"
                                    :rows="14"
                                    placeholder="可直接粘贴 HTML 或普通文字"
                                />
                            </el-form-item>
                            <el-form-item label="HTML处理">
                                <div class="batch-html-tools">
                                    <el-switch v-model="item.convertHtml" active-text="HTML 转纯文本" />
                                    <el-button v-if="item.convertHtml" link type="primary" @click="previewBatchConvert(item)">
                                        转换预览
                                    </el-button>
                                </div>
                            </el-form-item>
                        </el-form>
                    </div>
                </el-tab-pane>
            </el-tabs>
            <template #footer>
                <el-button @click="batchDialog.visible = false">取消</el-button>
                <el-button type="primary" :loading="batchSaving" @click="submitBatch">批量保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { computed, getCurrentInstance, reactive, ref } from 'vue'
import { ArrowLeft, ArrowRight, Delete, Edit, Plus, Refresh, Search, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { addChapter, addChapters, deleteChapters, getBook, getReaderData, listChapters, updateChapter } from '@/api/px/app/book'

const { proxy } = getCurrentInstance()
const bookId = computed(() => Number(proxy.$route.query.bookId))
const loading = ref(false)
const saving = ref(false)
const savingNext = ref(false)
const batchSaving = ref(false)
const total = ref(0)
const chapterList = ref([])
const chapterForm = ref()
const book = reactive({ title: '', lastReadChapterId: undefined })
const HTML_CONVERT_CACHE_KEY = 'myBookChapterConvertHtml'
const CLOSE_AFTER_SAVE_CACHE_KEY = 'myBookChapterCloseAfterSave'
const getCachedConvertHtml = () => {
    const cached = localStorage.getItem(HTML_CONVERT_CACHE_KEY)
    return cached == null ? true : cached === 'true'
}
const getCachedCloseAfterSave = () => {
    const cached = localStorage.getItem(CLOSE_AFTER_SAVE_CACHE_KEY)
    return cached == null ? true : cached === 'true'
}
const queryParams = reactive({ pageNum: 1, pageSize: 20, bookId: bookId.value, chapterName: '' })
const dialog = reactive({
    visible: false,
    title: '',
    navigating: false,
    closeAfterSave: getCachedCloseAfterSave(),
    previous: null,
    next: null,
    original: ''
})
const batchDialog = reactive({ visible: false, activeKey: '', quickTitles: '', items: [] })
const form = reactive({
    id: undefined,
    bookId: bookId.value,
    chapterName: '',
    chapterNo: 1,
    content: '',
    convertHtml: getCachedConvertHtml()
})
let batchKey = 0
const rules = {
    chapterName: [{ required: true, message: '章节名不能为空', trigger: 'blur' }],
    chapterNo: [{ required: true, message: '章节序号不能为空', trigger: 'change' }]
}

function htmlToPlainText(html) {
    if (!html) return ''
    const parser = new DOMParser()
    let source = html
    // 兼容从接口或网页复制得到的 &lt;p&gt;...&lt;/p&gt; 形式。
    if (!/<[a-z][^>]*>/i.test(source) && /&lt;\/?[a-z][^&]*&gt;/i.test(source)) {
        source = parser.parseFromString(source, 'text/html').body.textContent || source
    }
    const doc = parser.parseFromString(source, 'text/html')
    const blocks = new Set(['p', 'div', 'li', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'blockquote', 'tr', 'section', 'article'])
    const parts = []
    const appendLineBreak = () => {
        if (parts.length && parts[parts.length - 1] !== '\n') parts.push('\n')
    }
    const visit = node => {
        if (node.nodeType === 3) {
            parts.push(node.nodeValue || '')
            return
        }
        const tag = node.nodeName.toLowerCase()
        if (tag === 'script' || tag === 'style') return
        if (tag === 'br') {
            appendLineBreak()
            return
        }
        Array.from(node.childNodes || []).forEach(visit)
        if (blocks.has(tag)) appendLineBreak()
    }
    Array.from(doc.body.childNodes).forEach(visit)
    return parts.join('')
        .replace(/\u00a0/g, ' ')
        .replace(/[\t\f ]+/g, ' ')
        .replace(/[ \t]+\n/g, '\n')
        .replace(/\n[ \t]+/g, '\n')
        .replace(/\n{3,}/g, '\n\n')
        .trim()
}

function getList() {
    if (!bookId.value) return
    loading.value = true
    queryParams.bookId = bookId.value
    Promise.all([getBook(bookId.value), listChapters(queryParams)]).then(([bookRes, listRes]) => {
        Object.assign(book, bookRes.data || {})
        chapterList.value = listRes.rows || []
        total.value = listRes.total || 0
    }).finally(() => { loading.value = false })
}

function resetForm() {
    const lastChapter = chapterList.value[chapterList.value.length - 1]
    Object.assign(form, {
        id: undefined,
        bookId: bookId.value,
        chapterName: '',
        chapterNo: (lastChapter?.chapterNo || 0) + 1,
        content: '',
        convertHtml: getCachedConvertHtml()
    })
    chapterForm.value?.clearValidate()
}

function handleQuery() {
    queryParams.pageNum = 1
    getList()
}

function resetQuery() {
    queryParams.chapterName = ''
    handleQuery()
}

function handleAdd() {
    resetForm()
    dialog.title = '新增章节'
    dialog.visible = true
}

function getNextChapterNo() {
    return Math.max(0, ...chapterList.value.map(item => Number(item.chapterNo) || 0)) + 1
}

function createBatchItem(chapterNo) {
    batchKey += 1
    return {
        clientKey: `chapter-${batchKey}`,
        bookId: bookId.value,
        chapterName: '',
        chapterNo,
        content: '',
        convertHtml: true
    }
}

function handleBatchAdd() {
    const startNo = getNextChapterNo()
    batchDialog.items = [createBatchItem(startNo), createBatchItem(startNo + 1)]
    batchDialog.activeKey = batchDialog.items[0].clientKey
    batchDialog.quickTitles = ''
    batchDialog.visible = true
}

function quickGenerateChapters() {
    const titles = batchDialog.quickTitles
        .split(/\r?\n/)
        .map(title => title.trim())
        .filter(Boolean)
    if (!titles.length) {
        ElMessage.warning('请至少输入一个章节名')
        return
    }
    const hasEditedChapter = batchDialog.items.some(item => item.chapterName.trim() || item.content.trim())
    const remaining = hasEditedChapter ? 100 - batchDialog.items.length : 100
    if (remaining <= 0) {
        ElMessage.warning('每次最多批量新增 100 个章节')
        return
    }
    const acceptedTitles = titles.slice(0, remaining)
    const last = hasEditedChapter ? batchDialog.items[batchDialog.items.length - 1] : null
    const startNo = last ? (Number(last.chapterNo) || 0) + 1 : getNextChapterNo()
    const generated = acceptedTitles.map((title, index) => ({
        ...createBatchItem(startNo + index),
        chapterName: title
    }))
    if (hasEditedChapter) {
        batchDialog.items.push(...generated)
    } else {
        batchDialog.items = generated
    }
    batchDialog.activeKey = generated[0].clientKey
    batchDialog.quickTitles = ''
    ElMessage.success(`已生成 ${generated.length} 个空内容章节`)
    if (acceptedTitles.length < titles.length) {
        ElMessage.warning(`已达到 100 章上限，另有 ${titles.length - acceptedTitles.length} 行未生成`)
    }
}

function appendBatchItem() {
    if (batchDialog.items.length >= 100) {
        ElMessage.warning('每次最多批量新增 100 个章节')
        return
    }
    const last = batchDialog.items[batchDialog.items.length - 1]
    const item = createBatchItem((Number(last?.chapterNo) || 0) + 1)
    batchDialog.items.push(item)
    batchDialog.activeKey = item.clientKey
}

function removeBatchItem(targetName) {
    if (batchDialog.items.length === 1) {
        ElMessage.warning('至少保留一个章节')
        return
    }
    const index = batchDialog.items.findIndex(item => item.clientKey === targetName)
    if (index < 0) return
    batchDialog.items.splice(index, 1)
    if (batchDialog.activeKey === targetName) {
        const next = batchDialog.items[Math.min(index, batchDialog.items.length - 1)]
        batchDialog.activeKey = next.clientKey
    }
}

function previewBatchConvert(item) {
    item.content = htmlToPlainText(item.content)
    ElMessage.success('该章节已转换为纯文本')
}

function submitBatch() {
    const invalidIndex = batchDialog.items.findIndex(item => !item.chapterName || !item.chapterName.trim())
    if (invalidIndex >= 0) {
        ElMessage.warning(`请填写第 ${invalidIndex + 1} 个章节的章节名`)
        return
    }
    const chapterNos = batchDialog.items.map(item => Number(item.chapterNo))
    if (new Set(chapterNos).size !== chapterNos.length) {
        ElMessage.warning('批量章节序号不能重复')
        return
    }
    const payload = batchDialog.items.map(item => ({
        bookId: bookId.value,
        chapterName: item.chapterName.trim(),
        chapterNo: item.chapterNo,
        content: item.convertHtml ? htmlToPlainText(item.content) : item.content,
        convertHtml: item.convertHtml
    }))
    batchSaving.value = true
    addChapters(payload).then(() => {
        batchDialog.visible = false
        getList()
        ElMessage.success(`成功新增 ${payload.length} 个章节`)
    }).finally(() => { batchSaving.value = false })
}

function handleEdit(row) {
    loadEditChapter(row.id)
}

function formSnapshot() {
    return JSON.stringify({
        chapterName: form.chapterName,
        chapterNo: form.chapterNo,
        content: form.content,
        convertHtml: form.convertHtml
    })
}

function loadEditChapter(chapterId) {
    dialog.navigating = true
    return getReaderData(chapterId).then(res => {
        const data = res.data || {}
        resetForm()
        Object.assign(form, data.chapter || {}, { convertHtml: getCachedConvertHtml() })
        dialog.previous = data.previous || null
        dialog.next = data.next || null
        dialog.title = '编辑章节'
        dialog.visible = true
        dialog.original = formSnapshot()
    }).finally(() => { dialog.navigating = false })
}

function navigateEdit(target) {
    if (!target?.id || dialog.navigating) return
    const switchChapter = () => loadEditChapter(target.id)
    if (formSnapshot() === dialog.original) {
        switchChapter()
        return
    }
    proxy.$confirm('当前章节有未保存的修改，切换后将丢失这些修改，是否继续？', '切换章节', {
        confirmButtonText: '继续切换',
        cancelButtonText: '留在本章',
        type: 'warning'
    }).then(switchChapter).catch(() => {})
}

function previewConvert() {
    form.content = htmlToPlainText(form.content)
    ElMessage.success('已转换为纯文本，可检查后再保存')
}

function submitForm(mode = 'default') {
    if (saving.value || savingNext.value) return
    chapterForm.value.validate(valid => {
        if (!valid) return
        if (form.convertHtml) form.content = htmlToPlainText(form.content)
        if (mode === 'next') {
            savingNext.value = true
        } else {
            saving.value = true
        }
        const action = form.id ? updateChapter(form) : addChapter(form)
        action.then(res => {
            const savedId = form.id || res.data
            localStorage.setItem(HTML_CONVERT_CACHE_KEY, String(form.convertHtml))
            localStorage.setItem(CLOSE_AFTER_SAVE_CACHE_KEY, String(dialog.closeAfterSave))
            getList()
            if (mode === 'next') {
                return getReaderData(savedId).then(readerRes => {
                    const nextChapter = readerRes.data?.next
                    if (nextChapter?.id) {
                        ElMessage.success('保存成功，已打开下一章')
                        return loadEditChapter(nextChapter.id)
                    } else {
                        ElMessage.success('保存成功，当前已是最后一章')
                        return loadEditChapter(savedId)
                    }
                })
            }
            ElMessage.success('保存成功')
            if (dialog.closeAfterSave) {
                dialog.visible = false
            } else if (savedId) {
                loadEditChapter(savedId)
            } else {
                dialog.original = formSnapshot()
            }
        }).finally(() => {
            saving.value = false
            savingNext.value = false
        })
    })
}

function handleDelete(row) {
    proxy.$confirm(`确认删除章节“${row.chapterName}”吗？`, '删除提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => deleteChapters(row.id)).then(() => {
        getList()
        ElMessage.success('删除成功')
    }).catch(() => {})
}

function openReader(row) {
    proxy.$router.push({ name: 'MyBookReader', params: { chapterId: row.id } })
}

function backToBooks() {
    proxy.$router.push('/myapp/book')
}

if (!bookId.value) {
    ElMessage.warning('缺少书籍信息')
    backToBooks()
} else {
    getList()
}
</script>

<style lang="scss" scoped>
.chapter-page {
    .page-title { font-size: 18px; font-weight: 600; }
    .chapter-search { margin-top: 22px; padding: 16px 18px 0; background: var(--pnkx-surface, #fff); border-radius: 10px; }
    .chapter-table { margin-top: 14px; }
    .html-tools { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
    .hint { color: var(--pnkx-text-secondary, #909399); font-size: 12px; }
    .reading-progress-tag { margin-left: 8px; }
    .chapter-dialog-footer { width: 100%; display: flex; align-items: center; justify-content: space-between; column-gap: 48px; }
    .chapter-switcher, .dialog-actions { display: flex; align-items: center; }
    .dialog-actions { margin-left: auto; }
    .batch-toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; color: var(--pnkx-text-secondary, #606266); }
    .quick-generate { margin-bottom: 16px; padding: 12px; border: 1px dashed var(--el-border-color); border-radius: 8px; background: var(--el-fill-color-lighter); }
    .quick-generate-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; font-weight: 600; }
    .quick-generate-body { display: grid; grid-template-columns: 1fr auto; gap: 10px; align-items: end; }
    .batch-tabs { max-height: 65vh; overflow-y: auto; }
    .batch-tab-label { display: inline-block; max-width: 150px; overflow: hidden; text-overflow: ellipsis; vertical-align: bottom; }
    .batch-tab-content { padding: 14px 8px 0; }
    .batch-no { width: 100%; }
    .batch-html-tools { display: flex; align-items: center; gap: 10px; margin-top: 10px; }
}

@media (max-width: 768px) {
    .chapter-page .chapter-dialog-footer { align-items: stretch; flex-direction: column; }
    .chapter-page .chapter-switcher, .chapter-page .dialog-actions { justify-content: space-between; }
    .chapter-page .quick-generate-body { grid-template-columns: 1fr; }
    .chapter-page .quick-generate-title { align-items: flex-start; flex-direction: column; gap: 4px; }
}
</style>
