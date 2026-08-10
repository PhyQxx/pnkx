<!--
 * @File: detail
 * @Author: PHY
 * @Date: 2024/03/09
 * @Description: 笔记详情/编辑页
-->
<template>
  <view class="note-detail">
    <!-- 表单内容 -->
    <scroll-view class="form-scroll" scroll-y>
      <view class="editor-hero">
        <text class="editor-eyebrow">{{ noteForm.id ? 'Edit note' : 'New note' }}</text>
        <text class="editor-title">{{ noteForm.id ? '整理这篇记录' : '写下一点东西' }}</text>
        <text class="editor-subtitle">{{ selectedFolderName || '根目录' }} · {{ contentCount }} 字</text>
      </view>

      <!-- 标题输入 -->
      <view class="form-section">
        <view class="form-label">
          <uni-icons type="compose" size="16" color="#2563EB" />
          <text>标题</text>
        </view>
        <uni-easyinput
          v-model="noteForm.title"
          placeholder="给这篇笔记起个名字"
          :clearable="true"
          :maxlength="100"
        />
      </view>

      <!-- 文件夹选择 -->
      <view class="form-section">
        <view class="form-label">
          <uni-icons type="folder" size="16" color="#2563EB" />
          <text>位置</text>
        </view>
        <view class="folder-picker" @click="openFolderPicker">
          <text class="folder-name">{{ selectedFolderName || '选择文件夹' }}</text>
          <uni-icons type="right" size="16" color="#999999" />
        </view>
      </view>

      <!-- 内容编辑 -->
      <view class="form-section content-section">
        <view class="form-label">
          <uni-icons type="compose" size="16" color="#2563EB" />
          <text>正文</text>
        </view>
        <textarea
          v-model="noteForm.content"
          class="content-textarea"
          placeholder="开始记录..."
          :maxlength="10000"
          auto-height
        />
      </view>

      <!-- 元信息 -->
      <view class="meta-section" v-if="noteForm.id">
        <view class="meta-item">
          <text class="meta-label">创建时间</text>
          <text class="meta-value">{{ formatTime(noteForm.createTime) }}</text>
        </view>
        <view class="meta-item">
          <text class="meta-label">更新时间</text>
          <text class="meta-value">{{ formatTime(noteForm.updateTime) }}</text>
        </view>
      </view>

      <!-- 删除按钮 -->
      <view class="delete-section" v-if="noteForm.id">
        <button class="delete-btn" @click="handleDelete">删除笔记</button>
      </view>
    </scroll-view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar">
      <view class="cancel-btn" @click="handleCancel">取消</view>
      <view class="save-btn" @click="handleSave">保存</view>
    </view>

    <!-- 文件夹选择弹窗 -->
    <uni-popup ref="folderPopup" type="bottom">
      <view class="folder-picker-popup">
        <view class="popup-header">
          <view class="popup-cancel" @click="$refs.folderPopup.close()">取消</view>
          <view class="popup-title">选择文件夹</view>
          <view class="popup-confirm" @click="confirmFolder">确定</view>
        </view>
        <scroll-view class="popup-content" scroll-y>
          <view
            class="folder-option"
            :class="{ selected: tempFolderId === 0 }"
            @click="tempFolderId = 0"
          >
            <uni-icons type="folder" size="20" color="#6C9EFF" />
            <text class="folder-option-name">根目录</text>
            <uni-icons v-if="tempFolderId === 0" type="checkmarkempty" size="18" color="#6C9EFF" />
          </view>
          <view
            v-for="folder in flatFolderList"
            :key="folder.id"
            class="folder-option"
            :class="{ selected: tempFolderId === folder.id }"
            @click="tempFolderId = folder.id"
          >
            <uni-icons type="folder" size="20" :color="folder.password ? '#FBBF24' : '#6C9EFF'" />
            <text class="folder-option-name">{{ folder.indent }}{{ folder.name }}</text>
            <uni-icons v-if="tempFolderId === folder.id" type="checkmarkempty" size="18" color="#6C9EFF" />
          </view>
        </scroll-view>
      </view>
    </uni-popup>
  </view>
</template>

<script>
import uniPopup from '@/uni_modules/uni-popup/components/uni-popup/uni-popup.vue'
import {
  getNote,
  addNote,
  updateNote,
  delNote,
  treeList
} from '@/api/px/life/note'

