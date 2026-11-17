/**
 * PC ↔ APP（/m）路由映射与分流解析
 *
 * PC 端业务路由来自后端菜单（历史上有 /mytool/* 与 /px/life/* 两代前缀），
 * 因此除精确表外还提供关键词匹配；均未命中时移动端回落 /m/home、PC 端回落 /index。
 */
import { isMobilePath } from './guards.js'

// ---- 精确映射（PC 静态路由 / 常用入口）----
const PC_TO_M_EXACT = {
    '/': '/m/home',
    '/index': '/m/home',
    '/homepage': '/m/home',
    '/user/profile': '/m/mine',
    '/ai/tools': '/m/ai/chat',
    '/px/ai/chat': '/m/ai/chat',
    '/system/file-manager': '/m/note/obsidian',
    '/px/app/book': '/m/book',
    '/myapp/book/chapter': '/m/book',
    '/note-share': '/note-share' // 免登录分享页，两端通用，不映射
}

// ---- 关键词映射（按顺序首个命中生效；注意长词在前避免前缀误伤）----
const PC_TO_M_KEYWORDS = [
    ['bookkeeping/record', '/m/bookkeeping/record'],
    ['bookkeeping/account', '/m/bookkeeping/account'],
    ['bookkeeping/classification', '/m/bookkeeping'],
    ['bookkeeping/statistics', '/m/bookkeeping/statistics'],
    ['bookkeeping/budget', '/m/bookkeeping/budget'],
    ['bookkeeping/recurring', '/m/bookkeeping/recurring'],
    ['bookkeeping', '/m/bookkeeping'],
    ['webArticle', '/m/web-article'],
    ['mealPlan', '/m/meal-plan'],
    ['meal', '/m/meal-plan'],
    ['commemorationDay', '/m/commemoration-day'],
    ['menstruation', '/m/menstruation'],
    ['shopping', '/m/shopping-list'],
    ['recipe', '/m/recipe'],
    ['calendar', '/m/calendar'],
    ['reminder', '/m/reminder'],
    ['report', '/m/report'],
    ['diary/analysis', '/m/diary/analysis'],
    ['diary', '/m/diary'],
    ['todo', '/m/todo'],
    ['note', '/m/note'],
    ['book', '/m/book'],
    ['card', '/m/card'],
    ['lifeManager', '/m/life-manager'],
    ['automation', '/m/automation'],
    ['search', '/m/search'],
    ['ai/chat', '/m/ai/chat']
]

