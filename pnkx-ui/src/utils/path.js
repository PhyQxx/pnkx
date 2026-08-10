/**
 * Browser-compatible path.resolve for Vue route path joining
 * @param {string} basePath
 * @param {string} routePath
 * @returns {string}
 */
export function resolvePath(basePath, routePath) {
    if (routePath.startsWith('/')) return routePath
    if (basePath.endsWith('/')) return basePath + routePath
    return basePath + '/' + routePath
}