export default {
  name: 'NoteDetail',
  components: {
    uniPopup
  },
  data() {
    return {
      // 笔记ID（编辑模式）
      noteId: null,
      // 表单数据
      noteForm: {
        id: null,
        title: '',
        content: '',
        richText: '',
        folder: 0,
        order: 0
      },
      // 原始数据（用于判断是否修改）
      originalData: null,
      // 文件夹树
      folderTree: [],
      // 临时选中的文件夹ID
      tempFolderId: 0
    }
  },
  computed: {
    contentCount() {
      return (this.noteForm.content || '').length
    },
    selectedFolderName() {
      if (this.tempFolderId === 0) return '根目录'
      const folder = this.findFolderById(this.folderTree, this.tempFolderId)
      return folder ? folder.name : ''
    },
    flatFolderList() {
      const list = []
      this.flattenFolderList(this.folderTree, 0, list)
      return list
    }
  },
  onLoad(options) {
    if (options.id) {
      this.noteId = options.id
      this.loadNoteDetail()
    } else if (options.folder) {
      this.noteForm.folder = parseInt(options.folder)
      this.tempFolderId = this.noteForm.folder
    }
    this.loadFolderTree()
  },
  onNavigationBarButtonTap(e) {
    if (e.index === 0) {
      this.handleSave()
    }
  },
  methods: {
    /**
     * 加载笔记详情
     */
    async loadNoteDetail() {
      try {
        uni.showLoading({ title: '加载中' })
        const res = await getNote(this.noteId)
        this.noteForm = res.data;
        this.tempFolderId = this.noteForm.folder
        this.originalData = JSON.parse(JSON.stringify(this.noteForm))
      } catch (error) {
        console.error('加载笔记失败:', error)
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    },

    /**
     * 加载文件夹树
     */
    async loadFolderTree() {
      try {
        const res = await treeList({})
        this.folderTree = res.data || []
      } catch (error) {
        console.error('加载文件夹失败:', error)
      }
    },

    /**
     * 递归查找文件夹
     */
    findFolderById(folders, id) {
      for (const folder of folders) {
        if (folder.id === id) return folder
        if (folder.children && folder.children.length > 0) {
          const found = this.findFolderById(folder.children, id)
          if (found) return found
        }
      }
      return null
    },

    /**
     * 扁平化文件夹列表（用于选择器）
     */
    flattenFolderList(folders, level, list = []) {
      folders.forEach(folder => {
        list.push({
          ...folder,
          indent: '　'.repeat(level)
        })
        if (folder.children && folder.children.length > 0) {
          this.flattenFolderList(folder.children, level + 1, list)
        }
      })
      return list
    },

    /**
     * 打开文件夹选择器
     */
    openFolderPicker() {
      this.tempFolderId = this.noteForm.folder || 0
      this.$refs.folderPopup.open()
    },

    /**
     * 确认文件夹选择
     */
    confirmFolder() {
      this.noteForm.folder = this.tempFolderId
      this.$refs.folderPopup.close()
    },

    /**
     * 格式化时间
     */
    formatTime(time) {
      return this.$parseTime(time, '{y}-{m}-{d} {h}:{i}')
    },

    /**
     * 检查是否有修改
     */
    hasChanges() {
      if (!this.originalData) return true
      return (
        this.noteForm.title !== this.originalData.title ||
        this.noteForm.content !== this.originalData.content ||
        this.noteForm.folder !== this.originalData.folder
      )
    },

    /**
     * 取消
     */
    async handleCancel() {
      if (this.hasChanges()) {
        try {
          await this.$dialog.confirm({
            title: '提示',
            message: '有未保存的修改，确定要离开吗？',
            confirmButtonText: '离开',
            cancelButtonText: '继续编辑'
          })
          uni.navigateBack()
        } catch {
          // 取消离开
        }
      } else {
        uni.navigateBack()
      }
    },

    /**
     * 保存
     */
    async handleSave() {
      // 验证标题
      if (!this.noteForm.title || !this.noteForm.title.trim()) {
        uni.showToast({ title: '请输入笔记标题', icon: 'none' })
        return
      }

      try {
        uni.showLoading({ title: '保存中' })

        const data = {
          ...this.noteForm,
          title: this.noteForm.title.trim(),
          folder: this.noteForm.folder || 0
        }

        if (this.noteForm.id) {
          await updateNote(data)
          uni.showToast({ title: '修改成功', icon: 'success' })
        } else {
          await addNote(data)
          uni.showToast({ title: '创建成功', icon: 'success' })
        }

        // 延迟返回，让用户看到提示
        setTimeout(() => {
          uni.navigateBack()
        }, 500)
      } catch (error) {
        console.error('保存失败:', error)
        uni.showToast({ title: '保存失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    },

    /**
     * 删除
     */
    async handleDelete() {
      try {
        await this.$dialog.confirm({
          title: '删除确认',
          message: '确定要删除这篇笔记吗？删除后无法恢复。',
          confirmButtonText: '删除',
          confirmButtonColor: '#E74C3C'
        })

        uni.showLoading({ title: '删除中' })
        await delNote(this.noteForm.id)
        uni.showToast({ title: '删除成功', icon: 'success' })

        setTimeout(() => {
          uni.navigateBack()
        }, 500)
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除失败:', error)
        }
      } finally {
        uni.hideLoading()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.note-detail {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: linear-gradient(180deg, #EEF5FF 0%, #F7F8FB 360rpx, #F7F8FB 100%);

  .form-scroll {
    flex: 1;
    padding: 26rpx 24rpx;
    padding-bottom: 140rpx;
  }

  .editor-hero {
    display: flex;
    flex-direction: column;
    margin-bottom: 26rpx;
    padding: 28rpx 30rpx;
    border-radius: $radius-xl;
    background-color: rgba(255, 255, 255, 0.86);
    box-shadow: $shadow-sm;

    .editor-eyebrow {
      font-size: $font-mini;
      color: #2563EB;
      font-weight: $font-weight-semibold;
      text-transform: uppercase;
    }

    .editor-title {
      margin-top: 8rpx;
      font-size: 40rpx;
      line-height: 1.2;
      font-weight: $font-weight-bold;
      color: #172033;
    }

    .editor-subtitle {
      margin-top: 10rpx;
      font-size: $font-caption;
      color: $text-tertiary;
    }
  }

  .form-section {
    background-color: $bg-card;
    border-radius: $radius-xl;
    padding: 26rpx;
    margin-bottom: 24rpx;
    box-shadow: $shadow-card;

    .form-label {
      display: flex;
      align-items: center;
      font-size: $font-body;
      font-weight: $font-weight-semibold;
      color: $text-primary;
      margin-bottom: 18rpx;

      text {
        margin-left: 8rpx;
      }
    }
  }

  .folder-picker {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 22rpx 24rpx;
    border-radius: $radius-lg;
    background-color: $gray-50;

    .folder-name {
      font-size: $font-h3;
      color: $text-primary;
    }
  }

  .content-section {
    .content-textarea {
      width: 100%;
      min-height: 520rpx;
      font-size: $font-h3;
      color: $text-primary;
      line-height: 1.75;
      padding: 10rpx 0;
    }
  }

  .meta-section {
    background-color: $bg-card;
    border-radius: $radius-xl;
    padding: 24rpx 28rpx;
    margin-bottom: 24rpx;
    box-shadow: $shadow-card;

    .meta-item {
      display: flex;
      justify-content: space-between;
      padding: 14rpx 0;

      .meta-label {
        font-size: $font-body;
        color: $text-tertiary;
      }

      .meta-value {
        font-size: $font-body;
        color: $text-secondary;
      }
    }
  }

  .delete-section {
    padding: 16rpx 0 24rpx;

    .delete-btn {
      width: 100%;
      height: 88rpx;
      line-height: 88rpx;
      font-size: $font-h3;
      color: $danger;
      background-color: $bg-card;
      border-radius: $radius-xl;
      border: none;
      box-shadow: $shadow-card;

      &:active {
        background-color: $bg-page;
      }
    }
  }

  .bottom-bar {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    display: flex;
    padding: 20rpx 24rpx;
    padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
    background-color: rgba(255, 255, 255, 0.96);
    box-shadow: 0 -8rpx 24rpx rgba(23, 32, 51, 0.06);

    .cancel-btn,
    .save-btn {
      flex: 1;
      height: 88rpx;
      line-height: 88rpx;
      text-align: center;
      font-size: $font-h3;
      border-radius: 44rpx;
    }

    .cancel-btn {
      color: $text-secondary;
      background-color: $bg-page;
      margin-right: 24rpx;
    }

    .save-btn {
      color: $text-inverse;
      background: linear-gradient(135deg, #2563EB 0%, #60A5FA 100%);
    }
  }
}

.folder-picker-popup {
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
