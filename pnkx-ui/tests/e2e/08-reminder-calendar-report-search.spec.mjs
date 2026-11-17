import { test, expect, skipUnlessProject } from './helpers/setup.mjs'
import { TOKENS, loginAs, clearForceMode, overrideRoute, clearOverride, setStream, getRequests, waitForRequest, clearRequests } from './helpers/mock.mjs'

/**
 * 提醒中心 / 提醒偏好 / 家庭日历 / 驾驶舱 / 生活报告 / 报告行动 / 全局搜索 / 通知深链
 * 专属 token：test-token-rc（V2 全关）；R7 报告行动用 TOKENS.RA2
 */
const RC = 'test-token-rc'

// 同 token 的请求日志跨用例持久，先清空避免 waitForRequest 命中上一条用例的请求
test.beforeEach(async () => {
    await clearRequests(RC)
    await clearRequests(TOKENS.RA2)
})

const pad = n => String(n).padStart(2, '0')
const dateStr = d => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
const TODAY = new Date()
const D = offset => {
    const d = new Date(TODAY)
    d.setDate(d.getDate() + offset)
    return dateStr(d)
}

/** 用合成 TouchEvent 横向拖拽（vant SwipeCell 只监听 touch 事件） */
async function touchSwipe(page, locator, dx, dy = 0) {
    const box = await locator.boundingBox()
    const x = box.x + box.width / 2
    const y = box.y + box.height / 2
    await page.evaluate(({ x, y, dx, dy }) => {
        const target = document.elementFromPoint(x, y)
        const make = (type, cx, cy) => {
            const touch = new Touch({ identifier: 1, target, clientX: cx, clientY: cy })
            return new TouchEvent(type, {
                bubbles: true,
                cancelable: true,
                composed: true,
                touches: type === 'touchend' ? [] : [touch],
                targetTouches: [],
                changedTouches: [touch]
            })
        }
        target.dispatchEvent(make('touchstart', x, y))
        const steps = 6
        for (let i = 1; i <= steps; i++) {
            target.dispatchEvent(make('touchmove', x + (dx * i) / steps, y + (dy * i) / steps))
        }
        target.dispatchEvent(make('touchend', x + dx, y + dy))
    }, { x, y, dx, dy })
}

