<!--
 * @File: index
 * @Author: PHY
 * @Date: 2025/03/10
 * @Description: 纪念日列表页
-->
<template>
  <view class="commemoration-day-page subpage-shell">
    <view class="search-bar">
      <uni-search-bar
        v-model="searchKeyword"
        placeholder="搜索纪念日..."
        @confirm="handleSearch"
        @clear="handleSearch"
        @input="onSearchInput"
        radius="100"
        :bgColor="$bgPage"
        :focus="false"
        :show-action="false"
      />
    </view>

    <scroll-view
      class="content-scroll"
      scroll-y
      @scrolltolower="loadMore"
      lower-threshold="50"
      refresher-enabled
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onRefresh"
    >
      <view v-if="filteredList.length === 0" class="empty-state">
        <uni-icons type="calendar" size="80" :color="$textTertiary" />
        <text class="empty-text">暂无纪念日</text>
        <text class="empty-tip">点击右下角按钮添加纪念日吧</text>
      </view>

      <view v-else class="list-container">
        <CommemorationDayCard
          v-for="item in filteredList"
          :key="item.id"
          :item="item"
        />
      </view>

      <view v-if="filteredList.length > 0" class="load-more">
        <uni-load-more :status="loadMoreStatus" />
      </view>
    </scroll-view>

    <view class="fab-button" @click="handleAdd">
      <uni-icons type="plus" size="24" color="#FFFFFF" />
    </view>
  </view>
</template>

<script>
import { listDay, delDay } from '@/api/px/life/commemorationDay'
import CommemorationDayCard from './components/CommemorationDayCard.vue'

export default {
  name: 'CommemorationDayIndex',
  components: {
    CommemorationDayCard
  },
  data() {
    return {
      searchKeyword: '',
      commemorationList: [],
      loading: false,
      isRefreshing: false,
      loadMoreStatus: 'more',
      pageNum: 1,
      pageSize: 20,
      total: 0
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
  onLoad() {
    this.loadCommemorationList()
  },
  onShow() {
    this.refreshList()
  },
  methods: {
    async loadCommemorationList(refresh = false) {
      if (this.loading) return

      this.loading = true

      if (refresh) {
        this.pageNum = 1
        this.commemorationList = []
      }

      try {
        const params = {
          pageNum: this.pageNum,
          pageSize: this.pageSize
        }

        const response = await listDay(params)

        if (response.code === 200) {
          const newList = response.rows || []
          this.total = response.total || 0

          if (refresh) {
            this.commemorationList = newList
          } else {
            this.commemorationList = [...this.commemorationList, ...newList]
          }

          this.loadMoreStatus = this.commemorationList.length >= this.total ? 'noMore' : 'more'
        } else {
          this.loadMoreStatus = 'more'
        }
      } catch (error) {
        console.error('加载纪念日列表失败:', error)
        this.loadMoreStatus = 'more'
        uni.showToast({
          title: '加载失败',
          icon: 'none'
        })
      } finally {
        this.loading = false
        this.isRefreshing = false
      }
    },

    loadMore() {
      if (this.loadMoreStatus !== 'more') return
      this.pageNum++
      this.loadMoreStatus = 'loading'
      this.loadCommemorationList()
    },

    onRefresh() {
      this.isRefreshing = true
      this.refreshList()
    },

    refreshList() {
      this.loadCommemorationList(true)
    },

    handleSearch() {
      if (this.searchKeyword) {
        this.loadCommemorationList(true)
      }
    },

    onSearchInput(e) {
      if (e.value === '' || !e.value) {
        this.handleSearch()
      }
    },

    handleAdd() {
      uni.navigateTo({
        url: '/pages_life/commemorationDay/add'
      })
    },
  }
}
</script>

<style lang="scss" scoped>
.commemoration-day-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: $bg-page;

  .search-bar {
    padding: $spacing-md $page-padding;
    background-color: $bg-card;
    position: sticky;
    top: 0;
    z-index: $z-sticky;
  }

  .content-scroll {
    flex: 1;
    height: 0;
    padding: $spacing-md $page-padding 160rpx;
  }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: $spacing-3xl 0;

    .empty-text {
      font-size: $font-body;
      color: $text-tertiary;
      margin-top: $spacing-lg;
    }

    .empty-tip {
      font-size: $font-caption;
      color: $text-disabled;
      margin-top: $spacing-xs;
    }
  }

  .list-container {
    padding-bottom: $spacing-md;
  }

  .load-more {
    padding: $spacing-md 0;
  }

  .fab-button {
    position: fixed;
    right: 40rpx;
    bottom: 100rpx;
    width: 100rpx;
    height: 100rpx;
    background-color: $commemoration;
    border-radius: $radius-full;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4rpx 16rpx rgba($commemoration, 0.4);
    z-index: $z-fixed;
    transition: transform $duration-fast $ease-spring;

    &:active {
      transform: scale(0.92);
    }
  }
}
</style>
