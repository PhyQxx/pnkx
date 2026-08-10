import request from '@/utils/request'

/**
 * 获取统计每月日消费折线图
 * @param data
 * @returns
 */
export function getLineChart(data) {
  return request({
    url: '/bookkeeping/statistics/getLineChart',
    method: 'post',
    data: data
  })
}

/**
 * 一级分类统计
 * @param data
 * @returns
 */
export function getPrimaryStatistics(data) {
  return request({
    url: '/bookkeeping/statistics/getPrimaryStatistics',
    method: 'post',
    data: data
  })
}

/**
 * 二级分类统计
 * @param data
 * @returns
 */
export function getSecondaryStatistics(data) {
  return request({
    url: '/bookkeeping/statistics/getSecondaryStatistics',
    method: 'post',
    data: data
  })
}

/**
 * 账户统计
 * @param data
 * @returns
 */
export function getAccountStatistics(data) {
  return request({
    url: '/bookkeeping/statistics/getAccountStatistics',
    method: 'post',
    data: data
  })
}

/**
 * 资产负债统计
 * @returns
 */
export function getAssetsStatistics() {
  return request({
    url: '/bookkeeping/statistics/getAssetsStatistics',
    method: 'post',
  })
}

/**
 * 月度统计
 * @returns
 */
export function getMonthlyStatistics(data) {
  return request({
    url: '/bookkeeping/statistics/getMonthlyStatistics',
    method: 'post',
    data: data
  })
}
