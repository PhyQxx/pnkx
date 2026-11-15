/**
 * PC/APP 双端模式检测
 *
 * 视口 < 768px → APP 效果（/m/* 路由树）；>= 768px → PC 效果（现有后台）。
 * 支持手动强制覆盖：localStorage['pnkx-force-mode'] = 'pc' | 'mobile'，
 * 也可通过 URL 参数 ?force=pc / ?force=mobile 设置（设置页提供清除入口）。
 *
 * 断点取 767.98px 与 DESIGN.md §8 的移动档（<768px）对齐；
 * PC 壳内部 992px 侧栏抽屉行为（layout/mixin/ResizeHandler.js）不受影响。
 */
import { computed, ref } from 'vue'

export const MODE_PC = 'pc'
export const MODE_MOBILE = 'mobile'

const FORCE_KEY = 'pnkx-force-mode'
const MOBILE_QUERY = '(max-width: 767.98px)'

function readForced() {
    try {
        const v = localStorage.getItem(FORCE_KEY)
        return v === MODE_PC || v === MODE_MOBILE ? v : null
    } catch (e) {
        return null
    }
}

const mobileViewport = ref(false)
const forcedMode = ref(readForced())

// URL ?force=pc|mobile 一次性应用强制模式（与应用设置里的切换等价）
if (typeof window !== 'undefined') {
    const force = new URLSearchParams(window.location.search).get('force')
    if (force === MODE_PC || force === MODE_MOBILE) {
        forcedMode.value = force
        try {
            localStorage.setItem(FORCE_KEY, force)
        } catch (e) {
            /* ignore */
        }
    }
    if (window.matchMedia) {
        const mql = window.matchMedia(MOBILE_QUERY)
        mobileViewport.value = mql.matches
        if (mql.addEventListener) {
            mql.addEventListener('change', ev => {
                mobileViewport.value = ev.matches
            })
        } else if (mql.addListener) {
            // Safari < 14
            mql.addListener(ev => {
                mobileViewport.value = ev.matches
            })
        }
    } else {
        mobileViewport.value = window.innerWidth < 768
    }
}

/** 当前视口是否为移动宽度（不含强制覆盖） */
export function isMobileViewport() {
    return mobileViewport.value
}

/** 手动强制模式：'pc' | 'mobile' | null（跟随视口） */
export function getForcedMode() {
    return forcedMode.value
}

export function setForcedMode(mode) {
    forcedMode.value = mode === MODE_PC || mode === MODE_MOBILE ? mode : null
    try {
        if (forcedMode.value) {
            localStorage.setItem(FORCE_KEY, forcedMode.value)
        } else {
            localStorage.removeItem(FORCE_KEY)
        }
    } catch (e) {
        /* ignore */
    }
}

/** 当前生效模式（强制优先，其次视口） */
export function resolveMode() {
    return forcedMode.value || (mobileViewport.value ? MODE_MOBILE : MODE_PC)
}

/** 响应式模式引用，跨断点/手动切换时变化，供 App.vue 等全局监听 */
export const modeRef = computed(() => resolveMode())
