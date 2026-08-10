import router from './router'
import store from './store'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/auth-redirect', '/bind', '/register', '/homepage']

router.beforeEach((to, from, next) => {
    NProgress.start()
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
