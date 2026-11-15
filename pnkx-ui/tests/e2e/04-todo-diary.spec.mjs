import { test, expect, skipUnlessProject } from './helpers/setup.mjs'
import { loginAs, overrideRoute, clearOverride, setStream, getRequests, waitForRequest, clearRequests } from './helpers/mock.mjs'

/**
 * 待办 + 日记域（计划 §7 T1-T10）
 * 专属 token：test-token-td（V2 全关；overrides / 请求日志按 token 隔离）
 *
 * Mock 限制说明（server.mjs 只按 METHOD+PATH 匹配、不区分 query）：
 * - /admin/toDo/list 的 3 个统计查询（status=false / status=true / 今日区间）拿到同一份响应，
 *   故 4 个统计 Tab 恒为：全部=2×total、进行中=今日=完成=total，完成率恒 50%（total>0 时）。
 *   用例按此确定性映射断言；差异化数据一律用 overrideRoute 按 phase 切换。
 * - 「全部」Tab 会合并 active/done 两个 source 的同源响应（rows<10 时出现重复卡片），
 *   涉及计数/日历的用例先切到单 source 的 Tab（进行中）再断言。
 */

const TOKEN = 'test-token-td'

// ---------- 日期工具（与页面同构的本地格式化） ----------
const fmt = d => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
const addDays = n => {
    const d = new Date()
    d.setDate(d.getDate() + n)
    return d
}
const todayStr = () => fmt(new Date())
const tomorrowStr = () => fmt(addDays(1))

// ---------- 覆写管理（afterEach 统一清理，避免跨用例串扰） ----------
const overrideKeys = new Set()
async function ov(method, path, body, status) {
    overrideKeys.add(`${method} ${path}`)
    await overrideRoute(TOKEN, method, path, body, status)
}

const todoRow = (id, content, date, { status = false, label = '', endTime = '18:00:00' } = {}) => ({
    id,
    content,
    planStartTime: `${date} 09:00:00`,
    planEndTime: `${date} ${endTime}`,
    status,
    label,
    ...(status ? { finishTime: `${date} 12:00:00` } : {})
})

const diaryRow = (id, title, date, mood, weather, content) => ({ id, title, date, mood, weather, content })

