import request from '@/utils/request'

/**
 * 查询以 base 为基准的牌价（工具箱·免登录）
 * @param {string} base 基准币种，如 CNY
 * @returns {Promise<{base, rates, updateTime, stale, source, currencyNames}>}
 */
export function getExchangeRates(base) {
  return request({
    url: '/tools/exrate/list',
    method: 'get',
    params: { base }
  })
}
