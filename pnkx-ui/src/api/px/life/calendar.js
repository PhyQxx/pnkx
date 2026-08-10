import request from '@/utils/request'

// 按月份范围查询聚合事件（待办/纪念日/经期/记账）
export function getMonthEvents(startDate, endDate) {
    return request({
        url: '/calendar/month',
        method: 'get',
        params: { startDate, endDate }
    })
}

// 今日概览（Today Cockpit）
export function getCockpit() {
    return request({
        url: '/calendar/cockpit',
        method: 'get'
    })
}
