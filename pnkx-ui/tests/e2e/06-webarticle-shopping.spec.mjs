import { test, expect, skipUnlessProject } from './helpers/setup.mjs'
import { TOKENS, loginAs, clearForceMode, overrideRoute, clearOverride, getRequests, waitForRequest, clearRequests } from './helpers/mock.mjs'

/**
 * 网址收藏 / RSS / 购物清单（W1-W10）
 * 专属 token：test-token-ws（V2 全关）；W9/W10 用 TOKENS.SP2（shoppingPlanV2 开）
 * Mock Server 静态 fixture 不感知 query（状态/分类/标签筛选、清单条目按 listId 过滤），
 * 差异化数据统一用 overrideRoute 按 token 覆写。
 */
const T = 'test-token-ws'
const SP2 = TOKENS.SP2

const ARTICLES = [
    { id: 61, title: 'Vue3 组合式 API 实战', url: 'https://example.com/vue3-guide', category: '技术', domain: 'example.com', tags: 'vue,前端', summary: '组合式 API 的核心概念与实战技巧。', starFlag: '1', readFlag: '0', fetchStatus: '1', aiStatus: '1', createTime: '2026-11-14 09:30:00' },
    { id: 62, title: '本周科技资讯汇总', url: 'https://news.example.com/weekly', category: '资讯', domain: 'news.example.com', tags: '新闻', summary: 'AI 与芯片行业动态。', starFlag: '0', readFlag: '1', fetchStatus: '1', aiStatus: '1', createTime: '2026-11-13 18:00:00' },
    { id: 63, title: '自动化工具推荐', url: 'https://tools.example.com/top', category: '工具', domain: 'tools.example.com', tags: '', summary: '', starFlag: '0', readFlag: '0', fetchStatus: '2', aiStatus: '2', fetchError: '目标站点返回 403', createTime: '2026-11-12 12:00:00' },
    { id: 64, title: '周末露营清单', url: 'https://life.example.com/camping', category: '生活', domain: 'life.example.com', tags: '生活', summary: '装备与路线整理。', starFlag: '0', readFlag: '1', fetchStatus: '1', aiStatus: '1', createTime: '2026-11-11 08:00:00' }
]

const setArticles = rows => overrideRoute(T, 'GET', '/webArticle/list', { rows, total: rows.length })
const setItems = rows => overrideRoute(SP2, 'GET', '/shoppingItem/list', { rows, total: rows.length })

