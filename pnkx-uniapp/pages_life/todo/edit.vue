<!--
 * @File: edit
 * @Author: PHY
 * @Date: 2025/03/09
 * @Description: 待办编辑页
-->
<template>
  <view class="todo-edit">
    <!-- 内容输入 -->
    <view class="form-section">
      <view class="form-label">内容 <text class="required">*</text></view>
      <textarea
        v-model="todoForm.content"
        class="content-textarea"
        placeholder="请输入待办事项内容..."
        :maxlength="500"
        auto-height
      />
      <view class="content-count">{{ todoForm.content.length }}/500</view>
    </view>

    <!-- 执行者选择 -->
    <view class="form-section" @click="openPerformerPicker">
      <view class="form-label">执行者</view>
      <view class="picker-input">
        <text class="picker-value" :class="{ placeholder: !performerNameDisplay }">
          {{ performerNameDisplay || '请选择执行者' }}
        </text>
        <uni-icons type="arrowright" size="14" color="#9BA8B7" />
      </view>
    </view>

    <!-- 计划开始时间 -->
    <view class="form-section">
      <view class="form-label">计划开始时间</view>
      <uni-datetime-picker
        v-model="todoForm.planStartTime"
        type="datetime"
        :clear-icon="false"
        placeholder="请选择开始时间"
      />
    </view>

    <!-- 计划结束时间 -->
    <view class="form-section">
      <view class="form-label">计划结束时间</view>
      <uni-datetime-picker
        v-model="todoForm.planEndTime"
        type="datetime"
        :clear-icon="false"
        placeholder="请选择结束时间"
      />
    </view>

    <!-- 标签选择 -->
    <view class="form-section">
      <view class="form-label">标签</view>
      <view class="tag-selector">
        <uni-data-checkbox
          v-model="selectedLabels"
          :localdata="labelCheckboxes"
          multiple
          :map="{text:'name',value:'name'}"
        />
      </view>
    </view>

    <!-- 删除按钮（编辑模式显示） -->
    <view v-if="isEditMode" class="delete-section">
      <view class="delete-btn" @click="handleDelete">
        <uni-icons type="trash" size="16" color="#FF6B6B" />
        <text>删除待办</text>
      </view>
    </view>

    <!-- 选择执行者弹出层 -->
    <uni-popup ref="performerPopup" type="bottom" :safe-area="false">
      <view class="popup-container">
        <view class="popup-header">
          <text class="popup-title">选择执行者</text>
          <view class="popup-close" @click="closePerformerPicker">
            <uni-icons type="close" size="20" />
          </view>
        </view>
        <view class="popup-content">
          <view class="user-list">
            <view
              v-for="user in userList"
              :key="user.userId"
              class="user-item"
              :class="{ selected: selectedUserIds.includes(user.userId) }"
              @click="choiceUser(user)"
            >
              <view class="user-info">
                <uni-icons type="person-filled" size="18" color="#34D399" />
                <text class="user-name">{{ user.nickName }}</text>
              </view>
              <uni-icons
                :type="selectedUserIds.includes(user.userId) ? 'checkbox-filled' : 'circle'"
                :color="selectedUserIds.includes(user.userId) ? '#34D399' : '#D1D8E0'"
                size="22"
              />
            </view>
          </view>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script>
import { getDo, addDo, updateDo, delDo, getLabelList } from '@/api/px/life/todo'
import { listUser } from '@/api/system/user'
import uniPopup from '@/uni_modules/uni-popup/components/uni-popup/uni-popup.vue'

