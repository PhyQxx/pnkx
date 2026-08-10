import {constantRoutes} from '@/router'
import {getRouters} from '@/api/menu'
import Layout from '@/layout/index.vue'
import ParentView from '@/components/ParentView/index.vue';

const modules = import.meta.glob('/src/views/**/*.vue')

const permission = {
    state: {
        routes: [],
        addRoutes: [],
        sidebarRouters: []
    },
    mutations: {
        SET_ROUTES: (state, routes) => {
            state.addRoutes = routes
            state.routes = constantRoutes.concat(routes)
        },
        SET_SIDEBAR_ROUTERS: (state, routers) => {
            state.sidebarRouters = constantRoutes.concat(routers)
        },
    },
    actions: {
        // 生成路由
        GenerateRoutes({commit}) {
            return new Promise(resolve => {
                // 向后端请求路由数据
                getRouters().then(res => {
                    const sdata = JSON.parse(JSON.stringify(res.data))
                    const rdata = JSON.parse(JSON.stringify(res.data))
                    const sidebarRoutes = filterAsyncRouter(sdata)
                    const rewriteRoutes = filterAsyncRouter(rdata, true)
                    rewriteRoutes.push({path: '/:pathMatch(.*)*', redirect: '/404', hidden: true})
                    commit('SET_ROUTES', rewriteRoutes)
                    commit('SET_SIDEBAR_ROUTERS', sidebarRoutes)
                    resolve(rewriteRoutes)
                })
            })
        }
    }
}

// 遍历后台传来的路由字符串，转换为组件对象
function filterAsyncRouter(asyncRouterMap, isRewrite = false) {
    return asyncRouterMap.filter(route => {
        // 修复后端返回的路径双斜杠问题（如 //blog → /blog）
        if (route.path && route.path.startsWith('//')) {
            route.path = route.path.substring(1)
        }
        if (isRewrite && route.children) {
            route.children = filterChildren(route.children)
        }
        if (route.component) {
            // Layout ParentView 组件特殊处理
            if (route.component === 'Layout') {
                route.component = Layout
            } else if (route.component === 'ParentView') {
                route.component = ParentView
            } else {
                route.component = loadView(route.component)
            }
        }
        if (route.children != null && route.children && route.children.length) {
            route.children = filterAsyncRouter(route.children, route, isRewrite)
        }
        return true
    })
}

function filterChildren(childrenMap) {
    var children = []
    childrenMap.forEach((el, index) => {
        if (el.children && el.children.length) {
            if (el.component === 'ParentView') {
                el.children.forEach(c => {
                    const parentPath = el.path.endsWith('/') ? el.path.slice(0, -1) : el.path
                    const childPath = c.path.startsWith('/') ? c.path.slice(1) : c.path
                    c.path = parentPath + '/' + childPath
                    if (c.children && c.children.length) {
                        children = children.concat(filterChildren(c.children, c))
                        return
                    }
                    children.push(c)
                })
                return
            }
        }
        children = children.concat(el)
    })
    return children
}

export const loadView = (view) => { // 路由懒加载
    let path = `/src/views/${view}.vue`
    let loader = modules[path]
    if (!loader) {
        path = `/src/views/${view}/index.vue`
        loader = modules[path]
    }
    if (loader) {
        return loader
    }
    console.error(`Dynamic import not found: /src/views/${view}.vue 或 /src/views/${view}/index.vue`)
    return () => import('@/views/error/404.vue')
}

export default permission
