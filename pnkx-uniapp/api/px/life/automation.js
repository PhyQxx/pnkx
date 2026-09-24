import request from '@/utils/request'

export const listRules = () => request({ url: '/automation/rules', method: 'get' })
export const listTemplates = () => request({ url: '/automation/templates', method: 'get' })
export const saveRule = data => request({ url: '/automation/rules', method: data.id ? 'put' : 'post', data })
export const deleteRule = id => request({ url: `/automation/rules/${id}`, method: 'delete' })
export const runRule = (id, input = {}) => request({ url: `/automation/rules/${id}/run`, method: 'post', data: input })
export const dryRunRule = (id, input = {}) => request({ url: `/automation/rules/${id}/dry-run`, method: 'post', data: input })
export const listExecutions = ruleId => request({ url: '/automation/executions', method: 'get', params: { ruleId } })
export const retryExecution = id => request({ url: `/automation/executions/${id}/retry`, method: 'post' })
