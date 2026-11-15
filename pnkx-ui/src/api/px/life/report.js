import request from '@/utils/request'

export function getLifeReportData(params) {
  return request({
    url: '/lifeReport/data',
    method: 'get',
    params
  })
}

export function getLifeReportHistory(limit = 20) {
  return request({
    url: '/lifeReport/history',
    method: 'get',
    params: { limit }
  })
}

// 报告行动与复盘：事实快照列表
export function listLifeReportInsights() {
  return request({
    url: '/lifeReport/insights',
    method: 'get'
  })
}

// 生成当前周期事实快照
export function createInsightSnapshot(data) {
  return request({
    url: '/lifeReport/insights',
    method: 'post',
    data: data
  })
}

// 预览后续行动（返回操作计划）
export function previewInsightAction(id, data) {
  return request({
    url: '/lifeReport/insights/' + id + '/action-preview',
    method: 'post',
    data: data
  })
}

// 确认执行行动（创建待办）
export function acceptInsightAction(id, data) {
  return request({
    url: '/lifeReport/insights/' + id + '/accept',
    method: 'post',
    data: data
  })
}

// 忽略一条事实快照
export function dismissInsight(id) {
  return request({
    url: '/lifeReport/insights/' + id + '/dismiss',
    method: 'post'
  })
}

// 当前来源明细（可能与快照不同）
export function listInsightSources(id, pageNum) {
  return request({
    url: '/lifeReport/insights/' + id + '/sources',
    method: 'get',
    params: { pageNum }
  })
}

// 查询操作计划详情
export function getOperationPlan(id) {
  return request({
    url: '/operation-plans/' + id,
    method: 'get'
  })
}
