<template>
    <div class="m-book-page">
        <!-- 搜索栏 -->
        <van-search
            v-model="searchKeyword"
            class="m-book-page__search"
            placeholder="搜索书名、作者..."
            shape="round"
            :clearable="true"
            @search="handleSearch"
            @clear="handleSearch"
            @update:model-value="onSearchInput"
        />

        <!-- 状态筛选 Tab -->
        <div class="m-book-tabs">
            <div class="m-book-tabs__scroll">
                <span
                    v-for="tab in statusTabs"
                    :key="tab.value"
                    class="m-book-tabs__item"
                    :class="{ active: activeStatus === tab.value }"
                    @click="switchStatus(tab.value)"
                >{{ tab.label }}</span>
            </div>
        </div>

        <!-- 书架列表 -->
        <van-pull-refresh v-model="isRefreshing" class="m-pull m-book-pull" @refresh="onRefresh">
            <van-list
                v-model:loading="loading"
                :finished="loadFinished"
                finished-text="没有更多了"
                @load="loadMore"
            >
                <div v-if="bookList.length === 0 && !loading" class="m-empty">
                    <van-icon name="bookmark-o" size="42" color="var(--pnkx-primary)" />
                    <span>书架空空如也</span>
                    <span class="m-text-muted">点击右下角导入 TXT 或新建一本书吧</span>
                </div>
                <BookCard
                    v-for="item in bookList"
                    :key="item.id"
                    :item="item"
                    @click="handleOpenBook"
                    @edit="handleEdit"
                    @delete="handleDelete"
                />
            </van-list>
        </van-pull-refresh>

        <!-- FAB：导入 TXT / 新建 -->
        <button class="m-fab m-fab--no-tabbar" @click="showFabActions = true">
            <van-icon name="plus" />
        </button>
        <van-action-sheet
            v-model:show="showFabActions"
            :actions="fabActions"
            cancel-text="取消"
            close-on-click-action
            @select="onFabSelect"
        />

        <!-- TXT 导入隐藏 file input -->
        <input
            ref="fileInput"
            type="file"
            accept=".txt,text/plain"
            class="m-book-file-input"
            @change="onFileChange"
        />
    </div>
</template>

<script>
import { listBooks, deleteBooks, importBookTxt } from '@/api/px/app/book'
import { mConfirm, mError, mLoading, closeToast, mSuccess, showToast } from '@/mobile/utils/ui'
import BookCard from './components/BookCard.vue'

