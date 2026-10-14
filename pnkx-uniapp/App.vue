<script>
import config from './config'
import {getToken} from '@/utils/auth'
import { parsePushPayload, resolvePushRoute } from '@/utils/pushRouter'
// #ifdef APP-PLUS
import sqliteDB from '@/utils/sqliteDB'
import syncScheduler from '@/utils/syncScheduler'
import { isOnline } from '@/utils/network'
// #endif

export default {
  onLaunch: function () {
    this.initApp()
    this.watchDarkMode()
    // #ifdef APP-PLUS
    this.initOffline()
    this.initPush()
    // #endif
  },
  onShow: function () {
    // #ifdef APP-PLUS
    if (this._offlineReady) this.triggerOnlineSync()
    // #endif
  },
  methods: {
    /** 暗色模式（§暗色）：原生 tabBar/导航栏颜色跟随系统深浅 */
    watchDarkMode() {
      // #ifdef H5 || APP-PLUS
      const apply = (dark) => {
        uni.setTabBarStyle(dark
          ? { backgroundColor: '#0F172A', borderStyle: 'black', color: '#64748B', selectedColor: '#6AA1F9' }
          : { backgroundColor: '#FFFFFF', borderStyle: 'black', color: '#8EA0B8', selectedColor: '#4F86F7' });
      };
      const mq = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;
      if (mq) {
        apply(mq.matches);
        const handler = (e) => apply(e.matches);
        if (mq.addEventListener) mq.addEventListener('change', handler);
        else if (mq.addListener) mq.addListener(handler);
      }
      // #endif
    },

    /**
     * App 推送（uniPush 2.0）：登记设备 + 通知点击监听
     * 后端未配置 UNIPUSH_* 时上报仅落库不推送，无副作用
     */
    initPush() {
      try {
        uni.getPushClientId({
          success: (res) => {
            const token = getToken()
            if (!res.clientId || !token) return
            uni.request({
              url: config.baseUrl + '/system/push/device',
              method: 'POST',
              header: {'Authorization': 'Bearer ' + token},
              data: {
                clientId: res.clientId,
                platform: uni.getSystemInfoSync().platform,
                appVersion: plus.runtime.version || ''
              }
            })
          }
        })
        uni.onPushMessage((msg) => {
          if (msg.type === 'click') {
            const payload = parsePushPayload(msg)
            if (!payload) return
            const target = resolvePushRoute(payload)
            if (target === '/pages/index/index') uni.switchTab({ url: target })
            else uni.navigateTo({ url: target })
          }
        })
      } catch (e) {
        console.warn('推送初始化失败（不影响应用）', e)
      }
    },
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
    radial-gradient(120% 55% at 20% -5%, rgba($aurora-cyan, 0.92) 0%, rgba($aurora-cyan, 0) 60%), radial-gradient(110% 60% at 85% 10%, rgba($aurora-lavender, 0.85) 0%, rgba($aurora-lavender, 0) 62%);
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

/* ============================
   暗色模式（§暗色 双端）：核心表面/文字 CSS 变量
   跟随系统 prefers-color-scheme；uni.scss 的
   $bg-card/$text-primary/$border-color 等引用这些变量。
   ============================ */
page {
  --txt-1: #123B70;
  --txt-2: #4F627B;
  --txt-3: #8EA0B8;
  --bg-page: #F2F7FE;
  --bg-card: rgba(255, 255, 255, 0.92);
  --bg-elevated: #FFFFFF;
  --bg-hover: #F8FBFF;
  --surface-2: #F1F6FD;
  --accent-soft: #DDEAFF;
  --bd: rgba(142, 174, 218, 0.26);
  --bd-soft: rgba(142, 174, 218, 0.15);
  /* 极光渐变（RGB 三元组，供 rgba(var(--aurora-1), x) 使用） */
  --aurora-1: 221, 248, 255;
  --aurora-2: 238, 231, 255;
  --hero-veil: rgba(244, 249, 255, 0.12);
  /* 玻璃表面 */
  --glass: rgba(255, 255, 255, 0.88);
  --glass-strong: rgba(255, 255, 255, 0.94);
}

@media (prefers-color-scheme: dark) {
  page {
    --txt-1: #E2E8F0;
    --txt-2: #94A3B8;
    --txt-3: #64748B;
    --bg-page: #0F172A;
    --bg-card: rgba(30, 41, 59, 0.72);
    --bg-elevated: #1E293B;
    --bg-hover: #1E293B;
    --surface-2: #1E293B;
    --accent-soft: rgba(106, 161, 249, 0.16);
    --bd: rgba(148, 163, 184, 0.22);
    --bd-soft: rgba(148, 163, 184, 0.14);
    --aurora-1: 38, 58, 102;
    --aurora-2: 52, 48, 105;
    --hero-veil: rgba(15, 23, 42, 0.45);
    --glass: rgba(30, 41, 59, 0.78);
    --glass-strong: rgba(30, 41, 59, 0.9);
  }
}
</style>
