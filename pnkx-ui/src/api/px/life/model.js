import request from '@/utils/request'

// 查询账本记录模板列表
export function listModel(query) {
    return request({
        url: '/bookkeeping/recordModel/list',
        method: 'get',
        params: query
    })
}

// 查询账本记录模板详细
export function getModel(id) {
    return request({
        url: '/bookkeeping/recordModel/' + id,
        method: 'get'
    })
}

// 新增账本记录模板
export function addModel(data) {
    return request({
        url: '/bookkeeping/recordModel',
        method: 'post',
        data: data
    })
}

// 修改账本记录模板
export function updateModel(data) {
    return request({
        url: '/bookkeeping/recordModel',
        method: 'put',
        data: data
    })
}

// 删除账本记录模板
export function delModel(id) {
    return request({
        url: '/bookkeeping/recordModel/' + id,
        method: 'delete'
    })
}

// 导出账本记录模板
export function exportModel(query) {
    return request({
        url: '/bookkeeping/recordModel/export',
        method: 'get',
        params: query
    })
}
