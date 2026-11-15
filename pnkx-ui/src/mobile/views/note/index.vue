<template>
    <div class="note-page">
        <!-- 头部：标题 + Obsidian/文件夹管理入口 + 统计 -->
        <div class="note-hero">
            <div class="note-hero__top">
                <div class="note-hero__copy">
                    <div class="note-hero__eyebrow">My notes</div>
                    <div class="note-hero__title">{{ currentFolderName }}</div>
                    <div class="note-hero__subtitle">{{ headerSubtitle }}</div>
                </div>
                <div class="note-hero__actions">
                    <span class="note-hero__chip" @click="openObsidian">
                        <van-icon name="notes-o" size="14" />
                        Obsidian
                    </span>
                    <span class="note-hero__chip note-hero__chip--icon" @click="openFolderManage">
                        <van-icon name="manager-o" size="16" />
                    </span>
                </div>
            </div>
            <div class="m-stat-bar">
                <div class="m-stat-bar__item">
                    <div class="m-stat-bar__label">本页笔记</div>
                    <div class="m-stat-bar__value">{{ noteList.length }}</div>
                </div>
                <div class="m-stat-bar__item">
                    <div class="m-stat-bar__label">子文件夹</div>
                    <div class="m-stat-bar__value">{{ folderList.length }}</div>
                </div>
            </div>
        </div>

        <!-- 搜索 -->
        <van-search
            v-model="searchKeyword"
            class="note-search"
            placeholder="搜索标题或内容"
            shape="round"
            clearable
            @search="handleSearch"
            @clear="handleSearch"
            @update:model-value="onSearchInput"
        />

        <!-- 面包屑 -->
        <div class="note-crumbs">
            <template v-if="folderPath.length > 0">
                <span class="note-crumbs__item" @click="navigateToFolder(0, -1)">
                    <van-icon name="wap-home-o" size="13" />
                    全部
                </span>
                <template v-for="(folder, index) in folderPath" :key="folder.id">
                    <span class="note-crumbs__sep">/</span>
                    <span class="note-crumbs__item" @click="navigateToFolder(folder.id, index)">{{ folder.name }}</span>
                </template>
            </template>
            <span v-else class="note-crumbs__placeholder">
                <van-icon name="folder-o" size="13" />
                全部笔记
            </span>
        </div>

        <!-- 内容 -->
        <van-pull-refresh v-model="refreshing" class="m-pull" @refresh="onRefresh">
            <div v-if="searchKeyword" class="note-result-bar">
                <span>“{{ searchKeyword }}”的搜索结果</span>
                <span class="note-result-bar__count">{{ noteList.length }} 条</span>
            </div>

            <!-- 子文件夹横向卡片 -->
            <template v-if="folderList.length > 0 && !searchKeyword">
                <div class="m-section-title">文件夹</div>
                <div class="note-folders">
                    <div
                        v-for="folder in folderList"
                        :key="folder.id"
                        class="note-folders__card"
                        @click="enterFolder(folder)"
                    >
                        <span class="note-folders__icon">
                            <van-icon :name="folder.password ? 'lock' : 'folder-o'" size="20" :color="folder.password ? 'var(--pnkx-warning)' : 'var(--pnkx-primary)'" />
                        </span>
                        <div class="note-folders__body">
                            <div class="note-folders__name">{{ folder.name }}</div>
                            <div class="note-folders__count">{{ folder.noteCount || 0 }} 篇笔记</div>
                        </div>
                    </div>
                </div>
            </template>

            <div v-if="noteList.length > 0 && !searchKeyword" class="m-section-title">最近笔记</div>

            <van-list
                v-model:loading="listLoading"
                :finished="finished"
                finished-text="没有更多了"
                @load="loadMore"
            >
                <NoteCard
                    v-for="note in noteList"
                    :key="note.id"
                    :note="note"
                    @click="viewNote"
                    @edit="editNote"
                    @delete="deleteNote"
                />
                <div v-if="noteList.length === 0 && folderList.length === 0 && !listLoading && !loading" class="m-empty">
                    <van-icon name="edit" size="42" color="var(--pnkx-primary)" />
                    <span>{{ searchKeyword ? '没有匹配的笔记' : '这里还没有内容' }}</span>
                    <span class="m-text-muted">{{ searchKeyword ? '换个关键词试试' : '新建一篇笔记，或先创建一个文件夹' }}</span>
                </div>
            </van-list>
        </van-pull-refresh>

        <!-- 悬浮按钮（展开菜单：新建笔记 / 新建文件夹） -->
        <div class="note-fab-wrap">
            <div v-if="fabExpanded" class="note-fab-menu">
                <div class="note-fab-menu__item" @click="handleCreateNote">
                    <span class="note-fab-menu__text">新建笔记</span>
                    <span class="note-fab-menu__btn"><van-icon name="edit" size="16" /></span>
                </div>
                <div class="note-fab-menu__item" @click="handleCreateFolder">
                    <span class="note-fab-menu__text">新建文件夹</span>
                    <span class="note-fab-menu__btn"><van-icon name="folder-o" size="16" /></span>
                </div>
            </div>
            <button class="m-fab m-fab--no-tabbar" @click="toggleFab">
                <van-icon :name="fabExpanded ? 'cross' : 'plus'" size="22" />
            </button>
        </div>

        <!-- 新建文件夹弹窗 -->
        <van-popup v-model:show="folderPopupShow" position="bottom" round class="note-popup">
            <div class="note-popup__title">新建文件夹</div>
            <div class="note-popup__body">
                <van-field
                    v-model="newFolderName"
                    placeholder="请输入文件夹名称"
                    clearable
                    :maxlength="50"
                    class="note-popup__field"
                    @keyup.enter="createFolder"
                />
            </div>
            <div class="note-popup__actions">
                <van-button block plain @click="folderPopupShow = false">取消</van-button>
                <van-button block type="primary" :loading="creating" @click="createFolder">确定</van-button>
            </div>
        </van-popup>

        <!-- 密码输入弹窗 -->
        <van-popup v-model:show="passwordPopupShow" position="center" round class="note-pwd">
            <div class="note-pwd__title">输入密码</div>
            <div class="note-pwd__hint">该文件夹需要密码才能访问</div>
            <van-field
                v-model="passwordInput"
                type="password"
                placeholder="请输入访问密码"
                class="note-pwd__field"
            />
            <div class="note-pwd__actions">
                <van-button size="small" plain @click="cancelPassword">取消</van-button>
                <van-button size="small" type="primary" @click="confirmPassword">确定</van-button>
            </div>
        </van-popup>
    </div>
