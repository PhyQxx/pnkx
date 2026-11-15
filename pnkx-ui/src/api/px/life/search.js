import request from '@/utils/request'

/**
 * 全局搜索（跨模块统一搜索：文章/待办/日记/笔记/账本/纪念日/订阅/菜谱/购物/书籍）
 * @param q 关键词
 * @param type 类型筛选（all 或具体类型）
 * @param filters 附加筛选 { startDate, endDate, tag }
 */
export function globalSearch(q, type = 'all', filters = {}) {
    return request({
        url: '/admin/globalSearch',
        method: 'get',
        params: { q, type, ...filters }
    })
}

/**
 * AI 总结搜索结果
 * @param data { q, type, startDate, endDate, tag, question }
 */
export function summarizeSearch(data) {
    return request({
        url: '/admin/globalSearch/summary',
        method: 'post',
        data: data
    })
}
