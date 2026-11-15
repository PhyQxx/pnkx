import test from 'node:test'
import assert from 'node:assert/strict'
import {
    isMobilePath,
    resolveMobileTarget,
    resolvePcTarget,
    uniappPathToMobile
} from '../../src/mobile/router/map.js'
import { NEUTRAL_PATHS } from '../../src/mobile/router/guards.js'

test('isMobilePath：/m 前缀识别', () => {
    assert.equal(isMobilePath('/m'), true)
    assert.equal(isMobilePath('/m/home'), true)
    assert.equal(isMobilePath('/index'), false)
    assert.equal(isMobilePath('/mytool'), false) // 无前导斜杠 m 误伤防护
})

test('resolveMobileTarget：精确表命中', () => {
    assert.deepEqual(resolveMobileTarget('/index'), { path: '/m/home', fallback: false })
    assert.deepEqual(resolveMobileTarget('/user/profile'), { path: '/m/mine', fallback: false })
    assert.deepEqual(resolveMobileTarget('/ai/tools'), { path: '/m/ai/chat', fallback: false })
})

test('resolveMobileTarget：关键词匹配两代 PC 菜单前缀', () => {
    assert.equal(resolveMobileTarget('/px/life/todo').path, '/m/todo')
    assert.equal(resolveMobileTarget('/mytool/todo').path, '/m/todo')
    assert.equal(resolveMobileTarget('/px/life/bookkeeping/record').path, '/m/bookkeeping/record')
    assert.equal(resolveMobileTarget('/mytool/diary').path, '/m/diary')
})

test('resolveMobileTarget：长词优先（bookkeeping 先于 book）', () => {
    assert.equal(resolveMobileTarget('/px/life/bookkeeping/account').path, '/m/bookkeeping/account')
    assert.notEqual(resolveMobileTarget('/px/app/book').path, '/m/bookkeeping')
})

test('resolveMobileTarget：无映射时回落 /m/home 并标记 fallback', () => {
    const r = resolveMobileTarget('/tool/gen')
    assert.deepEqual(r, { path: '/m/home', fallback: true })
})

test('resolveMobileTarget：note 关键词不误伤 note-share（精确表放行）', () => {
    const r = resolveMobileTarget('/note-share')
    assert.equal(r.path, '/note-share')
    assert.equal(r.fallback, false)
})

test('resolvePcTarget：精确命中', () => {
    assert.equal(resolvePcTarget('/m/home'), '/index')
    assert.equal(resolvePcTarget('/m/mine/setting/family'), '/system/dataGroup')
    assert.equal(resolvePcTarget('/m/todo'), '/px/life/todo')
})

test('resolvePcTarget：未知路径剥尾段再试，最终回落 /index', () => {
    assert.equal(resolvePcTarget('/m/todo/unknown-sub'), '/px/life/todo')
    assert.equal(resolvePcTarget('/m/never-registered'), '/index')
})

test('uniappPathToMobile：主包/子包全量映射', () => {
    assert.equal(uniappPathToMobile('pages/index'), '/m/home')
    assert.equal(uniappPathToMobile('pages_life/bookkeeping/record/add'), '/m/bookkeeping/record/add')
    assert.equal(uniappPathToMobile('/pages_life/ai/chat'), '/m/ai/chat')
    assert.equal(uniappPathToMobile('pages_system/mine/setting/family'), '/m/mine/setting/family')
    assert.equal(uniappPathToMobile('pages_system/search/index'), '/m/search')
})

test('uniappPathToMobile：带 query 与未识别路径', () => {
    assert.equal(uniappPathToMobile('pages_life/todo/edit?id=1'), '/m/todo/edit')
    assert.equal(uniappPathToMobile('pages/unknown'), null)
    assert.equal(uniappPathToMobile(null), null)
})

test('NEUTRAL_PATHS 含登录与免登录分享页', () => {
    ;['/login', '/auth-redirect', '/bind', '/register', '/note-share', '/404', '/401'].forEach(p =>
        assert.ok(NEUTRAL_PATHS.includes(p), `缺少 ${p}`)
    )
})
