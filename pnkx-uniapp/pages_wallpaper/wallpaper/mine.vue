<!--
 * @File: mine
 * @Author: PHY
 * @Description: 壁纸应用「我的」页（账号信息 + 壁纸工具设置）
 *   与博客管理应用隔离：退出登录回工具台，不暴露博客专属内容。
-->
<template>
  <view class="wp-mine-page">
    <!-- 顶部账号区 -->
    <view class="account-header">
      <view class="account-bg"></view>
      <view class="account-content">
        <view class="avatar-wrapper">
          <view v-if="!avatar" class="avatar-placeholder">
            <view class="iconfont icon-people avatar-icon"></view>
          </view>
          <image v-if="avatar" :src="avatar" class="avatar-image" mode="aspectFill" />
        </view>
        <view class="account-meta">
          <view v-if="!name" class="login-tip" @click="handleToLogin">点击登录</view>
          <view v-else>
            <text class="username">{{ name }}</text>
            <text class="user-desc">登录后可点赞和下载壁纸</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 今日下载配额（独立卡片） -->
    <view class="quota-section" v-if="name">
      <view class="quota-card">
        <view class="quota-header">
          <view class="quota-title">
            <view class="iconfont icon-download quota-title-icon"></view>
            <text>今日下载配额</text>
          </view>
          <text class="quota-remaining">剩余 {{ downloadRemaining }} 次</text>
        </view>
        <view class="quota-progress">
          <view class="quota-progress-bar" :style="{ width: quotaPercent + '%' }"></view>
        </view>
        <view class="quota-meta">
          <text class="quota-meta-text">已下载 {{ downloadTodayCount }} / 上限 {{ downloadLimit }}{{ downloadTodayReward > 0 ? ' + 分享奖励 ' + downloadTodayReward : '' }}</text>
        </view>
        <!-- 分享小程序奖励入口：仅登录且今日还有分享获奖次数时显示 -->
        <button
          v-if="canShareForReward"
          class="quota-share-btn"
          open-type="share"
          hover-class="quota-share-btn-active"
          @click="handleShareReward"
        >
          <view class="iconfont icon-fenxiang quota-share-icon"></view>
          <text>分享小程序 +{{ shareRewardCount }} 次下载</text>
        </button>
        <view class="quota-share-tip" v-else-if="name">
          今日分享奖励次数已用完，明天再来吧～
        </view>
      </view>
    </view>

    <!-- 壁纸数据 -->
    <view class="section">
      <view class="section-title">
        <view class="section-accent"></view>
        <text>我的数据</text>
      </view>
      <view class="menu-card">
        <view class="menu-item" @click="goRecords('like')">
          <view class="menu-icon-bg menu-icon--like">
            <view class="iconfont icon-aixin menu-icon"></view>
          </view>
          <view class="menu-item-content">
            <text class="menu-item-title">我的点赞</text>
            <text class="menu-item-desc">查看我点过赞的壁纸</text>
          </view>
          <view class="iconfont icon-right menu-item-arrow"></view>
        </view>
        <view class="menu-item" @click="goRecords('download')">
          <view class="menu-icon-bg menu-icon--download">
            <uni-icons type="download" size="24" color="#0EA5E9" />
          </view>
          <view class="menu-item-content">
            <text class="menu-item-title">我的下载</text>
            <text class="menu-item-desc">查看我下载过的壁纸</text>
          </view>
          <view class="iconfont icon-right menu-item-arrow"></view>
        </view>
      </view>
    </view>

    <!-- 账号与通用 -->
    <view class="section">
      <view class="section-title">
        <view class="section-accent"></view>
        <text>账号与通用</text>
      </view>
      <view class="menu-card">
        <view class="menu-item" @click="handleToPwd">
          <view class="menu-icon-bg menu-icon--pwd">
            <view class="iconfont icon-password menu-icon"></view>
          </view>
          <view class="menu-item-content">
            <text class="menu-item-title">修改密码</text>
          </view>
          <view class="iconfont icon-right menu-item-arrow"></view>
        </view>
        <view class="menu-divider"></view>
        <view class="menu-item" @click="handleToUpgrade">
          <view class="menu-icon-bg menu-icon--upgrade">
            <view class="iconfont icon-refresh menu-icon"></view>
          </view>
          <view class="menu-item-content">
            <text class="menu-item-title">检查更新</text>
          </view>
          <view class="iconfont icon-right menu-item-arrow"></view>
        </view>
        <view class="menu-divider"></view>
        <view class="menu-item" @click="handleCleanTmp">
          <view class="menu-icon-bg menu-icon--clean">
            <view class="iconfont icon-clean menu-icon"></view>
          </view>
          <view class="menu-item-content">
            <text class="menu-item-title">清理缓存</text>
          </view>
          <view class="iconfont icon-right menu-item-arrow"></view>
        </view>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-section" v-if="name">
      <view class="logout-btn" @click="handleLogout" hover-class="logout-btn-active">
        <view class="iconfont icon-logout logout-icon"></view>
        <text>退出登录</text>
      </view>
    </view>

    <!-- 底部留白 -->
    <view class="page-bottom-safe"></view>

    <!-- 更新弹窗 -->
    <uni-popup ref="updateDialog" type="dialog" :is-mask-click="false">
      <uni-popup-dialog type="info" confirmText="后台下载" title="正在下载更新" @confirm="backgroundDownload"
                        :showClose="false">
        <view>下载进度
          <text style="padding: 0 0.2rem;color:#0EA5E9;">{{ percentageNum }}</text>
          %
        </view>
      </uni-popup-dialog>
    </uni-popup>
  </view>
