import request from '@/utils/request'

// 查询菜谱列表
export function listRecipe(query) {
  return request({
    url: '/recipe/list',
    method: 'get',
    params: query
  })
}

// 查询菜谱详情（含食材）
export function getRecipeWithIngredients(id) {
  return request({
    url: '/recipe/withIngredients/' + id,
    method: 'get'
  })
}

// 查询菜谱基础详情
export function getRecipe(id) {
  return request({
    url: '/recipe/' + id,
    method: 'get'
  })
}
