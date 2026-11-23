/**
 * 个税计算引擎单测
 * 运行：node tests/tools/tax.test.js
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

const { calcSalaryTax, calcBonusTax, findTier, ANNUAL_TIERS, MONTHLY_BONUS_TIERS, SPECIAL_DEDUCTIONS } = loadEngine(
  'utils/tools/tax.js',
  ['calcSalaryTax', 'calcBonusTax', 'findTier', 'ANNUAL_TIERS', 'MONTHLY_BONUS_TIERS', 'SPECIAL_DEDUCTIONS']
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

test('税率表完整性（7 档，速算扣除数正确）', () => {
  assert.strictEqual(ANNUAL_TIERS.length, 7)
  assert.strictEqual(MONTHLY_BONUS_TIERS.length, 7)
  assert.strictEqual(ANNUAL_TIERS[1].quick, 2520)
  assert.strictEqual(ANNUAL_TIERS[6].rate, 0.45)
  assert.strictEqual(MONTHLY_BONUS_TIERS[1].quick, 210)
})

test('月薪 2 万 无扣除：年应税 144000 命中 10% 档，年税 11880', () => {
  const r = calcSalaryTax({ monthlySalary: 20000, insuranceRate: 15, specialDeductions: 0 })
  approx(r.annualTaxable, 144000, '年应纳税所得额')
  assert.strictEqual(r.tier.rate, 0.10)
  approx(r.annualTax, 11880, '全年个税')
  approx(r.monthlyNet, 16010, '月到手')
  approx(r.effectiveRate, 0.0495, '税负率')
})

test('月薪 2 万 + 赡养老人 3000 + 房贷利息 1000：年应税 96000 年税 7080', () => {
  const r = calcSalaryTax({ monthlySalary: 20000, insuranceRate: 15, specialDeductions: 4000 })
  approx(r.annualTaxable, 96000, '年应纳税所得额')
  approx(r.annualTax, 7080, '全年个税')
  approx(r.monthlyNet, 16410, '月到手')
})

test('免税边界：月薪 5000 五险 10%，应税 0 税额 0', () => {
  const r = calcSalaryTax({ monthlySalary: 5000, insuranceRate: 10, specialDeductions: 0 })
  assert.strictEqual(r.annualTaxable, 0)
  assert.strictEqual(r.annualTax, 0)
  assert.strictEqual(r.tier.rate, 0.03) // 空表命中第一档，但税额为 0
  approx(r.monthlyNet, 4500, '月到手')
})

test('五险一金为 0 时正常计算', () => {
  const r = calcSalaryTax({ monthlySalary: 8000, insuranceRate: 0, specialDeductions: 0 })
  approx(r.annualTaxable, 36000, '年应纳税所得额')
  approx(r.annualTax, 1080, '全年个税')
})

test('年终奖 4 万单独计税 3790，且优于并入综合所得', () => {
  const r = calcBonusTax(40000, 144000)
  approx(r.separateTax, 3790, '单独计税税额')
  approx(r.mergedIncrement, 8000, '并入综合所得增量')
  assert.strictEqual(r.better, 'separate')
  approx(r.separateNet, 36210, '单独计税到手')
})

test('年终奖 3.6 万落 3% 档（36000/12=3000 边界）', () => {
  const r = calcBonusTax(36000, 0)
  approx(r.separateTax, 1080, '单独计税税额')
  assert.strictEqual(r.bonusTier.rate, 0.03)
})

test('低收入高奖金场景并入更优时返回 merged', () => {
  // 工资应税为 0 时，并入增量按年度表从 0 起算
  // bonus=20000：单独=20000*3%=600；并入=20000*3%=600 → 相等，取 separate（<=）
  const r1 = calcBonusTax(20000, 0)
  assert.strictEqual(r1.better, 'separate')
  // bonus=100000：单独=100000*10%-210=9790；并入=100000*10%-2520=7480 → merged 更优
  const r2 = calcBonusTax(100000, 0)
  approx(r2.mergedIncrement, 7480, '并入增量')
  assert.strictEqual(r2.better, 'merged')
})

test('非法参数抛错', () => {
  assert.throws(() => calcSalaryTax({ monthlySalary: -1, insuranceRate: 15, specialDeductions: 0 }), /月薪/)
  assert.throws(() => calcBonusTax(-100, 0), /年终奖/)
})

test('专项附加扣除配置（2023 后标准，房贷/租金并存待互斥）', () => {
  const byKey = Object.fromEntries(SPECIAL_DEDUCTIONS.map(d => [d.key, d.amount]))
  assert.strictEqual(byKey.child, 2000)
  assert.strictEqual(byKey.elder, 3000)
  assert.strictEqual(byKey.house, 1000)
  assert.strictEqual(byKey.rent, 1500)
  assert.strictEqual(byKey.edu, 400)
})

test('findTier 边界值命中（恰好等于档位上限）', () => {
  assert.strictEqual(findTier(36000, ANNUAL_TIERS).tier.rate, 0.03)
  assert.strictEqual(findTier(36001, ANNUAL_TIERS).tier.rate, 0.10)
  assert.strictEqual(findTier(-5, ANNUAL_TIERS).tier.rate, 0.03)
})

console.log('')
