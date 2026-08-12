<template>
  <view class="mine-page">
    <!-- Gradient header with wave bottom -->
    <view class="header-section">
      <view class="header-bg">
        <view class="mine-navbar">
          <text class="mine-navbar__title">我的</text>
          <view class="mine-navbar__setting" @click="handleToSetting">
            <view class="iconfont icon-setting"></view>
          </view>
        </view>
        <view class="header-content">
          <view class="user-row">
            <view class="avatar-wrapper" @click="handleToAvatar">
              <view v-if="!avatar" class="avatar-placeholder">
                <view class="iconfont icon-people avatar-icon"></view>
              </view>
              <image v-if="avatar" :src="avatar" class="avatar-image" mode="aspectFill" />
              <view class="avatar-ring"></view>
            </view>
            <view class="user-meta">
              <view v-if="!name" class="login-tip" @click="handleToLogin">
                点击登录
              </view>
              <view v-if="name" class="username" @click="handleToInfo">
                {{ name }}
              </view>
              <view v-if="name" class="user-desc">记录生活，陪伴每一刻</view>
              <view v-if="name" class="companionship">Pei你看雪 · 温柔陪伴每一天</view>
            </view>
          </view>
          <view class="info-link" @click="handleToInfo">
            <text class="info-link-text">个人信息</text>
            <view class="iconfont icon-right info-link-arrow"></view>
          </view>
        </view>
      </view>
      <!-- Wave divider -->
      <view class="header-wave">
        <view class="wave-curve"></view>
      </view>
    </view>

    <!-- Menu sections -->
    <view class="menu-section">
      <view class="menu-card">
        <!-- 个人资料 -->
        <view class="menu-item" @click="handleToEditInfo" hover-class="menu-item-active">
          <view class="stat-icon-wrapper stat-icon-pink">
            <view class="iconfont icon-edit stat-icon"></view>
          </view>
          <view class="menu-item-content">
            <text class="menu-item-title">编辑资料</text>
            <text class="menu-item-desc">修改个人资料</text>
          </view>
          <view class="menu-item-arrow">
            <view class="iconfont icon-right"></view>
          </view>
        </view>

        <!-- Divider -->
        <view class="menu-divider"></view>

        <!-- Help -->
        <view class="menu-item" @click="handleHelp" hover-class="menu-item-active">
          <view class="menu-icon-circle menu-icon-blue">
            <view class="iconfont icon-help menu-item-icon"></view>
          </view>
          <view class="menu-item-content">
            <text class="menu-item-title">常见问题</text>
            <text class="menu-item-desc">查看使用帮助与常见问题解答</text>
          </view>
          <view class="menu-item-arrow">
            <view class="iconfont icon-right"></view>
          </view>
        </view>

        <!-- Divider -->
        <view class="menu-divider"></view>

        <!-- About -->
        <view class="menu-item" @click="handleAbout" hover-class="menu-item-active">
          <view class="menu-icon-circle menu-icon-pink">
            <view class="iconfont icon-aixin menu-item-icon"></view>
          </view>
          <view class="menu-item-content">
            <text class="menu-item-title">关于我们</text>
            <text class="menu-item-desc">了解 Pei你看雪 的故事</text>
          </view>
          <view class="menu-item-arrow">
            <view class="iconfont icon-right"></view>
          </view>
        </view>

        <!-- Divider -->
        <view class="menu-divider"></view>

        <!-- Settings -->
        <view class="menu-item" @click="handleToSetting" hover-class="menu-item-active">
          <view class="menu-icon-circle menu-icon-green">
            <view class="iconfont icon-setting menu-item-icon"></view>
          </view>
          <view class="menu-item-content">
            <text class="menu-item-title">应用设置</text>
            <text class="menu-item-desc">通知、隐私与账号管理</text>
          </view>
          <view class="menu-item-arrow">
            <view class="iconfont icon-right"></view>
          </view>
        </view>
      </view>
    </view>

    <!-- Logout button -->
    <view class="logout-section" v-if="name">
      <view class="logout-btn" @click="handleLogout" hover-class="logout-btn-active">
        <view class="iconfont icon-logout logout-icon"></view>
        <text>退出登录</text>
      </view>
    </view>

    <!-- Footer version -->
    <view class="page-footer">
      <text class="version-text">Pei你看雪 v{{ version }}</text>
    </view>
  </view>
</template>

<script>
import { getAppVersion } from '@/utils/version'

