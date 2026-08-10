import request from '@/utils/request'

// 查询数据权限群组列表
export function listDataGroup(query) {
    return request({
        url: '/system/dataGroup/list',
        method: 'get',
        params: query
    })
}

// 查询数据权限群组详细
export function getDataGroup(id) {
    return request({
        url: '/system/dataGroup/' + id,
        method: 'get'
    })
}

// 新增数据权限群组
export function addDataGroup(data) {
    return request({
        url: '/system/dataGroup',
        method: 'post',
        data: data
    })
}

// 修改数据权限群组
export function updateDataGroup(data) {
    return request({
        url: '/system/dataGroup',
        method: 'put',
        data: data
    })
}

// 删除数据权限群组
export function delDataGroup(id) {
    return request({
        url: '/system/dataGroup/' + id,
        method: 'delete'
    })
}