</template>

<script>
import checkUpdate from '@/utils/update'
import { toLogin } from '@/permission'
import { getToken } from '@/utils/auth'
import { getAppVersion } from '@/utils/version'
import { getDownloadStatus, shareReward } from '../api/px/life/wallpaper'

export default {
  name: 'WallpaperMine',
  data() {
    return {
      percentageNum: 0,
      // 今日下载配额
      downloadLimit: 0,
      downloadTodayCount: 0,
      downloadTodayReward: 0,
      downloadRemaining: 0,
      shareRewardCount: 0,
      shareTimesUsed: 0,
      shareTimesLimit: 0,
      // 分享奖励发放锁，防止 onShareAppMessage 回调重复发奖
      rewardClaiming: false
    }
  },
  computed: {
    name() {
      return this.$store.getters.name
    },
    avatar() {
      return this.$store.getters.avatar
    },
    version() {
      return getAppVersion()
    },
    // 配额进度（已下载 / 总额度，含分享奖励）
    quotaPercent() {
      const total = this.downloadLimit + this.downloadTodayReward
      if (total <= 0) return 0
      const used = this.downloadTodayCount
      return Math.min(100, Math.round((used / total) * 100))
    },
    // 今日是否还能通过分享获得奖励
    canShareForReward() {
      // 0 表示不限次数
      if (this.shareTimesLimit <= 0) return true
      return this.shareTimesUsed < this.shareTimesLimit
    }
  },
  onShow() {
    // 退出登录后页面可能被缓存，强制刷新登录态
    if (!getToken()) {
      this.$store.dispatch('LogOut')
      return
    }
    // 登录态下刷新下载配额（从点赞/下载记录页返回也能及时更新）
    this.loadDownloadStatus()
  },
  // 开启「转发给朋友」（需同时存在，朋友圈分享才会生效）
  onShareAppMessage() {
    return {
      title: 'Pei你看雪 · 精选高清壁纸，一键保存到相册',
      path: '/pages_wallpaper/wallpaper/index'
    }
  },
  // 开启「分享到朋友圈」
  onShareTimeline() {
    return {
      title: 'Pei你看雪 · 精选高清壁纸，一键保存到相册'
    }
  },
  methods: {
    /**
     * 加载今日下载配额（剩余次数、限额、分享奖励）
     */
    loadDownloadStatus() {
      if (!getToken()) return
      getDownloadStatus().then(res => {
        if (res.code === 200) {
          this.downloadLimit = res.limit || 0
          this.downloadTodayCount = res.todayCount || 0
          this.downloadTodayReward = res.todayReward || 0
          this.downloadRemaining = res.remaining != null ? res.remaining : 0
          this.shareRewardCount = res.reward || 0
          this.shareTimesUsed = res.shareTimesUsed || 0
          this.shareTimesLimit = res.shareTimesLimit || 0
        }
      }).catch(() => {})
    },
    /**
     * 点击「分享小程序」按钮：open-type=share 会唤起分享面板，
     * 实际发奖在分享成功回调（onShareAppMessage 返回后由微信触发分享），
     * 这里调用后端发放奖励。用 rewardClaiming 防止重复发奖。
     */
    handleShareReward() {
      if (this.rewardClaiming) return
      if (!this.canShareForReward) {
        uni.showToast({ title: '今日分享奖励次数已用完', icon: 'none' })
        return
      }
      this.rewardClaiming = true
      shareReward().then(res => {
        if (res.code === 200) {
          const n = res.rewarded || this.shareRewardCount
          uni.showToast({ title: `分享成功，+${n} 次下载`, icon: 'none' })
        }
        // 无论成功或"次数用完"，刷新最新配额
        this.loadDownloadStatus()
      }).catch(() => {
        // request.js 已 toast 错误文案（如"今日分享奖励次数已用完"）
        this.loadDownloadStatus()
      }).finally(() => {
        this.rewardClaiming = false
      })
    },
    handleToLogin() {
      toLogin()
    },
    goRecords(type) {
      if (!this.name) {
        this.handleToLogin()
        return
      }
      this.$tab.navigateTo('/pages_wallpaper/wallpaper/records?type=' + type)
    },
    handleToPwd() {
      this.$tab.navigateTo('/pages_blog/mine/pwd/index')
    },
    handleToUpgrade() {
      checkUpdate({
        showToast: true,
        onDownloadStart: () => {
          this.percentageNum = 0
          this.$refs.updateDialog.open()
        },
        onProgress: (progress) => {
          this.percentageNum = progress
        },
        onDownloadComplete: () => {
          this.$refs.updateDialog.close()
        }
      })
    },
    backgroundDownload() {
      this.$refs.updateDialog.close()
    },
    handleCleanTmp() {
      this.$modal.confirm('清理缓存后将退出登录，确认清理缓存？').then(() => {
        try {
          uni.clearStorageSync()
        } catch (e) {
          // ignore
        }
        this.$store.dispatch('LogOut').then(() => {
          // 退出回工具台（壁纸应用独立，不回博客首页）
          this.$tab.reLaunch('/pages/tools')
        })
      })
    },
    handleLogout() {
      this.$modal.confirm('确定注销并退出系统吗？').then(() => {
        this.$store.dispatch('LogOut').then(() => {
          // 退出回工具台
          this.$tab.reLaunch('/pages/tools')
        })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
/* #ifndef APP-NVUE */
page {
  background-color: $bg-page;
}
/* #endif */

.wp-mine-page {
  min-height: 100vh;
  background: $bg-page;
}

/* 账号头部 */
.account-header {
  position: relative;
  padding: $spacing-xl $page-padding $spacing-2xl;
  overflow: hidden;

  .account-bg {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg, #0EA5E9 0%, #38BDF8 100%);
  }

  .account-content {
    position: relative;
    z-index: $z-base;
    display: flex;
    align-items: center;
  }

  .avatar-wrapper {
    width: 120rpx;
    height: 120rpx;
    border-radius: $radius-full;
    overflow: hidden;
    flex-shrink: 0;
    margin-right: $spacing-lg;
    box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.15);

    .avatar-image {
      width: 100%;
      height: 100%;
    }

    .avatar-placeholder {
      width: 100%;
      height: 100%;
      background: rgba(255, 255, 255, 0.3);
      display: flex;
      align-items: center;
      justify-content: center;

      .avatar-icon {
        font-size: 56rpx;
        color: #fff;
      }
    }
  }

  .account-meta {
    flex: 1;
    min-width: 0;

    .login-tip {
      font-size: $font-h3;
      font-weight: $font-weight-semibold;
      color: #fff;
    }

    .username {
      display: block;
      font-size: $font-h2;
      font-weight: $font-weight-bold;
      color: #fff;
    }

    .user-desc {
      display: block;
      margin-top: $spacing-2xs;
      font-size: $font-caption;
      color: rgba(255, 255, 255, 0.8);
    }
  }
}

/* 今日下载配额卡片 */
.quota-section {
  margin: #{$spacing-lg} $page-padding 0;
  margin-bottom: $spacing-xl;
  position: relative;
  z-index: $z-card;
}

.quota-card {
  background: $bg-card;
  border-radius: $radius-xl;
  box-shadow: $shadow-card;
  padding: $spacing-lg $spacing-lg $spacing-md;
}

.quota-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .quota-title {
    display: flex;
    align-items: center;

    .quota-title-icon {
      font-size: 32rpx;
      color: #0EA5E9;
      margin-right: $spacing-sm;
    }

    text {
      font-size: $font-body;
      font-weight: $font-weight-semibold;
      color: $text-primary;
    }
  }

  .quota-remaining {
    font-size: $font-h3;
    font-weight: $font-weight-bold;
    color: #0EA5E9;
  }
}

.quota-progress {
  margin-top: $spacing-md;
  height: 14rpx;
  border-radius: $radius-full;
  background: $gray-100;
  overflow: hidden;

  .quota-progress-bar {
    height: 100%;
    border-radius: $radius-full;
    background: linear-gradient(90deg, #0EA5E9 0%, #38BDF8 100%);
    transition: width $duration-normal $ease-default;
  }
}

.quota-meta {
  margin-top: $spacing-sm;

  .quota-meta-text {
    font-size: $font-caption;
    color: $text-tertiary;
  }
}

.quota-share-btn {
  margin-top: $spacing-md;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: $spacing-sm $spacing-md;
  border-radius: $radius-lg;
  background: linear-gradient(135deg, #0EA5E9 0%, #38BDF8 100%);
  color: #fff;
  font-size: $font-body;
  font-weight: $font-weight-medium;
  line-height: 1.6;

  &::after {
    border: none;
  }

  .quota-share-icon {
    font-size: 30rpx;
    margin-right: $spacing-xs;
  }
}

.quota-share-btn-active {
  opacity: 0.85;
  transform: scale(0.98);
}

.quota-share-tip {
  margin-top: $spacing-md;
  text-align: center;
  font-size: $font-caption;
  color: $text-disabled;
}

/* 区块 */
.section {
  position: relative;
  margin: #{$spacing-lg} $page-padding 0;
  margin-bottom: $spacing-xl;
  z-index: $z-card;

  .section-title {
    display: flex;
    align-items: center;
    margin-bottom: $spacing-sm;
    padding-left: $spacing-2xs;

    .section-accent {
      width: 6rpx;
      height: 28rpx;
      border-radius: $radius-full;
      margin-right: $spacing-sm;
      background-color: #0EA5E9;
    }

    text {
      font-size: $font-body;
      font-weight: $font-weight-semibold;
      color: $text-primary;
    }
  }
}

/* 菜单卡片 */
.menu-card {
  background: $bg-card;
  border-radius: $radius-xl;
  box-shadow: $shadow-card;
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: $spacing-md $spacing-lg;

  &:active {
    background: $gray-50;
  }

  .menu-icon-bg {
    width: 64rpx;
    height: 64rpx;
    border-radius: $radius-lg;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: $spacing-md;
    flex-shrink: 0;

    .menu-icon {
      font-size: 32rpx;
    }
  }

  .menu-icon--like {
    background: rgba(#FF4757, 0.1);
    .menu-icon { color: #FF4757; }
  }

  .menu-icon--download {
    background: rgba(#0EA5E9, 0.1);
  }

  .menu-icon--pwd {
    background: rgba($primary, 0.1);
    .menu-icon { color: $primary; }
  }

  .menu-icon--upgrade {
    background: rgba(#5352ED, 0.1);
    .menu-icon { color: #5352ED; }
  }

  .menu-icon--clean {
    background: rgba($text-tertiary, 0.1);
    .menu-icon { color: $text-tertiary; }
  }

  .menu-item-content {
    flex: 1;
    min-width: 0;

    .menu-item-title {
      display: block;
      font-size: $font-body;
      font-weight: $font-weight-medium;
      color: $text-primary;
    }

    .menu-item-desc {
      display: block;
      margin-top: 4rpx;
      font-size: $font-caption;
      color: $text-tertiary;
    }
  }

  .menu-item-arrow {
    flex-shrink: 0;
    font-size: 24rpx;
    color: $text-disabled;
  }
}

.menu-divider {
  height: 2rpx;
  background: $gray-100;
  margin-left: 96rpx;
}

/* 退出登录 */
.logout-section {
  padding: $spacing-lg $page-padding;

  .logout-btn {
    background: $bg-card;
    border-radius: $radius-xl;
    box-shadow: $shadow-card;
    display: flex;
    justify-content: center;
    align-items: center;
    padding: $spacing-md;
    transition: all $duration-fast $ease-default;

    .logout-icon {
      margin-right: $spacing-sm;
      color: $danger;
      font-size: 32rpx;
    }

    text {
      color: $danger;
      font-size: $font-h3;
      font-weight: $font-weight-medium;
    }
  }

  .logout-btn-active {
    transform: scale(0.97);
    background: $bg-hover;
  }
}

.page-bottom-safe {
  height: 120rpx;
}
</style>
