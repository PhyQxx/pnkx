import request from '@/utils/request'

// 查询待办事项列表
export function listDo(query) {
    return request({
        url: '/admin/toDo/list',
        method: 'get',
        params: query
    })
}

// 查询待办事项详细
export function getDo(id) {
    return request({
        url: '/admin/toDo/' + id,
        method: 'get'
    })
}

// 新增待办事项
export function addDo(data) {
    return request({
        url: '/admin/toDo',
        method: 'post',
        data: data
    })
}

// 修改待办事项
export function updateDo(data) {
    return request({
        url: '/admin/toDo',
        method: 'put',
        data: data
    })
}

// 删除待办事项
export function delDo(id) {
    return request({
        url: '/admin/toDo/' + id,
        method: 'delete'
    })
}

// 导出待办事项
export function exportDo(query) {
    return request({
        url: '/admin/toDo/export',
        method: 'get',
        params: query
    })
}

/**
 * 获取标签列表
 * @returns {*}
 */
export function getLabelList() {
    return request({
        url: '/admin/toDo/getLabelList',
        method: 'get',
    })
}

// 看板查询（返回 todo/doing/done 三栏）
export function kanbanDo(query) {
    return request({
        url: '/admin/toDo/kanban',
        method: 'get',
        params: query
    })
}

// 子任务查询
export function subtaskDo(parentId) {
    return request({
        url: '/admin/toDo/subtask/' + parentId,
        method: 'get'
    })
}

// 批量更新看板状态+排序（拖拽）
// list: [{id, kanbanStatus, sortOrder}, ...]
export function sortDo(list) {
    return request({
        url: '/admin/toDo/sort',
        method: 'put',
        data: list
    })
}

