<template>
  <view class="about-container subpage-shell">
    <view class="header-section">
      <view class="header-bg"></view>
      <view class="brand-area">
        <image class="brand-logo" src="/static/logo200.png" mode="widthFix"></image>
        <view class="brand-name">Pei你看雪</view>
        <view class="brand-slogan">陪伴你的生活每一天</view>
      </view>
    </view>

    <view class="content-section">
      <view class="menu-list">
        <view class="list-cell list-cell-arrow">
          <view class="menu-item-box">
            <view>版本信息</view>
            <view class="text-right">{{ version }}</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow">
          <view class="menu-item-box">
            <view>官方邮箱</view>
            <view class="text-right">pnkx0930@foxmail.com</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow">
          <view class="menu-item-box">
            <view>服务热线</view>
            <view class="text-right">15006732580</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow">
          <view class="menu-item-box">
            <view>公司网站</view>
            <view class="text-right">
              <uni-link :href="url" :text="url" showUnderLine="false"></uni-link>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="copyright">
      <view class="copyright-text">Copyright &copy; {{ new Date().getFullYear() }} pnkx.top All Rights Reserved.</view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      url: getApp().globalData.config.appInfo.site_url,
      version: getApp().globalData.config.appInfo.version
    }
  }
}
</script>

<style lang="scss" scoped>
page {
  background-color: $bg-page;
}

.about-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header-section {
  position: relative;
  overflow: hidden;
  padding: $spacing-2xl $spacing-lg $spacing-3xl;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, $primary 0%, $primary-dark 60%, $primary-darker 100%);
  z-index: 0;
}

.brand-area {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.brand-logo {
  width: 140rpx;
  height: 140rpx;
  border-radius: $radius-2xl;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.15);
}

.brand-name {
  color: $text-inverse;
  font-size: $font-h1;
  font-weight: $font-weight-bold;
  margin-top: $spacing-md;
  letter-spacing: 2rpx;
}

.brand-slogan {
  color: rgba(255, 255, 255, 0.75);
  font-size: $font-caption;
  margin-top: $spacing-xs;
}

.content-section {
  position: relative;
  margin-top: -$spacing-xl;
  z-index: 1;
}

.copyright {
  margin-top: auto;
  padding: $spacing-xl 0 $spacing-2xl;
  text-align: center;
}

.copyright-text {
  color: $text-tertiary;
  font-size: $font-small;
  line-height: $line-height-relaxed;
}
</style>
