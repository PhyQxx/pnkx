<template>
    <div class="commemoration-day-page">
        <!-- 搜索 -->
        <div class="search-bar">
            <van-search
                v-model="searchKeyword"
                placeholder="搜索纪念日..."
                shape="round"
                :clearable="true"
                @search="handleSearch"
                @clear="handleSearch"
                @update:model-value="onSearchInput"
            />
        </div>

        <!-- 列表 -->
        <van-pull-refresh v-model="isRefreshing" class="m-pull" success-text="已刷新" @refresh="onRefresh">
            <van-list
                v-model:loading="listLoading"
                :finished="listFinished"
                finished-text="没有更多了"
                @load="onListLoad"
            >
                <div v-if="filteredList.length === 0 && !listLoading && !fetching" class="m-empty">
                    <van-icon name="calendar-o" size="40" color="var(--pnkx-text-muted)" />
                    <span>暂无纪念日</span>
                    <span class="m-text-muted">点击右下角按钮添加纪念日吧</span>
                </div>

                <CommemorationDayCard
                    v-for="item in filteredList"
                    :key="item.id"
                    :item="item"
                    @click="handleEdit"
                    @edit="handleEdit"
                    @delete="handleDelete"
                />
            </van-list>
        </van-pull-refresh>

        <button class="m-fab m-fab--no-tabbar" @click="handleAdd">
            <van-icon name="plus" size="22" />
        </button>
    </div>
</template>

<script>
import { listDay, delDay } from '@/api/px/life/commemorationDay'
import { mConfirm, mSuccess, mError } from '@/mobile/utils/ui'
import CommemorationDayCard from './components/CommemorationDayCard.vue'

export default {
    name: 'MCommemorationDay',
    components: {
        CommemorationDayCard
    },
    data() {
        return {
            searchKeyword: '',
            commemorationList: [],
            listLoading: false,
            listFinished: false,
            fetching: false,
            isRefreshing: false,
            pageNum: 1,
            pageSize: 20,
            total: 0,
            activatedOnce: false
        }
    },
    computed: {
        filteredList() {
            if (!this.searchKeyword) {
                return this.commemorationList
            }
            const keyword = this.searchKeyword.toLowerCase()
            return this.commemorationList.filter(item =>
                (item.name && item.name.toLowerCase().includes(keyword)) ||
                (item.remark && item.remark.toLowerCase().includes(keyword))
            )
        }
    },
    activated() {
        if (this.activatedOnce) {
            this.refreshList()
        }
        this.activatedOnce = true
    },
    methods: {
        async loadPage() {
            if (this.fetching) {
                this.listLoading = false
                return
            }
            this.fetching = true
            this.listLoading = true
            try {
                const response = await listDay({
                    pageNum: this.pageNum,
                    pageSize: this.pageSize
                })

                if (response.code === 200) {
                    const newList = response.rows || []
                    this.total = response.total || 0
                    if (this.pageNum === 1) {
                        this.commemorationList = newList
                    } else {
                        this.commemorationList = [...this.commemorationList, ...newList]
                    }
                    this.listFinished = this.commemorationList.length >= this.total
                    this.pageNum++
                }
            } catch (error) {
                mError('加载失败')
            } finally {
                this.fetching = false
                this.listLoading = false
                this.isRefreshing = false
            }
        },
        onListLoad() {
            this.loadPage()
        },
        onRefresh() {
            this.refreshList()
        },
        refreshList() {
            this.pageNum = 1
            this.listFinished = false
            this.loadPage()
        },
        handleSearch() {
            if (this.searchKeyword) {
                this.refreshList()
            }
        },
        onSearchInput(value) {
            if (!value) {
                this.handleSearch()
            }
        },
        handleAdd() {
            this.$router.push('/m/commemoration-day/add')
        },
        handleEdit(item) {
            this.$router.push(`/m/commemoration-day/add?id=${item.id}`)
        },
        handleDelete(item) {
            mConfirm('确认删除', `确定要删除"${item.name}"吗？`).then(async () => {
                try {
                    const response = await delDay(item.id)
                    if (response.code === 200) {
                        mSuccess('删除成功')
                        this.refreshList()
                    }
                } catch (error) {
                    mError('删除失败')
                }
            }).catch(() => {})
        }
    }
}
</script>

<style lang="scss" scoped>
.commemoration-day-page {
    min-height: calc(100vh - 46px);
    padding: 0 12px 88px;
    background: var(--pnkx-bg);
}

.search-bar {
    margin: 0 -12px;
    background: var(--pnkx-surface);
    position: sticky;
    top: 0;
    z-index: 10;
}
</style>
