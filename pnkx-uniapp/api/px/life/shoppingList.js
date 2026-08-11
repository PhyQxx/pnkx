import request from '@/utils/request'

// ===== 购物清单 =====

// 查询购物清单列表
export function listShoppingList(query) {
  return request({
    url: '/shoppingList/list',
    method: 'get',
    params: query
  })
}

// 查询购物清单详情
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

// 删除购物清单
export function delShoppingList(ids) {
  return request({
    url: '/shoppingList/' + ids,
    method: 'delete'
  })
}

// ===== 购物项 =====

// 查询某清单下的购物项列表
export function listShoppingItem(listId) {
  return request({
    url: '/shoppingItem/list',
    method: 'get',
    params: { listId: listId }
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

// 修改购物项（含勾选状态切换）
export function updateShoppingItem(data) {
  return request({
    url: '/shoppingItem',
    method: 'put',
    data: data
  })
}

// 删除购物项
export function delShoppingItem(ids) {
  return request({
    url: '/shoppingItem/' + ids,
    method: 'delete'
  })
}

// 清空已勾选的购物项
export function clearChecked(listId) {
  return request({
    url: '/shoppingItem/clearChecked/' + listId,
    method: 'delete'
  })
}
