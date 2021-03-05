/*统计接口*/
import request from '@/utils/customer-request'

/**
 * 获取统计数据
 */
export function getStatistics(params) {
    return request({
        url: '/admin/statistics/getStatistics',
        method: 'post',
    })
}

/**
 * 获取统计数据
 */
export function getLineChart(params) {
    return request({
        url: '/admin/statistics/getLineChart',
        method: 'post',
    })
}
