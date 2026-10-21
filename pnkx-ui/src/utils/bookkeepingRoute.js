// 从用户已获授权的路由中定位账本，兼容后台调整菜单层级或路径。
export function getBookkeepingRoute(router, query = {}) {
    const route = router.getRoutes().find(
        route => route.meta?.viewPath === 'px/life/bookkeeping/record'
    )
    return route ? {path: route.path, query} : null
}
