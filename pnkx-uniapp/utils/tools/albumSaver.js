/**
 * 保存图片到相册（微信授权链路封装，模式与壁纸下载 detail.vue 一致）
 *
 * 链路：微信隐私授权（requirePrivacyAuthorize）→ 曾拒绝过权限则引导 openSetting
 * → saveImageToPhotosAlbum（必要时自行弹授权窗）→ 按 errMsg 分支提示
 */

/** errMsg 是否隐私授权类错误 */
function isPrivacyError(message) {
  const msg = String(message || '').toLowerCase()
  return msg.indexOf('privacy') !== -1 || msg.indexOf('隐私') !== -1
}

/** errMsg 是否「隐私指引未声明该 scope」（后台《用户隐私保护指引》缺相册声明） */
function isPrivacyScopeUndeclared(message) {
  const msg = String(message || '').toLowerCase()
  return msg.indexOf('api scope is not declared') !== -1 ||
    msg.indexOf('not declared in the privacy agreement') !== -1
}

/** errMsg 是否权限拒绝类错误 */
function isAuthError(message) {
  const msg = String(message || '').toLowerCase()
  return isPrivacyError(msg) ||
    msg.indexOf('auth') !== -1 ||
    msg.indexOf('deny') !== -1 ||
    msg.indexOf('permission') !== -1 ||
    msg.indexOf('未授权') !== -1
}

/**
 * 微信隐私协议授权；非微信端直接通过
 * @returns {Promise<boolean>}
 */
function ensureWechatPrivacyAuthorized() {
  return new Promise(resolve => {
    // #ifndef MP-WEIXIN
    resolve(true)
    // #endif
    // #ifdef MP-WEIXIN
    const wxApi = typeof wx !== 'undefined' ? wx : null
    if (!wxApi || !wxApi.requirePrivacyAuthorize) {
      resolve(true)
      return
    }
    wxApi.requirePrivacyAuthorize({
      success: () => resolve(true),
      fail: (err) => {
        console.error('requirePrivacyAuthorize fail:', err && err.errMsg)
        uni.showModal({
          title: '隐私授权',
          content: '保存图片到相册前，需要先同意小程序隐私保护指引。',
          confirmText: '查看指引',
          success: (res) => {
            if (res.confirm && wxApi.openPrivacyContract) {
              wxApi.openPrivacyContract({
                fail: (e2) => console.error('openPrivacyContract fail:', e2 && e2.errMsg)
              })
            }
            resolve(false)
          }
        })
      }
    })
    // #endif
  })
}

/**
 * 用户是否「曾经拒绝过」相册写入权限（false 时系统不再弹窗，须引导去设置页）
 * @returns {Promise<boolean>}
 */
function checkAlbumDeniedBefore() {
  return new Promise(resolve => {
    // #ifndef MP-WEIXIN
    resolve(false)
    // #endif
    // #ifdef MP-WEIXIN
    uni.getSetting({
      success: (res) => resolve(res.authSetting && res.authSetting['scope.writePhotosAlbum'] === false),
      fail: () => resolve(false)
    })
    // #endif
  })
}

/**
 * 引导用户去设置页开启相册权限
 * @param {Function} onGranted 可选：用户在设置页开启权限后的回调（供调用方续跑保存）
 */
function promptOpenSetting(onGranted) {
  uni.showModal({
    title: '需要相册权限',
    content: '请在设置中开启「添加到相册」权限后重试。',
    confirmText: '去设置',
    success: (res) => {
      if (!res.confirm) return
      // #ifdef APP-PLUS
      if (typeof plus !== 'undefined') {
        plus.runtime.openURL('app-settings:')
      }
      // #endif
      // #ifndef APP-PLUS
      uni.openSetting({
        success: (settingRes) => {
          if (settingRes && settingRes.authSetting &&
            settingRes.authSetting['scope.writePhotosAlbum'] === true &&
            typeof onGranted === 'function') {
            onGranted()
          }
        }
      })
      // #endif
    }
  })
}

/**
 * 带授权链路的保存到相册
 * @param {string} filePath 图片路径
 * @param {Object} options
 * @param {string} options.successTitle 保存成功提示（默认 已保存到相册）
 * @returns {Promise<void>} reject 时已弹出对应提示，调用方只需 catch 兜底日志
 */
export function saveToAlbumWithAuth(filePath, options = {}) {
  return ensureWechatPrivacyAuthorized().then(granted => {
    if (!granted) return Promise.reject(new Error('privacy denied'))
    return checkAlbumDeniedBefore().then(denied => {
      if (denied) {
        promptOpenSetting()
        return Promise.reject(new Error('album auth denied before'))
      }
      return new Promise((resolve, reject) => {
        uni.saveImageToPhotosAlbum({
          filePath,
          success: () => {
            uni.showToast({
              title: options.successTitle || '已保存到相册',
              icon: 'success'
            })
            resolve()
          },
          fail: (err) => {
            const msg = (err && err.errMsg) || ''
            console.error('saveImageToPhotosAlbum fail:', msg)
            if (isPrivacyScopeUndeclared(msg)) {
              uni.showModal({
                title: '隐私指引未配置',
                content: '当前小程序隐私保护指引未声明「保存图片到相册/写入相册」用途，请先在微信公众平台补充后重新发布。',
                showCancel: false,
                confirmText: '知道了'
              })
            } else if (isAuthError(msg)) {
              // 用户在设置页开启权限后自动续跑保存（onGranted 由调用方提供）
              promptOpenSetting(options.onGranted)
            } else {
              uni.showToast({
                title: '保存失败：' + (msg || '未知错误'),
                icon: 'none',
                duration: 3000
              })
            }
            reject(new Error(msg))
          }
        })
      })
    })
  })
}
