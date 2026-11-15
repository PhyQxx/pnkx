import { test, expect, skipUnlessProject } from './helpers/setup.mjs'
import { loginAs, logoutState } from './helpers/mock.mjs'

/**
 * 认证流（mobile 项目，登录页两端共用）
 */
test.describe('认证', () => {
    test.beforeEach(skipUnlessProject('mobile'))

    test('A1 无 token 访问移动页 → 落登录页', async ({ page }) => {
        await logoutState(page)
        await page.goto('/m/home')
        await expect(page).toHaveURL(/\/login$/)
        await expect(page.getByPlaceholder('请输入用户名')).toBeVisible()
    })

    test('A2 登录表单提交成功 → 进入 APP 首页', async ({ page }) => {
        await logoutState(page)
        await page.goto('/login')
        await page.getByPlaceholder('请输入用户名').fill('admin')
        await page.getByPlaceholder('请输入密码').fill('admin123')
        await page.getByRole('button', { name: /登\s*录/ }).click()
        await expect(page).toHaveURL(/\/m\/home$/, { timeout: 10000 })
        const token = await page.evaluate(() => localStorage.getItem('Admin-Token'))
        expect(token).toBeTruthy()
    })

    test('A3 密码错误 → 错误提示且停留登录页', async ({ page }) => {
        await logoutState(page)
        await page.goto('/login')
        await page.getByPlaceholder('请输入用户名').fill('admin')
        await page.getByPlaceholder('请输入密码').fill('wrong-password')
        await page.getByRole('button', { name: /登\s*录/ }).click()
        // 后端 code=500 → 全局 ElNotification 错误提示
        await expect(page.getByText('用户名或密码错误')).toBeVisible({ timeout: 5000 })
        await expect(page).toHaveURL(/\/login$/)
    })

    test('A4 登录过期（401）→ 清 token 回登录页', async ({ page }) => {
        await loginAs(page, 'expired-token')
        await page.goto('/m/home')
        await expect(page).toHaveURL(/\/login$/, { timeout: 8000 })
        const token = await page.evaluate(() => localStorage.getItem('Admin-Token'))
        expect(token).toBeNull()
    })
})
