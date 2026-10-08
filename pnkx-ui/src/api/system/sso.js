import request from '@/utils/request'

// 查询 SSO 应用列表
export function listSsoClient() {
    return request({
        url: '/system/sso/list',
        method: 'get'
    })
}

// 新增 SSO 应用
export function addSsoClient(data) {
    return request({
        url: '/system/sso',
        method: 'post',
        data: data
    })
}

// 修改 SSO 应用
export function updateSsoClient(data) {
    return request({
        url: '/system/sso',
        method: 'put',
        data: data
    })
}

// 删除 SSO 应用
export function delSsoClient(id) {
    return request({
        url: '/system/sso/' + id,
        method: 'delete'
    })
}
