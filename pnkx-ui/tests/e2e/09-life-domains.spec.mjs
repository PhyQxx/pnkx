import { test, expect, skipUnlessProject } from './helpers/setup.mjs'
import { TOKENS, loginAs, clearForceMode, overrideRoute, clearOverride, waitForRequest, getRequests, clearRequests } from './helpers/mock.mjs'

/**
 * 生活域：姨妈助手 / 纪念日 / 情侣卡券 / 自动化 / 生活管家(菜谱+膳食+订阅)
 * 专属 token：test-token-ld（V2 全关）；M10 需同时开 shoppingPlanV2 + chargeInstanceV2 → TOKENS.ALL
 */
const LD = 'test-token-ld'

const pad = n => String(n).padStart(2, '0')
const dateStr = d => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
const TODAY = new Date()
const D = offset => {
    const d = new Date(TODAY)
    d.setDate(d.getDate() + offset)
    return dateStr(d)
}

// 同 token 的请求日志跨用例持久，先清空避免 waitForRequest 命中上一条用例的请求
test.beforeEach(async () => {
    await clearRequests(LD)
    await clearRequests(TOKENS.ALL)
})

test.describe('姨妈助手', () => {
    test.beforeEach(skipUnlessProject('mobile'))

    test.afterEach(async () => {
        await clearOverride(LD, 'GET', '/myTool/menstruationRecord/list')
        await clearOverride(LD, 'GET', '/myTool/menstruationRecord/getPxMenstruationRecordList')
    })

    test('M1 状态 hero + 日历/列表双视图', async ({ page }) => {
        await loginAs(page, LD)
        await clearForceMode(page)
        const rows = [
            { id: 201, date: D(0), type: '0', makeLove: false },
            { id: 202, date: D(0), type: null, temperature: 36.5, weight: 52.5, items: '排卵试纸', results: '阴性', remark: '轻微腹痛' }
        ]
        await overrideRoute(LD, 'GET', '/myTool/menstruationRecord/list', { rows, total: 2 })
        await overrideRoute(LD, 'GET', '/myTool/menstruationRecord/getPxMenstruationRecordList', { rows, total: 2 })
        await page.goto('/m/menstruation')
        // hero：状态来自字典 + 预测来自最近开始日期
        await expect(page.locator('.menstruation-hero__status')).toHaveText('安全期', { timeout: 8000 })
        await expect(page.locator('.menstruation-hero__title')).toHaveText('今天是平稳期')
        await expect(page.locator('.menstruation-hero__summary-value')).toHaveText('28 天')
        // 日历视图：开始标记 + 当日记录聚合
        await expect(page.locator('.van-calendar')).toBeVisible()
        await expect(page.locator('.van-calendar__bottom-info').first()).toContainText('开始')
        await expect(page.locator('.section-header__subtitle')).toHaveText('2 条记录')
        await expect(page.locator('.record-card')).toHaveCount(2)
        await expect(page.locator('.record-card__tag', { hasText: '经期开始' })).toBeVisible()
        await expect(page.locator('.record-card__tag', { hasText: '检查' })).toBeVisible()
        await expect(page.locator('.record-card__metric-value').first()).toHaveText('36.5℃')
        // 列表视图
        await page.locator('.view-switch__item', { hasText: '列表' }).click()
        await expect(page.locator('.record-card')).toHaveCount(2, { timeout: 8000 })
        await expect(page.locator('.record-card__day').first()).toBeVisible()
        // 切回日历
        await page.locator('.view-switch__item', { hasText: '日历' }).click()
        await expect(page.locator('.van-calendar')).toBeVisible()
    })

    test('M2 经期记录新增（互斥开关）/ 编辑回填 / 删除', async ({ page }) => {
        await loginAs(page, LD)
        await clearForceMode(page)
        // 新增：从主页 FAB 进入（保证 router.back 有落点）
        await page.goto('/m/menstruation')
        await page.locator('.m-fab').click()
        await expect(page).toHaveURL(/\/m\/menstruation\/record/)
        await expect(page.locator('.record-hero__eyebrow')).toHaveText('新增记录', { timeout: 8000 })
        const comeSwitch = page.locator('.toggle-card', { hasText: '姨妈来了' }).locator('.van-switch')
        const goSwitch = page.locator('.toggle-card', { hasText: '姨妈走了' }).locator('.van-switch')
        await comeSwitch.click()
        await expect(comeSwitch).toHaveClass(/van-switch--on/)
        await goSwitch.click()
        await expect(goSwitch).toHaveClass(/van-switch--on/)
        await expect(comeSwitch).not.toHaveClass(/van-switch--on/)
        await comeSwitch.click()
        await expect(comeSwitch).toHaveClass(/van-switch--on/)
        await page.getByPlaceholder('℃').fill('36.8')
        await page.getByPlaceholder('kg').fill('53.2')
        await page.locator('.m-sticky-actions .van-button', { hasText: '保存记录' }).click()
        const post = await waitForRequest(LD, r => r.method === 'POST' && r.path === '/myTool/menstruationRecord')
        expect(post.body).toMatchObject({ date: D(0), type: '0', temperature: 36.8, weight: 53.2, makeLove: false })
        await expect(page.locator('.van-toast')).toContainText('添加成功')
        await page.waitForURL(/\/m\/menstruation$/, { timeout: 5000 })
        // 编辑回填
        await page.goto('/m/menstruation/record?id=202')
        await expect(page.locator('.record-hero__eyebrow')).toHaveText('编辑记录', { timeout: 8000 })
        await expect(page.getByPlaceholder('℃')).toHaveValue('36.5')
        await expect(page.getByPlaceholder('例如：排卵试纸、B超、激素检查')).toHaveValue('排卵试纸')
        await page.getByPlaceholder('℃').fill('37.1')
        await page.locator('.m-sticky-actions .van-button', { hasText: '保存记录' }).click()
        const put = await waitForRequest(LD, r => r.method === 'PUT' && r.path === '/myTool/menstruationRecord')
        expect(put.body).toMatchObject({ id: '202', temperature: 37.1 })
        await page.waitForURL(/\/m\/menstruation$/, { timeout: 5000 })
        // 删除（删除按钮位于底部固定保存栏下方：先滚动可见再点击）
        await page.goto('/m/menstruation/record?id=202')
        const delBtn = page.locator('.delete-section .van-button', { hasText: '删除记录' })
        await delBtn.scrollIntoViewIfNeeded()
        await delBtn.click()
        await page.locator('.van-dialog__confirm').click()
        await waitForRequest(LD, r => r.method === 'DELETE' && r.path === '/myTool/menstruationRecord/202')
        await page.waitForURL(/\/m\/menstruation$/, { timeout: 5000 })
    })

    test('M3 设置周期 stepper 边界 + 提醒入口', async ({ page }) => {
        await loginAs(page, LD)
        await clearForceMode(page)
        await page.goto('/m/menstruation/settings')
        const cycleCard = page.locator('.setting-card', { hasText: '经期周期' })
        await expect(cycleCard).toBeVisible({ timeout: 8000 })
        await expect(cycleCard.locator('.van-stepper input')).toHaveValue('28')
        // 减到下边界 21
        for (let i = 0; i < 7; i++) await cycleCard.locator('.van-stepper__minus').click()
        await expect(cycleCard.locator('.van-stepper input')).toHaveValue('21')
        await expect(cycleCard.locator('.van-stepper__minus')).toHaveClass(/van-stepper__minus--disabled/)
        // 加一步 → 22，保存写入 localStorage
        await cycleCard.locator('.van-stepper__plus').click()
        await expect(cycleCard.locator('.van-stepper input')).toHaveValue('22')
        // 下次经期提醒入口（依赖最近开始日期）
        await expect(page.locator('.van-cell', { hasText: '下次经期提醒' })).toBeVisible()
        await page.locator('.m-sticky-actions .van-button', { hasText: '保存设置' }).click()
        await expect(page.locator('.van-toast')).toContainText('保存成功')
        expect(await page.evaluate(() => localStorage.getItem('menstruation_cycle'))).toBe('22')
    })

    test('M4 统计均值卡 + 周期趋势', async ({ page }) => {
        await loginAs(page, LD)
        await clearForceMode(page)
        await overrideRoute(LD, 'GET', '/myTool/menstruationRecord/getPxMenstruationRecordList', {
            rows: [
                { id: 201, date: D(0), type: '0' },
                { id: 200, date: D(-27), type: '0' },
                { id: 199, date: D(-56), type: '0' },
                { id: 198, date: D(-52), type: '1' }
            ],
            total: 4
        })
        await page.goto('/m/menstruation/statistics')
        await expect(page.locator('.stats-hero__title')).toHaveText('28 天', { timeout: 8000 })
        const cards = page.locator('.stat-card__value')
        await expect(cards.nth(0)).toHaveText('28')
        await expect(cards.nth(1)).toHaveText('5')
        await expect(cards.nth(2)).toHaveText('4')
        await expect(cards.nth(3)).toHaveText('3')
        // 趋势：27 天 / 29 天 两条
        const trend = page.locator('.trend-item')
        await expect(trend).toHaveCount(2)
        await expect(trend.first()).toContainText('27 天')
        await expect(trend.nth(1)).toContainText('29 天')
        await expect(page.locator('.record-item')).toHaveCount(4)
    })
})

