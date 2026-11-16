/**
 * PWA 安装引导工具
 *
 * - beforeinstallprompt 只在页面生命周期早期触发一次，因此监听器必须随 main.js
 *   尽早注册（延迟到「我的」页 chunk 加载后再监听就会错过事件）
 * - Android/Chromium（含桌面）：捕获到事件后可用 prompt() 主动拉起原生安装弹窗
 * - iOS Safari：没有安装事件，只能引导用户手动「分享 → 添加到主屏幕」
 * - 已以独立窗口（standalone）运行说明已安装，不再展示引导入口
 */

/** 捕获到的 deferred 安装事件（一次性，prompt 后失效） */
let deferredPrompt = null

if (typeof window !== 'undefined') {
    window.addEventListener('beforeinstallprompt', (e) => {
        // 阻止浏览器自带的迷你安装信息栏，改为在「我的」页里主动引导
        e.preventDefault()
        deferredPrompt = e
    })
    window.addEventListener('appinstalled', () => {
        // 已安装：清掉事件引用，避免引导入口残留
        deferredPrompt = null
    })
}

/** 是否捕获到可用的安装事件（可主动拉起原生安装弹窗） */
export function canPromptInstall() {
    return !!deferredPrompt
}

/**
 * 拉起原生安装弹窗。
 * @returns {Promise<'accepted'|'dismissed'|null>} 用户选择结果；无可用事件时为 null
 */
export async function promptInstall() {
    if (!deferredPrompt) return null
    deferredPrompt.prompt()
    const { outcome } = await deferredPrompt.userChoice
    // userChoice 是一次性的，无论结果如何都置空，避免重复 prompt 抛错
    deferredPrompt = null
    return outcome
}

/** 是否已以独立窗口（已安装的 app）形态运行 */
export function isStandalone() {
    if (typeof window === 'undefined') return false
    return window.matchMedia('(display-mode: standalone)').matches ||
        // iOS Safari 从主屏幕打开时的私有标记
        window.navigator.standalone === true
}

/** 是否为 iOS 设备（iPadOS 13+ 伪装桌面 UA，需靠多点触控特征识别） */
export function isIOS() {
    if (typeof navigator === 'undefined') return false
    const ua = navigator.userAgent
    return /iphone|ipad|ipod/i.test(ua) ||
        (ua.includes('Macintosh') && navigator.maxTouchPoints > 1)
}
