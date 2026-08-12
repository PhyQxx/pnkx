<!--
 * @File: folder
 * @Author: PHY
 * @Date: 2024/03/09
 * @Description: 文件夹管理页
-->
<template>
  <view class="folder-page subpage-shell">
    <view class="folder-header">
      <view class="header-copy">
        <text class="eyebrow">Folders</text>
        <text class="header-title">文件夹管理</text>
        <text class="header-subtitle">{{ folderTree.length }} 个根文件夹 · 按层级整理笔记</text>
      </view>
      <view class="header-action" @click="openCreateDialog">
        <uni-icons type="plus" size="22" color="#FFFFFF" />
      </view>
    </view>

    <!-- 文件夹树形列表 -->
    <scroll-view class="folder-scroll" scroll-y>
      <view class="folder-tree-panel" v-if="folderTree.length > 0">
        <uni-collapse>
          <folder-tree-item
            v-for="folder in folderTree"
            :key="folder.id"
            :folder="folder"
            :level="0"
            @edit="editFolder"
            @delete="deleteFolder"
            @click="handleFolderClick"
          />
        </uni-collapse>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-else-if="!loading">
        <view class="empty-icon">
          <uni-icons type="folder-add" size="42" color="#2563EB" />
        </view>
        <text class="empty-text">还没有文件夹</text>
        <text class="empty-hint">按主题归档笔记，查找会轻松很多</text>
        <view class="empty-action" @click="openCreateDialog">新建文件夹</view>
      </view>
    </scroll-view>

    <!-- 新建/编辑文件夹弹窗 -->
    <uni-popup ref="folderPopup" type="center">
      <view class="folder-dialog">
        <view class="dialog-title">{{ editingFolder ? '编辑文件夹' : '新建文件夹' }}</view>

        <view class="dialog-form">
          <view class="form-item">
            <view class="form-label">名称</view>
            <uni-easyinput
              v-model="folderForm.name"
              placeholder="请输入文件夹名称"
              :clearable="true"
              :maxlength="50"
            />
          </view>

          <view class="form-item">
            <view class="form-label">访问密码（可选）</view>
            <uni-easyinput
              v-model="folderForm.password"
              type="password"
              placeholder="设置密码保护此文件夹"
              :clearable="true"
              :maxlength="20"
            />
            <view class="form-hint">设置密码后，访问此文件夹需要输入密码</view>
          </view>

          <view class="form-item">
            <view class="form-label">上级位置</view>
            <view class="parent-picker" @click="openParentPicker">
              <text class="parent-name">{{ parentFolderName }}</text>
              <uni-icons type="right" size="16" color="#999999" />
            </view>
          </view>
        </view>

        <view class="dialog-actions">
          <view class="dialog-btn cancel" @click="closeFolderDialog">取消</view>
          <view class="dialog-btn confirm" @click="saveFolder">确定</view>
        </view>
      </view>
    </uni-popup>

    <!-- 父级文件夹选择弹窗 -->
    <uni-popup ref="parentPopup" type="bottom">
      <view class="parent-picker-popup">
        <view class="popup-header">
          <view class="popup-cancel" @click="$refs.parentPopup.close()">取消</view>
          <view class="popup-title">选择父级文件夹</view>
          <view class="popup-confirm" @click="confirmParent">确定</view>
        </view>
        <scroll-view class="popup-content" scroll-y>
          <view
            class="folder-option"
            :class="{ selected: tempParentId === 0 }"
            @click="tempParentId = 0"
          >
            <uni-icons type="folder" size="20" color="#4F86F7" />
            <text class="folder-option-name">根目录</text>
            <uni-icons v-if="tempParentId === 0" type="checkmarkempty" size="18" color="#4F86F7" />
          </view>
          <view
            v-for="folder in flatFolderList"
            :key="folder.id"
            class="folder-option"
            :class="{ selected: tempParentId === folder.id, disabled: folder.id === (editingFolder && editingFolder.id) }"
            @click="selectParent(folder)"
          >
            <uni-icons type="folder" size="20" :color="folder.password ? '#FBBF24' : '#4F86F7'" />
            <text class="folder-option-name">{{ folder.indent }}{{ folder.name }}</text>
            <uni-icons v-if="tempParentId === folder.id" type="checkmarkempty" size="18" color="#4F86F7" />
          </view>
        </scroll-view>
      </view>
    </uni-popup>
  </view>
</template>

