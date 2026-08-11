import request from '@/utils/request'

// 获取今日提醒聚合数据（纪念日 / 情侣卡 / 经期 / 今日待办 / 经期配置）
export function getTodayReminders() {
  return request({
    url: '/reminder/today',
    method: 'get'
  })
}

// 获取通知列表（提醒中心）
export function getNotifications() {
  return request({
    url: '/reminder/notifications',
    method: 'get'
  })
}

// 获取未读通知数（首页铃铛红点）
export function getUnreadCount() {
  return request({
    url: '/reminder/unread/count',
    method: 'get'
  })
}

// 标记已读（ids 为空数组则全部已读）
export function markRead(ids) {
  return request({
    url: '/reminder/notifications/read',
    method: 'put',
    data: ids || []
  })
}

// 删除单条通知
export function deleteNotification(id) {
  return request({
    url: '/reminder/notifications/' + id,
    method: 'delete'
  })
}

