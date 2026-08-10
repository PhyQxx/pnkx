import request from '@/utils/request'

/**
 * 关键字检索日记
 * @param query
 * @returns {AxiosPromise}
 */
export function retrievalDiary(query) {
  return request({
    url: '/admin/diary/retrieval',
    method: 'get',
    params: query
  })
}

// 查询日记列表
export function listDiary(query) {
  return request({
    url: '/admin/diary/list',
    method: 'get',
    params: query
  })
}

// 查询日记详细
export function getDiary(id) {
  return request({
    url: '/admin/diary/' + id,
    method: 'get'
  })
}

// 新增日记
export function addDiary(data) {
  return request({
    url: '/admin/diary',
    method: 'post',
    data: data
  })
}

// 修改日记
export function updateDiary(data) {
  return request({
    url: '/admin/diary',
    method: 'put',
    data: data
  })
}

// 删除日记
export function delDiary(id) {
  return request({
    url: '/admin/diary/' + id,
    method: 'delete'
  })
}

// 导出日记
export function exportDiary(query) {
  return request({
    url: '/admin/diary/export',
    method: 'get',
    params: query
  })
}
