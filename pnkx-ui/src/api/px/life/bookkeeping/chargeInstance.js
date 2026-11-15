import request from '@/utils/request'

// 查询账期实例列表（分页）
export function listChargeInstance(query) {
    return request({
        url: '/charge/instances',
        method: 'get',
        params: query
    })
}

// 查询账期实例详情（含处理历史）
export function getChargeInstance(id) {
    return request({
        url: '/charge/instances/' + id,
        method: 'get'
    })
}

// 确认账期入账
export function confirmChargeInstance(id, data) {
    return request({
        url: '/charge/instances/' + id + '/confirm',
        method: 'post',
        data: data
    })
}

// 账期实例处理（skip=跳过本期 / payment-failed=扣费失败）
export function resolveChargeInstance(id, action, data) {
    return request({
        url: '/charge/instances/' + id + '/' + action,
        method: 'post',
        data: data
    })
}
