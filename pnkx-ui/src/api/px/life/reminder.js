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
