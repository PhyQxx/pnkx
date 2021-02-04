import request from '@/utils/request'

// 查询文章类型列表
export function listData(query) {
    return request({
        url: '/admin/getArticleTypeList',
        method: 'get',
        params: query
    })
}
// 校验字典项标签、键值唯一性
export function dictDataCheckUniqueness(query) {
    return request({
        url: '/admin/dictDataCheckUniqueness',
        method: 'post',
        params: query
    })
}
