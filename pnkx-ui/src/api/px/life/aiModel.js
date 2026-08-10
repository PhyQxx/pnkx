import request from '@/utils/request'

// 查询AI模型列表
export function listAiModel(query) {
    return request({
        url: '/aiModel/list',
        method: 'get',
        params: query
    })
}

// 查询AI模型详情
export function getAiModel(id) {
    return request({
        url: '/aiModel/' + id,
        method: 'get'
    })
}

// 新增AI模型
export function addAiModel(data) {
    return request({
        url: '/aiModel',
        method: 'post',
        data: data
    })
}

// 修改AI模型
export function updateAiModel(data) {
    return request({
        url: '/aiModel',
        method: 'put',
        data: data
    })
}

// 删除AI模型
export function delAiModel(id) {
    return request({
        url: '/aiModel/' + id,
        method: 'delete'
    })
}

// 设为默认
export function setDefaultAiModel(id) {
    return request({
        url: '/aiModel/setDefault/' + id,
        method: 'put'
    })
}

// 获取默认模型
export function getDefaultAiModel() {
    return request({
        url: '/aiModel/getDefault',
        method: 'get'
    })
}
