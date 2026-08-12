<template>
  <view class="recipe-page subpage-shell">
    <!-- Search -->
    <view class="search-bar">
      <uni-search-bar
        v-model="keyword"
        placeholder="搜索菜谱"
        :radius="20"
        :clearButton="true"
        @confirm="handleSearch"
        @clear="handleClear"
        @cancel="handleClear"
      />
    </view>

    <!-- List -->
    <view class="grid" v-if="list.length > 0">
      <view
        class="card"
        v-for="item in list"
        :key="item.id"
        @click="openDetail(item)"
      >
        <view class="card__cover">
          <image v-if="item.url" :src="item.url" mode="aspectFill" class="card__img" />
          <view v-else class="card__placeholder">
            <text class="card__placeholder-text">🍳</text>
          </view>
        </view>
        <view class="card__body">
          <text class="card__title">{{ item.title }}</text>
          <view class="card__meta">
            <text v-if="item.servings" class="card__meta-item">🍽 {{ item.servings }}人份</text>
          </view>
        </view>
      </view>
    </view>

    <!-- Loading -->
    <view class="status" v-else-if="loading">
      <text class="status__text">加载中…</text>
    </view>

    <!-- Empty -->
    <view class="empty" v-else>
      <text class="empty__emoji">🍳</text>
      <text class="empty__text">{{ keyword ? '没有找到相关菜谱' : '还没有菜谱' }}</text>
      <text class="empty__hint" v-if="!keyword">可在后台管理中添加菜谱</text>
    </view>

    <view class="safe-bottom"></view>
  </view>
</template>

<script>
import { listRecipe } from '@/api/px/life/recipe'

export default {
  data() {
    return {
      keyword: '',
      list: [],
      loading: true,
      page: { pageNum: 1, pageSize: 50 }
    }
  },
  onLoad() {
    this.loadList()
  },
  methods: {
    async loadList() {
      this.loading = true
      try {
        const params = { ...this.page }
        if (this.keyword) params.title = this.keyword
        const res = await listRecipe(params)
        this.list = res.rows || []
      } catch (e) {
        uni.showToast({ title: '加载失败', icon: 'none' })
      }
      this.loading = false
    },
    handleSearch() {
      this.loadList()
    },
    handleClear() {
      this.keyword = ''
      this.loadList()
    },
    openDetail(item) {
      uni.navigateTo({
        url: '/pages_life/recipe/detail?id=' + item.id + '&title=' + encodeURIComponent(item.title)
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.recipe-page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: $spacing-xl;
}

.search-bar {
  padding: $spacing-sm $page-padding;
  background: $bg-card;
  box-shadow: $shadow-card;
}

.grid {
  display: flex;
  flex-wrap: wrap;
  padding: $spacing-md $page-padding;
  gap: $spacing-md;
}

.card {
  width: calc(50% - #{$spacing-md} / 2);
  background: $bg-card;
  border-radius: $radius-lg;
  box-shadow: $shadow-card;
  overflow: hidden;

  &:active {
    opacity: 0.85;
  }

  &__cover {
    width: 100%;
    height: 240rpx;
    background: linear-gradient(135deg, rgba(251, 146, 60, 0.15), rgba(251, 191, 36, 0.15));
  }

  &__img {
    width: 100%;
    height: 100%;
  }

  &__placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__placeholder-text {
    font-size: 72rpx;
  }

  &__body {
    padding: $spacing-sm $spacing-md $spacing-md;
  }

  &__title {
    font-size: $font-body;
    font-weight: $font-weight-semibold;
    color: $text-primary;
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__meta {
    margin-top: $spacing-xs;
    display: flex;
    flex-wrap: wrap;
  }

  &__meta-item {
    font-size: $font-mini;
    color: $text-tertiary;
    margin-right: $spacing-sm;
  }
}

.status {
  padding-top: 160rpx;
  text-align: center;

  &__text {
    font-size: $font-body;
    color: $text-tertiary;
  }
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;

  &__emoji {
    font-size: 96rpx;
    margin-bottom: $spacing-md;
    opacity: 0.6;
  }

  &__text {
    font-size: $font-body;
    color: $text-tertiary;
  }

  &__hint {
    font-size: $font-caption;
    color: $text-tertiary;
    margin-top: $spacing-xs;
  }
}

.safe-bottom {
  height: env(safe-area-inset-bottom);
}
</style>
