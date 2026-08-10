import request from '@/utils/request'

// 获取AI分析
export function getAIAnalysis() {
    return request({
        url: '/bookkeeping/record/aiAnalysis',
        method: 'get',
    })
}

// AI账单分析流式接口（返回 Promise<ReadableStream>）
export function getAIAnalysisStream() {
    const baseUrl = import.meta.env.VUE_APP_BASE_API
    const url = baseUrl + '/bookkeeping/record/aiAnalysis/stream'
    const token = localStorage.getItem('Admin-Token') || localStorage.getItem('Token')
    return fetch(url, {
        method: 'GET',
        headers: {
            'Authorization': token ? 'Bearer ' + token : ''
        }
    })
}

// 查询账本记录列表
export function listRecord(query) {
    return request({
        url: '/bookkeeping/record/list',
        method: 'get',
        params: query
    })
}

// 查询账本记录详细
export function getRecord(id) {
    return request({
        url: '/bookkeeping/record/' + id,
        method: 'get'
    })
}

// 新增账本记录
export function addRecord(data) {
    return request({
        url: '/bookkeeping/record',
        method: 'post',
        data: data
    })
}

// 修改账本记录
export function updateRecord(data) {
    return request({
        url: '/bookkeeping/record',
        method: 'put',
        data: data
    })
}

// 删除账本记录
export function delRecord(id) {
    return request({
        url: '/bookkeeping/record/' + id,
        method: 'delete'
    })
}

// 批量新增账本记录
export function addBatchRecord(data) {
    return request({
        url: '/bookkeeping/record/batch',
        method: 'post',
        data: data
    })
}

// AI批量解析账单
export function aiBatchParse(text) {
    return request({
        url: '/bookkeeping/record/aiBatchParse',
        method: 'post',
        params: { text: text }
    })
}

// 导出账本记录
export function exportRecord(query) {
    return request({
        url: '/bookkeeping/record/export',
        method: 'get',
        params: query
    })
}
