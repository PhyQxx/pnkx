import request from '@/utils/customer-request'

/**
 * 获取文章列表
 */
export function getArticleList(params) {
    return request({
        url: '/customer/getArticleList',
        method: 'post',
        data: params
    })
}

/**
 * 获取文章分类列表
 */
export function getArticleTypeNumber(params) {
    return request({
        url: '/customer/getArticleTypeNumber',
        method: 'post',
        data: params
    })
}

/**
 * 获取文章分类列表
 */
export function getArticleTypeList(params) {
    return request({
        url: '/customer/getArticleTypeList',
        method: 'post',
        data: params
    })
}

/**
 * 根据文章ID获取留言列表
 */
export function getLeaveMessageByArticleId(params) {
    return request({
        url: '/customer/getLeaveMessageByArticleId',
        method: 'post',
        data: params
    })
}

/**
 * 留言
 */
export function addMessage(params) {
    return request({
        url: '/customer/addMessage',
        method: 'post',
        data: params
    })
}

// 查询文章列表
export function listArticle(query) {
    return request({
        url: '/customer/list',
        method: 'get',
        params: query
    })
}

// 查询文章详细
export function getArticle(id) {
    return request({
        url: '/customer/' + id,
        method: 'get'
    })
}

//获取留言板内容
export function getMessageList(query) {
    return request({
        url: '/customer/getMessageList',
        method: 'get',
        params: query
    })
}
