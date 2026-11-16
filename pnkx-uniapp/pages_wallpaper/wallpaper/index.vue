<!--
 * @File: index
 * @Author: PHY
 * @Description: 壁纸列表页（免登录，按文件夹查看，支持批量打包下载）
-->
<template>
  <view class="wallpaper-page">
    <!-- #ifdef MP-WEIXIN -->
    <page-container
      :show="backGuardVisible"
      :overlay="false"
      @beforeleave="handleBackGuardBeforeLeave"
      @afterleave="resetBackGuard"
    />
    <!-- #endif -->

    <!-- 顶部统计 -->
    <view class="wp-header">
      <view class="header-top-row">
        <view class="header-subtitle-line">
          <uni-icons type="folder" size="14" color="#6B7B8D" />
          <text class="header-subtitle">{{ headerSubtitle }}</text>
        </view>
        <!-- 登录态徽标：点击跳壁纸应用专属「我的」页；未登录跳登录页 -->
        <user-badge theme="light" to="/pages_wallpaper/wallpaper/mine" />
      </view>
      <view class="wp-stats">
        <view class="stat-card">
          <text class="stat-value">{{ wallpaperList.length }}</text>
          <text class="stat-label">本页壁纸</text>
        </view>
        <view class="stat-card">
          <text class="stat-value">{{ folderList.length }}</text>
          <text class="stat-label">子文件夹</text>
        </view>
        <!-- 批量下载入口：仅在二级（folderPath 非空）且有壁纸时显示，再次点击退出选择 -->
        <view
          v-if="folderPath.length > 0 && wallpaperList.length > 0"
          class="batch-btn"
          :class="{ active: selectMode }"
          @click="toggleSelectMode"
        >
          <uni-icons type="download" size="16" :color="selectMode ? '#fff' : '#38BDF8'" />
          <text class="batch-btn-text">{{ selectMode ? '退出选择' : '批量下载' }}</text>
        </view>
      </view>
    </view>

    <!-- 搜索栏：按关键字全局搜索壁纸或文件夹 -->
    <view class="search-bar" v-if="!selectMode">
      <view class="search-input-wrap">
        <uni-icons class="search-icon" type="search" size="16" color="#9BA8B7" />
        <input
          class="search-input"
          type="text"
          v-model="keyword"
          :placeholder="searchPlaceholder"
          placeholder-class="search-placeholder"
          confirm-type="search"
          @confirm="onSearch"
          @input="onSearchInput"
        />
        <view
          v-if="keyword"
          class="search-clear"
          @click="clearSearch"
        >
          <uni-icons type="clear" size="16" color="#9BA8B7" />
        </view>
      </view>
    </view>

    <!-- 工具栏（面包屑） -->
    <view class="toolbar">
      <scroll-view class="breadcrumb" scroll-x v-if="folderPath.length > 0 && !searchMode">
        <view class="breadcrumb-inner">
          <view class="breadcrumb-item root" @click="navigateToFolder(0, -1)">
            <uni-icons type="home" size="14" color="#6B7B8D" />
            <text class="breadcrumb-text">全部</text>
          </view>
          <view v-for="(folder, index) in folderPath" :key="index" class="breadcrumb-content">
            <text class="breadcrumb-separator">/</text>
            <view class="breadcrumb-item" @click="navigateToFolder(folder.id, index)">
              <text class="breadcrumb-text">{{ folder.name }}</text>
            </view>
          </view>
        </view>
      </scroll-view>
      <view class="breadcrumb-placeholder" v-else>
        <uni-icons :type="searchMode ? 'search' : 'folder'" size="14" color="#9BA8B7" />
        <text>{{ searchMode ? ('「' + keyword + '」的搜索结果') : '全部壁纸' }}</text>
      </view>
    </view>

    <!-- 内容区域 -->
    <scroll-view
      class="content-scroll"
      scroll-y
      @scrolltolower="loadMore"
      refresher-enabled
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onRefresh"
    >
      <!-- 文件夹列表（多行网格） -->
      <view class="folder-section" v-if="folderList.length > 0">
        <view class="section-header">
          <text class="section-title">文件夹</text>
        </view>
        <view class="folder-grid">
          <view
            v-for="folder in folderList"
            :key="folder.id"
            class="folder-card"
            @click="enterFolder(folder)"
          >
            <view class="folder-card-cover">
              <image
                v-if="folder.cover"
                class="cover-img"
                :src="folder.cover"
                mode="aspectFit"
                :lazy-load="true"
              />
              <view v-else class="cover-placeholder">
                <text class="cover-placeholder-text">{{ folder.name }}</text>
              </view>
              <!-- 整夹下载按钮：仅二级文件夹卡片显示（一级不显示） -->
              <view
                v-if="folderPath.length > 0 && folder.wallpaperCount"
                class="folder-download"
                @click.stop="downloadFolder(folder)"
              >
                <uni-icons type="download" size="14" color="#fff" />
              </view>
            </view>
            <view class="folder-card-body">
              <view class="folder-card-name">{{ folder.name }}</view>
              <view class="folder-card-count">{{ folder.wallpaperCount || 0 }} 张</view>
            </view>
          </view>
        </view>
      </view>

      <!-- 壁纸网格 -->
      <view class="wallpaper-section" :class="{ 'select-padding': selectMode }">
        <view class="section-header" v-if="wallpaperList.length > 0">
          <text class="section-title">壁纸</text>
          <text class="section-extra">
            {{ selectMode ? '已选 ' + selectedCount + ' 张' : wallpaperList.length + ' 张' }}
          </text>
        </view>

        <!-- 排序切换 -->
        <view class="sort-bar" v-if="wallpaperList.length > 0 && !selectMode">
          <view
            v-for="opt in sortOptions"
            :key="opt.value"
            class="sort-item"
            :class="{ active: currentSort === opt.value }"
            @click="changeSort(opt.value)"
          >
            <text>{{ opt.label }}</text>
          </view>
        </view>

        <view class="wallpaper-grid" v-if="wallpaperList.length > 0">
          <view
            v-for="item in wallpaperList"
            :key="item.id"
            class="wallpaper-item"
            :class="{ 'is-selected': isSelected(item.id) }"
            @click="onWallpaperClick(item)"
            @longpress="onWallpaperLongPress(item)"
          >
            <image
              class="wallpaper-img"
              :src="item.thumbnail || item.url"
              mode="aspectFit"
              :lazy-load="true"
            />
            <!-- 底部信息栏：名称 + 点赞数 -->
            <view class="card-footer">
              <text class="card-name">{{ item.name || '' }}</text>
              <view class="card-like" v-if="!selectMode" @click.stop="toggleLikeFromList(item)">
                <text class="wallpaper-action-icon" :class="{ active: item.likeCount > 0 }">{{ item.likeCount > 0 ? '♥' : '♡' }}</text>
                <text class="card-like-count">{{ item.likeCount || 0 }}</text>
              </view>
            </view>
            <!-- 选择勾选框 -->
            <view v-if="selectMode" class="select-mask">
              <view class="select-check" :class="{ checked: isSelected(item.id) }">
                <uni-icons v-if="isSelected(item.id)" type="checkmarkempty" size="18" color="#fff" />
              </view>
            </view>
          </view>
        </view>

        <!-- 空状态 -->
        <view class="empty-state" v-if="wallpaperList.length === 0 && folderList.length === 0 && !loading">
          <view class="empty-icon">
            <uni-icons type="image" size="42" color="#38BDF8" />
          </view>
          <text class="empty-text">{{ searchMode ? '没有找到相关内容' : '这里还没有壁纸' }}</text>
          <text class="empty-hint">{{ searchMode ? '换个关键词试试吧' : '下拉刷新试试，或换个文件夹看看' }}</text>
        </view>

        <!-- 加载状态 -->
        <uni-load-more :status="loadStatus" />
      </view>
    </scroll-view>

    <!-- 底部批量操作栏 -->
    <!-- #ifdef MP-WEIXIN -->
    <view class="weapp-share-bar" v-if="weappShareFilePath">
      <view class="weapp-share-info">
        <text class="weapp-share-title">ZIP 已准备好</text>
        <text class="weapp-share-desc">{{ weappShareFileName }}</text>
      </view>
      <view class="weapp-share-actions">
        <text class="weapp-share-cancel" @click="clearPreparedZip">取消</text>
        <view class="weapp-share-btn" @click="sharePreparedZipInWeapp">
          <uni-icons type="redo" size="14" color="#fff" />
          <text>转发ZIP</text>
        </view>
      </view>
    </view>
    <!-- #endif -->

    <view class="batch-bar" v-if="selectMode">
      <view class="batch-bar-left" @click="toggleSelectAll">
        <view class="select-check" :class="{ checked: isAllSelected }">
          <uni-icons v-if="isAllSelected" type="checkmarkempty" size="16" color="#fff" />
        </view>
        <text class="batch-bar-text">{{ isAllSelected ? '取消全选' : '全选' }}</text>
      </view>
      <view class="batch-bar-right">
        <text class="batch-bar-cancel" @click="exitSelectMode">取消</text>
        <view class="batch-bar-download" :class="{ disabled: selectedCount === 0 }" @click="downloadSelected">
          <uni-icons type="download" size="14" color="#fff" />
          <text class="batch-bar-download-text">下载{{ selectedCount > 0 ? '(' + selectedCount + ')' : '' }}</text>
        </view>
      </view>
    </view>

    <!-- 下载次数用完引导分享弹窗 -->
    <download-limit-dialog
      :show="showLimitDialog"
      :reward="shareRewardCount"
      :share-times-used="shareTimesUsed"
      :share-times-limit="shareTimesLimit"
      :reward-claiming="rewardClaiming"
      @share="handleShareReward"
      @close="showLimitDialog = false"
    />
  </view>
