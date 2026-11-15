/**
 * E2E Mock Server（监听 :8068，vite dev 的 /dev-api 代理天然指向这里）
 *
 * 设计：
 * - 静态 fixtures：tests/mock-server/fixtures/*.json，格式 { "METHOD /path": responseBody }
 *   - 路径可含 :param 段（如 "GET /bookkeeping/account/:id"）按段匹配
 *   - 响应体自动包 RuoYi 信封：body 自带 code 则原样返回，否则 { code:200, msg:'操作成功', ...body }
 *   - 流式端点用 "STREAM /path" 声明，默认脚本见 sse.mjs，可用 __test/stream 覆写
 * - token 档案（Authorization: Bearer <token>）：
 *   - test-token          → V2 功能开关全关
 *   - test-token-all      → V2 全开
 *   - test-token-ac2/op2/ci2/sp2/ra2 → 对应单项开
 *   - test-token-<自定义>   → 全关（各 spec 自取专属 token 隔离覆写/日志）
 *   - expired-token       → /getInfo 返回 401（测登录过期）
 * - 测试控制面（__test/*，按 token 隔离）：
 *   - POST /__test/override {method,path,body|status}   覆写某路由响应（可注入 500/401）
 *   - DELETE /__test/override {method,path}             清除覆写
 *   - POST /__test/stream {path,chunks:[{data,event,delay}]} 覆写 SSE 脚本
 *   - GET  /__test/requests?path=&since=                读取请求日志（含 query/body 摘要）
 *   - DELETE /__test/requests                           清空日志
 */
import http from 'node:http'
import { readdirSync, readFileSync } from 'node:fs'
import { buildStreamScript, writeSseFrames } from './sse.mjs'

const PORT = Number(process.env.MOCK_PORT || 8068)

// ---------- fixtures 加载（文件名排序，后加载覆盖同 key，保证确定性） ----------
const routes = new Map() // "METHOD /path" -> body | {__stream:true}
const fixturesDir = new URL('./fixtures/', import.meta.url)
for (const file of readdirSync(fixturesDir).filter(f => f.endsWith('.json')).sort()) {
    const json = JSON.parse(readFileSync(new URL(file, fixturesDir), 'utf8'))
    for (const [key, body] of Object.entries(json)) {
        routes.set(key, body)
    }
}

