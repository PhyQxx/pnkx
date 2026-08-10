<template>
  <view class="setting-container" :style="{height: `${windowHeight}px`}">
    <view class="menu-list">
      <view class="list-cell list-cell-arrow" @click="handleToPwd">
        <view class="menu-item-box">
          <view class="iconfont icon-password menu-icon"></view>
          <view>修改密码</view>
        </view>
      </view>
      <view class="list-cell list-cell-arrow" @click="handleToUpgrade">
        <view class="menu-item-box">
          <view class="iconfont icon-refresh menu-icon"></view>
          <view>检查更新</view>
        </view>
      </view>
      <view class="list-cell list-cell-arrow" @click="handleCleanTmp">
        <view class="menu-item-box">
          <view class="iconfont icon-clean menu-icon"></view>
          <view>清理缓存</view>
        </view>
      </view>
    </view>
    <view class="logout-section">
      <view class="logout-btn" @click="handleLogout">
        <text class="logout-text">退出登录</text>
      </view>
    </view>

    <!-- Update Dialog -->
    <uni-popup ref="updateDialog" type="dialog" :is-mask-click="false">
      <uni-popup-dialog type="info" confirmText="后台下载" title="正在下载更新" @confirm="backgroundDownload" :showClose="false">
        <view>下载进度
          <text class="theme-blue" style="padding: 0 0.2rem">{{ percentageNum }}</text>
          %
        </view>
      </uni-popup-dialog>
    </uni-popup>
  </view>
</template>

<script>
import checkUpdate from '@/utils/update'

import uniPopup from '@/uni_modules/uni-popup/components/uni-popup/uni-popup.vue';
import uniPopupDialog from '@/uni_modules/uni-popup/components/uni-popup-dialog/uni-popup-dialog.vue';

export default {
  components: {
    uniPopup,
    uniPopupDialog
  },
  data() {
    return {
      windowHeight: uni.getSystemInfoSync().windowHeight,
      percentageNum: 0
    }
  },
  methods: {
    handleToPwd() {
      this.$tab.navigateTo('/pages_system/mine/pwd/index')
    },
    handleToUpgrade() {
      checkUpdate({
        showToast: true,
        onDownloadStart: () => {
          this.percentageNum = 0;
          this.$refs.updateDialog.open();
        },
        onProgress: (progress) => {
          this.percentageNum = progress;
        },
        onDownloadComplete: () => {
          this.$refs.updateDialog.close();
        }
      });
    },
    backgroundDownload() {
      this.$refs.updateDialog.close();
    },
    handleCleanTmp() {
      uni.showModal({
        title: '清理缓存',
        content: '清理缓存后将退出登录，确认清理缓存？',
        success: (res) => {
          if (res.confirm) {
            try {
              uni.clearStorageSync();
              this.$store.dispatch('LogOut').then(() => {
                this.$tab.reLaunch('/pages/index')
              })
            } catch (e) {
              // error
            }
          }
        }
      })
    },
    handleLogout() {
      this.$modal.confirm('确定注销并退出系统吗？').then(() => {
        this.$store.dispatch('LogOut').then(() => {
          this.$tab.reLaunch('/pages/index')
        })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.setting-container {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.logout-section {
  padding: $spacing-lg $page-padding;
}

.logout-btn {
  background-color: $bg-card;
  border-radius: $radius-xl;
  box-shadow: $shadow-card;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: $spacing-md;
  transition: all $duration-fast $ease-default;

  &:active {
    transform: scale(0.97);
    background-color: $bg-hover;
  }
}

.logout-text {
  color: $danger;
  font-size: $font-h3;
  font-weight: $font-weight-medium;
}

/* List Cell */
.menu-list {
  background-color: #ffffff;
  margin-bottom: $spacing-md;
}

.list-cell {
  position: relative;
  display: flex;
  box-sizing: border-box;
  width: 100%;
  padding: 26rpx 32rpx;
  font-size: 28rpx;
  line-height: 48rpx;
  color: #333;
  background-color: #fff;
  align-items: center;
}

.list-cell-arrow::before {
  content: " ";
  display: inline-block;
  height: 12rpx;
  width: 12rpx;
  border-width: 4rpx 4rpx 0 0;
  border-color: #c0c0c0;
  border-style: solid;
  transform: matrix(0.71, 0.71, -0.71, 0.71, 0, 0);
  position: absolute;
  top: 50%;
  margin-top: -8rpx;
  right: 32rpx;
}

.menu-item-box {
  width: 100%;
  display: flex;
  align-items: center;
}

.menu-icon {
  margin-right: 10rpx;
  color: #5A8DEE;
  font-size: 32rpx;
}
</style>