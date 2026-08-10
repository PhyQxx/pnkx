import request from '@/utils/request'

// 查询邮件记录列表
export function listEmail(query) {
    return request({
        url: '/system/email/list',
        method: 'get',
        params: query
    })
}

// 查询邮件记录详细
export function getEmail(id) {
    return request({
        url: '/system/email/' + id,
        method: 'get'
    })
}

// 新增邮件记录
export function addEmail(data) {
    return request({
        url: '/system/email',
        method: 'post',
        data: data
    })
}

// 修改邮件记录
export function updateEmail(data) {
    return request({
        url: '/system/email',
        method: 'put',
        data: data
    })
}

// 删除邮件记录
export function delEmail(id) {
    return request({
        url: '/system/email/' + id,
        method: 'delete'
    })
}

// 导出邮件记录
export function exportEmail(query) {
    return request({
        url: '/system/email/export',
        method: 'get',
        params: query
    })
}

// 发送简单邮件
export function sendHtmlEmail(query) {
    return request({
        url: '/system/email/sendHtmlEmail',
        method: 'post',
        data: query
    })
}
