import request from '@/utils/customer-request'
/**
 * 根据文章ID获取留言列表
 */
export function getLeaveMessageByArticleId(params) {
    return request({
        url: '/px-leave-message/getLeaveMessageByArticleId',
        method: 'post',
        data: params
    })
}
