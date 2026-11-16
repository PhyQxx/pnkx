import request from '@/utils/request'

// 查询膳食计划列表（分页，可按 planDate / mealType 过滤）
export function listMealPlan(query) {
  return request({
    url: '/mealPlan/list',
    method: 'get',
    params: query
  })
}

// 查询膳食计划详情
export function getMealPlan(id) {
  return request({
    url: '/mealPlan/' + id,
    method: 'get'
  })
}

// 查询周视图（startDate / endDate 格式 yyyy-MM-dd，返回平铺列表）
export function getMealPlanWeek(startDate, endDate) {
  return request({
    url: '/mealPlan/week',
    method: 'get',
    params: { startDate, endDate }
  })
}

// 新增膳食计划（data 需含 planDate、mealType、title；可选 recipeId）
export function addMealPlan(data) {
  return request({
    url: '/mealPlan',
    method: 'post',
    data: data
  })
}

// 修改膳食计划
export function updateMealPlan(data) {
  return request({
    url: '/mealPlan',
    method: 'put',
    data: data
  })
}

// 删除膳食计划（逻辑删除，ids 逗号拼接）
export function delMealPlan(ids) {
  return request({
    url: '/mealPlan/' + ids,
    method: 'delete'
  })
}

// 将日期范围内的膳食计划食材转入购物清单
// 参数：listId 目标购物清单ID，startDate/endDate 日期范围 yyyy-MM-dd
export function transferToShopping(listId, startDate, endDate) {
  return request({
    url: '/mealPlan/transferToShopping',
    method: 'post',
    params: { listId, startDate, endDate }
  })
}
