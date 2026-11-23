import request from '@/utils/request'

// 查询工具使用明细（分页，管理端）
export function listToolUseLog(query) {
    return request({
        url: '/tools/use/log/list',
        method: 'get',
        params: query
    })
}

// 查询工具使用统计（按工具聚合，支持时间范围）
export function statsToolUseLog(query) {
    return request({
        url: '/tools/use/log/stats',
        method: 'get',
        params: query
    })
}
