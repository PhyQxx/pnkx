<!--
 * @File: recipe/index
 * @Author: PHY
 * @Date: 2026/08/04
 * @Description: 菜谱列表页 — 浏览/搜索/新建菜谱
-->
<template>
  <view class="recipe-page">
    <!-- 顶部 -->
    <view class="fixed-header">
      <view class="overview-panel">
        <view class="overview-main">
          <text class="overview-title">菜谱</text>
          <text class="overview-subtitle">{{ headerSubtitle }}</text>
        </view>
        <view class="overview-stat">
          <text class="stat-number">{{ total }}</text>
          <text class="stat-label">道菜</text>
        </view>
      </view>

      <view class="search-bar">
        <uni-search-bar
          v-model="queryParams.title"
          placeholder="搜索菜名..."
          @confirm="handleSearch"
          @clear="handleSearch"
          radius="100"
          bgColor="#F5F5F5"
          :focus="false"
          :show-action="false"
        />
      </view>
    </view>

    <!-- 列表 -->
    <scroll-view
      class="content-scroll"
      scroll-y
      @scrolltolower="loadMore"
      lower-threshold="50"
      refresher-enabled
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onRefresh"
    >
      <!-- 空状态 -->
      <view v-if="recipeList.length === 0 && !loading" class="empty-state">
        <view class="empty-icon">
          <uni-icons type="soup-noodles" size="42" :color="themeColor" />
        </view>
        <text class="empty-text">还没有菜谱</text>
        <text class="empty-tip">点击右下角按钮，添加第一道菜吧</text>
      </view>

      <view v-else class="recipe-list">
        <view
          v-for="item in recipeList"
          :key="item.id"
          class="recipe-card"
          @click="handleDetail(item)"
          @longpress="handleDelete(item)"
        >
          <view class="card-icon" :style="{ background: getCardBg(item) }">
            <uni-icons type="soup-noodles" size="26" color="#fff" />
          </view>
          <view class="card-body">
            <view class="card-top">
              <text class="card-title">{{ item.title }}</text>
              <view v-if="item.servings" class="card-serving">
                <uni-icons type="person" size="13" :color="themeColor" />
                <text>{{ item.servings }}人份</text>
              </view>
            </view>
            <text v-if="item.notes" class="card-notes">{{ item.notes }}</text>
            <view class="card-footer">
              <view v-if="item.url" class="card-link">
                <uni-icons type="paperclip" size="13" color="#94A3B8" />
                <text>含参考链接</text>
              </view>
              <text v-else class="card-time">{{ formatDate(item.createTime) }}</text>
            </view>
          </view>
          <view class="card-arrow">
            <uni-icons type="right" size="16" color="#CBD5E1" />
          </view>
        </view>
      </view>

      <view v-if="recipeList.length > 0" class="load-more">
        <uni-load-more :status="loadMoreStatus" />
      </view>

      <view class="bottom-safe"></view>
    </scroll-view>

    <!-- 新建菜谱 -->
    <view class="fab-button" @click="handleAdd">
      <uni-icons type="plus" size="24" color="#FFFFFF" />
    </view>
  </view>
</template>

<script>
import { listRecipe, delRecipe } from '@/api/px/life/recipe'

export default {
  name: 'RecipeIndex',
  data() {
    return {
      themeColor: '#F59E0B',
      recipeList: [],
      loading: false,
      isRefreshing: false,
      loadMoreStatus: 'more',
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: ''
      },
      total: 0
    }
  },
  computed: {
    headerSubtitle() {
      const weekList = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      const now = new Date()
      return `${now.getMonth() + 1}月${now.getDate()}日 ${weekList[now.getDay()]}`
    }
  },
  onLoad() {
    this.loadList(true)
  },
  onShow() {
    // 从详情页返回时刷新
    if (this.$data.__loaded) {
      this.loadList(true)
    }
  },
  methods: {
    async loadList(reset = false) {
      if (this.loading) return
      this.loading = true
      if (reset) {
        this.queryParams.pageNum = 1
        this.recipeList = []
        this.loadMoreStatus = 'more'
      }
      try {
        const res = await listRecipe(this.queryParams)
        if (res.code === 200) {
          const rows = res.rows || []
          this.recipeList = reset ? rows : [...this.recipeList, ...rows]
          this.total = res.total || 0
          this.loadMoreStatus = this.recipeList.length >= this.total ? 'noMore' : 'more'
        }
      } catch (e) {
        console.error('加载菜谱失败', e)
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.loading = false
        this.isRefreshing = false
        this.$data.__loaded = true
      }
    },

    loadMore() {
      if (this.loadMoreStatus !== 'more' || this.loading) return
      this.queryParams.pageNum++
      this.loadMoreStatus = 'loading'
      this.loadList()
    },

    onRefresh() {
      this.isRefreshing = true
      this.loadList(true)
    },

    handleSearch() {
      this.loadList(true)
    },

    handleDetail(item) {
      uni.navigateTo({
        url: `/pages_blog/recipe/detail?id=${item.id}`
      })
    },

    handleAdd() {
      uni.navigateTo({
        url: '/pages_blog/recipe/detail'
      })
    },

    handleDelete(item) {
      uni.showModal({
        title: '删除菜谱',
        content: `确定要删除「${item.title}」吗？`,
        success: async (res) => {
          if (!res.confirm) return
          try {
            const r = await delRecipe(item.id)
            if (r.code === 200) {
              uni.showToast({ title: '已删除', icon: 'success' })
              this.loadList(true)
            }
          } catch (e) {
            console.error('删除菜谱失败', e)
            uni.showToast({ title: '删除失败', icon: 'none' })
          }
        }
      })
    },

    getCardBg() {
      return 'linear-gradient(135deg, #F59E0B, #FBBF24)'
    },

    formatDate(t) {
      if (!t) return ''
      return t.substring(0, 10)
    }
  }
}
</script>

