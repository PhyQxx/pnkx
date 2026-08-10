import request from '@/utils/request'

// 查询自定义回复规则列表
export function listRule(query) {
    return request({
        url: '/chat/customReply/list',
        method: 'get',
        params: query
    })
}

// 查询自定义回复规则详细
export function getRule(id) {
    return request({
        url: '/chat/customReply/' + id,
        method: 'get'
    })
}

// 新增自定义回复规则
export function addRule(data) {
    return request({
        url: '/chat/customReply',
        method: 'post',
        data: data
    })
}

// 修改自定义回复规则
export function updateRule(data) {
    return request({
        url: '/chat/customReply',
        method: 'put',
        data: data
    })
}

// 删除自定义回复规则
export function delRule(id) {
    return request({
        url: '/chat/customReply/' + id,
        method: 'delete'
    })
}

// 导出自定义回复规则
export function exportRule(query) {
    return request({
        url: '/chat/customReply/export',
        method: 'get',
        params: query
    })
}