test.describe('待办+日记域', () => {
    test.beforeEach(skipUnlessProject('mobile'))

    test.afterEach(async () => {
        for (const key of overrideKeys) {
            const [method, ...rest] = key.split(' ')
            await clearOverride(TOKEN, method, rest.join(' '))
        }
        overrideKeys.clear()
        await clearRequests(TOKEN)
    })

    test('T1 待办列表渲染：完成率与 4 统计 Tab 计数', async ({ page }) => {
        await loginAs(page, TOKEN)
        await page.goto('/m/todo')

        await expect(page.locator('.todo-header__title')).toHaveText('待办事项')
        // 完成率 = completedTotal / (activeTotal + completedTotal) = total / 2×total = 50%
        await expect(page.locator('.todo-header__progress-num')).toHaveText('50%')

        // 4 统计 Tab（fixtures total=5 → 全部 10 / 其余 5）
        const tabValue = label => page.locator('.todo-tab', { hasText: label }).locator('.todo-tab__value')
        await expect(tabValue('全部')).toHaveText('10')
        await expect(tabValue('进行中')).toHaveText('5')
        await expect(tabValue('今日')).toHaveText('5')
        await expect(tabValue('完成')).toHaveText('5')

        // 列表卡片：进行中在前、逾期标记、标签、执行者
        const card = content => page.locator('.todo-card').filter({ hasText: content })
        await expect(page.locator('.todo-card')).toHaveCount(10)
        await expect(card('写周报')).toBeVisible()
        await expect(card('写周报').locator('.todo-card__label')).toHaveText('工作')
        await expect(card('写周报').locator('.todo-card__performer')).toContainText('小美')
        await expect(card('交房租').locator('.todo-card__overdue')).toHaveText('已逾期')
        // 完成项置底且有删除线样式
        await expect(card('取快递')).toHaveClass(/is-completed/)
        await expect(card('取快递').locator('.todo-card__status-text')).toHaveText('已完成')
        // 标签筛选 chips（getLabelList fixtures）
        await expect(page.locator('.todo-header__labels .label-chip', { hasText: '工作' })).toBeVisible()
        await expect(page.locator('.todo-header__labels .label-chip', { hasText: '生活' })).toBeVisible()
    })

    test('T2 新建待办 → 列表出现 → 勾选完成 → 完成率变化', async ({ page }) => {
        // 阶段 1：空列表（total=0 → 完成率 0%）
        await ov('GET', '/admin/toDo/list', { rows: [], total: 0 })
        await loginAs(page, TOKEN)
        await page.goto('/m/todo')
        await expect(page.locator('.m-empty', { hasText: '暂无待办' })).toBeVisible()
        await expect(page.locator('.todo-header__progress-num')).toHaveText('0%')

        // 新建：内容 + 计划时间（选择器确认）+ 标签
        await page.locator('.m-fab').click()
        await expect(page).toHaveURL(/\/m\/todo\/edit$/)
        await page.locator('textarea[placeholder="请输入待办事项内容..."]').fill('给妈妈订生日蛋糕')
        await page.locator('.label-selector .label-chip', { hasText: '工作' }).click()
        // 计划结束时间：打开日期+时间选择器并确认默认值
        await page.locator('.van-cell', { hasText: '计划结束时间' }).click()
        await expect(page.locator('.time-picker')).toBeVisible()
        await page.locator('.time-picker__actions .van-button').last().click()
        await expect(page.locator('.van-cell', { hasText: '计划结束时间' })).toContainText(/\d{4}-\d{2}-\d{2} \d{2}:\d{2}/)

        // 保存 → 断言 POST 请求体
        await ov('GET', '/admin/toDo/list', {
            rows: [todoRow(9101, '给妈妈订生日蛋糕', todayStr(), { label: '工作' })],
            total: 0
        })
        await page.locator('.m-sticky-actions .van-button', { hasText: '保存' }).click()
        const post = await waitForRequest(TOKEN, r => r.method === 'POST' && r.path === '/admin/toDo')
        expect(post.body.content).toBe('给妈妈订生日蛋糕')
        expect(post.body.label).toBe('工作')
        expect(post.body.planStartTime).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/)
        expect(post.body.planEndTime).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/)
        expect(post.body.status).toBe(false)

        // 返回列表 → 新待办出现（「全部」Tab 双 source 同响应会出现重复卡片，断言取第一个）
        await expect(page).toHaveURL(/\/m\/todo$/)
        const newCard = page.locator('.todo-card').filter({ hasText: '给妈妈订生日蛋糕' }).first()
        await expect(newCard).toBeVisible()
        await expect(page.locator('.todo-header__progress-num')).toHaveText('0%')

        // 勾选完成 → 断言 PUT 请求体；完成后统计 total>0 → 完成率 50%
        await ov('GET', '/admin/toDo/list', {
            rows: [
                todoRow(9101, '给妈妈订生日蛋糕', todayStr(), { label: '工作', status: true }),
                todoRow(9102, '写周报', tomorrowStr(), { label: '工作' })
            ],
            total: 2
        })
        await newCard.locator('.todo-card__status').click()
        const put = await waitForRequest(TOKEN, r => r.method === 'PUT' && r.path === '/admin/toDo')
        expect(put.body.id).toBe(9101)
        expect(put.body.content).toBe('给妈妈订生日蛋糕')
        expect(put.body.status).toBe(true)
        expect(put.body.finishTime).toBeTruthy()

        await expect(page.locator('.todo-header__progress-num')).toHaveText('50%')
        await expect(newCard).toHaveClass(/is-completed/)
    })

    test('T3 状态筛选：全部/进行中/今日/完成', async ({ page }) => {
        await loginAs(page, TOKEN)

        // 进行中：单 source，列表唯一
        await ov('GET', '/admin/toDo/list', {
            rows: [
                todoRow(9301, '写项目周报', todayStr(), { label: '工作' }),
                todoRow(9302, '预约牙医', tomorrowStr(), { label: '生活' }),
                todoRow(9303, '买牛奶和鸡蛋', todayStr(), { label: '生活' })
            ],
            total: 3
        })
        await page.goto('/m/todo')
        await page.locator('.todo-tab', { hasText: '进行中' }).click()
        await waitForRequest(TOKEN, r => r.path === '/admin/toDo/list' && r.query.status === 'false')
        await expect(page.locator('.todo-card')).toHaveCount(3)
        await expect(page.locator('.todo-card').filter({ hasText: '写项目周报' })).toBeVisible()
        await expect(page.locator('.todo-card').filter({ hasText: '取快递' })).toHaveCount(0)

        // 完成
        await ov('GET', '/admin/toDo/list', {
            rows: [
                todoRow(9311, '取快递', todayStr(), { label: '生活', status: true }),
                todoRow(9312, '缴水电费', todayStr(), { label: '生活', status: true })
            ],
            total: 2
        })
        await page.locator('.todo-tab', { hasText: '完成' }).click()
        await waitForRequest(TOKEN, r => r.path === '/admin/toDo/list' && r.query.status === 'true')
        await expect(page.locator('.todo-card')).toHaveCount(2)
        for (const name of ['取快递', '缴水电费']) {
            await expect(page.locator('.todo-card').filter({ hasText: name })).toHaveClass(/is-completed/)
        }

        // 今日：断言查询携带今日时间区间
        await ov('GET', '/admin/toDo/list', {
            rows: [todoRow(9321, '今日站会', todayStr(), { label: '工作' })],
            total: 1
        })
        await page.locator('.todo-tab', { hasText: '今日' }).click()
        await waitForRequest(TOKEN, r =>
            r.path === '/admin/toDo/list' &&
            r.query.planStartTime === `${todayStr()} 00:00:00` &&
            r.query.planEndTime === `${todayStr()} 23:59:59`
        )
        await expect(page.locator('.todo-card').filter({ hasText: '今日站会' })).toBeVisible()

        // 全部：进行中在前 + 完成项
        await ov('GET', '/admin/toDo/list', {
            rows: [
                todoRow(9331, '写周报', tomorrowStr(), { label: '工作' }),
                todoRow(9332, '取快递', todayStr(), { label: '生活', status: true })
            ],
            total: 2
        })
        await page.locator('.todo-tab', { hasText: '全部' }).click()
        await expect(page.locator('.todo-card').filter({ hasText: '写周报' }).first()).toBeVisible()
        await expect(page.locator('.todo-card').filter({ hasText: '取快递' }).first()).toBeVisible()
    })

    test('T4 日历视图：日期标记与点选过滤', async ({ page }) => {
        await ov('GET', '/admin/toDo/list', {
            rows: [
                todoRow(9401, '今日复盘', todayStr(), { label: '工作' }),
                todoRow(9402, '明日出行准备', tomorrowStr(), { label: '生活' }),
                todoRow(9403, '预约餐厅', tomorrowStr(), { label: '生活', endTime: '12:00:00' })
            ],
            total: 3
        })
        await loginAs(page, TOKEN)
        await page.goto('/m/todo')
        // 切到单 source 的「进行中」避免「全部」双 source 重复行干扰计数
        await page.locator('.todo-tab', { hasText: '进行中' }).click()
        await expect(page.locator('.todo-card')).toHaveCount(3)

        // 切日历
        await page.locator('.todo-header__switch .switch-item', { hasText: '日历' }).click()
        await expect(page.locator('.calendar-view .van-calendar')).toBeVisible()
        // 今日/明日两个标记点（bottomInfo '•'）
        const marks = page.locator('.van-calendar__day').filter({ has: page.locator('.van-calendar__bottom-info') })
        await expect(marks).toHaveCount(2)

        // 默认选中今日 → 1 项
        await expect(page.locator('.calendar-view .section-count')).toHaveText('1 项')
        await expect(page.locator('.calendar-view .todo-card').filter({ hasText: '今日复盘' })).toBeVisible()

        // 点选明日（第二个标记日）→ 2 项
        await marks.nth(1).click()
        await expect(page.locator('.calendar-view .section-count')).toHaveText('2 项')
        await expect(page.locator('.calendar-view .todo-card').filter({ hasText: '明日出行准备' })).toBeVisible()
        await expect(page.locator('.calendar-view .todo-card').filter({ hasText: '预约餐厅' })).toBeVisible()

        // 点选无待办日期 → 空态
        await page.locator('.van-calendar__day:not(:has(.van-calendar__bottom-info))').first().click()
        await expect(page.locator('.calendar-view .m-empty', { hasText: '该日期暂无待办' })).toBeVisible()
    })

    test('T5 标签筛选 + 搜索防抖', async ({ page }) => {
        await loginAs(page, TOKEN)

        // 标签筛选
        await ov('GET', '/admin/toDo/list', {
            rows: [todoRow(9501, '买牛奶和鸡蛋', todayStr(), { label: '生活' })],
            total: 1
        })
        await page.goto('/m/todo')
        await page.locator('.todo-header__labels .label-chip', { hasText: '生活' }).click()
        await waitForRequest(TOKEN, r => r.path === '/admin/toDo/list' && r.query.label === '生活')
        await expect(page.locator('.todo-card').filter({ hasText: '买牛奶和鸡蛋' }).first()).toBeVisible()
        await expect(page.locator('.todo-card').filter({ hasText: '写周报' })).toHaveCount(0)

        // 重置标签，进入搜索场景
        await page.locator('.todo-header__labels .label-chip', { hasText: '全部' }).click()
        await expect(page.locator('.todo-header__labels .label-chip', { hasText: '全部' })).toHaveClass(/active/)

        // 搜索防抖：逐字输入不立即请求，400ms 防抖后仅触发一次
        await ov('GET', '/admin/toDo/list', {
            rows: [todoRow(9502, '写周报', tomorrowStr(), { label: '工作' })],
            total: 1
        })
        await clearRequests(TOKEN)
        const input = page.locator('.todo-header__search input')
        await input.click()
        await input.pressSequentially('写周报', { delay: 200 })
        await page.waitForTimeout(150)
        const early = (await getRequests(TOKEN, { path: '/admin/toDo/list' })).filter(r => r.query.content)
        expect(early.length).toBe(0)

        await waitForRequest(TOKEN, r => r.path === '/admin/toDo/list' && r.query.content === '写周报')
        await page.waitForTimeout(800) // 足以暴露「每次按键都请求」的坏情况
        const searchLoads = (await getRequests(TOKEN, { path: '/admin/toDo/list' }))
            .filter(r => r.query.content === '写周报' && r.query.status === 'false' && r.query.pageNum === '1' && r.query.pageSize === '10')
        expect(searchLoads.length).toBe(1)
        await expect(page.locator('.todo-card').filter({ hasText: '写周报' }).first()).toBeVisible()
        await expect(page.locator('.todo-card').filter({ hasText: '买牛奶和鸡蛋' })).toHaveCount(0)
    })

    test('T6 编辑页：ReminderSetting 绑定提醒 + AssignmentHistory 转派', async ({ page }) => {
        await loginAs(page, TOKEN)
        await page.goto('/m/todo/edit?id=9101')
        await expect(page.locator('textarea[placeholder="请输入待办事项内容..."]')).toHaveValue('准备家庭旅行')

        // ---- 转派（AssignmentHistory）----
        await page.locator('.m-assignment__header').click()
        await expect(page.locator('.m-assignment__body')).toBeVisible()
        await expect(page.locator('.m-assignment__value')).toHaveText('小美')
        await expect(page.locator('.m-assignment__body')).toContainText('出差转交') // 历史记录回显
        await page.locator('.m-assignment__candidate', { hasText: '阿强' }).click()
        await expect(page.locator('.m-assignment__candidate', { hasText: '阿强' })).toHaveClass(/active/)
        await page.locator('textarea[placeholder="填写转派原因（必填）"]').fill('下周我出差，转给阿强跟进')
        await page.locator('.m-assignment .van-button', { hasText: '确认转派' }).click()
        const transfer = await waitForRequest(TOKEN, r => r.method === 'POST' && r.path === '/life/assignments/todo/9101/transfer')
        expect(transfer.body.assignee).toBe('3')
        expect(transfer.body.expectedAssignee).toBe('2')
        expect(transfer.body.reason).toBe('下周我出差，转给阿强跟进')
        expect(transfer.body.requestId).toMatch(/^assign_/)
        // 转派成功后负责人更新
        await expect(page.locator('.m-assignment__value')).toHaveText('阿强')

        // ---- 绑定提醒（ReminderSetting）----
        await expect(page.locator('.van-cell', { hasText: '到期提醒' })).toContainText('未设置')
        await page.locator('.van-cell', { hasText: '到期提醒' }).click()
        await expect(page.locator('.m-reminder')).toBeVisible()
        await expect(page.locator('.m-reminder__name')).toHaveText('准备家庭旅行')
        await expect(page.locator('.m-reminder__time')).toContainText('2026-11-20 18:00')

        // 提前量选择：默认「提前 1 小时」→ 选「提前 1 天」
        const leadCell = page.locator('.m-reminder .van-cell', { hasText: '提前提醒' })
        await expect(leadCell).toContainText('提前 1 小时')
        await leadCell.click()
        await expect(page.locator('.van-picker')).toBeVisible()
        await page.locator('.van-picker-column__item', { hasText: '提前 1 天' }).click()
        await page.waitForTimeout(600) // 等待滚轮归位
        await page.locator('.van-picker__confirm').click()
        await expect(leadCell).toContainText('提前 1 天')

        await page.locator('.m-reminder__actions .van-button', { hasText: '保存提醒' }).click()
        const bind = await waitForRequest(TOKEN, r => r.method === 'POST' && r.path === '/reminder/bind')
        expect(bind.body.sourceType).toBe('todo')
        expect(bind.body.sourceId).toBe(9101)
        expect(bind.body.leadMinutes).toBe(1440)
        expect(bind.body.enabled).toBe(true)
        // remindTime = 事件时间 2026-11-20 18:00 - 1 天
        expect(bind.body.remindTime).toBe('2026-11-19 18:00:00')
        // 绑定成功后 cell 显示「已开启」
        await expect(page.locator('.van-cell', { hasText: '到期提醒' })).toContainText('已开启')
    })

    test('T7 日记首页：时间线/日历双视图 + 统计卡', async ({ page }) => {
        const now = new Date()
        const dToday = todayStr()
        const dYesterday = fmt(addDays(-1))
        const prev = new Date(now.getFullYear(), now.getMonth() - 1, 15)
        const dPrevMonth = fmt(prev)
        const rows = [
            diaryRow(9301, '今天的日记', dToday, 'x-gaoxing', 'w-qing', '今天心情不错，去了新开的咖啡店。'),
            diaryRow(9302, '昨天的日记', dYesterday, 'x-ku', 'w-yin', '昨天加班到九点。'),
            diaryRow(9303, '上月小结', dPrevMonth, 'x-xihuan', 'w-duoyun', '读完了两本书。')
        ]
        await ov('GET', '/admin/diary/list', { rows, total: 3 })
        await loginAs(page, TOKEN)
        await page.goto('/m/diary')

        // 篇记录 = total；本月/本周/连续为前端按日期聚合（与页面同构算法）
        await expect(page.locator('.diary-header__count-num')).toHaveText('3')
        const statValue = label => page.locator('.stat-item', { hasText: label }).locator('.stat-item__value')
        const curMonth = dToday.slice(0, 7)
        const expectedMonth = rows.filter(r => r.date.slice(0, 7) === curMonth).length
        const dayIdx = (now.getDay() + 6) % 7 // 周一为一周起点
        const weekStart = new Date(now)
        weekStart.setHours(0, 0, 0, 0)
        weekStart.setDate(now.getDate() - dayIdx)
        const expectedWeek = rows.filter(r => new Date(r.date.replace(/-/g, '/')) >= weekStart).length
        await expect(statValue('本月')).toHaveText(String(expectedMonth))
        await expect(statValue('本周')).toHaveText(String(expectedWeek))
        await expect(statValue('连续')).toHaveText('2')

        // 时间线视图：卡片 + 心情/天气图标
        const card = page.locator('.diary-card').filter({ hasText: '今天的日记' })
        await expect(card).toBeVisible()
        await expect(card.locator('.icon-chip img').first()).toBeVisible()

        // 切日历：'记' 标记 + 默认选中今日（标记数随行内日期动态计算）
        await page.locator('.diary-header__switch .switch-item', { hasText: '日历' }).click()
        await expect(page.locator('.calendar-view .van-calendar')).toBeVisible()
        const marks = page.locator('.van-calendar__day').filter({ has: page.locator('.van-calendar__bottom-info') })
        // 日历范围为今年1/1~明年12/31，1月运行时上月15号超出范围（3个标记日中最多渲染2~3个）
        const calendarStart = new Date(now.getFullYear(), 0, 1)
        const expectedMarks = [...new Set(rows.map(r => r.date))].filter(d => new Date(d.replace(/-/g, '/')) >= calendarStart).length
        await expect(marks).toHaveCount(expectedMarks)
        await expect(page.locator('.calendar-view .section-count')).toHaveText('1篇')
        await expect(page.locator('.calendar-view .diary-card').filter({ hasText: '今天的日记' })).toBeVisible()

        // 点昨天 → 昨天的日记（标记日按 DOM 时序列，倒数第 2 个即昨天）
        const markCount = await marks.count()
        await marks.nth(markCount - 2).click()
        await expect(page.locator('.calendar-view .section-count')).toHaveText('1篇')
        await expect(page.locator('.calendar-view .diary-card').filter({ hasText: '昨天的日记' })).toBeVisible()

        // 切回时间线
        await page.locator('.diary-header__switch .switch-item', { hasText: '时间线' }).click()
        await expect(page.locator('.diary-card').filter({ hasText: '今天的日记' })).toBeVisible()
    })

    test('T8 写日记：心情/天气选择器 + 保存', async ({ page }) => {
        const baseRows = [
            diaryRow(9301, '今天的日记', todayStr(), 'x-gaoxing', 'w-qing', '今天心情不错。'),
            diaryRow(9302, '昨天的日记', fmt(addDays(-1)), 'x-ku', 'w-yin', '昨天加班。')
        ]
        await ov('GET', '/admin/diary/list', { rows: baseRows, total: 2 })
        await loginAs(page, TOKEN)
        await page.goto('/m/diary')
        await page.locator('.m-fab').click()
        await expect(page).toHaveURL(/\/m\/diary\/edit$/)
        await expect(page.locator('.edit-header__title')).toHaveText('写日记')
        await expect(page.locator('.mood-picker__placeholder', { hasText: '选择心情' })).toBeVisible()

        // 心情选择器：20 个 x-* 图标全部带真实 src
        await page.locator('.mood-picker__item', { hasText: '心情' }).click()
        const moodPopup = page.locator('.popup-container').filter({ hasText: '选择心情' })
        await expect(moodPopup.locator('.grid-item')).toHaveCount(20)
        const moodSrcs = await moodPopup.locator('.grid-item__icon').evaluateAll(els => els.map(e => e.getAttribute('src') || ''))
        expect(moodSrcs.filter(s => s.length > 0)).toHaveLength(20)
        await moodPopup.locator('.grid-item', { hasText: 'gaoxing' }).click()
        await expect(page.locator('.mood-picker__icon').first()).toBeVisible()

        // 天气选择器：25 个 w-* 图标
        await page.locator('.mood-picker__item', { hasText: '天气' }).click()
        const weatherPopup = page.locator('.popup-container').filter({ hasText: '选择天气' })
        await expect(weatherPopup.locator('.grid-item')).toHaveCount(25)
        const weatherSrcs = await weatherPopup.locator('.grid-item__icon').evaluateAll(els => els.map(e => e.getAttribute('src') || ''))
        expect(weatherSrcs.filter(s => s.length > 0)).toHaveLength(25)
        await weatherPopup.locator('.grid-item', { hasText: 'qing' }).first().click()
        await expect(page.locator('.mood-picker__icon')).toHaveCount(2)

        // 标题/正文
        await page.locator('input[placeholder="给今天起个标题"]').fill('公园散步')
        const content = '今天下午去公园走了走，阳光很好，心情舒畅。'
        await page.locator('textarea[placeholder="写下今天发生了什么，或只是此刻的心情..."]').fill(content)

        // 保存返回后列表含新日记
        await ov('GET', '/admin/diary/list', {
            rows: [...baseRows, diaryRow(9399, '公园散步', todayStr(), 'x-gaoxing', 'w-qing', content)],
            total: 3
        })
        await page.locator('.m-sticky-actions .save-btn').click()
        const post = await waitForRequest(TOKEN, r => r.method === 'POST' && r.path === '/admin/diary')
        expect(post.body.title).toBe('公园散步')
        expect(post.body.content).toBe(content)
        expect(post.body.richText).toBe(content)
        expect(post.body.mood).toBe('x-gaoxing')
        expect(post.body.weather).toBe('w-qing')
        expect(post.body.date).toBe(todayStr())

        await expect(page).toHaveURL(/\/m\/diary$/)
        await expect(page.locator('.diary-card').filter({ hasText: '公园散步' })).toBeVisible()
    })

    test('T9 日记编辑回填 + 删除确认', async ({ page }) => {
        // 经列表点卡片进编辑页（保证保存/删除后的 router.back() 回到 /m/diary）
        await ov('GET', '/admin/diary/list', {
            rows: [diaryRow(9201, '周末爬山', '2026-11-14', 'x-gaoxing', 'w-qing', '早上八点出发，走了一条新路线，山顶风景很好。')],
            total: 1
        })
        await loginAs(page, TOKEN)
        await page.goto('/m/diary')
        const entry = page.locator('.diary-card').filter({ hasText: '周末爬山' })
        await entry.click()

        // 回填
        await expect(page).toHaveURL(/\/m\/diary\/edit\?id=9201$/)
        await expect(page.locator('.edit-header__title')).toHaveText('编辑日记')
        await expect(page.locator('input[placeholder="给今天起个标题"]')).toHaveValue('周末爬山')
        await expect(page.locator('textarea[placeholder="写下今天发生了什么，或只是此刻的心情..."]'))
            .toHaveValue('早上八点出发，走了一条新路线，山顶风景很好。')
        await expect(page.locator('.edit-header__subtitle')).toContainText('2026年11月14日')
        await expect(page.locator('.mood-picker__icon')).toHaveCount(2) // 心情+天气图标回填

        // 修改保存 → PUT
        const edited = '早上八点出发，走了一条新路线，山顶风景很好。来回用了六个小时。'
        await page.locator('textarea[placeholder="写下今天发生了什么，或只是此刻的心情..."]').fill(edited)
        await page.locator('.m-sticky-actions .save-btn').click()
        const put = await waitForRequest(TOKEN, r => r.method === 'PUT' && r.path === '/admin/diary')
        expect(put.body.id).toBe(9201)
        expect(put.body.content).toBe(edited)
        expect(put.body.richText).toBe(edited)
        expect(put.body.mood).toBe('x-gaoxing')
        await expect(page).toHaveURL(/\/m\/diary$/)

        // 删除：先进编辑页，先取消再确认
        await entry.click()
        await expect(page).toHaveURL(/\/m\/diary\/edit\?id=9201$/)
        const delBtn = page.locator('.danger-zone .van-button', { hasText: '删除这篇日记' })
        await delBtn.click()
        await expect(page.locator('.van-dialog')).toContainText('确定删除这篇日记吗？')
        await page.locator('.van-dialog__cancel').click()
        await expect(page.locator('.van-dialog')).toBeHidden()

        await delBtn.click()
        await expect(page.locator('.van-dialog')).toBeVisible()
        await page.locator('.van-dialog__confirm').click()
        await waitForRequest(TOKEN, r => r.method === 'DELETE' && r.path === '/admin/diary/9201')
        await expect(page).toHaveURL(/\/m\/diary$/)
    })

    test('T10 心情分析：分布条形图 / 本月-全部切换 / AI 洞察流式', async ({ page }) => {
        await loginAs(page, TOKEN)
        await page.goto('/m/diary/analysis')

        // 本月（fixtures）：统计卡 + 分布条形图
        await expect(page.locator('.analysis-header__subtitle')).toHaveText('本月日记')
        const statValue = label => page.locator('.stats-item', { hasText: label }).locator('.stats-item__value')
        await expect(statValue('日记总数')).toHaveText('6')
        await expect(statValue('心情种类')).toHaveText('2')
        await expect(statValue('主导心情')).toHaveText('gaoxing')
        await expect(page.locator('.mood-bar-row')).toHaveCount(2)
        await expect(page.locator('.mood-bar-row').first().locator('.mood-bar-row__label')).toHaveText('gaoxing')
        await expect(page.locator('.mood-bar-row').first().locator('.mood-bar-row__count')).toHaveText('4')
        await expect(page.locator('.mood-bar-row').first().locator('.mood-bar-row__fill')).toHaveAttribute('style', /width:\s*100%/)
        await expect(page.locator('.mood-bar-row').nth(1).locator('.mood-bar-row__fill')).toHaveAttribute('style', /width:\s*50%/)
        await expect(page.locator('.trend-item')).toHaveCount(2)
        await expect(page.locator('.timeline-item')).toHaveCount(2)

        // 切「全部」：数字变化 + isAll=true 查询
        await ov('GET', '/diary/analysis/data', {
            data: {
                total: 9,
                moodDistribution: [
                    { name: 'x-gaoxing', value: 5 },
                    { name: 'x-ku', value: 3 },
                    { name: 'x-jingya', value: 1 }
                ],
                timeline: [
                    { date: '2026-11-14', mood: 'x-gaoxing', weather: 'w-qing', title: '周末爬山', contentPreview: '山顶风景很好' },
                    { date: '2026-11-12', mood: 'x-ku', weather: 'w-yin', title: '加班的一天', contentPreview: '改了三版方案' },
                    { date: '2026-10-20', mood: 'x-jingya', weather: 'w-duoyun', title: '老友重逢', contentPreview: '十年没见' }
                ]
            }
        })
        await page.locator('.range-switch__item', { hasText: '全部' }).click()
        await waitForRequest(TOKEN, r => r.path === '/diary/analysis/data' && r.query.isAll === 'true')
        await expect(page.locator('.analysis-header__subtitle')).toHaveText('全部日记')
        await expect(statValue('日记总数')).toHaveText('9')
        await expect(statValue('心情种类')).toHaveText('3')
        await expect(page.locator('.mood-bar-row')).toHaveCount(3)

        // AI 洞察：setStream 覆写时序，流式逐段出现
        await setStream(TOKEN, '/diary/analysis/stream', [
            { data: '本月整体心情平稳', delay: 0 },
            { data: '，建议早睡早起', delay: 900 },
            { data: '。', delay: 900 },
            { data: '[DONE]', delay: 0 }
        ])
        await page.locator('.ai-run-btn').click()
        await expect(page.locator('.ai-result')).toContainText('本月整体心情平稳')
        await expect(page.locator('.ai-result')).not.toContainText('建议早睡早起') // 仍在流式输出中
        await expect(page.locator('.ai-result__cursor')).toBeVisible()
        await expect(page.locator('.ai-result')).toContainText('本月整体心情平稳，建议早睡早起。')
        await expect(page.locator('.ai-result__cursor')).toBeHidden()
        await expect(page.locator('.ai-run-btn')).toHaveText('重新分析')
        await waitForRequest(TOKEN, r => r.path === '/diary/analysis/stream' && r.query.isAll === 'true')
    })
})
