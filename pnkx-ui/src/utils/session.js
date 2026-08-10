
/**
 * session存
 * @param key
 * @param value
 */
export function setSession(key, value) {
    if (!key) {
        return
    }
    sessionStorage.setItem(key, JSON.stringify(value))
}

/**
 * session取
 * @param key
 * @returns {any}
 */
export function getSession(key) {
    if (!key) {
        return
    }
    const val = sessionStorage.getItem(key)
    if (val === null || val === undefined || val === '') {
        return null
    }
    try {
        return JSON.parse(val)
    } catch (e) {
        console.error('Error parsing session storage for key: ' + key, e)
        return null
    }
}

/**
 * session清除
 * @param key
 * @returns {any}
 */
export function removeSession(key) {
    if (!key) {
        return
    }
    return sessionStorage.removeItem(key)
}
