/**
 * 移动端路由映射一致性检查
 *
 * 断言：
 * 1. map.js 的 UNIAPP_TO_M 与 M_TO_PC_EXACT 中出现的每个 /m 路径，
 *    都在 mobile/router/index.js 中注册（redirect 目标除外）。
 * 2. mobile/router/index.js 中注册的每个 /m 路径，在 map.js 的 M_TO_PC_EXACT
 *    中有 PC 回落映射（TabBar/通用页允许豁免清单）。
 * 3. map.js 的 PC_TO_M_EXACT 的移动目标均已注册。
 *
 * 用法：node scripts/check-mobile-route-map.mjs
 */
import { readFileSync } from 'node:fs'
import { strict as assert } from 'node:assert'

const read = path => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

const routerSrc = read('src/mobile/router/index.js')
const mapSrc = read('src/mobile/router/map.js')

// ---- 从 router/index.js 提取已注册移动路径 ----
const routePaths = new Set()
for (const m of routerSrc.matchAll(/path:\s*'([^']+)'/g)) {
    // 跳过父路由（path 以 / 开头的绝对路径，如 '/m'）
    if (!m[1].startsWith('/')) routePaths.add('/m/' + m[1])
}
for (const m of routerSrc.matchAll(/alias:\s*'([^']+)'/g)) {
    if (!m[1].startsWith('/')) routePaths.add('/m/' + m[1])
}
assert(routePaths.size > 0, 'mobile router 中未解析到任何路由 path')

// ---- 从 map.js 提取映射 ----
const mPathsInMap = new Set()
for (const m of mapSrc.matchAll(/'\/m(?![a-z])[a-z/.-]*'/g)) {
    mPathsInMap.add(m[0].slice(1, -1))
}
const pcToMTargets = [...mapSrc.matchAll(/'\/(?!m)[^']*':\s*'(\/m[/.a-z-]*)'/g)].map(m => m[1])

// 无需 PC 回落映射的豁免（TabBar 壳/通用页/alias 变体）
const EXEMPT_FROM_PC_FALLBACK = new Set([
    '/m',            // redirect 壳
    '/m/home',       // TabBar，有映射（占位防误报）
    '/m/function',   // TabBar，有映射（占位防误报）
    '/m/mine',       // TabBar，有映射（占位防误报）
    '/m/webview',
    '/m/textview',
    '/m/bookkeeping/record-add', // record/add 的 alias
    '/m/commemoration-day/add',
    '/m/book/reader',
    '/m/book/edit',
    '/m/mine/avatar',
    '/m/mine/info/edit',
    '/m/diary/edit',
    '/m/todo/edit',
    '/m/diary/analysis',
    '/m/note/detail',
    '/m/note/obsidian/edit',
    '/m/note/folder',
    '/m/shopping-list/detail',
    '/m/recipe/detail',
    '/m/web-article/detail',
    '/m/web-article/edit',
    '/m/web-article/rss',
    '/m/menstruation/record',
    '/m/menstruation/settings',
    '/m/menstruation/statistics',
    '/m/card/history',
    '/m/reminder/settings',
    '/m/bookkeeping/account/details',
    '/m/bookkeeping/account/maintenance',
    '/m/bookkeeping/record/add',
    '/m/bookkeeping/record',
    '/m/ai/chat',
    '/m/search',
    '/m/automation',
    '/m/meal-plan'
].filter(p => p !== '/m'))

// ---- 1. map 中的 /m 路径必须已注册 ----
const unregistered = [...mPathsInMap].filter(p => p !== '/m' && !routePaths.has(p))
assert.deepStrictEqual(unregistered, [], `map.js 引用了未注册的移动路由: ${unregistered.join(', ')}`)

// ---- 2. PC_TO_M_EXACT 的目标必须已注册 ----
const badPcTargets = pcToMTargets.filter(p => !routePaths.has(p) && p !== '/m/home')
assert.deepStrictEqual([...new Set(badPcTargets)], [], `PC_TO_M_EXACT 指向未注册路由: ${[...new Set(badPcTargets)].join(', ')}`)

// ---- 3. 已注册路由应有 PC 回落（豁免除外）----
const missingFallback = [...routePaths].filter(p => !EXEMPT_FROM_PC_FALLBACK.has(p) && !mPathsInMap.has(p))
assert.deepStrictEqual(missingFallback, [], `以下移动路由缺少 PC 回落映射: ${missingFallback.join(', ')}`)

console.log(`✓ 移动路由映射一致：${routePaths.size} 个移动路由、${mPathsInMap.size} 个映射路径校验通过`)
