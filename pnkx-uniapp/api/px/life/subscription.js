import request from '@/utils/request'

// 查询订阅列表
export function listSubscription(query) {
  return request({
    url: '/subscription/list',
    method: 'get',
    params: query
  })
}

// 查询订阅详情
export function getSubscription(id) {
  return request({
    url: '/subscription/' + id,
    method: 'get'
  })
}

// 新增订阅
export function addSubscription(data) {
  return request({
    url: '/subscription',
    method: 'post',
    data
  })
}

// 修改订阅
export function updateSubscription(data) {
  return request({
    url: '/subscription',
    method: 'put',
    data
  })
}

// 删除订阅
export function delSubscription(ids) {
  return request({
    url: '/subscription/' + ids,
    method: 'delete'
  })
}

// 订阅支出预测（月度/年度汇总 + 明细）
export function forecast() {
  return request({
    url: '/subscription/forecast',
    method: 'get'
  })
}
