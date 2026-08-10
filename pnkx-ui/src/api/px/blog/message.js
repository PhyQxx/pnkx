import request from '@/utils/request'

/**
 * 留言
 */
export function addMessage(params) {
    return request({
        url: '/admin/message/addMessage',
        method: 'post',
        data: params
    })
}

//获取留言板内容
export function getMessageList(query) {
    return request({
        url: '/admin/message/getMessageList',
        method: 'get',
        params: query
    })
}

//获取留言审核
export function getMessageExamine(query) {
    return request({
        url: '/admin/message/getMessageExamine',
        method: 'get',
        params: query
    })
}

// 修改留言
export function updateMessage(data) {
    return request({
        url: '/admin/message/updateMessage',
        method: 'put',
        data: data
    })
}
