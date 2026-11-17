import { test, expect, skipUnlessProject } from './helpers/setup.mjs'
import { TOKENS, loginAs, clearForceMode, overrideRoute, clearOverride, getRequests, waitForRequest, clearRequests } from './helpers/mock.mjs'

/**
 * 记账域（B1-B12）
 * 专属 token：test-token-bk（V2 关）；B12 用 TOKENS.CI2（chargeInstanceV2 开）
 *
 * Mock 限制（不区分 query）：
 * - /bookkeeping/record/list 对所有月份/账户参数返回同一份数据（6 条：支出 25.50+88.00+12.00+15.00、收入 5000、转账 200）
 * - /bookkeeping/classification/getClassificationList 对支出/收入 typeDifference 返回同一份（含支出 2 组 + 收入 1 组）
 *   → 支出 Tab 显示 3 个一级分类，用例只依赖「餐饮」
 */
const T = 'test-token-bk'
const CI2 = TOKENS.CI2

// 首页/清单页月汇总：mock 恒定返回同一份流水 → 支出 140.50、收入 5000、结余 4859.50
const EXPENSE_TOTAL = '140.50'
const INCOME_TOTAL = '5000.00'

test.describe('记账域', () => {
    test.beforeEach(skipUnlessProject('mobile'))

    test('B1 生活账本主页：月汇总 + 宫格入口 + 最近流水', async ({ page }) => {
        await loginAs(page, T)
        await clearForceMode(page)
        await page.goto('/m/bookkeeping')
        await expect(page.locator('.bk-header__summary-value').first()).toHaveText(EXPENSE_TOTAL, { timeout: 8000 })
        // 6 个功能入口
        await expect(page.locator('.bk-menu__card')).toHaveCount(6)
        // 宫格必须落在首屏视口内（回归防护：section 误用整屏 .m-page 会把宫格推到折叠线下，页面看似"没数据"）
        await expect(page.locator('.bk-menu__card').first()).toBeInViewport({ ratio: 0.5 })
        // 最近记录最多 5 条，且首条也在首屏内
        await expect(page.locator('.bk-recent__list .record-card').first()).toBeVisible()
        await expect(page.locator('.bk-recent__list .record-card').first()).toBeInViewport()
        const recentCount = await page.locator('.bk-recent__list > *').count()
        expect(recentCount).toBeLessThanOrEqual(5)
        // FAB → 记一笔
        await page.locator('.m-fab').click()
        await expect(page).toHaveURL(/bookkeeping\/record(-add|\/add)/)
    })

    test('B2 记一笔支出：分类宫格 → 二级选择 → 计算器 25.5 → 保存', async ({ page }) => {
        await loginAs(page, T)
        await clearForceMode(page)
        await clearRequests(T)
        await page.goto('/m/bookkeeping')
        await page.locator('.m-fab').click()
        await expect(page).toHaveURL(/bookkeeping\/record(-add|\/add)/)
        // 默认已选第一个一级分类的第一个二级分类（餐饮-午餐），金额为空
        await expect(page.locator('.add-record__category-item').first()).toHaveClass(/selected/, { timeout: 8000 })
        // 点「餐饮」→ 自动弹二级选择器 → 选「晚餐」
        await page.locator('.add-record__category-item', { hasText: '餐饮' }).click()
        const secondaryPopup = page.locator('.add-record__picker', { hasText: '选择分类' })
        await expect(secondaryPopup).toBeVisible()
        await secondaryPopup.locator('.add-record__picker-item', { hasText: '晚餐' }).click()
        await expect(page.locator('.add-record__chip-text', { hasText: '晚餐' })).toBeVisible()
        // 计算器输入 25.5
        for (const key of ['2', '5', '.', '5']) {
            await page.locator('.calc__key', { hasText: new RegExp('^' + key.replace('.', '\\.') + '$') }).click()
        }
        await expect(page.locator('.add-record__amount-process')).toHaveText('25.5')
        // 保存（计算器确定键）
        await page.locator('.calc__key--confirm').click()
        const post = await waitForRequest(T, r => r.method === 'POST' && r.path === '/bookkeeping/record')
        expect(post.body).toMatchObject({ type: 13, account: 71, money: 25.5, typeDifference: '1' })
        await expect(page).toHaveURL(/\/m\/bookkeeping$/, { timeout: 6000 })
    })

    test('B3 记一笔转账：双账户选择与保存', async ({ page }) => {
        await loginAs(page, T)
        await clearForceMode(page)
        await clearRequests(T)
        await page.goto('/m/bookkeeping')
        await page.locator('.m-fab').click()
        await expect(page).toHaveURL(/bookkeeping\/record(-add|\/add)/)
        // 切转账 Tab
        await page.locator('.add-record__tab-item', { hasText: '转账' }).click()
        await expect(page.locator('.add-record__category-grid')).toHaveCount(0)
        // 转出/转入默认为第一组前两个账户（工商银行/招商银行）
        await expect(page.locator('.add-record__chip-text', { hasText: '工商银行' })).toBeVisible()
        await expect(page.locator('.add-record__chip-text', { hasText: '招商银行' })).toBeVisible()
        // 金额 200 → 保存
        for (const key of ['2', '0', '0']) {
            await page.locator('.calc__key', { hasText: new RegExp('^' + key + '$') }).click()
        }
        await page.locator('.calc__key--confirm').click()
        const post = await waitForRequest(T, r => r.method === 'POST' && r.path === '/bookkeeping/record')
        expect(post.body).toMatchObject({ account: 71, otherAccount: 72, money: 200, typeDifference: '2' })
    })

    test('B4 AI 记账：自然语言解析回填', async ({ page }) => {
        await loginAs(page, T)
        await clearForceMode(page)
        await clearRequests(T)
        await page.goto('/m/bookkeeping')
        await page.locator('.m-fab').click()
        await expect(page).toHaveURL(/bookkeeping\/record(-add|\/add)/)
        await expect(page.locator('.add-record__category-item').first()).toHaveClass(/selected/, { timeout: 8000 })
        await page.locator('.add-record__ai-toggle').click()
        await page.locator('input.add-record__ai-input').fill('午餐花了30块')
        await page.locator('.add-record__ai-parse-btn').click()
        await waitForRequest(T, r => r.method === 'POST' && r.path === '/bookkeeping/record/aiParse')
        // 回填金额 30
        await expect(page.locator('.add-record__amount-process')).toHaveText('30', { timeout: 6000 })
    })

    test('B5 金额为空保存被拦截', async ({ page }) => {
        await loginAs(page, T)
        await clearForceMode(page)
        await page.goto('/m/bookkeeping')
        await page.locator('.m-fab').click()
        await expect(page).toHaveURL(/bookkeeping\/record(-add|\/add)/)
        await expect(page.locator('.add-record__category-item').first()).toHaveClass(/selected/, { timeout: 8000 })
        await clearRequests(T)
        // 金额空 → 确定键 → toast 拦截，无保存请求
        await page.locator('.calc__key--confirm').click()
        await expect(page.locator('.van-toast', { hasText: '请输入金额' })).toBeVisible({ timeout: 5000 })
        const posts = (await getRequests(T)).filter(r => r.method === 'POST' && r.path === '/bookkeeping/record')
        expect(posts).toHaveLength(0)
    })

    test('B6 编辑回填与修改保存', async ({ page }) => {
        await loginAs(page, T)
        await clearForceMode(page)
        await clearRequests(T)
        await page.goto('/m/bookkeeping/record')
        const card701 = page.locator('.record-swipe__cell, .van-swipe-item, .record-day', { hasText: '超市采购' }).first()
        await expect(card701).toBeVisible({ timeout: 8000 })
        await card701.locator('..').locator('..').locator('.record-swipe__btn', { hasText: '编辑' }).first().click()
        await expect(page).toHaveURL(/record(-add|\/add)/)
        // 回填：金额 88、分类「午餐」、备注「超市采购」
        await expect(page.locator('.add-record__amount-process')).toHaveText('88.00', { timeout: 8000 })
        await expect(page.locator('.add-record__chip-text', { hasText: '午餐' })).toBeVisible()
        // 修改金额为 66 → 保存 PUT
        await page.locator('.calc__key--clear').click()
        for (const key of ['6', '6']) {
            await page.locator('.calc__key', { hasText: new RegExp('^' + key + '$') }).click()
        }
        await page.locator('.calc__key--confirm').click()
        const put = await waitForRequest(T, r => r.method === 'PUT' && r.path === '/bookkeeping/record')
        expect(put.body).toMatchObject({ id: 701, money: 66 })
    })

    test('B7 账目清单：月份切换与导出', async ({ page }) => {
        await loginAs(page, T)
        await clearForceMode(page)
        await clearRequests(T)
        await page.goto('/m/bookkeeping/record')
        await expect(page.locator('.record-card, .item, .m-card').first()).toBeVisible({ timeout: 8000 })
        // 月份切换触发新请求
        const before = (await getRequests(T, { path: '/bookkeeping/record/list' })).length
        await page.locator('.record-header__arrow').last().click()
        await page.waitForTimeout(400)
        const after = (await getRequests(T, { path: '/bookkeeping/record/list' })).length
        expect(after).toBeGreaterThan(before)
        // 导出
        await page.locator('.record-header__export').click()
        await waitForRequest(T, r => r.path === '/bookkeeping/record/export')
    })

    test('B8 账户管理：总资产与新增账户', async ({ page }) => {
        await loginAs(page, T)
        await clearForceMode(page)
        await clearRequests(T)
        await page.goto('/m/bookkeeping/account')
        // 总资产 = 12500.50 + 3200 + 800 = 16500.5
        await expect(page.locator('.account-total__number')).toContainText('16,500', { timeout: 8000 })
        // 账户卡渲染
        await expect(page.locator('.account-card', { hasText: '工商银行' })).toBeVisible()
        // FAB 新增账户 → 表单 → 图标选择器 → 保存
        await page.locator('.m-fab').click()
        await expect(page).toHaveURL(/account-maintenance/)
        await page.locator('.van-field', { hasText: '账户名称' }).locator('input').fill('零钱通')
        // 账户类型（必填）：picker 选「银行卡」
        await page.locator('.van-field', { hasText: '账户类型' }).click()
        const typePopup = page.locator('.van-popup .van-picker')
        await expect(typePopup).toBeVisible({ timeout: 5000 })
        await typePopup.getByRole('button', { name: '确认' }).click()
        // 图标选择器（van-field 只读点击弹层）
        const iconField = page.locator('.van-field', { hasText: '图标' })
        if (await iconField.count()) {
            await iconField.click()
            const iconPopup = page.locator('.van-popup', { hasText: '选择图标' }).last()
            await expect(iconPopup).toBeVisible({ timeout: 5000 })
            await iconPopup.locator('img').first().click()
            await iconPopup.getByRole('button', { name: /确认|确定/ }).click()
        }
        await page.locator('.m-sticky-actions .van-button', { hasText: '保存' }).click()
        const post = await waitForRequest(T, r => r.method === 'POST' && r.path === '/bookkeeping/account')
        expect(post.body).toMatchObject({ accountName: '零钱通' })
    })

    test('B9 账户详情与维护改名', async ({ page }) => {
        await loginAs(page, T)
        await clearForceMode(page)
        await page.goto('/m/bookkeeping/account/details?id=71')
        await expect(page.getByText('工商银行').first()).toBeVisible({ timeout: 8000 })
        // 维护页改名
        await page.goto('/m/bookkeeping/account/maintenance?id=71')
        const nameInput = page.locator('.van-field', { hasText: '名称' }).locator('input')
        await expect(nameInput).toHaveValue('工商银行', { timeout: 8000 })
        await nameInput.fill('工商银行改')
        await page.locator('.m-sticky-actions .van-button', { hasText: '保存' }).click()
        const put = await waitForRequest(T, r => r.method === 'PUT' && r.path === '/bookkeeping/account')
        expect(put.body).toMatchObject({ id: 71, accountName: '工商银行改' })
    })

    test('B10 统计图表 canvas 渲染', async ({ page }) => {
        await loginAs(page, T)
        await clearForceMode(page)
        await page.goto('/m/bookkeeping/statistics')
        // 趋势图 canvas
        await expect(page.locator('.stat-chart__canvas').first()).toBeVisible({ timeout: 8000 })
        await page.waitForTimeout(800)
        // canvas 有内容（echarts 渲染非空白）
        const hasPaint = await page.evaluate(() => {
            const c = document.querySelector('.stat-chart__canvas canvas')
            if (!c) return false
            const ctx = c.getContext('2d')
            const data = ctx.getImageData(0, 0, c.width, c.height).data
            for (let i = 3; i < data.length; i += 4) if (data[i] !== 0) return true
            return false
        })
        expect(hasPaint).toBe(true)
        // 切分类图
        await page.locator('.van-tab', { hasText: '分类图' }).click()
        await page.waitForTimeout(600)
        await expect(page.locator('.stat-legend__name', { hasText: '餐饮' })).toBeVisible()
        // 无图表相关报错
    })

    test('B11 预算：进度/超支标记/新建', async ({ page }) => {
        await loginAs(page, T)
        await clearForceMode(page)
        await clearRequests(T)
        await page.goto('/m/bookkeeping/budget')
        // 两张预算卡，第二张超支
        await expect(page.locator('.budget-card')).toHaveCount(2, { timeout: 8000 })
        await expect(page.locator('.budget-card', { hasText: '餐饮' })).toHaveClass(/exceeded/)
        await expect(page.locator('.budget-card__tag', { hasText: '已超支' })).toBeVisible()
        // 新建：FAB → 弹层 → 金额 → 保存
        await page.locator('.m-fab').click()
        const amountInput = page.locator('.van-popup .van-field input[type=number], .van-popup .budget-editor input').last()
        await amountInput.fill('800')
        await page.locator('.budget-editor__save', { hasText: '保存' }).click()
        const post = await waitForRequest(T, r => r.method === 'POST' && r.path === '/bookkeeping/budget')
        expect(post.body.amount).toBe(800)
    })

    test('B12【CI2】周期记账：账期面板 + 规则启停 + 三步变更向导', async ({ page }) => {
        await loginAs(page, CI2)
        await clearForceMode(page)
        await clearRequests(CI2)
        await page.goto('/m/bookkeeping/recurring')
        // 账期实例面板渲染
        await expect(page.getByText('房租', { exact: false }).first()).toBeVisible({ timeout: 8000 })
        // 规则卡两条；启停开关
        await expect(page.locator('.rule-card')).toHaveCount(2)
        await page.locator('.rule-card', { hasText: '视频会员' }).locator('.van-switch').click()
        await waitForRequest(CI2, r => r.method === 'PUT' && r.path === '/bookkeeping/recurring/toggle/82')
        // 编辑规则（点卡片）→ 改金额 → 保存 → 三步向导
        await page.locator('.rule-card', { hasText: '房租' }).locator('.rule-card__top').click()
        const moneyInput = page.locator('.van-popup .van-field', { hasText: '金额' }).last().locator('input')
        await moneyInput.fill('3600')
        await page.locator('.editor__save', { hasText: '保存' }).click()
        await waitForRequest(CI2, r => r.method === 'POST' && r.path === '/charge/rules/change-preview')
        // 向导：下一步 → 下一步 → 确认执行
        const next = page.locator('.flow__footer .van-button', { hasText: '下一步' })
        await expect(next).toBeVisible({ timeout: 6000 })
        await next.click()
        await page.locator('.flow__footer .van-button', { hasText: '下一步' }).click()
        await page.locator('.flow__footer .van-button', { hasText: '确认执行' }).click()
        const commit = await waitForRequest(CI2, r => r.method === 'POST' && r.path === '/charge/rules/change-commit')
        expect(commit.body).toMatchObject({ previewId: 'pv1' })
    })
})
