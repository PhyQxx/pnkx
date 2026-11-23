<!--
 * @File: tools
 * @Author: PHY
 * @Description: 工具箱（免登录，工具列表由后台配置 sys.tools.list 驱动）
 * v2：按 group 分组渲染 + 最近使用 + NEW 角标（字段缺省时向后兼容旧配置）
 *     分组区块顺序跟随后台配置数组中各组首次出现的顺序，后台排序对小程序完全生效
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
      <!-- 最近使用 -->
      <template v-if="recentTools.length > 0">
        <view class="section-title">
          <view class="section-accent" style="background-color: #38BDF8;"></view>
          <text class="section-text">最近使用</text>
        </view>
        <scroll-view class="recent-strip" scroll-x>
          <view
            v-for="tool in recentTools"
            :key="'recent-' + tool.id"
            class="recent-chip"
            @click="openTool(tool)"
          >
            <text class="recent-chip__text">{{ tool.name }}</text>
          </view>
        </scroll-view>
      </template>

      <!-- 分组工具列表 -->
      <template v-if="toolList.length > 0">
        <view
          v-for="group in groupedTools"
          :key="group.key"
          class="tool-group"
        >
          <view class="section-title">
            <view class="section-accent" :style="{ backgroundColor: group.accent }"></view>
            <text class="section-text">{{ group.label }}</text>
          </view>
          <view class="tool-grid">
            <view
              v-for="tool in group.tools"
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
                <view class="tool-name-line">
                  <text class="tool-name">{{ tool.name }}</text>
                  <text v-if="tool.isNew" class="badge-new">NEW</text>
                </view>
                <text class="tool-desc" v-if="tool.desc">{{ tool.desc }}</text>
              </view>
              <view class="tool-arrow">
                <uni-icons type="right" size="18" :color="tool.color || '#38BDF8'" />
              </view>
            </view>
          </view>
        </view>
      </template>

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
import { recordToolUse } from '@/utils/toolLog'
import config from '@/config'

/** 分组元信息（label 分组标题 / accent 主题色）；展示顺序由后台配置中各组首次出现顺序决定 */
const TOOL_GROUPS = [
  { key: 'finance', label: '财务计算', accent: '#6C9EFF' },
  { key: 'life', label: '生活工具', accent: '#FB923C' },
  { key: 'fun', label: '趣味 AI', accent: '#F472B6' },
  { key: 'default', label: '常用工具', accent: '#38BDF8' }
]

/** 最近使用存储 key 与上限 */
const RECENT_KEY = 'tools_recent_usage'
const RECENT_MAX = 6

