import request from '@/utils/request'

// 获取今日提醒聚合数据（纪念日 / 情侣卡 / 经期）
export function getTodayReminders() {
  return request({
    url: '/reminder/today',
    method: 'get'
  })
}

// 查询当前用户的通知列表（提醒投递记录）
export function getNotifications() {
  return request({
    url: '/reminder/notifications',
    method: 'get'
  })
}

// 统计当前用户未读通知数（首页铃铛红点用）
export function getUnreadCount() {
  return request({
    url: '/reminder/unread/count',
    method: 'get'
  })
}

// 标记通知已读（ids 为空数组/不传则全部已读）
export function markRead(ids) {
  return request({
    url: '/reminder/notifications/read',
    method: 'put',
    data: ids || []
  })
}

export function deleteNotification(id) {
  return request({
    url: '/reminder/notifications/' + id,
    method: 'delete'
  })
}

export function getWechatTemplateIds() {
  return request({
    url: '/reminder/wechat-template-ids',
    method: 'get'
  })
}

export function saveWechatSubscription(data) {
  return request({
    url: '/reminder/wechat-subscription',
    method: 'post',
    data
  })
}
