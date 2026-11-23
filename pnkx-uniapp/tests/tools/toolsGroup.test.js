/**
 * 工具箱分组顺序单测
 * 运行：node tests/tools/toolsGroup.test.js
 * 验证 pages/tools.vue groupedTools：分组区块顺序跟随后台配置数组中各组首次出现顺序
 */
const fs = require('fs')
const path = require('path')
const assert = require('assert')

// pages/tools.vue 为 .vue 单文件组件，无构建环境；读源码提取 computed 与常量后执行
const src = fs.readFileSync(path.resolve(__dirname, '../../pages/tools.vue'), 'utf8')

const toolGroupsSrc = src.match(/const TOOL_GROUPS = (\[[\s\S]*?\n\])/)
assert.ok(toolGroupsSrc, '未找到 TOOL_GROUPS 定义')
const toolGroups = new Function(`return ${toolGroupsSrc[1]}`)()

const fnMatch = src.match(/groupedTools\(\)\s*\{([\s\S]*?)\n    \},/)
assert.ok(fnMatch, '未找到 groupedTools 计算属性')
const body = fnMatch[1].replace(/this\.toolList/g, 'toolList')
const compute = new Function('toolList', 'TOOL_GROUPS', body)

function test(name, fn) {
  try {
    fn()
    console.log(`ok - ${name}`)
  } catch (error) {
    console.error(`not ok - ${name}`)
    console.error(error.message)
    process.exitCode = 1
  }
}

const T = {
  wallpaper: { id: 'wallpaper', name: '壁纸下载', group: 'life' },
  mortgage: { id: 'mortgage', name: '房贷计算器', group: 'finance' },
  tax: { id: 'tax', name: '个税计算器', group: 'finance' },
  whatToEat: { id: 'what-to-eat', name: '今天吃什么', group: 'fun' },
  blog: { id: 'blog', name: '博客后台' } // 老/新增配置可能无 group
}

test('后台把壁纸移到第 1 位 → 生活工具区块排到最前（本次修复场景）', () => {
  const groups = compute([T.wallpaper, T.mortgage, T.tax, T.whatToEat, T.blog], toolGroups)
  assert.deepStrictEqual(groups.map(g => g.key), ['life', 'finance', 'fun', 'default'])
  assert.strictEqual(groups[0].label, '生活工具')
  assert.strictEqual(groups[0].tools[0].id, 'wallpaper')
})

test('迁移脚本初始顺序 → 财务计算在前，组内保持数组顺序', () => {
  const groups = compute([T.mortgage, T.tax, T.whatToEat, T.wallpaper, T.blog], toolGroups)
  assert.deepStrictEqual(groups.map(g => g.key), ['finance', 'fun', 'life', 'default'])
  assert.deepStrictEqual(groups[0].tools.map(t => t.id), ['mortgage', 'tax'])
})

test('后台交换同组内两个工具 → 组内顺序跟随交换', () => {
  const groups = compute([T.tax, T.mortgage, T.wallpaper], toolGroups)
  const finance = groups.find(g => g.key === 'finance')
  assert.deepStrictEqual(finance.tools.map(t => t.id), ['tax', 'mortgage'])
})

test('老配置无 group 字段 → 全部归入常用工具单区块', () => {
  const oldList = [
    { id: 'wallpaper', name: '壁纸下载' },
    { id: 'blog', name: '博客后台' }
  ]
  const groups = compute(oldList, toolGroups)
  assert.deepStrictEqual(groups.map(g => g.key), ['default'])
  assert.strictEqual(groups[0].label, '常用工具')
  assert.deepStrictEqual(groups[0].tools.map(t => t.id), ['wallpaper', 'blog'])
})

test('未知 group 值 → 归入常用工具，不拆出重复区块', () => {
  const groups = compute([T.wallpaper, { ...T.blog, group: 'xyz' }], toolGroups)
  assert.deepStrictEqual(groups.map(g => g.key), ['life', 'default'])
  assert.deepStrictEqual(groups[1].tools.map(t => t.id), ['blog'])
})

test('分组标题与主题色映射正确', () => {
  const groups = compute([T.mortgage, T.wallpaper, T.whatToEat], toolGroups)
  const finance = groups.find(g => g.key === 'finance')
  assert.strictEqual(finance.label, '财务计算')
  assert.strictEqual(finance.accent, '#6C9EFF')
  const life = groups.find(g => g.key === 'life')
  assert.strictEqual(life.label, '生活工具')
  assert.strictEqual(life.accent, '#FB923C')
  const fun = groups.find(g => g.key === 'fun')
  assert.strictEqual(fun.label, '趣味 AI')
})
