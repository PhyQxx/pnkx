import request from '@/utils/request'

// 查询文章类型列表
export function listData(query) {
    return request({
        url: '/admin/article/getArticleTypeList',
        method: 'get',
        params: query
    })
}

