<template>
    <div class="folder-page">
        <!-- 头部 -->
        <div class="folder-header">
            <div class="folder-header__copy">
                <div class="folder-header__eyebrow">Folders</div>
                <div class="folder-header__title">文件夹管理</div>
                <div class="folder-header__subtitle">{{ folderTree.length }} 个根文件夹 · 按层级整理笔记</div>
            </div>
        </div>

        <!-- 文件夹树 -->
        <div v-if="folderTree.length > 0" class="m-cell-card folder-tree">
            <FolderTreeItem
                v-for="folder in folderTree"
                :key="folder.id"
                :folder="folder"
                :level="0"
                @edit="editFolder"
                @delete="deleteFolder"
                @select="handleFolderClick"
            />
        </div>

        <!-- 空状态 -->
        <div v-else-if="!loading" class="m-empty folder-empty">
            <van-icon name="folder-o" size="42" color="var(--pnkx-primary)" />
            <span>还没有文件夹</span>
            <span class="m-text-muted">按主题归档笔记，查找会轻松很多</span>
            <van-button size="small" round type="primary" class="folder-empty__btn" @click="openCreateDialog">新建文件夹</van-button>
        </div>

        <!-- 悬浮新建 -->
        <button class="m-fab m-fab--no-tabbar" @click="openCreateDialog">
            <van-icon name="plus" size="22" />
        </button>

        <!-- 新建/编辑弹窗 -->
        <van-popup v-model:show="folderDialogShow" position="bottom" round class="folder-dialog">
            <div class="folder-dialog__title">{{ editingFolder ? '编辑文件夹' : '新建文件夹' }}</div>
            <div class="folder-dialog__body">
                <div class="folder-dialog__label">名称</div>
                <van-field
                    v-model="folderForm.name"
                    placeholder="请输入文件夹名称"
                    clearable
                    :maxlength="50"
                    class="folder-dialog__field"
                />
                <div class="folder-dialog__label">访问密码（可选）</div>
                <van-field
                    v-model="folderForm.password"
                    type="password"
                    placeholder="设置密码保护此文件夹"
                    clearable
                    :maxlength="20"
                    class="folder-dialog__field"
                />
                <div class="folder-dialog__hint">设置密码后，访问此文件夹需要输入密码</div>
                <div class="folder-dialog__label">上级位置</div>
                <div class="folder-dialog__picker" @click="openParentPicker">
                    <span>{{ parentFolderName }}</span>
                    <van-icon name="arrow" size="14" color="var(--pnkx-text-muted)" />
                </div>
            </div>
            <div class="folder-dialog__actions">
                <van-button block plain @click="closeFolderDialog">取消</van-button>
                <van-button block type="primary" :loading="saving" @click="saveFolder">确定</van-button>
            </div>
        </van-popup>

        <!-- 父级文件夹选择弹窗 -->
        <van-popup v-model:show="parentPickerShow" position="bottom" round class="folder-picker">
            <div class="folder-picker__header">
                <span class="folder-picker__cancel" @click="parentPickerShow = false">取消</span>
                <span class="folder-picker__title">选择父级文件夹</span>
                <span class="folder-picker__confirm" @click="confirmParent">确定</span>
            </div>
            <div class="folder-picker__list">
                <div
                    class="folder-picker__item"
                    :class="{ 'folder-picker__item--active': tempParentId === 0 }"
                    @click="tempParentId = 0"
                >
                    <van-icon name="folder-o" size="18" color="var(--pnkx-primary)" />
                    <span class="folder-picker__name">根目录</span>
                    <van-icon v-if="tempParentId === 0" name="success" size="16" color="var(--pnkx-primary)" />
                </div>
                <div
                    v-for="folder in flatFolderList"
                    :key="folder.id"
                    class="folder-picker__item"
                    :class="{
                        'folder-picker__item--active': tempParentId === folder.id,
                        'folder-picker__item--disabled': isDescendantOrSelf(folder)
                    }"
                    @click="selectParent(folder)"
                >
                    <van-icon
                        :name="folder.password ? 'lock' : 'folder-o'"
                        size="18"
                        :color="folder.password ? 'var(--pnkx-warning)' : 'var(--pnkx-primary)'"
                    />
                    <span class="folder-picker__name" :style="{ paddingLeft: (folder.level * 14 + 4) + 'px' }">{{ folder.name }}</span>
                    <van-icon v-if="tempParentId === folder.id" name="success" size="16" color="var(--pnkx-primary)" />
                </div>
            </div>
        </van-popup>
    </div>
</template>

<script>
import { treeList, addFolder, updateFolder, delFolder } from '@/api/px/life/note'
import { showToast, mSuccess, mError, mConfirm, mLoading, closeToast } from '@/mobile/utils/ui'
import FolderTreeItem from './components/FolderTreeItem.vue'

