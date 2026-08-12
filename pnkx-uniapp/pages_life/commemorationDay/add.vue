<!--
 * @File: add
 * @Author: PHY
 * @Date: 2025/03/10
 * @Description: 纪念日编辑页
-->
<template>
  <view class="commemoration-add-page subpage-shell">
    <view class="form-section">
      <view class="form-item">
        <view class="form-label">纪念日名称</view>
        <uni-easyinput
          v-model="formData.name"
          placeholder="请输入纪念日名称"
          :clearable="true"
          :maxlength="50"
        />
      </view>
    </view>

    <view class="form-section">
      <view class="form-item">
        <view class="form-label">纪念日日期</view>
        <uni-datetime-picker
          v-model="formData.date"
          type="date"
          :clear-icon="false"
        />
      </view>
    </view>

    <view class="form-section">
      <view class="form-item" @click="showRepeatPicker = true">
        <view class="form-label">重复提醒</view>
        <view class="form-value">
          <text>{{ formData.repeat ? '每年重复' : '不重复' }}</text>
          <uni-icons type="right" size="16" color="$text-disabled" />
        </view>
      </view>
    </view>

    <view class="form-section">
      <view class="form-item" @click="handleSelectIcon">
        <view class="form-label">选择图标</view>
        <view class="form-value">
          <view class="icon-preview">
            <svg-icon :icon-class="formData.icon || '纪念日'" size="24px" />
          </view>
          <uni-icons type="right" size="16" color="$text-disabled" />
        </view>
      </view>
    </view>

    <view class="form-section">
      <view class="form-item textarea-item">
        <view class="form-label">备注</view>
        <textarea
          v-model="formData.remark"
          class="remark-textarea"
          placeholder="请输入备注信息..."
          :maxlength="200"
          auto-height
        />
        <view class="word-count">{{ formData.remark.length }}/200</view>
      </view>
    </view>

    <view v-if="formData.id" class="delete-section">
      <view class="delete-btn" @click="handleDelete">
        <uni-icons type="trash" size="16" color="#FF4D4F" />
        <text>删除纪念日</text>
      </view>
    </view>

    <uni-popup ref="repeatPopup" type="bottom" :safe-area="true">
      <view class="picker-popup">
        <view class="picker-header">
          <text class="picker-cancel" @click="showRepeatPicker = false">取消</text>
          <text class="picker-title">重复提醒</text>
          <text class="picker-confirm" @click="confirmRepeat">确定</text>
        </view>
        <view class="picker-options">
          <view
            class="picker-option"
            :class="{ active: tempRepeat === false }"
            @click="tempRepeat = false"
          >
            <text>不重复</text>
            <uni-icons v-if="tempRepeat === false" type="checkmarkempty" size="20" color="#FB923C" />
          </view>
          <view
            class="picker-option"
            :class="{ active: tempRepeat === true }"
            @click="tempRepeat = true"
          >
            <text>每年重复</text>
            <uni-icons v-if="tempRepeat === true" type="checkmarkempty" size="20" color="#FB923C" />
          </view>
        </view>
      </view>
    </uni-popup>

    <IconPicker ref="iconPicker" v-model="formData.icon" />
  </view>
</template>

<script>
import { getDay, addDay, updateDay, delDay } from '@/api/px/life/commemorationDay'
import IconPicker from '../components/IconPicker.vue'
import uniPopup from '@/uni_modules/uni-popup/components/uni-popup/uni-popup.vue'

