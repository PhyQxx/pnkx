import { test, expect, skipUnlessProject } from './helpers/setup.mjs'
import { loginAs, clearForceMode, overrideRoute, clearOverride, getRequests, waitForRequest, clearRequests } from './helpers/mock.mjs'

/**
 * 笔记 / Obsidian / 书城（N1-N9）
 * 专属 token：test-token-nb（V2 全关）
 * Mock Server 静态 fixture 不感知 query（如 parentId / pageNum），
 * 层级目录 / 翻页等差异化数据统一用 overrideRoute 按 token 覆写。
 */
const T = 'test-token-nb'

const ROOT_FOLDERS = [
    { id: 1, name: '工作', noteCount: 3, parentId: 0 },
    { id: 2, name: '生活', noteCount: 2, parentId: 0 }
]
const WORK_FOLDERS = [{ id: 11, name: '项目A', noteCount: 2, parentId: 1 }]
const ROOT_NOTES = [
    { id: 302, title: '购物灵感', content: '人体工学椅、升降桌。', folder: 0, updateTime: '2026-11-13 09:00:00' }
]
const WORK_NOTES = [
    { id: 301, title: '工作周报模板', content: '本周完成事项与下周计划。', folder: 1, folderName: '工作', updateTime: '2026-11-14 10:00:00' }
]
const PROJECT_NOTES = [
    { id: 311, title: '项目A 需求清单', content: '登录、支付、消息。', folder: 11, updateTime: '2026-11-12 15:00:00' }
]

const setFolders = rows => overrideRoute(T, 'GET', '/note/folder/list', { rows, total: rows.length })
const setNotes = rows => overrideRoute(T, 'GET', '/note/list', { rows, total: rows.length })

