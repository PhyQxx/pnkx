/**
 * 汇率换算引擎（纯函数，与 UI 解耦，可直接单测）
 *
 * rates 为「以某基准币种为 1」的牌价表，如 base=CNY 时 rates.USD=0.14 表示 1 元 ≈ 0.14 美元
 */

/**
 * 金额换算：from 币种金额 → to 币种金额（经牌价基准币种交叉换算）
 * @param {Object} params
 * @param {number} params.amount 金额
 * @param {Object<string, number>} params.rates 牌价表
 * @param {string} params.from 源币种代码
 * @param {string} params.to 目标币种代码
 * @returns {number} 换算结果（保留 4 位小数）
 */
export function convertAmount({ amount, rates, from, to }) {
  if (!(amount >= 0)) {
    throw new Error('金额不能为负数')
  }
  const fromRate = rates[from]
  const toRate = rates[to]
  if (!fromRate || !toRate) {
    throw new Error('暂不支持该币种')
  }
  const converted = (amount / fromRate) * toRate
  return Math.round(converted * 10000) / 10000
}

/**
 * 币种显示格式化：去尾零，最多保留 4 位小数
 * @param {number} value 数值
 * @returns {string} 如 14.0521 / 100
 */
export function formatRate(value) {
  const fixed = value.toFixed(4)
  return String(parseFloat(fixed))
}