test.describe('提醒中心', () => {
    test.beforeEach(skipUnlessProject('mobile'))

    test('R1 未读/全部 Tab + 角标 + 已读 + 滑动删除 + 重试 + 全部已读', async ({ page }) => {
        await loginAs(page, RC)
        await clearForceMode(page)
        await page.goto('/m/reminder')
        // 角标 = 未读数（fixtures 3 条未读）
        await expect(page.locator('.notice')).toHaveCount(4, { timeout: 8000 })
        await expect(page.locator('.reminder-tab-badge')).toHaveText('3')
        // 未读 Tab 过滤
        await page.locator('.van-tab', { hasText: '未读' }).click()
        await expect(page.locator('.notice')).toHaveCount(3)
        await page.locator('.van-tab', { hasText: '全部' }).click()
        await expect(page.locator('.notice')).toHaveCount(4)
        // 点开未读 → 标记已读 + 弹详情
        await page.locator('.notice', { hasText: '待办提醒' }).click()
        await waitForRequest(RC, r => r.method === 'PUT' && r.path === '/reminder/notifications/read' && JSON.stringify(r.body) === '[1]')
        await expect(page.locator('.van-dialog__message')).toContainText('取快递')
        await page.locator('.van-dialog__confirm').click()
        await expect(page.locator('.reminder-tab-badge')).toHaveText('2')
        // 发送失败重试
        await page.locator('.notice__retry', { hasText: '发送失败，点击重试' }).click()
        await waitForRequest(RC, r => r.method === 'POST' && r.path === '/reminder/notifications/2/retry')
        await expect(page.locator('.van-toast')).toContainText('已重新发送')
        // 滑动删除已读通知
        const row = page.locator('.notice-swipe', { hasText: '视频会员' })
        await touchSwipe(page, row, -160)
        await row.locator('.notice__delete').click()
        await page.locator('.van-dialog__confirm').click()
        await waitForRequest(RC, r => r.method === 'DELETE' && r.path === '/reminder/notifications/3')
        await expect(page.locator('.notice', { hasText: '视频会员' })).toHaveCount(0)
        // 全部标为已读
        await page.locator('.m-sticky-actions .van-button', { hasText: '全部标为已读' }).click()
        await page.locator('.van-dialog__confirm').click()
        await waitForRequest(RC, r => r.method === 'PUT' && r.path === '/reminder/notifications/read' && JSON.stringify(r.body) === '[]')
        await expect(page.locator('.reminder-tab-badge')).toHaveCount(0)
        await page.locator('.van-tab', { hasText: '未读' }).click()
        await expect(page.getByText('没有未读通知')).toBeVisible()
    })

    test('R2 提醒偏好：渠道开关 + 免打扰时段保存', async ({ page }) => {
        await loginAs(page, RC)
        await clearForceMode(page)
        await page.goto('/m/reminder/settings')
        const emailCell = page.locator('.van-cell', { hasText: '邮件提醒' })
        await expect(emailCell.locator('.van-switch')).toBeVisible({ timeout: 8000 })
        // fixtures emailEnabled=false → 初始关闭
        await expect(emailCell.locator('.van-switch')).not.toHaveClass(/van-switch--on/)
        await emailCell.locator('.van-switch').click()
        await expect(emailCell.locator('.van-switch')).toHaveClass(/van-switch--on/)
        // 免打扰开始时间：23:00 → 22:00（TimePicker 渲染为内部 van-picker，无 van-time-picker 根类）
        await page.locator('.van-cell', { hasText: '开始时间' }).click()
        const picker = page.locator('.van-popup .van-picker')
        await expect(picker).toBeVisible()
        await picker.locator('.van-picker-column').first().locator('li', { hasText: '22' }).first().click()
        await picker.getByRole('button', { name: '确认' }).click()
        await expect(page.locator('.van-cell', { hasText: '开始时间' })).toContainText('22:00')
        // 保存断言
        await page.locator('.m-sticky-actions .van-button', { hasText: '保存设置' }).click()
        const req = await waitForRequest(RC, r => r.method === 'PUT' && r.path === '/reminder/preference')
        expect(req.body).toMatchObject({
            websocketEnabled: true,
            pushEnabled: true,
            emailEnabled: true,
            quietStart: '22:00',
            quietEnd: '07:00'
        })
        await expect(page.locator('.van-toast')).toContainText('已保存')
    })
})

