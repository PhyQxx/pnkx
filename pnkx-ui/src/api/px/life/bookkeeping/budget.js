import request from '@/utils/request'

// 查询某月预算配置列表
export function listBudget(month) {
    return request({
        url: '/bookkeeping/budget/list',
        method: 'get',
        params: {month}
    })
}

// 查询某月预算使用状态（已用/剩余/百分比/超支）
export function getBudgetStatus(month) {
    return request({
        url: '/bookkeeping/budget/status',
        method: 'get',
        params: {month}
    })
}

// 保存预算（同月同分类自动覆盖更新）
export function saveBudget(data) {
    return request({
        url: '/bookkeeping/budget',
        method: 'post',
        data
    })
}

// 删除预算
export function delBudget(id) {
    return request({
        url: '/bookkeeping/budget/' + id,
        method: 'delete'
    })
}
