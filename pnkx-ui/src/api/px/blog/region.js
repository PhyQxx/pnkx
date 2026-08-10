import request from '@/utils/request'

/**
 * 获取地区信息列表
 * @param query
 * @returns {*}
 */
export function getRegionList(query) {
    return request({
        url: '/client/getRegionList',
        method: 'get',
        params: query
    })
}
