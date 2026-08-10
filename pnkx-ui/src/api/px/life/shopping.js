import request from '@/utils/request'

// ============ 购物清单 ============
export function listShoppingList(query) {
    return request({url: '/shoppingList/list', method: 'get', params: query})
}
export function getShoppingList(id) {
    return request({url: '/shoppingList/' + id, method: 'get'})
}
export function addShoppingList(data) {
    return request({url: '/shoppingList', method: 'post', data})
}
export function updateShoppingList(data) {
    return request({url: '/shoppingList', method: 'put', data})
}
export function delShoppingList(id) {
    return request({url: '/shoppingList/' + id, method: 'delete'})
}

// ============ 购物条目 ============
export function listShoppingItem(query) {
    return request({url: '/shoppingItem/list', method: 'get', params: query})
}
export function addShoppingItem(data) {
    return request({url: '/shoppingItem', method: 'post', data})
}
export function updateShoppingItem(data) {
    return request({url: '/shoppingItem', method: 'put', data})
}
export function delShoppingItem(id) {
    return request({url: '/shoppingItem/' + id, method: 'delete'})
}
export function clearChecked(listId) {
    return request({url: '/shoppingItem/clearChecked/' + listId, method: 'delete'})
}

// ============ 菜谱 ============
export function listRecipe(query) {
    return request({url: '/recipe/list', method: 'get', params: query})
}
export function getRecipe(id) {
    return request({url: '/recipe/' + id, method: 'get'})
}
export function getRecipeWithIngredients(id) {
    return request({url: '/recipe/withIngredients/' + id, method: 'get'})
}
export function addRecipe(data) {
    return request({url: '/recipe', method: 'post', data})
}
export function updateRecipe(data) {
    return request({url: '/recipe', method: 'put', data})
}
export function delRecipe(id) {
    return request({url: '/recipe/' + id, method: 'delete'})
}

// ============ 餐饮计划 ============
export function listMealPlan(query) {
    return request({url: '/mealPlan/list', method: 'get', params: query})
}
export function getMealWeek(startDate, endDate) {
    return request({url: '/mealPlan/week', method: 'get', params: {startDate, endDate}})
}
export function addMealPlan(data) {
    return request({url: '/mealPlan', method: 'post', data})
}
export function updateMealPlan(data) {
    return request({url: '/mealPlan', method: 'put', data})
}
export function delMealPlan(id) {
    return request({url: '/mealPlan/' + id, method: 'delete'})
}
// 餐饮食材 → 购物清单联动
export function transferToShopping(listId, startDate, endDate) {
    return request({url: '/mealPlan/transferToShopping', method: 'post', params: {listId, startDate, endDate}})
}
