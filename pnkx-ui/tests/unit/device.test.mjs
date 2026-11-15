import test from 'node:test'
import assert from 'node:assert/strict'

/**
 * utils/device.js 单元测试
 *
 * device.js 是模块级单例（视口/强制覆盖都是模块内 ref），因此：
 * - 在模块顶层先注入 window/matchMedia/localStorage stub（宽屏初始），
 *   再动态 import（直接走文件 URL，使裸导入 'vue' 能按 node_modules 解析）
 * - 用 exported setter + 视口事件驱动状态翻转
 */

function makeEnv({ width = 1280 } = {}) {
    const store = new Map()
    const listeners = []
    const mql = {
        matches: width < 768,
        addEventListener: (_, cb) => listeners.push(cb),
        removeEventListener: () => {},
        addListener: cb => listeners.push(cb)
    }
    globalThis.window = {
        innerWidth: width,
        matchMedia: () => mql,
        location: { search: '' }
    }
    globalThis.matchMedia = () => mql
    globalThis.localStorage = {
        getItem: k => (store.has(k) ? store.get(k) : null),
        setItem: (k, v) => store.set(k, String(v)),
        removeItem: k => store.delete(k)
    }
    return {
        fireViewport(widthNow) {
            mql.matches = widthNow < 768
            listeners.forEach(cb => cb({ matches: mql.matches }))
        },
        store
    }
}

const env = makeEnv({ width: 1280 })
const device = await import(new URL('../../src/utils/device.js', import.meta.url))

test('宽屏默认 PC 模式，窄屏判定为移动视口', () => {
    assert.equal(device.resolveMode(), 'pc')
    assert.equal(device.isMobileViewport(), false)
    assert.equal(device.modeRef.value, 'pc')
})

test('强制覆盖优先于视口：宽屏强制 mobile', () => {
    device.setForcedMode('mobile')
    assert.equal(device.resolveMode(), 'mobile')
    assert.equal(device.getForcedMode(), 'mobile')
})

test('清除强制覆盖后恢复自动识别（宽屏→pc）', () => {
    device.setForcedMode(null)
    assert.equal(device.getForcedMode(), null)
    assert.equal(device.resolveMode(), 'pc')
})

test('断点跨越 768px 时 mobileViewport/modeRef 响应式翻转', () => {
    assert.equal(device.resolveMode(), 'pc')
    env.fireViewport(375)
    assert.equal(device.isMobileViewport(), true)
    assert.equal(device.resolveMode(), 'mobile')
    assert.equal(device.modeRef.value, 'mobile')
    env.fireViewport(1024)
    assert.equal(device.resolveMode(), 'pc')
})

test('强制覆盖优先于视口：窄屏强制 pc', () => {
    env.fireViewport(375)
    device.setForcedMode('pc')
    assert.equal(device.resolveMode(), 'pc')
    device.setForcedMode(null)
})

test('setForcedMode 非法值视为清除', () => {
    env.fireViewport(375)
    device.setForcedMode('mobile')
    device.setForcedMode('whatever')
    assert.equal(device.getForcedMode(), null)
    assert.equal(device.resolveMode(), 'mobile')
})

test('localStorage 不可用时静默降级（不抛异常）', () => {
    globalThis.localStorage = {
        getItem: () => { throw new Error('denied') },
        setItem: () => { throw new Error('denied') },
        removeItem: () => { throw new Error('denied') }
    }
    device.setForcedMode('pc') // 不应抛
    assert.equal(device.resolveMode(), 'pc')
    device.setForcedMode(null)
})
