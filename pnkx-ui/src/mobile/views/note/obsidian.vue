<template>
    <div class="obsidian-page">
        <!-- 头部：标题 + 目录面包屑 -->
        <div class="obs-header">
            <div class="obs-header__title">Obsidian 笔记</div>
            <div class="obs-breadcrumb">
                <span class="obs-breadcrumb__item" @click="navigateTo('')">根目录</span>
                <template v-for="(crumb, index) in breadcrumbs" :key="index">
                    <span class="obs-breadcrumb__sep">/</span>
                    <span class="obs-breadcrumb__item" @click="navigateTo(crumb.path)">{{ crumb.name }}</span>
                </template>
            </div>
        </div>

        <!-- 搜索 -->
        <van-search
            v-model="searchQuery"
            class="obs-search"
            placeholder="搜索文件名或内容"
            shape="round"
            clearable
            @search="doSearch"
            @clear="clearSearch"
        />

        <!-- 内容 -->
        <van-pull-refresh v-model="refreshing" class="m-pull" @refresh="onRefresh">
            <!-- 搜索结果 -->
            <template v-if="searchResults.length > 0">
                <div class="m-section-title">搜索结果（{{ searchResults.length }}）</div>
                <van-swipe-cell v-for="item in searchResults" :key="item.path" class="obs-swipe">
                    <div class="obs-item" @click="openFile(item)">
                        <span class="obs-item__icon">{{ item.isDirectory ? '📁' : getFileEmoji(item.name) }}</span>
                        <div class="obs-item__body">
                            <div class="obs-item__name">{{ item.name }}</div>
                            <div v-if="item.matchedContent" class="obs-item__match">内容匹配</div>
                        </div>
                        <van-icon name="arrow" size="14" color="var(--pnkx-text-muted)" />
                    </div>
                    <template #right>
                        <van-button square type="danger" text="删除" class="obs-swipe__btn" @click="handleDelete(item)" />
                    </template>
                </van-swipe-cell>
            </template>

            <!-- 文件列表 -->
            <template v-else>
                <div v-if="loading" class="m-empty">
                    <van-loading size="24" color="var(--pnkx-primary)" />
                    <span>加载中…</span>
                </div>
                <div v-else-if="sortedList.length === 0" class="m-empty">
                    <van-icon name="folder-o" size="42" color="var(--pnkx-primary)" />
                    <span>空目录</span>
                    <span class="m-text-muted">点右下角新建一个文件或文件夹</span>
                </div>
                <!-- 文件夹优先 -->
                <van-swipe-cell v-for="item in sortedList" :key="item.path" class="obs-swipe">
                    <div class="obs-item" @click="onFileClick(item)">
                        <span class="obs-item__icon">{{ item.isDirectory ? '📁' : getFileEmoji(item.name) }}</span>
                        <div class="obs-item__body">
                            <div class="obs-item__name">{{ item.name }}</div>
                            <div v-if="!item.isDirectory && item.size" class="obs-item__size">{{ formatSize(item.size) }}</div>
                        </div>
                        <van-icon v-if="!item.isDirectory" name="arrow" size="14" color="var(--pnkx-text-muted)" />
                    </div>
                    <template #right>
                        <van-button square type="danger" text="删除" class="obs-swipe__btn" @click="handleDelete(item)" />
                    </template>
                </van-swipe-cell>
            </template>
        </van-pull-refresh>

        <!-- 悬浮新建 -->
        <div class="obs-fab-wrap">
            <div v-if="fabExpanded" class="obs-fab-menu">
                <div class="obs-fab-menu__item" @click="openCreate('file')">
                    <span class="obs-fab-menu__text">新建文件</span>
                    <span class="obs-fab-menu__btn"><van-icon name="notes-o" size="16" /></span>
                </div>
                <div class="obs-fab-menu__item" @click="openCreate('dir')">
                    <span class="obs-fab-menu__text">新建文件夹</span>
                    <span class="obs-fab-menu__btn"><van-icon name="folder-o" size="16" /></span>
                </div>
            </div>
            <button class="m-fab m-fab--no-tabbar" @click="toggleFab">
                <van-icon :name="fabExpanded ? 'cross' : 'plus'" size="22" />
            </button>
        </div>

        <!-- 新建弹窗 -->
        <van-popup v-model:show="createShow" position="bottom" round class="obs-create">
            <div class="obs-create__title">{{ createType === 'dir' ? '新建文件夹' : '新建文件' }}</div>
            <div class="obs-create__body">
                <van-field
                    v-model="createName"
                    :placeholder="createType === 'dir' ? '请输入文件夹名称' : '请输入文件名（如 笔记.md）'"
                    clearable
                    class="obs-create__field"
                    @keyup.enter="handleCreate"
                />
            </div>
            <div class="obs-create__actions">
                <van-button block plain @click="createShow = false">取消</van-button>
                <van-button block type="primary" :loading="creating" @click="handleCreate">确定</van-button>
            </div>
        </van-popup>
    </div>
