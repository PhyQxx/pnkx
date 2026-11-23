/**
 * 房贷计算引擎单测
 * 运行：node tests/tools/loan.test.js
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

const { calcEqualPayment, calcEqualPrincipal, calcCombinedLoan, formatMoney } = loadEngine(
  'utils/tools/loan.js',
  ['calcEqualPayment', 'calcEqualPrincipal', 'calcCombinedLoan', 'formatMoney']
)

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

// 数值近似比较（两位小数精度）
function approx(actual, expected, label) {
  assert.ok(
    Math.abs(actual - expected) < 0.011,
    `${label}: 期望 ${expected}，实际 ${actual}`
  )
}

test('等额本息：84万 3.6% 360期 月供 3819.02', () => {
  const r = calcEqualPayment(840000, 3.6, 360)
  approx(r.monthlyPayment, 3819.02, '月供')
  // 总额按未取整月供累计（与银行口径一致，展示误差 < 1 元）
  approx(r.totalInterest, 534847.54, '利息总额')
  approx(r.totalPayment, 1374847.54, '还款总额')
})

test('等额本金：84万 3.6% 360期 首月 4853.33 月递减 7.00', () => {
  const r = calcEqualPrincipal(840000, 3.6, 360)
  approx(r.firstMonth, 4853.33, '首月月供')
  approx(r.monthlyDecrease, 7.0, '每月递减')
  approx(r.monthlyPrincipal, 2333.33, '每月本金')
  approx(r.totalInterest, 454860, '利息总额')
})

test('组合贷等额本息 = 两笔月供之和', () => {
  const ac = calcEqualPayment(500000, 3.6, 360)
  const af = calcEqualPayment(340000, 2.85, 360)
  const combined = calcCombinedLoan({
    commercial: { amount: 500000, annualRate: 3.6 },
    fund: { amount: 340000, annualRate: 2.85 },
    months: 360,
    method: 'ac'
  })
  approx(combined.month, ac.monthlyPayment + af.monthlyPayment, '合并月供')
  approx(combined.totalInterest, ac.totalInterest + af.totalInterest, '合并利息')
  approx(combined.monthlyDecrease, 0, '等额本息无递减')
})

test('组合贷等额本金 = 两笔首月/递减之和', () => {
  const c = calcEqualPrincipal(500000, 3.6, 360)
  const f = calcEqualPrincipal(340000, 2.85, 360)
  const combined = calcCombinedLoan({
    commercial: { amount: 500000, annualRate: 3.6 },
    fund: { amount: 340000, annualRate: 2.85 },
    months: 360,
    method: 'ab'
  })
  approx(combined.month, c.firstMonth + f.firstMonth, '合并首月')
  approx(combined.monthlyDecrease, c.monthlyDecrease + f.monthlyDecrease, '合并递减')
})

test('非法参数抛错', () => {
  assert.throws(() => calcEqualPayment(0, 3.6, 360), /本金/)
  assert.throws(() => calcEqualPayment(840000, 0, 360), /利率/)
  assert.throws(() => calcEqualPayment(840000, 3.6, 0), /期数/)
  assert.throws(() => calcEqualPayment(840000, 3.6, 12.5), /期数/)
  assert.throws(() => calcCombinedLoan({ months: 360, method: 'ac' }), /组合贷/)
})

test('金额千分位格式化', () => {
  assert.strictEqual(formatMoney(5180.66), '5,180.66')
  assert.strictEqual(formatMoney(0), '0.00')
  assert.strictEqual(formatMoney(1374838.6), '1,374,838.60')
  assert.strictEqual(formatMoney(-1234.5), '-1,234.50')
  assert.strictEqual(formatMoney(12.345), '12.35')
})

console.log('')
