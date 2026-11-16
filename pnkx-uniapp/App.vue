<script>
import config from './config'
import {getToken} from '@/utils/auth'
// #ifdef APP-PLUS
import sqliteDB from '@/utils/sqliteDB'
import syncScheduler from '@/utils/syncScheduler'
import { isOnline } from '@/utils/network'
// #endif

export default {
  onLaunch: function () {
    this.initApp()
    // #ifdef APP-PLUS
    this.initOffline()
    // #endif
  },
  onShow: function () {
    // #ifdef APP-PLUS
    if (this._offlineReady) this.triggerOnlineSync()
    // #endif
  },
  methods: {
    // 初始化应用
    initApp() {
      // 初始化应用配置
      this.initConfig()
      // 检查用户登录状态
      //#ifdef H5
      this.checkLogin()
      //#endif
    },
    initConfig() {
      this.globalData.config = config
    },
    checkLogin() {
      if (!getToken()) {
        // 白名单页面（工具箱、壁纸等免登录页面）不强制跳登录
        // H5 刷新时当前页 URL 形如 http://host/#/pages/tools
        const currentPage = window.location.hash.replace(/^#/, '').split('?')[0] || '/pages/launch'
        const whiteList = ['/pages/launch', '/pages/login', '/pages/register',
          '/pages/common/webview/index', '/pages/tools',
          '/pages_wallpaper/wallpaper/index', '/pages_wallpaper/wallpaper/detail',
          '/pages_wallpaper/wallpaper/mine']
        if (whiteList.indexOf(currentPage) === -1) {
          // 携带当前路由，登录成功后跳回原页面
          const current = window.location.hash.replace(/^#/, '')
          this.$tab.reLaunch(`/pages/login?redirect=${encodeURIComponent(current)}`)
        }
      }
    },

    // ★ 离线模块初始化（仅 APP-PLUS）
    async initOffline() {
      try {
        await sqliteDB.open()
        await sqliteDB.initTables()
        this._offlineReady = true
        syncScheduler.start()
        if (isOnline.value) {
          syncScheduler.triggerSync('app_launch')
        }
        console.log('[App] 离线模块初始化完成')
      } catch (e) {
        console.error('[App] 离线模块初始化失败:', e)
      }
    },

    // ★ App 回前台时触发同步
    triggerOnlineSync() {
      try {
        if (isOnline.value) {
          syncScheduler.triggerSync('app_show')
        }
      } catch (e) {
        // 同步模块未初始化时忽略
      }
    }
  }
}
</script>

<style lang="scss">
@import '@/static/scss/index.scss';

.uni-page-head__title {
  color: $primary;
  font-weight: $font-weight-semibold;
}

uni-page {
  position: fixed;
  overflow: auto;
  background-color: $bg-page;
}

uni-page-body {
  padding: 1rpx;
}

.uni-section {
  margin: $spacing-lg $page-padding 0 $page-padding;
  border-radius: $radius-xl;
}

/* 修复 uni-popup 在生产环境中定位问题 */
.uni-popup {
  position: fixed !important;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
  pointer-events: auto;
}

.uni-popup .uni-popup__wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

/* 确保弹窗内容居中显示 */
.uni-popup.center {
  display: flex;
  justify-content: center;
  align-items: center;
}

.uni-popup.bottom {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.uni-popup.top {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
}
</style>
