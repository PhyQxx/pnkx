import { test, expect, skipUnlessProject } from './helpers/setup.mjs'
import { TOKENS, loginAs, clearForceMode, overrideRoute } from './helpers/mock.mjs'

/**
 * PC 端冒烟回归（desktop 项目）：双端改造后 PC 现状不回归
 */
test.describe('PC 回归', () => {
    test.beforeEach(skipUnlessProject('desktop'))

    test('P1 登录后进入工作台（问候+账单/待办卡区）', async ({ page }) => {
        await loginAs(page, 'test-token-pc')
        await clearForceMode(page)
        await page.goto('/index')
        await expect(page.locator('.sidebar-container').first()).toBeVisible({ timeout: 10000 })
        await expect(page.locator('.home-greeting h1')).toContainText('你好', { timeout: 8000 })
        await expect(page.locator('.card-title', { hasText: '今日账单' })).toBeVisible()
        await expect(page.locator('.card-title', { hasText: '待办事项' })).toBeVisible()
    })

    test('P2 992px 以下侧栏自动收为抽屉，汉堡可展开', async ({ page }) => {
        await loginAs(page, 'test-token-pc')
        await page.goto('/index')
        await expect(page.locator('.sidebar-container').first()).toBeVisible({ timeout: 10000 })
        await page.setViewportSize({ width: 900, height: 800 })
        // ResizeHandler 判定 mobile → hideSidebar 抽屉形态
        await expect(page.locator('.app-wrapper').first()).toHaveClass(/mobile/, { timeout: 6000 })
        await expect(page.locator('.hamburger-container')).toBeVisible()
        await page.locator('.hamburger-container').click()
        await expect(page.locator('.app-wrapper').first()).not.toHaveClass(/hideSidebar/)
    })

    test('P3 后端菜单动态路由：侧栏点开待办表格页', async ({ page }) => {
        await loginAs(page, 'test-token-pc')
        // 按 token 覆写列表数据，避免与其他 fixture 文件的同 key 合并互相干扰
        await overrideRoute('test-token-pc', 'GET', '/admin/toDo/list', {
            rows: [
                { id: 1, content: 'PC 端回归用例待办', state: '1', planStartTime: '2026-11-15 09:00:00', planEndTime: '2026-11-15 18:00:00', label: '回归测试' }
            ],
            total: 1
        })
        await page.goto('/index')
        await expect(page.locator('.sidebar-container').first()).toBeVisible({ timeout: 10000 })
        // 展开生活菜单组，点击待办
        await page.locator('.el-sub-menu__title, .el-submenu__title').filter({ hasText: '生活' }).first().click()
        await page.getByRole('menuitem', { name: '待办' }).click()
        await expect(page).toHaveURL(/\/px\/life\/todo\/?$/)
        // 现代分栏待办页渲染 mock 数据
        await expect(page.getByText('PC 端回归用例待办').first()).toBeVisible({ timeout: 10000 })
    })

    test('P4 TagsView 多标签打开与关闭', async ({ page }) => {
        await loginAs(page, 'test-token-pc')
        await page.goto('/index')
        await expect(page.locator('.tags-view-wrapper')).toBeVisible({ timeout: 10000 })
        await page.locator('.el-sub-menu__title, .el-submenu__title').filter({ hasText: '生活' }).first().click()
        await page.getByRole('menuitem', { name: '日记' }).click()
        await expect(page).toHaveURL(/\/px\/life\/diary\/?$/)
        // 至少两个标签
        const tags = page.locator('.tags-view-item')
        expect(await tags.count()).toBeGreaterThanOrEqual(2)
        // 关闭日记标签
        await tags.filter({ hasText: '日记' }).first().locator('.el-icon-close').click()
        await expect(page.locator('.tags-view-item').filter({ hasText: '日记' })).toHaveCount(0)
    })

    test('P5 快捷 dock 展开并打开功能弹窗', async ({ page }) => {
        await loginAs(page, 'test-token-pc')
        await page.goto('/index')
        await expect(page.locator('.quick-action-dock')).toBeVisible({ timeout: 10000 })
        // hover 展开菜单
        await page.locator('.quick-action-dock').hover()
        await expect(page.locator('.quick-action-menu')).toBeVisible()
        const items = page.locator('.quick-action-item')
        expect(await items.count()).toBeGreaterThanOrEqual(4)
        // 打开文件管理弹窗（dialog 80vw）
        await items.filter({ hasText: '文件管理' }).first().click()
        await expect(page.locator('.el-dialog').filter({ hasText: /文件/ }).first()).toBeVisible({ timeout: 12000 })
    })

    test('P6 明暗主题切换（data-theme 翻转）', async ({ page }) => {
        await loginAs(page, 'test-token-pc')
        await page.goto('/index')
        await expect(page.locator('.home-greeting h1')).toContainText('你好', { timeout: 8000 })
        const before = await page.evaluate(() => document.documentElement.getAttribute('data-theme'))
        // Navbar 主题切换按钮
        await page.locator('.navbar .theme-toggle').click()
        await page.waitForTimeout(400)
        const after = await page.evaluate(() => document.documentElement.getAttribute('data-theme'))
        expect(after).not.toBe(before)
    })
})
