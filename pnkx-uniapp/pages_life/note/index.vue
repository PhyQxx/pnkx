<!--
 * @File: index
 * @Author: PHY
 * @Date: 2024/03/09
 * @Description: 笔记列表页
-->
<template>
  <view class="note-page subpage-shell">
    <view class="note-header">
      <view class="header-copy">
        <text class="eyebrow">My notes</text>
        <text class="header-title">{{ currentFolderName }}</text>
        <text class="header-subtitle">{{ headerSubtitle }}</text>
      </view>
      <view class="header-actions">
        <view class="header-action" @click="openObsidian">
          <text class="header-action-text">Obsidian</text>
        </view>
        <view class="header-action" @click="openFolderManage">
          <uni-icons type="folder-add" size="20" color="#2563EB" />
        </view>
      </view>
      <view class="note-stats">
        <view class="stat-card">
          <text class="stat-value">{{ noteList.length }}</text>
          <text class="stat-label">本页笔记</text>
        </view>
        <view class="stat-card">
          <text class="stat-value">{{ folderList.length }}</text>
          <text class="stat-label">子文件夹</text>
        </view>
      </view>
    </view>

    <view class="toolbar">
      <view class="search-bar">
        <uni-search-bar
          v-model="searchKeyword"
          placeholder="搜索标题或内容"
          @confirm="handleSearch"
          @clear="handleSearch"
          @input="onSearchInput"
          radius="100"
          bgColor="#F2F7FE"
          :focus="false"
          :show-action="false"
        />
      </view>

      <!-- 面包屑导航 -->
      <scroll-view class="breadcrumb" scroll-x v-if="folderPath.length > 0">
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
        <uni-icons type="folder" size="14" color="#8EA0B8" />
        <text>全部笔记</text>
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
      <view class="search-result-bar" v-if="searchKeyword">
        <text>“{{ searchKeyword }}”的搜索结果</text>
        <text class="result-count">{{ noteList.length }} 条</text>
      </view>

      <!-- 文件夹列表 -->
      <view class="folder-section" v-if="folderList.length > 0 && !searchKeyword">
        <view class="section-header">
          <text class="section-title">文件夹</text>
          <text class="section-extra" @click="openFolderManage">管理</text>
        </view>
        <scroll-view class="folder-scroll-x" scroll-x>
          <view class="folder-grid">
            <view
              v-for="folder in folderList"
              :key="folder.id"
              class="folder-card"
              @click="enterFolder(folder)"
            >
              <view class="folder-card-icon">
                <uni-icons v-if="folder.password" type="locked" size="24" color="#D97706" />
                <uni-icons v-else type="folder" size="24" color="#2563EB" />
              </view>
              <view class="folder-card-body">
                <view class="folder-card-name">{{ folder.name }}</view>
                <view class="folder-card-count">{{ folder.noteCount || 0 }} 篇笔记</view>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- 笔记列表 -->
      <view class="note-section">
        <view class="section-header" v-if="noteList.length > 0 && !searchKeyword">
          <text class="section-title">最近笔记</text>
          <text class="section-extra">{{ noteList.length }} 条</text>
        </view>
        <view class="note-list" v-if="noteList.length > 0">
          <note-card
            v-for="note in noteList"
            :key="note.id"
            :note="note"
            @click="viewNote"
            @edit="editNote"
            @delete="deleteNote"
          />
        </view>

        <!-- 空状态 -->
        <view class="empty-state" v-if="noteList.length === 0 && folderList.length === 0 && !loading">
          <view class="empty-icon">
            <uni-icons type="compose" size="42" color="#2563EB" />
          </view>
          <text class="empty-text">{{ searchKeyword ? '没有匹配的笔记' : '这里还没有内容' }}</text>
          <text class="empty-hint">{{ searchKeyword ? '换个关键词试试' : '新建一篇笔记，或先创建一个文件夹' }}</text>
          <view class="empty-action" v-if="!searchKeyword" @click="handleCreateNote">写第一篇</view>
        </view>

        <!-- 加载状态 -->
        <uni-load-more :status="loadStatus" />
      </view>
    </scroll-view>

    <!-- 悬浮按钮 -->
    <view class="fab-container">
      <view v-if="fabExpanded" class="fab-menu">
        <view class="fab-menu-item" @click="handleCreateNote">
          <view class="fab-menu-icon">
            <uni-icons type="plus" size="20" color="#FFFFFF" />
          </view>
          <text class="fab-menu-text">新建笔记</text>
        </view>
        <view class="fab-menu-item" @click="handleCreateFolder">
          <view class="fab-menu-icon">
            <uni-icons type="folder-add" size="20" color="#FFFFFF" />
          </view>
          <text class="fab-menu-text">新建文件夹</text>
        </view>
      </view>
      <view class="fab-btn" @click="toggleFab">
        <uni-icons :type="fabExpanded ? 'close' : 'plus'" size="28" color="#FFFFFF" />
      </view>
    </view>

    <!-- 新建文件夹弹窗 -->
    <uni-popup ref="folderPopup" type="dialog">
      <uni-popup-dialog
        mode="input"
        title="新建文件夹"
        placeholder="请输入文件夹名称"
        :before-close="true"
        @confirm="createFolder"
        @close="$refs.folderPopup.close()"
      />
    </uni-popup>

    <!-- 密码输入弹窗 -->
    <uni-popup ref="passwordPopup" type="center">
      <view class="password-dialog">
        <view class="password-title">输入密码</view>
        <view class="password-hint">该文件夹需要密码才能访问</view>
        <uni-easyinput
          v-model="passwordInput"
          type="password"
          placeholder="请输入访问密码"
          :clearable="false"
        />
        <view class="password-actions">
          <view class="password-btn cancel" @click="cancelPassword">取消</view>
          <view class="password-btn confirm" @click="confirmPassword">确定</view>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script>
