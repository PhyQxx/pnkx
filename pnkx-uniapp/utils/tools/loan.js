/**
 * 房贷计算引擎（纯函数，与 UI 解耦，可直接单测）
 *
 * 约定：
 * - principal / 金额单位为「元」，annualRate 为百分数（如 3.6 表示 3.6%），months 为总期数
 * - 所有返回金额保留两位小数（四舍五入）
 */

/**
 * 保留两位小数
 * @param {number} value 数值
 * @returns {number}
 */
function round2(value) {
  return Math.round((value + Number.EPSILON) * 100) / 100
}

/**
 * 校验贷款参数合法性
 * @param {number} principal 贷款本金（元）
 * @param {number} annualRate 年利率（百分数）
 * @param {number} months 期数
 */
function assertLoanParams(principal, annualRate, months) {
  if (!(principal > 0)) {
    throw new Error('贷款本金必须大于 0')
  }
  if (!(annualRate > 0 && annualRate < 100)) {
    throw new Error('年利率需在 (0, 100) 范围内')
  }
  if (!(months > 0 && Number.isInteger(months))) {
    throw new Error('期数必须为正整数')
  }
}

/**
 * 千分位格式化金额（保留两位小数）
 * 相比 toLocaleString，手动拼接在低版本 Android WebView 上兼容性更好
 * @param {number} value 金额（元）
 * @returns {string} 如 5,180.66
 */
export function formatMoney(value) {
  const negative = value < 0
  const abs = Math.abs(round2(value))
  const intPart = Math.floor(abs)
  const decPart = Math.round((abs - intPart) * 100)
  const intStr = String(intPart).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  const decStr = decPart < 10 ? '0' + decPart : String(decPart)
  return (negative ? '-' : '') + intStr + '.' + decStr
}

/**
 * 等额本息：每月固定还款
 * 公式：M = P × r × (1+r)^n / ((1+r)^n − 1)，r = 年利率/12
 * @param {number} principal 贷款本金（元）
 * @param {number} annualRate 年利率（百分数，如 3.6）
 * @param {number} months 总期数（月）
 * @returns {{monthlyPayment:number, totalInterest:number, totalPayment:number}}
 */
export function calcEqualPayment(principal, annualRate, months) {
  assertLoanParams(principal, annualRate, months)
  const r = annualRate / 100 / 12
  const pow = Math.pow(1 + r, months)
  const monthlyPayment = principal * r * pow / (pow - 1)
  const totalPayment = monthlyPayment * months
  return {
    monthlyPayment: round2(monthlyPayment),
    totalInterest: round2(totalPayment - principal),
    totalPayment: round2(totalPayment)
  }
}

/**
 * 等额本金：每月还固定本金 + 剩余本金利息，月供逐月递减
 * 首月 = P/n + P×r；每月递减 = (P/n)×r；总利息 = (n+1)×P×r/2
 * @param {number} principal 贷款本金（元）
 * @param {number} annualRate 年利率（百分数）
 * @param {number} months 总期数（月）
 * @returns {{firstMonth:number, monthlyDecrease:number, monthlyPrincipal:number, totalInterest:number, totalPayment:number}}
 */
export function calcEqualPrincipal(principal, annualRate, months) {
  assertLoanParams(principal, annualRate, months)
  const r = annualRate / 100 / 12
  const monthlyPrincipal = principal / months
  const firstMonth = monthlyPrincipal + principal * r
  const monthlyDecrease = monthlyPrincipal * r
  const totalInterest = (months + 1) * principal * r / 2
  return {
    firstMonth: round2(firstMonth),
    monthlyDecrease: round2(monthlyDecrease),
    monthlyPrincipal: round2(monthlyPrincipal),
    totalInterest: round2(totalInterest),
    totalPayment: round2(principal + totalInterest)
  }
}

/**
 * 组合贷：商贷 + 公积金分别计算后合并
 * - 等额本息：两笔月供直接相加
 * - 等额本金：两笔首月相加、每月递减相加
 * @param {Object} params
 * @param {{amount:number, annualRate:number}} params.commercial 商贷 {金额（元）, 年利率}
 * @param {{amount:number, annualRate:number}} params.fund 公积金 {金额（元）, 年利率}
 * @param {number} params.months 总期数（月）
 * @param {'ac'|'ab'} params.method 还款方式：ac=等额本息，ab=等额本金
 * @returns {{month:number, monthlyDecrease:number, totalInterest:number, totalPayment:number, commercial:Object, fund:Object}}
 */
export function calcCombinedLoan({ commercial, fund, months, method }) {
  if (!commercial || !fund) {
    throw new Error('组合贷必须同时提供商贷与公积金参数')
  }
  const isAn = method !== 'ab' // 默认等额本息
  const calcOne = (part) => isAn
    ? calcEqualPayment(part.amount, part.annualRate, months)
    : calcEqualPrincipal(part.amount, part.annualRate, months)

  const c = calcOne(commercial)
  const f = calcOne(fund)
  const month = isAn
    ? c.monthlyPayment + f.monthlyPayment
    : c.firstMonth + f.firstMonth
  const monthlyDecrease = isAn ? 0 : c.monthlyDecrease + f.monthlyDecrease
  const totalInterest = c.totalInterest + f.totalInterest
  return {
    month: round2(month),
    monthlyDecrease: round2(monthlyDecrease),
    totalInterest: round2(totalInterest),
    totalPayment: round2(month * months),
    commercial: c,
    fund: f
  }
}
