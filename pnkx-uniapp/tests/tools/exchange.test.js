/**
 * 汇率换算引擎单测
 * 运行：node tests/tools/exchange.test.js
 */
const fs = require('fs')
const path = require('path')
const assert = require('assert')

// utils 目录为 ESM 语法，项目无构建环境，这里读源码去 export 后执行加载
function loadEngine(relPath, exportNames) {
  const src = fs.readFileSync(path.resolve(__dirname, '../..', relPath), 'utf8')
  const body = src.replace(/export\s+/g, '')
  return new Function(`${body}\nreturn { ${exportNames.join(', ')} }`)()
}

const { convertAmount, formatRate } = loadEngine('utils/tools/exchange.js', ['convertAmount', 'formatRate'])

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

// 以 CNY 为基准的示例牌价表
const RATES = { CNY: 1, USD: 0.14, EUR: 0.13, JPY: 21.5, HKD: 1.09 }

test('CNY → USD：100 元 ≈ 14 美元', () => {
  const result = convertAmount({ amount: 100, rates: RATES, from: 'CNY', to: 'USD' })
  assert.strictEqual(result, 14)
})

test('交叉换算 USD → JPY：经基准 CNY 交叉', () => {
  // 1 USD = 1/0.14 CNY = 21.5/0.14 JPY ≈ 153.5714
  const result = convertAmount({ amount: 1, rates: RATES, from: 'USD', to: 'JPY' })
  assert.ok(Math.abs(result - 153.5714) < 0.001, `期望 153.5714，实际 ${result}`)
})

test('相同币种换算金额不变', () => {
  const result = convertAmount({ amount: 88.88, rates: RATES, from: 'CNY', to: 'CNY' })
  assert.strictEqual(result, 88.88)
})

test('反向换算 JPY → CNY 往返近似还原', () => {
  const jpy = convertAmount({ amount: 100, rates: RATES, from: 'CNY', to: 'JPY' })
  const back = convertAmount({ amount: jpy, rates: RATES, from: 'JPY', to: 'CNY' })
  assert.ok(Math.abs(back - 100) < 0.1, `往返还原期望 ≈100，实际 ${back}`)
})

test('不支持的币种抛错', () => {
  assert.throws(() => convertAmount({ amount: 100, rates: RATES, from: 'CNY', to: 'XXX' }), /不支持/)
  assert.throws(() => convertAmount({ amount: 100, rates: RATES, from: 'GBP', to: 'CNY' }), /不支持/)
})

test('负数金额抛错', () => {
  assert.throws(() => convertAmount({ amount: -1, rates: RATES, from: 'CNY', to: 'USD' }), /负数/)
})

test('汇率显示格式化：去尾零最多 4 位', () => {
  assert.strictEqual(formatRate(153.571429), '153.5714')
  assert.strictEqual(formatRate(14), '14')
  assert.strictEqual(formatRate(0.14), '0.14')
  assert.strictEqual(formatRate(1.0900), '1.09')
})

console.log('')
