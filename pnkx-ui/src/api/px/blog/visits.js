import request from '@/utils/request'

// 查询访客列表
export function listVisits(query) {
    return request({
        url: '/admin/visits/list',
        method: 'get',
        params: query
    })
}

// 删除访客
export function delVisits(id) {
    return request({
        url: '/admin/visits/' + id,
        method: 'delete'
    })
}

// 导出访客
export function exportVisits(query) {
    return request({
        url: '/admin/visits/export',
        method: 'get',
        params: query
    })
}
