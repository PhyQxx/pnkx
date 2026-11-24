import request from '@/utils/request'
export const analyticsReport = params => request({ url: '/admin/analytics/report', method: 'get', params })
export const analyticsEvents = params => request({ url: '/admin/analytics/events', method: 'get', params })

export const analyticsFunnels = params => request({ url: '/admin/analytics/funnels', method: 'get', params })
