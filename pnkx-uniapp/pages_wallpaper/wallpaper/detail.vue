<!--
 * @File: detail
 * @Author: PHY
 * @Description: 壁纸详情页（大图预览 + 左右滑动切换 + 保存到相册）
-->
<template>
  <view class="detail-page">
    <!-- #ifdef MP-WEIXIN -->
    <page-container
      :show="backGuardVisible"
      :overlay="false"
      @beforeleave="handleBackGuardBeforeLeave"
      @afterleave="resetBackGuard"
    />
    <!-- #endif -->

    <!-- 横向滑动切换壁纸 -->
    <swiper
      class="detail-swiper"
      :current="currentIndex"
      :circular="false"
      @change="onSwiperChange"
    >
      <swiper-item v-for="(wp, idx) in list" :key="wp.id">
        <view v-if="shouldRenderSwiperItem(idx)" class="swiper-item-content">
          <!-- 壁纸大图 -->
          <view class="image-wrap">
            <image
              v-if="wp.url"
              class="wallpaper-image"
              :src="getSwiperImageSrc(wp, idx)"
              mode="widthFix"
              @load="onImageLoad(idx)"
            />
            <view v-if="loadingIdx === idx" class="image-loading">
              <view class="loading-spinner"></view>
              <text class="loading-text">加载中...</text>
            </view>
          </view>

          <!-- 底部操作栏（仅当前项显示） -->
          <view class="action-bar" v-if="idx === currentIndex" @click.stop>
            <view class="action-info">
              <text class="action-title">{{ wp.name || '壁纸' }}</text>
              <!-- 点赞按钮 -->
              <view class="like-btn" @click="toggleLike" :class="{ likeing: likeLoading }">
                <text class="wallpaper-action-icon like-icon" :class="{ active: liked }">{{ liked ? '♥' : '♡' }}</text>
                <text class="like-count" v-if="likeCount">{{ likeCount }}</text>
              </view>
              <text class="action-meta" v-if="wp.width && wp.height">
                {{ wp.width }} × {{ wp.height }}
              </text>
            </view>
            <view class="action-right">
              <view class="action-btn" :class="{ disabled: downloading }" @click="saveWallpaper">
                <uni-icons v-if="!downloading" type="download" size="20" color="#FFFFFF" />
                <text class="action-btn-text">{{ downloadBtnText }}</text>
              </view>
            </view>
          </view>
        </view>
      </swiper-item>
    </swiper>

    <!-- #ifdef MP-WEIXIN -->
    <view class="privacy-mask" v-if="showPrivacyAuthorize">
      <view class="privacy-panel">
        <text class="privacy-title">隐私保护指引</text>
        <text class="privacy-desc">保存壁纸到相册前，需要你先阅读并同意小程序隐私保护指引。</text>
        <view class="privacy-link" @click="openPrivacyContract">
          <text>查看《小程序隐私保护指引》</text>
        </view>
        <view class="privacy-actions">
          <button class="privacy-btn privacy-btn-cancel" @click="handleDisagreePrivacyAuthorization">暂不同意</button>
          <button
            id="agree-btn"
            class="privacy-btn privacy-btn-confirm"
            open-type="agreePrivacyAuthorization"
            @agreeprivacyauthorization="handleAgreePrivacyAuthorization"
          >同意并继续</button>
        </view>
      </view>
    </view>
    <!-- #endif -->

    <!-- 下载次数用完引导分享弹窗 -->
    <download-limit-dialog
      :show="showLimitDialog"
      :reward="shareRewardCount"
      :share-times-used="shareTimesUsed"
      :share-times-limit="shareTimesLimit"
      :reward-claiming="rewardClaiming"
      @share="handleShareReward"
      @close="showLimitDialog = false"
    />
  </view>
</template>

<script>
import { getWallpaper, likeWallpaper, getDownloadStatus, shareReward } from '../api/px/life/wallpaper'
import config from '@/config'
import { getToken } from '@/utils/auth'
import { toLogin } from '@/permission'
import { saveToAlbumWithAuth } from '@/utils/tools/albumSaver'

