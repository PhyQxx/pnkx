import store from '@/store'
import config from '@/config'
import {getToken} from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import {showConfirm, tansParams, toast} from '@/utils/common'
import { toLogin } from '@/permission'
// #ifdef APP-PLUS
import offlineProxy from '@/utils/offlineProxy'
import { isOnline } from '@/utils/network'
// #endif

// 请求配置常量
const REQUEST_CONFIG = {
  TIMEOUT: 10000,
  MAX_RETRIES: 2,
  RETRY_DELAY: 1000,
}

const baseUrl = config.baseUrl

// 请求任务管理器（用于取消请求）
const requestTaskMap = new Map()

/**
 * 生成请求唯一标识
 * @param {Object} config 请求配置
 * @returns {string} 请求唯一标识
 */
function generateRequestKey(config) {
  return `${config.method || 'get'}_${config.url}_${JSON.stringify(config.params || {})}`
}

/**
 * 延迟函数
 * @param {number} ms 延迟毫秒数
 * @returns {Promise}
 */
function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

/**
 * 处理请求错误
 * @param {Error} error 错误对象
 * @returns {string} 错误消息
 */
function handleError(error) {
  let message = (error && typeof error === 'object' && error.message)
    ? error.message
    : String(error || '')

  if (message === 'Network Error' || message === '') {
    message = '后端接口连接异常'
  } else if (message.includes('timeout')) {
    message = '系统接口请求超时'
  } else if (message.includes('Request failed with status code')) {
    message = '系统接口' + message.substr(message.length - 3) + '异常'
  }

  return message
}

/**
 * 处理响应状态码
 * @param {Object} res 响应对象
 * @param {Object} config 请求配置
 * @returns {Object} 处理结果
 */
function handleResponseCode(res, config) {
  const code = res.data.code || 200
  const msg = errorCode[code] || res.data.msg || errorCode['default']

  if (code === 401) {
    showConfirm('登录状态已过期，您可以继续留在该页面，或者重新登录?').then(res => {
      if (res.confirm) {
        store.dispatch('LogOut').then(() => {
          toLogin()
        })
      }
    })
    throw new Error('无效的会话，或者会话已过期，请重新登录。')
  } else if (code === 500) {
    toast(msg)
    throw new Error('500')
  } else if (code !== 200) {
    toast(msg)
    throw new Error(code)
  }

  // ★ 在线 GET 请求成功后，自动缓存响应数据（供离线时使用）
  // #ifdef APP-PLUS
  if (!config._isSyncCall && (config.method || 'get').toLowerCase() === 'get' && code === 200) {
    try { offlineProxy.cacheResponse(config, res.data) } catch(e) {}
  }
  // #endif

  return res.data
}

/**
 * 执行单次请求
 * @param {Object} config 请求配置
 * @returns {Promise} 请求Promise
 */
function executeRequest(config) {
  return new Promise((resolve, reject) => {
    const task = uni.request({
      method: config.method || 'get',
      timeout: config.timeout || REQUEST_CONFIG.TIMEOUT,
      url: config.baseUrl || baseUrl + config.url,
      data: config.data,
      header: config.header,
      dataType: 'json'
    })

    // 存储请求任务（用于取消）
    if (config.cancelable) {
      const requestKey = generateRequestKey(config)
      requestTaskMap.set(requestKey, task)
    }

    task.then(response => {
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

      try {
        const data = handleResponseCode(res, config)
        resolve(data)
      } catch (err) {
        reject(err.message)
      }
    }).catch(error => {
      const message = handleError(error)
      toast(message)
      reject(error)
    }).finally(() => {
      // 清理请求任务
      if (config.cancelable) {
        const requestKey = generateRequestKey(config)
        requestTaskMap.delete(requestKey)
      }
    })
  })
}

/**
 * 发送请求（支持重试）
 * @param {Object} config 请求配置
 * @param {number} retryCount 当前重试次数
 * @returns {Promise} 请求Promise
 */
const request = async (config, retryCount = 0) => {
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

  try {
    return await executeRequest(config)
  } catch (error) {
    // 判断是否需要重试
    const maxRetries = config.maxRetries ?? REQUEST_CONFIG.MAX_RETRIES
    const isRetryable = config.method?.toLowerCase() === 'get' || !config.method

    if (retryCount < maxRetries && isRetryable) {
      console.log(`[Request] 请求失败，${REQUEST_CONFIG.RETRY_DELAY}ms 后进行第 ${retryCount + 1} 次重试: ${config.url}`)
      await delay(REQUEST_CONFIG.RETRY_DELAY)
      return request(config, retryCount + 1)
    }

    throw error
  }
}

/**
 * 取消指定请求
 * @param {Object} config 请求配置
 */
request.cancel = function(config) {
  const requestKey = generateRequestKey(config)
  const task = requestTaskMap.get(requestKey)
  if (task && task.abort) {
    task.abort()
    requestTaskMap.delete(requestKey)
  }
}

/**
 * 取消所有进行中的请求
 */
request.cancelAll = function() {
  requestTaskMap.forEach((task) => {
    if (task && task.abort) {
      task.abort()
    }
  })
  requestTaskMap.clear()
}

export default request
