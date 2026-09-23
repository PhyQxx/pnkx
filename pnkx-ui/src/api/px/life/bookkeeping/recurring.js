import request from '@/utils/request'

// 当前用户的周期记账规则列表
export function listRecurring() {
    return request({
        url: '/bookkeeping/recurring/list',
        method: 'get'
    })
}

// 新增规则
export function addRecurring(data) {
    return request({
        url: '/bookkeeping/recurring',
        method: 'post',
        data
    })
}

// 修改规则
export function updateRecurring(data) {
    return request({
        url: '/bookkeeping/recurring',
        method: 'put',
        data
    })
}

// 启停规则
export function toggleRecurring(id, enabled) {
    return request({
        url: '/bookkeeping/recurring/toggle/' + id,
        method: 'put',
        data: {enabled}
    })
}

// 删除规则
export function delRecurring(id) {
    return request({
        url: '/bookkeeping/recurring/' + id,
        method: 'delete'
    })
}