export default {
  name: 'WallpaperDetail',
  data() {
    return {
      id: null,
      // 列表上下文（来自列表页 globalData）
      list: [],
      currentIndex: 0,
      // 当前项详情（点赞态/数量/尺寸），切换时懒加载
      liked: false,
      likeCount: 0,
      // 各项加载态
      loadingIdx: -1,
      // 下载
      downloading: false,
      downloadBtnText: '保存到相册',
      // 点赞请求锁
      likeLoading: false,
      // 下载限额状态
      downloadLimit: 0,
      // -1 表示未知（接口未返回前不拦截），0 表示已用完
      downloadRemaining: -1,
      downloadRemindText: '',
      // 下载次数用完引导分享弹窗
      showLimitDialog: false,
      shareRewardCount: 0,
      shareTimesUsed: 0,
      shareTimesLimit: 0,
      rewardClaiming: false,
      // 微信隐私保护指引弹窗
      showPrivacyAuthorize: false,
      privacyAuthorizationResolve: null,
      // 微信小程序物理返回拦截
      backGuardVisible: true,
      backGuardLeaving: false
    }
  },
  onLoad(options = {}) {
    this.registerPrivacyAuthorization()
    this.id = options.id
    // 从 globalData 读取列表上下文
    const ctx = getApp().globalData.__wallpaperContext
    if (ctx && ctx.list && ctx.list.length > 0) {
      this.list = ctx.list
      this.currentIndex = ctx.index || 0
      // 校正：若传了 id，以 id 定位
      if (this.id) {
        const idx = this.list.findIndex(w => String(w.id) === String(this.id))
        if (idx >= 0) this.currentIndex = idx
      }
    } else {
      // 兜底：无列表上下文，单张模式
      this.list = [{ id: this.id }]
      this.currentIndex = 0
    }
    // 加载当前项详情
    this.loadCurrentDetail()
    // 加载今日下载状态（剩余次数、提醒文案）
    this.loadDownloadStatus()
  },
  // 开启「转发给朋友」：带上当前壁纸 id，打开后定位到同一张
  // （onShareTimeline 依赖本方法存在才会生效）
  onShareAppMessage() {
    const wp = this.current()
    return {
      title: wp.name ? `${wp.name} - Pei你看雪壁纸` : 'Pei你看雪 · 精选高清壁纸',
      path: `/pages_wallpaper/wallpaper/detail?id=${wp.id}`,
      imageUrl: wp.thumbnail || wp.url
    }
  },
  // 开启「分享到朋友圈」：朋友圈仅支持 query 参数，无法用 path
  onShareTimeline() {
    const wp = this.current()
    return {
      title: wp.name ? `${wp.name} - Pei你看雪壁纸` : 'Pei你看雪 · 精选高清壁纸',
      query: `id=${wp.id}`,
      imageUrl: wp.thumbnail || wp.url
    }
  },
  methods: {
    /**
     * 微信小程序物理返回：有页面栈则回上一页，直达详情则回壁纸列表。
     */
    handleBackGuardBeforeLeave() {
      if (this.backGuardLeaving) return
      this.backGuardLeaving = true
      this.backGuardVisible = false
      const pages = typeof getCurrentPages === 'function' ? getCurrentPages() : []
      if (pages.length > 1) {
        setTimeout(() => {
          uni.navigateBack({ delta: 1 })
        }, 0)
      } else {
        setTimeout(() => {
          uni.reLaunch({ url: '/pages_wallpaper/wallpaper/index' })
        }, 0)
      }
    },

    /** 重新挂起返回拦截容器 */
    resetBackGuard() {
      // #ifdef MP-WEIXIN
      if (this.backGuardLeaving) return
      this.backGuardVisible = false
      setTimeout(() => {
        this.backGuardVisible = true
      }, 50)
      // #endif
    },

    /**
     * 注册微信隐私保护指引授权回调。
     * 当 requirePrivacyAuthorize 触发隐私授权时，展示自定义弹窗。
     */
    registerPrivacyAuthorization() {
      // #ifdef MP-WEIXIN
      const wxApi = typeof wx !== 'undefined' ? wx : null
      if (!wxApi || !wxApi.onNeedPrivacyAuthorization) return
      wxApi.onNeedPrivacyAuthorization((resolve) => {
        this.privacyAuthorizationResolve = resolve
        this.showPrivacyAuthorize = true
      })
      // #endif
    },

    /**
     * 用户同意隐私保护指引后，继续原 API 调用。
     */
    handleAgreePrivacyAuthorization() {
      this.showPrivacyAuthorize = false
      if (this.privacyAuthorizationResolve) {
        this.privacyAuthorizationResolve({
          buttonId: 'agree-btn',
          event: 'agree'
        })
        this.privacyAuthorizationResolve = null
      }
    },

    /**
     * 用户不同意隐私保护指引，终止本次保存。
     */
    handleDisagreePrivacyAuthorization() {
      this.showPrivacyAuthorize = false
      if (this.privacyAuthorizationResolve) {
        this.privacyAuthorizationResolve({ event: 'disagree' })
        this.privacyAuthorizationResolve = null
      }
      uni.showToast({ title: '需要同意隐私指引后才能保存', icon: 'none' })
    },

    /**
     * 打开微信小程序隐私保护指引。
     */
    openPrivacyContract() {
      // #ifdef MP-WEIXIN
      const wxApi = typeof wx !== 'undefined' ? wx : null
      if (wxApi && wxApi.openPrivacyContract) {
        wxApi.openPrivacyContract({
          fail: (err) => console.error('openPrivacyContract fail:', err && err.errMsg)
        })
      }
      // #endif
    },

    /**
     * 当前壁纸对象
     */
    current() {
      return this.list[this.currentIndex] || {}
    },

    /**
     * 只渲染当前页和相邻页，避免详情页一次挂载大量高清图。
     */
    shouldRenderSwiperItem(idx) {
      return Math.abs(idx - this.currentIndex) <= 1
    },

    /**
     * 非渲染窗口内不绑定图片地址，避免触发隐藏项的原图加载。
     */
    getSwiperImageSrc(wp, idx) {
      if (!this.shouldRenderSwiperItem(idx)) return ''
      return wp && wp.url ? wp.url : ''
    },

    /**
     * 加载当前项详情（点赞数、尺寸等）
     * 列表数据里只有基础字段，详情接口补充 likeCount 等
     */
    loadCurrentDetail() {
      const wp = this.current()
      if (!wp || !wp.id) return
      // 先用列表已有数据占位
      this.likeCount = wp.likeCount || 0
      this.liked = false
      this.loadingIdx = this.currentIndex
      getWallpaper(wp.id).then(res => {
        const detail = res.data || {}
        // 合并详情到当前项
        Object.assign(wp, detail)
        this.likeCount = detail.likeCount || 0
        this.liked = !!detail.liked
      }).catch(() => {
        // 静默失败，保留列表已有数据
      }).finally(() => {
        this.loadingIdx = -1
      })
    },

    /**
     * swiper 切换
     */
    onSwiperChange(e) {
      const next = e.detail.current
      if (next === this.currentIndex) return
      this.currentIndex = next
      this.id = this.current().id
      // 重置下载态
      this.resetBtn()
      // 重新加载详情
      this.loadCurrentDetail()
    },

    /**
     * 图片加载完成
     */
    onImageLoad(idx) {
      if (this.loadingIdx === idx) this.loadingIdx = -1
    },

    /**
     * 点赞/取消点赞
     */
    toggleLike() {
      if (this.likeLoading) return
      const wp = this.current()
      const token = getToken()
      if (!token) {
        uni.showModal({
          title: '提示',
          content: '登录后即可点赞，是否去登录？',
          confirmText: '去登录',
          success: (res) => {
            if (res.confirm) {
              toLogin()
            }
          }
        })
        return
      }
      this.likeLoading = true
      likeWallpaper(wp.id).then(res => {
        if (res.code === 200) {
          this.liked = res.liked
          this.likeCount = res.liked
            ? this.likeCount + 1
            : Math.max(0, this.likeCount - 1)
          // 同步到列表项
          wp.likeCount = this.likeCount
          uni.showToast({ title: res.liked ? '点赞成功' : '已取消', icon: 'none' })
        } else {
          uni.showToast({ title: res.msg || '操作失败', icon: 'none' })
        }
      }).catch(() => {
        uni.showToast({ title: '网络异常', icon: 'none' })
      }).finally(() => {
        this.likeLoading = false
      })
    },

    /**
     * 保存当前壁纸到相册
     */
    saveWallpaper() {
      if (this.downloading) return
      const token = getToken()
      if (!token) {
        uni.showModal({
          title: '提示',
          content: '登录后即可下载壁纸，是否去登录？',
          confirmText: '去登录',
          success: (res) => {
            if (res.confirm) {
              toLogin()
            }
          }
        })
        return
      }
      const wp = this.current()
      if (!wp.url) {
        uni.showToast({ title: '壁纸地址无效', icon: 'none' })
        return
      }
      // 下载前主动校验剩余次数：用完则直接弹引导分享弹窗，不发起下载
      this.checkBeforeDownload().then(canDownload => {
        if (!canDownload) return
        this.proceedDownload()
      })
    },

    /**
     * 下载前校验：刷新今日下载状态，若剩余次数为 0 则弹引导分享弹窗并返回 false。
     * 校验失败（网络异常等）放行，交由后端 429 兜底。
     */
    checkBeforeDownload() {
      return new Promise(resolve => {
        const token = getToken()
        if (!token) {
          resolve(true)
          return
        }
        // 纯同步判断：用 onLoad 时缓存的剩余次数，保证点击立即有反馈。
        // downloadRemaining 默认 -1（未知），仅当明确为 0 时才拦截，
        // 避免接口未返回时误拦截导致「点击没反应」。
        if (this.downloadRemaining === 0) {
          // 弹窗打开时刷新最新配额（防跨页状态过期）
          this.refreshShareStatus()
          this.showLimitDialog = true
          resolve(false)
          return
        }
        resolve(true)
      })
    },

    /**
     * 刷新今日下载配额与分享奖励状态（供弹窗/页面同步）
     */
    refreshShareStatus() {
      if (!getToken()) return
      getDownloadStatus().then(res => {
        if (res.code === 200) {
          this.downloadRemaining = res.remaining != null ? res.remaining : 0
          this.shareRewardCount = res.reward || 0
          this.shareTimesUsed = res.shareTimesUsed || 0
          this.shareTimesLimit = res.shareTimesLimit || 0
        }
      }).catch(() => {})
    },

    /**
     * 通过权限校验后真正发起下载与保存
     * 授权链路（微信隐私授权 → 曾拒绝引导设置页 → 保存）已收敛到
     * utils/tools/albumSaver.saveToAlbumWithAuth，此处直接下载
     */
    proceedDownload() {
      this.downloadAndSave()
    },

    /**
     * 构造后端代理下载地址（基于当前项 id）
     */
    getDownloadUrl() {
      return config.baseUrl + '/client/wallpaper/download/' + this.current().id
    },

    /**
     * 构造下载请求鉴权头：登录用户携带 token，后端据此识别下载人并记录。
     * 下载接口需登录，未登录的下载请求会在入口拦截，不会进到此处。
     */
    buildAuthHeader() {
      const token = getToken()
      return token ? { Authorization: 'Bearer ' + token } : {}
    },

    /**
     * 下载并保存到相册
     */
    downloadAndSave() {
      this.downloading = true
      this.downloadBtnText = '准备中...'
      this.doDownload()
    },

    /**
     * 执行下载
     */
    doDownload() {
      this.downloadBtnText = '下载中 0%'
      const downloadTask = uni.downloadFile({
        url: this.getDownloadUrl(),
        header: this.buildAuthHeader(),
        success: (res) => {
          // 429：下载达上限，拉取自定义提醒文案并弹窗
          if (res.statusCode === 429) {
            this.resetBtn()
            this.checkDownloadRemaining()
            return
          }
          if (res.statusCode !== 200) {
            this.resetBtn()
            uni.showToast({ title: '下载失败', icon: 'none' })
            return
          }
          this.saveToAlbum(res.tempFilePath)
        },
        fail: (err) => {
          this.resetBtn()
          console.error('downloadFile fail:', err && err.errMsg)
          // 兜底：下载失败可能是超限被拒（429 在小程序可能静默失败），
          // 主动检查配额，若已用完则弹引导分享弹窗
          if (getToken()) {
            getDownloadStatus().then(res => {
              if (res.code === 200 && (res.remaining != null ? res.remaining : 1) <= 0) {
                this.downloadRemaining = res.remaining != null ? res.remaining : 0
                this.shareRewardCount = res.reward || 0
                this.shareTimesUsed = res.shareTimesUsed || 0
                this.shareTimesLimit = res.shareTimesLimit || 0
                this.showLimitDialog = true
              } else {
                uni.showToast({ title: '下载失败', icon: 'none' })
              }
            }).catch(() => {
              uni.showToast({ title: '下载失败', icon: 'none' })
            })
          } else {
            uni.showToast({ title: '下载失败', icon: 'none' })
          }
        }
      })
      downloadTask.onProgressUpdate((res) => {
        this.downloadBtnText = `下载中 ${res.progress}%`
      })
    },

    /**
     * 保存到相册（授权/隐私链路走共享 albumSaver）
     */
    saveToAlbum(tempPath) {
      this.downloadBtnText = '保存中...'
      saveToAlbumWithAuth(tempPath, {
        successTitle: '已保存到相册',
        // 用户在设置页开启权限后自动续跑下载保存（保持原有体验）
        onGranted: () => this.downloadAndSave()
      }).then(() => {
        this.resetBtn()
        // 下载成功后刷新剩余次数，达上限则弹提醒
        this.checkDownloadRemaining()
      }).catch(() => {
        this.resetBtn()
      })
    },

    /**
     * 重置下载按钮状态
     */
    resetBtn() {
      this.downloading = false
      this.downloadBtnText = '保存到相册'
    },

    /**
     * 加载今日下载状态（剩余次数、限额、提醒文案）
     */
    loadDownloadStatus() {
      const token = getToken()
      if (!token) return
      getDownloadStatus().then(res => {
        if (res.code === 200) {
          this.downloadLimit = res.limit || 0
          this.downloadRemaining = res.remaining != null ? res.remaining : 0
          this.downloadRemindText = res.remindText || ''
          this.shareRewardCount = res.reward || 0
          this.shareTimesUsed = res.shareTimesUsed || 0
          this.shareTimesLimit = res.shareTimesLimit || 0
        }
      }).catch(() => {})
    },

    /**
     * 下载成功后刷新剩余次数，用完则弹「引导分享」弹窗
     */
    checkDownloadRemaining() {
      const token = getToken()
      if (!token) return
      getDownloadStatus().then(res => {
        if (res.code === 200) {
          this.downloadRemaining = res.remaining != null ? res.remaining : 0
          this.shareRewardCount = res.reward || 0
          this.shareTimesUsed = res.shareTimesUsed || 0
          this.shareTimesLimit = res.shareTimesLimit || 0
          // 剩余 0 表示已用完，弹引导分享弹窗
          if (this.downloadRemaining <= 0) {
            setTimeout(() => {
              this.showLimitDialog = true
            }, 1200)
          }
        }
      }).catch(() => {})
    },
    /**
     * 点击弹窗「分享小程序」按钮：调用后端发放奖励。
     * open-type=share 会同步唤起分享面板（由 onShareAppMessage 配置分享内容）。
     */
    handleShareReward() {
      if (this.rewardClaiming) return
      this.rewardClaiming = true
      shareReward().then(res => {
        if (res.code === 200) {
          const n = res.rewarded || this.shareRewardCount
          // 发奖成功：刷新配额，若仍有剩余则关闭弹窗
          this.downloadRemaining = res.remaining != null ? res.remaining : 0
          this.shareTimesUsed = res.shareTimesUsed || 0
          uni.showToast({ title: `分享成功，+${n} 次下载`, icon: 'none' })
          if (this.downloadRemaining > 0) {
            this.showLimitDialog = false
          }
        }
      }).catch(() => {
        // request.js 已 toast 错误文案；刷新配额同步按钮状态（如次数用完）
        getDownloadStatus().then(res => {
          if (res.code === 200) {
            this.shareTimesUsed = res.shareTimesUsed || 0
            this.shareRewardCount = res.reward || 0
          }
        }).catch(() => {})
      }).finally(() => {
        this.rewardClaiming = false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.detail-page {
  position: relative;
  width: 100%;
  height: 100vh;
  background: #000;
}

.detail-swiper {
  width: 100%;
  height: 100vh;
}

.swiper-item-content {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: auto;
}

.image-wrap {
  position: relative;
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;

  .wallpaper-image {
    width: 100%;
    display: block;
  }

  .image-loading {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;

    .loading-spinner {
      width: 56rpx;
      height: 56rpx;
      border: 4rpx solid rgba(255, 255, 255, 0.2);
      border-top-color: #38BDF8;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    .loading-text {
      margin-top: 20rpx;
      font-size: $font-caption;
      color: rgba(255, 255, 255, 0.7);
    }
  }
}

.action-bar {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: calc(40rpx + env(safe-area-inset-bottom));
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  border-radius: $radius-2xl;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(20rpx);
  z-index: 10;

  .action-info {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
    flex-wrap: wrap;

    .action-title {
      font-size: $font-body;
      font-weight: $font-weight-semibold;
      color: $text-primary;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      max-width: 50%;
    }

    .action-meta {
      margin-left: 12rpx;
      font-size: $font-caption;
      color: $text-tertiary;
    }
  }

  .action-btn {
    display: flex;
    align-items: center;
    height: 80rpx;
    padding: 0 32rpx;
    border-radius: $radius-full;
    background: linear-gradient(135deg, #38BDF8 0%, #0EA5E9 100%);
    box-shadow: 0 6rpx 18rpx rgba(56, 189, 248, 0.4);

    .action-btn-text {
      margin-left: 10rpx;
      color: #fff;
      font-size: $font-body;
      font-weight: $font-weight-medium;
    }

    &.disabled {
      opacity: 0.7;
    }

    &:active {
      transform: scale(0.97);
    }
  }

  .action-right {
    display: flex;
    align-items: center;
    gap: 16rpx;
  }

  .like-btn {
    display: flex;
    align-items: center;
    gap: 4rpx;
    padding: 8rpx 16rpx;
    border-radius: $radius-full;
    margin-left: 12rpx;

    .like-count {
      font-size: $font-caption;
      color: #666;
      min-width: 24rpx;
      text-align: center;
    }

    .wallpaper-action-icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 40rpx;
      height: 40rpx;
      font-size: 40rpx;
      line-height: 40rpx;
      color: #666;
      font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Arial, sans-serif;

      &.like-icon.active {
        color: #ff4757;
      }

    }

    &.likeing {
      opacity: 0.6;
    }

    &:active {
      transform: scale(0.95);
    }
  }
}

.privacy-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  z-index: 99;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
  background: rgba(0, 0, 0, 0.55);

  .privacy-panel {
    width: 100%;
    padding: 36rpx 32rpx 28rpx;
    border-radius: $radius-xl;
    background: #fff;
    box-shadow: 0 16rpx 48rpx rgba(0, 0, 0, 0.25);
  }

  .privacy-title {
    display: block;
    font-size: $font-h2;
    font-weight: $font-weight-semibold;
    color: $text-primary;
    text-align: center;
  }

  .privacy-desc {
    display: block;
    margin-top: 20rpx;
    font-size: $font-body;
    line-height: 1.6;
    color: $text-secondary;
  }

  .privacy-link {
    margin-top: 18rpx;
    font-size: $font-caption;
    color: #0EA5E9;
  }

  .privacy-actions {
    display: flex;
    align-items: center;
    gap: 18rpx;
    margin-top: 30rpx;
  }

  .privacy-btn {
    flex: 1;
    height: 76rpx;
    line-height: 76rpx;
    margin: 0;
    padding: 0;
    border-radius: $radius-full;
    font-size: $font-body;

    &::after {
      border: none;
    }
  }

  .privacy-btn-cancel {
    color: $text-secondary;
    background: $gray-100;
  }

  .privacy-btn-confirm {
    color: #fff;
    background: linear-gradient(135deg, #38BDF8 0%, #0EA5E9 100%);
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