</template>

<script>
import { listFiles, searchFiles, createFile, mkdirFile, deleteFile } from '@/api/system/fileManager'
import { showToast, mSuccess, mError, mConfirm, mLoading, closeToast } from '@/mobile/utils/ui'

const FILE_EMOJI = {
    md: '📝',
    txt: '📄',
    png: '🖼',
    jpg: '🖼',
    jpeg: '🖼',
    gif: '🖼',
    pdf: '📕',
    json: '⚙️',
    js: '⚙️',
    html: '🌐'
}

export default {
    name: 'MNoteObsidian',
    data() {
        return {
            currentPath: '',
            fileList: [],
            loading: false,
            refreshing: false,
            searchQuery: '',
            searchResults: [],
            fabExpanded: false,
            createShow: false,
            createType: 'file',
            createName: '',
            creating: false
        }
    },
    computed: {
        breadcrumbs() {
            if (!this.currentPath) return []
            const parts = this.currentPath.split('/').filter(Boolean)
            const crumbs = []
            let path = ''
            parts.forEach(part => {
                path = path ? path + '/' + part : part
                crumbs.push({ name: part, path })
            })
            return crumbs
        },
        sortedList() {
            return [...this.fileList].sort((a, b) => {
                if (a.isDirectory && !b.isDirectory) return -1
                if (!a.isDirectory && b.isDirectory) return 1
                return (a.name || '').localeCompare(b.name || '')
            })
        }
    },
    mounted() {
        // 从编辑页返回时（页面重挂载）恢复目录并刷新
        this.loadFiles(String(this.$route.query.path || ''))
    },
    methods: {
        loadFiles(path) {
            this.loading = true
            this.currentPath = path || ''
            listFiles(this.currentPath).then(res => {
                this.fileList = res.data || []
            }).catch(() => {
                mError('加载失败')
            }).finally(() => {
                this.loading = false
                this.refreshing = false
            })
        },
        /** 同步目录到 query（编辑页返回时恢复） */
        syncQuery() {
            const query = this.currentPath ? { path: this.currentPath } : {}
            this.$router.replace({ query }).catch(() => {})
        },
        navigateTo(path) {
            this.searchQuery = ''
            this.searchResults = []
            this.loadFiles(path)
            this.syncQuery()
        },
        onRefresh() {
            this.loadFiles(this.currentPath)
        },
        onFileClick(item) {
            if (item.isDirectory) {
                this.navigateTo(item.path)
            } else {
                this.openFile(item)
            }
        },
        openFile(item) {
            // 路由路径以 router/map.js 的 uniapp 映射为准（/m/note/obsidian/edit）
            const path = `/m/note/obsidian/edit?path=${encodeURIComponent(item.path)}`
            if (this.$router.resolve(path).matched.length > 0) {
                this.$router.push(path)
            } else {
                showToast('功能即将上线')
            }
        },
        async doSearch() {
            if (!this.searchQuery.trim()) return
            mLoading('搜索中')
            try {
                const res = await searchFiles(this.searchQuery)
                this.searchResults = res.data || []
                closeToast()
            } catch (e) {
                console.error('搜索失败:', e)
                closeToast()
                mError('搜索失败')
            }
        },
        clearSearch() {
            this.searchQuery = ''
            this.searchResults = []
        },
        async handleDelete(item) {
            const typeText = item.isDirectory ? '文件夹' : '文件'
            try {
                await mConfirm('删除确认', `确定要删除${typeText}「${item.name}」吗？删除后无法恢复。`)
            } catch (e) {
                return
            }
            mLoading('删除中')
            try {
                await deleteFile(item.path)
                closeToast()
                mSuccess('删除成功')
                if (this.searchResults.length > 0) {
                    this.searchResults = this.searchResults.filter(f => f.path !== item.path)
                } else {
                    this.loadFiles(this.currentPath)
                }
            } catch (error) {
                console.error('删除失败:', error)
                closeToast()
                mError('删除失败')
            }
        },
        toggleFab() {
            this.fabExpanded = !this.fabExpanded
        },
        openCreate(type) {
            this.fabExpanded = false
            this.createType = type
            this.createName = ''
            this.createShow = true
        },
        async handleCreate() {
            const name = (this.createName || '').trim()
            if (!name) {
                showToast(this.createType === 'dir' ? '请输入文件夹名称' : '请输入文件名')
                return
            }
            this.creating = true
            try {
                if (this.createType === 'dir') {
                    await mkdirFile({ name, path: this.currentPath })
                } else {
                    await createFile({ name, path: this.currentPath, content: '' })
                }
                this.createShow = false
                mSuccess('创建成功')
                this.loadFiles(this.currentPath)
            } catch (error) {
                console.error('创建失败:', error)
                mError('创建失败')
            } finally {
                this.creating = false
            }
        },
        getFileEmoji(name) {
            const ext = (name || '').split('.').pop().toLowerCase()
            return FILE_EMOJI[ext] || '📄'
        },
        formatSize(bytes) {
            if (!bytes) return ''
            if (bytes < 1024) return bytes + 'B'
            if (bytes < 1048576) return (bytes / 1024).toFixed(1) + 'K'
            return (bytes / 1048576).toFixed(1) + 'M'
        }
    }
}
</script>

