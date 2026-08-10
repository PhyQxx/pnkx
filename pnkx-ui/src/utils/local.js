
/**
 * session存
 * @param key
 * @param value
 */
export function setLocal(key, value) {
    if (!key) {
        return
    }
    localStorage.setItem(key, JSON.stringify(value))
}

/**
 * session取
 * @param key
 * @returns {any}
 */
export function getLocal(key) {
    if (!key) {
        return
    }
    const val = localStorage.getItem(key)
    if (val === null || val === undefined || val === '') {
        return null
    }
    try {
        return JSON.parse(val)
    } catch (e) {
        console.error('Error parsing local storage for key: ' + key, e)
        return null
    }
}

/**
 * session清除
 * @param key
 * @returns {any}
 */
export function removeLocal(key) {
    if (!key) {
        return
    }
    return localStorage.removeItem(key)
}
