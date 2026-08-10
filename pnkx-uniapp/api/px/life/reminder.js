import request from '@/utils/request'

// 获取今日提醒聚合数据
export function getTodayReminders() {
  return request({
    url: '/reminder/today',
    method: 'get'
  })
}
