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
        this.$tab.reLaunch('/pages/login')
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
@import '@/static/scss/global.scss';
@import '@/static/scss/colorui.css';
@import '@/static/font/iconfont.css';

.uni-page-head__title {
  color: $text-primary;
  font-weight: $font-weight-semibold;
}

.uni-page-head .uni-btn-icon,
.uni-page-head .uni-page-head-btn,
.uni-page-head .uni-page-head__btn {
  color: $text-primary !important;
}

.uni-page-head {
  background: rgba(248, 251, 255, 0.88) !important;
  border-bottom: 1rpx solid rgba(215, 229, 247, 0.66) !important;
  -webkit-backdrop-filter: blur(20px);
  backdrop-filter: blur(20px);
}

uni-page {
  position: fixed;
  overflow: auto;
  background-color: $bg-page;
  background-image:
    linear-gradient(180deg, rgba(246, 250, 255, 0.08) 0%, $bg-page 520rpx),
    url('/static/images/glacier-aurora-bg.png');
  background-repeat: no-repeat;
  background-position: top center;
  background-size: 100% auto;
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
