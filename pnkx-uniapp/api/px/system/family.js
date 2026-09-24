import request from '@/utils/request'

export function listSpaces() {
  return request({ url: '/system/dataGroup/list', method: 'get', params: { pageNum: 1, pageSize: 100 } })
}

export function getSpace(id) {
  return request({ url: `/system/dataGroup/${id}`, method: 'get' })
}

export function leaveSpace(id) {
  return request({ url: `/system/dataGroup/${id}/leave`, method: 'post' })
}

export function transferSpace(id, userId) {
  return request({ url: `/system/dataGroup/${id}/transfer/${userId}`, method: 'post' })
}
