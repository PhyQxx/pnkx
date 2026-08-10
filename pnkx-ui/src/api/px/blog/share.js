import request from '@/utils/request'

// 查询分享资源列表
export function listShare(query) {
    return request({
        url: '/admin/share/list',
        method: 'get',
        params: query
    })
}

// 查询分享资源详细
export function getShare(id) {
    return request({
        url: '/admin/share/' + id,
        method: 'get'
    })
}

// 新增分享资源
export function addShare(data) {
    return request({
        url: '/admin/share',
        method: 'post',
        data: data
    })
}

// 修改分享资源
export function updateShare(data) {
    return request({
        url: '/admin/share',
        method: 'put',
        data: data
    })
}

// 删除分享资源
export function delShare(id) {
    return request({
        url: '/admin/share/' + id,
        method: 'delete'
    })
}

// 导出分享资源
export function exportShare(query) {
    return request({
        url: '/admin/share/export',
        method: 'get',
        params: query
    })
}

// 获取标签列表
export function getShareLabelList() {
    return request({
        url: '/admin/share/getLabelList',
        method: 'get'
    })
}
