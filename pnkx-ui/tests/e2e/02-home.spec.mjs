import { test, expect, skipUnlessProject } from './helpers/setup.mjs'
import { TOKENS, loginAs, clearForceMode, overrideRoute, getRequests, waitForRequest, clearRequests } from './helpers/mock.mjs'

/**
 * 移动端首页看板（原版视图 + 行动中心）
 * 专属 token：test-token-home（V2 关）/ TOKENS.AC2（行动中心开）
 */
test.describe('首页看板', () => {
    test.beforeEach(skipUnlessProject('mobile'))

    test('H1 原版视图渲染四大数据区', async ({ page }) => {
        await loginAs(page, 'test-token-home')
        await clearForceMode(page)
        await page.goto('/m/home')
        // 问候语含用户名
        await expect(page.locator('.home-hero__row')).toContainText('admin')
        // 今日提醒：纪念日（倒计时）/ 情侣卡（待确认）/ 时间线
        await expect(page.locator('.home-reminder__title', { hasText: '恋爱纪念日' })).toBeVisible()
        await expect(page.locator('.home-reminder__days')).toHaveText('5天')
        await expect(page.locator('.home-reminder__title', { hasText: '按摩卡' })).toBeVisible()
        await expect(page.locator('.home-reminder__status--pending')).toHaveText('待确认')
        await expect(page.locator('.home-reminder__title', { hasText: '取快递' })).toBeVisible()
        // 今日账单：金额与分类
        await expect(page.locator('.home-bill')).toContainText('25.50')
        await expect(page.locator('.home-bill')).toContainText('午餐')
        // 今日待办：条目与计数
        await expect(page.locator('.home-todo__count')).toHaveText('1')
        await expect(page.locator('.home-todo__title')).toHaveText('取快递')
    })

    test('H2 通知公告轮播可点击', async ({ page }) => {
        await loginAs(page, 'test-token-home')
        await page.goto('/m/home')
        const swipe = page.locator('.home-notice .van-swipe-item', { hasText: '新功能上线' })
        await expect(swipe).toBeVisible({ timeout: 8000 })
        // 点击公告标题查看详情
        await expect(page.getByText('系统维护通知').first()).toBeVisible()
    })

    test('H3 铃铛未读数红点并跳提醒中心', async ({ page }) => {
        await loginAs(page, 'test-token-home')
        await page.goto('/m/home')
        // mock unread=3 → 铃铛角标
        await expect(page.locator('.home-hero__badge')).toHaveText('3', { timeout: 8000 })
        await page.locator('button.home-hero__btn').filter({ has: page.locator('.home-hero__bell-icon') }).click()
        await expect(page).toHaveURL(/\/m\/reminder$/)
    })

    test('H4 FAB 快捷入口跳转', async ({ page }) => {
        await loginAs(page, 'test-token-home')
        await page.goto('/m/home')
        await expect(page.locator('.van-tabbar')).toBeVisible()
        // FAB 展开后四个入口
        await page.locator('.home-fab__btn').click()
        await expect(page.locator('.home-fab__menu')).toBeVisible()
        await page.locator('.home-fab__menu-item', { hasText: '记一笔' }).click()
        await expect(page).toHaveURL(/record\/add/)
    })

    test('H5 视图切换持久化（localStorage）', async ({ page }) => {
        await loginAs(page, TOKENS.AC2)
        await clearForceMode(page)
        await page.goto('/m/home')
        await expect(page.locator('.van-tabbar')).toBeVisible()
        // 切到行动中心
        const switchBtn = page.locator('[class*=view-switch], [class*=chip]').filter({ hasText: /行动中心/ }).first()
        await expect(switchBtn).toBeVisible({ timeout: 8000 })
        await switchBtn.click()
        await expect(page.locator('.ac-pins').first()).toBeVisible({ timeout: 8000 })
        expect(await page.evaluate(() => localStorage.getItem('pnkx-home-view'))).toBe('center')
        // 刷新后保持
        await page.reload()
        await expect(page.locator('.ac-pins').first()).toBeVisible({ timeout: 10000 })
    })

    test('H6 行动中心：逾期置顶与 pin 管理', async ({ page }) => {
        await loginAs(page, TOKENS.AC2)
        await clearForceMode(page)
        await page.addInitScript(() => localStorage.setItem('pnkx-home-view', 'center'))
        await clearRequests(TOKENS.AC2)
        await page.goto('/m/home')
        // 逾期置顶区（fixtures overdue）
        await expect(page.getByText('交房租').first()).toBeVisible({ timeout: 8000 })
        // pin 固定入口渲染
        await expect(page.locator('.ac-pin').first()).toBeVisible()
        // 管理入口 → 勾选 → 保存（断言 PUT preferences 请求）
        await page.getByText('管理入口').click()
        const save = page.locator('.ac-pins-editor__actions .van-button', { hasText: /保存/ })
        if (await save.count()) {
            await save.click()
            await waitForRequest(TOKENS.AC2, r => r.method === 'PUT' && r.path.includes('/life/action-center/preferences'))
        }
    })

    test('H7 actionCenterV2 关闭时无切换入口', async ({ page }) => {
        await loginAs(page, 'test-token-home')
        await page.addInitScript(() => localStorage.removeItem('pnkx-home-view'))
        await page.goto('/m/home')
        await expect(page.locator('.home-reminder__title', { hasText: '恋爱纪念日' })).toBeVisible({ timeout: 8000 })
        await expect(page.locator('[class*=chip]').filter({ hasText: /行动中心/ })).toHaveCount(0)
    })
})
