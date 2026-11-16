<!--
 * @Component: UserBadge 登录态展示
 * @Author: PHY
 * @Description: 全局登录态徽标，用于各工具页顶部展示当前登录用户。
 *   - 已登录：头像 + 昵称，点击跳转「我的」详情页
 *   - 未登录：占位头像 + 「点击登录」，点击带 redirect 跳登录页，登录后回到当前页
 *   登录/退出后自动刷新（依赖 store 状态，computed 实时响应）。
 *   用法：
 *     <user-badge />                       默认跳 pages/mine/index（博客应用用）
 *     <user-badge theme="light" to="/pages_wallpaper/wallpaper/mine" />  指定跳某工具自己的「我的」页
 *     theme: light=浅色背景(深色字) / dark=深色背景(白色字)，默认 dark
-->
<template>
  <view class="user-badge" :class="theme" @click="handleClick">
    <!-- 头像 -->
    <view class="avatar-wrap">
      <image v-if="avatar" class="avatar-img" :src="avatar" mode="aspectFill" />
      <view v-else class="avatar-placeholder">
        <view class="iconfont icon-people avatar-icon"></view>
      </view>
    </view>
    <!-- 昵称 / 登录提示 -->
    <view class="badge-info">
      <text class="badge-name" v-if="name">{{ name }}</text>
      <text class="badge-login" v-else>点击登录</text>
    </view>
    <!-- 登录后显示个人中心入口箭头 -->
    <view class="badge-arrow" v-if="isLoggedIn">
      <view class="iconfont icon-right"></view>
    </view>
  </view>
</template>

<script>
import { toLogin } from '@/permission'

export default {
  name: 'UserBadge',
  props: {
    // 主题：light=浅色背景(深色字)，dark=深色/渐变背景(白色字)
    theme: {
      type: String,
      default: 'dark'
    },
    // 已登录时点击跳转的目标页面，默认 pages/mine/index（博客应用）
    // 各工具可传自己应用专属的「我的」页路径
    to: {
      type: String,
      default: '/pages/mine/index'
    }
  },
  computed: {
    // 登录态、用户信息都从 store 实时读取，登录/退出后自动刷新
    isLoggedIn() {
      return !!this.$store.getters.token
    },
    name() {
      return this.$store.getters.name
    },
    avatar() {
      return this.$store.getters.avatar
    }
  },
  methods: {
    handleClick() {
      if (this.isLoggedIn) {
        // 已登录 → 跳「我的」详情页（清空页面栈）
        this.$tab.reLaunch(this.to)
      } else {
        // 未登录 → 跳登录页，登录后回到当前页
        toLogin()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.user-badge {
  display: flex;
  align-items: center;
  padding: $spacing-xs $spacing-sm;
  border-radius: $radius-full;
  transition: opacity $duration-fast $ease-default;

  &:active {
    opacity: 0.7;
  }
}

/* 深色/渐变背景：半透明白底 + 白字 */
.user-badge.dark {
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

/* 浅色背景：浅灰底 + 深色字 */
.user-badge.light {
  background: $gray-100;
}

.avatar-wrap {
  width: 48rpx;
  height: 48rpx;
  border-radius: $radius-full;
  overflow: hidden;
  flex-shrink: 0;

  .avatar-img {
    width: 100%;
    height: 100%;
  }

  .avatar-placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;

    .avatar-icon {
      font-size: 32rpx;
    }
  }
}

.badge-info {
  margin: 0 $spacing-xs;
  max-width: 200rpx;
  overflow: hidden;

  .badge-name,
  .badge-login {
    font-size: $font-caption;
    font-weight: $font-weight-medium;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    display: block;
  }
}

.badge-arrow {
  display: flex;
  align-items: center;
  margin-left: $spacing-xs;

  .icon-right {
    font-size: 24rpx;
  }
}

/* 文字/图标颜色随主题切换 */
.user-badge.dark {
  .avatar-placeholder {
    background: rgba(255, 255, 255, 0.3);

    .avatar-icon {
      color: #FFFFFF;
    }
  }

  .badge-name {
    color: #FFFFFF;
  }

  .badge-login {
    color: rgba(255, 255, 255, 0.9);
  }

  .icon-right {
    color: rgba(255, 255, 255, 0.8);
  }
}

.user-badge.light {
  .avatar-placeholder {
    background: $gray-200;

    .avatar-icon {
      color: $gray-500;
    }
  }

  .badge-name {
    color: $text-primary;
  }

  .badge-login {
    color: $text-secondary;
  }

  .icon-right {
    color: $text-tertiary;
  }
}
</style>