test.describe('家庭日历与驾驶舱', () => {
    test.beforeEach(skipUnlessProject('mobile'))

    test.beforeEach(async () => {
        await overrideRoute(RC, 'GET', '/calendar/month', {
            data: [
                { date: D(0), sourceType: 'todo', color: 'todo', title: '写周报', status: 'pending', sourceId: 701 },
                { date: D(0), sourceType: 'commemoration', color: 'commemoration', title: '恋爱纪念日', status: 'info', sourceId: 801 },
                { date: D(3), sourceType: 'bookkeeping', color: 'bookkeeping', title: '午餐支出 25.5 元', status: 'info', sourceId: 501 }
            ]
        })
    })

    test.afterEach(async () => {
        await clearOverride(RC, 'GET', '/calendar/month')
    })

    test('R3 月历 42 格 + 彩点图例 + 选中日事件聚合', async ({ page }) => {
        await loginAs(page, RC)
        await clearForceMode(page)
        await page.goto('/m/calendar')
        await expect(page.locator('.cal-day')).toHaveCount(42, { timeout: 8000 })
        // 今日两枚彩点
        const todayCell = page.locator('.cal-day--today')
        await expect(todayCell).toBeVisible()
        await expect(todayCell.locator('.cal-day__dot')).toHaveCount(2)
        // 图例：fixtures 出现的三种类型
        const legend = page.locator('.cal-legend')
        await expect(legend).toBeVisible()
        await expect(legend.locator('.cal-legend__item')).toHaveCount(3)
        await expect(legend).toContainText('待办')
        await expect(legend).toContainText('纪念日')
        await expect(legend).toContainText('记账')
        // 选中日（默认今天）事件聚合
        await expect(page.locator('.cal-events__count')).toHaveText('2项')
        await expect(page.locator('.cal-event__title', { hasText: '写周报' })).toBeVisible()
        await expect(page.locator('.cal-event__title', { hasText: '恋爱纪念日' })).toBeVisible()
        await expect(page.locator('.cal-event__type', { hasText: '待办 · 待处理' })).toBeVisible()
        // 切换选中日 → 聚合另一天的事件
        const dayNum = String(new Date(TODAY.getTime() + 3 * 86400000).getDate())
        await page.locator('.cal-day:not(.cal-day--other)').filter({ hasText: dayNum }).first().click()
        await expect(page.locator('.cal-events__count')).toHaveText('1项')
        await expect(page.locator('.cal-event__title', { hasText: '午餐支出 25.5 元' })).toBeVisible()
    })

    test('R4 待办就地完成 + 快捷创建跳转', async ({ page }) => {
        await loginAs(page, RC)
        await clearForceMode(page)
        await page.goto('/m/calendar')
        await expect(page.locator('.cal-event__title', { hasText: '写周报' })).toBeVisible({ timeout: 8000 })
        // 就地完成：getDo + updateDo
        await page.locator('.cal-event__done', { hasText: '完成' }).click()
        await waitForRequest(RC, r => r.method === 'GET' && r.path === '/admin/toDo/701')
        const put = await waitForRequest(RC, r => r.method === 'PUT' && r.path === '/admin/toDo')
        expect(put.body).toMatchObject({ id: 701, status: true })
        await expect(page.locator('.van-toast')).toContainText('待办已完成')
        // 快捷创建：＋待办
        await page.locator('.quick-actions__item', { hasText: '＋待办' }).click()
        await expect(page).toHaveURL(new RegExp('/m/todo/edit\\?date=' + D(0)))
        await page.goBack()
        await expect(page.locator('.quick-actions__item', { hasText: '＋记账' })).toBeVisible()
        await page.locator('.quick-actions__item', { hasText: '＋记账' }).click()
        await expect(page).toHaveURL(new RegExp('/m/bookkeeping/record/add\\?date=' + D(0)))
    })

    test('R5 驾驶舱数据卡', async ({ page }) => {
        await loginAs(page, RC)
        await clearForceMode(page)
        await page.goto('/m/calendar')
        const card = page.locator('.cockpit-card')
        await expect(card).toBeVisible({ timeout: 8000 })
        await expect(card.locator('.cockpit-card__item').nth(0).locator('.cockpit-card__value')).toHaveText('2')
        await expect(card.locator('.cockpit-card__item').nth(1).locator('.cockpit-card__value')).toHaveText('1')
        await expect(card.locator('.cockpit-card__item--wide')).toContainText('恋爱纪念日 · 5天后')
        await expect(card.locator('.cockpit-card__todo-text', { hasText: '写周报' })).toBeVisible()
    })
})

