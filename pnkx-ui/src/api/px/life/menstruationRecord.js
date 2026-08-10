import request from '@/utils/request'

// 查询姨妈记录列表
export function listMenstruationRecord(query) {
    return request({
        url: '/myTool/menstruationRecord/list',
        method: 'get',
        params: query
    })
}

// 查询姨妈记录列表
export function getPxMenstruationRecordList(query) {
    return request({
        url: '/myTool/menstruationRecord/getPxMenstruationRecordList',
        method: 'get',
        params: query
    })
}

/**
 * 获取最后一次开始时间
 * @returns {*}
 */
export function getLastStartDate() {
    return request({
        url: '/myTool/menstruationRecord/getLastStartDate',
        method: 'get',
    })
}


// 查询姨妈记录详细
export function getMenstruationRecord(id) {
    return request({
        url: '/myTool/menstruationRecord/' + id,
        method: 'get'
    })
}

// 新增姨妈记录
export function addMenstruationRecord(data) {
    return request({
        url: '/myTool/menstruationRecord',
        method: 'post',
        data: data
    })
}

// 修改姨妈记录
export function updateMenstruationRecord(data) {
    return request({
        url: '/myTool/menstruationRecord',
        method: 'put',
        data: data
    })
}

// 删除姨妈记录
export function delMenstruationRecord(id) {
    return request({
        url: '/myTool/menstruationRecord/' + id,
        method: 'delete'
    })
}

// 导出姨妈记录
export function exportMenstruationRecord(query) {
    return request({
        url: '/myTool/menstruationRecord/export',
        method: 'get',
        params: query
    })
}
