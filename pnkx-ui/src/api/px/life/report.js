import request from '@/utils/request'

export function getLifeReportData(params) {
  return request({
    url: '/lifeReport/data',
    method: 'get',
    params
  })
}

export function getLifeReportHistory(limit = 20) {
  return request({
    url: '/lifeReport/history',
    method: 'get',
    params: { limit }
  })
}