test.describe('网址收藏与购物清单', () => {
    test.beforeEach(skipUnlessProject('mobile'))

    test('W1 收藏列表状态 Tab/分类/标签筛选组合', async ({ page }) => {
        await loginAs(page, T)
        await clearForceMode(page)
        await clearOverride(T, 'GET', '/webArticle/list')
        await page.goto('/m/web-article')

        // 全量渲染 + 标签 chips + 抓取失败态
        await expect(page.locator('.m-article-card')).toHaveCount(4)
        await expect(page.locator('.m-article-tabs__item--tag', { hasText: 'vue' })).toBeVisible()
        await expect(page.locator('.m-article-card', { hasText: '自动化工具推荐' }).locator('.error')).toHaveText('抓取失败')
        await expect(page.locator('.m-article-card__title.unread', { hasText: 'Vue3 组合式 API 实战' })).toBeVisible()

        // 未读 Tab
        await setArticles([ARTICLES[0], ARTICLES[2]])
        await page.locator('.m-article-tabs__item', { hasText: '未读' }).click()
        await waitForRequest(T, r => r.path === '/webArticle/list' && r.query.readFlag === '0')
        await expect(page.locator('.m-article-card')).toHaveCount(2)
        await expect(page.locator('.m-article-card__title', { hasText: '本周科技资讯汇总' })).toHaveCount(0)

        // 星标 Tab（与未读互斥）
        await setArticles([ARTICLES[0]])
        await page.locator('.m-article-tabs__item', { hasText: '星标' }).click()
        await waitForRequest(T, r => r.path === '/webArticle/list' && r.query.starFlag === '1')
        await expect(page.locator('.m-article-card')).toHaveCount(1)

        // 分类 chip：技术（与星标组合）
        await page.locator('.m-article-tabs__item', { hasText: '技术' }).click()
        await waitForRequest(T, r => r.path === '/webArticle/list' && r.query.starFlag === '1' && r.query.category === '技术')
        await expect(page.locator('.m-article-card')).toHaveCount(1)

        // 标签 chip：# vue（三者组合）
        await page.locator('.m-article-tabs__item--tag', { hasText: 'vue' }).click()
        await waitForRequest(T, r => r.path === '/webArticle/list' && r.query.starFlag === '1' && r.query.category === '技术' && r.query.tag === 'vue')
        await expect(page.locator('.m-article-card')).toHaveCount(1)
        await expect(page.locator('.m-article-card__title', { hasText: 'Vue3 组合式 API 实战' })).toBeVisible()
    })

    test('W2 FAB 粘贴网址收藏 → 抓取轮询 0→1 → 列表出现', async ({ page }) => {
        await loginAs(page, T)
        const pending = { id: 65, title: '示例文章标题', url: 'https://example.com/new-post', category: '技术', domain: 'example.com', tags: '', summary: '', starFlag: '0', readFlag: '0', fetchStatus: '0', aiStatus: '0', createTime: '2026-11-15 10:00:00' }
        const done = { ...pending, fetchStatus: '1', aiStatus: '1', summary: '文章摘要完成。' }
        await setArticles([...ARTICLES, pending])
        await page.goto('/m/web-article')
        await expect(page.locator('.m-article-card')).toHaveCount(5)

        // FAB → 收藏网址 → 粘贴链接 → 收藏
        await page.locator('button.m-fab').click()
        await page.locator('.van-action-sheet__item', { hasText: '收藏网址' }).click()
        await page.getByPlaceholder('粘贴网址链接，如 https://example.com/article').fill('https://example.com/new-post')
        await clearRequests(T)
        await page.locator('.m-article-add__actions .van-button', { hasText: '收藏' }).click()
        await waitForRequest(T, r => r.method === 'POST' && r.path === '/webArticle' && r.body.url === 'https://example.com/new-post')
        await expect(page.locator('.van-toast', { hasText: '已收藏，后台整理中' })).toBeVisible()

        // 新收藏出现且处于「抓取中…」
        const card = page.locator('.m-article-card', { hasText: '示例文章标题' })
        await expect(card).toBeVisible()
        await expect(card.locator('.pending')).toHaveText('抓取中…')

        // 抓取完成（fetchStatus 0→1）：页面 5s 轮询自动刷新为正常态
        await setArticles([...ARTICLES, done])
        await expect(card.locator('.pending')).toHaveCount(0, { timeout: 12000 })
        await expect(card).toContainText('文章摘要完成。')
    })

    test('W3 星标/已读切换（请求断言 + 状态即时生效）', async ({ page }) => {
        await loginAs(page, T)
        await clearOverride(T, 'GET', '/webArticle/list')
        await page.goto('/m/web-article')

        // 列表星标切换：62 未星标 → 点击星标 → PUT star + 图标点亮
        const star = page.locator('.m-article-card', { hasText: '本周科技资讯汇总' }).locator('.m-article-card__star')
        await expect(star).not.toHaveClass(/stared/)
        await star.click()
        await waitForRequest(T, r => r.method === 'PUT' && r.path === '/webArticle/star/62/1')
        await expect(star).toHaveClass(/stared/)
        await expect(page.locator('.van-toast', { hasText: '已星标' })).toBeVisible()

        // 阅读页已读切换：已读 → 未读
        await page.locator('.m-article-card', { hasText: '本周科技资讯汇总' }).click()
        await expect(page).toHaveURL(/web-article\/detail\?id=62/)
        await expect(page.locator('.article-head__title')).toHaveText('本周科技资讯汇总')
        const readLabel = page.locator('.article-action-bar__item', { hasText: '已读' }).locator('.article-action-bar__label')
        await expect(readLabel).toHaveText('已读')
        await page.locator('.article-action-bar__item', { hasText: '已读' }).click()
        await waitForRequest(T, r => r.method === 'PUT' && r.path === '/webArticle/read/62/0')
        await expect(page.locator('.article-action-bar__item').nth(3).locator('.article-action-bar__label')).toHaveText('未读')
    })

    test('W4 阅读页 AI 摘要卡 + 正文 DOMPurify + 打开原文', async ({ page }) => {
        await loginAs(page, T)
        // 拦截 window.open，避免真实弹窗/外网请求
        await page.addInitScript(() => {
            window.__openedUrls = []
            window.open = url => { window.__openedUrls.push(String(url)); return null }
        })
        await page.goto('/m/web-article/detail?id=61')

        // AI 摘要卡
        await expect(page.locator('.article-summary__title')).toHaveText('AI 摘要')
        await expect(page.locator('.article-summary__text')).toContainText('组合式 API')
        // 标签
        await expect(page.locator('.article-head__tag').first()).toHaveText('vue')

        // 正文渲染：段落可见；script 被清除且未执行；onerror 属性被剥离
        await expect(page.locator('.article-content__html p').first()).toHaveText('正文第一段：响应式基础。')
        await expect(page.locator('.article-content__html script')).toHaveCount(0)
        const xss = await page.evaluate(() => ({
            script: typeof window.__xss_script,
            attr: typeof window.__xss_attr,
            onerror: document.querySelector('.article-content__html img')?.getAttribute('onerror')
        }))
        expect(xss.script).toBe('undefined')
        expect(xss.attr).toBe('undefined')
        expect(xss.onerror).toBeNull()

        // 打开原文 → window.open 收到文章 url
        await page.locator('.article-action-bar__primary', { hasText: '打开原文' }).click()
        const opened = await page.evaluate(() => window.__openedUrls)
        expect(opened).toEqual(['https://example.com/vue3-guide'])
    })

    test('W5 编辑收藏保存', async ({ page }) => {
        await loginAs(page, T)
        await page.goto('/m/web-article/edit?id=61')

        // 回填
        await expect(page.locator('.van-field input[type="text"]').first()).toHaveValue('Vue3 组合式 API 实战')
        // 修改摘要/标签 + 切换分类 chip
        await page.getByPlaceholder('摘要（可修正 AI 生成结果）').fill('修订后的摘要。')
        await page.getByPlaceholder('多个标签用英文逗号分隔').fill('vue,进阶')
        await page.locator('.m-article-edit__chip', { hasText: '生活' }).click()
        await expect(page.locator('.m-article-edit__chip.active')).toHaveText('生活')

        await page.locator('.m-sticky-actions .van-button', { hasText: '保存' }).click()
        await waitForRequest(T, r => r.method === 'PUT' && r.path === '/webArticle'
            && r.body.id === 61 && r.body.summary === '修订后的摘要。' && r.body.tags === 'vue,进阶' && r.body.category === '生活')
        await expect(page.locator('.van-toast', { hasText: '已保存' })).toBeVisible()
    })

    test('W6 RSS 列表/启停/立即抓取/新增/删除', async ({ page }) => {
        await loginAs(page, T)
        await page.goto('/m/web-article/rss')

        // 列表：启停状态 + 失败原因
        const feed1 = page.locator('.m-rss-card', { hasText: '少数派' })
        const feed2 = page.locator('.m-rss-card', { hasText: '阮一峰周刊' })
        await expect(feed1.locator('.m-rss-card__status.on')).toHaveText('启用')
        await expect(feed1).toContainText('最近抓取：11-14 08:00')
        await expect(feed1).toContainText('+3 篇')
        await expect(feed2.locator('.m-rss-card__status.off')).toHaveText('停用')
        await expect(feed2.locator('.m-rss-card__error')).toContainText('抓取失败：DNS 解析失败')

        // 停用 → PUT feed
        await clearRequests(T)
        await feed1.locator('.m-rss-chip', { hasText: '停用' }).click()
        await waitForRequest(T, r => r.method === 'PUT' && r.path === '/webArticle/feed'
            && r.body.id === 1 && r.body.enabled === '0')

        // 立即抓取 → PUT fetch + toast
        await feed1.locator('.m-rss-chip', { hasText: '立即抓取' }).click()
        await waitForRequest(T, r => r.method === 'PUT' && r.path === '/webArticle/feed/fetch/1')
        await expect(page.locator('.van-toast', { hasText: '新收录 2 篇' })).toBeVisible()

        // 新增订阅 → POST feed（name 由 url 派生）
        await page.locator('button.m-fab').click()
        await page.getByPlaceholder('RSS/Atom 链接，如 https://sspai.com/feed').fill('https://example.com/rss')
        await page.locator('.m-rss-add__actions .van-button', { hasText: '添加' }).click()
        await waitForRequest(T, r => r.method === 'POST' && r.path === '/webArticle/feed'
            && r.body.url === 'https://example.com/rss' && r.body.name === 'example.com/rss')
        await expect(page.locator('.van-toast', { hasText: '已添加' })).toBeVisible()

        // 删除订阅：确认弹窗 → DELETE
        await feed2.locator('.m-rss-chip', { hasText: '删除' }).click()
        await expect(page.locator('.van-dialog')).toContainText('删除「阮一峰周刊」')
        await page.locator('.van-dialog__confirm').click()
        await waitForRequest(T, r => r.method === 'DELETE' && r.path === '/webArticle/feed/2')
    })

    test('W7 购物清单进度/新建清单/采购日期', async ({ page }) => {
        await loginAs(page, T)
        await clearForceMode(page)
        await clearOverride(T, 'GET', '/shoppingList/list')
        await clearOverride(T, 'GET', '/shoppingItem/list')
        await page.goto('/m/shopping-list')

        // 进度 done/total（home fixtures：条目 111 未勾选 / 112 已勾选）
        const card = page.locator('.list-card', { hasText: '本周采购' })
        await expect(card.locator('.list-card__progress-text')).toHaveText('1/2')
        await expect(card.locator('.list-card__progress-fill')).not.toHaveCSS('width', '0px')

        // 新建清单 → POST 断言
        await page.locator('button.m-fab').click()
        await expect(page.locator('.van-dialog__header')).toHaveText('新建购物清单')
        await page.getByPlaceholder('请输入清单名称').fill('周末大采购')
        await clearRequests(T)
        await page.locator('.van-dialog__confirm').click()
        await waitForRequest(T, r => r.method === 'POST' && r.path === '/shoppingList'
            && r.body.name === '周末大采购' && r.body.icon === '🛒')
        await expect(page.locator('.van-toast', { hasText: '已创建' })).toBeVisible()

        // 覆写清单数据模拟入库 → 刷新出现
        await overrideRoute(T, 'GET', '/shoppingList/list', {
            rows: [
                { id: 11, name: '本周采购', icon: 'gouwu', done: 1, total: 2, shoppingDate: '2026-11-16', createTime: '2026-11-10 09:00:00' },
                { id: 12, name: '家庭常备', icon: 'gouwu', done: 0, total: 2, shoppingDate: null, createTime: '2026-11-11 09:00:00' },
                { id: 13, name: '周末大采购', icon: '🛒', createTime: '2026-11-15 10:00:00' }
            ],
            total: 3
        })
        await page.reload()
        await expect(page.locator('.list-card', { hasText: '周末大采购' })).toBeVisible()

        // 设置采购日期：date picker 确认 → 保存 → PUT 断言
        await page.locator('.list-card', { hasText: '家庭常备' }).locator('.list-card__plan').click()
        await expect(page.locator('.schedule__title')).toHaveText('采购计划')
        await page.locator('.van-picker__confirm').click()
        await page.locator('.schedule__actions .van-button', { hasText: '保存' }).click()
        const req = await waitForRequest(T, r => r.method === 'PUT' && r.path === '/shoppingList' && r.body.id === 12)
        expect(req.body.plannedDate).toMatch(/^\d{4}-\d{2}-\d{2}$/)
        await expect(page.locator('.van-toast', { hasText: '计划已更新' })).toBeVisible()
    })

    test('W8 清单详情四分组/勾选移动分组/清空已勾选', async ({ page }) => {
        await loginAs(page, T)
        await overrideRoute(T, 'GET', '/shoppingItem/list', {
            rows: [
                { id: 301, listId: 11, name: '牛奶', quantity: '2盒', checked: false, fulfillmentState: 'needed' },
                { id: 302, listId: 11, name: '纸巾', quantity: '', checked: false, fulfillmentState: 'available' },
                { id: 303, listId: 11, name: '鸡蛋', quantity: '1打', checked: true, fulfillmentState: 'purchased' },
                { id: 304, listId: 11, name: '啤酒', quantity: '', checked: true, fulfillmentState: 'purchased', batchId: 9 }
            ],
            total: 4
        })
        await page.goto('/m/shopping-list/detail?id=11&name=' + encodeURIComponent('本周采购'))

        // 四分组渲染 + 计数
        const groupCount = label => page.locator('.group-head', { hasText: label }).locator('.group-count')
        await expect(page.locator('.group-title', { hasText: '待购' })).toBeVisible()
        await expect(groupCount('待购')).toHaveText('1')
        await expect(groupCount('家里已有')).toHaveText('1')
        await expect(groupCount('已买到')).toHaveText('1')
        await expect(groupCount('已结算')).toHaveText('1')

        // 已结算默认折叠，展开后可见
        await expect(page.locator('.item__name', { hasText: '啤酒' })).toHaveCount(0)
        await page.locator('.group-toggle', { hasText: '展开' }).click()
        await expect(page.locator('.item__name', { hasText: '啤酒' })).toBeVisible()

        // 勾选待购项「牛奶」→ 请求断言 + 移动到「已买到」分组
        await page.locator('.item', { hasText: '牛奶' }).locator('.item__check').click()
        await waitForRequest(T, r => r.method === 'PUT' && r.path === '/shoppingItem'
            && r.body.id === 301 && r.body.checked === true)
        await expect(groupCount('已买到')).toHaveText('2')
        // 待购组清空后整组隐藏（visibleGroups 过滤空组）
        await expect(page.locator('.group-title', { hasText: '待购' })).toHaveCount(0)

        // 清空已勾选：底部统计 + 确认弹窗 + DELETE
        await expect(page.locator('.shopping-detail__footer-count')).toHaveText('已完成 3/4')
        await page.locator('.shopping-detail__footer-btn', { hasText: '清空已勾选' }).click()
        await expect(page.locator('.van-dialog')).toContainText('确认清空所有已勾选的项目？')
        await page.locator('.van-dialog__confirm').click()
        await waitForRequest(T, r => r.method === 'DELETE' && r.path === '/shoppingItem/clearChecked/11')
    })

    test('W9【SP2】结算面板多选 → checkout 请求断言', async ({ page }) => {
        await loginAs(page, SP2)
        await clearForceMode(page)
        await setItems([
            { id: 301, listId: 11, name: '牛奶', quantity: '2盒', checked: false, fulfillmentState: 'needed' },
            { id: 303, listId: 11, name: '鸡蛋', quantity: '1打', checked: true, fulfillmentState: 'purchased' },
            { id: 305, listId: 11, name: '酸奶', quantity: '6杯', checked: true, fulfillmentState: 'purchased' }
        ])
        await page.goto('/m/shopping-list/detail?id=11&name=' + encodeURIComponent('本周采购'))

        // 结算面板（shoppingPlanV2）
        await expect(page.locator('.checkout-panel__bar')).toBeVisible()
        await expect(page.locator('.checkout-panel__hint').first()).toHaveText('2 项已买到、待结算')

        // 已买到条目进入结算选择：勾选鸡蛋 → 底部「已选 1 项」→ 去结算
        await page.locator('.item', { hasText: '鸡蛋' }).locator('.item__check').click()
        await expect(page.locator('.shopping-detail__footer-count')).toHaveText('已选 1 项')
        await page.locator('.shopping-detail__footer-btn', { hasText: '去结算' }).click()

        // 结算弹层：预选 1 项 → 再补选酸奶 → 确认结算
        await expect(page.locator('.checkout__title')).toHaveText('核对本次采购')
        await expect(page.locator('.checkout__section-head', { hasText: '已选 1 项' })).toBeVisible()
        await page.locator('.checkout__item', { hasText: '酸奶' }).click()
        await expect(page.locator('.checkout__section-head', { hasText: '已选 2 项' })).toBeVisible()
        await page.locator('.checkout__footer-actions .van-button', { hasText: '确认结算' }).click()

        const req = await waitForRequest(SP2, r => r.method === 'POST' && r.path === '/shoppingList/11/checkout')
        expect(req.body.purchasedItemIds).toEqual([303, 305])
        expect(req.body.recordExpense).toBe(false)
        expect(typeof req.body.requestId).toBe('string')
        await expect(page.locator('.checkout-panel__success')).toContainText('本次采购已结算')
    })

    test('W10【SP2】条目认领/释放/转派', async ({ page }) => {
        await loginAs(page, SP2)
        await setItems([
            { id: 301, listId: 11, name: '牛奶', quantity: '2盒', checked: false, fulfillmentState: 'needed', revision: 3 },
            { id: 303, listId: 11, name: '鸡蛋', quantity: '1打', checked: true, fulfillmentState: 'purchased', revision: 3 }
        ])
        await page.goto('/m/shopping-list/detail?id=11&name=' + encodeURIComponent('本周采购'))

        // 认领 → POST claim（带 expectedRevision + requestId 幂等键）
        const milk = page.locator('.item', { hasText: '牛奶' })
        await expect(milk.locator('.item-actions')).toBeVisible()
        await milk.locator('.item-actions__buttons .van-button', { hasText: '认领' }).click()
        const claimReq = await waitForRequest(SP2, r => r.method === 'POST' && r.path === '/shoppingItem/301/claim')
        expect(claimReq.body.expectedRevision).toBe(3)
        expect(typeof claimReq.body.requestId).toBe('string')

        // 覆写为已认领数据 → 刷新 → 显示认领人 + 释放按钮
        await setItems([
            { id: 301, listId: 11, name: '牛奶', quantity: '2盒', checked: false, fulfillmentState: 'needed', revision: 4, claimedBy: '测试用户' },
            { id: 303, listId: 11, name: '鸡蛋', quantity: '1打', checked: true, fulfillmentState: 'purchased', revision: 3 }
        ])
        await page.reload()
        const milk2 = page.locator('.item', { hasText: '牛奶' })
        await expect(milk2.locator('.item-actions__claim')).toHaveText('· 认领人 测试用户')

        // 释放 → POST release
        await milk2.locator('.item-actions__buttons .van-button', { hasText: '释放' }).click()
        await waitForRequest(SP2, r => r.method === 'POST' && r.path === '/shoppingItem/301/release')

        // 转派：展开分工面板 → 选成员 → 填原因 → 确认转派
        await milk2.locator('.m-assignment__header').click()
        await expect(milk2.locator('.m-assignment__value')).toHaveText('成员 1')
        await milk2.locator('.m-assignment__candidate', { hasText: '成员 2' }).click()
        await milk2.getByPlaceholder('填写转派原因（必填）').fill('本周出差')
        await milk2.locator('.m-assignment .van-button', { hasText: '确认转派' }).click()
        const transferReq = await waitForRequest(SP2, r => r.method === 'POST' && r.path === '/life/assignments/shopping/301/transfer')
        expect(transferReq.body).toMatchObject({ assignee: '2', expectedAssignee: '1', reason: '本周出差' })
        expect(typeof transferReq.body.requestId).toBe('string')
        // 转派成功后面板刷新为新负责人
        await expect(milk2.locator('.m-assignment__value')).toHaveText('成员 2')
    })
})
