import request from '@/utils/request'

/**
 * 获取生活报告结构化数据
 * @param {Object} params - { period: 'week'|'month', reportType: 'summary'|'expense'|'mood' }
 * @returns {Promise}
 */
export function getLifeReportData(params) {
  return request({
    url: '/lifeReport/data',
    method: 'get',
    params
  })
}