test.describe('纪念日', () => {
    test.beforeEach(skipUnlessProject('mobile'))

    test.afterEach(async () => {
        await clearOverride(LD, 'GET', '/commemorationDay/list')
        await clearOverride(LD, 'GET', '/life/action-center')
    })

    test('M5 列表倒计时 + 新建（重复每年/图标选择）+ 编辑态提醒与准备待办', async ({ page }) => {
        await loginAs(page, LD)
        await clearForceMode(page)
        await overrideRoute(LD, 'GET', '/commemorationDay/list', {
            rows: [
                { id: 801, name: '恋爱纪念日', date: D(5) + ' 00:00:00', repeat: true, icon: 'x-aiqing', remark: '在一起的日子' },
                { id: 802, name: '搬家纪念日', date: '2026-10-01 00:00:00', repeat: false, icon: 'x-fangzi', remark: '' }
            ],
            total: 2
        })
        await page.goto('/m/commemoration-day')
        // 列表倒计时
        await expect(page.locator('.commemoration-card')).toHaveCount(2, { timeout: 8000 })
        const card = page.locator('.commemoration-card', { hasText: '恋爱纪念日' })
        await expect(card.locator('.count-number')).toHaveText('5')
        await expect(card.locator('.count-label')).toHaveText('还有')
        // 新建：重复每年 + 图标选择
        await page.locator('.m-fab').click()
        await expect(page).toHaveURL(/\/m\/commemoration-day\/add$/)
        await page.getByPlaceholder('请输入纪念日名称').fill('周年纪念')
        await page.locator('.van-cell', { hasText: '重复提醒' }).click()
        await page.locator('.repeat-picker__option', { hasText: '每年重复' }).click()
        await page.locator('.repeat-picker__confirm').click()
        await expect(page.locator('.van-cell', { hasText: '重复提醒' })).toContainText('每年重复')
        await page.locator('.van-cell', { hasText: '选择图标' }).click()
        await expect(page.locator('.icon-picker')).toBeVisible()
        await page.locator('.icon-item').first().click()
        await page.locator('.picker-confirm').click()
        await page.locator('.m-sticky-actions .van-button', { hasText: '保存' }).click()
        const post = await waitForRequest(LD, r => r.method === 'POST' && r.path === '/commemorationDay')
        expect(post.body).toMatchObject({ name: '周年纪念', repeat: true, date: D(0) + ' 00:00:00' })
        await expect(page.locator('.van-toast')).toContainText('保存成功')
        // 保存成功后 800ms 会 router.back() 回列表，等它完成再进编辑页
        await page.waitForURL(/\/m\/commemoration-day$/, { timeout: 5000 })
        // 编辑态：到期提醒 + 准备待办面板
        await overrideRoute(LD, 'GET', '/life/action-center', {
            data: {
                todos: [],
                commemorations: [
                    { id: 801, name: '恋爱纪念日', occurrenceDate: D(5), daysUntil: 5, preparationStatus: 'draft', suggestedDueDate: D(2) }
                ],
                canEdit: true
            }
        })
        await page.goto('/m/commemoration-day/add?id=801')
        await expect(page.getByPlaceholder('请输入纪念日名称')).toHaveValue('恋爱纪念日', { timeout: 8000 })
        // 准备待办面板
        const prep = page.locator('.preparation-section')
        await expect(prep).toBeVisible()
        await expect(prep.locator('.cp-item__title')).toHaveText('恋爱纪念日')
        await expect(prep.locator('.cp-tag')).toHaveText('待确认')
        await expect(prep.getByRole('button', { name: '创建待办' })).toBeVisible()
        // 到期提醒：选择提前 1 天并保存
        await page.locator('.van-cell', { hasText: '到期提醒' }).click()
        const reminder = page.locator('.m-reminder')
        await expect(reminder).toBeVisible()
        await reminder.locator('.van-cell', { hasText: '提前提醒' }).click()
        await page.locator('.van-picker li', { hasText: '提前 1 天' }).click()
        await page.locator('.van-picker').getByRole('button', { name: '确认' }).click()
        await expect(reminder.locator('.van-cell', { hasText: '提前提醒' })).toContainText('提前 1 天')
        await reminder.getByRole('button', { name: '保存提醒' }).click()
        const bind = await waitForRequest(LD, r => r.method === 'POST' && r.path === '/reminder/bind')
        expect(bind.body).toMatchObject({ sourceType: 'commemoration', sourceId: 801, leadMinutes: 1440 })
    })
})

