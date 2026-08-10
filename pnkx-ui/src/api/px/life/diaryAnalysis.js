import request from '@/utils/request'

export function getDiaryAnalysisData(params) {
  return request({
    url: '/diary/analysis/data',
    method: 'get',
    params
  })
}
