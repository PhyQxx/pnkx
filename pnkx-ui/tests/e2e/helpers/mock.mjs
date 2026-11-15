/**
 * 鉴权与 Mock 控制工具
 *
 * token 档案（Mock Server 按 token 区分 V2 功能开关 / 覆写 / 日志）：
 *   DEFAULT 全关 | ALL 全开 | AC2 行动中心 | OP2 操作计划 | CI2 账期 | SP2 购物 | RA2 报告
 * 任何其他字符串也是合法 token（全关档案），spec 可自取专属 token 隔离状态。
 */
export const TOKENS = {
    DEFAULT: 'test-token',
    ALL: 'test-token-all',
    AC2: 'test-token-ac2',
    OP2: 'test-token-op2',
    CI2: 'test-token-ci2',
    SP2: 'test-token-sp2',
    RA2: 'test-token-ra2'
}

const MOCK = 'http://localhost:' + (process.env.E2E_MOCK_PORT || 8068)

/** 注入登录态（须在 goto 之前调用） */
export async function loginAs(page, token = TOKENS.DEFAULT) {
    await page.addInitScript(t => localStorage.setItem('Admin-Token', t), token)
}

/** 清除登录态（须在 goto 之前调用） */
export async function logoutState(page) {
    await page.addInitScript(() => localStorage.removeItem('Admin-Token'))
}

/** 清除形态强制覆盖（须在 goto 之前调用） */
export async function clearForceMode(page) {
    await page.addInitScript(() => localStorage.removeItem('pnkx-force-mode'))
}

const ctlHeaders = token => ({ Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' })

/** 覆写某路由响应（body 或注入 status 错误码） */
export async function overrideRoute(token, method, path, body, status) {
    await fetch(`${MOCK}/__test/override`, {
        method: 'POST',
        headers: ctlHeaders(token),
        body: JSON.stringify({ method, path, body, status })
    })
}

/** 清除覆写 */
export async function clearOverride(token, method, path) {
    await fetch(`${MOCK}/__test/override`, {
        method: 'DELETE',
        headers: ctlHeaders(token),
        body: JSON.stringify({ method, path })
    })
}

/** 覆写 SSE 流脚本：chunks = [{data, event?, delay?}] */
export async function setStream(token, path, chunks) {
    await fetch(`${MOCK}/__test/stream`, {
        method: 'POST',
        headers: ctlHeaders(token),
        body: JSON.stringify({ path, chunks })
    })
}

/** 读取该 token 档案下捕获的请求日志（可按 path 子串过滤） */
export async function getRequests(token, { path = '', since = 0 } = {}) {
    const res = await fetch(`${MOCK}/__test/requests?path=${encodeURIComponent(path)}&since=${since}`, {
        headers: ctlHeaders(token)
    })
    const { requests } = await res.json()
    return requests
}

/** 断言某请求已被发出（含 body 深匹配子集） */
export async function waitForRequest(token, predicate, timeoutMs = 5000) {
    const start = Date.now()
    while (Date.now() - start < timeoutMs) {
        const reqs = await getRequests(token)
        const hit = reqs.find(predicate)
        if (hit) return hit
        await new Promise(r => setTimeout(r, 150))
    }
    throw new Error(`等待中的请求未出现（token=${token}）`)
}

/** 清空请求日志 */
export async function clearRequests(token) {
    await fetch(`${MOCK}/__test/requests`, { method: 'DELETE', headers: ctlHeaders(token) })
}
