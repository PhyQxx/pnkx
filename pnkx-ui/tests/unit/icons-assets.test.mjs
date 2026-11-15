import test from 'node:test'
import assert from 'node:assert/strict'
import { readdirSync, readFileSync } from 'node:fs'

/**
 * 业务 SVG 图标资产检查（icons.js 依赖 import.meta.glob，无法在 node 里直接导入，
 * 以 fs 级断言 + 源码级断言替代）
 */

const svgDir = new URL('../../src/mobile/assets/svg/', import.meta.url)
const files = readdirSync(svgDir).filter(f => f.endsWith('.svg')).map(f => f.replace(/\.svg$/, ''))

test('业务 SVG 图标齐全（≥260 个，源自 uniapp staticPackage）', () => {
    assert.ok(files.length >= 260, `图标数量异常: ${files.length}`)
})

test('四类前缀图标齐备（c- 记账分类 / w- 天气 / x- 心情 / a- 账户）', () => {
    for (const prefix of ['c-', 'w-', 'x-', 'a-']) {
        const n = files.filter(f => f.startsWith(prefix)).length
        assert.ok(n > 5, `${prefix}* 图标过少: ${n}`)
    }
})

test('图标名唯一且 icons.js 以 eager glob 收集', () => {
    assert.equal(new Set(files).size, files.length, '存在重名图标')
    const src = readFileSync(new URL('../../src/mobile/utils/icons.js', import.meta.url), 'utf8')
    assert.ok(src.includes("import.meta.glob('../assets/svg/*.svg'"), 'icons.js 未使用 eager glob 收集')
    assert.ok(src.includes('eager: true'), 'icons.js glob 必须 eager')
})