<script>
import FolderTreeItem from './components/FolderTreeItem.vue'
import uniPopup from '@/uni_modules/uni-popup/components/uni-popup/uni-popup.vue'
import {
  treeList,
  addFolder,
  updateFolder,
  delFolder
} from '@/api/px/life/note'

export default {
  name: 'FolderManage',
  components: {
    FolderTreeItem,
    uniPopup
  },
  data() {
    return {
      loading: false,
      folderTree: [],
      flatFolderList: [],
      editingFolder: null,
      folderForm: {
        name: '',
        password: '',
        parentId: 0
      },
      tempParentId: 0
    }
  },
  computed: {
    parentFolderName() {
      if (this.tempParentId === 0) return '根目录'
      const folder = this.flatFolderList.find(f => f.id === this.tempParentId)
      return folder ? folder.name : '根目录'
    }
  },
  onLoad() {
    this.loadFolderTree()
  },
  onNavigationBarButtonTap(e) {
    if (e.index === 0) {
      this.openCreateDialog()
    }
  },
  methods: {
    /**
     * 加载文件夹树
     */
    async loadFolderTree() {
      this.loading = true
      try {
        const res = await treeList({})
        this.folderTree = res.data || []
        this.flatFolderList = []
        this.flattenFolderList(this.folderTree, 0)
      } catch (error) {
        console.error('加载文件夹失败:', error)
      } finally {
        this.loading = false
      }
    },

    /**
     * 扁平化文件夹列表（用于选择器）
     */
    flattenFolderList(folders, level) {
      folders.forEach(folder => {
        this.flatFolderList.push({
          ...folder,
          indent: '　'.repeat(level)
        })
        if (folder.children && folder.children.length > 0) {
          this.flattenFolderList(folder.children, level + 1)
        }
      })
    },

    /**
     * 打开新建弹窗
     */
    openCreateDialog() {
      this.editingFolder = null
      this.folderForm = {
        name: '',
        password: '',
        parentId: 0
      }
      this.tempParentId = 0
      this.$refs.folderPopup.open()
    },

    /**
     * 编辑文件夹
     */
    editFolder(folder) {
      this.editingFolder = folder
      this.folderForm = {
        name: folder.name || '',
        password: folder.password || '',
        parentId: folder.parentId || 0
      }
      this.tempParentId = this.folderForm.parentId
      this.$refs.folderPopup.open()
    },

    /**
     * 关闭弹窗
     */
    closeFolderDialog() {
      this.$refs.folderPopup.close()
      this.editingFolder = null
    },

    /**
     * 保存文件夹
     */
    async saveFolder() {
      if (!this.folderForm.name || !this.folderForm.name.trim()) {
        uni.showToast({ title: '请输入文件夹名称', icon: 'none' })
        return
      }

      try {
        uni.showLoading({ title: '保存中' })

        const data = {
          name: this.folderForm.name.trim(),
          password: this.folderForm.password || '',
          parentId: this.folderForm.parentId || 0
        }

        if (this.editingFolder) {
          data.id = this.editingFolder.id
          await updateFolder(data)
          uni.showToast({ title: '修改成功', icon: 'success' })
        } else {
          await addFolder(data)
          uni.showToast({ title: '创建成功', icon: 'success' })
        }

        this.$refs.folderPopup.close()
        this.loadFolderTree()
      } catch (error) {
        console.error('保存失败:', error)
      } finally {
        uni.hideLoading()
      }
    },

    /**
     * 删除文件夹
     */
    async deleteFolder(folder) {
      try {
        await this.$dialog.confirm({
          title: '删除确认',
          message: `确定要删除文件夹「${folder.name}」吗？该文件夹下的所有笔记将一并删除。`,
          confirmButtonText: '删除',
          confirmButtonColor: '#E74C3C'
        })

        uni.showLoading({ title: '删除中' })
        await delFolder(folder.id)
        uni.showToast({ title: '删除成功', icon: 'success' })
        this.loadFolderTree()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除失败:', error)
        }
      } finally {
        uni.hideLoading()
      }
    },

    /**
     * 打开父级选择器
     */
    openParentPicker() {
      this.tempParentId = this.folderForm.parentId || 0
      this.$refs.parentPopup.open()
    },

    /**
     * 选择父级
     */
    selectParent(folder) {
      // 不能选择自己作为父级
      if (this.editingFolder && folder.id === this.editingFolder.id) {
        uni.showToast({ title: '不能选择自己作为父级', icon: 'none' })
        return
      }
      this.tempParentId = folder.id
    },

    /**
     * 确认父级选择
     */
    confirmParent() {
      this.folderForm.parentId = this.tempParentId
      this.$refs.parentPopup.close()
    },

    /**
     * 点击文件夹
     */
    handleFolderClick(folder) {
      // 可以跳转到该文件夹下的笔记列表
      this.$tab.navigateTo(`/pages_life/note/index?folderId=${folder.id}`)
    }
  }
}
</script>

