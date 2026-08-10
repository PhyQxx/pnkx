import request from '@/utils/request'

// 查询账本记录列表
export function listRecord(query) {
  return request({
    url: '/bookkeeping/record/list',
    method: 'get',
    params: query
  })
}

// 查询账本记录详细
export function getRecord(id) {
  return request({
    url: '/bookkeeping/record/' + id,
    method: 'get'
  })
}

// 新增账本记录
export function addRecord(data) {
  return request({
    url: '/bookkeeping/record',
    method: 'post',
    data: data
  })
}

// 修改账本记录
export function updateRecord(data) {
  return request({
    url: '/bookkeeping/record',
    method: 'put',
    data: data
  })
}

// 删除账本记录
export function delRecord(id) {
  return request({
    url: '/bookkeeping/record/' + id,
    method: 'delete'
  })
}

// 导出账本记录
export function exportRecord(query) {
  return request({
    url: '/bookkeeping/record/export',
    method: 'get',
    params: query
  })
}

// AI解析自然语言为记账数据
export function aiParse(text) {
  return request({
    url: '/bookkeeping/record/aiParse',
    method: 'post',
    params: { text }
  })
}
