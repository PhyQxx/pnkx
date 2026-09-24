import request from '@/utils/request'

export function listRecurring() {
  return request({ url: '/bookkeeping/recurring/list', method: 'get' })
}

export function addRecurring(data) {
  return request({ url: '/bookkeeping/recurring', method: 'post', data })
}

export function updateRecurring(data) {
  return request({ url: '/bookkeeping/recurring', method: 'put', data })
}

export function toggleRecurring(id, enabled) {
  return request({
    url: '/bookkeeping/recurring/toggle/' + id,
    method: 'put',
    data: { enabled }
  })
}

export function delRecurring(id) {
  return request({ url: '/bookkeeping/recurring/' + id, method: 'delete' })
}
