import request from '@/utils/request'

// 查询菜谱列表（分页，支持 title 模糊搜索）
export function listRecipe(query) {
  return request({
    url: '/recipe/list',
    method: 'get',
    params: query
  })
}

// 查询菜谱详情（不含食材）
export function getRecipe(id) {
  return request({
    url: '/recipe/' + id,
    method: 'get'
  })
}

// 查询菜谱详情（含食材列表）
export function getRecipeWithIngredients(id) {
  return request({
    url: '/recipe/withIngredients/' + id,
    method: 'get'
  })
}

// 新增菜谱（data 可含 ingredients 数组一并落库）
export function addRecipe(data) {
  return request({
    url: '/recipe',
    method: 'post',
    data: data
  })
}

// 修改菜谱（data 需带 id；ingredients 为全量覆盖，须提交完整列表）
export function updateRecipe(data) {
  return request({
    url: '/recipe',
    method: 'put',
    data: data
  })
}

// 删除菜谱（逻辑删除，ids 逗号拼接）
export function delRecipe(ids) {
  return request({
    url: '/recipe/' + ids,
    method: 'delete'
  })
}
