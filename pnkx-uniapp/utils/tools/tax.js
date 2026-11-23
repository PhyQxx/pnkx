/**
 * 个税计算引擎（纯函数，与 UI 解耦，可直接单测）
 *
 * 口径说明：
 * - 按个人所得税「年度累计预扣预缴」简化估算：全年应纳税所得额一次性套年度税率表
 * - 年终奖按「单独计税」（奖金 ÷12 落月度税率表）与「并入综合所得」两种方式对比
 * - 金额单位均为「元」，返回值保留两位小数
 */

/** 起征点（每月减除费用） */
export const TAX_FREE_THRESHOLD = 5000

/** 综合所得年度税率表（年应纳税所得额） */
export const ANNUAL_TIERS = [
  { max: 36000, rate: 0.03, quick: 0 },
  { max: 144000, rate: 0.10, quick: 2520 },
  { max: 300000, rate: 0.20, quick: 16920 },
  { max: 420000, rate: 0.25, quick: 31920 },
  { max: 660000, rate: 0.30, quick: 52920 },
  { max: 960000, rate: 0.35, quick: 85920 },
  { max: Infinity, rate: 0.45, quick: 181920 }
]

/** 按月换算税率表（年终奖单独计税用：奖金 ÷12 套此表） */
export const MONTHLY_BONUS_TIERS = [
  { max: 3000, rate: 0.03, quick: 0 },
  { max: 12000, rate: 0.10, quick: 210 },
  { max: 25000, rate: 0.20, quick: 1410 },
  { max: 35000, rate: 0.25, quick: 2660 },
  { max: 55000, rate: 0.30, quick: 4410 },
  { max: 80000, rate: 0.35, quick: 7160 },
  { max: Infinity, rate: 0.45, quick: 15160 }
]

/**
 * 专项附加扣除项（月额度，2023 年后标准）
 * 房贷利息与住房租金只能二选一，互斥逻辑由页面交互控制
 */
export const SPECIAL_DEDUCTIONS = [
  { key: 'child', name: '子女教育', amount: 2000 },
  { key: 'elder', name: '赡养老人', amount: 3000 },
  { key: 'house', name: '房贷利息', amount: 1000 },
  { key: 'rent', name: '住房租金', amount: 1500 },
  { key: 'edu', name: '继续教育', amount: 400 }
]

/**
 * 保留两位小数
 * @param {number} value 数值
 * @returns {number}
 */
function round2(value) {
  return Math.round((value + Number.EPSILON) * 100) / 100
}

/**
 * 命中税率档
 * @param {number} taxable 应纳税所得额
 * @param {Array<{max:number, rate:number, quick:number}>} tiers 税率表
 * @returns {{index:number, tier:{max:number, rate:number, quick:number}}}
 */
export function findTier(taxable, tiers) {
  const index = tiers.findIndex(t => taxable <= t.max)
  const safeIndex = index === -1 ? tiers.length - 1 : index
  return { index: safeIndex, tier: tiers[safeIndex] }
}

/**
 * 按年度税率表计算全年个税
 * @param {number} annualTaxable 年应纳税所得额（>=0）
 * @returns {number} 全年税额
 */
function annualTaxOn(annualTaxable) {
  if (annualTaxable <= 0) return 0
  const { tier } = findTier(annualTaxable, ANNUAL_TIERS)
  return annualTaxable * tier.rate - tier.quick
}

/**
 * 工资个税（年度累计预扣简化口径）
 * @param {Object} params
 * @param {number} params.monthlySalary 税前月薪
 * @param {number} params.insuranceRate 五险一金个人比例（百分数，如 15 表示 15%）
 * @param {number} params.specialDeductions 专项附加扣除合计（每月）
 * @returns {{insurance:number, monthlyTaxable:number, annualTaxable:number, tierIndex:number, tier:Object, annualTax:number, monthlyTax:number, monthlyNet:number, annualNet:number, effectiveRate:number}}
 */
export function calcSalaryTax({ monthlySalary, insuranceRate, specialDeductions }) {
  if (!(monthlySalary >= 0)) {
    throw new Error('月薪不能为负数')
  }
  const insurance = monthlySalary * (insuranceRate / 100)
  const monthlyTaxable = monthlySalary - insurance - TAX_FREE_THRESHOLD - specialDeductions
  const annualTaxable = Math.max(0, monthlyTaxable * 12)
  const { index, tier } = findTier(annualTaxable, ANNUAL_TIERS)
  const annualTax = annualTaxOn(annualTaxable)
  const monthlyTax = annualTax / 12
  const annualSalary = monthlySalary * 12
  const annualInsurance = insurance * 12
  return {
    insurance: round2(insurance),
    monthlyTaxable: round2(monthlyTaxable),
    annualTaxable: round2(annualTaxable),
    tierIndex: index,
    tier,
    annualTax: round2(annualTax),
    monthlyTax: round2(monthlyTax),
    monthlyNet: round2(monthlySalary - insurance - monthlyTax),
    annualNet: round2(annualSalary - annualInsurance - annualTax),
    effectiveRate: annualSalary > 0 ? annualTax / annualSalary : 0
  }
}

/**
 * 年终奖计税：单独计税 vs 并入综合所得，返回更优方案
 * @param {number} bonus 年终奖金额
 * @param {number} annualTaxable 工资部分年应纳税所得额（用于并入口径对比）
 * @returns {{separateTax:number, separateNet:number, mergedIncrement:number, mergedNet:number, better:'separate'|'merged', bonusTier:Object}}
 */
export function calcBonusTax(bonus, annualTaxable) {
  if (!(bonus >= 0)) {
    throw new Error('年终奖不能为负数')
  }
  // 单独计税：奖金 ÷12 落月度税率表
  const { tier } = findTier(bonus / 12, MONTHLY_BONUS_TIERS)
  const separateTax = bonus > 0 ? bonus * tier.rate - tier.quick : 0
  const separateNet = bonus - separateTax
  // 并入综合所得：只算增量税额（工资部分税额不重复计入）
  const mergedIncrement = Math.max(0, annualTaxOn(annualTaxable + bonus) - annualTaxOn(annualTaxable))
  const mergedNet = bonus - mergedIncrement
  return {
    separateTax: round2(separateTax),
    separateNet: round2(separateNet),
    mergedIncrement: round2(mergedIncrement),
    mergedNet: round2(mergedNet),
    better: separateTax <= mergedIncrement ? 'separate' : 'merged',
    bonusTier: tier
  }
}
