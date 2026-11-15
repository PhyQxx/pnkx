import { test, expect, skipUnlessProject } from './helpers/setup.mjs'
import { loginAs, clearForceMode } from './helpers/mock.mjs'

/**
 * P0 设备分流矩阵：PC/APP 双形态按视口自动切换
 */
test.describe('设备分流（mobile 项目）', () => {
    test.beforeEach(skipUnlessProject('mobile'))

    test('D1 手机视口访问 PC 首页 → 自动进入 APP 首页', async ({ page }) => {
        await loginAs(page)
        await page.goto('/index')
        await expect(page).toHaveURL(/\/m\/home$/)
        // TabBar 三项可见
        await expect(page.locator('.van-tabbar-item', { hasText: '首页' })).toBeVisible()
        await expect(page.locator('.van-tabbar-item', { hasText: '生活助手' })).toBeVisible()
        await expect(page.locator('.van-tabbar-item', { hasText: '我的' })).toBeVisible()
    })

    test('D2 手机视口访问移动路由保持不动', async ({ page }) => {
        await loginAs(page)
        await page.goto('/m/todo')
        await expect(page).toHaveURL(/\/m\/todo$/)
    })

    test('D3 手机访问 PC 专属页 → 落 APP 首页并提示', async ({ page }) => {
        await loginAs(page)
        await page.goto('/tool/gen')
        await expect(page).toHaveURL(/\/m\/home$/)
        await expect(page.getByText('该功能请使用电脑版访问')).toBeVisible({ timeout: 4000 })
    })

    test('D6 拖拽跨断点 375→1280 自动切 PC', async ({ page }) => {
        await loginAs(page)
        await clearForceMode(page)
        await page.goto('/m/home')
        await expect(page).toHaveURL(/\/m\/home$/)
        // 等应用完全挂载（TabBar 渲染）再触发跨断点，避免与首屏加载竞态
        await expect(page.locator('.van-tabbar')).toBeVisible({ timeout: 8000 })
        await page.setViewportSize({ width: 1280, height: 800 })
        // App.vue 200ms 防抖 + 路由跳转
        await expect(page).toHaveURL(/\/index$/, { timeout: 6000 })
        // PC 壳可见（侧栏）
        await expect(page.locator('.sidebar-container').first()).toBeVisible({ timeout: 8000 })
    })

    test('D7 拖拽跨断点 1280→375 自动切回 APP', async ({ page }) => {
        await loginAs(page)
        await clearForceMode(page)
        await page.setViewportSize({ width: 1280, height: 800 })
        await page.goto('/index')
        await expect(page.locator('.sidebar-container').first()).toBeVisible({ timeout: 8000 })
        await page.setViewportSize({ width: 375, height: 812 })
        await expect(page).toHaveURL(/\/m\/home$/, { timeout: 6000 })
    })

    test('D8 ?force=pc 强制电脑版（手机视口）', async ({ page }) => {
        page.allowPageErrors() // PC 工作台对空 mock 数据有既有脆弱性（null.card），非本用例关注点
        await loginAs(page)
        await clearForceMode(page)
        await page.goto('/?force=pc')
        // 强制 pc：进入 PC 壳（有 token → /index）
        await expect(page.locator('.sidebar-container').first()).toBeVisible({ timeout: 10000 })
        const forced = await page.evaluate(() => localStorage.getItem('pnkx-force-mode'))
        expect(forced).toBe('pc')
    })

    test('D9 清除强制覆盖后恢复自动分流', async ({ page }) => {
        await loginAs(page)
        await clearForceMode(page)
        // 用 URL 参数让 device.js 自己写入强制键（避免 addInitScript 每次导航重放）
        await page.goto('/?force=pc')
        await expect(page.locator('.sidebar-container').first()).toBeVisible({ timeout: 10000 })
        expect(await page.evaluate(() => localStorage.getItem('pnkx-force-mode'))).toBe('pc')
        // 清除后一次干净导航（不带 ?force、窄屏）→ 恢复自动分流
        await page.evaluate(() => localStorage.removeItem('pnkx-force-mode'))
        await page.setViewportSize({ width: 360, height: 800 })
        await page.goto('/index')
        await expect(page).toHaveURL(/\/m\/home$/, { timeout: 8000 })
    })

    test('D10 免登录分享页 /note-share 不做分流', async ({ page }) => {
        page.allowPageErrors() // 无 id 参数时页面自身可能报数据错误，不在本用例范围
        await page.goto('/note-share')
        await expect(page).toHaveURL(/\/note-share/)
    })
})

test.describe('设备分流（desktop 项目）', () => {
    test.beforeEach(skipUnlessProject('desktop'))

    test('D4 桌面访问 /m/todo → 重定向 PC 对应页', async ({ page }) => {
        await loginAs(page)
        await page.goto('/m/todo')
        await expect(page).toHaveURL(/\/px\/life\/todo$/)
    })

    test('D5 桌面访问 /m/mine/setting/family → 精确映射到 PC 家庭空间', async ({ page }) => {
        await loginAs(page)
        await page.goto('/m/mine/setting/family')
        await expect(page).toHaveURL(/\/system\/dataGroup$/)
    })
})