<style lang="scss" scoped>
.obsidian-page {
    min-height: calc(100vh - 46px);
    padding: 12px 12px 90px;
    box-sizing: border-box;
    background: var(--pnkx-bg);
}

.obs-header {
    position: relative;
    padding: 14px 16px;
    border-radius: 12px;
    background: var(--pnkx-primary);
    overflow: hidden;

    &::before {
        content: '';
        position: absolute;
        top: -40px;
        right: -24px;
        width: 130px;
        height: 130px;
        border-radius: 50%;
        background: var(--pnkx-primary-soft);
        opacity: 0.35;
    }

    &__title {
        position: relative;
        font-size: 18px;
        font-weight: 700;
        color: #fff;
    }
}

.obs-breadcrumb {
    position: relative;
    margin-top: 6px;
    display: flex;
    flex-wrap: wrap;
    align-items: center;

    &__item {
        font-size: 12px;
        color: rgba(255, 255, 255, 0.88);

        &:active {
            opacity: 0.7;
        }
    }

    &__sep {
        margin: 0 5px;
        font-size: 12px;
        color: rgba(255, 255, 255, 0.55);
    }
}

.obs-search {
    padding: 0;
    margin: 10px 0 6px;
    background: transparent;

    :deep(.van-search__content) {
        background: var(--pnkx-surface);
        border: 1px solid var(--pnkx-border-soft);
    }
}

.obs-swipe {
    display: block;
    margin-bottom: 10px;

    &__btn {
        height: 100%;
    }
}

.obs-item {
    display: flex;
    align-items: center;
    padding: 12px;
    border-radius: 12px;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);

    &:active {
        opacity: 0.8;
    }

    &__icon {
        font-size: 20px;
        margin-right: 11px;
        flex-shrink: 0;
    }

    &__body {
        flex: 1;
        min-width: 0;
    }

    &__name {
        font-size: 14px;
        color: var(--pnkx-text);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__size {
        margin-top: 3px;
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }

    &__match {
        margin-top: 3px;
        font-size: 11px;
        color: var(--pnkx-primary);
    }
}

.obs-fab-wrap {
    position: fixed;
    right: 16px;
    bottom: calc(24px + env(safe-area-inset-bottom));
    z-index: 100;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
}

.obs-fab-menu {
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

.obs-create {
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
</style>
