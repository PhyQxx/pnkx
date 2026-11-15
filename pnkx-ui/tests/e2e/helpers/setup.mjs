import { test as base, expect } from '@playwright/test'

/**
 * 共享测试夹具：
 * - 屏蔽外网（百度统计 / 腾讯验证码等第三方脚本），只放行本地 dev server
 * - 收集 pageerror（未捕获异常），用例结束默认断言为空；个别允许例外用 page.allowPageErrors()
 */
const WEB_PORT = process.env.E2E_WEB_PORT || 4173
const MOCK_PORT = process.env.E2E_MOCK_PORT || 8068

export const test = base.extend({
    page: async ({ page }, use) => {
        await page.route('**/*', route => {
            const u = route.request().url()
            if (new RegExp(`^https?://(localhost|127\\.0\\.0\\.1):(${WEB_PORT}|${MOCK_PORT})`).test(u)) {
                return route.continue()
            }
            return route.abort()
        })
        const errors = []
        page.__allowErrors = false
        page.on('pageerror', e => errors.push(String(e)))
        page.allowPageErrors = () => { page.__allowErrors = true }
        await use(page)
        if (!page.__allowErrors) {
            expect(errors, `页面未捕获异常:\n${errors.join('\n')}`).toEqual([])
        }
    }
})
export { expect }

/**
 * 限定只在某个 project（mobile/desktop）下运行的 beforeEach 钩子：
 *   test.beforeEach(skipUnlessProject('mobile'))
 */
export const skipUnlessProject = name => {
    return async ({ }, testInfo) => testInfo.skip(testInfo.project.name !== name, `仅在 ${name} 项目运行`)
}