import NoteCard from './components/NoteCard.vue'
import uniPopup from '@/uni_modules/uni-popup/components/uni-popup/uni-popup.vue'
import uniPopupDialog from '@/uni_modules/uni-popup/components/uni-popup-dialog/uni-popup-dialog.vue'
import {
  listNote,
  delNote,
  listFolder,
  addFolder
} from '@/api/px/life/note'

export default {
  name: 'NoteIndex',
  components: {
    NoteCard,
    uniPopup,
    uniPopupDialog
  },
  data() {
    return {
      // 当前文件夹ID（0为根目录）
      currentFolderId: 0,
      // 面包屑路径
      folderPath: [],
      // 文件夹列表
      folderList: [],
      // 笔记列表
      noteList: [],
      // 搜索关键词
      searchKeyword: '',
      // 搜索定时器（防抖）
      searchTimer: null,
      // 分页参数
      queryParams: {
        pageNum: 1,
        pageSize: 20
      },
      // 加载状态
      loading: false,
      isRefreshing: false,
      hasMore: true,
      loadStatus: 'more',
      // 悬浮按钮展开状态
      fabExpanded: false,
      // 密码相关
      pendingFolder: null,
      passwordInput: '',
      // 是否已初始化（防止重复加载）
      initialized: false
    }
  },
  computed: {
    currentFolderName() {
      if (this.currentFolderId && this.folderPath.length === 0) return '当前文件夹'
      if (this.folderPath.length === 0) return '我的笔记'
      return this.folderPath[this.folderPath.length - 1].name
    },
    headerSubtitle() {
      if (this.searchKeyword) return `正在搜索：${this.searchKeyword}`
      if (this.currentFolderId || this.folderPath.length > 0) return '当前文件夹内容'
      return '收纳灵感、清单和随手记录'
    }
  },
  onLoad(options = {}) {
    if (options.folderId) {
      this.currentFolderId = Number(options.folderId) || 0
    }
    this.initialized = true
    this.loadData()
  },
  onShow() {
    // 只在已初始化后刷新（从详情页返回时）
    // 避免首次加载时重复调用
    if (this.initialized) {
      this.refreshData()
    }
  },
  methods: {
    /**
     * 加载数据
     */
    async loadData() {
      if (this.loading) return

      this.loading = true
      this.loadStatus = 'loading'

      try {
        // 并行加载文件夹和笔记
        await Promise.all([
          this.loadFolders(),
          this.loadNotes()
        ])
      } catch (error) {
        console.error('加载数据失败:', error)
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.loading = false
        this.isRefreshing = false
        this.loadStatus = this.hasMore ? 'more' : 'noMore'
      }
    },

    /**
     * 加载文件夹列表（只加载当前层级）
     */
    async loadFolders() {
      if (this.searchKeyword) {
        this.folderList = []
        return
      }

      // 只传递 parentId 参数，获取当前层级的子文件夹
      const params = {
        parentId: this.currentFolderId
      }

      const res = await listFolder(params)
      this.folderList = res.rows.filter(folder => !folder.folder) || []
    },

    /**
     * 加载笔记列表
     */
    async loadNotes() {
      const params = {
        pageNum: this.queryParams.pageNum,
        pageSize: this.queryParams.pageSize,
        folder: this.currentFolderId || '',
        title: this.searchKeyword || ''
      }

      const res = await listNote(params)
      const rows = res.rows || []

      if (this.queryParams.pageNum === 1) {
        this.noteList = rows
      } else {
        this.noteList = [...this.noteList, ...rows]
      }

      this.hasMore = rows.length >= this.queryParams.pageSize
    },

    /**
     * 刷新数据
     */
    refreshData() {
      this.queryParams.pageNum = 1
      this.hasMore = true
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
     * 加载更多
     */
    async loadMore() {
      if (!this.hasMore || this.loading) return

      this.queryParams.pageNum++
      await this.loadNotes()
      this.loadStatus = this.hasMore ? 'more' : 'noMore'
    },

    /**
     * 搜索输入处理（防抖）
     */
    onSearchInput(value) {
      // 清除之前的定时器
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
      }
      // 延迟300ms后执行搜索
      this.searchTimer = setTimeout(() => {
        this.handleSearch()
      }, 1000)
    },

    /**
     * 搜索
     */
    handleSearch() {
      this.refreshData()
    },

    /**
     * 进入文件夹（向下穿透）
     */
    enterFolder(folder) {
      // 检查是否需要密码
      if (folder.password) {
        this.pendingFolder = folder
        this.passwordInput = ''
        this.$refs.passwordPopup.open()
        return
      }

      this.navigateToFolder(folder)
    },

    /**
     * 导航到指定文件夹
     * @param {Number|Object} target - 文件夹ID 或 文件夹对象
     * @param {Number} pathIndex - 面包屑索引（用于返回上级）
     */
    navigateToFolder(target, pathIndex = -1) {
      const folderId = typeof target === 'object' ? target.id : target

      // 返回根目录
      if (folderId === 0) {
        this.folderPath = []
        this.currentFolderId = 0
        this.refreshData()
        return
      }

      // 从面包屑点击（pathIndex >= 0），跳转到指定层级
      if (pathIndex >= 0 && pathIndex < this.folderPath.length) {
        // 截断路径到指定层级（保留到 pathIndex）
        this.folderPath = this.folderPath.slice(0, pathIndex + 1)
        // 跳转到的文件夹ID就是路径中该位置文件夹的ID
        this.currentFolderId = folderId
        this.refreshData()
        return
      }

      // 进入子文件夹，添加到路径
      const folder = typeof target === 'object' ? target : this.folderList.find(f => f.id === target)
      this.currentFolderId = folderId
      if (folder) {
        this.folderPath.push({ id: folder.id, name: folder.name })
      }

      this.refreshData()
    },

    /**
     * 查看笔记详情
     */
    viewNote(note) {
      this.$tab.navigateTo(`/pages_life/note/detail?id=${note.id}`)
    },

    /**
     * 编辑笔记
     */
    editNote(note) {
      this.$tab.navigateTo(`/pages_life/note/detail?id=${note.id}`)
    },

    /**
     * 删除笔记
     */
    async deleteNote(note) {
      try {
        await this.$dialog.confirm({
          title: '删除确认',
          message: `确定要删除笔记「${note.title || '无标题'}」吗？`
        })

        await delNote(note.id)
        uni.showToast({ title: '删除成功', icon: 'success' })
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除失败:', error)
        }
      }
    },

    /**
     * 切换悬浮按钮菜单
     */
    toggleFab() {
      this.fabExpanded = !this.fabExpanded
    },

    /**
     * 新建笔记
     */
    handleCreateNote() {
      this.fabExpanded = false
      this.$tab.navigateTo(`/pages_life/note/detail?folder=${this.currentFolderId}`)
    },

    /**
     * 显示新建文件夹弹窗
     */
    handleCreateFolder() {
      this.fabExpanded = false
      this.$refs.folderPopup.open()
    },

    /**
     * 打开文件夹管理
     */
    openFolderManage() {
      this.fabExpanded = false
      this.$tab.navigateTo('/pages_life/note/folder')
    },
    /**
     * 跳转到 Obsidian 笔记（文件管理器）
     */
    openObsidian() {
      this.$tab.navigateTo('/pages_life/note/obsidian')
    },

    /**
     * 创建文件夹
     */
    async createFolder(value) {
      if (!value || !value.trim()) {
        uni.showToast({ title: '请输入文件夹名称', icon: 'none' })
        return
      }

      try {
        await addFolder({
          name: value.trim(),
          parentId: this.currentFolderId || 0
        })
        uni.showToast({ title: '创建成功', icon: 'success' })
        this.$refs.folderPopup.close()
        this.refreshData()
      } catch (error) {
        console.error('创建文件夹失败:', error)
      }
    },

    /**
     * 取消密码输入
     */
    cancelPassword() {
      this.pendingFolder = null
      this.passwordInput = ''
      this.$refs.passwordPopup.close()
    },

    /**
     * 确认密码
     */
    confirmPassword() {
      if (!this.passwordInput) {
        uni.showToast({ title: '请输入密码', icon: 'none' })
        return
      }

      // 验证密码（这里简化处理，实际应该调用后端接口验证）
      if (this.pendingFolder && this.pendingFolder.password === this.passwordInput) {
        this.$refs.passwordPopup.close()
        this.navigateToFolder(this.pendingFolder)
      } else {
        uni.showToast({ title: '密码错误', icon: 'none' })
      }

      this.passwordInput = ''
    }
  }
}
</script>

