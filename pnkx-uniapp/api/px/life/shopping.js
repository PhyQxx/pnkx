import request from '@/utils/request'

// ==================== 购物清单 ====================

// 查询购物清单列表（分页，支持 name 模糊搜索）
export function listShoppingList(query) {
  return request({
    url: '/shoppingList/list',
    method: 'get',
    params: query
  })
}

// 查询购物清单详情（不含购物项，购物项需另调 listShoppingItem）
export function getShoppingList(id) {
  return request({
    url: '/shoppingList/' + id,
    method: 'get'
  })
}

// 新增购物清单
export function addShoppingList(data) {
  return request({
    url: '/shoppingList',
    method: 'post',
    data: data
  })
}

// 修改购物清单
export function updateShoppingList(data) {
  return request({
    url: '/shoppingList',
    method: 'put',
    data: data
  })
}

// 删除购物清单（级联软删其下购物项，ids 逗号拼接）
export function delShoppingList(ids) {
  return request({
    url: '/shoppingList/' + ids,
    method: 'delete'
  })
}

// ==================== 购物项 ====================

// 查询购物项列表（分页，可按 listId / name / checked 过滤）
export function listShoppingItem(query) {
  return request({
    url: '/shoppingItem/list',
    method: 'get',
    params: query
  })
}

// 查询购物项详情
export function getShoppingItem(id) {
  return request({
    url: '/shoppingItem/' + id,
    method: 'get'
  })
}

// 新增购物项
export function addShoppingItem(data) {
  return request({
    url: '/shoppingItem',
    method: 'post',
    data: data
  })
}

// 修改购物项（勾选/取消勾选也用此接口，改 checked 字段）
export function updateShoppingItem(data) {
  return request({
    url: '/shoppingItem',
    method: 'put',
    data: data
  })
}

// 删除购物项（ids 逗号拼接）
export function delShoppingItem(ids) {
  return request({
    url: '/shoppingItem/' + ids,
    method: 'delete'
  })
}

// 清空指定清单下已勾选的购物项
export function clearCheckedShoppingItem(listId) {
  return request({
    url: '/shoppingItem/clearChecked/' + listId,
    method: 'delete'
  })
}
