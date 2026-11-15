import { test, expect, skipUnlessProject } from './helpers/setup.mjs'
import { TOKENS, loginAs, clearForceMode, overrideRoute, clearOverride, setStream, waitForRequest, clearRequests } from './helpers/mock.mjs'

/**
 * AI 对话（流式 SSE / 操作计划 / 草稿编辑 / 待确认动作 / 会话管理）
 * 专属 token：test-token-ai（V2 全关）；A6 会话+操作计划面板用 TOKENS.OP2
 *
 * 备注：chatStreamParser 的 operation-plan 事件要求 planId 形如 UUID
 * （^[0-9a-f]{8}-[0-9a-f-]{27}$），因此 fixtures 使用 aabbcc01-0123456789abcdef0123456789a。
 */
const AI = 'test-token-ai'
const PLAN_ID = 'aabbcc01-0123456789abcdef0123456789a'

// 同 token 的请求日志跨用例持久，先清空避免 waitForRequest 命中上一条用例的请求
test.beforeEach(async () => {
    await clearRequests(AI)
    await clearRequests(TOKENS.OP2)
})

/** 把一条消息的 SSE 流脚本注入 mock（chunks 见 tests/mock-server/sse.mjs） */
async function planStream(token, planId = PLAN_ID) {
    await setStream(token, '/ai/chat/stream', [
        { data: '好的，已为你准备好操作计划。', delay: 100 },
        { event: 'operation-plan', data: JSON.stringify({ planId }), delay: 150 },
        { data: '[DONE]', delay: 0 }
    ])
}

async function sendMessage(page, text) {
    await page.locator('.chat-input').fill(text)
    await page.locator('.send-btn').click()
}

