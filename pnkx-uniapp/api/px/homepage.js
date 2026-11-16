import request from '@/utils/request'

// 首页查询姨妈提醒列表
export function selectMenstruationRecordList(query) {
  return request({
    url: '/homepage/selectMenstruationRecordList',
    method: 'get',
    params: query
  })
}

// 首页查询纪念日列表
export function getCommemorationDayList(query) {
  return request({
    url: '/homepage/getCommemorationDayList',
    method: 'get',
    params: query
  })
}