test.describe('生活报告', () => {
    test.beforeEach(skipUnlessProject('mobile'))

    test('R6 周期/类型 Tab + 汇总卡 + AI 流式生成与中断', async ({ page }) => {
        await loginAs(page, RC)
        await clearForceMode(page)
        await page.goto('/m/report')
        // 汇总三卡
        await expect(page.locator('.summary-card__value--expense')).toHaveText('¥258.50', { timeout: 8000 })
        await expect(page.locator('.summary-card__value--diary')).toHaveText('2')
        await expect(page.locator('.summary-card__value--todo')).toHaveText('67%')
        await expect(page.locator('.summary-card__sub', { hasText: '3笔记录' })).toBeVisible()
        // 周期 / 类型 Tab
        await page.locator('.period-selector__item', { hasText: '本月' }).click()
        await waitForRequest(RC, r => r.method === 'GET' && r.path === '/lifeReport/data' && r.query.period === 'month')
        await page.locator('.report-tabs__item', { hasText: '消费' }).click()
        await waitForRequest(RC, r => r.method === 'GET' && r.path === '/lifeReport/data' && r.query.reportType === 'expense')
        // 流式生成 → 中断
        await setStream(RC, '/lifeReport/stream', [
            { data: '## 本周生活小结', delay: 150 },
            { data: '\n\n- 支出集中在餐饮', delay: 200 },
            { data: '\n- 待办完成率 67%', delay: 1500 },
            { data: '\n\n**建议**：保持节奏。', delay: 60 }
        ])
        await page.locator('.m-sticky-actions .van-button', { hasText: '生成 AI 报告' }).click()
        const content = page.locator('.ai-report__content')
        await expect(content).toContainText('支出集中在餐饮', { timeout: 8000 })
        await page.locator('.van-button', { hasText: '停止生成' }).click()
        await expect(page.locator('.van-button', { hasText: '停止生成' })).toHaveCount(0)
        // 中断后保留已生成内容
        await expect(content).toContainText('支出集中在餐饮')
        await expect(content).not.toContainText('建议')
        // 重新生成完整 markdown
        await setStream(RC, '/lifeReport/stream', [
            { data: '## 本周生活小结', delay: 120 },
            { data: '\n\n- 支出集中在餐饮', delay: 160 },
            { data: '\n- 待办完成率 67%', delay: 160 },
            { data: '\n\n**建议**：保持节奏。', delay: 120 },
            { data: '[DONE]', delay: 0 }
        ])
        await page.locator('.m-sticky-actions .van-button', { hasText: '生成 AI 报告' }).click()
        await expect(content.locator('h2', { hasText: '本周生活小结' })).toBeVisible({ timeout: 8000 })
        await expect(content.locator('li')).toHaveCount(2)
        await expect(content.locator('strong', { hasText: '建议' })).toBeVisible()
    })

    test('R7 ReportActions 快照 / 候选 / 预览确认 / 忽略', async ({ page }) => {
        const RA2 = TOKENS.RA2
        await loginAs(page, RA2)
        await clearForceMode(page)
        await page.goto('/m/report')
        const actions = page.locator('.report-actions')
        await expect(actions).toBeVisible({ timeout: 8000 })
        await expect(actions.locator('.insight__fact', { hasText: '咖啡支出' })).toBeVisible()
        // 生成事实快照
        await actions.getByRole('button', { name: '生成事实快照' }).click()
        const snap = await waitForRequest(RA2, r => r.method === 'POST' && r.path === '/lifeReport/insights')
        expect(snap.body).toMatchObject({ period: 'week' })
        await expect(page.locator('.van-toast')).toContainText('快照已生成')
        // 候选行动 → 预览并确认
        const ins1 = actions.locator('.insight', { hasText: '咖啡支出' })
        await ins1.locator('.candidate', { hasText: '设置每周咖啡预算' }).getByRole('button', { name: '选择并编辑' }).click()
        const form = page.locator('.action-form')
        await expect(form).toBeVisible()
        await expect(form.locator('textarea')).toHaveValue('设置每周咖啡预算 50 元')
        await form.getByRole('button', { name: '预览并确认' }).click()
        await waitForRequest(RA2, r => r.method === 'POST' && r.path === '/lifeReport/insights/ins1/action-preview')
        await expect(page.locator('.van-dialog__message')).toContainText('设置每周咖啡预算 50 元')
        await page.locator('.van-dialog__confirm').click()
        const accept = await waitForRequest(RA2, r => r.method === 'POST' && r.path === '/lifeReport/insights/ins1/accept')
        expect(accept.body).toMatchObject({ expectedPlanId: 'ccddee03-0123456789abcdef0123456789c' })
        await expect(page.locator('.van-toast')).toContainText('已创建待办')
        // 忽略另一条
        await actions.locator('.insight', { hasText: '待办完成率' }).getByRole('button', { name: '忽略' }).click()
        await waitForRequest(RA2, r => r.method === 'POST' && r.path === '/lifeReport/insights/ins2/dismiss')
        await expect(page.locator('.van-toast')).toContainText('已忽略')
    })
})