</template>

<script>
import {
  listWallpaper,
  listWallpaperFolder,
  getWallpaperZipUrl,
  likeWallpaper,
  getDownloadStatus,
  shareReward
} from '../api/px/life/wallpaper'
import { toLogin } from '@/permission'
import { getToken } from '@/utils/auth'

function normalizeZipFileName(name) {
  const raw = String(name || '').trim()
  if (!raw) return 'wallpapers.zip'
  return raw.toLowerCase().endsWith('.zip') ? raw : raw + '.zip'
}

function buildWeappZipFilePath(fileName, wxApi, uniApi) {
  let basePath = ''
  if (wxApi && wxApi.env && wxApi.env.USER_DATA_PATH) {
    basePath = wxApi.env.USER_DATA_PATH
  } else if (uniApi && uniApi.env && uniApi.env.USER_DATA_PATH) {
    basePath = uniApi.env.USER_DATA_PATH
  }
  return basePath ? basePath + '/' + normalizeZipFileName(fileName) : normalizeZipFileName(fileName)
}

function isUserCancel(message) {
  const msg = String(message || '')
  return msg.indexOf('cancel') !== -1 || msg.indexOf('取消') !== -1
}

export default {
  name: 'WallpaperIndex',
  data() {
    return {
      // 当前文件夹ID（0为根目录）
      currentFolderId: 0,
      // 面包屑路径
      folderPath: [],
      // 文件夹列表
      folderList: [],
      // 壁纸列表
      wallpaperList: [],
      // 壁纸分页参数
      queryParams: {
        pageNum: 1,
        pageSize: 20
      },
      // 文件夹分页（与壁纸各自分页，滚动加载时先翻完文件夹页再翻壁纸页）
      folderPageNum: 1,
      folderHasMore: true,
      // 加载状态
      loading: false,
      isRefreshing: false,
      hasMore: true,
      loadStatus: 'more',
      initialized: false,
      // 排序（默认按最热）
      currentSort: 'like',
      sortOptions: [
        { label: '最热', value: 'like' },
        { label: '最新', value: 'time' },
        { label: '名称', value: 'name' }
      ],
      // ===== 批量下载相关 =====
      // 是否处于选择模式
      selectMode: false,
      // 选中的壁纸 id 集合
      selectedIds: [],
      // 是否正在打包下载
      zipping: false,
      // 微信小程序已准备好的 ZIP 转发文件
      weappShareFilePath: '',
      weappShareFileName: '',
      // 从详情页返回时不强制刷新整页图片网格
      skipNextShowRefresh: false,
      // 微信小程序物理返回拦截
      backGuardVisible: true,
      backGuardLeaving: false,
      // ===== 关键字搜索相关 =====
      // 搜索输入内容
      keyword: '',
      // 是否处于搜索态（全局搜索壁纸 + 文件夹）
      searchMode: false,
      // 输入防抖计时器
      searchTimer: null,
      // ===== 下载次数用完引导分享弹窗 =====
      // -1 表示未知（接口未返回前不拦截），0 表示已用完
      downloadRemaining: -1,
      showLimitDialog: false,
      shareRewardCount: 0,
      shareTimesUsed: 0,
      shareTimesLimit: 0,
      rewardClaiming: false
    }
  },
  computed: {
    headerSubtitle() {
      if (this.folderPath.length > 0) {
        return this.folderPath[this.folderPath.length - 1].name
      }
      return '精选高清壁纸，一键保存到相册'
    },
    searchPlaceholder() {
      return '搜索壁纸或文件夹'
    },
    selectedCount() {
      return this.selectedIds.length
    },
    isAllSelected() {
      return this.wallpaperList.length > 0 &&
        this.wallpaperList.every(item => this.selectedIds.includes(item.id))
    }
  },
  onLoad(options = {}) {
    if (options.folderId) {
      this.currentFolderId = Number(options.folderId) || 0
    }
    this.initialized = true
    this.loadData()
    // 拉取今日下载配额（供超限时本地同步拦截）
    this.refreshShareStatus()
  },
  onShow() {
    if (this.initialized) {
      if (this.skipNextShowRefresh) {
        this.skipNextShowRefresh = false
        return
      }
      this.refreshData()
      // 刷新下载配额（下载次数可能跨页变化）
      this.refreshShareStatus()
    }
  },
  // 开启「转发给朋友」（需同时存在，朋友圈分享才会生效）
  onShareAppMessage() {
    return {
      title: 'Pei你看雪 · 精选高清壁纸，一键保存到相册',
      path: '/pages_wallpaper/wallpaper/index'
    }
  },
  // 开启「分享到朋友圈」
  onShareTimeline() {
    return {
      title: 'Pei你看雪 · 精选高清壁纸，一键保存到相册'
    }
  },
  methods: {
    /**
     * 微信小程序物理返回：优先回到当前工具的上一层状态。
     */
    handleBackGuardBeforeLeave() {
      if (this.backGuardLeaving) return
      if (this.selectMode) {
        this.exitSelectMode()
        this.resetBackGuard()
        return
      }
      // 搜索态：物理返回优先退出搜索，回到浏览态
      if (this.searchMode) {
        this.exitSearch()
        this.resetBackGuard()
        return
      }
      if (this.weappShareFilePath) {
        this.clearPreparedZip()
        this.resetBackGuard()
        return
      }
      if (this.folderPath.length > 0) {
        this.navigateBackFolder()
        this.resetBackGuard()
        return
      }
      this.backGuardLeaving = true
      this.backGuardVisible = false
      const pages = typeof getCurrentPages === 'function' ? getCurrentPages() : []
      if (pages.length > 1) {
        setTimeout(() => {
          uni.navigateBack({ delta: 1 })
        }, 0)
      } else {
        setTimeout(() => {
          uni.reLaunch({ url: '/pages/tools' })
        }, 0)
      }
    },

    /** 重新挂起返回拦截容器 */
    resetBackGuard() {
      // #ifdef MP-WEIXIN
      if (this.backGuardLeaving) return
      this.backGuardVisible = false
      setTimeout(() => {
        this.backGuardVisible = true
      }, 50)
      // #endif
    },

    /** 返回上一级壁纸文件夹 */
    navigateBackFolder() {
      if (this.folderPath.length <= 1) {
        this.folderPath = []
        this.currentFolderId = 0
      } else {
        this.folderPath = this.folderPath.slice(0, this.folderPath.length - 1)
        this.currentFolderId = this.folderPath[this.folderPath.length - 1].id
      }
      this.refreshData()
    },

    /**
     * 加载数据
     */
    async loadData() {
      if (this.loading) return
      this.loading = true
      this.loadStatus = 'loading'
      try {
        await Promise.all([
          this.loadFolders(),
          this.loadWallpapers()
        ])
      } catch (error) {
        console.error('加载数据失败:', error)
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.loading = false
        this.isRefreshing = false
        this.loadStatus = (this.hasMore || this.folderHasMore) ? 'more' : 'noMore'
      }
    },

    /**
     * 加载文件夹列表（分页，只加载当前层级）
     * 搜索态：忽略层级，按 name 全局模糊匹配所有启用文件夹
     */
    async loadFolders(append = false) {
      const params = this.searchMode
        ? { name: this.keyword }
        : { parentId: this.currentFolderId }
      // 后端 startPage 默认 pageSize=10，必须显式传分页参数
      params.pageNum = this.folderPageNum
      params.pageSize = this.queryParams.pageSize
      const res = await listWallpaperFolder(params)
      const rows = res.rows || []
      this.folderList = append ? [...this.folderList, ...rows] : rows
      // 以 total 判断是否还有下一页；返回空页时兜底结束
      this.folderHasMore = rows.length > 0 && this.folderList.length < (Number(res.total) || 0)
    },

    /**
     * 加载壁纸列表
     * 搜索态：跨文件夹全局按 name 模糊匹配（不传 folder，避免后端按空串过滤）
     */
    async loadWallpapers() {
      const params = {
        pageNum: this.queryParams.pageNum,
        pageSize: this.queryParams.pageSize,
        sort: this.currentSort
      }
      if (this.searchMode) {
        // 全局搜索：带 name，不带 folder
        params.name = this.keyword
      } else {
        params.folder = this.currentFolderId || ''
      }
      const res = await listWallpaper(params)
      const rows = res.rows || []
      if (this.queryParams.pageNum === 1) {
        this.wallpaperList = rows
      } else {
        this.wallpaperList = [...this.wallpaperList, ...rows]
      }
      this.hasMore = rows.length >= this.queryParams.pageSize
    },

    /**
     * 刷新数据
     */
    refreshData() {
      this.resetPaging()
      this.loadData()
    },

    /**
     * 重置分页参数（文件夹 + 壁纸都回到第一页）
     */
    resetPaging() {
      this.queryParams.pageNum = 1
      this.folderPageNum = 1
      this.folderHasMore = true
      this.hasMore = true
    },

    /**
     * 切换排序方式
     */
    changeSort(sort) {
      if (this.currentSort === sort) return
      this.currentSort = sort
      this.resetPaging()
      this.loadData()
    },

    // ===== 关键字搜索 =====

    /**
     * 执行搜索：进入搜索态，忽略当前文件夹，全局匹配壁纸 + 文件夹
     */
    onSearch() {
      const kw = (this.keyword || '').trim()
      if (!kw) {
        // 输入为空则回到浏览态
        this.exitSearch()
        return
      }
      this.searchMode = true
      // 全局搜索无层级概念，清空文件夹上下文
      this.currentFolderId = 0
      this.folderPath = []
      this.resetPaging()
      this.loadData()
    },

    /**
     * 输入时防抖触发搜索（400ms），避免每次按键都请求
     */
    onSearchInput() {
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
      }
      // 输入清空时立即回到浏览态，无需等待防抖
      if (!(this.keyword || '').trim()) {
        this.exitSearch()
        return
      }
      this.searchTimer = setTimeout(() => {
        this.onSearch()
      }, 400)
    },

    /**
     * 清除搜索内容并回到浏览态
     */
    clearSearch() {
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
      }
      this.keyword = ''
      this.exitSearch()
    },

    /**
     * 退出搜索态，回到根目录浏览
     */
    exitSearch() {
      if (!this.searchMode && this.currentFolderId === 0) {
        return
      }
      this.searchMode = false
      this.currentFolderId = 0
      this.folderPath = []
      this.resetPaging()
      this.loadData()
    },

    /**
     * 下拉刷新
     */
    onRefresh() {
      this.isRefreshing = true
      this.refreshData()
    },

    /**
     * 加载更多：先翻完文件夹分页，再翻壁纸分页（与页面渲染顺序一致）
     */
    async loadMore() {
      if (this.loading) return
      if (!this.folderHasMore && !this.hasMore) return
      this.loading = true
      try {
        if (this.folderHasMore) {
          this.folderPageNum++
          await this.loadFolders(true)
        } else {
          this.queryParams.pageNum++
          await this.loadWallpapers()
        }
      } catch (error) {
        console.error('加载更多失败:', error)
      } finally {
        this.loading = false
        this.loadStatus = (this.hasMore || this.folderHasMore) ? 'more' : 'noMore'
      }
    },

    /**
     * 进入文件夹
     * 搜索态下点击：先退出搜索回到浏览态，再进入该文件夹
     */
    enterFolder(folder) {
      if (this.searchMode) {
        this.searchMode = false
        this.currentFolderId = 0
        this.folderPath = []
      }
      this.navigateToFolder(folder)
    },

    /**
     * 导航到指定文件夹
     */
    navigateToFolder(target, pathIndex = -1) {
      const folderId = typeof target === 'object' ? target.id : target
      if (folderId === 0) {
        this.folderPath = []
        this.currentFolderId = 0
        this.refreshData()
        return
      }
      if (pathIndex >= 0 && pathIndex < this.folderPath.length) {
        this.folderPath = this.folderPath.slice(0, pathIndex + 1)
        this.currentFolderId = folderId
        this.refreshData()
        return
      }
      const folder = typeof target === 'object' ? target : this.folderList.find(f => f.id === target)
      this.currentFolderId = folderId
      if (folder) {
        this.folderPath.push({ id: folder.id, name: folder.name })
      }
      this.refreshData()
    },

    /**
     * 壁纸点击：选择模式下切换选中，否则查看详情
     */
    onWallpaperClick(item) {
      if (this.selectMode) {
        this.toggleSelect(item.id)
      } else {
        this.viewWallpaper(item)
      }
    },

    /**
     * 壁纸长按：进入选择模式并选中当前
     */
    onWallpaperLongPress(item) {
      if (!this.selectMode) {
        this.enterSelectMode()
      }
      if (!this.selectedIds.includes(item.id)) {
        this.selectedIds.push(item.id)
      }
    },

    /**
     * 查看壁纸详情
     * 将当前壁纸列表和点击位置传给详情页，用于左右滑动切换
     */
    viewWallpaper(item) {
      const index = this.wallpaperList.findIndex(w => w.id === item.id)
      // 通过 globalData 传递列表上下文（URL 无法承载图片数据）
      getApp().globalData.__wallpaperContext = {
        list: this.wallpaperList,
        index: index < 0 ? 0 : index
      }
      this.skipNextShowRefresh = true
      this.$tab.navigateTo(`/pages_wallpaper/wallpaper/detail?id=${item.id}`)
    },

    /**
     * 列表页直接点赞
     */
    async toggleLikeFromList(item) {
      const token = getToken()
      if (!token) {
        uni.showModal({
          title: '提示',
          content: '点赞需要先登录，是否前往登录？',
          confirmText: '去登录',
          success: (res) => {
            if (res.confirm) {
              toLogin()
            }
          }
        })
        return
      }
      try {
        const res = await likeWallpaper(item.id)
        // 更新本地数据（统一用后端返回的 liked 布尔值，避免依赖 msg 文案）
        if (res.code === 200) {
          if (res.liked) {
            item.likeCount = (item.likeCount || 0) + 1
          } else {
            item.likeCount = Math.max(0, (item.likeCount || 0) - 1)
          }
          uni.showToast({ title: res.msg, icon: 'none', duration: 800 })
        } else {
          uni.showToast({ title: res.msg || '操作失败', icon: 'none' })
        }
      } catch (e) {
        console.error('点赞失败', e)
        uni.showToast({ title: '网络异常', icon: 'none' })
      }
    },

    // ===== 批量下载 =====

    /** 切换选择模式开关（顶部按钮 / 长按均可触发） */
    toggleSelectMode() {
      if (this.selectMode) {
        this.exitSelectMode()
      } else {
        this.enterSelectMode()
      }
    },

    /** 进入选择模式 */
    enterSelectMode() {
      this.selectMode = true
      this.selectedIds = []
    },

    /** 退出选择模式 */
    exitSelectMode() {
      this.selectMode = false
      this.selectedIds = []
    },

    /** 切换单个壁纸选中状态 */
    toggleSelect(id) {
      const idx = this.selectedIds.indexOf(id)
      if (idx >= 0) {
        this.selectedIds.splice(idx, 1)
      } else {
        this.selectedIds.push(id)
      }
    },

    /** 判断是否选中 */
    isSelected(id) {
      return this.selectedIds.includes(id)
    },

    /** 全选 / 取消全选 */
    toggleSelectAll() {
      if (this.isAllSelected) {
        this.selectedIds = []
      } else {
        this.selectedIds = this.wallpaperList.map(item => item.id)
      }
    },

    /**
     * 批量下载选中的壁纸（ZIP 打包）
     */
    downloadSelected() {
      if (this.selectedCount === 0 || this.zipping) return
      this.downloadZip({ ids: [...this.selectedIds] }, '已选壁纸')
    },

    /**
     * 下载整个文件夹（含子文件夹）
     */
    downloadFolder(folder) {
      if (!folder || !folder.wallpaperCount) {
        uni.showToast({ title: '该文件夹没有壁纸', icon: 'none' })
        return
      }
      uni.showModal({
        title: '打包下载',
        content: `将「${folder.name}」下的 ${folder.wallpaperCount} 张壁纸打包为 ZIP 下载？`,
        confirmText: '下载',
        success: (res) => {
          if (res.confirm) {
            this.downloadZip({ folderId: folder.id }, folder.name)
          }
        }
      })
    },

    /**
     * 执行 ZIP 打包下载
     * @param params {ids?: number[], folderId?: number}
     * @param name zip 文件名
     */
    downloadZip(params, name) {
      if (this.zipping) return
      // 下载前主动校验剩余次数：用完则直接弹引导分享弹窗，不发起下载
      this.checkBeforeDownload().then(canDownload => {
        if (!canDownload) return
        this.proceedDownloadZip(params, name)
      })
    },

    /**
     * 下载前校验：刷新今日下载状态，若剩余次数为 0 则弹引导分享弹窗并返回 false。
     * 未登录或校验失败（网络异常等）放行，交由后端 429 兜底。
     */
    checkBeforeDownload() {
      return new Promise(resolve => {
        if (!getToken()) {
          resolve(true)
          return
        }
        // 纯同步判断：用已缓存的剩余次数，保证点击立即有反馈。
        // downloadRemaining 默认 -1（未知），仅当明确为 0 时才拦截。
        if (this.downloadRemaining === 0) {
          this.refreshShareStatus()
          this.showLimitDialog = true
          resolve(false)
          return
        }
        resolve(true)
      })
    },

    /**
     * 刷新今日下载配额与分享奖励状态（供弹窗/页面同步）
     */
    refreshShareStatus() {
      if (!getToken()) return
      getDownloadStatus().then(res => {
        if (res.code === 200) {
          this.downloadRemaining = res.remaining != null ? res.remaining : 0
          this.shareRewardCount = res.reward || 0
          this.shareTimesUsed = res.shareTimesUsed || 0
          this.shareTimesLimit = res.shareTimesLimit || 0
        }
      }).catch(() => {})
    },

    /**
     * 通过校验后真正发起 ZIP 打包下载
     */
    proceedDownloadZip(params, name) {
      if (this.zipping) return
      this.zipping = true
      this.clearPreparedZip()
      const url = getWallpaperZipUrl({ ...params, name })
      uni.showLoading({ title: '打包中...', mask: true })

      const task = uni.downloadFile({
        url,
        header: this.buildAuthHeader(),
        success: (res) => {
          uni.hideLoading()
          // 429：下载达上限，拉取自定义提醒文案并弹窗
          if (res.statusCode === 429) {
            this.zipping = false
            this.checkZipDownloadRemaining()
            return
          }
          if (res.statusCode !== 200) {
            uni.showToast({ title: '打包失败，请重试', icon: 'none' })
            return
          }
          this.saveZipFile(res.tempFilePath, name)
        },
        fail: () => {
          uni.hideLoading()
          // 兜底：下载失败可能是超限被拒（429 在小程序可能静默失败），
          // 主动检查配额，若已用完则弹引导分享弹窗
          if (getToken()) {
            getDownloadStatus().then(res => {
              if (res.code === 200 && (res.remaining != null ? res.remaining : 1) <= 0) {
                this.downloadRemaining = res.remaining != null ? res.remaining : 0
                this.shareRewardCount = res.reward || 0
                this.shareTimesUsed = res.shareTimesUsed || 0
                this.shareTimesLimit = res.shareTimesLimit || 0
                this.showLimitDialog = true
              } else {
                uni.showToast({ title: '下载失败，请重试', icon: 'none' })
              }
            }).catch(() => {
              uni.showToast({ title: '下载失败，请重试', icon: 'none' })
            })
          } else {
            uni.showToast({ title: '下载失败，请重试', icon: 'none' })
          }
        },
        complete: () => {
          this.zipping = false
        }
      })
      // 大包可能较慢，显示进度。流式 ZIP 总大小未知，
      // 部分平台（H5）progress 为 undefined，按已下载量展示，避免出现 Infinity%
      if (task && task.onProgressUpdate) {
        task.onProgressUpdate((p) => {
          let title = '下载中...'
          if (typeof p.progress === 'number' && isFinite(p.progress)) {
            title = `下载中 ${Math.min(p.progress, 99)}%`
          } else if (p.totalBytesWritten) {
            const kb = Math.round(p.totalBytesWritten / 1024)
            title = `下载中 ${kb} KB`
          }
          uni.showLoading({ title, mask: true })
        })
      }
    },

    /**
     * 构造下载请求鉴权头：登录用户携带 token，后端据此识别下载人并记录。
     */
    buildAuthHeader() {
      const token = getToken()
      return token ? { Authorization: 'Bearer ' + token } : {}
    },

    /**
     * 保存 ZIP 文件
     * H5：触发浏览器下载
     * App：保存到本地文件系统
     * 微信小程序：转发到聊天（文件传输助手），这是小程序内让用户真正拿到
     *             zip 文件的官方唯一方式（saveFile 存的是沙箱，用户不可见）。
     */
    saveZipFile(tempPath, name) {
      const fileName = normalizeZipFileName(name)
      // #ifdef H5
      // H5：通过 a 标签触发浏览器下载
      try {
        const a = document.createElement('a')
        a.href = tempPath
        a.download = fileName
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
        uni.showToast({ title: '已开始下载', icon: 'success' })
        this.checkZipDownloadRemaining()
      } catch (e) {
        uni.showToast({ title: '下载失败', icon: 'none' })
      }
      // #endif
      // #ifdef APP-PLUS
      // App：保存到本地文件系统
      uni.saveFile({
        tempFilePath: tempPath,
        success: (res) => {
          uni.showToast({ title: '已保存：' + fileName, icon: 'success' })
          this.exitSelectMode()
          this.checkZipDownloadRemaining()
        },
        fail: () => {
          uni.showModal({
            title: '下载完成',
            content: 'ZIP 已下载到：' + tempPath,
            showCancel: false
          })
        }
      })
      // #endif
      // #ifdef MP-WEIXIN
      // 微信小程序：先把文件准备到本地沙箱，再让用户点一次“转发ZIP”。
      // shareFileMessage 必须由用户点击触发，不能在下载回调里自动调用。
      this.shareZipInWeapp(tempPath, fileName)
      // #endif
    },

    /**
     * 微信小程序：准备 zip 转发文件
     */
    shareZipInWeapp(tempPath, fileName) {
      uni.saveFile({
        tempFilePath: tempPath,
        success: (saveRes) => {
          const wxApi = typeof wx !== 'undefined' ? wx : null
          const dotZipPath = buildWeappZipFilePath(fileName, wxApi, uni)
          const fs = uni.getFileSystemManager && uni.getFileSystemManager()
          if (fs && fs.copyFileSync) {
            try {
              fs.copyFileSync(saveRes.savedFilePath, dotZipPath)
              this.setPreparedZip(dotZipPath, fileName)
              return
            } catch (e) {
              console.error('copy zip fail:', e)
            }
          }
          this.setPreparedZip(saveRes.savedFilePath, fileName)
        },
        fail: (err) => {
          console.error('saveFile fail:', err && err.errMsg)
          uni.showToast({ title: '保存失败：' + ((err && err.errMsg) || ''), icon: 'none', duration: 3000 })
        }
      })
    },

    /**
     * 标记已准备好待转发文件
     */
    setPreparedZip(filePath, fileName) {
      this.weappShareFilePath = filePath
      this.weappShareFileName = fileName
      this.exitSelectMode()
      uni.showToast({ title: 'ZIP已准备好，请点击转发', icon: 'none', duration: 2200 })
    },

    /** 清除待转发 ZIP */
    clearPreparedZip() {
      this.weappShareFilePath = ''
      this.weappShareFileName = ''
    },

    /**
     * 执行微信小程序文件转发。该方法必须直接由点击触发。
     */
    sharePreparedZipInWeapp() {
      const wxApi = typeof wx !== 'undefined' ? wx : null
      if (!wxApi || !wxApi.shareFileMessage) {
        uni.showModal({
          title: '下载完成',
          content: '当前微信版本不支持文件转发，请升级微信后再试。',
          showCancel: false
        })
        return
      }
      wxApi.shareFileMessage({
        filePath: this.weappShareFilePath,
        fileName: this.weappShareFileName,
        success: () => {
          uni.showToast({ title: '已转发，请在聊天中下载', icon: 'success' })
          this.clearPreparedZip()
          this.checkZipDownloadRemaining()
        },
        fail: (err) => {
          console.error('shareFileMessage fail:', err && err.errMsg)
          // 用户取消转发不算错误，不弹失败提示
          const msg = (err && err.errMsg) || ''
          if (!isUserCancel(msg)) {
            uni.showToast({ title: '转发失败：' + msg, icon: 'none', duration: 3000 })
          }
        }
      })
    },

    /**
     * ZIP 下载完成后，检查剩余次数，用完则弹「引导分享」弹窗
     */
    checkZipDownloadRemaining() {
      if (!getToken()) return
      getDownloadStatus().then(res => {
        if (res.code === 200) {
          this.downloadRemaining = res.remaining != null ? res.remaining : 0
          this.shareRewardCount = res.reward || 0
          this.shareTimesUsed = res.shareTimesUsed || 0
          this.shareTimesLimit = res.shareTimesLimit || 0
          if (this.downloadRemaining <= 0) {
            setTimeout(() => {
              this.showLimitDialog = true
            }, 1500)
          }
        }
      }).catch(() => {})
    },
    /**
     * 点击弹窗「分享小程序」按钮：调用后端发放奖励。
     * open-type=share 会同步唤起分享面板（由 onShareAppMessage 配置分享内容）。
     */
    handleShareReward() {
      if (this.rewardClaiming) return
      this.rewardClaiming = true
      shareReward().then(res => {
        if (res.code === 200) {
          const n = res.rewarded || this.shareRewardCount
          this.shareTimesUsed = res.shareTimesUsed || 0
          const remaining = res.remaining != null ? res.remaining : 0
          uni.showToast({ title: `分享成功，+${n} 次下载`, icon: 'none' })
          if (remaining > 0) {
            this.showLimitDialog = false
          }
        }
      }).catch(() => {
        // request.js 已 toast 错误文案；刷新配额同步按钮状态
        getDownloadStatus().then(res => {
          if (res.code === 200) {
            this.shareTimesUsed = res.shareTimesUsed || 0
            this.shareRewardCount = res.reward || 0
          }
        }).catch(() => {})
      }).finally(() => {
        this.rewardClaiming = false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.wallpaper-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: linear-gradient(180deg, #E6F6FF 0%, #F7F8FB 360rpx, #F7F8FB 100%);

  .wp-header {
    position: relative;
    padding: 24rpx 28rpx 8rpx;
    overflow: hidden;

    .header-top-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .header-subtitle-line {
      display: flex;
      align-items: center;

      .header-subtitle {
        margin-left: 8rpx;
        font-size: $font-caption;
        color: $text-secondary;
      }
    }

    .wp-stats {
      display: flex;
      margin-top: 20rpx;

      .stat-card {
        flex: 1;
        display: flex;
        flex-direction: column;
        padding: 20rpx 22rpx;
        border-radius: $radius-lg;
        background-color: rgba(255, 255, 255, 0.86);
        border: 1rpx solid rgba(255, 255, 255, 0.7);
        box-shadow: $shadow-sm;

        & + .stat-card {
          margin-left: 16rpx;
        }

        .stat-value {
          font-size: 34rpx;
          line-height: 1.2;
          color: #172033;
          font-weight: $font-weight-bold;
        }

        .stat-label {
          margin-top: 6rpx;
          font-size: $font-caption;
          color: $text-tertiary;
        }
      }

      // 批量下载入口按钮
      .batch-btn {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 6rpx;
        margin-left: 16rpx;
        padding: 0 26rpx;
        height: 96rpx;
        border-radius: $radius-lg;
        background-color: rgba(56, 189, 248, 0.12);
        border: 1rpx solid rgba(56, 189, 248, 0.3);

        .batch-btn-text {
          font-size: $font-caption;
          color: #38BDF8;
          font-weight: $font-weight-semibold;
          white-space: nowrap;
        }

        &:active {
          opacity: 0.8;
        }
      }
    }
  }

  // 搜索栏
  .search-bar {
    margin: 4rpx 24rpx 16rpx;

    .search-input-wrap {
      position: relative;
      display: flex;
      align-items: center;
      padding: 0 20rpx;
      height: 72rpx;
      border-radius: $radius-full;
      background-color: $bg-card;
      box-shadow: $shadow-sm;
    }

    .search-icon {
      margin-right: 10rpx;
      flex-shrink: 0;
    }

    .search-input {
      flex: 1;
      height: 72rpx;
      font-size: $font-body;
      color: $text-primary;
    }

    .search-placeholder {
      color: $text-tertiary;
      font-size: $font-body;
    }

    .search-clear {
      flex-shrink: 0;
      padding: 8rpx;
      margin-left: 8rpx;
      display: flex;
      align-items: center;
      justify-content: center;

      &:active {
        opacity: 0.6;
      }
    }
  }

  .toolbar {
    margin: 0 24rpx 18rpx;
    padding: 8rpx 10rpx 14rpx;
    border-radius: $radius-xl;
    background-color: $bg-card;
    box-shadow: $shadow-card;
  }

  .breadcrumb {
    width: 100%;
    margin-top: 10rpx;
    white-space: nowrap;

    .breadcrumb-inner {
      display: inline-flex;
      align-items: center;
      padding: 0 12rpx 2rpx;
    }

    .breadcrumb-content {
      display: flex;
      align-items: center;
    }
  }

  .breadcrumb-placeholder {
    display: flex;
    align-items: center;
    padding: 2rpx 16rpx 0;
    font-size: $font-caption;
    color: $text-tertiary;

    text {
      margin-left: 8rpx;
    }
  }

  .breadcrumb-item {
    display: flex;
    align-items: center;
    height: 48rpx;
    padding: 0 14rpx;
    border-radius: $radius-full;
    color: $text-secondary;
    font-size: $font-caption;
    background-color: $gray-50;
    white-space: nowrap;

    &.root .breadcrumb-text {
      margin-left: 6rpx;
    }

    &:active {
      color: $wallpaper;
      background-color: rgba($wallpaper, 0.08);
    }
  }

  .breadcrumb-separator {
    padding: 0 6rpx;
    color: $text-disabled;
    font-size: $font-caption;
  }

  .content-scroll {
    flex: 1;
    height: 0;
  }

  .section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 6rpx 4rpx 18rpx;

    .section-title {
      font-size: $font-h3;
      font-weight: $font-weight-semibold;
      color: $text-primary;
    }

    .section-extra {
      font-size: $font-caption;
      color: $wallpaper;
    }
  }

  .folder-section {
    margin: 0 24rpx 24rpx;

    .folder-grid {
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      padding-bottom: 4rpx;

      .folder-card {
        width: 48.5%;
        margin-bottom: 18rpx;
        border-radius: $radius-lg;
        background-color: $bg-card;
        box-shadow: $shadow-card;
        overflow: hidden;

        .folder-card-cover {
          position: relative;
          width: 100%;
          height: 300rpx;
          background-color: #E6F6FF;

          .cover-img {
            width: 100%;
            height: 100%;
          }

          // 整夹下载按钮（右上角浮层）
          .folder-download {
            position: absolute;
            top: 12rpx;
            right: 12rpx;
            width: 48rpx;
            height: 48rpx;
            border-radius: $radius-full;
            background-color: rgba(0, 0, 0, 0.45);
            display: flex;
            align-items: center;
            justify-content: center;

            &:active {
              background-color: rgba(0, 0, 0, 0.6);
            }
          }

          .cover-placeholder {
            width: 100%;
            height: 100%;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 0 16rpx;

            .cover-placeholder-text {
              font-size: 30rpx;
              font-weight: 600;
              color: #38BDF8;
              text-align: center;
              word-break: break-all;
              line-height: 1.3;
            }
          }
        }

        .folder-card-body {
          padding: 18rpx 22rpx 20rpx;
        }

        .folder-card-name {
          font-size: $font-body;
          color: $text-primary;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .folder-card-count {
          margin-top: 10rpx;
          font-size: $font-caption;
          color: $text-tertiary;
        }

        &:active {
          transform: scale(0.98);
        }
      }
    }
  }

  .wallpaper-section {
    padding: 0 24rpx 60rpx;

    // 选择模式下底部留白，避免内容被固定操作栏遮挡
    &.select-padding {
      padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
    }
  }

  // 排序栏
  .sort-bar {
    display: flex;
    gap: 16rpx;
    margin-bottom: 18rpx;

    .sort-item {
      padding: 8rpx 24rpx;
      border-radius: $radius-full;
      font-size: $font-caption;
      color: $text-secondary;
      background-color: $bg-card;
      border: 1rpx solid transparent;

      &.active {
        color: #38BDF8;
        background-color: rgba(56, 189, 248, 0.1);
        border-color: rgba(56, 189, 248, 0.3);
        font-weight: $font-weight-semibold;
      }

      &:active {
        opacity: 0.8;
      }
    }
  }

  .wallpaper-grid {
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;

    .wallpaper-item {
      position: relative;
      width: 48.5%;
      margin-bottom: 18rpx;
      border-radius: $radius-lg;
      background-color: #E6F6FF;
      box-shadow: $shadow-card;
      overflow: hidden;
      transition: transform 0.12s ease;

      .wallpaper-img {
        width: 100%;
        height: 420rpx;
        display: block;
      }

      // 底部信息栏
      .card-footer {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 12rpx 16rpx;
        min-height: 44rpx;
        background-color: #ffffff;

        .card-name {
          flex: 1;
          font-size: $font-caption;
          color: $text-secondary;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          margin-right: 12rpx;
        }

        .card-like {
          display: flex;
          align-items: center;
          gap: 4rpx;
          flex-shrink: 0;

          .wallpaper-action-icon {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 28rpx;
            height: 28rpx;
            font-size: 28rpx;
            line-height: 28rpx;
            color: #999;
            font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Arial, sans-serif;

            &.active {
              color: #ff4757;
            }
          }

          .card-like-count {
            font-size: 22rpx;
            color: #666;
            font-weight: 500;
          }
        }
      }

      // 选择模式下遮罩 + 勾选
      .select-mask {
        position: absolute;
        top: 0;
        right: 0;
        width: 56rpx;
        height: 56rpx;
        display: flex;
        align-items: center;
        justify-content: center;

        .select-check {
          width: 40rpx;
          height: 40rpx;
          border-radius: $radius-full;
          border: 3rpx solid #fff;
          background-color: rgba(0, 0, 0, 0.3);
          display: flex;
          align-items: center;
          justify-content: center;

          &.checked {
            background-color: #38BDF8;
            border-color: #38BDF8;
          }
        }
      }

      // 选中态：边框高亮 + 轻微缩放
      &.is-selected {
        box-shadow: 0 0 0 4rpx #38BDF8, $shadow-card;
      }

      &:active {
        transform: scale(0.98);
        opacity: 0.92;
      }
    }
  }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 110rpx 36rpx;
    margin: 32rpx 0;
    border-radius: $radius-xl;
    background-color: $bg-card;
    box-shadow: $shadow-card;

    .empty-icon {
      width: 108rpx;
      height: 108rpx;
      border-radius: $radius-full;
      background-color: #E6F6FF;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .empty-text {
      font-size: $font-h2;
      font-weight: $font-weight-semibold;
      color: $text-primary;
      margin-top: 24rpx;
    }

    .empty-hint {
      font-size: $font-caption;
      color: $text-disabled;
      margin-top: 12rpx;
    }
  }

  // 底部批量操作栏
  .weapp-share-bar {
    position: fixed;
    left: 24rpx;
    right: 24rpx;
    bottom: calc(24rpx + env(safe-area-inset-bottom));
    z-index: 100;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 18rpx 20rpx;
    border-radius: $radius-lg;
    background-color: $bg-card;
    box-shadow: 0 8rpx 28rpx rgba(15, 23, 42, 0.14);

    .weapp-share-info {
      min-width: 0;
      display: flex;
      flex-direction: column;
      margin-right: 16rpx;

      .weapp-share-title {
        font-size: $font-body;
        color: $text-primary;
        font-weight: $font-weight-semibold;
      }

      .weapp-share-desc {
        margin-top: 4rpx;
        font-size: $font-caption;
        color: $text-tertiary;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .weapp-share-actions {
      display: flex;
      align-items: center;
      flex-shrink: 0;

      .weapp-share-cancel {
        padding: 12rpx 18rpx;
        font-size: $font-caption;
        color: $text-secondary;
      }

      .weapp-share-btn {
        display: flex;
        align-items: center;
        gap: 6rpx;
        padding: 14rpx 24rpx;
        border-radius: $radius-full;
        background: linear-gradient(135deg, #38BDF8 0%, #0EA5E9 100%);
        color: #fff;
        font-size: $font-caption;
        font-weight: $font-weight-semibold;
      }
    }
  }

  .batch-bar {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 99;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 18rpx 28rpx;
    padding-bottom: calc(18rpx + env(safe-area-inset-bottom));
    background-color: $bg-card;
    box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);

    .batch-bar-left {
      display: flex;
      align-items: center;

      .batch-bar-text {
        margin-left: 14rpx;
        font-size: $font-body;
        color: $text-primary;
      }
    }

    .batch-bar-right {
      display: flex;
      align-items: center;
    }

    .batch-bar-cancel {
      font-size: $font-body;
      color: $text-secondary;
      padding: 12rpx 24rpx;
    }

    .batch-bar-download {
      display: flex;
      align-items: center;
      gap: 8rpx;
      padding: 16rpx 36rpx;
      border-radius: $radius-full;
      background: linear-gradient(135deg, #38BDF8 0%, #0EA5E9 100%);

      .batch-bar-download-text {
        font-size: $font-body;
        color: #fff;
        font-weight: $font-weight-semibold;
      }

      &:active {
        opacity: 0.85;
      }

      &.disabled {
        opacity: 0.4;
      }
    }
  }

  // 复用勾选圆（全选栏用）
  .select-check {
    width: 36rpx;
    height: 36rpx;
    border-radius: $radius-full;
    border: 3rpx solid #cdd6e0;
    background-color: transparent;
    display: flex;
    align-items: center;
    justify-content: center;

    &.checked {
      background-color: #38BDF8;
      border-color: #38BDF8;
    }
  }
}
</style>
