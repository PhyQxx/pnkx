import router from './router'
import store from './store'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { MODE_MOBILE, resolveMode } from '@/utils/device'
import { isMobilePath, NEUTRAL_PATHS } from '@/mobile/router/guards'
import { resolveMobileTarget, resolvePcTarget } from '@/mobile/router/map'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/auth-redirect', '/bind', '/register', '/homepage']

router.beforeEach((to, from, next) => {
    NProgress.start()
    if (to.path === '/note-share') { next(); NProgress.done(); return }
    // ---- PC/APP 设备分流：按视口（或手动覆盖）自动切换两种形态 ----
    if (!NEUTRAL_PATHS.includes(to.path)) {
        if (resolveMode() === MODE_MOBILE && !isMobilePath(to.path)) {
            // 手机视口访问 PC 路由 → 跳对应移动页（无映射时落 APP 首页并提示）
            const { path, fallback } = resolveMobileTarget(to.path)
            if (path !== to.path) {
                if (fallback) {
                    ElMessage.info('该功能请使用电脑版访问')
                }
                next({ path, query: to.query, replace: true })
                NProgress.done()
                return
            }
        } else if (resolveMode() !== MODE_MOBILE && isMobilePath(to.path)) {
            // 桌面视口访问移动路由 → 跳对应 PC 页
            const target = resolvePcTarget(to.path)
            next({ path: target, replace: true })
            NProgress.done()
            return
        }
    }
    if (getToken()) {
        if (to.path === '/login' || to.path === '/homepage') {
            next({ path: '/index' })
            NProgress.done()
        } else {
            if (store.getters.roles.length === 0) {
                // 判断当前用户是否已拉取完user_info信息
                store.dispatch('GetInfo').then(res => {
                    // 拉取user_info
                    const roles = res.roles
                    store.dispatch('GenerateRoutes', { roles }).then(accessRoutes => {
                        // 根据roles权限生成可访问的路由表
                        accessRoutes.forEach(route => router.addRoute(route))
                        next({ ...to, replace: true }) // hack方法 确保addRoutes已完成
                    })
                }).catch(err => {
                    store.dispatch('LogOut').then(() => {
                        const msg = err?.response?.data?.msg || err?.message || JSON.stringify(err)
                        ElMessage.error(msg)
                        next({ path: '/login' })
                    })
                })
            } else {
                next()
            }
        }
    } else {
        if (whiteList.includes(to.path)) {
            next()
        } else {
            next({ path: '/login' })
            NProgress.done()
        }
    }
})

router.afterEach(() => {
    NProgress.done()
})
