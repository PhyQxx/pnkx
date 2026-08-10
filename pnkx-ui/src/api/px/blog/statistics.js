import request from '@/utils/request'

/**
 * 获取统计数据
 */
export function getStatistics(params) {
    return request({
        url: '/admin/statistics/getStatistics',
        method: 'post',
        data: params
    })
}

/**
 * 获取折线统计数据
 */
export function getLineChart(params) {
    return request({
        url: '/admin/statistics/getLineChart',
        method: 'post',
        data: params
    })
}

/**
 * 获取饼状统计数据
 */
export function getPieChart(params) {
    return request({
        url: '/admin/statistics/getPieChart',
        method: 'post',
        data: params
    })
}

/**
 * 获取更多统计数据
 */
export function getMoreStatistics(params) {
    return request({
        url: '/admin/statistics/getMoreStatistics',
        method: 'post',
        data: params
    })
}
