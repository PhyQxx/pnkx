import request from '@/utils/request'

/**
 * 获取ip地址等信息
 * @returns {*}
 */
export function getIpLocation() {
    return request({
        url: '/client/getIpLocation',
        method: 'get',
    })
}

/**
 * 获取全量待办
 */
export function getAllTodoList() {
    return request({
        url: '/admin/getAllToDo',
        method: 'get',
    })
}
