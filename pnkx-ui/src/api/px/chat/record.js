import request from '@/utils/request'

// 查询聊天记录列表
export function listMessage(query) {
    return request({
        url: '/chat/message/list',
        method: 'get',
        params: query
    })
}

// 查询聊天记录详细
export function getMessage(id) {
    return request({
        url: '/chat/message/' + id,
        method: 'get'
    })
}

// 新增聊天记录
export function addMessage(data) {
    return request({
        url: '/chat/message',
        method: 'post',
        data: data
    })
}

// 修改聊天记录
export function updateMessage(data) {
    return request({
        url: '/chat/message',
        method: 'put',
        data: data
    })
}

// 删除聊天记录
export function delMessage(id) {
    return request({
        url: '/chat/message/' + id,
        method: 'delete'
    })
}

// 导出聊天记录
export function exportMessage(query) {
    return request({
        url: '/chat/message/export',
        method: 'get',
        params: query
    })
}