export default {
  name: 'Tools',
  data() {
    return {
      loading: true,
      // 当前小程序/应用版本号
      appVersion: '1.0.0',
      // 工具列表（由后台配置驱动）
      toolList: [],
      // 最近使用（toolId 数组，新使用的在前）
      recentIds: [],
      // 内置默认工具（后台未配置时的兜底，保证页面可用）
      defaultTools: [
        {
          id: 'mortgage',
          name: '房贷计算器',
          icon: 'wallet',
          color: '#6C9EFF',
          desc: '商贷 / 公积金，等额本息、本金对比',
          type: 'page',
          target: '/pages_tools/mortgage/index',
          enabled: true,
          group: 'finance',
          isNew: true
        },
        {
          id: 'tax',
          name: '个税计算器',
          icon: 'compose',
          color: '#FBBF24',
          desc: '年度累计预扣，年终奖单独计税',
          type: 'page',
          target: '/pages_tools/tax/index',
          enabled: true,
          group: 'finance',
          isNew: true
        },
        {
          id: 'exchange',
          name: '汇率换算',
          icon: 'flag',
          color: '#34D399',
          desc: '实时汇率，配合记账外币场景',
          type: 'page',
          target: '/pages_tools/exchange/index',
          enabled: true,
          group: 'finance',
          isNew: true
        },
        {
          id: 'ai-pickup',
          name: 'AI 情话生成',
          icon: 'heart',
          color: '#A78BFA',
          desc: '土味情话、藏头诗、三行情书',
          type: 'page',
          target: '/pages_tools/ai-pickup/index',
          enabled: true,
          group: 'fun',
          isNew: true
        },
        {
          id: 'what-to-eat',
          name: '今天吃什么',
          icon: 'shop',
          color: '#F472B6',
          desc: '选择困难症救星，转一转决定',
          type: 'page',
          target: '/pages_tools/what-to-eat/index',
          enabled: true,
          group: 'fun',
          isNew: true
        },
        {
          id: 'date-calc',
          name: '日期计算器',
          icon: 'calendar',
          color: '#FB923C',
          desc: '日期间隔、工作日、农历查询',
          type: 'page',
          target: '/pages_tools/date-calc/index',
          enabled: true,
          group: 'life',
          isNew: true
        },
        {
          id: 'img-compress',
          name: '图片压缩',
          icon: 'images',
          color: '#60A5FA',
          desc: '批量压缩，博客传图更省流量',
          type: 'page',
          target: '/pages_tools/img-compress/index',
          enabled: true,
          group: 'life',
          isNew: true
        },
        {
          id: 'wallpaper',
          name: '壁纸下载',
          icon: 'picture',
          color: '#38BDF8',
          desc: '精选高清壁纸，一键保存到相册',
          type: 'page',
          target: '/pages_wallpaper/wallpaper/index',
          enabled: true,
          group: 'life'
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
  computed: {
    /**
     * 按 group 分桶渲染：分组区块顺序 = 后台数组中各组「首次出现」的顺序，
     * 组内工具按数组顺序 —— 后台上移/下移对小程序展示完全生效（含跨组移动）。
     * 旧配置无 group 字段、或出现未知 group 时归入「常用工具」组（向后兼容）
     */
    groupedTools() {
      const buckets = {}
      this.toolList.forEach(tool => {
        const known = TOOL_GROUPS.some(g => g.key === tool.group)
        const key = known ? tool.group : 'default'
        if (!buckets[key]) buckets[key] = []
        buckets[key].push(tool)
      })
      // Object.keys 对字符串键保持插入顺序，即首次出现顺序
      return Object.keys(buckets)
        .map(key => {
          const meta = TOOL_GROUPS.find(g => g.key === key)
          return {
            key,
            label: meta.label,
            accent: meta.accent,
            tools: buckets[key]
          }
        })
    },
    /**
     * 最近使用的工具对象列表（过滤已下线/已禁用的 id）
     */
    recentTools() {
      return this.recentIds
        .map(id => this.toolList.find(tool => tool.id === id))
        .filter(Boolean)
    }
  },
  onLoad() {
    this.loadVersion()
    this.loadRecentIds()
    this.loadTools()
  },
  onShow() {
    // 每次展示时刷新，保证后台改动及时生效
    if (this.$data.__loaded) {
      this.loadTools()
      this.loadRecentIds()
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
        images: 'images',
        article: 'compose',
        wallet: 'wallet',
        compose: 'compose',
        shop: 'shop',
        calendar: 'calendar',
        heart: 'heart',
        flag: 'flag'
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
     * 从本地缓存读取最近使用记录
     */
    loadRecentIds() {
      try {
        const cached = uni.getStorageSync(RECENT_KEY)
        this.recentIds = Array.isArray(cached) ? cached.slice(0, RECENT_MAX) : []
      } catch (e) {
        console.warn('读取最近使用缓存失败', e)
        this.recentIds = []
      }
    },

    /**
     * 记录最近使用（去重、置顶、截断）
     * @param {string} id 工具 id
     */
    recordRecent(id) {
      const list = this.recentIds.filter(item => item !== id)
      list.unshift(id)
      this.recentIds = list.slice(0, RECENT_MAX)
      try {
        uni.setStorageSync(RECENT_KEY, this.recentIds)
      } catch (e) {
        console.warn('保存最近使用缓存失败', e)
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
      // 使用打点（fire-and-forget，失败静默；page/web 工具都记）
      recordToolUse(tool)
      if (tool.type === 'web') {
        this.openWeb(tool.target, tool.name)
      } else {
        // page 类型记录最近使用后跳转（兼容后台老配置里的旧分包路径）
        this.recordRecent(tool.id)
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

/* 最近使用横滑条 */
.recent-strip {
  white-space: nowrap;
  margin-bottom: $spacing-xs;

  .recent-chip {
    display: inline-flex;
    align-items: center;
    background: $bg-card;
    border-radius: $radius-full;
    padding: 14rpx 24rpx;
    margin-right: 16rpx;
    box-shadow: $shadow-card;
    transition: transform $duration-fast $ease-default;

    &:active {
      transform: scale(0.96);
    }

    &__text {
      font-size: $font-caption;
      color: $text-secondary;
      font-weight: $font-weight-medium;
    }
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

  .tool-name-line {
    display: flex;
    align-items: center;
  }

  .tool-name {
    font-size: $font-h3;
    font-weight: $font-weight-semibold;
    color: $text-primary;
  }

  .badge-new {
    font-size: 18rpx;
    font-weight: $font-weight-bold;
    color: $text-inverse;
    background: $danger;
    border-radius: 10rpx;
    padding: 3rpx 10rpx;
    margin-left: 12rpx;
    letter-spacing: 1rpx;
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
