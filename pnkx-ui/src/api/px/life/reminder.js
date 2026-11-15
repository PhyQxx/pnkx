import request from '@/utils/request'

// 查询提醒配置列表
export function listReminder(query) {
    return request({
        url: '/reminder/list',
        method: 'get',
        params: query
    })
}

// 查询提醒配置详细
export function getReminder(id) {
    return request({
        url: '/reminder/' + id,
        method: 'get'
    })
}

// 新增提醒配置
export function addReminder(data) {
    return request({
        url: '/reminder',
        method: 'post',
        data: data
    })
}

// 修改提醒配置
export function updateReminder(data) {
    return request({
        url: '/reminder',
        method: 'put',
        data: data
    })
}

// 删除提醒配置
export function delReminder(id) {
    return request({
        url: '/reminder/' + id,
        method: 'delete'
    })
}

// 给来源实体绑定/更新提醒（upsert）
// data: { sourceType, sourceId, userId, remindTime, leadMinutes, eventTime }
export function bindReminder(data) {
    return request({
        url: '/reminder/bind',
        method: 'post',
        data: data
    })
}

// 按来源实体解绑提醒
export function unbindReminder(sourceType, sourceId) {
    return request({
        url: '/reminder/unbind',
        method: 'delete',
        params: { sourceType, sourceId }
    })
}

// 今日提醒聚合（纪念日 / 情侣卡 / 经期）
export function getTodayReminders() {
    return request({
        url: '/reminder/today',
        method: 'get'
    })
}

// 查询当前用户的通知列表（通知中心）
export function listNotifications() {
    return request({
        url: '/reminder/notifications',
        method: 'get'
    })
}

// 统计当前用户未读通知数（顶部铃铛）
export function countUnread() {
    return request({
        url: '/reminder/unread/count',
        method: 'get'
    })
}

// 标记已读（ids 为空则全部已读）
export function markNotificationsRead(ids) {
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

// 重发发送失败的通知
export function retryNotification(id) {
    return request({
        url: '/reminder/notifications/' + id + '/retry',
        method: 'post'
    })
}

// 查询提醒渠道偏好（站内/推送/邮件 + 免打扰时段）
export function getReminderPreference() {
    return request({
        url: '/reminder/preference',
        method: 'get'
    })
}

// 保存提醒渠道偏好
export function saveReminderPreference(data) {
    return request({
        url: '/reminder/preference',
        method: 'put',
        data: data
    })
}
