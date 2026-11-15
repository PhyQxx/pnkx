<template>
    <div class="note-detail">
        <div class="m-form">
            <!-- 头部信息 -->
            <div class="m-card note-hero">
                <div class="note-hero__eyebrow">{{ noteForm.id ? 'Edit note' : 'New note' }}</div>
                <div class="note-hero__title">{{ noteForm.id ? '整理这篇记录' : '写下一点东西' }}</div>
                <div class="note-hero__subtitle">{{ selectedFolderName }} · {{ contentCount }} 字</div>
            </div>

            <van-cell-group :border="false" class="m-cell-card">
                <van-field
                    v-model="noteForm.title"
                    label="标题"
                    placeholder="给这篇笔记起个名字"
                    clearable
                    :maxlength="100"
                />
                <van-cell
                    title="位置"
                    is-link
                    :value="selectedFolderName"
                    @click="openFolderPicker"
                />
            </van-cell-group>

            <div class="m-card note-editor">
                <div class="note-editor__label">正文</div>
                <van-field
                    v-model="noteForm.content"
                    type="textarea"
                    placeholder="开始记录..."
                    :maxlength="10000"
                    rows="10"
                    autosize
                    show-word-limit
                    class="note-editor__field"
                />
            </div>

            <!-- 元信息 -->
            <van-cell-group v-if="noteForm.id" :border="false" class="m-cell-card">
                <van-cell title="创建时间" :value="formatTime(noteForm.createTime)" />
                <van-cell title="更新时间" :value="formatTime(noteForm.updateTime)" />
            </van-cell-group>

            <!-- 删除 -->
            <div v-if="noteForm.id" class="note-delete">
                <van-button block plain type="danger" @click="handleDelete">删除笔记</van-button>
            </div>
        </div>

        <!-- 底部操作条 -->
        <div class="m-sticky-actions">
            <van-button block plain @click="handleCancel">取消</van-button>
            <van-button block type="primary" :loading="saving" @click="handleSave">保存</van-button>
        </div>

        <!-- 文件夹选择弹窗 -->
        <van-popup v-model:show="folderPickerShow" position="bottom" round class="note-picker">
            <div class="note-picker__header">
                <span class="note-picker__cancel" @click="folderPickerShow = false">取消</span>
                <span class="note-picker__title">选择文件夹</span>
                <span class="note-picker__confirm" @click="confirmFolder">确定</span>
            </div>
            <div class="note-picker__list">
                <div
                    class="note-picker__item"
                    :class="{ 'note-picker__item--active': tempFolderId === 0 }"
                    @click="tempFolderId = 0"
                >
                    <van-icon name="folder-o" size="18" color="var(--pnkx-primary)" />
                    <span class="note-picker__name">根目录</span>
                    <van-icon v-if="tempFolderId === 0" name="success" size="16" color="var(--pnkx-primary)" />
                </div>
                <div
                    v-for="folder in flatFolderList"
                    :key="folder.id"
                    class="note-picker__item"
                    :class="{ 'note-picker__item--active': tempFolderId === folder.id }"
                    @click="tempFolderId = folder.id"
                >
                    <van-icon
                        :name="folder.password ? 'lock' : 'folder-o'"
                        size="18"
                        :color="folder.password ? 'var(--pnkx-warning)' : 'var(--pnkx-primary)'"
                    />
                    <span class="note-picker__name" :style="{ paddingLeft: (folder.level * 14 + 4) + 'px' }">{{ folder.name }}</span>
                    <van-icon v-if="tempFolderId === folder.id" name="success" size="16" color="var(--pnkx-primary)" />
                </div>
            </div>
        </van-popup>
    </div>
</template>

<script>
import { getNote, addNote, updateNote, delNote, treeList } from '@/api/px/life/note'
import { showToast, mSuccess, mError, mConfirm, mLoading, closeToast } from '@/mobile/utils/ui'

