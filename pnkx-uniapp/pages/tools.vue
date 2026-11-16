<!--
 * @File: tools
 * @Author: PHY
 * @Description: 工具箱（免登录，工具列表由后台配置 sys.tools.list 驱动）
-->
<template>
  <view class="tools-page">
    <!-- 顶部头部 -->
    <view class="tools-header">
      <view class="header-bg">
        <view class="header-decor header-decor--circle"></view>
        <view class="header-decor header-decor--dot"></view>
      </view>
      <view class="header-content">
        <text class="header-subtitle">实用小工具集合，免登录即用</text>
      </view>
    </view>

    <!-- 工具列表 -->
    <view class="tools-body">
      <view class="section-title">
        <view class="section-accent"></view>
        <text class="section-text">全部工具</text>
      </view>

      <!-- 工具宫格 -->
      <view class="tool-grid" v-if="toolList.length > 0">
        <view
          v-for="tool in toolList"
          :key="tool.id"
          class="tool-card"
          :style="{ '--tool-color': tool.color || '#38BDF8' }"
          @click="openTool(tool)"
        >
          <view class="tool-icon">
            <uni-icons
              v-if="getToolIconType(tool)"
              :type="getToolIconType(tool)"
              size="30"
              :color="tool.color || '#38BDF8'"
            />
            <svg-icon
              v-else
              :icon-class="tool.icon"
              size="56rpx"
              class-name="tool-icon-svg"
            />
          </view>
          <view class="tool-info">
            <text class="tool-name">{{ tool.name }}</text>
            <text class="tool-desc" v-if="tool.desc">{{ tool.desc }}</text>
          </view>
          <view class="tool-arrow">
            <uni-icons type="right" size="18" :color="tool.color || '#38BDF8'" />
          </view>
        </view>
      </view>

      <!-- 加载中 -->
      <view class="loading-state" v-else-if="loading">
        <view class="loading-spinner"></view>
        <text class="loading-text">加载中...</text>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-else>
        <view class="empty-icon">
          <uni-icons type="info" size="36" color="#C4CCD6" />
        </view>
        <text class="empty-text">暂无可用工具</text>
        <text class="empty-hint">请在后台「小程序配置」中配置工具列表</text>
      </view>
    </view>

    <!-- 当前版本号（贴页面底部） -->
    <view class="version-footer">
      <text class="version-text">当前版本 v{{ appVersion }}</text>
    </view>

    <!-- 底部留白 -->
    <view class="tools-bottom-safe"></view>
  </view>
</template>

<script>
import { getClientConfigKey } from '@/api/system/clientConfig'
import config from '@/config'

