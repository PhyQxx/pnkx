/**
 * 移动端（APP 效果）路由树
 *
 * - 全静态路由，不依赖后端菜单；路径与 pnkx-uniapp 页面路径同构
 * - meta.tab = true 的 3 个页面挂底部 TabBar 并被 keep-alive 缓存
 * - meta.plain = true 的页面隐藏 MobileLayout 默认导航栏（页面自绘）
 */
import MobileLayout from '@/mobile/layout/index.vue'

export const mobileRoutes = [
    {
        path: '/m',
        component: MobileLayout,
        redirect: '/m/home',
        children: [
            // ---- TabBar ----
            {
                path: 'home',
                name: 'MHome',
                component: () => import('@/mobile/views/home/index.vue'),
                meta: { title: '首页', tab: true }
            },
            {
                path: 'function',
                name: 'MFunction',
                component: () => import('@/mobile/views/function/index.vue'),
                meta: { title: '生活助手', tab: true }
            },
            {
                path: 'mine',
                name: 'MMine',
                component: () => import('@/mobile/views/mine/index.vue'),
                meta: { title: '我的', tab: true }
            },
            // ---- 个人中心 ----
            {
                path: 'mine/avatar',
                name: 'MMineAvatar',
                component: () => import('@/mobile/views/mine/avatar.vue'),
                meta: { title: '修改头像' }
            },
            {
                path: 'mine/info',
                name: 'MMineInfo',
                component: () => import('@/mobile/views/mine/info.vue'),
                meta: { title: '个人信息' }
            },
            {
                path: 'mine/info/edit',
                name: 'MMineInfoEdit',
                component: () => import('@/mobile/views/mine/info-edit.vue'),
                meta: { title: '编辑资料' }
            },
            {
                path: 'mine/pwd',
                name: 'MMinePwd',
                component: () => import('@/mobile/views/mine/pwd.vue'),
                meta: { title: '修改密码' }
            },
            {
                path: 'mine/setting',
                name: 'MMineSetting',
                component: () => import('@/mobile/views/mine/setting.vue'),
                meta: { title: '应用设置' }
            },
            {
                path: 'mine/setting/family',
                name: 'MFamily',
                component: () => import('@/mobile/views/mine/family.vue'),
                meta: { title: '家庭与情侣空间' }
            },
            {
                path: 'mine/help',
                name: 'MMineHelp',
                component: () => import('@/mobile/views/mine/help.vue'),
                meta: { title: '常见问题' }
            },
            {
                path: 'mine/about',
                name: 'MMineAbout',
                component: () => import('@/mobile/views/mine/about.vue'),
                meta: { title: '关于我们' }
            },
            // ---- 通用页 ----
            {
                path: 'webview',
                name: 'MWebview',
                component: () => import('@/mobile/views/common/webview.vue'),
                meta: { title: '浏览网页', plain: true }
            },
            {
                path: 'textview',
                name: 'MTextview',
                component: () => import('@/mobile/views/common/textview.vue'),
                meta: { title: '浏览文本' }
            },
            // ---- 记账 ----
            {
                path: 'bookkeeping',
                name: 'MBookkeeping',
                component: () => import('@/mobile/views/bookkeeping/index.vue'),
                meta: { title: '生活账本' }
            },
            {
                path: 'bookkeeping/record/add',
                alias: 'bookkeeping/record-add',
                name: 'MBookkeepingRecordAdd',
                component: () => import('@/mobile/views/bookkeeping/record-add.vue'),
                meta: { title: '记一笔' }
            },
            {
                path: 'bookkeeping/record',
                name: 'MBookkeepingRecord',
                component: () => import('@/mobile/views/bookkeeping/record.vue'),
                meta: { title: '账目清单' }
            },
            {
                path: 'bookkeeping/account',
                name: 'MBookkeepingAccount',
                component: () => import('@/mobile/views/bookkeeping/account.vue'),
                meta: { title: '账户管理' }
            },
            {
                path: 'bookkeeping/account/details',
                alias: 'bookkeeping/account-details',
                name: 'MBookkeepingAccountDetails',
                component: () => import('@/mobile/views/bookkeeping/account-details.vue'),
                meta: { title: '账户详情' }
            },
            {
                path: 'bookkeeping/account/maintenance',
                alias: 'bookkeeping/account-maintenance',
                name: 'MBookkeepingAccountMaintenance',
                component: () => import('@/mobile/views/bookkeeping/account-maintenance.vue'),
                meta: { title: '账户维护' }
            },
            {
                path: 'bookkeeping/statistics',
                name: 'MBookkeepingStatistics',
                component: () => import('@/mobile/views/bookkeeping/statistics.vue'),
                meta: { title: '账目统计' }
            },
            {
                path: 'bookkeeping/budget',
                name: 'MBookkeepingBudget',
                component: () => import('@/mobile/views/bookkeeping/budget.vue'),
                meta: { title: '预算管理' }
            },
            {
                path: 'bookkeeping/recurring',
                name: 'MBookkeepingRecurring',
                component: () => import('@/mobile/views/bookkeeping/recurring.vue'),
                meta: { title: '周期记账' }
            },
            // ---- 待办 ----
            {
                path: 'todo',
                name: 'MTodo',
                component: () => import('@/mobile/views/todo/index.vue'),
                meta: { title: '待办事项' }
            },
            {
                path: 'todo/edit',
                name: 'MTodoEdit',
                component: () => import('@/mobile/views/todo/edit.vue'),
                meta: { title: '待办编辑' }
            },
            // ---- 日记 ----
            {
                path: 'diary',
                name: 'MDiary',
                component: () => import('@/mobile/views/diary/index.vue'),
                meta: { title: '我的日记' }
            },
            {
                path: 'diary/edit',
                name: 'MDiaryEdit',
                component: () => import('@/mobile/views/diary/edit.vue'),
                meta: { title: '写日记' }
            },
            {
                path: 'diary/analysis',
                name: 'MDiaryAnalysis',
                component: () => import('@/mobile/views/diary/analysis.vue'),
                meta: { title: '心情分析' }
            },
            // ---- 笔记 ----
            {
                path: 'note',
                name: 'MNote',
                component: () => import('@/mobile/views/note/index.vue'),
                meta: { title: '我的笔记' }
            },
            {
                path: 'note/detail',
                name: 'MNoteDetail',
                component: () => import('@/mobile/views/note/detail.vue'),
                meta: { title: '笔记详情' }
            },
            {
                path: 'note/folder',
                name: 'MNoteFolder',
                component: () => import('@/mobile/views/note/folder.vue'),
                meta: { title: '文件夹管理' }
            },
            {
                path: 'note/obsidian',
                name: 'MNoteObsidian',
                component: () => import('@/mobile/views/note/obsidian.vue'),
                meta: { title: 'Obsidian 笔记' }
            },
            {
                path: 'note/obsidian/edit',
                name: 'MNoteObsidianEdit',
                component: () => import('@/mobile/views/note/obsidian-edit.vue'),
                meta: { title: '编辑文件' }
            },
            // ---- 书城 ----
            {
                path: 'book',
                name: 'MBook',
                component: () => import('@/mobile/views/book/index.vue'),
                meta: { title: '我的书城' }
            },
            {
                path: 'book/detail',
                name: 'MBookDetail',
                component: () => import('@/mobile/views/book/detail.vue'),
                meta: { title: '书籍详情' }
            },
            {
                path: 'book/reader',
                name: 'MBookReader',
                component: () => import('@/mobile/views/book/reader.vue'),
                meta: { title: '阅读', plain: true }
            },
            {
                path: 'book/edit',
                name: 'MBookEdit',
                component: () => import('@/mobile/views/book/edit.vue'),
                meta: { title: '编辑书籍' }
            },
            // ---- 网址收藏 ----
            {
                path: 'web-article',
                name: 'MWebArticle',
                component: () => import('@/mobile/views/web-article/index.vue'),
                meta: { title: '网址收藏' }
            },
            {
                path: 'web-article/detail',
                name: 'MWebArticleDetail',
                component: () => import('@/mobile/views/web-article/detail.vue'),
                meta: { title: '阅读文章' }
            },
            {
                path: 'web-article/edit',
                name: 'MWebArticleEdit',
                component: () => import('@/mobile/views/web-article/edit.vue'),
                meta: { title: '编辑收藏' }
            },
            {
                path: 'web-article/rss',
                name: 'MWebArticleRss',
                component: () => import('@/mobile/views/web-article/rss.vue'),
                meta: { title: 'RSS 订阅' }
            },
            // ---- 购物清单 ----
            {
                path: 'shopping-list',
                name: 'MShoppingList',
                component: () => import('@/mobile/views/shopping-list/index.vue'),
                meta: { title: '购物清单' }
            },
            {
                path: 'shopping-list/detail',
                name: 'MShoppingListDetail',
                component: () => import('@/mobile/views/shopping-list/detail.vue'),
                meta: { title: '清单详情' }
            },
            // ---- 菜谱 / 膳食计划 ----
            {
                path: 'recipe',
                name: 'MRecipe',
                component: () => import('@/mobile/views/recipe/index.vue'),
                meta: { title: '菜谱库' }
            },
            {
                path: 'recipe/detail',
                name: 'MRecipeDetail',
                component: () => import('@/mobile/views/recipe/detail.vue'),
                meta: { title: '菜谱详情' }
            },
            {
                path: 'meal-plan',
                name: 'MMealPlan',
                component: () => import('@/mobile/views/meal-plan/index.vue'),
                meta: { title: '膳食计划' }
            },
            // ---- AI 助手 ----
            {
                path: 'ai/chat',
                name: 'MAiChat',
                component: () => import('@/mobile/views/ai/chat.vue'),
                meta: { title: 'AI 助手' }
            },
            // ---- 提醒中心 ----
            {
                path: 'reminder',
                name: 'MReminder',
                component: () => import('@/mobile/views/reminder/index.vue'),
                meta: { title: '提醒中心' }
            },
            {
                path: 'reminder/settings',
                name: 'MReminderSettings',
                component: () => import('@/mobile/views/reminder/settings.vue'),
                meta: { title: '提醒偏好' }
            },
            // ---- 家庭日历 ----
            {
                path: 'calendar',
                name: 'MCalendar',
                component: () => import('@/mobile/views/calendar/index.vue'),
                meta: { title: '家庭日历' }
            },
            // ---- 生活报告 ----
            {
                path: 'report',
                name: 'MReport',
                component: () => import('@/mobile/views/report/index.vue'),
                meta: { title: '生活报告' }
            },
            // ---- 全局搜索 ----
            {
                path: 'search',
                name: 'MSearch',
                component: () => import('@/mobile/views/search/index.vue'),
                meta: { title: '全局搜索' }
            },
            // ---- 姨妈助手 ----
            {
                path: 'menstruation',
                name: 'MMenstruation',
                component: () => import('@/mobile/views/menstruation/index.vue'),
                meta: { title: '姨妈助手' }
            },
            {
                path: 'menstruation/record',
                name: 'MMenstruationRecord',
                component: () => import('@/mobile/views/menstruation/record.vue'),
                meta: { title: '经期记录' }
            },
            {
                path: 'menstruation/settings',
                name: 'MMenstruationSettings',
                component: () => import('@/mobile/views/menstruation/settings.vue'),
                meta: { title: '经期设置' }
            },
            {
                path: 'menstruation/statistics',
                name: 'MMenstruationStatistics',
                component: () => import('@/mobile/views/menstruation/statistics.vue'),
                meta: { title: '经期统计' }
            },
            // ---- 纪念日 ----
            {
                path: 'commemoration-day',
                name: 'MCommemorationDay',
                component: () => import('@/mobile/views/commemoration-day/index.vue'),
                meta: { title: '纪念日' }
            },
            {
                path: 'commemoration-day/add',
                name: 'MCommemorationDayAdd',
                component: () => import('@/mobile/views/commemoration-day/add.vue'),
                meta: { title: '纪念日编辑' }
            },
            // ---- 情侣卡券 ----
            {
                path: 'card',
                name: 'MCard',
                component: () => import('@/mobile/views/card/index.vue'),
                meta: { title: '情侣卡券' }
            },
            {
                path: 'card/history',
                name: 'MCardHistory',
                component: () => import('@/mobile/views/card/history.vue'),
                meta: { title: '使用记录' }
            },
            // ---- 生活自动化 ----
            {
                path: 'automation',
                name: 'MAutomation',
                component: () => import('@/mobile/views/automation/index.vue'),
                meta: { title: '生活自动化' }
            },
            // ---- 生活管家 ----
            {
                path: 'life-manager',
                name: 'MLifeManager',
                component: () => import('@/mobile/views/life-manager/index.vue'),
                meta: { title: '生活管家' }
            }
        ]
    }
]

export default mobileRoutes
