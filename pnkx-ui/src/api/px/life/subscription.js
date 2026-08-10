import request from '@/utils/request'

export function listSubscription(query) {
    return request({url: '/subscription/list', method: 'get', params: query})
}
export function getSubscription(id) {
    return request({url: '/subscription/' + id, method: 'get'})
}
export function addSubscription(data) {
    return request({url: '/subscription', method: 'post', data})
}
export function updateSubscription(data) {
    return request({url: '/subscription', method: 'put', data})
}
export function delSubscription(id) {
    return request({url: '/subscription/' + id, method: 'delete'})
}
// 月度/年度预测汇总
export function forecast() {
    return request({url: '/subscription/forecast', method: 'get'})
}