// 移动端独有页面 → 回 PC 时给一个语义合理的落点
const M_TO_PC_EXACT = {
    '/m/home': '/index',
    '/m/function': '/index',
    '/m/mine': '/user/profile',
    '/m/mine/avatar': '/user/profile',
    '/m/mine/info': '/user/profile',
    '/m/mine/info/edit': '/user/profile',
    '/m/mine/pwd': '/user/profile',
    '/m/mine/setting': '/user/profile',
    '/m/mine/setting/family': '/system/dataGroup',
    '/m/mine/help': '/user/profile',
    '/m/mine/about': '/index',
    '/m/ai/chat': '/ai/tools',
    '/m/note/obsidian': '/system/file-manager',
    '/m/book': '/px/app/book',
    '/m/book/detail': '/px/app/book',
    '/m/book/edit': '/px/app/book',
    '/m/todo': '/px/life/todo',
    '/m/todo/edit': '/px/life/todo',
    '/m/diary': '/px/life/diary',
    '/m/diary/edit': '/px/life/diary',
    '/m/diary/analysis': '/px/life/diary',
    '/m/note': '/px/life/note',
    '/m/note/detail': '/px/life/note',
    '/m/note/folder': '/px/life/note',
    '/m/bookkeeping': '/px/life/bookkeeping/record',
    '/m/bookkeeping/record': '/px/life/bookkeeping/record',
    '/m/bookkeeping/record/add': '/px/life/bookkeeping/record',
    '/m/bookkeeping/account': '/px/life/bookkeeping/account',
    '/m/bookkeeping/account-details': '/px/life/bookkeeping/account',
    '/m/bookkeeping/account-maintenance': '/px/life/bookkeeping/account',
    '/m/bookkeeping/account/details': '/px/life/bookkeeping/account',
    '/m/bookkeeping/account/maintenance': '/px/life/bookkeeping/account',
    '/m/bookkeeping/statistics': '/px/life/bookkeeping/statistics',
    '/m/bookkeeping/budget': '/px/life/bookkeeping/budget',
    '/m/bookkeeping/recurring': '/px/life/bookkeeping/recurring',
    '/m/shopping-list': '/px/life/shopping',
    '/m/shopping-list/detail': '/px/life/shopping',
    '/m/reminder': '/px/life/reminder',
    '/m/reminder/settings': '/px/life/reminder',
    '/m/card': '/px/life/card',
    '/m/card/history': '/px/life/card',
    '/m/commemoration-day': '/px/life/commemorationDay',
    '/m/commemoration-day/add': '/px/life/commemorationDay',
    '/m/menstruation': '/mytool/menstruationAssistant',
    '/m/menstruation/record': '/mytool/menstruationAssistant',
    '/m/menstruation/settings': '/mytool/menstruationAssistant',
    '/m/menstruation/statistics': '/mytool/menstruationAssistant',
    '/m/recipe': '/px/life/recipe',
    '/m/recipe/detail': '/px/life/recipe',
    '/m/meal-plan': '/px/life/meal',
    '/m/report': '/px/life/report',
    '/m/calendar': '/px/life/calendar',
    '/m/web-article': '/px/app/webArticle',
    '/m/web-article/detail': '/px/app/webArticle',
    '/m/web-article/edit': '/px/app/webArticle',
    '/m/web-article/rss': '/px/app/webArticle',
    '/m/search': '/index',
    '/m/automation': '/index',
    '/m/life-manager': '/px/life/lifeManager',
    '/m/webview': '/m/webview', // 通用页，两端无差异
    '/m/textview': '/m/textview'
}

/**
 * PC 路径 → 移动路径。
 * 返回 { path, fallback }：fallback=true 表示没有对应移动页（调用方提示并落首页）。
 * 白名单页（/login 等）返回原路径（不映射，交给登录流程）。
 */
export function resolveMobileTarget(pcPath) {
    if (PC_TO_M_EXACT[pcPath]) {
        return { path: PC_TO_M_EXACT[pcPath], fallback: false }
    }
    for (const [keyword, mPath] of PC_TO_M_KEYWORDS) {
        if (pcPath.includes(keyword)) {
            return { path: mPath, fallback: false }
        }
    }
    return { path: '/m/home', fallback: true }
}

/** 移动路径 → PC 路径；无映射时回落 /index */
export function resolvePcTarget(mPath) {
    if (M_TO_PC_EXACT[mPath]) {
        return M_TO_PC_EXACT[mPath]
    }
    // 带参数/子路径的移动页：剥掉尾段再试一次
    const base = mPath.replace(/\/[^/]+$/, '')
    if (base && base !== mPath && M_TO_PC_EXACT[base]) {
        return M_TO_PC_EXACT[base]
    }
    return '/index'
}

/**
 * uniapp 菜单 appPath（如 /pages_life/bookkeeping/record/add）→ /m 路由。
 * 生活助手宫格用；未识别返回 null 由调用方走 emoji 兜底或隐藏。
 */
