<template>
  <view class="obsidian-page">
    <!-- Header -->
    <view class="obs-header">
      <view class="obs-header__bg">
        <view class="obs-header__decor obs-header__decor--circle"></view>
      </view>
      <view class="obs-header__content">
        <text class="obs-header__title">Obsidian 笔记</text>
        <view class="obs-breadcrumb">
          <text class="obs-crumb" @click="navigateTo('')">根目录</text>
          <template v-for="(crumb, i) in breadcrumbs" :key="i">
            <text class="obs-crumb-sep">/</text>
            <text class="obs-crumb" @click="navigateTo(crumb.path)">{{ crumb.name }}</text>
          </template>
        </view>
      </view>
    </view>

    <!-- Search -->
    <view class="obs-search">
      <uni-search-bar
        v-model="searchQuery"
        placeholder="搜索文件名或内容"
        radius="100"
        @confirm="doSearch"
        @clear="clearSearch"
      />
    </view>

    <!-- Search Results -->
    <view v-if="searchResults.length > 0" class="obs-list">
      <view class="obs-section-title">搜索结果（{{ searchResults.length }}）</view>
      <view
        class="obs-item"
        v-for="item in searchResults"
        :key="item.path"
        @click="openFile(item.path)"
      >
        <text class="obs-item__icon">{{ item.isDirectory ? '📁' : getFileEmoji(item.name) }}</text>
        <view class="obs-item__body">
          <text class="obs-item__name">{{ item.name }}</text>
          <text v-if="item.matchedContent" class="obs-item__match">内容匹配</text>
        </view>
      </view>
    </view>

    <!-- File List -->
    <view v-else class="obs-list">
      <view v-if="loading" class="obs-loading">加载中…</view>
      <view v-else-if="fileList.length === 0" class="obs-empty">
        <text class="obs-empty__emoji">📂</text>
        <text class="obs-empty__text">空目录</text>
      </view>
      <!-- 文件夹优先 -->
      <view
        class="obs-item"
        v-for="item in sortedList"
        :key="item.path"
        @click="onFileClick(item)"
      >
        <text class="obs-item__icon">{{ item.isDirectory ? '📁' : getFileEmoji(item.name) }}</text>
        <view class="obs-item__body">
          <text class="obs-item__name">{{ item.name }}</text>
          <text v-if="!item.isDirectory && item.size" class="obs-item__size">{{ formatSize(item.size) }}</text>
        </view>
        <text v-if="!item.isDirectory" class="obs-item__arrow">›</text>
      </view>
    </view>

    <view class="safe-bottom"></view>
  </view>
</template>

<script>
import { listFiles, searchFiles } from '@/api/system/fileManager'

export default {
  data() {
    return {
      currentPath: '',
      fileList: [],
      loading: false,
      searchQuery: '',
      searchResults: []
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
        return a.name.localeCompare(b.name)
      })
    }
  },
  onLoad() {
    this.loadFiles('')
  },
  onShow() {
    // 从编辑页返回时刷新
    if (this.currentPath !== undefined) {
      this.loadFiles(this.currentPath)
    }
  },
  methods: {
    loadFiles(path) {
      this.loading = true
      this.currentPath = path || ''
      listFiles(path || '').then(res => {
        this.fileList = res.data || []
      }).catch(() => {
        uni.showToast({ title: '加载失败', icon: 'none' })
      }).finally(() => {
        this.loading = false
      })
    },
    navigateTo(path) {
      this.searchQuery = ''
      this.searchResults = []
      this.loadFiles(path)
    },
    onFileClick(item) {
      if (item.isDirectory) {
        this.navigateTo(item.path)
      } else {
        this.openFile(item.path)
      }
    },
    openFile(path) {
      uni.navigateTo({
        url: '/pages_life/note/obsidianEdit?path=' + encodeURIComponent(path)
      })
    },
    async doSearch() {
      if (!this.searchQuery.trim()) return
      uni.showLoading({ title: '搜索中…' })
      try {
        const res = await searchFiles(this.searchQuery)
        this.searchResults = res.data || []
      } catch (e) {
        uni.showToast({ title: '搜索失败', icon: 'none' })
      }
      uni.hideLoading()
    },
    clearSearch() {
      this.searchQuery = ''
      this.searchResults = []
    },
    getFileEmoji(name) {
      const ext = name.split('.').pop().toLowerCase()
      const map = {
        md: '📝', txt: '📄', png: '🖼', jpg: '🖼', jpeg: '🖼',
        gif: '🖼', pdf: '📕', json: '⚙️', js: '⚙️', html: '🌐'
      }
      return map[ext] || '📄'
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
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: $spacing-xl;
}

/* Header */
.obs-header {
  position: relative;
  background: linear-gradient(135deg, #6C63FF 0%, #5A8DEE 100%);
  padding: $spacing-xl $page-padding $spacing-2xl;
  overflow: hidden;

  &__bg {
    position: absolute;
    top: 0; left: 0; right: 0; bottom: 0;
    pointer-events: none;
  }

  &__decor--circle {
    position: absolute;
    width: 300rpx;
    height: 300rpx;
    border-radius: 50%;
    background: #fff;
    opacity: 0.1;
    top: -100rpx;
    right: -60rpx;
  }

  &__content {
    position: relative;
    z-index: $z-base;
  }

  &__title {
    font-size: $font-h1;
    color: #fff;
    font-weight: $font-weight-bold;
    display: block;
  }
}

.obs-breadcrumb {
  margin-top: $spacing-xs;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.obs-crumb {
  font-size: $font-caption;
  color: rgba(255, 255, 255, 0.85);

  &:active {
    opacity: 0.7;
  }
}

.obs-crumb-sep {
  font-size: $font-caption;
  color: rgba(255, 255, 255, 0.5);
  margin: 0 4rpx;
}

/* Search */
.obs-search {
  margin: -$spacing-lg $page-padding 0;
  position: relative;
  z-index: $z-card;
}

/* List */
.obs-list {
  padding: $spacing-md $page-padding;
}

.obs-section-title {
  font-size: $font-caption;
  color: $text-tertiary;
  margin-bottom: $spacing-sm;
}

.obs-item {
  display: flex;
  align-items: center;
  background: $bg-card;
  border-radius: $radius-md;
  box-shadow: $shadow-xs;
  padding: $spacing-md;
  margin-bottom: $spacing-sm;

  &:active {
    opacity: 0.7;
  }

  &__icon {
    font-size: 40rpx;
    margin-right: $spacing-md;
    flex-shrink: 0;
  }

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__name {
    font-size: $font-body;
    color: $text-primary;
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__size {
    font-size: $font-mini;
    color: $text-tertiary;
  }

  &__match {
    font-size: $font-mini;
    color: $primary;
    display: block;
    margin-top: 2rpx;
  }

  &__arrow {
    font-size: 36rpx;
    color: $text-tertiary;
    margin-left: $spacing-sm;
  }
}

.obs-loading, .obs-empty {
  text-align: center;
  padding: 120rpx 0;
  font-size: $font-body;
  color: $text-tertiary;
}

.obs-empty {
  &__emoji {
    font-size: 96rpx;
    display: block;
    margin-bottom: $spacing-sm;
    opacity: 0.5;
  }

  &__text {
    font-size: $font-body;
    color: $text-tertiary;
  }
}

.safe-bottom {
  height: env(safe-area-inset-bottom);
}
</style>
