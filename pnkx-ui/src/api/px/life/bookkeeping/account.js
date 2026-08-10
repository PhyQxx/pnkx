import request from '@/utils/request'

// 查询最近帐户列表
export function getAccountList(query) {
    return request({
        url: '/bookkeeping/account/getAccountList',
        method: 'get',
        params: query
    })
}

// 查询账本用户列表
export function listAccount(query) {
    return request({
        url: '/bookkeeping/account/list',
        method: 'get',
        params: query
    })
}

// 查询账本用户详细
export function getAccount(id) {
    return request({
        url: '/bookkeeping/account/' + id,
        method: 'get'
    })
}

// 新增账本用户
export function addAccount(data) {
    return request({
        url: '/bookkeeping/account',
        method: 'post',
        data: data
    })
}

// 修改账本用户
export function updateAccount(data) {
    return request({
        url: '/bookkeeping/account',
        method: 'put',
        data: data
    })
}

// 删除账本用户
export function delAccount(id) {
    return request({
        url: '/bookkeeping/account/' + id,
        method: 'delete'
    })
}

// 导出账本用户
export function exportAccount(query) {
    return request({
        url: '/bookkeeping/account/export',
        method: 'get',
        params: query
    })
}
