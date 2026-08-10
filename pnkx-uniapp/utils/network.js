/**
 * 网络状态监听模块
 * 提供响应式的网络连接状态，供离线代理和同步调度器使用
 */
import { ref } from 'vue'

const isOnline = ref(true)
const networkType = ref('unknown')

/** 网络恢复回调（由 syncScheduler 注册） */
let _onNetworkRestore = null

export function setOnNetworkRestore(cb) {
  _onNetworkRestore = cb
}

// #ifdef APP-PLUS
try {
  uni.getNetworkType({
    success: (res) => {
      networkType.value = res.networkType
      isOnline.value = res.networkType !== 'none'
    }
  })
} catch (e) {
  console.warn('[NetMonitor] getNetworkType 初始化失败:', e)
}

uni.onNetworkStatusChange((res) => {
  const wasOffline = !isOnline.value
  isOnline.value = res.isConnected
  networkType.value = res.networkType

  if (wasOffline && res.isConnected) {
    // 网络恢复时触发同步（延迟 1 秒，等网络稳定）
    setTimeout(() => {
      if (_onNetworkRestore) _onNetworkRestore()
    }, 1000)
  }
})
// #endif

// #ifndef APP-PLUS
// 非 APP 环境始终在线
isOnline.value = true
networkType.value = 'unknown'
// #endif

export { isOnline, networkType }
