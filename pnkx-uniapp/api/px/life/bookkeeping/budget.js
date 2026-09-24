import request from '@/utils/request'

export function getBudgetStatus(month) {
  return request({
    url: '/bookkeeping/budget/status',
    method: 'get',
    params: { month }
  })
}

export function saveBudget(data) {
  return request({
    url: '/bookkeeping/budget',
    method: 'post',
    data
  })
}

export function delBudget(id) {
  return request({
    url: '/bookkeeping/budget/' + id,
    method: 'delete'
  })
}
