import request from '@/utils/request'

export function globalSearch(q, type = 'all', filters = {}) {
  return request({
    url: '/admin/globalSearch',
    method: 'get',
    params: { q, type, ...filters }
  })
}

export function summarizeSearch(data) {
  return request({
    url: '/admin/globalSearch/summary',
    method: 'post',
    data
  })
}