test.describe('情侣卡券', () => {
    test.beforeEach(skipUnlessProject('mobile'))

    test('M6 持有张数 + 使用弹窗（张数-1）+ 待确认/待评分流转', async ({ page }) => {
        await loginAs(page, LD)
        await clearForceMode(page)
        await page.goto('/m/card')
        // 持有张数
        const item = page.locator('.card-item', { hasText: '按摩卡' })
        await expect(item).toBeVisible({ timeout: 8000 })
        await expect(item.locator('.card-item__count')).toHaveText('剩余 3 张')
        // 使用弹窗：填写说明 → 确认（期间改写持有数 → 刷新后张数-1）
        await item.locator('.card-item__use-btn').click()
        const popup = page.locator('.use-popup')
        await expect(popup).toBeVisible()
        await expect(popup.locator('.use-popup__hint')).toContainText('按摩卡')
        await popup.locator('textarea').fill('轻一点力度')
        await overrideRoute(LD, 'GET', '/px/card/getCardByUserId', {
            data: [{ cardId: 1, title: '按摩卡', describe: '一次全身按摩', cardNumber: 2 }]
        })
        await popup.getByRole('button', { name: '确定' }).click()
        const use = await waitForRequest(LD, r => r.method === 'POST' && r.path === '/px/card/useCard')
        expect(use.body).toMatchObject({ cardId: 1, instructions: '轻一点力度' })
        await expect(item.locator('.card-item__count')).toHaveText('剩余 2 张', { timeout: 8000 })
        // 待确认 → 确认使用
        const todo1 = page.locator('.todo-item', { hasText: '按摩卡' })
        await expect(todo1.locator('.todo-item__status')).toHaveText('待确认')
        await todo1.getByRole('button', { name: '确认使用' }).click()
        await page.locator('.van-dialog__confirm').click()
        await waitForRequest(LD, r => r.method === 'POST' && r.path === '/px/card/confirmCard')
        await expect(page.locator('.van-toast')).toContainText('确认成功')
        // 待评分 → 提交评分
        const todo2 = page.locator('.todo-item', { hasText: '电影卡' })
        await expect(todo2.locator('.todo-item__status')).toHaveText('待评分')
        await todo2.getByRole('button', { name: '去评分' }).click()
        const scorePopup = page.locator('.score-popup')
        await expect(scorePopup).toBeVisible()
        await scorePopup.locator('.van-rate__icon').nth(4).click()
        await scorePopup.locator('textarea').fill('服务很棒')
        await scorePopup.getByRole('button', { name: '提交评分' }).click()
        const score = await waitForRequest(LD, r => r.method === 'POST' && r.path === '/px/card/scoreCard')
        expect(score.body).toMatchObject({ id: 12, score: 5, remark: '服务很棒' })
        await clearOverride(LD, 'GET', '/px/card/getCardByUserId')
    })

    test('M7 使用记录分页 + 确认/评分操作', async ({ page }) => {
        await loginAs(page, LD)
        await clearForceMode(page)
        await page.goto('/m/card/history')
        await expect(page.locator('.history-item')).toHaveCount(10, { timeout: 8000 })
        // 滚动触发第二页（覆写为第 2 页数据）
        const page2 = {
            rows: [
                { id: 311, cardName: '按摩卡', userName: '另一半', userId: 2, instructions: '记录11', createTime: '2026-11-09 10:00:00', confirm: false, score: 0 },
                { id: 312, cardName: '按摩卡', userName: '我', userId: 1, instructions: '记录12', createTime: '2026-11-09 12:00:00', confirm: true, confirmTime: '2026-11-09 14:00:00', score: 0 },
                { id: 313, cardName: '按摩卡', userName: '另一半', userId: 2, instructions: '记录13', createTime: '2026-11-10 10:00:00', confirm: true, confirmTime: '2026-11-10 12:00:00', score: 3 },
                { id: 314, cardName: '按摩卡', userName: '另一半', userId: 2, instructions: '记录14', createTime: '2026-11-11 10:00:00', confirm: true, confirmTime: '2026-11-11 12:00:00', score: 4 },
                { id: 315, cardName: '按摩卡', userName: '另一半', userId: 2, instructions: '记录15', createTime: '2026-11-12 10:00:00', confirm: true, confirmTime: '2026-11-12 12:00:00', score: 5 }
            ],
            total: 15
        }
        await overrideRoute(LD, 'GET', '/px/card/listRecord', page2)
        await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))
        await waitForRequest(LD, r => r.method === 'GET' && r.path === '/px/card/listRecord' && r.query.pageNum === '2')
        await expect(page.locator('.history-item')).toHaveCount(15, { timeout: 8000 })
        await expect(page.getByText('没有更多了')).toBeVisible()
        // 后续确认/评分会触发整页刷新（pageNum=1），把覆写改为自洽的第 2 页数据避免重复追加
        await overrideRoute(LD, 'GET', '/px/card/listRecord', { ...page2, total: 5 })
        // 第 2 页：确认使用
        const row311 = page.locator('.history-item', { hasText: '记录11' }).first()
        await row311.scrollIntoViewIfNeeded()
        await row311.getByRole('button', { name: '确认使用' }).click()
        await page.locator('.van-dialog__confirm').click()
        await waitForRequest(LD, r => r.method === 'POST' && r.path === '/px/card/confirmCard')
        // 第 2 页：评分
        const row312 = page.locator('.history-item', { hasText: '记录12' }).first()
        await row312.scrollIntoViewIfNeeded()
        await row312.getByRole('button', { name: '去评分' }).click()
        const scorePopup = page.locator('.score-popup')
        await expect(scorePopup).toBeVisible()
        await scorePopup.locator('.van-rate__icon').nth(3).click()
        await scorePopup.getByRole('button', { name: '提交评分' }).click()
        const score = await waitForRequest(LD, r => r.method === 'POST' && r.path === '/px/card/scoreCard')
        expect(score.body).toMatchObject({ id: 312, score: 4 })
        await clearOverride(LD, 'GET', '/px/card/listRecord')
    })
})

