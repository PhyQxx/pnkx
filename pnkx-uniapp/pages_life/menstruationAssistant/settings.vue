<!--
  * @File: settings
  * @Author: PHY
  * @Date: 2025/03/10
  * @Description: 经期设置页
-->
<template>
  <view class="settings-page">
    <view class="settings-hero">
      <text class="settings-hero__eyebrow">预测偏好</text>
      <text class="settings-hero__title">让提醒更贴近你的节律</text>
      <text class="settings-hero__text">周期和经期时长会用于主页预测，不会影响历史记录。</text>
    </view>

    <view class="settings-section">
      <view class="setting-card">
        <view class="setting-card__main">
          <text class="setting-card__label">经期周期</text>
          <text class="setting-card__tip">从本次经期第一天到下次经期第一天</text>
        </view>
        <view class="setting-card__control">
          <uni-number-box v-model="cycle" :min="21" :max="35" />
          <text class="setting-card__unit">天</text>
        </view>
      </view>

      <view class="setting-card">
        <view class="setting-card__main">
          <text class="setting-card__label">经期时长</text>
          <text class="setting-card__tip">每次经期通常持续的天数</text>
        </view>
        <view class="setting-card__control">
          <uni-number-box v-model="duration" :min="2" :max="8" />
          <text class="setting-card__unit">天</text>
        </view>
      </view>
    </view>

    <view class="info-section">
      <view class="info-section__header">
        <view class="info-section__icon">
          <uni-icons type="info" size="18" color="#2563EB" />
        </view>
        <text>参考范围</text>
      </view>
      <view class="info-list">
        <view class="info-item">
          <text class="info-item__dot"></text>
          <text>常见月经周期为 21-35 天，平均约 28 天。</text>
        </view>
        <view class="info-item">
          <text class="info-item__dot"></text>
          <text>常见经期时长为 2-8 天，平均约 4-6 天。</text>
        </view>
        <view class="info-item">
          <text class="info-item__dot"></text>
          <text>预测仅作记录参考，身体不适时建议及时就医。</text>
        </view>
      </view>
    </view>

    <view class="bottom-bar">
      <button class="save-btn" @click="handleSave">保存设置</button>
    </view>
  </view>
</template>

<script>
export default {
  name: 'MenstruationAssistantSettings',
  data() {
    return {
      cycle: 28,
      duration: 5
    }
  },
  onLoad() {
    this.loadSettings()
  },
  methods: {
    loadSettings() {
      const cycle = uni.getStorageSync('menstruation_cycle')
      const duration = uni.getStorageSync('menstruation_duration')
      if (cycle) this.cycle = parseInt(cycle)
      if (duration) this.duration = parseInt(duration)
    },

    handleSave() {
      uni.setStorageSync('menstruation_cycle', this.cycle.toString())
      uni.setStorageSync('menstruation_duration', this.duration.toString())

      uni.showToast({
        title: '保存成功',
        icon: 'success'
      })

      setTimeout(() => {
        uni.navigateBack()
      }, 800)
    }
  }
}
</script>

<style lang="scss" scoped>
.settings-page {
  min-height: 100vh;
  padding: 24rpx 30rpx 140rpx;
  background: #F7F8FB;
}

.settings-hero {
  padding: 30rpx;
  border-radius: 24rpx;
  margin-bottom: 22rpx;
  background: linear-gradient(135deg, #FFF1F6 0%, #FFFFFF 60%, #EEF6FF 100%);
  box-shadow: 0 12rpx 32rpx rgba(244, 114, 182, 0.1);

  &__eyebrow {
    display: block;
    font-size: 24rpx;
    color: #BE185D;
    font-weight: 600;
  }

  &__title {
    display: block;
    margin-top: 10rpx;
    font-size: 38rpx;
    line-height: 1.25;
    font-weight: 800;
    color: #1A202C;
  }

  &__text {
    display: block;
    margin-top: 12rpx;
    font-size: 25rpx;
    line-height: 1.5;
    color: #6B7B8D;
  }
}

.settings-section {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.setting-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  padding: 26rpx;
  border-radius: 18rpx;
  background: #FFFFFF;

  &__main {
    flex: 1;
    min-width: 0;
  }

  &__label {
    display: block;
    font-size: 30rpx;
    color: #1A202C;
    font-weight: 700;
  }

  &__tip {
    display: block;
    margin-top: 8rpx;
    font-size: 23rpx;
    line-height: 1.45;
    color: #9BA8B7;
  }

  &__control {
    display: flex;
    align-items: center;
    gap: 12rpx;
    flex-shrink: 0;
  }

  &__unit {
    font-size: 24rpx;
    color: #6B7B8D;
  }
}

.info-section {
  padding: 26rpx;
  margin-top: 20rpx;
  border-radius: 18rpx;
  background: #FFFFFF;

  &__header {
    display: flex;
    align-items: center;
    gap: 12rpx;
    margin-bottom: 20rpx;

    text {
      font-size: 30rpx;
      color: #1A202C;
      font-weight: 700;
    }
  }

  &__icon {
    width: 42rpx;
    height: 42rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: #DBEAFE;
  }
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;

  &__dot {
    width: 10rpx;
    height: 10rpx;
    margin-top: 16rpx;
    border-radius: 50%;
    background: #F472B6;
    flex-shrink: 0;
  }

  text:last-child {
    flex: 1;
    min-width: 0;
    font-size: 25rpx;
    line-height: 1.55;
    color: #4A5568;
  }
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 18rpx 30rpx 24rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -8rpx 24rpx rgba(0, 0, 0, 0.06);
}

.save-btn {
  width: 100%;
  height: 90rpx;
  line-height: 90rpx;
  color: #FFFFFF;
  background: linear-gradient(135deg, #F472B6 0%, #DB2777 100%);
  border: none;
  border-radius: 18rpx;
  font-size: 30rpx;
  font-weight: 700;
}
</style>
