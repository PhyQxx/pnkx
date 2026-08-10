import request from '@/utils/request'

// 全体发送
export function sendAllWebSocket(query) {
    return request({
        url: '/pxWebSocket/sendAllWebSocket',
        method: 'get',
        params: query
    })
}

// 个人发送
export function sendOneWebSocket(name) {
    return request({
        url: '/pxWebSocket/sendOneWebSocket/' + name,
        method: 'get',
    })
}
