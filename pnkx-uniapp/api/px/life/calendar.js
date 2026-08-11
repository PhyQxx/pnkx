import request from '@/utils/request'

// 查询月事件列表（startDate ~ endDate）
export function getMonthEvents(startDate, endDate) {
  return request({
    url: '/calendar/month',
    method: 'get',
    params: { startDate, endDate }
  })
}

// 今日概览
export function getCockpit() {
  return request({
    url: '/calendar/cockpit',
    method: 'get'
  })
}
