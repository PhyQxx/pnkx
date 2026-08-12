<template>
  <view class="diary-edit subpage-shell">
    <view class="edit-header">
      <view class="header-main">
        <text class="header-title">{{ isEditMode ? '编辑日记' : '写日记' }}</text>
        <text class="header-subtitle">{{ formattedDateText }}</text>
      </view>
      <view class="header-word-count">
        <text class="word-number">{{ contentLength }}</text>
        <text class="word-label">字</text>
      </view>
    </view>

    <view class="editor-card">
      <view class="field-row date-row">
        <view class="field-label">
          <uni-icons type="calendar" size="18" color="#A78BFA" />
          <text>日期</text>
        </view>
        <view class="date-picker-wrap">
          <uni-datetime-picker
            v-model="diaryForm.date"
            type="date"
            :clear-icon="false"
          />
        </view>
      </view>

      <view class="picker-section">
        <MoodWeatherPicker
          :mood="diaryForm.mood"
          :weather="diaryForm.weather"
          @mood-change="handleMoodChange"
          @weather-change="handleWeatherChange"
        />
      </view>

      <view class="title-section">
        <input
          v-model="diaryForm.title"
          class="title-input"
          placeholder="给今天起个标题"
          placeholder-class="title-placeholder"
          maxlength="100"
        />
      </view>

      <view class="content-section">
        <textarea
          v-model="diaryForm.content"
          class="content-textarea"
          placeholder="写下今天发生了什么，或只是此刻的心情..."
          placeholder-class="content-placeholder"
          :maxlength="10000"
          auto-height
        />
      </view>
    </view>

    <view v-if="diaryForm.id" class="danger-zone">
      <view class="delete-btn" @click="handleDelete">
        <uni-icons type="trash" size="17" color="#FF6B6B" />
        <text>删除这篇日记</text>
      </view>
    </view>

    <view class="bottom-actions">
      <view class="save-hint">
        <text>{{ saveHint }}</text>
      </view>
      <view class="save-btn" @click="handleSave">
        <uni-icons type="checkmarkempty" size="22" color="#FFFFFF" />
        <text>保存</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getDiary, addDiary, updateDiary, delDiary } from '@/api/px/life/diary'
import { formatDate } from '@/utils/pnkx'
import MoodWeatherPicker from '@/components/MoodWeatherPicker/index.vue'