export default {
  name: 'TodoEdit',
  components: {
    uniPopup
  },
  data() {
    return {
      isEditMode: false,
      todoForm: {
        id: null,
        content: '',
        performer: '',
        performerName: '',
        planStartTime: '',
        planEndTime: '',
        label: '',
        status: false
      },
      labelList: [],
      selectedLabels: [],
      userList: [],
      selectedUserIds: []
    }
  },
  computed: {
    labelCheckboxes() {
      return this.labelList.map(label => ({
        name: label,
        value: label,
        disabled: false
      }))
    },
    labelsString() {
      return this.selectedLabels.join(',')
    },
    performerNameDisplay() {
      return this.todoForm.performerName || ''
    }
  },
  onLoad(options) {
    if (options.id) {
      this.isEditMode = true
      this.loadTodoDetail(options.id)
    } else {
      this.initNewTodo()
    }
    this.loadLabelList()
    this.loadUserList()
  },
  onNavigationBarButtonTap(e) {
    if (e.index === 0) {
      this.handleSave()
    }
  },
  methods: {
    initNewTodo() {
      const now = new Date()
      this.todoForm.planStartTime = this.formatDateTime(now)
      this.todoForm.planEndTime = this.formatDateTime(new Date(now.getTime() + 24 * 60 * 60 * 1000))
    },

    formatDateTime(date) {
      if (!date) return ''
      const d = new Date(date)
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      const hours = String(d.getHours()).padStart(2, '0')
      const minutes = String(d.getMinutes()).padStart(2, '0')
      const seconds = String(d.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    },

    async loadLabelList() {
      try {
        const response = await getLabelList()
        if (response.code === 200) {
          this.labelList = response.data || []
        }
      } catch (error) {
        console.error('加载标签列表失败:', error)
      }
    },

    async loadUserList() {
      try {
        const response = await listUser({})
        if (response.code === 200) {
          this.userList = response.rows || []
        }
      } catch (error) {
        console.error('加载用户列表失败:', error)
      }
    },

    openPerformerPicker() {
      this.$refs.performerPopup.open()
    },

    closePerformerPicker() {
      this.$refs.performerPopup.close()
    },

    handlePerformerChange(e) {
      this.selectedUserIds = e.detail.value
      this.updatePerformData()
    },

    choiceUser(user) {
      const index = this.selectedUserIds.indexOf(user.userId)
      if (index > -1) {
        this.selectedUserIds.splice(index, 1)
      } else {
        this.selectedUserIds.push(user.userId)
      }
      this.updatePerformData()
    },

    updatePerformData() {
      const userIds = this.selectedUserIds
      const users = this.userList.filter(user => userIds.includes(user.userId))
      
      this.todoForm.performer = userIds.join(',')
      this.todoForm.performerName = users.map(user => user.nickName).join(' | ')
    },

    async loadTodoDetail(id) {
      try {
        uni.showLoading({ title: '加载中...' })
        const response = await getDo(id)
        if (response.code === 200 && response.data) {
          this.todoForm = { ...response.data }
          this.selectedLabels = this.parseLabels(this.todoForm.label)
          
          // 解析执行者
          if (this.todoForm.performer) {
            const performerIds = this.todoForm.performer.split(',').filter(id => id)
            this.selectedUserIds = performerIds.map(id => parseInt(id))
          }
        }
      } catch (error) {
        console.error('加载待办详情失败:', error)
        uni.showToast({
          title: '加载失败',
          icon: 'none'
        })
      } finally {
        uni.hideLoading()
      }
    },

    parseLabels(labelString) {
      return labelString ? labelString.split(',').filter(l => l.trim()) : []
    },

    async handleSave() {
      if (!this.validateForm()) {
        return
      }

      try {
        uni.showLoading({ title: '保存中...' })

        const data = {
          ...this.todoForm,
          label: this.labelsString,
          performer: this.selectedUserIds.join(','),
          performerName: this.todoForm.performerName
        }

        let response
        if (this.isEditMode) {
          response = await updateDo(data)
        } else {
          response = await addDo(data)
        }

        if (response.code === 200) {
          uni.showToast({
            title: '保存成功',
            icon: 'success'
          })
          setTimeout(() => {
            uni.navigateBack()
          }, 1500)
        }
      } catch (error) {
        console.error('保存待办失败:', error)
        uni.showToast({
          title: '保存失败',
          icon: 'none'
        })
      } finally {
        uni.hideLoading()
      }
    },

    validateForm() {
      if (!this.todoForm.content || !this.todoForm.content.trim()) {
        uni.showToast({
          title: '请输入待办内容',
          icon: 'none'
        })
        return false
      }

      if (this.todoForm.planStartTime && this.todoForm.planEndTime) {
        const start = new Date(this.todoForm.planStartTime).getTime()
        const end = new Date(this.todoForm.planEndTime).getTime()
        if (start > end) {
          uni.showToast({
            title: '开始时间不能晚于结束时间',
            icon: 'none'
          })
          return false
        }
      }

      return true
    },

    handleDelete() {
      uni.showModal({
        title: '确认删除',
        content: '确定要删除这个待办吗？',
        success: async (res) => {
          if (res.confirm) {
            try {
              uni.showLoading({ title: '删除中...' })
              const response = await delDo(this.todoForm.id)
              if (response.code === 200) {
                uni.showToast({
                  title: '删除成功',
                  icon: 'success'
                })
                setTimeout(() => {
                  uni.navigateBack()
                }, 1500)
              }
            } catch (error) {
              console.error('删除待办失败:', error)
              uni.showToast({
                title: '删除失败',
                icon: 'none'
              })
            } finally {
              uni.hideLoading()
            }
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.todo-edit {
  min-height: 100vh;
  background-color: $bg-page;
  padding-bottom: 40rpx;

  .form-section {
    background-color: $bg-card;
    padding: 30rpx;
    margin-bottom: 20rpx;

    .form-label {
      font-size: $font-body;
      font-weight: 500;
      color: $text-primary;
      margin-bottom: 20rpx;

      .required {
        color: $danger;
        margin-left: 4rpx;
      }
    }

    .content-textarea {
      width: 100%;
      min-height: 200rpx;
      padding: 20rpx;
      background-color: $bg-page;
      border-radius: $radius-sm;
      font-size: $font-h3;
      line-height: 1.6;
      color: $text-primary;
    }

    .content-count {
      text-align: right;
      font-size: $font-caption;
      color: $text-tertiary;
      margin-top: 10rpx;
    }

    .tag-selector {
      ::v-deep .uni-data-checklist {
        .checklist-box {
          padding: 20rpx;
          margin-right: 20rpx;
          margin-bottom: 20rpx;
          background-color: $bg-page;
          border-radius: $radius-sm;

          &.is-checked {
            background-color: $todo;
            border-color: $todo;

            .checklist-text {
              color: #FFFFFF;
            }
          }

          .checklist-text {
            font-size: $font-body;
            color: $text-secondary;
          }
        }
      }
    }
  }

  .delete-section {
    margin-top: 40rpx;
    padding: 0 30rpx;

    .delete-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10rpx;
      padding: 30rpx;
      background-color: $bg-card;
      border-radius: $radius-sm;
      font-size: $font-body;
      color: $danger;
      transition: all $duration-fast;

      &:active {
        background-color: #FFF0F0;
      }
    }
  }

  .picker-input {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20rpx;
    background-color: $bg-page;
    border-radius: $radius-sm;

    .picker-value {
      flex: 1;
      font-size: $font-body;
      color: $text-primary;

      &.placeholder {
        color: $text-tertiary;
      }
    }
  }

  .popup-container {
    background-color: $bg-card;
    border-radius: $radius-xl $radius-xl 0 0;
    max-height: 70vh;
    display: flex;
    flex-direction: column;

    .popup-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 30rpx;
      border-bottom: 1rpx solid $border-color;

      .popup-title {
        font-size: $font-h2;
        font-weight: 600;
        color: $text-primary;
      }

      .popup-close {
        padding: 10rpx;

        ::v-deep .uni-icons {
          color: $text-tertiary;
        }
      }
    }

    .popup-content {
      flex: 1;
      overflow-y: auto;
      padding: 20rpx 0;

      .user-list {
        .user-item {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 24rpx 30rpx;
          border-bottom: 1rpx solid $bg-page;
          transition: all $duration-fast;

          &:last-child {
            border-bottom: none;
          }

          &.selected {
            background-color: rgba($todo, 0.08);
          }

          &:active {
            background-color: $bg-page;
          }

          .user-info {
            display: flex;
            align-items: center;
            gap: 12rpx;

            .user-name {
              font-size: $font-h3;
              color: $text-primary;
            }
          }
        }
      }
    }
  }
}
</style>