test.describe('AI 对话', () => {
    test.beforeEach(skipUnlessProject('mobile'))

    test('A1 欢迎态与快捷提示词 chips', async ({ page }) => {
        await loginAs(page, AI)
        await clearForceMode(page)
        await page.goto('/m/ai/chat')
        // 欢迎态（无历史会话）
        await expect(page.locator('.welcome')).toBeVisible({ timeout: 8000 })
        await expect(page.locator('.welcome__title')).toHaveText('你好，有什么可以帮你的？')
        // 4 个快捷提示词 chips
        const chips = page.locator('.welcome__prompt')
        await expect(chips).toHaveCount(4)
        await expect(page.locator('.welcome__prompt-text', { hasText: '午餐花了30块' })).toBeVisible()
        await expect(page.locator('.welcome__prompt-text', { hasText: '提醒我明天开会' })).toBeVisible()
        await expect(page.locator('.welcome__prompt-text', { hasText: '分析本月消费' })).toBeVisible()
    })

    test('A2 发送消息 SSE 流式逐字渲染并完成 markdown', async ({ page }) => {
        await loginAs(page, AI)
        await clearForceMode(page)
        // 注：SSE data 帧不能包含 \n（会提前分帧），用 \r 做换行，marked 会归一化为 \n
        await setStream(AI, '/ai/chat/stream', [
            { data: '### 本周生活小结', delay: 200 },
            { data: '\r\r- 支出 25.5 元，共 1 笔', delay: 260 },
            { data: '\r- 待办完成 1 项', delay: 260 },
            { data: '\r\r**建议**：保持节奏。', delay: 220 },
            { data: '[DONE]', delay: 0 }
        ])
        await page.goto('/m/ai/chat')
        await expect(page.locator('.chat-toolbar__btn', { hasText: '新建会话' })).toBeVisible()

        await sendMessage(page, '给我一份本周总结')
        // 用户消息入列
        await expect(page.locator('.message.user .message__content--user')).toHaveText('给我一份本周总结')
        // 流式渲染：采样至少 2 次增量后出现完整 markdown
        const bubble = page.locator('.message.assistant .message__content').last()
        await expect(bubble).toBeVisible()
        const snapshots = []
        const deadline = Date.now() + 15000
        while (Date.now() < deadline) {
            snapshots.push((await bubble.textContent()) || '')
            if ((await page.locator('.typing-cursor').count()) === 0 && snapshots[snapshots.length - 1].includes('建议')) break
            await page.waitForTimeout(70)
        }
        const distinct = [...new Set(snapshots.map(s => s.trim()))].filter(Boolean)
        expect(distinct.length, `应有至少 2 次增量，实际采样：${distinct.join(' | ')}`).toBeGreaterThanOrEqual(3)
        // 完成 markdown 渲染（标题 / 列表 / 加粗）
        await expect(page.locator('.message__content h3', { hasText: '本周生活小结' })).toBeVisible()
        await expect(page.locator('.message__content li')).toHaveCount(2)
        await expect(page.locator('.message__content strong', { hasText: '建议' })).toBeVisible()
    })

    test('A3 SSE operation-plan 事件渲染 ChatPlanCard', async ({ page }) => {
        await loginAs(page, AI)
        await clearForceMode(page)
        await planStream(AI)
        await page.goto('/m/ai/chat')
        await sendMessage(page, '帮我记一笔午餐 25.5 元')
        // 计划卡出现并请求了计划详情
        const card = page.locator('.plan-card')
        await expect(card).toBeVisible({ timeout: 8000 })
        await waitForRequest(AI, r => r.method === 'GET' && r.path === '/operation-plans/' + PLAN_ID)
        await expect(card.locator('.plan-card__title')).toHaveText('记一笔午餐支出')
        await expect(card.locator('.plan-card__badge')).toHaveText('待确认')
        await expect(card.locator('.plan-card__step-title')).toContainText('AI记账')
        await expect(card.locator('.plan-card__field', { hasText: '金额（元）' })).toContainText('25.5')
        await expect(card.getByRole('button', { name: '确认执行以上内容' })).toBeVisible()
    })

    test('A4 PlanDraftEditor 草稿编辑（boolean 开关 / 日期 / 多行文本）', async ({ page }) => {
        await loginAs(page, AI)
        await clearForceMode(page)
        const today = new Date()
        const pad = n => String(n).padStart(2, '0')
        const todayStr = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`

        // 流程 1：commemoration.create —— boolean 开关 + 日期
        const commemPlan = {
            data: {
                id: PLAN_ID,
                title: '创建生日提醒',
                status: 'awaiting_confirmation',
                revision: 2,
                steps: [{
                    step_number: 1,
                    tool_code: 'commemoration.create',
                    status: 'pending',
                    input_json: JSON.stringify({ name: '生日提醒', date: '', repeat: false })
                }]
            }
        }
        await overrideRoute(AI, 'GET', '/operation-plans/:id', commemPlan)
        await overrideRoute(AI, 'POST', '/operation-plans/:id/revise', {
            data: {
                id: PLAN_ID,
                title: '创建生日提醒',
                status: 'awaiting_confirmation',
                revision: 3,
                steps: [{
                    step_number: 1,
                    tool_code: 'commemoration.create',
                    status: 'pending',
                    input_json: JSON.stringify({ name: '生日聚会提醒', date: todayStr, repeat: true })
                }]
            }
        })
        await planStream(AI)
        await page.goto('/m/ai/chat')
        await sendMessage(page, '帮我创建生日提醒')
        const card1 = page.locator('.plan-card').last()
        await expect(card1).toBeVisible({ timeout: 8000 })
        await card1.getByRole('button', { name: '修改草稿' }).click()
        // boolean 开关：每年重复
        const switchEl = card1.locator('.draft-editor .van-switch').first()
        await switchEl.click()
        await expect(switchEl).toHaveClass(/van-switch--on/)
        // 日期：打开 picker 后确认（默认今天）
        await card1.locator('.draft-editor__field', { hasText: '日期' }).locator('.draft-editor__input').click()
        await expect(page.locator('.draft-editor__picker')).toBeVisible()
        await page.locator('.draft-editor__picker-actions').getByRole('button', { name: '确定' }).click()
        // 文本：名称
        await card1.locator('.draft-editor__field', { hasText: '纪念日名称' }).locator('input').fill('生日聚会提醒')
        await card1.getByRole('button', { name: '保存并重新预览' }).click()
        const revise1 = await waitForRequest(AI, r => r.method === 'POST' && r.path.includes('/operation-plans/') && r.path.endsWith('/revise'))
        expect(revise1.body).toMatchObject({
            expectedRevision: 2,
            fields: { name: '生日聚会提醒', date: todayStr, repeat: true }
        })
        // 保存后卡片按新草稿渲染（重复规则 = 每年重复）
        await expect(card1).toContainText('生日聚会提醒')
        await expect(card1).toContainText('每年重复')

        // 流程 2：todo.create —— 多行文本
        await overrideRoute(AI, 'GET', '/operation-plans/:id', {
            data: {
                id: PLAN_ID,
                title: '创建待办',
                status: 'awaiting_confirmation',
                revision: 1,
                steps: [{
                    step_number: 1,
                    tool_code: 'todo.create',
                    status: 'pending',
                    input_json: JSON.stringify({ content: '预约体检', label: '健康' })
                }]
            }
        })
        await planStream(AI)
        await sendMessage(page, '提醒我预约体检')
        const card2 = page.locator('.plan-card').last()
        await expect(card2).toBeVisible({ timeout: 8000 })
        await expect(card2.locator('.plan-card__step-title')).toContainText('创建待办')
        await card2.getByRole('button', { name: '修改草稿' }).click()
        await card2.locator('.draft-editor__field', { hasText: '待办内容' }).locator('textarea').fill('预约周六上午体检，需空腹')
        await card2.getByRole('button', { name: '保存并重新预览' }).click()
        const revise2 = await waitForRequest(
            AI,
            r => r.method === 'POST' && r.path.endsWith('/revise') && r.body && r.body.fields && r.body.fields.content === '预约周六上午体检，需空腹'
        )
        expect(revise2.body.fields.label).toBe('健康')
        await clearOverride(AI, 'GET', '/operation-plans/:id')
        await clearOverride(AI, 'POST', '/operation-plans/:id/revise')
    })

    test('A5 pending-action 双按钮分别调用 confirm / cancel', async ({ page }) => {
        await loginAs(page, AI)
        await clearForceMode(page)
        const pendingReply = [
            { data: '已为你生成记账草稿：午餐 25.5 元（分类：午餐）。', delay: 100 },
            { data: '回复“确认”保存，回复“取消”放弃。', delay: 160 },
            { data: '[DONE]', delay: 0 }
        ]
        await setStream(AI, '/ai/chat/stream', pendingReply)
        await setStream(AI, '/ai/pending/confirm', [
            { data: '已保存记账记录。', delay: 120 },
            { data: '[DONE]', delay: 0 }
        ])
        await setStream(AI, '/ai/pending/cancel', [
            { data: '好的，草稿已丢弃。', delay: 120 },
            { data: '[DONE]', delay: 0 }
        ])
        await page.goto('/m/ai/chat')

        // 确认保存
        await sendMessage(page, '记一笔午餐 25.5 元')
        const actions = page.locator('.pending-actions')
        await expect(actions).toBeVisible({ timeout: 8000 })
        await expect(actions.locator('.pending-btn--confirm')).toHaveText('确认保存')
        await expect(actions.locator('.pending-btn', { hasText: '取消' })).toHaveText('取消')
        // 指令文案不进入气泡展示
        await expect(page.locator('.message.assistant .message__content').last()).toContainText('记账草稿')
        await actions.locator('.pending-btn--confirm').click()
        await waitForRequest(AI, r => r.method === 'POST' && r.path === '/ai/pending/confirm')
        await expect(page.locator('.message.assistant .message__content').last()).toContainText('已保存记账记录', { timeout: 8000 })

        // 取消
        await setStream(AI, '/ai/chat/stream', pendingReply)
        await sendMessage(page, '再记一笔打车 12 元')
        await expect(actions).toBeVisible({ timeout: 8000 })
        await actions.locator('.pending-btn', { hasText: '取消' }).click()
        await waitForRequest(AI, r => r.method === 'POST' && r.path === '/ai/pending/cancel')
        await expect(page.locator('.message.assistant .message__content').last()).toContainText('草稿已丢弃', { timeout: 8000 })
    })

    test('A6 会话管理：恢复历史 / 新建会话删除旧会话 / OperationPlans 面板', async ({ page }) => {
        const OP2 = TOKENS.OP2
        await loginAs(page, OP2)
        await clearForceMode(page)
        await overrideRoute(OP2, 'GET', '/ai/chat/sessions', {
            data: [{ sessionId: 'hist-001', title: '上周的会话', updateTime: '2026-11-14 10:00:00' }]
        })
        await overrideRoute(OP2, 'GET', '/ai/chat/messages', {
            data: [
                { id: 1, role: 'user', content: '帮我记一笔午餐' },
                { id: 2, role: 'assistant', content: '已生成草稿\n回复“确认”保存，回复“取消”放弃。' }
            ]
        })
        await page.goto('/m/ai/chat')
        // 恢复最近会话历史
        await expect(page.locator('.message.user .message__content--user')).toHaveText('帮我记一笔午餐', { timeout: 8000 })
        await expect(page.locator('.message.assistant .message__content').first()).toContainText('已生成草稿')
        // 历史中推导出的待确认状态同样出现
        await expect(page.locator('.pending-actions')).toBeVisible()

        // 新建会话：清空并删除服务端旧会话
        await clearRequests(OP2)
        await page.locator('.chat-toolbar__btn', { hasText: '新建会话' }).click()
        await expect(page.locator('.welcome')).toBeVisible()
        await waitForRequest(OP2, r => r.method === 'DELETE' && r.path.includes('/ai/chat/sessions/hist-001'))

        // operationPlanV2：操作计划面板
        await page.locator('.chat-toolbar__btn', { hasText: '操作计划与恢复' }).click()
        const plans = page.locator('.chat-plans .plans')
        await expect(plans).toBeVisible({ timeout: 8000 })
        await expect(plans.locator('.plans__row')).toHaveCount(2)
        await expect(plans.locator('.plans__filter', { hasText: '待处理' })).toContainText('1')
        await expect(plans.locator('.plans__title', { hasText: '记一笔午餐支出' })).toBeVisible()
        // 打开待确认计划并确认执行
        await plans.locator('.plans__view-button').first().click()
        const detail = plans.locator('.plans__detail')
        await expect(detail).toBeVisible({ timeout: 8000 })
        await expect(detail.locator('.plans__step')).toContainText('AI记账')
        await detail.getByRole('button', { name: '确认并执行' }).click()
        await waitForRequest(OP2, r => r.method === 'POST' && r.path === '/operation-plans/' + PLAN_ID + '/confirm')
        await expect(detail.locator('.plans__title-line').first().locator('.plans__badge')).toHaveText('已完成', { timeout: 8000 })
        await expect(detail).toContainText('已创建记录 #501')
        await clearOverride(OP2, 'GET', '/ai/chat/sessions')
        await clearOverride(OP2, 'GET', '/ai/chat/messages')
    })
})