export default {
  data() {
    return {
      name: this.$store.state.user.name,
      version: getAppVersion()
    }
  },
  computed: {
    avatar() {
      return this.$store.state.user.avatar
    },
    windowHeight() {
      return uni.getSystemInfoSync().windowHeight - 1
    }
  },
  methods: {
    handleToInfo() {
      this.$tab.navigateTo('/pages_system/mine/info/index')
    },
    handleToEditInfo() {
      this.$tab.navigateTo('/pages_system/mine/info/edit')
    },
    handleToSetting() {
      this.$tab.navigateTo('/pages_system/mine/setting/index')
    },
    handleToLogin() {
      this.$tab.reLaunch('/pages/login')
    },
    handleToAvatar() {
      this.$tab.navigateTo('/pages_system/mine/avatar/index')
    },
    async handleLogout() {
      await this.$modal.confirm('确定注销并退出系统吗？')
      await this.$store.dispatch('LogOut')
      this.$tab.reLaunch('/pages/index')
    },
    handleHelp() {
      this.$tab.navigateTo('/pages_system/mine/help/index')
    },
    handleAbout() {
      this.$tab.navigateTo('/pages_system/mine/about/index')
    },
    handleBuilding() {
      this.$modal.showToast('模块建设中~')
    }
  }
}
</script>

<style lang="scss" scoped>
/* Page background */
page {
  background-color: $bg-page;
}

.mine-page {
  min-height: 100vh;
  background-color: $bg-page;
  padding-bottom: $spacing-3xl;
}

/* ================================
   Header Section
   ================================ */
.header-section {
  position: relative;
  overflow: hidden;
}

.header-bg {
  background:
    linear-gradient(180deg, rgba(248, 251, 255, 0.02), rgba(242, 247, 254, 0.18)),
    url('/static/images/glacier-aurora-bg.png') top center / cover no-repeat;
  padding: calc(var(--status-bar-height, 44px) + 24rpx) $page-padding 112rpx;
  position: relative;
}

.mine-navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 42rpx;

  &__title {
    color: $text-primary;
    font-size: 44rpx;
    font-weight: $font-weight-bold;
  }

  &__setting {
    width: 76rpx;
    height: 76rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    color: $text-primary;
    font-size: 36rpx;
    border-radius: $radius-full;
    background: rgba(255, 255, 255, 0.84);
    border: 1rpx solid rgba(255, 255, 255, 0.92);
    box-shadow: $shadow-md;
  }
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.user-row {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}

.avatar-wrapper {
  position: relative;
  width: 128rpx;
  height: 128rpx;
  flex-shrink: 0;
}

.avatar-image {
  width: 128rpx;
  height: 128rpx;
  border-radius: $radius-full;
  border: 6rpx solid rgba(255, 255, 255, 0.4);
}

.avatar-placeholder {
  width: 128rpx;
  height: 128rpx;
  border-radius: $radius-full;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 6rpx solid rgba(255, 255, 255, 0.4);

  .avatar-icon {
    font-size: 56rpx;
    color: rgba(255, 255, 255, 0.8);
  }
}

.avatar-ring {
  position: absolute;
  top: -6rpx;
  left: -6rpx;
  right: -6rpx;
  bottom: -6rpx;
  border-radius: $radius-full;
  border: 4rpx solid rgba(255, 255, 255, 0.15);
  pointer-events: none;
}

.user-meta {
  margin-left: $spacing-md;
  flex: 1;
  min-width: 0;
}

.username {
  font-size: $font-h1;
  font-weight: $font-weight-bold;
  color: $text-primary;
  line-height: $line-height-tight;
}

.login-tip {
  font-size: $font-h2;
  font-weight: $font-weight-medium;
  color: $text-primary;
  opacity: 0.9;
}

.user-desc {
  font-size: $font-caption;
  color: $text-secondary;
  margin-top: $spacing-2xs;
}

.companionship {
  margin-top: 10rpx;
  color: $primary-dark;
  font-size: $font-mini;
}

.info-link {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.78);
  border-radius: $radius-full;
  padding: $spacing-xs $spacing-sm $spacing-xs $spacing-md;
  margin-left: $spacing-sm;

  .info-link-text {
    font-size: $font-caption;
    color: $primary;
    white-space: nowrap;
  }

  .info-link-arrow {
    font-size: $font-caption;
    color: $primary;
    margin-left: $spacing-2xs;
  }
}

