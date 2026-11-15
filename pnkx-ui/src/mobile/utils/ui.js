/**
 * 移动端 UI 反馈工具（Toast/Dialog 等）
 *
 * Vant 的函数式组件（showToast 等）不会被 unplugin-vue-components 自动注入样式，
 * 统一从这里引用，样式在此集中引入一次。移动端页面请勿直接 `import { showToast } from 'vant'`。
 */
import {
    closeToast,
    showConfirmDialog,
    showDialog,
    showToast,
    showLoadingToast,
    showSuccessToast,
    showFailToast
} from 'vant'
import 'vant/es/toast/style'
import 'vant/es/dialog/style'

export {
    closeToast,
    showConfirmDialog,
    showDialog,
    showToast,
    showLoadingToast,
    showSuccessToast,
    showFailToast
}

/** 轻提示（成功） */
export function mSuccess(message) {
    showSuccessToast(message)
}

/** 轻提示（失败） */
export function mError(message) {
    showFailToast(message)
}

/** 确认框：resolve=确定 / reject=取消 */
export function mConfirm(title, message) {
    return showConfirmDialog({
        title: title || '提示',
        message: message || ''
    })
}

/** 加载中 */
export function mLoading(message, duration = 0) {
    showLoadingToast({
        message: message || '加载中...',
        duration,
        forbidClick: true
    })
}
