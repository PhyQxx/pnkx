import request from '@/utils/request'

// ============ 行动中心（首页“今天要处理”聚合视图）============
// 端点与 pnkx-uniapp components/ActionCenter 保持一致

// 查询行动中心聚合数据（scope: personal/group；group 时带 groupId；page 分页待办）
export function getActionCenter(params) {
    return request({
        url: '/life/action-center',
        method: 'get',
        params
    })
}

// 查询可用空间（私人空间之外的家庭/情侣空间）
export function listActionCenterSpaces() {
    return request({
        url: '/life/action-center/spaces',
        method: 'get'
    })
}

// 查询固定入口偏好（entries_json + revision）
export function getActionCenterPreferences() {
    return request({
        url: '/life/action-center/preferences',
        method: 'get'
    })
}

// 保存固定入口顺序（乐观并发：expectedRevision 校验）
export function saveActionCenterPreferences(data) {
    return request({
        url: '/life/action-center/preferences',
        method: 'put',
        data
    })
}

// 提醒事件操作（action: read / snooze / dismiss）
// data: { requestId, expectedRevision, until }（snooze 时 until 为延后到的时间）
export function actReminderEvent(id, action, data) {
    return request({
        url: '/reminder/events/' + id + '/' + action,
        method: 'post',
        data
    })
}

// 出账失败处理（action: retry / dismiss）
export function retryChargeFailure(id, action) {
    return request({
        url: '/charge/instances/failures/' + id + '/' + action,
        method: 'post'
    })
}

// 纪念日准备待办：预览（服务端校验日期与来源）
export function previewCommemorationPreparation(id, data) {
    return request({
        url: '/life/action-center/commemorations/' + id + '/preview',
        method: 'post',
        data
    })
}

// 纪念日准备待办：按预览结果落库
export function commitCommemorationPreparation(data) {
    return request({
        url: '/life/action-center/commemorations/commit',
        method: 'post',
        data
    })
}

// 纪念日准备待办：忽略本次
export function dismissCommemorationPreparation(id, data) {
    return request({
        url: '/life/action-center/commemorations/' + id + '/dismiss',
        method: 'post',
        data
    })
}