</template>

<script>
import { listNote, delNote, listFolder, addFolder, treeList } from '@/api/px/life/note'
import { showToast, mSuccess, mError, mConfirm, mLoading, closeToast } from '@/mobile/utils/ui'
import NoteCard from './components/NoteCard.vue'

export default {
    name: 'MNote',
    components: {
        NoteCard
    },
    data() {
        return {
            currentFolderId: 0,
            folderPath: [],
            folderList: [],
            noteList: [],
            searchKeyword: '',
            searchTimer: null,
            queryParams: {
                pageNum: 1,
                pageSize: 20
            },
            loading: false,
            listLoading: false,
            refreshing: false,
            finished: false,
            fabExpanded: false,
            folderPopupShow: false,
            newFolderName: '',
            creating: false,
            pendingFolder: null,
            passwordPopupShow: false,
            passwordInput: ''
        }
    },
    computed: {
        currentFolderName() {
            if (this.folderPath.length === 0) return '我的笔记'
            return this.folderPath[this.folderPath.length - 1].name
        },
        headerSubtitle() {
            if (this.searchKeyword) return `正在搜索：${this.searchKeyword}`
            if (this.currentFolderId || this.folderPath.length > 0) return '当前文件夹内容'
            return '收纳灵感、清单和随手记录'
        }
    },
    mounted() {
        const folderId = Number(this.$route.query.folderId) || 0
        this.initFolder(folderId)
    },
    methods: {
        /** 初始化：根目录直接加载；子目录先重建面包屑再加载 */
        async initFolder(folderId) {
            if (folderId > 0) {
                this.currentFolderId = folderId
                this.loading = true
                try {
                    const res = await treeList({})
                    const path = this.buildPathFromTree(res.data || [], folderId)
                    this.folderPath = path || []
                } catch (error) {
                    console.error('重建文件夹路径失败:', error)
                } finally {
                    this.loading = false
                }
            }
            this.loadData()
        },
        /** 在文件夹树中查找目标文件夹的祖先链 */
        buildPathFromTree(nodes, folderId, trail = []) {
            for (const node of nodes || []) {
                const next = [...trail, { id: node.id, name: node.name }]
                if (node.id === folderId) return next
                const found = this.buildPathFromTree(node.children || [], folderId, next)
                if (found) return found
            }
            return null
        },
        /** 加载文件夹 + 笔记 */
        async loadData() {
            if (this.loading) return
            this.loading = true
            try {
                await Promise.all([this.loadFolders(), this.loadNotes()])
            } catch (error) {
                console.error('加载数据失败:', error)
                mError('加载失败')
            } finally {
                this.loading = false
                this.refreshing = false
                this.listLoading = false
            }
        },
        async loadFolders() {
            if (this.searchKeyword) {
                this.folderList = []
                return
            }
            const res = await listFolder({ parentId: this.currentFolderId })
            this.folderList = (res.rows || []).filter(folder => !folder.folder)
        },
        async loadNotes() {
            const params = {
                pageNum: this.queryParams.pageNum,
                pageSize: this.queryParams.pageSize,
                folder: this.currentFolderId || '',
                title: this.searchKeyword || ''
            }
            const res = await listNote(params)
            const rows = res.rows || []
            if (this.queryParams.pageNum === 1) {
                this.noteList = rows
            } else {
                this.noteList = [...this.noteList, ...rows]
            }
            this.finished = rows.length < this.queryParams.pageSize
        },
        refreshData() {
            this.queryParams.pageNum = 1
            this.finished = false
            this.loadData()
        },
        onRefresh() {
            this.refreshData()
        },
        async loadMore() {
            if (this.loading || this.finished) {
                this.listLoading = false
                return
            }
            this.loading = true
            this.queryParams.pageNum++
            try {
                await this.loadNotes()
            } catch (error) {
                console.error('加载更多失败:', error)
            } finally {
                this.loading = false
                this.listLoading = false
            }
        },
        onSearchInput() {
            if (this.searchTimer) clearTimeout(this.searchTimer)
            this.searchTimer = setTimeout(() => {
                this.handleSearch()
            }, 600)
        },
        handleSearch() {
            this.refreshData()
        },
        /** 路由跳转（目标可能未注册，先校验） */
        go(path) {
            if (this.$router.resolve(path).matched.length > 0) {
                this.$router.push(path)
            } else {
                showToast('功能即将上线')
            }
        },
        /** 进入文件夹（带密码校验） */
        enterFolder(folder) {
            if (folder.password) {
                this.pendingFolder = folder
                this.passwordInput = ''
                this.passwordPopupShow = true
                return
            }
            this.navigateToFolder(folder)
        },
        /** 导航到指定文件夹（target 为 id 或文件夹对象；pathIndex 为面包屑索引） */
        navigateToFolder(target, pathIndex = -1) {
            const folderId = typeof target === 'object' ? target.id : Number(target)

            if (folderId === 0) {
                this.folderPath = []
                this.currentFolderId = 0
                this.syncQuery()
                this.refreshData()
                return
            }

            if (pathIndex >= 0 && pathIndex < this.folderPath.length) {
                this.folderPath = this.folderPath.slice(0, pathIndex + 1)
                this.currentFolderId = folderId
                this.syncQuery()
                this.refreshData()
                return
            }

            const folder = typeof target === 'object' ? target : this.folderList.find(f => f.id === folderId)
            this.currentFolderId = folderId
            if (folder) {
                this.folderPath.push({ id: folder.id, name: folder.name })
            }
            this.syncQuery()
            this.refreshData()
        },
        /** 同步 folderId 到路由 query（返回此页时恢复层级） */
        syncQuery() {
            const query = this.currentFolderId ? { folderId: String(this.currentFolderId) } : {}
            this.$router.replace({ query }).catch(() => {})
        },
        viewNote(note) {
            this.go(`/m/note/detail?id=${note.id}`)
        },
        editNote(note) {
            this.go(`/m/note/detail?id=${note.id}`)
        },
        async deleteNote(note) {
            try {
                await mConfirm('删除确认', `确定要删除笔记「${note.title || '无标题'}」吗？`)
            } catch (e) {
                return
            }
            mLoading('删除中')
            try {
                await delNote(note.id)
                closeToast()
                mSuccess('删除成功')
                this.refreshData()
            } catch (error) {
                console.error('删除失败:', error)
                closeToast()
                mError('删除失败')
            }
        },
        toggleFab() {
            this.fabExpanded = !this.fabExpanded
        },
        handleCreateNote() {
            this.fabExpanded = false
            this.go(`/m/note/detail?folder=${this.currentFolderId}`)
        },
        handleCreateFolder() {
            this.fabExpanded = false
            this.newFolderName = ''
            this.folderPopupShow = true
        },
        openFolderManage() {
            this.fabExpanded = false
            this.go('/m/note/folder')
        },
        openObsidian() {
            this.fabExpanded = false
            this.go('/m/note/obsidian')
        },
        async createFolder() {
            const name = (this.newFolderName || '').trim()
            if (!name) {
                showToast('请输入文件夹名称')
                return
            }
            this.creating = true
            try {
                await addFolder({
                    name,
                    parentId: this.currentFolderId || 0
                })
                this.folderPopupShow = false
                mSuccess('创建成功')
                this.refreshData()
            } catch (error) {
                console.error('创建文件夹失败:', error)
                mError('创建失败')
            } finally {
                this.creating = false
            }
        },
        cancelPassword() {
            this.pendingFolder = null
            this.passwordInput = ''
            this.passwordPopupShow = false
        },
        confirmPassword() {
            if (!this.passwordInput) {
                showToast('请输入密码')
                return
            }
            if (this.pendingFolder && this.pendingFolder.password === this.passwordInput) {
                this.passwordPopupShow = false
                const folder = this.pendingFolder
                this.pendingFolder = null
                this.passwordInput = ''
                this.navigateToFolder(folder)
            } else {
                showToast('密码错误')
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.note-page {
    min-height: calc(100vh - 46px);
    padding: 12px 12px 90px;
    box-sizing: border-box;
    background: var(--pnkx-bg);
}

.note-hero {
    &__top {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        margin-bottom: 10px;
    }

    &__copy {
        flex: 1;
        min-width: 0;
    }

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
        line-height: 1.2;
        color: var(--pnkx-text);
    }

    &__subtitle {
        margin-top: 4px;
        font-size: 12px;
        color: var(--pnkx-text-secondary);
    }

    &__actions {
        display: flex;
        align-items: center;
        gap: 6px;
        flex-shrink: 0;
        margin-left: 8px;
    }

    &__chip {
        display: flex;
        align-items: center;
        gap: 3px;
        height: 30px;
        padding: 0 11px;
        border-radius: 999px;
        background: var(--pnkx-surface);
        border: 1px solid var(--pnkx-border-soft);
        color: var(--pnkx-primary);
        font-size: 12px;
        font-weight: 600;
        white-space: nowrap;

        &:active {
            opacity: 0.8;
        }

        &--icon {
            padding: 0 7px;
        }
    }
}

.note-search {
    padding: 0;
    margin: 10px 0 8px;
    background: transparent;

    :deep(.van-search__content) {
        background: var(--pnkx-surface);
        border: 1px solid var(--pnkx-border-soft);
    }
}

.note-crumbs {
    display: flex;
    align-items: center;
    flex-wrap: nowrap;
    overflow-x: auto;
    margin-bottom: 4px;
    -webkit-overflow-scrolling: touch;

    &__item {
        display: flex;
        align-items: center;
        gap: 3px;
        flex-shrink: 0;
        height: 28px;
        padding: 0 10px;
        border-radius: 999px;
        background: var(--pnkx-surface-muted);
        color: var(--pnkx-text-secondary);
        font-size: 12px;
        white-space: nowrap;

        &:active {
            color: var(--pnkx-primary);
        }
    }

    &__sep {
        flex-shrink: 0;
        padding: 0 4px;
        color: var(--pnkx-text-muted);
        font-size: 12px;
    }

    &__placeholder {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }
}

.note-result-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
    padding: 10px 12px;
    border-radius: 10px;
    background: var(--pnkx-primary-soft);
    color: var(--pnkx-primary);
    font-size: 12px;

    &__count {
        color: var(--pnkx-text-secondary);
    }
}

.note-folders {
    display: flex;
    gap: 10px;
    overflow-x: auto;
    padding-bottom: 4px;
    -webkit-overflow-scrolling: touch;

    &__card {
        display: flex;
        align-items: flex-start;
        flex-shrink: 0;
        width: 150px;
        padding: 12px;
        border-radius: 12px;
        background: var(--pnkx-surface);
        border: 1px solid var(--pnkx-border-soft);
        box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);

        &:active {
            opacity: 0.85;
        }
    }

    &__icon {
        width: 32px;
        height: 32px;
        flex-shrink: 0;
        margin-right: 9px;
        border-radius: 9px;
        background: var(--pnkx-primary-soft);
        display: flex;
        align-items: center;
        justify-content: center;
    }

    &__body {
        flex: 1;
        min-width: 0;
    }

    &__name {
        font-size: 13px;
        font-weight: 600;
        color: var(--pnkx-text);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__count {
        margin-top: 5px;
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }
}

.note-fab-wrap {
    position: fixed;
    right: 16px;
    bottom: calc(24px + env(safe-area-inset-bottom));
    z-index: 100;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
}

.note-fab-menu {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    margin-bottom: 12px;

    &__item {
        display: flex;
        align-items: center;
        margin-bottom: 10px;

        &:active {
            opacity: 0.8;
        }
    }

    &__text {
        padding: 6px 10px;
        border-radius: 6px;
        background: var(--pnkx-surface);
        border: 1px solid var(--pnkx-border-soft);
        color: var(--pnkx-text);
        font-size: 12px;
        box-shadow: 0 2px 6px rgba(15, 23, 42, 0.1);
    }

    &__btn {
        width: 34px;
        height: 34px;
        margin-left: 8px;
        border-radius: 50%;
        background: var(--pnkx-primary);
        color: #fff;
        display: flex;
        align-items: center;
        justify-content: center;
    }
}

.note-popup {
    padding: 18px 16px calc(16px + env(safe-area-inset-bottom));

    &__title {
        font-size: 16px;
        font-weight: 600;
        color: var(--pnkx-text);
        text-align: center;
        margin-bottom: 14px;
    }

    &__field {
        border: 1px solid var(--pnkx-border-soft);
        border-radius: 8px;
        margin-bottom: 14px;
    }

    &__actions {
        display: flex;
        gap: 10px;

        .van-button {
            flex: 1;
        }
    }
}

.note-pwd {
    width: 300px;
    padding: 20px 18px 16px;

    &__title {
        font-size: 16px;
        font-weight: 600;
        color: var(--pnkx-text);
        text-align: center;
    }

    &__hint {
        margin-top: 6px;
        font-size: 12px;
        color: var(--pnkx-text-muted);
        text-align: center;
    }

    &__field {
        margin-top: 14px;
        border: 1px solid var(--pnkx-border-soft);
        border-radius: 8px;
    }

    &__actions {
        display: flex;
        justify-content: flex-end;
        gap: 8px;
        margin-top: 14px;
    }
}
</style>