<style lang="scss" scoped>
.note-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: linear-gradient(180deg, #EEF5FF 0%, #F2F7FE 360rpx, #F2F7FE 100%);

  .note-header {
    position: relative;
    padding: 34rpx 28rpx 24rpx;
    overflow: hidden;

    .header-copy {
      display: flex;
      flex-direction: column;
      padding-right: 92rpx;
    }

    .eyebrow {
      font-size: $font-mini;
      color: #2563EB;
      font-weight: $font-weight-semibold;
      text-transform: uppercase;
    }

    .header-title {
      margin-top: 8rpx;
      font-size: 46rpx;
      line-height: 1.15;
      font-weight: $font-weight-bold;
      color: #172033;
    }

    .header-subtitle {
      margin-top: 10rpx;
      font-size: $font-caption;
      color: $text-secondary;
    }

    .header-actions {
      position: absolute;
      right: 28rpx;
      top: 38rpx;
      display: flex;
      align-items: center;
      gap: $spacing-sm;

      .header-action {
        height: 64rpx;
        min-width: 64rpx;
        padding: 0 $spacing-md;
        border-radius: $radius-full;
        background-color: rgba(255, 255, 255, 0.92);
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: $shadow-sm;
      }

      .header-action-text {
        font-size: $font-mini;
        color: #2563EB;
        font-weight: $font-weight-semibold;
        white-space: nowrap;
      }
    }

    .note-stats {
      display: flex;
      margin-top: 28rpx;

      .stat-card {
        flex: 1;
        display: flex;
        flex-direction: column;
        padding: 22rpx 24rpx;
        border-radius: $radius-lg;
        background-color: rgba(255, 255, 255, 0.86);
        border: 1rpx solid rgba(255, 255, 255, 0.7);
        box-shadow: $shadow-sm;

        & + .stat-card {
          margin-left: 16rpx;
        }

        .stat-value {
          font-size: 36rpx;
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
      color: $note;
      background-color: rgba($note, 0.08);
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

  .search-result-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin: 0 24rpx 18rpx;
    padding: 18rpx 22rpx;
    border-radius: $radius-lg;
    background-color: #EBF3FF;
    color: #2563EB;
    font-size: $font-caption;

    .result-count {
      color: $text-secondary;
    }
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
      color: $note;
    }
  }

  .folder-section {
    margin: 0 24rpx 24rpx;

    .folder-scroll-x {
      width: 100%;
      white-space: nowrap;
    }

    .folder-grid {
      display: inline-flex;
      padding-bottom: 4rpx;

      .folder-card {
        width: 288rpx;
        min-height: 132rpx;
        margin-right: 18rpx;
        padding: 22rpx;
        display: inline-flex;
        align-items: flex-start;
        border-radius: $radius-lg;
        background-color: $bg-card;
        box-shadow: $shadow-card;

        .folder-card-icon {
          width: 58rpx;
          height: 58rpx;
          margin-right: 16rpx;
          border-radius: $radius-md;
          background-color: #EEF5FF;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
        }

        .folder-card-body {
          min-width: 0;
          flex: 1;
        }

        .folder-card-name {
          font-size: $font-body;
          color: $text-primary;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .folder-card-count {
          margin-top: 12rpx;
          font-size: $font-caption;
          color: $text-tertiary;
        }

        &:active {
          transform: scale(0.98);
          background-color: $gray-50;
        }
      }
    }
  }

  .note-section {
    padding: 0 24rpx 140rpx;
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
      background-color: #EEF5FF;
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

    .empty-action {
      margin-top: 30rpx;
      height: 72rpx;
      line-height: 72rpx;
      padding: 0 34rpx;
      border-radius: $radius-full;
      background-color: $note;
      color: $text-inverse;
      font-size: $font-body;
      font-weight: $font-weight-medium;
    }
  }
}

.password-dialog {
  width: 560rpx;
  background-color: $bg-card;
  border-radius: $radius-xl;
  padding: 40rpx;

  .password-title {
    font-size: 34rpx;
    font-weight: $font-weight-medium;
    color: $text-primary;
    text-align: center;
    margin-bottom: 16rpx;
  }

  .password-hint {
    font-size: $font-caption;
    color: $text-tertiary;
    text-align: center;
    margin-bottom: 32rpx;
  }

  .password-actions {
    display: flex;
    margin-top: 32rpx;

    .password-btn {
      flex: 1;
      height: 80rpx;
      line-height: 80rpx;
      text-align: center;
      font-size: $font-h3;
      border-radius: $radius-full;

      &.cancel {
        color: $text-secondary;
        background-color: $bg-page;
        margin-right: 20rpx;
      }

      &.confirm {
        color: $text-inverse;
        background-color: $note;
      }
    }
  }
}

.fab-container {
  position: fixed;
  right: 32rpx;
  bottom: calc(120rpx + env(safe-area-inset-bottom));
  z-index: 100;

  .fab-menu {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    margin-bottom: 20rpx;

    .fab-menu-item {
      display: flex;
      align-items: center;
      margin-bottom: 16rpx;

      .fab-menu-icon {
        width: 72rpx;
        height: 72rpx;
        background-color: #2563EB;
        border-radius: $radius-full;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-left: 16rpx;
      }

      .fab-menu-text {
        font-size: $font-caption;
        color: $text-primary;
        background-color: $bg-card;
        padding: 12rpx 20rpx;
        border-radius: $radius-sm;
        box-shadow: $shadow-card;
      }
    }
  }

  .fab-btn {
    width: 100rpx;
    height: 100rpx;
    background: linear-gradient(135deg, #2563EB 0%, #60A5FA 100%);
    border-radius: $radius-full;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 10rpx 28rpx rgba(37, 99, 235, 0.28);

    &:active {
      transform: scale(0.95);
    }
  }
}
</style>