export default {
    name: 'MNoteDetail',
    data() {
        return {
            noteId: null,
            noteForm: {
                id: null,
                title: '',
                content: '',
                richText: '',
                folder: 0,
                order: 0
            },
            originalData: null,
            folderTree: [],
            tempFolderId: 0,
            folderPickerShow: false,
            saving: false
        }
    },
    computed: {
        contentCount() {
            return (this.noteForm.content || '').length
        },
        selectedFolderName() {
            if (!this.noteForm.folder) return '根目录'
            const folder = this.findFolderById(this.folderTree, this.noteForm.folder)
            return folder ? folder.name : '根目录'
        },
        flatFolderList() {
            const list = []
            this.flattenFolderList(this.folderTree, 0, list)
            return list
        }
    },
    mounted() {
        const query = this.$route.query
        if (query.id) {
            this.noteId = query.id
            this.loadNoteDetail()
        } else if (query.folder) {
            this.noteForm.folder = parseInt(query.folder) || 0
        }
        this.loadFolderTree()
    },
    methods: {
        async loadNoteDetail() {
            mLoading('加载中')
            try {
                const res = await getNote(this.noteId)
                this.noteForm = res.data || {}
                this.originalData = JSON.parse(JSON.stringify(this.noteForm))
            } catch (error) {
                console.error('加载笔记失败:', error)
                mError('加载失败')
            } finally {
                closeToast()
            }
        },
        async loadFolderTree() {
            try {
                const res = await treeList({})
                this.folderTree = res.data || []
            } catch (error) {
                console.error('加载文件夹失败:', error)
            }
        },
        findFolderById(folders, id) {
            for (const folder of folders) {
                if (folder.id === id) return folder
                if (folder.children && folder.children.length > 0) {
                    const found = this.findFolderById(folder.children, id)
                    if (found) return found
                }
            }
            return null
        },
        flattenFolderList(folders, level, list = []) {
            folders.forEach(folder => {
                list.push({ ...folder, level })
                if (folder.children && folder.children.length > 0) {
                    this.flattenFolderList(folder.children, level + 1, list)
                }
            })
            return list
        },
        openFolderPicker() {
            this.tempFolderId = this.noteForm.folder || 0
            this.folderPickerShow = true
        },
        confirmFolder() {
            this.noteForm.folder = this.tempFolderId
            this.folderPickerShow = false
        },
        formatTime(time) {
            return this.$parseTime(time, '{y}-{m}-{d} {h}:{i}')
        },
        hasChanges() {
            if (!this.originalData) {
                return !!(this.noteForm.title || this.noteForm.content)
            }
            return (
                this.noteForm.title !== this.originalData.title ||
                this.noteForm.content !== this.originalData.content ||
                this.noteForm.folder !== this.originalData.folder
            )
        },
        async handleCancel() {
            if (this.hasChanges()) {
                try {
                    await mConfirm('提示', '有未保存的修改，确定要离开吗？')
                } catch (e) {
                    return
                }
            }
            this.$router.back()
        },
        async handleSave() {
            if (!this.noteForm.title || !this.noteForm.title.trim()) {
                showToast('请输入笔记标题')
                return
            }
            this.saving = true
            try {
                const data = {
                    ...this.noteForm,
                    title: this.noteForm.title.trim(),
                    folder: this.noteForm.folder || 0
                }
                if (this.noteForm.id) {
                    await updateNote(data)
                    mSuccess('修改成功')
                } else {
                    await addNote(data)
                    mSuccess('创建成功')
                }
                setTimeout(() => {
                    this.$router.back()
                }, 500)
            } catch (error) {
                console.error('保存失败:', error)
                mError('保存失败')
            } finally {
                this.saving = false
            }
        },
        async handleDelete() {
            try {
                await mConfirm('删除确认', '确定要删除这篇笔记吗？删除后无法恢复。')
            } catch (e) {
                return
            }
            mLoading('删除中')
            try {
                await delNote(this.noteForm.id)
                closeToast()
                mSuccess('删除成功')
                setTimeout(() => {
                    this.$router.back()
                }, 500)
            } catch (error) {
                console.error('删除失败:', error)
                closeToast()
                mError('删除失败')
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.note-detail {
    min-height: calc(100vh - 46px);
    background: var(--pnkx-bg);
}

.note-hero {
    &__eyebrow {
        font-size: 11px;
        font-weight: 600;
        text-transform: uppercase;
        color: var(--pnkx-primary);
    }

    &__title {
        margin-top: 3px;
        font-size: 19px;
        font-weight: 700;
        color: var(--pnkx-text);
        line-height: 1.25;
    }

    &__subtitle {
        margin-top: 5px;
        font-size: 12px;
        color: var(--pnkx-text-secondary);
    }
}

.note-editor {
    &__label {
        font-size: 14px;
        font-weight: 600;
        color: var(--pnkx-text);
        margin-bottom: 8px;
    }

    &__field {
        padding: 0;

        :deep(.van-field__control) {
            min-height: 220px;
            font-size: 14px;
            line-height: 1.75;
            color: var(--pnkx-text);
        }
    }
}

.note-delete {
    margin-top: 4px;
}

.note-picker {
    &__header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 14px 16px;
        border-bottom: 1px solid var(--pnkx-border-soft);
    }

    &__title {
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__cancel {
        font-size: 13px;
        color: var(--pnkx-text-muted);
        padding: 4px 8px;
    }

    &__confirm {
        font-size: 13px;
        color: var(--pnkx-primary);
        font-weight: 600;
        padding: 4px 8px;
    }

    &__list {
        max-height: 55vh;
        overflow-y: auto;
        padding: 6px 0 calc(12px + env(safe-area-inset-bottom));
    }

    &__item {
        display: flex;
        align-items: center;
        padding: 13px 16px;

        &:active {
            background: var(--pnkx-surface-muted);
        }

        &--active {
            background: var(--pnkx-primary-soft);
        }

        .note-picker__name {
            flex: 1;
            min-width: 0;
            margin-left: 10px;
            font-size: 14px;
            color: var(--pnkx-text);
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
    }
}
</style>
