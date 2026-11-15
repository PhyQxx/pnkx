import request from '@/utils/request'

// ============ 生活自动化（规则 / 模板 / 执行记录）============
// 端点与 pnkx-uniapp api/px/life/automation.js 保持一致

// 查询自动化规则列表
export function listRules() {
    return request({
        url: '/automation/rules',
        method: 'get'
    })
}

// 查询自动化模板市场
export function listTemplates() {
    return request({
        url: '/automation/templates',
        method: 'get'
    })
}

// 保存自动化规则（新增/修改）
export function saveRule(data) {
    return request({
        url: '/automation/rules',
        method: data.id ? 'put' : 'post',
        data: data
    })
}

// 删除自动化规则
export function deleteRule(id) {
    return request({
        url: '/automation/rules/' + id,
        method: 'delete'
    })
}

// 立即执行自动化规则
export function runRule(id, input = {}) {
    return request({
        url: '/automation/rules/' + id + '/run',
        method: 'post',
        data: input
    })
}

// 试运行（dry-run）自动化规则
export function dryRunRule(id, input = {}) {
    return request({
        url: '/automation/rules/' + id + '/dry-run',
        method: 'post',
        data: input
    })
}

// 查询执行记录
export function listExecutions(ruleId) {
    return request({
        url: '/automation/executions',
        method: 'get',
        params: { ruleId }
    })
}

// 重放失败的执行记录
export function retryExecution(id) {
    return request({
        url: '/automation/executions/' + id + '/retry',
        method: 'post'
    })
}