<style lang="scss" scoped>
.folder-page {
  height: 100vh;
  background: linear-gradient(180deg, #EEF5FF 0%, #F2F7FE 360rpx, #F2F7FE 100%);

  .folder-header {
    position: relative;
    padding: 34rpx 28rpx 24rpx;

    .header-copy {
      display: flex;
      flex-direction: column;
      padding-right: 98rpx;
    }

    .eyebrow {
      font-size: $font-mini;
      color: #2563EB;
      font-weight: $font-weight-semibold;
      text-transform: uppercase;
    }

    .header-title {
      margin-top: 8rpx;
      font-size: 44rpx;
      line-height: 1.15;
      font-weight: $font-weight-bold;
      color: #172033;
    }

    .header-subtitle {
      margin-top: 10rpx;
      font-size: $font-caption;
      color: $text-secondary;
    }

    .header-action {
      position: absolute;
      right: 28rpx;
      top: 42rpx;
      width: 76rpx;
      height: 76rpx;
      border-radius: $radius-full;
      background: linear-gradient(135deg, #2563EB 0%, #60A5FA 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 10rpx 28rpx rgba(37, 99, 235, 0.25);

      &:active {
        transform: scale(0.96);
      }
    }
  }

  .folder-scroll {
    height: calc(100vh - 178rpx);
    padding: 0 24rpx 36rpx;
  }

  .folder-tree-panel {
    border-radius: $radius-xl;
    overflow: hidden;
    background-color: $bg-card;
    box-shadow: $shadow-card;
  }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 110rpx 36rpx;
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
      font-size: $font-small;
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

.folder-dialog {
  width: 600rpx;
  background-color: $bg-card;
  border-radius: $radius-xl;
  padding: 42rpx 36rpx 36rpx;

  .dialog-title {
    font-size: $font-h1;
    font-weight: $font-weight-medium;
    color: $text-primary;
    text-align: center;
    margin-bottom: 32rpx;
  }

  .dialog-form {
    .form-item {
      margin-bottom: 24rpx;

      .form-label {
        font-size: $font-body;
        font-weight: $font-weight-semibold;
        color: $text-secondary;
        margin-bottom: 12rpx;
      }

      .form-hint {
        font-size: $font-caption;
        color: $text-tertiary;
        margin-top: 8rpx;
      }
    }

    .parent-picker {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 20rpx;
      background-color: $bg-page;
      border-radius: $radius-lg;

      .parent-name {
        font-size: $font-body;
        color: $text-primary;
      }
    }
  }

  .dialog-actions {
    display: flex;
    margin-top: 32rpx;

    .dialog-btn {
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
        color: #FFFFFF;
        background-color: $note;
      }
    }
  }
}

.parent-picker-popup {
  background-color: $bg-card;
  border-radius: $radius-xl $radius-xl 0 0;
  max-height: 70vh;

  .popup-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 32rpx;
    border-bottom: 1rpx solid $border-color;

    .popup-cancel,
    .popup-confirm {
      font-size: $font-body;
      padding: 8rpx 16rpx;
    }

    .popup-cancel {
      color: $text-tertiary;
    }

    .popup-confirm {
      color: $note;
    }

    .popup-title {
      font-size: $font-h2;
      font-weight: $font-weight-medium;
      color: $text-primary;
    }
  }

  .popup-content {
    max-height: 60vh;
    padding: 16rpx 0;
  }

  .folder-option {
    display: flex;
    align-items: center;
    padding: 28rpx 32rpx;

    &.selected {
      background-color: rgba($note, 0.08);
    }

    &.disabled {
      opacity: 0.5;
      pointer-events: none;
    }

    &:active {
      background-color: $bg-page;
    }

    .folder-option-name {
      flex: 1;
      margin-left: 16rpx;
      font-size: $font-h3;
      color: $text-primary;
    }
  }
}
</style>
