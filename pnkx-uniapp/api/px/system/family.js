import { clearSharedCache } from '@/utils/sharedCache'
import request from '@/utils/request'

export function listSpaces() {
  return request({ url: '/system/dataGroup/list', method: 'get', params: { pageNum: 1, pageSize: 100 } })
}

export function getSpace(id) {
  return request({ url: `/system/dataGroup/${id}`, method: 'get' })
}

export async function leaveSpace(id) {
  const result = await request({ url: `/system/dataGroup/${id}/leave`, method: 'post' })
  try { await clearSharedCache() } catch (_) { uni.showToast({ title: '已退出，缓存清理将在下次启动重试', icon: 'none' }) }
  return result
}

export function transferSpace(id, userId) {
  return request({ url: `/system/dataGroup/${id}/transfer/${userId}`, method: 'post' })
}