test.describe('生活自动化', () => {
    test.beforeEach(skipUnlessProject('mobile'))

    test('M8 模板横滑 + 规则启停 + 试运行 + 执行记录 + JSON 校验拦截', async ({ page }) => {
        await loginAs(page, LD)
        await clearForceMode(page)
        await page.goto('/m/automation')
        // 模板市场横滑
        await expect(page.locator('.template')).toHaveCount(3, { timeout: 8000 })
        const scroll = page.locator('.templates-scroll')
        await scroll.evaluate(el => { el.scrollLeft = 200 })
        expect(await scroll.evaluate(el => el.scrollLeft)).toBeGreaterThan(50)
        // 模板点击 → 编辑器带出名称与模板字段
        await page.locator('.template', { hasText: '纪念日提前准备' }).click()
        const editor = page.locator('.editor')
        await expect(editor.locator('.editor__title')).toHaveText('新建规则')
        await expect(editor.locator('input').first()).toHaveValue('纪念日提前准备')
        // 编辑器 JSON 校验拦截
        const conditionBlock = editor.locator('.json-block', { hasText: '条件 JSON' })
        await conditionBlock.locator('textarea').fill('{invalid')
        await conditionBlock.getByRole('button', { name: '校验' }).click()
        await expect(page.locator('.van-toast')).toContainText('JSON 格式错误')
        const postsBefore = (await getRequests(LD, { path: '/automation/rules' })).filter(r => r.method === 'POST').length
        await editor.getByRole('button', { name: '保存规则' }).click()
        await expect(page.locator('.van-toast')).toContainText('JSON 配置格式错误')
        const postsAfter = (await getRequests(LD, { path: '/automation/rules' })).filter(r => r.method === 'POST').length
        expect(postsAfter).toBe(postsBefore)
        // 修正后保存成功（成功后弹层自动关闭）
        await conditionBlock.locator('textarea').fill('{"daysBefore":3}')
        await editor.getByRole('button', { name: '保存规则' }).click()
        const post = await waitForRequest(LD, r => r.method === 'POST' && r.path === '/automation/rules')
        expect(post.body).toMatchObject({ name: '纪念日提前准备', templateCode: 'anniv_prep', triggerType: 'subscription_due', conditionJson: '{"daysBefore":3}' })
        await expect(editor).toBeHidden({ timeout: 5000 })
        // 规则启停
        const rule2 = page.locator('.rule-card', { hasText: '周报转日记' })
        await expect(rule2.locator('.van-switch')).not.toHaveClass(/van-switch--on/)
        await rule2.locator('.van-switch').click()
        const putEnable = await waitForRequest(LD, r => r.method === 'PUT' && r.path === '/automation/rules')
        expect(putEnable.body).toMatchObject({ id: 'r2', enabled: true })
        // 试运行 dry-run
        const rule1 = page.locator('.rule-card', { hasText: '纪念日提前准备' }).first()
        await rule1.getByRole('button', { name: '试运行' }).click()
        await waitForRequest(LD, r => r.method === 'POST' && r.path === '/automation/rules/r1/dry-run')
        await expect(page.locator('.van-toast')).toContainText('试运行完成')
        // 执行记录 + 失败重放
        await rule1.getByRole('button', { name: '记录' }).click()
        await waitForRequest(LD, r => r.method === 'GET' && r.path === '/automation/executions' && r.query.ruleId === 'r1')
        const history = page.locator('.history')
        await expect(history.locator('.execution')).toHaveCount(2, { timeout: 8000 })
        await expect(history.locator('.execution__status', { hasText: '成功' })).toBeVisible()
        await expect(history.locator('.execution__error')).toContainText('接口超时')
        await history.locator('.execution__retry').click()
        await waitForRequest(LD, r => r.method === 'POST' && r.path === '/automation/executions/e2/retry')
        await expect(page.locator('.van-toast')).toContainText('已重放')
    })
})

