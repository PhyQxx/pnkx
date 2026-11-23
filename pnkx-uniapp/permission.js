import {getToken} from '@/utils/auth'

// 登录页面
const LOGIN_PAGE = "/pages/login"
const LOGIN_PAGE_PATH = LOGIN_PAGE.split('?')[0]

// 页面白名单（使用 Set 提高查找性能）
const WHITE_LIST_SET = new Set([
  '/pages/launch', '/pages/login', '/pages/register', '/pages/common/webview/index',
  '/pages/tools',
  '/pages_wallpaper/wallpaper/index', '/pages_wallpaper/wallpaper/detail',
  '/pages_wallpaper/wallpaper/mine',
  // 工具箱（pages_tools 分包，免登录工具）
  '/pages_tools/mortgage/index', '/pages_tools/tax/index', '/pages_tools/what-to-eat/index',
  '/pages_tools/ai-pickup/index', '/pages_tools/exchange/index', '/pages_tools/date-calc/index',
  '/pages_tools/img-compress/index'
])

// 白名单路径缓存（避免重复解析 URL）
const whiteListCache = new Map()

/**
 * 检查地址白名单（兼容带/不带前导斜杠、带 query 的写法）
 * @param {string} url 页面路径
 * @returns {boolean} 是否在白名单中
 */
function checkWhite(url) {
  // 检查缓存
  if (whiteListCache.has(url)) {
    return whiteListCache.get(url)
  }

  let path = url.split('?')[0]
  if (path.indexOf('/') !== 0) {
    path = '/' + path
  }

  const result = WHITE_LIST_SET.has(path)

  // 缓存结果（限制缓存大小，避免内存泄漏）
  if (whiteListCache.size > 100) {
    // 删除最早添加的缓存项
    const firstKey = whiteListCache.keys().next().value
    whiteListCache.delete(firstKey)
  }
  whiteListCache.set(url, result)

  return result
}

/**
 * 获取当前页面完整路由（含 query）
 * 用于在手动触发登录的入口处，把当前页作为登录后的回跳地址
 * @returns {string} 当前页面路径
 */
export function getCurrentPageUrl() {
  try {
    const pages = getCurrentPages()
    const current = pages[pages.length - 1]
    if (!current) return ''

    const url = '/' + current.route
    const options = current.options

    if (options && Object.keys(options).length > 0) {
      const query = Object.keys(options)
        .map(k => `${encodeURIComponent(k)}=${encodeURIComponent(options[k])}`)
        .join('&')
      return query ? `${url}?${query}` : url
    }

    return url
  } catch (e) {
    console.warn('[Permission] 获取当前页面URL失败:', e)
    return ''
  }
}

/**
 * 跳转到登录页，并携带登录前的目标路由，登录成功后可跳回
 * @param {string} redirectUrl 登录成功后要跳回的路由（带 query 也支持）；不传则用当前页
 */
export function toLogin(redirectUrl) {
  let redirect = redirectUrl || getCurrentPageUrl()

  if (redirect) {
    // 规范化：补全开头的 '/'
    if (redirect.indexOf('/') !== 0) {
      redirect = '/' + redirect
    }

    // 排除登录页自身避免死循环
    if (redirect.split('?')[0] !== LOGIN_PAGE_PATH) {
      uni.reLaunch({ url: `${LOGIN_PAGE_PATH}?redirect=${encodeURIComponent(redirect)}` })
      return
    }
  }

  uni.reLaunch({ url: LOGIN_PAGE_PATH })
}

// 需要拦截的路由方法
const NAVIGATION_METHODS = ["navigateTo", "redirectTo", "reLaunch", "switchTab"]

// 页面跳转验证拦截器
NAVIGATION_METHODS.forEach(method => {
  uni.addInterceptor(method, {
    invoke(to) {
      // 已登录状态
      if (getToken()) {
        // 如果已登录却要跳转到登录页，则重定向到首页
        if (to.url === LOGIN_PAGE || to.url === LOGIN_PAGE_PATH) {
          uni.reLaunch({ url: "/" })
          return false
        }
        return true
      }

      // 未登录状态：检查白名单
      if (checkWhite(to.url)) {
        return true
      }

      // 不在白名单中，跳转登录页
      toLogin(to.url)
      return false
    },
    fail(err) {
      console.error(`[Permission] ${method} 拦截器错误:`, err)
    }
  })
})
