import request from '@/utils/request'

/**
 * 全文检索
 * @param query
 * @returns {AxiosPromise}
 */
export function fullRetrieval(query) {
    return request({
        url: '/admin/fullRetrieval',
        method: 'get',
        params: query
    })
}

export function globalSearch(query) {
    return request({
        url: '/admin/globalSearch',
        method: 'get',
        params: query
    })
}

export function globalSearchPage(query) {
    return request({ url: '/admin/globalSearch/page', method: 'get', params: query })
}
