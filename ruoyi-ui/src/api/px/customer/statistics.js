/*统计接口*/
import request from '@/utils/customer-request'

/**
 * 获取统计数据
 */
export function getStatistics(params) {
    return request({
        url: '/customer/getStatistics',
        method: 'post',
        data: params
    })
}