export default {
    name: 'MNoteFolder',
    components: {
        FolderTreeItem
    },
    data() {
        return {
            loading: false,
            saving: false,
            folderTree: [],
            flatFolderList: [],
            editingFolder: null,
            folderForm: {
                name: '',
                password: '',
                parentId: 0
            },
            folderDialogShow: false,
            tempParentId: 0,
            parentPickerShow: false
        }
    },
    computed: {
        parentFolderName() {
            if (this.folderForm.parentId === 0) return '根目录'
            const folder = this.flatFolderList.find(f => f.id === this.folderForm.parentId)
            return folder ? folder.name : '根目录'
        }
    },
    mounted() {
        this.loadFolderTree()
    },
    methods: {
        async loadFolderTree() {
            this.loading = true
            try {
                const res = await treeList({})
                this.folderTree = res.data || []
                this.flatFolderList = []
                this.flattenFolderList(this.folderTree, 0)
            } catch (error) {
                console.error('加载文件夹失败:', error)
                mError('加载失败')
            } finally {
                this.loading = false
            }
        },
        flattenFolderList(folders, level) {
            folders.forEach(folder => {
                this.flatFolderList.push({ ...folder, level })
                if (folder.children && folder.children.length > 0) {
                    this.flattenFolderList(folder.children, level + 1)
                }
            })
        },
        openCreateDialog() {
            this.editingFolder = null
            this.folderForm = { name: '', password: '', parentId: 0 }
            this.tempParentId = 0
            this.folderDialogShow = true
        },
        editFolder(folder) {
            this.editingFolder = folder
            this.folderForm = {
                name: folder.name || '',
                password: folder.password || '',
                parentId: folder.parentId || 0
            }
            this.tempParentId = this.folderForm.parentId
            this.folderDialogShow = true
        },
        closeFolderDialog() {
            this.folderDialogShow = false
            this.editingFolder = null
        },
        async saveFolder() {
            if (!this.folderForm.name || !this.folderForm.name.trim()) {
                showToast('请输入文件夹名称')
                return
            }
            this.saving = true
            mLoading('保存中')
            try {
                const data = {
                    name: this.folderForm.name.trim(),
                    password: this.folderForm.password || '',
                    parentId: this.folderForm.parentId || 0
                }
                if (this.editingFolder) {
                    data.id = this.editingFolder.id
                    await updateFolder(data)
                    closeToast()
                    mSuccess('修改成功')
                } else {
                    await addFolder(data)
                    closeToast()
                    mSuccess('创建成功')
                }
                this.folderDialogShow = false
                this.editingFolder = null
                this.loadFolderTree()
            } catch (error) {
                console.error('保存失败:', error)
                closeToast()
                mError('保存失败')
            } finally {
                this.saving = false
            }
        },
        async deleteFolder(folder) {
            try {
                await mConfirm('删除确认', `确定要删除文件夹「${folder.name}」吗？该文件夹下的所有笔记将一并删除。`)
            } catch (e) {
                return
            }
            mLoading('删除中')
            try {
                await delFolder(folder.id)
                closeToast()
                mSuccess('删除成功')
                this.loadFolderTree()
            } catch (error) {
                console.error('删除失败:', error)
                closeToast()
                mError('删除失败')
            }
        },
        openParentPicker() {
            this.tempParentId = this.folderForm.parentId || 0
            this.parentPickerShow = true
        },
        /** 是否为编辑中文件夹自身或其子孙（不可作为父级） */
        isDescendantOrSelf(folder) {
            if (!this.editingFolder) return false
            if (folder.id === this.editingFolder.id) return true
            const stack = [this.editingFolder]
            while (stack.length > 0) {
                const node = stack.pop()
                for (const child of node.children || []) {
                    if (child.id === folder.id) return true
                    stack.push(child)
                }
            }
            return false
        },
        selectParent(folder) {
            if (this.isDescendantOrSelf(folder)) {
                showToast('不能选择自己或子文件夹作为父级')
                return
            }
            this.tempParentId = folder.id
        },
        confirmParent() {
            this.folderForm.parentId = this.tempParentId
            this.parentPickerShow = false
        },
        handleFolderClick(folder) {
            const path = `/m/note?folderId=${folder.id}`
            if (this.$router.resolve(path).matched.length > 0) {
                this.$router.push(path)
            } else {
                showToast('功能即将上线')
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.folder-page {
    min-height: calc(100vh - 46px);
    padding: 12px 12px 90px;
    box-sizing: border-box;
    background: var(--pnkx-bg);
}

.folder-header {
    margin-bottom: 12px;

    &__eyebrow {
        font-size: 11px;
        font-weight: 600;
        text-transform: uppercase;
        color: var(--pnkx-primary);
    }

    &__title {
        margin-top: 3px;
        font-size: 20px;
        font-weight: 700;
        color: var(--pnkx-text);
        line-height: 1.2;
    }

    &__subtitle {
        margin-top: 4px;
        font-size: 12px;
        color: var(--pnkx-text-secondary);
    }
}

.folder-tree {
    :deep(.tree-node:last-child .tree-node__row) {
        border-bottom: none;
    }
}

.folder-empty {
    padding: 64px 0;

    &__btn {
        margin-top: 8px;
    }
}

.folder-dialog {
    padding: 18px 16px calc(16px + env(safe-area-inset-bottom));

    &__title {
        font-size: 16px;
        font-weight: 600;
        color: var(--pnkx-text);
        text-align: center;
        margin-bottom: 14px;
    }

    &__label {
        font-size: 13px;
        font-weight: 600;
        color: var(--pnkx-text-secondary);
        margin: 10px 0 6px;

        &:first-child {
            margin-top: 0;
        }
    }

    &__field {
        border: 1px solid var(--pnkx-border-soft);
        border-radius: 8px;
    }

    &__hint {
        margin-top: 5px;
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }

    &__picker {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 11px 12px;
        border: 1px solid var(--pnkx-border-soft);
        border-radius: 8px;
        background: var(--pnkx-surface-muted);
        font-size: 14px;
        color: var(--pnkx-text);
    }

    &__actions {
        display: flex;
        gap: 10px;
        margin-top: 16px;

        .van-button {
            flex: 1;
        }
    }
}

.folder-picker {
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

        &--disabled {
            opacity: 0.45;
        }

        .folder-picker__name {
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