<style lang="scss" scoped>
.recipe-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, rgba(245, 158, 11, 0.10) 0%, $bg-page 360rpx);

  .fixed-header {
    position: sticky;
    top: 0;
    z-index: 100;
    background: linear-gradient(180deg, rgba(245, 158, 11, 0.14) 0%, rgba(244, 246, 249, 0.96) 100%);
    padding: 24rpx $page-padding 18rpx;
    box-shadow: 0 8rpx 24rpx rgba(74, 85, 104, 0.06);

    .overview-panel {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 18rpx;

      .overview-main {
        display: flex;
        flex-direction: column;
        gap: 8rpx;

        .overview-title {
          font-size: 40rpx;
          font-weight: 700;
          color: $text-primary;
          line-height: 1.2;
        }

        .overview-subtitle {
          font-size: $font-caption;
          color: $text-secondary;
        }
      }

      .overview-stat {
        min-width: 124rpx;
        height: 124rpx;
        border-radius: $radius-full;
        background: linear-gradient(135deg, #F59E0B 0%, #FBBF24 100%);
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        box-shadow: 0 12rpx 28rpx rgba(245, 158, 11, 0.28);

        .stat-number {
          font-size: 36rpx;
          font-weight: 700;
          color: $text-inverse;
          line-height: 1.1;
        }

        .stat-label {
          margin-top: 4rpx;
          font-size: $font-mini;
          color: rgba(255, 255, 255, 0.86);
        }
      }
    }

    .search-bar {
      margin: 0 -16rpx;
    }
  }

  .content-scroll {
    flex: 1;
    height: 0;
    padding: 22rpx $page-padding 0;
  }

  .recipe-list {
    display: flex;
    flex-direction: column;
    gap: 18rpx;
    padding-bottom: 160rpx;
  }

  .recipe-card {
    display: flex;
    align-items: center;
    gap: 20rpx;
    padding: 26rpx;
    border-radius: $radius-xl;
    background-color: $bg-card;
    box-shadow: $shadow-card;
    transition: transform $duration-fast $ease-default;

    &:active {
      transform: scale(0.985);
    }

    .card-icon {
      width: 88rpx;
      height: 88rpx;
      border-radius: $radius-lg;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .card-body {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 10rpx;
    }

    .card-top {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12rpx;

      .card-title {
        flex: 1;
        min-width: 0;
        font-size: $font-body;
        font-weight: $font-weight-semibold;
        color: $text-primary;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .card-serving {
        display: flex;
        align-items: center;
        gap: 4rpx;
        flex-shrink: 0;
        padding: 4rpx 14rpx;
        border-radius: $radius-full;
        background-color: rgba(245, 158, 11, 0.12);

        text {
          font-size: $font-mini;
          color: #F59E0B;
        }
      }
    }

    .card-notes {
      font-size: $font-caption;
      color: $text-secondary;
      line-height: 1.5;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .card-footer {
      display: flex;
      align-items: center;
      gap: 16rpx;

      .card-link {
        display: flex;
        align-items: center;
        gap: 4rpx;

        text {
          font-size: $font-mini;
          color: $text-tertiary;
        }
      }

      .card-time {
        font-size: $font-mini;
        color: $text-tertiary;
      }
    }

    .card-arrow {
      flex-shrink: 0;
    }
  }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 430rpx;
    padding: $spacing-3xl 0;

    .empty-icon {
      width: 132rpx;
      height: 132rpx;
      border-radius: $radius-full;
      background-color: rgba(245, 158, 11, 0.12);
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .empty-text {
      font-size: $font-body;
      color: $text-secondary;
      margin-top: $spacing-lg;
      font-weight: $font-weight-semibold;
    }

    .empty-tip {
      font-size: $font-caption;
      color: $text-tertiary;
      margin-top: $spacing-xs;
    }
  }

  .load-more {
    padding: $spacing-md 0;
  }

  .bottom-safe {
    height: env(safe-area-inset-bottom);
  }

  .fab-button {
    position: fixed;
    right: $page-padding;
    bottom: 92rpx;
    width: 104rpx;
    height: 104rpx;
    background: linear-gradient(135deg, #F59E0B 0%, #FBBF24 100%);
    border-radius: $radius-full;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 12rpx 28rpx rgba(245, 158, 11, 0.38);
    z-index: 999;
    transition: transform 0.2s;

    &:active {
      transform: scale(0.92);
    }
  }
}
</style>
