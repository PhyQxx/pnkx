/**
 * 移动端路由判定工具（守卫与映射共用，避免循环依赖集中放这里）
 */

/** 是否为移动端路径（/m 或 /m/...） */
export function isMobilePath(path) {
    return path === '/m' || path.startsWith('/m/')
}

/** 这些页面在两端间不做自动分流（登录、免登录分享页等） */
export const NEUTRAL_PATHS = ['/login', '/auth-redirect', '/bind', '/register', '/note-share', '/404', '/401']
