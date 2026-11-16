<!--
 * @File: records
 * @Author: PHY
 * @Description: 壁纸「我的记录」列表（点赞/下载 通用，query.type 区分）
-->
<template>
  <view class="records-page">
    <!-- 顶部 Tab -->
    <view class="tab-bar">
      <view
        v-for="tab in tabs"
        :key="tab.type"
        class="tab-item"
        :class="{ active: currentType === tab.type }"
        @click="switchTab(tab.type)"
      >
        <text>{{ tab.label }}</text>
      </view>
    </view>

    <!-- 记录网格 -->
    <view class="record-grid" v-if="list.length > 0">
      <view
        v-for="item in list"
        :key="item.id"
        class="record-item"
        @click="goDetail(item)"
      >
        <view class="record-img-wrapper">
          <image
            class="record-img"
              :src="item.itemThumbnail || item.itemUrl || defaultThumb"
            mode="aspectFill"
            :lazy-load="true"
          />
        </view>
        <view class="record-info">
          <text class="record-name">{{ item.itemName || '未命名' }}</text>
          <text class="record-time">{{ formatTime(item.createTime) }}</text>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty" v-if="!loading && list.length === 0">
      <view class="iconfont icon-empty empty-icon"></view>
      <text class="empty-text">{{ emptyText }}</text>
    </view>

    <!-- 加载状态 -->
    <view class="load-status" v-if="list.length > 0">
      <text v-if="loading">加载中...</text>
      <text v-else-if="!hasMore">没有更多了</text>
    </view>

    <view class="page-bottom-safe"></view>
  </view>
</template>

<script>
import { listMyLikes, listMyDownloads } from '../api/px/life/wallpaper'

export default {
  name: 'WallpaperRecords',
  data() {
    return {
      currentType: 'like',
      tabs: [
        { type: 'like', label: '点赞' },
        { type: 'download', label: '下载' }
      ],
      list: [],
      pageNum: 1,
      pageSize: 20,
      total: 0,
      loading: false,
      defaultThumb: ''
    }
  },
  computed: {
    hasMore() {
      return this.list.length < this.total
    },
    emptyText() {
      const map = { like: '还没有点赞过壁纸', download: '还没有下载过壁纸' }
      return map[this.currentType] || '暂无记录'
    }
  },
  onLoad(options = {}) {
    if (options.type && this.tabs.some(tab => tab.type === options.type)) {
      this.currentType = options.type
    }
    this.loadList(true)
  },
  onPullDownRefresh() {
    this.loadList(true).finally(() => {
      uni.stopPullDownRefresh()
    })
  },
  onReachBottom() {
    if (this.hasMore && !this.loading) {
      this.pageNum++
      this.loadList(false)
    }
  },
  methods: {
    getApi() {
      if (this.currentType === 'like') return listMyLikes
      return listMyDownloads
    },
    loadList(reset) {
      this.loading = true
      if (reset) {
        this.pageNum = 1
        this.list = []
      }
      return this.getApi()({
        pageNum: this.pageNum,
        pageSize: this.pageSize
      }).then(res => {
        const rows = res.rows || []
        this.list = reset ? rows : this.list.concat(rows)
        this.total = res.total || 0
      }).catch(() => {
        uni.showToast({ title: '加载失败', icon: 'none' })
      }).finally(() => {
        this.loading = false
      })
    },
    switchTab(type) {
      if (this.currentType === type) return
      this.currentType = type
      this.loadList(true)
    },
    goDetail(item) {
      if (!item.itemId) return
      this.$tab.navigateTo('/pages_wallpaper/wallpaper/detail?id=' + item.itemId)
    },
    formatTime(t) {
      if (!t) return ''
      // 取 YYYY-MM-DD HH:mm 部分
      return String(t).replace('T', ' ').substring(0, 16)
    }
  }
}
</script>

<style lang="scss" scoped>
page {
  background-color: $bg-page;
}

.records-page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: env(safe-area-inset-bottom);
}

/* Tab 栏 */
.tab-bar {
  display: flex;
  background: $bg-card;
  position: sticky;
  top: 0;
  z-index: $z-sticky;
  box-shadow: $shadow-sm;

  .tab-item {
    flex: 1;
    text-align: center;
    padding: $spacing-md 0;
    font-size: $font-body;
    color: $text-tertiary;
    position: relative;
    transition: color $duration-fast $ease-default;

    &.active {
      color: $primary;
      font-weight: $font-weight-semibold;

      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 48rpx;
        height: 6rpx;
        border-radius: $radius-full;
        background: $primary;
      }
    }
  }
}

/* 网格 */
.record-grid {
  display: flex;
  flex-wrap: wrap;
  padding: $spacing-sm $page-padding;
}

.record-item {
  width: calc((100% - #{$spacing-sm}) / 2);
  margin-right: $spacing-sm;
  margin-bottom: $spacing-md;
  border-radius: $radius-lg;
  overflow: hidden;
  background: $bg-card;
  box-shadow: $shadow-card;

  &:nth-child(2n) {
    margin-right: 0;
  }

  &:active {
    opacity: 0.85;
  }

  .record-img-wrapper {
    width: 100%;
    padding-top: 133%;
    /* 3:4 比例占位 */
    position: relative;

    .record-img {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
    }
  }

  .record-info {
    padding: $spacing-xs $spacing-sm;

    .record-name {
      display: block;
      font-size: $font-caption;
      color: $text-primary;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .record-time {
      display: block;
      margin-top: 2rpx;
      font-size: 22rpx;
      color: $text-disabled;
    }
  }
}

/* 空状态 */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;

  .empty-icon {
    font-size: 96rpx;
    color: $text-disabled;
  }

  .empty-text {
    margin-top: $spacing-md;
    font-size: $font-body;
    color: $text-tertiary;
  }
}

/* 加载状态 */
.load-status {
  text-align: center;
  padding: $spacing-lg 0;
  font-size: $font-caption;
  color: $text-tertiary;
}

.page-bottom-safe {
  height: 80rpx;
}
</style>
