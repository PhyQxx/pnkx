import request from '@/utils/customer-request'

/**
 * 获取文章列表
 */
export function getArticleList(params) {
    return request({
        url: '/px-article/getArticleList',
        method: 'post',
        data: params
    })
}
/**
 * 获取文章分类分组数据
 */
export function getArticleTypeNumber(params) {
    return request({
        url: '/px-article/getArticleTypeNumber',
        method: 'post',
        data: params
    })
}

/**
 * 根据文章ID获取留言列表
 */
export function getLeaveMessageByArticleId(params) {
    return request({
        url: '/px-article/getLeaveMessageByArticleId',
        method: 'post',
        data: params
    })
}

/**
 * 留言
 */
export function addMessage(params) {
    return request({
        url: '/px-article/addMessage',
        method: 'post',
        data: params
    })
}