export default {
    name: 'MBook',
    components: { BookCard },
    data() {
        return {
            searchKeyword: '',
            activeStatus: '',
            statusTabs: [
                { label: '全部', value: '' },
                { label: '在读', value: 'reading' },
                { label: '已读完', value: 'finished' },
                { label: '书架', value: 'shelved' }
            ],
            bookList: [],
            loading: false,
            fetching: false,
            loadFinished: false,
            isRefreshing: false,
            pageNum: 1,
            pageSize: 20,
            total: 0,
            showFabActions: false,
            fabActions: [
                { name: '导入 TXT', subname: '选择本地 .txt 文件（不超过 20MB）', key: 'import' },
                { name: '新建书籍', key: 'add' }
            ]
        }
    },
    mounted() {
        // van-list 首次进入会自动触发 load，无需手动加载
    },
    methods: {
        async loadBookList(refresh = false) {
            if (this.fetching) {
                this.isRefreshing = false
                this.loading = false
                return
            }
            this.fetching = true
            this.loading = true
            if (refresh) {
                this.pageNum = 1
                this.bookList = []
                this.loadFinished = false
            }
            try {
                const params = {
                    pageNum: this.pageNum,
                    pageSize: this.pageSize
                }
                if (this.searchKeyword) params.title = this.searchKeyword
                if (this.activeStatus) params.status = this.activeStatus
                const response = await listBooks(params)
                if (response.code === 200) {
                    const newList = response.rows || []
                    this.total = response.total || 0
                    this.bookList = refresh ? newList : [...this.bookList, ...newList]
                    this.loadFinished = this.bookList.length >= this.total
                }
            } catch (error) {
                console.error('加载书架失败:', error)
            } finally {
                this.fetching = false
                this.loading = false
                this.isRefreshing = false
            }
        },

        loadMore() {
            this.pageNum = Math.floor(this.bookList.length / this.pageSize) + 1
            this.loadBookList(this.bookList.length === 0)
        },

        onRefresh() {
            this.loadBookList(true)
        },

        handleSearch() {
            this.loadBookList(true)
        },

        onSearchInput(value) {
            if (!value) this.handleSearch()
        },

        switchStatus(value) {
            if (this.activeStatus === value) return
            this.activeStatus = value
            this.loadBookList(true)
        },

        go(path) {
            // 路由分批注册，跳转前先校验
            if (this.$router.resolve(path).matched.length === 0) {
                showToast('功能即将上线')
                return
            }
            this.$router.push(path)
        },

        handleOpenBook(item) {
            this.go(`/m/book/detail?id=${item.id}`)
        },

        handleEdit(item) {
            this.go(`/m/book/edit?id=${item.id}`)
        },

        async handleDelete(item) {
            try {
                await mConfirm('确认删除', `确定要删除《${item.title}》吗？所有章节将一并删除。`)
            } catch (e) {
                return
            }
            try {
                mLoading('删除中...')
                const response = await deleteBooks(item.id)
                if (response.code === 200) {
                    closeToast()
                    mSuccess('删除成功')
                    this.loadBookList(true)
                }
            } catch (error) {
                console.error('删除书籍失败:', error)
            } finally {
                closeToast()
            }
        },

        onFabSelect(action) {
            this.showFabActions = false
            if (action.key === 'add') {
                this.go('/m/book/edit')
            } else if (action.key === 'import') {
                this.handleImportTxt()
            }
        },

        // ===== TXT 导入 =====
        handleImportTxt() {
            this.$nextTick(() => {
                const input = this.$refs.fileInput
                if (input) input.click()
            })
        },

        onFileChange(e) {
            const file = e.target.files && e.target.files[0]
            e.target.value = ''
            if (!file) return
            const name = file.name || ''
            if (!/\.txt$/i.test(name)) {
                showToast('请选择 .txt 文件')
                return
            }
            if (file.size > 20 * 1024 * 1024) {
                showToast('文件不能超过 20MB')
                return
            }
            this.doImport(file)
        },

        async doImport(file) {
            try {
                mLoading('导入中...')
                const response = await importBookTxt(file, {})
                if (response.code === 200) {
                    mSuccess('导入成功')
                    this.loadBookList(true)
                }
            } catch (error) {
                console.error('导入 TXT 失败:', error)
                mError('导入失败')
            } finally {
                closeToast()
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.m-book-page {
    min-height: calc(100vh - 46px);
    background: var(--pnkx-bg);
    padding-bottom: calc(80px + env(safe-area-inset-bottom));
}

.m-book-page__search {
    background: var(--pnkx-surface);
}

.m-book-tabs {
    background: var(--pnkx-surface);
    padding: 6px 12px 10px;
    border-bottom: 1px solid var(--pnkx-border-soft);
}

.m-book-tabs__scroll {
    display: flex;
    gap: 8px;
    overflow-x: auto;
    white-space: nowrap;

    &::-webkit-scrollbar {
        display: none;
    }
}

.m-book-tabs__item {
    display: inline-flex;
    align-items: center;
    padding: 4px 14px;
    border-radius: 999px;
    background: var(--pnkx-surface-muted);
    font-size: 13px;
    color: var(--pnkx-text-secondary);
    flex-shrink: 0;

    &.active {
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
        font-weight: 600;
    }
}

.m-book-pull {
    padding: 12px;
}

.m-book-file-input {
    position: absolute;
    width: 0;
    height: 0;
    opacity: 0;
    pointer-events: none;
    left: -9999px;
}
</style>