test.describe('笔记与书城', () => {
    test.beforeEach(skipUnlessProject('mobile'))

    test('N1 笔记面包屑逐级进出 + 子文件夹统计', async ({ page }) => {
        await loginAs(page, T)
        await clearForceMode(page)
        await page.goto('/m/note')

        // 根目录：占位面包屑 + 两个子文件夹 + 统计
        await expect(page.locator('.note-crumbs__placeholder')).toHaveText('全部笔记')
        await expect(page.locator('.note-folders__card', { hasText: '工作' })).toBeVisible()
        await expect(page.locator('.note-folders__card', { hasText: '生活' })).toBeVisible()
        await expect(page.locator('.note-folders__card', { hasText: '工作' }).locator('.note-folders__count')).toHaveText('3 篇笔记')
        await expect(page.locator('.m-stat-bar__item', { hasText: '子文件夹' }).locator('.m-stat-bar__value')).toHaveText('2')

        // 进入 工作：面包屑 全部 / 工作，子文件夹只剩 项目A
        await setFolders(WORK_FOLDERS)
        await setNotes(WORK_NOTES)
        await page.locator('.note-folders__card', { hasText: '工作' }).click()
        await expect(page.locator('.note-hero__title')).toHaveText('工作')
        await expect(page.locator('.note-crumbs__item')).toHaveCount(2)
        await expect(page.locator('.note-crumbs__item').nth(1)).toHaveText('工作')
        await expect(page.locator('.m-stat-bar__item', { hasText: '子文件夹' }).locator('.m-stat-bar__value')).toHaveText('1')
        await expect(page.locator('.note-folders__card', { hasText: '项目A' })).toBeVisible()
        await expect(page.locator('.note-card__title', { hasText: '工作周报模板' })).toBeVisible()

        // 进入 项目A：三级面包屑
        await setFolders([])
        await setNotes(PROJECT_NOTES)
        await page.locator('.note-folders__card', { hasText: '项目A' }).click()
        await expect(page.locator('.note-crumbs__item')).toHaveCount(3)
        await expect(page.locator('.note-crumbs__item').nth(2)).toHaveText('项目A')
        await expect(page.locator('.note-card__title', { hasText: '项目A 需求清单' })).toBeVisible()

        // 面包屑点击 工作：回退一级
        await setFolders(WORK_FOLDERS)
        await setNotes(WORK_NOTES)
        await page.locator('.note-crumbs__item', { hasText: '工作' }).click()
        await expect(page.locator('.note-crumbs__item')).toHaveCount(2)
        await expect(page.locator('.note-crumbs__item').nth(1)).toHaveText('工作')

        // 面包屑点击 全部：回根目录
        await setFolders(ROOT_FOLDERS)
        await setNotes(ROOT_NOTES)
        await page.locator('.note-crumbs__item', { hasText: '全部' }).first().click()
        await expect(page.locator('.note-crumbs__placeholder')).toHaveText('全部笔记')
        await expect(page.locator('.m-stat-bar__item', { hasText: '子文件夹' }).locator('.m-stat-bar__value')).toHaveText('2')
    })

    test('N2 新建笔记（选文件夹/正文）→ 列表出现 + 编辑保存', async ({ page }) => {
        await loginAs(page, T)
        await clearForceMode(page)
        await clearOverride(T, 'GET', '/note/list')
        await page.goto('/m/note')
        await expect(page.locator('.note-folders__card', { hasText: '工作' })).toBeVisible()

        // FAB → 新建笔记
        await page.locator('.note-fab-wrap .m-fab').click()
        await page.locator('.note-fab-menu__item', { hasText: '新建笔记' }).click()
        await expect(page).toHaveURL(/\/m\/note\/detail/)

        // 标题 + 选文件夹（项目A）+ 正文
        await page.getByPlaceholder('给这篇笔记起个名字').fill('晨间复盘')
        await page.locator('.van-cell', { hasText: '位置' }).click()
        await page.locator('.note-picker__item', { hasText: '项目A' }).click()
        await page.locator('.note-picker__confirm').click()
        await expect(page.locator('.van-cell', { hasText: '位置' }).locator('.van-cell__value')).toHaveText('项目A')
        await page.getByPlaceholder('开始记录...').fill('今天完成了 E2E 测试基建。')

        // 保存 → POST /note 断言
        await clearRequests(T)
        await page.locator('.m-sticky-actions .van-button', { hasText: '保存' }).click()
        await waitForRequest(T, r => r.method === 'POST' && r.path === '/note'
            && r.body.title === '晨间复盘' && r.body.content === '今天完成了 E2E 测试基建。' && r.body.folder === 11)

        // 保存后自动返回列表，新笔记出现（覆写列表数据模拟入库）
        await setNotes([
            { id: 901, title: '晨间复盘', content: '今天完成了 E2E 测试基建。', folder: 11, updateTime: '2026-11-15 10:00:00' },
            ...WORK_NOTES, ...ROOT_NOTES
        ])
        await expect(page).toHaveURL(/\/m\/note/)
        await expect(page.locator('.note-card__title', { hasText: '晨间复盘' })).toBeVisible()

        // 点击已有笔记进编辑，改标题保存 → PUT /note 断言
        await page.locator('.note-card', { hasText: '工作周报模板' }).click()
        await expect(page).toHaveURL(/detail\?id=301/)
        await expect(page.getByPlaceholder('给这篇笔记起个名字')).toHaveValue('工作周报模板')
        await page.getByPlaceholder('给这篇笔记起个名字').fill('工作周报模板 V2')
        await page.locator('.m-sticky-actions .van-button', { hasText: '保存' }).click()
        await waitForRequest(T, r => r.method === 'PUT' && r.path === '/note'
            && r.body.id === 301 && r.body.title === '工作周报模板 V2')
        await expect(page.locator('.van-toast', { hasText: '修改成功' })).toBeVisible()
    })

    test('N3 文件夹管理：树形展开/新建/改名/删除拦截', async ({ page }) => {
        await loginAs(page, T)
        await page.goto('/m/note/folder')

        // 树形：根层级默认展开，项目A 可见
        await expect(page.locator('.tree-node__name', { hasText: '工作' })).toBeVisible()
        await expect(page.locator('.tree-node__name', { hasText: '项目A' })).toBeVisible()
        await expect(page.locator('.tree-node__name', { hasText: '生活' })).toBeVisible()

        // 收起/展开 工作
        await page.locator('.tree-node__row', { hasText: '工作' }).locator('.tree-node__toggle').click()
        await expect(page.locator('.tree-node__name', { hasText: '项目A' })).toHaveCount(0)
        await page.locator('.tree-node__row', { hasText: '工作' }).locator('.tree-node__toggle').click()
        await expect(page.locator('.tree-node__name', { hasText: '项目A' })).toBeVisible()

        // 改名：编辑弹窗回填旧名 → PUT 断言
        await page.locator('.tree-node__row', { hasText: '工作' }).locator('.tree-node__btn').first().click()
        await expect(page.locator('.folder-dialog__title')).toHaveText('编辑文件夹')
        const nameField = page.locator('.folder-dialog .folder-dialog__field input').first()
        await expect(nameField).toHaveValue('工作')
        await nameField.fill('工作归档')
        await clearRequests(T)
        await page.locator('.folder-dialog__actions .van-button', { hasText: '确定' }).click()
        await waitForRequest(T, r => r.method === 'PUT' && r.path === '/note/folder'
            && r.body.id === 1 && r.body.name === '工作归档' && r.body.parentId === 0)
        await expect(page.locator('.van-toast', { hasText: '修改成功' })).toBeVisible()

        // 新建：FAB → 弹窗 → POST 断言
        await page.locator('button.m-fab').click()
        await expect(page.locator('.folder-dialog__title')).toHaveText('新建文件夹')
        await page.locator('.folder-dialog .folder-dialog__field input').first().fill('灵感收集')
        await page.locator('.folder-dialog__actions .van-button', { hasText: '确定' }).click()
        await waitForRequest(T, r => r.method === 'POST' && r.path === '/note/folder'
            && r.body.name === '灵感收集' && r.body.parentId === 0)
        await expect(page.locator('.van-toast', { hasText: '创建成功' })).toBeVisible()

        // 删除拦截：取消 → 无 DELETE 请求
        await clearRequests(T)
        await page.locator('.tree-node__row', { hasText: '生活' }).locator('.tree-node__btn--danger').click()
        await expect(page.locator('.van-dialog')).toContainText('确定要删除文件夹「生活」')
        await page.locator('.van-dialog__cancel').click()
        let reqs = await getRequests(T)
        expect(reqs.filter(r => r.method === 'DELETE' && r.path.startsWith('/note/folder/'))).toHaveLength(0)

        // 确认删除 → DELETE 请求发出
        await page.locator('.tree-node__row', { hasText: '生活' }).locator('.tree-node__btn--danger').click()
        await page.locator('.van-dialog__confirm').click()
        await waitForRequest(T, r => r.method === 'DELETE' && r.path === '/note/folder/2')
    })

    test('N4 Obsidian 目录浏览/面包屑/搜索结果进编辑', async ({ page }) => {
        await loginAs(page, T)
        await page.goto('/m/note/obsidian')

        // 根目录：文件夹 + md 文件
        await expect(page.locator('.obs-item__name', { hasText: '日记' })).toBeVisible()
        await expect(page.locator('.obs-item__name', { hasText: '周报.md' })).toBeVisible()
        await expect(page.locator('.obs-item__size').first()).toHaveText('2.0K')

        // 进入 日记：覆写子目录数据 → 面包屑 根目录/日记
        await overrideRoute(T, 'GET', '/system/file-manager/list', {
            data: [{ name: '2026-11-14.md', path: '日记/2026-11-14.md', isDirectory: false, size: 512 }]
        })
        await page.locator('.obs-item', { hasText: '日记' }).click()
        await expect(page.locator('.obs-breadcrumb__item').nth(1)).toHaveText('日记')
        await expect(page.locator('.obs-item__name', { hasText: '2026-11-14.md' })).toBeVisible()

        // 面包屑回根目录
        await clearOverride(T, 'GET', '/system/file-manager/list')
        await page.locator('.obs-breadcrumb__item', { hasText: '根目录' }).click()
        await expect(page.locator('.obs-item__name', { hasText: '周报.md' })).toBeVisible()
        await expect(page.locator('.obs-breadcrumb__item')).toHaveCount(1)

        // 搜索：结果带「内容匹配」标记，点击进编辑页
        await page.locator('.obs-search input').fill('周报')
        await page.locator('.obs-search input').press('Enter')
        await expect(page.locator('.m-section-title')).toContainText('搜索结果（1）')
        await expect(page.locator('.obs-item__match')).toHaveText('内容匹配')
        await page.locator('.obs-item', { hasText: '周报.md' }).click()
        await expect(page).toHaveURL(/\/m\/note\/obsidian\/edit\?path=/)
    })

    test('N5 Obsidian md 预览渲染 + 保存断言 + 未保存离开确认', async ({ page }) => {
        await loginAs(page, T)
        await page.goto('/m/note/obsidian')
        await page.locator('.obs-item', { hasText: '周报.md' }).click()
        await expect(page).toHaveURL(/obsidian\/edit/)

        // 预览：标题/列表/代码块
        await page.locator('.obs-mode__item', { hasText: '预览' }).click()
        await expect(page.locator('.obs-preview__md h1')).toHaveText('观影笔记')
        await expect(page.locator('.obs-preview__md li').first()).toHaveText('《星际穿越》')
        await expect(page.locator('.obs-preview__md pre code')).toContainText('const x = 1')

        // 编辑：修改内容 → 未保存标记 → 保存 → PUT write 断言
        await page.locator('.obs-mode__item', { hasText: '编辑' }).click()
        await page.locator('.obs-editor__field textarea').fill('# 更新后的周报\n')
        await expect(page.locator('.obs-info__modified')).toHaveText('未保存')
        await page.locator('.m-sticky-actions .van-button', { hasText: '保存' }).click()
        await waitForRequest(T, r => r.method === 'PUT' && r.path === '/system/file-manager/write'
            && r.body.path === '周报.md' && r.body.content === '# 更新后的周报\n')
        await expect(page.locator('.van-toast', { hasText: '已保存' })).toBeVisible()
        await expect(page.locator('.obs-info__modified')).toHaveCount(0)

        // 未保存离开：弹自绘确认弹层；取消 = 放弃修改离开（不发保存请求）
        await page.locator('.obs-editor__field textarea').fill('# 再次修改\n')
        await expect(page.locator('.obs-info__modified')).toHaveText('未保存')
        await clearRequests(T)
        await page.locator('.m-app__navbar .van-nav-bar__left').click()
        await expect(page.locator('.leave-dialog')).toContainText('有未保存的修改，保存后离开？')
        await page.locator('.leave-dialog .van-button', { hasText: '取消' }).click()
        await expect(page).toHaveURL(/\/m\/note\/obsidian$/)
        const discards = await getRequests(T)
        expect(discards.filter(r => r.method === 'PUT' && r.path === '/system/file-manager/write')).toHaveLength(0)

        // 确认 = 保存后离开（PUT write 后返回目录）
        await page.locator('.obs-item', { hasText: '周报.md' }).click()
        await expect(page).toHaveURL(/obsidian\/edit/)
        await expect(page.locator('.obs-editor__field textarea')).toHaveValue(/观影笔记/)
        await page.locator('.obs-editor__field textarea').fill('# 确认保存版\n')
        await page.locator('.m-app__navbar .van-nav-bar__left').click()
        await page.locator('.leave-dialog .van-button', { hasText: '确定' }).click()
        await waitForRequest(T, r => r.method === 'PUT' && r.path === '/system/file-manager/write'
            && r.body.content === '# 确认保存版\n')
        await expect(page).toHaveURL(/\/m\/note\/obsidian$/)
    })

    test('N6 书架状态 Tab/搜索/分页', async ({ page }) => {
        await loginAs(page, T)
        await page.goto('/m/book')
        await expect(page.locator('.m-book-card__title', { hasText: '三体' })).toBeVisible()
        await expect(page.locator('.m-book-card', { hasText: '活着' }).locator('.m-book-card__status')).toHaveText('已读完')
        await expect(page.locator('.m-book-card', { hasText: '三体' }).locator('.m-book-card__meta-read')).toContainText('第一章 起点')
        await expect(page.locator('.m-book-card')).toHaveCount(20)

        // 分页：滚动触底加载第二页（mock 不分页，第二页返回同款数据 → 拼接后 finished）
        await clearRequests(T)
        await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))
        await waitForRequest(T, r => r.path === '/myBook/list' && r.query.pageNum === '2')
        await expect(page.getByText('没有更多了')).toBeVisible()

        // 状态 Tab：在读
        await overrideRoute(T, 'GET', '/myBook/list', {
            rows: [{ id: 1, title: '三体', author: '刘慈欣', status: 'reading', chapterCount: 12 }],
            total: 1
        })
        await page.locator('.m-book-tabs__item', { hasText: '在读' }).click()
        await waitForRequest(T, r => r.path === '/myBook/list' && r.query.status === 'reading')
        await expect(page.locator('.m-book-card')).toHaveCount(1)
        await expect(page.locator('.m-book-card__title', { hasText: '活着' })).toHaveCount(0)

        // 搜索：标题关键词
        await overrideRoute(T, 'GET', '/myBook/list', {
            rows: [{ id: 2, title: '活着', author: '余华', status: 'finished', chapterCount: 10 }],
            total: 1
        })
        await page.getByPlaceholder('搜索书名、作者...').fill('活着')
        await page.getByPlaceholder('搜索书名、作者...').press('Enter')
        await waitForRequest(T, r => r.path === '/myBook/list' && r.query.title === '活着')
        await expect(page.locator('.m-book-card')).toHaveCount(1)
        await expect(page.locator('.m-book-card__title', { hasText: '活着' })).toBeVisible()
    })

    test('N7 TXT 导入 → POST /myBook/txt/import 断言 → 书架出现', async ({ page }) => {
        await loginAs(page, T)
        await clearOverride(T, 'GET', '/myBook/list') // 清掉 N6 留下的筛选覆写
        await page.goto('/m/book')
        await expect(page.locator('.m-book-card__title', { hasText: '三体' })).toBeVisible()

        // 隐藏 file input 直接 setInputFiles 触发 change → 导入
        await clearRequests(T)
        await page.setInputFiles('input.m-book-file-input', {
            name: '测试小说.txt',
            mimeType: 'text/plain',
            buffer: Buffer.from('第一章 起点\n正文内容。\n第二章 转折\n正文内容。\n', 'utf8')
        })
        await waitForRequest(T, r => r.method === 'POST' && r.path === '/myBook/txt/import')
        await expect(page.locator('.van-toast', { hasText: '导入成功' })).toBeVisible()

        // 覆写书架数据模拟入库 → 刷新后新书出现
        await overrideRoute(T, 'GET', '/myBook/list', {
            rows: [
                { id: 1, title: '三体', author: '刘慈欣', status: 'reading', chapterCount: 12 },
                { id: 99, title: '测试小说', author: '未知作者', status: 'reading', chapterCount: 2 }
            ],
            total: 2
        })
        await page.reload()
        await expect(page.locator('.m-book-card__title', { hasText: '测试小说' })).toBeVisible()
        await expect(page.locator('.m-book-card', { hasText: '测试小说' }).locator('.m-book-card__meta-item').first()).toContainText('2章')
    })

    test('N8 阅读器：plain 全屏/字号主题持久化/章节切换/进度保存', async ({ page }) => {
        await loginAs(page, T)
        await page.goto('/m/book/reader?chapterId=201')

        // plain 路由：无默认导航栏，自绘顶栏
        await expect(page.locator('.van-nav-bar')).toHaveCount(0)
        await expect(page.locator('.reader-topbar')).toBeVisible()
        await expect(page.locator('.reader-topbar__title')).toHaveText('第一章 起点')
        await expect(page.locator('.reader-inner__title')).toHaveText('第一章 起点')

        // 打开即静默保存进度
        await waitForRequest(T, r => r.method === 'PUT' && r.path === '/myBook/progress/201')

        // 设置面板：字号 + / 主题绿
        await page.locator('.reader-toolbar__btn .van-icon-setting-o').click()
        await expect(page.locator('.reader-setting')).toBeVisible()
        await expect(page.locator('.reader-inner')).toHaveCSS('font-size', '17px')
        await page.locator('.reader-setting__size:has(.van-icon-plus)').click()
        await expect(page.locator('.reader-inner')).toHaveCSS('font-size', '19px')
        await page.locator('.reader-setting__theme.dot-green').click()
        await expect(page.locator('.m-book-reader')).toHaveClass(/theme-green/)
        const saved = await page.evaluate(() => JSON.parse(localStorage.getItem('book_reader_setting')))
        expect(saved).toEqual({ fontIndex: 2, theme: 'green' })

        // 刷新后持久化
        await page.reload()
        await expect(page.locator('.reader-inner')).toHaveCSS('font-size', '19px')
        await expect(page.locator('.m-book-reader')).toHaveClass(/theme-green/)

        // 下一章（覆写 202 章节数据）→ 进度保存
        await overrideRoute(T, 'GET', '/myBook/chapter/202/reader', {
            data: {
                chapter: { id: 202, chapterName: '第二章 转折', chapterNo: 2, wordCount: 2800, content: '第二章正文。' },
                previous: { id: 201, chapterName: '第一章 起点' },
                next: null
            }
        })
        await page.locator('.reader-end__btn', { hasText: '下一章' }).click()
        await expect(page.locator('.reader-inner__title')).toHaveText('第二章 转折')
        await waitForRequest(T, r => r.method === 'PUT' && r.path === '/myBook/progress/202')
        await expect(page.getByText('— 全书完 —')).toBeVisible()

        // 上一章回到第一章
        await page.locator('.reader-end__btn', { hasText: '上一章' }).click()
        await expect(page.locator('.reader-inner__title')).toHaveText('第一章 起点')
    })

    test('N9 书籍编辑保存/删除确认', async ({ page }) => {
        await loginAs(page, T)
        await page.goto('/m/book/edit?id=1')
        await expect(page.getByPlaceholder('请输入书名')).toHaveValue('三体')
        await expect(page.getByPlaceholder('请选择', { exact: true })).toHaveValue('在读')

        // 删除确认：取消 → 无请求；确认 → DELETE
        await clearRequests(T)
        await page.locator('.book-edit-delete .van-button', { hasText: '删除书籍' }).click()
        await expect(page.locator('.van-dialog')).toContainText('确定要删除《三体》')
        await page.locator('.van-dialog__cancel').click()
        let reqs = await getRequests(T)
        expect(reqs.filter(r => r.method === 'DELETE' && r.path === '/myBook/1')).toHaveLength(0)
        await page.locator('.book-edit-delete .van-button', { hasText: '删除书籍' }).click()
        await page.locator('.van-dialog__confirm').click()
        await waitForRequest(T, r => r.method === 'DELETE' && r.path === '/myBook/1')

        // 修改书名保存 → PUT 断言
        await page.getByPlaceholder('请输入书名').fill('三体（纪念版）')
        await page.locator('.m-sticky-actions .van-button', { hasText: '保存' }).click()
        await waitForRequest(T, r => r.method === 'PUT' && r.path === '/myBook'
            && r.body.id === 1 && r.body.title === '三体（纪念版）')
        await expect(page.locator('.van-toast', { hasText: '保存成功' })).toBeVisible()
    })
})
