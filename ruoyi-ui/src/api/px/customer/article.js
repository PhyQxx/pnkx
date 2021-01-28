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
