import store from '@/store'
import config from '@/config'
import {getToken} from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import {showConfirm, tansParams, toast} from '@/utils/common'
// #ifdef APP-PLUS
import offlineProxy from '@/utils/offlineProxy'
import { isOnline } from '@/utils/network'
// #endif

let timeout = 10000
const baseUrl = config.baseUrl

const request = config => {
  // 是否需要设置 token
  const isToken = (config.headers || {}).isToken === false
  config.header = config.header || {}
  if (getToken() && !isToken) {
    config.header['Authorization'] = 'Bearer ' + getToken()
  }
  // get请求映射params参数
  if (config.params) {
    let url = config.url + '?' + tansParams(config.params)
    url = url.slice(0, -1)
    config.url = url
  }

  // ★ 离线代理拦截（仅 APP-PLUS 且离线时生效）
  // #ifdef APP-PLUS
  if (config.offline !== false && !config._isSyncCall) {
    if (!isOnline.value) {
      return offlineProxy.handle(config)
    }
  }
  // #endif

  return new Promise((resolve, reject) => {
    uni.request({
      method: config.method || 'get',
      timeout: config.timeout || timeout,
      url: config.baseUrl || baseUrl + config.url,
      data: config.data,
      header: config.header,
      dataType: 'json'
    }).then(response => {
      let error, res
      if (Array.isArray(response)) {
        [error, res] = response
      } else {
        error = null
        res = response
      }
      if (error) {
        toast('后端接口连接异常')
        reject('后端接口连接异常')
        return
      }
      const code = res.data.code || 200
      const msg = errorCode[code] || res.data.msg || errorCode['default']
      if (code === 401) {
        showConfirm('登录状态已过期，您可以继续留在该页面，或者重新登录?').then(res => {
          if (res.confirm) {
            store.dispatch('LogOut').then(res => {
              uni.reLaunch({url: '/pages/login'})
            })
          }
        })
        reject('无效的会话，或者会话已过期，请重新登录。')
      } else if (code === 500) {
        toast(msg)
        reject('500')
      } else if (code !== 200) {
        toast(msg)
        reject(code)
      }

      // ★ 在线 GET 请求成功后，自动缓存响应数据（供离线时使用）
      // #ifdef APP-PLUS
      if (!config._isSyncCall && (config.method || 'get').toLowerCase() === 'get' && code === 200) {
        try { offlineProxy.cacheResponse(config, res.data) } catch(e) {}
      }
      // #endif

      resolve(res.data)
    })
      .catch(error => {
        let {message} = error
        if (message === 'Network Error') {
          message = '后端接口连接异常'
        } else if (message.includes('timeout')) {
          message = '系统接口请求超时'
        } else if (message.includes('Request failed with status code')) {
          message = '系统接口' + message.substr(message.length - 3) + '异常'
        }
        toast(message)
        reject(error)
      })
  })
}

export default request
