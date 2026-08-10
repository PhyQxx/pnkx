import request from "@/utils/request";

/**
 * 登录聊天室
 */
export function loginChat() {
    return request({
        url: '/admin/chat/loginChat',
        method: 'post',
    })
}


/**
 * 发送信息
 */
export function sendMessage(params) {
    return request({
        url: '/admin/chat/sendMessage',
        method: 'post',
        data: params
    })
}

/**
 * 获取信息
 */
export function getMessageRecord(params) {
    return request({
        url: '/admin/chat/getMessageRecord',
        method: 'post',
        data: params
    })
}

/**
 * 退出聊天室
 */
export function signOut(params) {
    return request({
        url: '/admin/chat/signOut',
        method: 'post',
        data: params
    })
}

// 查询文件记录列表
export function listFile(query) {
    return request({
        url: '/system/file/list',
        method: 'get',
        params: query
    })
}

// 根据字典类型查询字典数据信息
export function getDicts(dictType) {
    return request({
        url: '/client/dictType/' + dictType,
        method: 'get'
    })
}
