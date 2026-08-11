import request from '@/utils/request'

// 查询周膳食计划（startDate ~ endDate）
export function getMealPlanWeek(startDate, endDate) {
  return request({
    url: '/mealPlan/week',
    method: 'get',
    params: { startDate: startDate, endDate: endDate }
  })
}

// 查询膳食计划列表
export function listMealPlan(query) {
  return request({
    url: '/mealPlan/list',
    method: 'get',
    params: query
  })
}

// 新增膳食计划（往某天某餐次添加菜谱）
export function addMealPlan(data) {
  return request({
    url: '/mealPlan',
    method: 'post',
    data: data
  })
}

// 删除膳食计划
export function delMealPlan(ids) {
  return request({
    url: '/mealPlan/' + ids,
    method: 'delete'
  })
}

// 将日期范围内的膳食食材转入购物清单
export function transferToShopping(listId, startDate, endDate) {
  return request({
    url: '/mealPlan/transferToShopping',
    method: 'post',
    params: { listId: listId, startDate: startDate, endDate: endDate }
  })
}