// ---------- 路由匹配 ----------
function compilePattern(pattern) {
    const segs = pattern.split('/').map(s => (s.startsWith(':') ? '([^/]+)' : s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
    return new RegExp('^' + segs.join('/') + '/?$')
}
const compiled = [...routes.keys()].map(key => {
    const [method, ...rest] = key.split(' ')
    // "STREAM /path" 声明的流式端点按 GET 匹配（fetch/EventSource 均为 GET）
    const m = method === 'STREAM' ? 'GET' : method
    return { key, method: m, path: rest.join(' '), re: compilePattern(rest.join(' ')) }
})
function matchRoute(method, pathname) {
    let patternHit = null
    for (const r of compiled) {
        if (r.method !== method) continue
        if (r.path === pathname) return { body: routes.get(r.key), pattern: false }
        if (!patternHit && r.re.test(pathname)) patternHit = { body: routes.get(r.key), pattern: true }
    }
    return patternHit
}

// ---------- token 档案 ----------
const FEATURES = {
    actionCenterV2: ['test-token-all', 'test-token-ac2'],
    operationPlanV2: ['test-token-all', 'test-token-op2'],
    chargeInstanceV2: ['test-token-all', 'test-token-ci2'],
    shoppingPlanV2: ['test-token-all', 'test-token-sp2'],
    reportActionV2: ['test-token-all', 'test-token-ra2']
}
function tokenOf(req) {
    const auth = req.headers.authorization || ''
    return auth.startsWith('Bearer ') ? auth.slice(7) : '(anonymous)'
}
function featuresFor(token) {
    const flags = {}
    for (const [flag, tokens] of Object.entries(FEATURES)) flags[flag] = tokens.includes(token)
    return flags
}

// ---------- 按 token 的运行时状态 ----------
function makeState() {
    return { overrides: new Map(), streams: new Map(), requests: [], seq: 0 }
}
const states = new Map()
const stateOf = token => {
    if (!states.has(token)) states.set(token, makeState())
    return states.get(token)
}

// ---------- 工具 ----------
function send(res, status, obj) {
    const body = JSON.stringify(obj)
    res.writeHead(status, { 'Content-Type': 'application/json;charset=utf-8', 'Content-Length': Buffer.byteLength(body) })
    res.end(body)
}
const envelope = body => (body && typeof body === 'object' && 'code' in body ? body : { code: 200, msg: '操作成功', ...body })

function readBody(req) {
    return new Promise(resolve => {
        let raw = ''
        req.on('data', c => { raw += c })
        req.on('end', () => {
            if (!raw) return resolve({})
            try { resolve(JSON.parse(raw)) } catch { resolve({ _raw: raw }) }
        })
    })
}

const server = http.createServer(async (req, res) => {
    const url = new URL(req.url, 'http://localhost')
    const pathname = decodeURIComponent(url.pathname)
    const token = tokenOf(req)
    const st = stateOf(token)

    // ---- 测试控制面 ----
    if (pathname.startsWith('/__test/')) {
        if (req.method === 'POST' && pathname === '/__test/override') {
            const { method, path, body, status } = await readBody(req)
            st.overrides.set(`${method} ${path}`, { body, status })
            return send(res, 200, { code: 200 })
        }
        if (req.method === 'DELETE' && pathname === '/__test/override') {
            const { method, path } = await readBody(req)
            st.overrides.delete(`${method} ${path}`)
            return send(res, 200, { code: 200 })
        }
        if (req.method === 'POST' && pathname === '/__test/stream') {
            const { path, chunks } = await readBody(req)
            st.streams.set(`STREAM ${path}`, buildStreamScript(chunks))
            return send(res, 200, { code: 200 })
        }
        if (req.method === 'GET' && pathname === '/__test/requests') {
            const since = Number(url.searchParams.get('since') || 0)
            const pathFilter = url.searchParams.get('path') || ''
            return send(res, 200, {
                requests: st.requests
                    .filter(r => r.seq > since && (!pathFilter || r.path.includes(pathFilter)))
            })
        }
        if (req.method === 'DELETE' && pathname === '/__test/requests') {
            st.requests = []
            return send(res, 200, { code: 200 })
        }
        return send(res, 404, { code: 404, msg: 'unknown __test endpoint' })
    }

    // ---- 请求日志 ----
    const entry = { seq: ++st.seq, method: req.method, path: pathname, query: url.searchParams.toString() }
    if (req.method !== 'GET' && req.method !== 'DELETE') {
        const b = await readBody(req)
        entry.body = b
        req.__loggedBody = b
    } else {
        entry.query = Object.fromEntries(url.searchParams.entries())
    }
    if (st.requests.length > 500) st.requests.shift()
    st.requests.push(entry)

    // ---- 特殊业务：登录 / 用户信息 ----
    if (req.method === 'POST' && pathname === '/login') {
        const body = entry.body || {}
        const username = body.username ?? body.userName
        if (username === 'admin' && body.password === 'admin123') {
            return send(res, 200, { code: 200, msg: '登录成功', token: 'test-token' })
        }
        return send(res, 200, { code: 500, msg: '用户名或密码错误' })
    }
    if (req.method === 'GET' && pathname === '/captchaImage') {
        return send(res, 200, { code: 200, captchaEnabled: false, uuid: 'mock-uuid' })
    }
    if (req.method === 'GET' && pathname === '/getInfo') {
        if (token === 'expired-token') {
            return send(res, 200, { code: 401, msg: '登录状态已过期' })
        }
        return send(res, 200, {
            code: 200,
            msg: '操作成功',
            permissions: ['*:*:*'],
            roles: ['admin'],
            isDefaultModifyPwd: false,
            isPasswordExpired: false,
            user: { userId: 1, userName: 'admin', nickName: '测试用户', avatar: '', phonenumber: '13800000000', email: 'test@pnkx.top', sex: '0', createTime: '2026-01-01 00:00:00', admin: true }
        })
    }
    if (req.method === 'GET' && pathname === '/life/features') {
        return send(res, 200, { code: 200, data: featuresFor(token) })
    }
    if (token === 'expired-token') {
        return send(res, 200, { code: 401, msg: '登录状态已过期' })
    }

    // ---- 覆写优先 ----
    const overrideKey = [...st.overrides.keys()].find(k => {
        const [m, p] = k.split(' ')
        if (m !== req.method) return false
        return p === pathname || compilePattern(p).test(pathname)
    })
    if (overrideKey) {
        const ov = st.overrides.get(overrideKey)
        if (ov.status) return send(res, ov.status, { code: ov.status, msg: 'mock error' })
        return send(res, 200, envelope(ov.body))
    }

    // ---- 静态路由 ----
    const hit = matchRoute(req.method, pathname)
    if (!hit) {
        return send(res, 200, { code: 200, msg: '操作成功', rows: [], total: 0, data: null, __unmocked: pathname })
    }
    const body = hit.body
    if (typeof body === 'object' && body !== null && body.__stream) {
        const script = st.streams.get(`STREAM ${pathname}`) || buildStreamScript(body.chunks)
        res.writeHead(200, { 'Content-Type': 'text/event-stream;charset=utf-8', 'Cache-Control': 'no-cache', Connection: 'keep-alive' })
        return writeSseFrames(res, script).then(() => res.end())
    }
    return send(res, 200, envelope(body))
})

server.on('error', err => {
    if (err.code === 'EADDRINUSE') {
        console.error('[mock-server] 端口 8068 被占用：请先停掉本地后端或占用进程（E2E 需要独占该端口）')
        process.exit(1)
    }
    throw err
})

server.listen(PORT, () => {
    console.log(`[mock-server] listening on :${PORT}, routes=${routes.size}`)
})
