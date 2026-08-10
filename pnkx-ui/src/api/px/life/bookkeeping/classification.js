import request from '@/utils/request'

// 查询最近分类列表
export function getClassificationList(query) {
    return request({
        url: '/bookkeeping/classification/getClassificationList',
        method: 'get',
        params: query
    })
}

// 查询账本分类列表
export function listClassification(query) {
    return request({
        url: '/bookkeeping/classification/list',
        method: 'get',
        params: query
    })
}

// 查询账本分类详细
export function getClassification(id) {
    return request({
        url: '/bookkeeping/classification/' + id,
        method: 'get'
    })
}

// 新增账本分类
export function addClassification(data) {
    return request({
        url: '/bookkeeping/classification',
        method: 'post',
        data: data
    })
}

// 修改账本分类
export function updateClassification(data) {
    return request({
        url: '/bookkeeping/classification',
        method: 'put',
        data: data
    })
}

// 删除账本分类
export function delClassification(id) {
    return request({
        url: '/bookkeeping/classification/' + id,
        method: 'delete'
    })
}

// 导出账本分类
export function exportClassification(query) {
    return request({
        url: '/bookkeeping/classification/export',
        method: 'get',
        params: query
    })
}
