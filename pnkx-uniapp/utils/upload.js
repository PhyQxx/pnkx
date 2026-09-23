import store from '@/store'
import config from '@/config'
import {getToken} from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import {showConfirm, tansParams, toast} from '@/utils/common'

let timeout = 10000
const baseUrl = config.baseUrl

const upload = config => {
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
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      timeout: config.timeout || timeout,
      url: baseUrl + config.url,
      filePath: config.filePath,
      name: config.name || 'file',
      header: config.header,
      formData: config.formData,
      success: (res) => {
        let result
        try {
          result = JSON.parse(res.data)
        } catch (e) {
          // 网关/代理返回 HTML 错误页时 res.data 不是 JSON
          toast('上传失败：服务返回异常(' + res.statusCode + ')')
          reject('上传响应解析失败')
          return
        }
        const code = result.code || 200
        const msg = errorCode[code] || result.msg || errorCode['default']
        if (code === 200) {
          resolve(result)
        } else if (code == 401) {
          showConfirm("登录状态已过期，您可以继续留在该页面，或者重新登录?").then(res => {
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
      },
      fail: (error) => {
        // uni.uploadFile 失败对象字段为 errMsg，兼容 message
        let message = (error && (error.errMsg || error.message)) || ''
        if (message === 'Network Error') {
          message = '后端接口连接异常'
        } else if (message.includes('timeout')) {
          message = '系统接口请求超时'
        } else if (message.includes('uploadFile:fail')) {
          message = '文件上传失败'
        }
        toast(message || '文件上传失败')
        reject(error)
      }
    })
  })
}

export default upload
