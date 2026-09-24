import request from '@/utils/request'
import config from '@/config'
import { getToken } from '@/utils/auth'
import { tansParams } from '@/utils/common'

// 查询账本记录列表
export function listRecord(query) {
  return request({
    url: '/bookkeeping/record/list',
    method: 'get',
    params: query
  })
}

// 查询账本记录详细
export function getRecord(id) {
  return request({
    url: '/bookkeeping/record/' + id,
    method: 'get'
  })
}

// 新增账本记录
export function addRecord(data) {
  return request({
    url: '/bookkeeping/record',
    method: 'post',
    data: data
  })
}

// 修改账本记录
export function updateRecord(data) {
  return request({
    url: '/bookkeeping/record',
    method: 'put',
    data: data
  })
}

// 删除账本记录
export function delRecord(id) {
  return request({
    url: '/bookkeeping/record/' + id,
    method: 'delete'
  })
}

// 导出账本记录
export function exportRecord(query) {
  return request({
    url: '/bookkeeping/record/export',
    method: 'get',
    params: query
  })
}

// 下载月度账单文件，返回临时文件路径
export function downloadRecordExport(query) {
  return exportRecord(query).then(res => new Promise((resolve, reject) => {
    const queryString = tansParams({ fileName: res.msg, delete: true }).replace(/&$/, '')
    uni.downloadFile({
      url: config.baseUrl + '/common/download?' + queryString,
      header: { Authorization: 'Bearer ' + getToken() },
      success(downloadRes) {
        if (downloadRes.statusCode === 200) resolve(downloadRes.tempFilePath)
        else reject(new Error('下载失败，状态码：' + downloadRes.statusCode))
      }, fail: reject
    })
  }))
}

// AI解析自然语言为记账数据
export function aiParse(text) {
  return request({
    url: '/bookkeeping/record/aiParse',
    method: 'post',
    params: { text }
  })
}