export default {
  name: 'DiaryEdit',
  components: {
    MoodWeatherPicker
  },
  data() {
    return {
      diaryForm: {
        id: null,
        title: '',
        content: '',
        mood: '',
        weather: '',
        date: ''
      },
      isEditMode: false,
      saving: false
    }
  },
  computed: {
    contentLength() {
      return (this.diaryForm.content || '').length
    },
    formattedDateText() {
      if (!this.diaryForm.date) return '选择一个日期'
      const date = new Date(this.diaryForm.date)
      if (Number.isNaN(date.getTime())) return this.diaryForm.date
      const weekList = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${weekList[date.getDay()]}`
    },
    saveHint() {
      if (!this.diaryForm.date) return '先选择日期'
      if (!this.contentLength) return '正文还空着'
      return this.diaryForm.title ? '准备好了' : '未填标题将自动生成'
    }
  },
  onLoad(options) {
    if (options.id && options.id !== 'undefined') {
      this.isEditMode = true
      uni.setNavigationBarTitle({ title: '编辑日记' })
      this.loadDiaryDetail(options.id)
    } else {
      this.isEditMode = false
      uni.setNavigationBarTitle({ title: '写日记' })
      this.initNewDiary()
    }
  },
  onNavigationBarButtonTap(e) {
    if (e.index === 0) {
      this.handleSave()
    }
  },
  methods: {
    initNewDiary() {
      const now = new Date()
      this.diaryForm.date = formatDate(now)
    },

    async loadDiaryDetail(id) {
      try {
        uni.showLoading({ title: '加载中...' })
        const response = await getDiary(id)
        if (response.code === 200 && response.data) {
          const data = response.data
          this.diaryForm = {
            id: data.id || id,
            title: data.title || '',
            content: data.content || data.richText || '',
            mood: data.mood || '',
            weather: data.weather || '',
            date: this.formatDate(data.date) || ''
          }
        }
      } catch (error) {
        console.error('加载日记详情失败:', error)
        uni.showToast({
          title: '加载失败',
          icon: 'none'
        })
      } finally {
        uni.hideLoading()
      }
    },

    handleMoodChange(mood) {
      this.diaryForm.mood = mood
    },

    handleWeatherChange(weather) {
      this.diaryForm.weather = weather
    },

    async handleSave() {
      if (this.saving || !this.validateForm()) {
        return
      }

      this.saving = true

      try {
        uni.showLoading({ title: '保存中...' })

        const data = {
          ...this.diaryForm,
          title: this.diaryForm.title || this.buildDefaultTitle(),
          richText: this.diaryForm.content
        }

        const response = this.isEditMode ? await updateDiary(data) : await addDiary(data)

        if (response.code === 200) {
          uni.showToast({
            title: '保存成功',
            icon: 'success'
          })
          setTimeout(() => {
            uni.navigateBack()
          }, 800)
        }
      } catch (error) {
        console.error('保存日记失败:', error)
        uni.showToast({
          title: '保存失败',
          icon: 'none'
        })
      } finally {
        this.saving = false
        uni.hideLoading()
      }
    },

    buildDefaultTitle() {
      const date = this.diaryForm.date || this.formatDate(new Date())
      return `${date} 随记`
    },

    validateForm() {
      if (!this.diaryForm.date) {
        uni.showToast({
          title: '请选择日期',
          icon: 'none'
        })
        return false
      }

      if (!this.diaryForm.content || !this.diaryForm.content.trim()) {
        uni.showToast({
          title: '请写一点内容',
          icon: 'none'
        })
        return false
      }

      return true
    },

    handleDelete() {
      uni.showModal({
        title: '删除确认',
        content: '确定删除这篇日记吗？',
        success: async (res) => {
          if (res.confirm) {
            try {
              uni.showLoading({ title: '删除中...' })
              const response = await delDiary(this.diaryForm.id)
              if (response.code === 200) {
                uni.showToast({
                  title: '删除成功',
                  icon: 'success'
                })
                setTimeout(() => {
                  uni.navigateBack()
                }, 800)
              }
            } catch (error) {
              console.error('删除日记失败:', error)
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
.diary-edit {
  min-height: 100vh;
  padding: 28rpx $page-padding 170rpx;
  box-sizing: border-box;
  background: linear-gradient(180deg, rgba($diary, 0.16) 0%, $bg-page 420rpx);

  .edit-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: $spacing-lg;

    .header-main {
      display: flex;
      flex-direction: column;
      gap: 8rpx;

      .header-title {
        font-size: 42rpx;
        font-weight: $font-weight-bold;
        color: $text-primary;
        line-height: 1.2;
      }

      .header-subtitle {
        font-size: $font-caption;
        color: $text-secondary;
      }
    }

    .header-word-count {
      width: 116rpx;
      height: 116rpx;
      border-radius: $radius-full;
      background-color: rgba($diary, 0.12);
      border: 1rpx solid rgba($diary, 0.18);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;

      .word-number {
        max-width: 90rpx;
        font-size: 30rpx;
        font-weight: $font-weight-bold;
        color: $diary;
        line-height: 1.1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .word-label {
        margin-top: 4rpx;
        font-size: $font-mini;
        color: $text-tertiary;
      }
    }
  }

  .editor-card {
    overflow: hidden;
    border-radius: $radius-lg;
    background-color: $bg-card;
    border: 1rpx solid rgba($diary, 0.12);
    box-shadow: 0 10rpx 30rpx rgba(74, 85, 104, 0.07);
  }

  .field-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: $spacing-md;
    padding: 26rpx;
    border-bottom: 1rpx solid $border-light;

    .field-label {
      display: flex;
      align-items: center;
      gap: 10rpx;
      font-size: $font-body;
      font-weight: $font-weight-semibold;
      color: $text-primary;
    }

    .date-picker-wrap {
      flex: 1;
      min-width: 0;
    }
  }

  .picker-section {
    padding: 26rpx;
    border-bottom: 1rpx solid $border-light;
  }

  .title-section {
    padding: 30rpx 30rpx 20rpx;
    border-bottom: 1rpx solid $border-light;

    .title-input {
      width: 100%;
      min-height: 58rpx;
      font-size: 38rpx;
      line-height: 1.35;
      font-weight: $font-weight-semibold;
      color: $text-primary;
    }
  }

  .content-section {
    padding: 28rpx 30rpx 34rpx;

    .content-textarea {
      width: 100%;
      min-height: 560rpx;
      font-size: $font-h3;
      line-height: 1.75;
      color: $text-primary;
      background-color: transparent;
    }
  }

  .title-placeholder,
  .content-placeholder {
    color: $text-disabled;
  }

  .danger-zone {
    margin-top: $spacing-lg;

    .delete-btn {
      height: 92rpx;
      border-radius: $radius-md;
      background-color: rgba($danger, 0.08);
      color: $danger;
      font-size: $font-body;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10rpx;
      transition: background-color $duration-fast $ease-default;

      &:active {
        background-color: rgba($danger, 0.14);
      }
    }
  }

  .bottom-actions {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: $z-fixed;
    padding: 18rpx $page-padding calc(18rpx + env(safe-area-inset-bottom));
    background-color: rgba(255, 255, 255, 0.96);
    box-shadow: 0 -8rpx 24rpx rgba(74, 85, 104, 0.08);
    display: flex;
    align-items: center;
    gap: $spacing-md;

    .save-hint {
      flex: 1;
      min-width: 0;
      font-size: $font-caption;
      color: $text-tertiary;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .save-btn {
      width: 228rpx;
      height: 88rpx;
      border-radius: $radius-full;
      background: linear-gradient(135deg, $diary 0%, #EC4899 100%);
      color: $text-inverse;
      font-size: $font-body;
      font-weight: $font-weight-semibold;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8rpx;
      box-shadow: 0 10rpx 24rpx rgba($diary, 0.3);
      transition: transform $duration-fast $ease-spring;

      &:active {
        transform: scale(0.96);
      }
    }
  }
}
</style>
