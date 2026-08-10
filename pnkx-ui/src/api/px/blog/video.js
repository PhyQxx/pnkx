import request from '@/utils/request'

// 查询视频模块列表
export function listVideo(query) {
    return request({
        url: '/admin/video/list',
        method: 'get',
        params: query
    })
}

// 查询视频模块详细
export function getVideo(id) {
    return request({
        url: '/admin/video/' + id,
        method: 'get'
    })
}

// 新增视频模块
export function addVideo(data) {
    return request({
        url: '/admin/video',
        method: 'post',
        data: data
    })
}

// 修改视频模块
export function updateVideo(data) {
    return request({
        url: '/admin/video',
        method: 'put',
        data: data
    })
}

// 删除视频模块
export function delVideo(id) {
    return request({
        url: '/admin/video/' + id,
        method: 'delete'
    })
}

// 导出视频模块
export function exportVideo(query) {
    return request({
        url: '/admin/video/export',
        method: 'get',
        params: query
    })
}

/**
 * 获取标签列表
 * @returns {*}
 */
export function getLabelList() {
    return request({
        url: '/admin/video/getLabelList',
        method: 'get',
    })
}
