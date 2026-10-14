import {constantRoutes} from '@/router'
import {getRouters} from '@/api/menu'
import Layout from '@/layout/index.vue'
import WorkbenchLayout from '@/views/workbench/layout.vue'
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
                    // 工作台镜像路由：同样的功能页挂在新外壳下（/workbench 前缀），
                    // 使「记一笔/新建笔记」等在保留新版侧栏的右侧内容区打开；旧路由不受影响
                    const workbenchRoutes = rewriteRoutes
                        .map(route => toWorkbenchRoute(route))
                        .filter(Boolean)
                    commit('SET_ROUTES', rewriteRoutes)
                    commit('SET_SIDEBAR_ROUTERS', sidebarRoutes)
                    resolve(rewriteRoutes.concat(workbenchRoutes))
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


/**
 * 将旧后台动态路由克隆为工作台版本：
 * - 顶层 Layout 组件替换为 WorkbenchLayout，路径加 /workbench 前缀
 * - 子路由绝对路径转为相对（避免与旧路由冲突），名称追加 Wb 防重名
 * - 去掉 redirect（避免镜像路由跳回旧路径）
 */
function toWorkbenchRoute(route) {
    if (!route.path || route.path === '/:pathMatch(.*)*' || route.component !== Layout) {
        return null
    }
    const topAbs = route.path.startsWith('/') ? route.path : '/' + route.path
    const clone = { ...route, hidden: true }
    delete clone.redirect
    // 关键：父级组件换为新工作台外壳（此前遗漏导致镜像路由仍渲染旧 Layout）
    clone.component = WorkbenchLayout
    clone.path = '/workbench' + topAbs
    if (clone.name) clone.name = clone.name + 'Wb'
    if (route.children && route.children.length) {
        clone.children = route.children.map(child => toWorkbenchChild(child, topAbs)).filter(Boolean)
        // 目录镜像没有 index 子页：重定向到第一个可见子页，
        // 避免目录级 URL（如 /workbench/blog、/workbench/monitor）内容区空白
        const first = clone.children.find(c => !c.hidden && c.component && c.path !== '/')
        if (first) {
            clone.redirect = first.path.startsWith('/') ? first.path : clone.path + '/' + first.path
        }
    }
    return clone.children && clone.children.length ? clone : (clone.component ? { ...clone, children: undefined } : null)
}

function toWorkbenchChild(route, parentAbs) {
    // 先算出该节点在原树中的绝对 URL：相对路径拼父级，绝对路径子路由（如 笔记 /note、
    // 聊天记录 /record）本身就是挂根的完整地址，vue-router 允许子路由使用绝对路径。
    // 镜像必须逐条保证 最终 URL = /workbench + 原绝对 URL，工作台内所有跳转才可命中
    const abs = route.path && route.path.startsWith('/') ? route.path : parentAbs + '/' + (route.path || '')
    const clone = { ...route }
    if (abs === parentAbs) {
        clone.path = '/'
    } else if (abs.startsWith(parentAbs + '/')) {
        // 普通相对子路由：转为相对镜像父级（/workbench + parentAbs）的路径
        clone.path = abs.slice(parentAbs.length + 1)
    } else {
        // 绝对路径子路由：镜像同样保持绝对，避免被错误降级嵌进父级前缀
        clone.path = '/workbench' + abs
    }
    if (clone.name) clone.name = clone.name + 'Wb'
    // 打标：镜像页面在外壳内容区获得统一内边距（旧页面样式不自带 padding）
    clone.meta = { ...(route.meta || {}), wbMirror: true }
    if (route.children && route.children.length) {
        clone.children = route.children.map(child => toWorkbenchChild(child, abs)).filter(Boolean)
    }
    return clone
}