export default {
  name: 'Tools',
  data() {
    return {
      loading: true,
      // 当前小程序/应用版本号
      appVersion: '1.0.0',
      // 工具列表（由后台配置驱动）
      toolList: [],
      // 内置默认工具（后台未配置时的兜底，保证页面可用）
      defaultTools: [
        {
          id: 'wallpaper',
          name: '壁纸下载',
          icon: 'picture',
          color: '#38BDF8',
          desc: '精选高清壁纸，一键保存到相册',
          type: 'page',
          target: '/pages_wallpaper/wallpaper/index',
          enabled: true
        },
        {
          id: 'blog',
          name: '博客后台',
          icon: 'article',
          color: '#60A5FA',
          desc: '管理文章、照片与博客内容',
          type: 'web',
          target: config.appInfo.site_url,
          enabled: true
        }
      ]
    }
  },
  onLoad() {
    this.loadVersion()
    this.loadTools()
  },
  onShow() {
    // 每次展示时刷新，保证后台改动及时生效
    if (this.$data.__loaded) {
      this.loadTools()
    }
  },
  // 开启「转发给朋友」（需同时存在，朋友圈分享才会生效）
  onShareAppMessage() {
    return {
      title: 'Pei你看雪 · 工具箱',
      path: '/pages/tools'
    }
  },
  // 开启「分享到朋友圈」
  onShareTimeline() {
    return {
      title: 'Pei你看雪 · 工具箱'
    }
  },
  methods: {
    /**
     * 工具台优先使用 uni-icons 字体图标。
     * 真机上本地 SVG 图片存在兼容差异，字体图标更稳定。
     */
    getToolIconType(tool) {
      const icon = tool && tool.icon
      const map = {
        picture: 'image',
        article: 'compose'
      }
      return map[icon] || ''
    },

    /**
     * 读取当前版本号
     * 微信小程序：取小程序后台提交发布时填写的版本号（最准确，发布后会变化）
     * 其他端：回退到固定值
     */
    loadVersion() {
      try {
        // #ifdef MP-WEIXIN
        const accountInfo = wx.getAccountInfoSync()
        const version = accountInfo && accountInfo.miniProgram && accountInfo.miniProgram.version
        if (version) this.appVersion = version
        // #endif
      } catch (e) {
        console.warn('读取版本号失败', e)
      }
    },

    /**
     * 从后台加载工具列表配置
     * 配置 key: sys.tools.list，值为 JSON 数组
     */
    loadTools() {
      this.loading = true
      getClientConfigKey('sys.tools.list').then(res => {
        // 后端 AjaxResult 把配置值放 data 字段；兼容个别版本可能落到 msg 的情况
        const raw = (res && res.data !== undefined && res.data !== null)
          ? res.data
          : (res && res.msg)
        let list = []
        if (raw && typeof raw === 'string') {
          try {
            list = JSON.parse(raw)
          } catch (e) {
            console.warn('工具列表配置解析失败，使用默认配置', e)
          }
        } else if (Array.isArray(raw)) {
          list = raw
        }
        // 过滤无效项 + 禁用项（enabled !== false 兼容老数据，未配置视为启用）
        list = (list || []).filter(t => t && t.name && t.enabled !== false)
        this.toolList = list.length > 0 ? list : this.defaultTools
        this.$data.__loaded = true
      }).catch(() => {
        // 接口异常时使用默认配置，保证页面可用
        this.toolList = this.defaultTools
        this.$data.__loaded = true
      }).finally(() => {
        this.loading = false
      })
    },

    /**
     * 打开工具
     * @param tool 工具配置 {type: 'page'|'web', target: 路径或URL}
     */
    openTool(tool) {
      if (!tool.target) {
        uni.showToast({ title: '该工具未配置跳转地址', icon: 'none' })
        return
      }
      if (tool.type === 'web') {
        this.openWeb(tool.target, tool.name)
      } else {
        // page 类型，默认跳转（兼容后台老配置里的旧分包路径）
        this.$tab.navigateTo(this.normalizeToolTarget(tool.target))
      }
    },

    /**
     * 规范化工具跳转路径
     * 兼容后台 sys.tools.list 中残留的旧分包路径，避免老配置跳转到已迁移的页面时报错：
     *   - pages_life/wallpaper/* → pages_wallpaper/wallpaper/*
     *   - pages_life/*           → pages_blog/*           （博客工具已整体迁移）
     * 同时补全前导 '/'，并过滤可能的重复斜杠。
     */
    normalizeToolTarget(target) {
      let t = (target || '').trim()
      if (!t) return t
      // 旧路径映射到新分包
      t = t
        .replace(/^\/?pages_life\/wallpaper\//, '/pages_wallpaper/wallpaper/')
        .replace(/^\/?pages_life\//, '/pages_blog/')
      // 补全前导斜杠
      if (t.indexOf('/') !== 0) {
        t = '/' + t
      }
      return t
    },

    /**
     * 打开网页（H5 新窗口；APP/小程序用内置 web-view）
     */
    openWeb(url, title) {
      // #ifdef H5
      window.open(url, '_blank')
      return
      // #endif
      // #ifndef H5
      this.$tab.navigateTo(
        `/pages/common/webview/index?title=${encodeURIComponent(title || '')}&url=${encodeURIComponent(url)}`
      )
      // #endif
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

.tools-page {
  min-height: 100vh;
  background: $bg-page;
  display: flex;
  flex-direction: column;
}

/* 顶部头部 */
.tools-header {
  position: relative;
  background: linear-gradient(135deg, #0EA5E9 0%, #38BDF8 100%);
  padding: $spacing-md $page-padding $spacing-xl;
  border-radius: 0 0 $radius-2xl $radius-2xl;
  overflow: hidden;

  .header-bg {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    pointer-events: none;
  }

  .header-decor {
    position: absolute;
    border-radius: 50%;
    opacity: 0.1;
    background: #fff;

    &--circle {
      width: 300rpx;
      height: 300rpx;
      top: -100rpx;
      right: -60rpx;
    }

    &--dot {
      width: 120rpx;
      height: 120rpx;
      bottom: 20rpx;
      left: 60rpx;
    }
  }

  .header-content {
    position: relative;
    z-index: $z-base;
    display: flex;
  }

  .header-subtitle {
    font-size: $font-caption;
    color: rgba(255, 255, 255, 0.85);
  }
}

/* 工具列表 */
.tools-body {
  position: relative;
  z-index: $z-card;
  padding: 0 $page-padding;
  flex: 1;
}

.section-title {
  display: flex;
  align-items: center;
  margin: $spacing-lg 0 $spacing-md;

  .section-accent {
    width: 6rpx;
    height: 28rpx;
    border-radius: $radius-full;
    margin-right: $spacing-sm;
    flex-shrink: 0;
    background-color: $wallpaper;
  }

  .section-text {
    font-size: $font-body;
    font-weight: $font-weight-semibold;
    color: $text-primary;
  }
}

/* 工具宫格 */
.tool-grid {
  display: flex;
  flex-direction: column;
}

.tool-card {
  display: flex;
  align-items: center;
  padding: $spacing-md $spacing-lg;
  margin-bottom: $spacing-md;
  border-radius: $radius-xl;
  background: $bg-card;
  box-shadow: $shadow-card;
  transition: transform $duration-fast $ease-default;

  &:active {
    transform: scale(0.98);
    opacity: 0.95;
  }

  .tool-icon {
    width: 96rpx;
    height: 96rpx;
    border-radius: $radius-lg;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: $spacing-md;
    flex-shrink: 0;
    background: $gray-50;
  }

  .tool-info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
  }

  .tool-name {
    font-size: $font-h3;
    font-weight: $font-weight-semibold;
    color: $text-primary;
  }

  .tool-desc {
    margin-top: 6rpx;
    font-size: $font-caption;
    color: $text-tertiary;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .tool-arrow {
    flex-shrink: 0;
    margin-left: $spacing-sm;
  }
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;

  .loading-spinner {
    width: 48rpx;
    height: 48rpx;
    border: 4rpx solid rgba($wallpaper, 0.2);
    border-top-color: $wallpaper;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  .loading-text {
    margin-top: 20rpx;
    font-size: $font-caption;
    color: $text-tertiary;
  }
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;

  .empty-icon {
    width: 96rpx;
    height: 96rpx;
    border-radius: $radius-full;
    background-color: $gray-50;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .empty-text {
    margin-top: 24rpx;
    font-size: $font-body;
    color: $text-secondary;
  }

  .empty-hint {
    margin-top: 8rpx;
    font-size: $font-caption;
    color: $text-disabled;
  }
}

/* 版本号 */
.version-footer {
  display: flex;
  justify-content: center;
  padding: $spacing-md 0 $spacing-xs;

  .version-text {
    font-size: $font-caption;
    color: $text-disabled;
  }
}

.tools-bottom-safe {
  height: calc(env(safe-area-inset-bottom) + 40rpx);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
