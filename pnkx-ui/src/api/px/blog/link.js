import request from '@/utils/request'

// 查询友链列表
export function listLink(query) {
    return request({
        url: '/admin/link/list',
        method: 'get',
        params: query
    })
}

// 查询友链详细
export function getLink(id) {
    return request({
        url: '/admin/link/' + id,
        method: 'get'
    })
}

// 新增友链
export function addLink(data) {
    return request({
        url: '/admin/link',
        method: 'post',
        data: data
    })
}

// 修改友链
export function updateLink(data) {
    return request({
        url: '/admin/link',
        method: 'put',
        data: data
    })
}

// 删除友链
export function delLink(id) {
    return request({
        url: '/admin/link/' + id,
        method: 'delete'
    })
}

// 导出友链
export function exportLink(query) {
    return request({
        url: '/admin/link/export',
        method: 'get',
        params: query
    })
}