const UNIAPP_TO_M = {
    'pages/index': '/m/home',
    'pages/function/index': '/m/function',
    'pages/mine/index': '/m/mine',
    'pages_life/ai/chat': '/m/ai/chat',
    'pages_life/automation/index': '/m/automation',
    'pages_life/book/index': '/m/book',
    'pages_life/book/detail': '/m/book',
    'pages_life/book/reader': '/m/book/reader',
    'pages_life/book/edit': '/m/book/edit',
    'pages_life/bookkeeping/index': '/m/bookkeeping',
    'pages_life/bookkeeping/record/add': '/m/bookkeeping/record/add',
    'pages_life/bookkeeping/record/index': '/m/bookkeeping/record',
    'pages_life/bookkeeping/account/index': '/m/bookkeeping/account',
    'pages_life/bookkeeping/account/details': '/m/bookkeeping/account/details',
    'pages_life/bookkeeping/account/maintenance': '/m/bookkeeping/account/maintenance',
    'pages_life/bookkeeping/statistics/index': '/m/bookkeeping/statistics',
    'pages_life/bookkeeping/budget/index': '/m/bookkeeping/budget',
    'pages_life/bookkeeping/recurring/index': '/m/bookkeeping/recurring',
    'pages_life/calendar/index': '/m/calendar',
    'pages_life/card/index': '/m/card',
    'pages_life/card/history': '/m/card/history',
    'pages_life/commemorationDay/index': '/m/commemoration-day',
    'pages_life/commemorationDay/add': '/m/commemoration-day/add',
    'pages_life/diary/index': '/m/diary',
    'pages_life/diary/edit': '/m/diary/edit',
    'pages_life/diary/analysis': '/m/diary/analysis',
    'pages_life/todo/index': '/m/todo',
    'pages_life/todo/edit': '/m/todo/edit',
    'pages_life/menstruationAssistant/index': '/m/menstruation',
    'pages_life/menstruationAssistant/record': '/m/menstruation/record',
    'pages_life/menstruationAssistant/settings': '/m/menstruation/settings',
    'pages_life/menstruationAssistant/statistics': '/m/menstruation/statistics',
    'pages_life/reminder/index': '/m/reminder',
    'pages_life/reminder/settings': '/m/reminder/settings',
    'pages_life/report/index': '/m/report',
    'pages_life/search/index': '/m/search',
    'pages_life/lifeManager/index': '/m/life-manager',
    'pages_life/recipe/index': '/m/recipe',
    'pages_life/recipe/detail': '/m/recipe/detail',
    'pages_life/mealPlan/index': '/m/meal-plan',
    'pages_life/shoppingList/index': '/m/shopping-list',
    'pages_life/shoppingList/detail': '/m/shopping-list/detail',
    'pages_life/note/index': '/m/note',
    'pages_life/note/detail': '/m/note/detail',
    'pages_life/note/obsidian': '/m/note/obsidian',
    'pages_life/note/obsidianEdit': '/m/note/obsidian/edit',
    'pages_life/note/folder': '/m/note/folder',
    'pages_life/webArticle/index': '/m/web-article',
    'pages_life/webArticle/detail': '/m/web-article/detail',
    'pages_life/webArticle/edit': '/m/web-article/edit',
    'pages_life/webArticle/rss': '/m/web-article/rss',
    // pages_system（后端菜单 appPath 兼容）
    'pages_system/mine/avatar/index': '/m/mine/avatar',
    'pages_system/mine/info/index': '/m/mine/info',
    'pages_system/mine/info/edit': '/m/mine/info/edit',
    'pages_system/mine/pwd/index': '/m/mine/pwd',
    'pages_system/mine/setting/index': '/m/mine/setting',
    'pages_system/mine/setting/family': '/m/mine/setting/family',
    'pages_system/mine/help/index': '/m/mine/help',
    'pages_system/mine/about/index': '/m/mine/about',
    'pages_system/common/webview/index': '/m/webview',
    'pages_system/common/textview/index': '/m/textview',
    'pages_system/search/index': '/m/search'
}

export function uniappPathToMobile(appPath) {
    if (!appPath) return null
    const p = String(appPath).replace(/^\/?pages_system\//, 'pages_system/')
    if (UNIAPP_TO_M[p]) return UNIAPP_TO_M[p]
    // 无前导斜杠/带 query 的宽松匹配
    const key = p.replace(/^\//, '').split('?')[0]
    return UNIAPP_TO_M[key] || null
}

export { isMobilePath }
