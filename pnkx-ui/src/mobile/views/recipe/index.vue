<template>
    <div class="recipe-page">
        <!-- 搜索 -->
        <van-search
            v-model="keyword"
            class="recipe-page__search"
            placeholder="搜索菜谱"
            shape="round"
            :clearable="true"
            @search="handleSearch"
            @clear="handleClear"
        />

        <van-pull-refresh v-model="refreshing" class="m-pull" success-text="已刷新" @refresh="onRefresh">
            <van-list
                v-model:loading="loading"
                :finished="finished"
                finished-text="没有更多了"
                @load="loadMore"
            >
                <!-- 菜谱卡片（两列网格） -->
                <div v-if="list.length > 0" class="recipe-grid">
                    <div v-for="item in list" :key="item.id" class="recipe-card" @click="openDetail(item)">
                        <div class="recipe-card__cover">
                            <img v-if="item.url" :src="item.url" alt="" class="recipe-card__img">
                            <div v-else class="recipe-card__placeholder">
                                <span>🍳</span>
                            </div>
                        </div>
                        <div class="recipe-card__body">
                            <div class="recipe-card__title">{{ item.title }}</div>
                            <div class="recipe-card__meta">
                                <span v-if="item.servings">🍽 {{ item.servings }}人份</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 空态 -->
                <div v-if="!loading && list.length === 0" class="m-empty">
                    <span class="recipe-page__empty-emoji">🍳</span>
                    <span>{{ keyword ? '没有找到相关菜谱' : '还没有菜谱' }}</span>
                    <span v-if="!keyword" class="m-text-muted">可在后台管理中添加菜谱</span>
                </div>
            </van-list>
        </van-pull-refresh>
    </div>
</template>

<script>
import { listRecipe } from '@/api/px/life/recipe'
import { showToast } from '@/mobile/utils/ui'

export default {
    name: 'MRecipe',
    data() {
        return {
            keyword: '',
            list: [],
            loading: false,
            finished: false,
            refreshing: false,
            pageNum: 1,
            pageSize: 20,
            total: 0
        }
    },
    methods: {
        async fetchPage(reset) {
            this.loading = true
            try {
                const params = { pageNum: this.pageNum, pageSize: this.pageSize }
                if (this.keyword) params.title = this.keyword
                const res = await listRecipe(params)
                const rows = res.rows || []
                this.total = Number(res.total || 0)
                this.list = reset ? rows : this.list.concat(rows)
                this.finished = this.list.length >= this.total || rows.length < this.pageSize
            } catch (e) {
                showToast('加载失败')
                this.finished = true
            } finally {
                this.loading = false
            }
        },
        loadMore() {
            if (this.list.length === 0 || this.list.length < this.total) this.pageNum = Math.floor(this.list.length / this.pageSize) + 1
            this.fetchPage(this.list.length === 0)
        },
        onRefresh() {
            this.pageNum = 1
            this.fetchPage(true).finally(() => {
                this.refreshing = false
            })
        },
        handleSearch() {
            this.pageNum = 1
            this.fetchPage(true)
        },
        handleClear() {
            this.keyword = ''
            this.handleSearch()
        },
        openDetail(item) {
            const path = '/m/recipe/detail?id=' + item.id + '&title=' + encodeURIComponent(item.title)
            if (this.$router.resolve(path).matched.length === 0) {
                showToast('功能即将上线')
                return
            }
            this.$router.push(path)
        }
    }
}
</script>

<style lang="scss" scoped>
.recipe-page {
    min-height: calc(100vh - 46px);
    background: var(--pnkx-bg);
    padding-bottom: 24px;

    &__search {
        background: var(--pnkx-surface);
        border-bottom: 1px solid var(--pnkx-border-soft);
    }

    &__empty-emoji {
        font-size: 44px;
        line-height: 1.2;
        opacity: 0.6;
    }
}

.recipe-grid {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    padding: 12px;
}

.recipe-card {
    width: calc(50% - 6px);
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 12px;
    overflow: hidden;

    &:active {
        opacity: 0.85;
    }

    &__cover {
        width: 100%;
        height: 120px;
        background: var(--pnkx-warning-soft);
    }

    &__img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        display: block;
    }

    &__placeholder {
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 36px;
    }

    &__body {
        padding: 8px 10px 10px;
    }

    &__title {
        font-size: 14px;
        font-weight: 600;
        color: var(--pnkx-text);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__meta {
        margin-top: 4px;
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }
}
</style>