/* Wave bottom edge */
.header-wave {
  display: none;
}

.wave-curve {
  position: absolute;
  top: 0;
  left: -10%;
  right: -10%;
  height: 80rpx;
  border-radius: 50% 50% 0 0 / 100% 100% 0 0;
}

/* ================================
   Stats Row
   ================================ */
.stats-row {
  display: flex;
  justify-content: space-around;
  margin: $spacing-lg $page-padding 0;
  padding: $spacing-lg $spacing-sm;
  background: $bg-card;
  border-radius: $radius-xl;
  box-shadow: $shadow-card;
}

.stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-xs;
  transition: transform $duration-fast $ease-default;

  &:active {
    transform: scale(0.95);
  }
}

.stat-icon-wrapper {
  width: 80rpx;
  height: 80rpx;
  border-radius: $radius-lg;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon-blue {
  background: $bookkeeping-light;

  .stat-icon {
    color: $bookkeeping;
    font-size: 36rpx;
  }
}

.stat-icon-pink {
  background: $card-light;

  .stat-icon {
    color: $card;
    font-size: 36rpx;
  }
}

.stat-icon-orange {
  background: $commemoration-light;

  .stat-icon {
    color: $commemoration;
    font-size: 36rpx;
  }
}

.stat-label {
  font-size: $font-small;
  color: $text-secondary;
}

/* ================================
   Menu Section
   ================================ */
.menu-section {
  margin: -56rpx $page-padding 0;
  position: relative;
  z-index: 2;
}

.menu-card {
  background: rgba(255, 255, 255, 0.9);
  border: 1rpx solid rgba(255, 255, 255, 0.94);
  border-radius: $radius-2xl;
  box-shadow: $shadow-card;
  overflow: hidden;
  backdrop-filter: blur(24rpx);
}

.menu-item {
  display: flex;
  align-items: center;
  padding: $spacing-md $spacing-lg;
  transition: background-color $duration-fast $ease-default;
}

.menu-item-active {
  background-color: $bg-hover;
}

.menu-icon-circle {
  width: 80rpx;
  height: 80rpx;
  border-radius: $radius-lg;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.menu-icon-blue {
  background: rgba(79, 134, 247, 0.12);

  .menu-item-icon {
    color: $primary;
    font-size: 36rpx;
  }
}

.menu-icon-pink {
  background: rgba(244, 114, 182, 0.12);

  .menu-item-icon {
    color: $menstruation;
    font-size: 36rpx;
  }
}

.menu-icon-green {
  background: rgba(74, 222, 128, 0.12);

  .menu-item-icon {
    color: $success-dark;
    font-size: 36rpx;
  }
}

.menu-item-content {
  flex: 1;
  margin-left: $spacing-md;
  min-width: 0;
}

.menu-item-title {
  font-size: $font-body;
  font-weight: $font-weight-medium;
  color: $text-primary;
  display: block;
  line-height: $line-height-tight;
}

.menu-item-desc {
  font-size: $font-small;
  color: $text-tertiary;
  display: block;
  margin-top: $spacing-2xs;
  line-height: $line-height-tight;
}

.menu-item-arrow {
  flex-shrink: 0;
  color: $text-tertiary;
  font-size: $font-caption;
  margin-left: $spacing-sm;
}

.menu-divider {
  height: 1rpx;
  background-color: $border-color;
  margin-left: calc(80rpx + #{$spacing-md} + #{$spacing-lg});
  transform: scaleY(0.5);
  transform-origin: 0 100%;
}

/* ================================
   Logout Section
   ================================ */
.logout-section {
  margin: $section-gap $page-padding 0;
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-xs;
  background: rgba(255, 255, 255, 0.84);
  border: 1rpx solid rgba(255, 255, 255, 0.92);
  border-radius: $radius-2xl;
  padding: $spacing-md 0;
  font-size: $font-body;
  color: $danger;
  box-shadow: $shadow-card;
  transition: all $duration-fast $ease-default;

  .logout-icon {
    font-size: $font-h3;
  }
}

.logout-btn-active {
  background-color: $bg-hover;
  transform: scale(0.98);
}

/* ================================
   Footer
   ================================ */
.page-footer {
  text-align: center;
  padding: $spacing-xl 0 $spacing-lg;
}

.version-text {
  font-size: $font-mini;
  color: $text-tertiary;
}
</style>
