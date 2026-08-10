import request from '@/utils/request'

export function listAiLog(query) {
  return request({
    url: '/ai/log/list',
    method: 'get',
    params: query
  })
}

export function getAiLogStatistics(params) {
  return request({
    url: '/ai/log/statistics',
    method: 'get',
    params
  })
}