test.describe('全局搜索', () => {
    test.beforeEach(skipUnlessProject('mobile'))

    test('R8 关键词 + 类型 chips + 日期范围 + 分组结果；R9 AI 总结', async ({ page }) => {
        await loginAs(page, RC)
        await clearForceMode(page)
        await page.goto('/m/search')
        await page.locator('.van-search input').fill('快递')
        await page.locator('.advanced-filter--action', { hasText: '搜索' }).click()
        // 分组结果
        const groups = page.locator('.result-group')
        await expect(groups).toHaveCount(3, { timeout: 8000 })
        await expect(groups.filter({ hasText: '待办' }).locator('.result-group__count')).toHaveText('1条')
        await expect(groups.filter({ hasText: '账本' }).locator('.result__title', { hasText: '午餐 25.5 元' })).toBeVisible()
        await expect(groups.filter({ hasText: '日记' }).locator('.result__title', { hasText: '晴，心情不错' })).toBeVisible()
        // 类型 chip：请求带 type=todo
        await page.locator('.filter-chip', { hasText: '待办' }).click()
        await page.locator('.advanced-filter--action', { hasText: '搜索' }).click()
        const typed = await getRequests(RC, { path: '/admin/globalSearch' })
        expect(typed.at(-1).query.type).toBe('todo')
        await expect(page.locator('.filter-chip--active')).toHaveText('待办')
        // 日期范围 chips：最近 7 天 → 请求带 startDate
        await page.locator('.advanced-filter', { hasText: '全部时间' }).click()
        const rangePicker = page.locator('.van-picker')
        await expect(rangePicker).toBeVisible()
        await rangePicker.locator('li', { hasText: '最近7天' }).click()
        await rangePicker.getByRole('button', { name: '确认' }).click()
        const ranged = await waitForRequest(RC, r => r.method === 'GET' && r.path === '/admin/globalSearch' && r.query.startDate)
        expect(ranged.query.endDate).toBeTruthy()
        // R9 AI 总结（同步 POST）→ markdown 渲染
        await page.locator('.ai-summary').click()
        await waitForRequest(RC, r => r.method === 'POST' && r.path === '/admin/globalSearch/summary')
        const summary = page.locator('.summary-card')
        await expect(summary).toBeVisible({ timeout: 8000 })
        await expect(summary.locator('.summary-card__content strong', { hasText: '搜索总结' })).toBeVisible()
        await expect(summary.locator('.summary-card__content li')).toHaveCount(2)
    })
})

test.describe('通知深链', () => {
    test.beforeEach(skipUnlessProject('mobile'))

    test('R10 通知跳转 todo/纪念日/经期 deep link', async ({ page }) => {
        await loginAs(page, RC)
        await clearForceMode(page)
        await overrideRoute(RC, 'GET', '/reminder/notifications', {
            data: [
                { id: 11, title: '待办提醒', content: '取快递', status: '0', sourceType: 'todo', sourceId: 101, sendTime: '2026-11-15 09:00:00' },
                { id: 12, title: '纪念日提醒', content: '恋爱纪念日还有 5 天', status: '0', sourceType: 'commemoration', sourceId: 801, sendTime: '2026-11-15 08:00:00' },
                { id: 14, title: '经期提醒', content: '经期第 5 天', status: '0', sourceType: 'menstruation', sourceId: null, sendTime: '2026-11-15 06:00:00' }
            ]
        })
        const open = async (title) => {
            await page.goto('/m/reminder')
            await page.locator('.notice', { hasText: title }).first().click()
        }
        await open('待办提醒')
        await expect(page).toHaveURL(/\/m\/todo\/edit\?id=101/, { timeout: 8000 })
        await open('纪念日提醒')
        await expect(page).toHaveURL(/\/m\/commemoration-day\/add\?id=801/)
        await open('经期提醒')
        await expect(page).toHaveURL(/\/m\/menstruation$/)
        await expect(page.locator('.menstruation-hero__eyebrow')).toHaveText('姨妈助手')
        await clearOverride(RC, 'GET', '/reminder/notifications')
    })
})
