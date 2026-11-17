import { test, expect, skipUnlessProject } from './helpers/setup.mjs'
import { loginAs, clearForceMode } from './helpers/mock.mjs'

/**
 * L4 视觉回归：移动端主页面 × 明/暗 基线快照
 *
 * - 基线更新：`npm run test:visual -- --update-snapshots`（需人工 review diff 后提交）
 * - 首版基线：light 全量 + dark 代表页 8 张
 */

const LIGHT_PAGES = [
    ['/m/home', '首页'],
    ['/m/function', '生活助手'],
    ['/m/mine', '我的'],
    ['/m/mine/setting', '应用设置'],
    ['/m/bookkeeping', '生活账本'],
    ['/m/bookkeeping/record/add', '记一笔'],
    ['/m/bookkeeping/record', '账目清单'],
    ['/m/bookkeeping/account', '账户管理'],
    ['/m/bookkeeping/statistics', '账目统计'],
    ['/m/bookkeeping/budget', '预算管理'],
    ['/m/bookkeeping/recurring', '周期记账'],
    ['/m/todo', '待办事项'],
    ['/m/diary', '我的日记'],
    ['/m/diary/analysis', '心情分析'],
    ['/m/note', '我的笔记'],
    ['/m/note/obsidian', 'Obsidian'],
    ['/m/book', '书城'],
    ['/m/web-article', '网址收藏'],
    ['/m/shopping-list', '购物清单'],
    ['/m/recipe', '菜谱库'],
    ['/m/meal-plan', '膳食计划'],
    ['/m/ai/chat', 'AI助手'],
    ['/m/reminder', '提醒中心'],
    ['/m/calendar', '家庭日历'],
    ['/m/report', '生活报告'],
    ['/m/search', '全局搜索'],
    ['/m/menstruation', '姨妈助手'],
    ['/m/commemoration-day', '纪念日'],
    ['/m/card', '情侣卡券'],
    ['/m/automation', '生活自动化'],
    ['/m/life-manager', '生活管家']
]

const DARK_PAGES = [
    ['/m/home', '首页'],
    ['/m/bookkeeping/record/add', '记一笔'],
    ['/m/todo', '待办事项'],
    ['/m/diary', '我的日记'],
    ['/m/ai/chat', 'AI助手'],
    ['/m/calendar', '家庭日历'],
    ['/m/mine', '我的'],
    ['/m/search', '全局搜索']
]

test.beforeEach(skipUnlessProject('mobile'))

for (const [path, name] of LIGHT_PAGES) {
    test(`visual[light] ${name} ${path}`, async ({ page }) => {
        await loginAs(page, 'test-token-home')
        await clearForceMode(page)
        await page.emulateMedia({ reducedMotion: 'reduce' })
        await page.goto(path)
        await expect(page.locator('.m-app')).toBeVisible({ timeout: 12000 })
        await page.waitForTimeout(500)
        await expect(page).toHaveScreenshot(`light${path.replace(/\//g, '_')}.png`, {
            fullPage: false,
            animations: 'disabled',
            maxDiffPixelRatio: 0.02
        })
    })
}

for (const [path, name] of DARK_PAGES) {
    test(`visual[dark] ${name} ${path}`, async ({ page }) => {
        await loginAs(page, 'test-token-home')
        await clearForceMode(page)
        await page.addInitScript(() => localStorage.setItem('App-Theme', 'dark'))
        await page.emulateMedia({ reducedMotion: 'reduce' })
        await page.goto(path)
        await expect(page.locator('.m-app')).toBeVisible({ timeout: 12000 })
        await expect(page.locator('.m-app')).toHaveCSS('background-color', /rgb/, { timeout: 4000 }).catch(() => { })
        await page.waitForTimeout(500)
        await expect(page).toHaveScreenshot(`dark${path.replace(/\//g, '_')}.png`, {
            fullPage: false,
            animations: 'disabled',
            maxDiffPixelRatio: 0.02
        })
    })
}
