/**
 * 保存到localStorage中
 * @type {string}
 */
const TokenKey = 'Admin-Token'
const ThemeKey = 'App-Theme'

export function getToken() {
    return localStorage.getItem(TokenKey)
}

export function setToken(token) {
    return localStorage.setItem(TokenKey, token)
}

export function removeToken() {
    return localStorage.removeItem(TokenKey)
}

/**
 * 获取主题设置
 * @returns {string} 'light' | 'dark'
 */
export function getTheme() {
    return localStorage.getItem(ThemeKey) || 'light'
}

/**
 * 设置主题
 * @param {string} theme 'light' | 'dark'
 */
export function setTheme(theme) {
    return localStorage.setItem(ThemeKey, theme)
}