test.describe('菜谱与膳食', () => {
    test.beforeEach(skipUnlessProject('mobile'))

    test.afterEach(async () => {
        await clearOverride(LD, 'GET', '/mealPlan/week')
    })

    test('M9 菜谱库/详情（食材/做法/加入膳食计划）+ 膳食周视图增删菜品', async ({ page }) => {
        await loginAs(page, LD)
        await clearForceMode(page)
        await overrideRoute(LD, 'GET', '/mealPlan/week', {
            data: [{ id: 501, planDate: D(0), mealType: 2, title: '番茄炒蛋', servings: 2 }]
        })
        // 菜谱库
        await page.goto('/m/recipe')
        await expect(page.locator('.recipe-card')).toHaveCount(2, { timeout: 8000 })
        await expect(page.locator('.recipe-card__title', { hasText: '番茄炒蛋' })).toBeVisible()
        // 详情：食材 + 做法
        await page.locator('.recipe-card', { hasText: '番茄炒蛋' }).click()
        await expect(page).toHaveURL(/\/m\/recipe\/detail\?id=401/)
        await expect(page.locator('.recipe-detail__ingredient')).toHaveCount(2, { timeout: 8000 })
        await expect(page.locator('.recipe-detail__ingredient', { hasText: '番茄' })).toContainText('2个')
        await expect(page.locator('.recipe-detail__notes')).toContainText('热油先炒蛋')
        // 加入膳食计划
        await page.getByRole('button', { name: '加入膳食计划' }).click()
        await expect(page).toHaveURL(/\/m\/meal-plan\?recipeId=401/)
        // 周视图：7 天 + 今日午餐已有菜品
        await expect(page.locator('.meal-plan__day')).toHaveCount(7, { timeout: 8000 })
        const today = page.locator('.meal-plan__day--today')
        await expect(today).toBeVisible()
        await expect(today.locator('.meal-plan__dish', { hasText: '番茄炒蛋' })).toBeVisible()
        // 新增菜品（菜谱带出标题预填）
        await today.locator('.meal-plan__slot', { hasText: '早餐' }).click()
        const popup = page.locator('.meal-plan__popup')
        await expect(popup).toBeVisible()
        await expect(popup.locator('input').first()).toHaveValue('番茄炒蛋')
        await page.getByPlaceholder('用餐份数').fill('2')
        await popup.getByRole('button', { name: '添加' }).click()
        const add = await waitForRequest(LD, r => r.method === 'POST' && r.path === '/mealPlan')
        expect(add.body).toMatchObject({ planDate: D(0), mealType: 1, recipeId: '401', title: '番茄炒蛋', servings: 2 })
        // 移除菜品
        await today.locator('.meal-plan__slot', { hasText: '午餐' }).locator('.meal-plan__dish', { hasText: '番茄炒蛋' }).click()
        await page.locator('.van-dialog__confirm').click()
        await waitForRequest(LD, r => r.method === 'DELETE' && r.path === '/mealPlan/501')
    })

    test('M10 膳食生成购物清单预览/提交 + 订阅预测/启停/编辑 + 订阅账期面板', async ({ page }) => {
        const ALL = TOKENS.ALL
        await loginAs(page, ALL)
        await clearForceMode(page)
        await overrideRoute(ALL, 'GET', '/mealPlan/week', {
            data: [{ id: 501, planDate: D(0), mealType: 2, title: '番茄炒蛋', servings: 2 }]
        })
        await page.goto('/m/meal-plan')
        // shoppingPlanV2：生成本周购物清单
        await page.getByRole('button', { name: /生成本周购物清单/ }).click()
        const transfer = page.locator('.meal-plan__popup', { hasText: '选择购物清单' })
        await expect(transfer).toBeVisible({ timeout: 8000 })
        await transfer.locator('.meal-plan__popup-list-item', { hasText: '本周采购' }).click()
        const preview = await waitForRequest(ALL, r => r.method === 'POST' && r.path === '/mealPlan/shopping-preview')
        expect(preview.body).toMatchObject({ listId: 11 })
        expect(preview.body.startDate).toMatch(/^\d{4}-\d{2}-\d{2}$/)
        // 预览确认弹窗（含人工核对提示）
        await expect(page.locator('.van-dialog__message')).toContainText('蔬菜', { timeout: 8000 })
        await expect(page.locator('.van-dialog__message')).toContainText('人工核对')
        await page.locator('.van-dialog__confirm').click()
        const commit = await waitForRequest(ALL, r => r.method === 'POST' && r.path === '/mealPlan/shopping-commit')
        expect(commit.body).toEqual({ previewId: 'pv-meal-1' })
        await expect(page.locator('.van-toast')).toContainText('已生成购物清单')
        await clearOverride(ALL, 'GET', '/mealPlan/week')

        // 订阅管理：预测卡 + 账期面板（chargeInstanceV2）
        await page.goto('/m/subscription')
        const subPage = page.locator('.subscription-page')
        await expect(subPage.locator('.subscription-page__summary-value').first()).toHaveText('¥33', { timeout: 8000 })
        await expect(subPage.locator('.subscription-page__summary-value--plain')).toHaveText('¥121')
        await expect(subPage.locator('.subscription-page__summary-sub')).toHaveText('2 项订阅')
        await expect(subPage.locator('.subscription-page__item')).toHaveCount(2)
        // 账期面板
        const charges = page.locator('.charges')
        await expect(charges).toBeVisible({ timeout: 8000 })
        await expect(charges.locator('.charges__item')).toHaveCount(2)
        await expect(charges.locator('.van-tag', { hasText: '待确认' })).toBeVisible()
        await expect(charges.locator('.van-tag', { hasText: '扣费失败' })).toBeVisible()
        // 账期详情 + 确认入账
        await charges.locator('.charges__item', { hasText: '视频会员' }).getByRole('button', { name: '详情' }).click()
        const detail = page.locator('.charges__detail')
        await expect(detail).toBeVisible({ timeout: 8000 })
        await expect(detail).toContainText('账期详情')
        await detail.getByRole('button', { name: '收起详情' }).click()
        await charges.locator('.charges__item', { hasText: '视频会员' }).getByRole('button', { name: '确认入账' }).click()
        const editor = page.locator('.charges__editor')
        await expect(editor).toBeVisible({ timeout: 8000 })
        await expect(page.getByPlaceholder('实际金额')).toHaveValue('25')
        await editor.getByRole('button', { name: '确认入账' }).click()
        await waitForRequest(ALL, r => r.method === 'POST' && r.path === '/charge/instances/ci1/confirm')
        await expect(page.locator('.van-toast')).toContainText('已入账')
        // 订阅启停
        const sub1 = subPage.locator('.subscription-page__item', { hasText: '视频会员' })
        await sub1.locator('.subscription-page__item-toggle', { hasText: '停用' }).click()
        const toggle = await waitForRequest(ALL, r => r.method === 'PUT' && r.path === '/subscription')
        expect(toggle.body).toMatchObject({ id: 901, enabled: false })
        // 编辑订阅（金额 25 → 30，三步确认流程）
        await page.waitForTimeout(300)
        const sub1b = page.locator('.subscription-page__item', { hasText: '视频会员' })
        await sub1b.getByRole('button', { name: '编辑' }).click()
        const subEditor = page.locator('.subscription-page__editor')
        await expect(subEditor).toBeVisible({ timeout: 8000 })
        await expect(subEditor).toContainText('编辑订阅')
        await page.getByPlaceholder('0.00').fill('30')
        await subEditor.getByRole('button', { name: '保存' }).click()
        const change = await waitForRequest(ALL, r => r.method === 'POST' && r.path === '/charge/rules/change-preview')
        expect(change.body).toMatchObject({ sourceType: 'subscription', sourceId: 901 })
        expect(change.body.changes.amount).toBe(30)
        // 三步：变化摘要 → 影响账期 → 确认执行
        const flow = page.locator('.flow')
        await expect(flow).toBeVisible({ timeout: 8000 })
        await expect(flow.locator('.flow__diff')).toContainText('金额')
        await expect(flow.locator('.flow__diff-after')).toHaveText('30')
        await flow.getByRole('button', { name: '下一步' }).click()
        await expect(flow.locator('.flow__step--on', { hasText: '影响账期' })).toBeVisible()
        await flow.getByRole('button', { name: '下一步' }).click()
        await flow.getByRole('button', { name: '确认执行' }).click()
        const commitChange = await waitForRequest(ALL, r => r.method === 'POST' && r.path === '/charge/rules/change-commit')
        expect(commitChange.body).toEqual({ previewId: 'pv-sub-1' })
        // 向导 toast「规则已更新」随后被订阅页自身 toast「已保存」覆盖
        await expect(page.locator('.van-toast')).toContainText('已保存')
    })
})
