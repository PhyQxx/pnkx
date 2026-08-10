import request from '@/utils/request'

// 查询App版本列表
export function listVersion(query) {
    return request({
        url: '/system/version/list',
        method: 'get',
        params: query
    })
}

// 查询App版本详细
export function getVersion(id) {
    return request({
        url: '/system/version/' + id,
        method: 'get'
    })
}

// 新增App版本
export function addVersion(data) {
    return request({
        url: '/system/version',
        method: 'post',
        data: data
    })
}

// 修改App版本
export function updateVersion(data) {
    return request({
        url: '/system/version',
        method: 'put',
        data: data
    })
}

// 删除App版本
export function delVersion(id) {
    return request({
        url: '/system/version/' + id,
        method: 'delete'
    })
}
