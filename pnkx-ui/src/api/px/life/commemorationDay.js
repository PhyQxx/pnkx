import request from '@/utils/request'

// 查询纪念日列表
export function listDay(query) {
    return request({
        url: '/commemorationDay/list',
        method: 'get',
        params: query
    })
}

// 查询纪念日详细
export function getDay(id) {
    return request({
        url: '/commemorationDay/' + id,
        method: 'get'
    })
}

// 新增纪念日
export function addDay(data) {
    return request({
        url: '/commemorationDay',
        method: 'post',
        data: data
    })
}

// 修改纪念日
export function updateDay(data) {
    return request({
        url: '/commemorationDay',
        method: 'put',
        data: data
    })
}

// 删除纪念日
export function delDay(id) {
    return request({
        url: '/commemorationDay/' + id,
        method: 'delete'
    })
}

// 导出纪念日
export function exportDay(query) {
    return request({
        url: '/commemorationDay/export',
        method: 'get',
        params: query
    })
}
