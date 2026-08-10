<!--
 * @File: MoodWeatherPicker
 * @Author: PHY
 * @Date: 2025/03/09
 * @Description: 心情天气选择器组件
-->
<template>
  <view class="mood-weather-picker">
    <view class="picker-row">
      <view class="picker-item" @click="openMoodPicker">
        <view class="picker-label">心情</view>
        <view class="picker-content">
          <svg-icon v-if="mood" :icon-class="mood" size="32px" />
          <view v-else class="picker-placeholder">选择心情</view>
        </view>
        <uni-icons type="arrowright" size="14" color="#999999" />
      </view>
      <view class="picker-item" @click="openWeatherPicker">
        <view class="picker-label">天气</view>
        <view class="picker-content">
          <svg-icon v-if="weather" :icon-class="weather" size="32px" />
          <view v-else class="picker-placeholder">选择天气</view>
        </view>
        <uni-icons type="arrowright" size="14" color="#999999" />
      </view>
    </view>

    <uni-popup ref="moodPopup" type="bottom" :safe-area="false">
      <view class="popup-container">
        <view class="popup-header">
          <text class="popup-title">选择心情</text>
          <view class="popup-close" @click="closeMoodPicker">
            <uni-icons type="close" size="20" />
          </view>
        </view>
        <view class="popup-content">
          <IconSelect prefix="x-" :columns="5" @selected="onMoodSelected" />
        </view>
      </view>
    </uni-popup>

    <uni-popup ref="weatherPopup" type="bottom" :safe-area="false">
      <view class="popup-container">
        <view class="popup-header">
          <text class="popup-title">选择天气</text>
          <view class="popup-close" @click="closeWeatherPicker">
            <uni-icons type="close" size="20" />
          </view>
        </view>
        <view class="popup-content">
          <IconSelect prefix="w-" :columns="5" @selected="onWeatherSelected" />
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script>
import IconSelect from '@/components/IconSelect/index.vue'
import uniPopup from '@/uni_modules/uni-popup/components/uni-popup/uni-popup.vue'

export default {
  name: 'MoodWeatherPicker',
  components: {
    IconSelect,
    uniPopup
  },
  props: {
    mood: {
      type: String,
      default: ''
    },
    weather: {
      type: String,
      default: ''
    }
  },
  methods: {
    openMoodPicker() {
      this.$refs.moodPopup.open()
    },
    openWeatherPicker() {
      this.$refs.weatherPopup.open()
    },
    closeMoodPicker() {
      this.$refs.moodPopup.close()
    },
    closeWeatherPicker() {
      this.$refs.weatherPopup.close()
    },
    onMoodSelected(mood) {
      this.$emit('mood-change', mood)
      this.closeMoodPicker()
    },
    onWeatherSelected(weather) {
      this.$emit('weather-change', weather)
      this.closeWeatherPicker()
    }
  }
}
</script>

<style lang="scss" scoped>
.mood-weather-picker {
  .picker-row {
    display: flex;
    justify-content: space-between;
    gap: 20rpx;

    .picker-item {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 24rpx;
      background-color: $gray-50;
      border-radius: $radius-md;

      .picker-label {
        font-size: $font-body;
        color: $text-primary;
        font-weight: $font-weight-medium;
        margin-right: 16rpx;
      }

      .picker-content {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: center;

        .picker-placeholder {
          font-size: $font-caption;
          color: $text-tertiary;
        }
      }
    }
  }

  .popup-container {
    background-color: $bg-card;
    border-radius: $radius-xl $radius-xl 0 0;
    max-height: 80vh;
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
        font-weight: $font-weight-semibold;
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
    }
  }
}
</style>
