import request from '@/utils/request'

// 查询情侣卡券列表
export function listCard(query) {
    return request({
        url: '/px/card/list',
        method: 'get',
        params: query
    })
}

// 查询情侣卡券使用记录列表
export function listRecord(query) {
    return request({
        url: '/px/card/listRecord',
        method: 'get',
        params: query
    })
}

// 查询情侣卡券详细
export function getCard(id) {
    return request({
        url: '/px/card/' + id,
        method: 'get'
    })
}

// 查询情侣卡券记录详细
export function getCardRecord(id) {
    return request({
        url: '/px/card/record/' + id,
        method: 'get'
    })
}

// 新增情侣卡券
export function addCard(data) {
    return request({
        url: '/px/card',
        method: 'post',
        data: data
    })
}

// 修改情侣卡券
export function updateCard(data) {
    return request({
        url: '/px/card',
        method: 'put',
        data: data
    })
}

// 删除情侣卡券
export function delCard(id) {
    return request({
        url: '/px/card/' + id,
        method: 'delete'
    })
}

// 导出情侣卡券
export function exportCard(query) {
    return request({
        url: '/px/card/export',
        method: 'get',
        params: query
    })
}

// 获取当前人的卡券
export function getCardByUserId() {
    return request({
        url: '/px/card/getCardByUserId',
        method: 'get',
    })
}

// 获取当前人的卡券
export function useCard(data) {
    return request({
        url: '/px/card/useCard',
        method: 'post',
        data: data
    })
}

// 确认卡券使用
export function confirmCard(data) {
    return request({
        url: '/px/card/confirmCard',
        method: 'post',
        data: data
    })
}

// 卡券使用评分
export function scoreCard(data) {
    return request({
        url: '/px/card/scoreCard',
        method: 'post',
        data: data
    })
}

// 获取待处理的卡券
export function getToDoCard() {
    return request({
        url: '/px/card/getToDoCard',
        method: 'get',
    })
}