export default {
  name: 'CommemorationDayAdd',
  components: {
    IconPicker,
    uniPopup
  },
  data() {
    return {
      formData: {
        id: null,
        name: '',
        date: '',
        repeat: false,
        icon: '纪念日',
        remark: ''
      },
      isEditMode: false,
      showRepeatPicker: false,
      tempRepeat: false
    }
  },
  watch: {
    showRepeatPicker(val) {
      if (val) {
        this.tempRepeat = this.formData.repeat
        this.$refs.repeatPopup.open()
      } else {
        this.$refs.repeatPopup.close()
      }
    }
  },
  onLoad(options) {
    if (options.id) {
      this.isEditMode = true
      this.loadCommemorationDetail(options.id)
    } else {
      this.initNewCommemoration()
    }
  },
  onNavigationBarButtonTap(e) {
    if (e.index === 0) {
      this.handleSave()
    }
  },
  methods: {
    initNewCommemoration() {
      const now = new Date()
      this.formData.date = this.formatDate(now)
    },

    formatDate(date) {
      if (!date) return ''
      const d = new Date(date)
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    },

    async loadCommemorationDetail(id) {
      try {
        uni.showLoading({ title: '加载中...' })
        const response = await getDay(id)
        if (response.code === 200 && response.data) {
          this.formData = {
            ...this.formData,
            ...response.data
          }
        }
      } catch (error) {
        console.error('加载纪念日详情失败:', error)
        uni.showToast({
          title: '加载失败',
          icon: 'none'
        })
      } finally {
        uni.hideLoading()
      }
    },

    handleSelectIcon() {
      this.$refs.iconPicker.open()
    },

    confirmRepeat() {
      this.formData.repeat = this.tempRepeat
      this.showRepeatPicker = false
    },

    async handleSave() {
      if (!this.validateForm()) {
        return
      }

      try {
        uni.showLoading({ title: '保存中...' })
        // 时间格式为yyyy-MM-dd HH:mm:ss
        this.formData.date = this.$moment(this.formData.date).format('YYYY-MM-DD HH:mm:ss')
        let response
        if (this.isEditMode) {
          response = await updateDay(this.formData)
        } else {
          response = await addDay(this.formData)
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
        console.error('保存纪念日失败:', error)
        uni.showToast({
          title: '保存失败',
          icon: 'none'
        })
      } finally {
        uni.hideLoading()
      }
    },

    validateForm() {
      if (!this.formData.name) {
        uni.showToast({
          title: '请输入纪念日名称',
          icon: 'none'
        })
        return false
      }

      if (!this.formData.date) {
        uni.showToast({
          title: '请选择纪念日日期',
          icon: 'none'
        })
        return false
      }

      return true
    },

    handleDelete() {
      uni.showModal({
        title: '确认删除',
        content: '确定要删除这个纪念日吗？',
        success: async (res) => {
          if (res.confirm) {
            try {
              uni.showLoading({ title: '删除中...' })
              const response = await delDay(this.formData.id)
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
              console.error('删除纪念日失败:', error)
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
.commemoration-add-page {
  min-height: 100vh;
  background-color: $bg-page;
  padding-bottom: $spacing-2xl;

  .form-section {
    background-color: $bg-card;
    padding: 0 $spacing-lg;
    margin-bottom: $spacing-md;

    .form-item {
      display: flex;
      align-items: center;
      padding: $spacing-lg 0;
      border-bottom: 1rpx solid $border-light;

      &:last-child {
        border-bottom: none;
      }

      &.textarea-item {
        flex-direction: column;
        align-items: flex-start;
      }

      .form-label {
        font-size: $font-body;
        color: $text-primary;
        width: 160rpx;
        flex-shrink: 0;
      }

      .form-value {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: flex-end;
        font-size: $font-body;
        color: $text-secondary;

        .icon-preview {
          width: 48rpx;
          height: 48rpx;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-right: $spacing-xs;
        }
      }

      .remark-textarea {
        width: 100%;
        min-height: 200rpx;
        padding: 20rpx;
        background-color: $gray-50;
        border-radius: $radius-md;
        font-size: $font-body;
        line-height: 1.6;
        color: $text-primary;
        margin-top: $spacing-md;
      }

      .word-count {
        width: 100%;
        text-align: right;
        font-size: $font-caption;
        color: $text-tertiary;
        margin-top: $spacing-xs;
      }
    }
  }

  .delete-section {
    margin-top: $spacing-2xl;
    padding: 0 $spacing-lg;

    .delete-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: $spacing-xs;
      padding: $spacing-lg;
      background-color: $bg-card;
      border-radius: $radius-md;
      font-size: $font-body;
      color: $danger;
      transition: all $duration-fast $ease-default;

      &:active {
        background-color: $danger-light;
      }
    }
  }

  .picker-popup {
    background-color: $bg-card;
    border-radius: $radius-xl 24rpx 0 0;

    .picker-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: $spacing-lg;
      border-bottom: 1rpx solid $border-light;

      .picker-cancel {
        font-size: $font-body;
        color: $text-tertiary;
      }

      .picker-title {
        font-size: $font-h2;
        font-weight: $font-weight-semibold;
        color: $text-primary;
      }

      .picker-confirm {
        font-size: $font-body;
        color: $commemoration;
      }
    }

    .picker-options {
      padding: 20rpx 0;

      .picker-option {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: $spacing-lg;
        font-size: $font-body;
        color: $text-primary;

        &.active {
          color: $commemoration;
          background-color: rgba($commemoration, 0.12);
        }
      }
    }
  }
}
</style>
