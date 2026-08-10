import request from '@/utils/request'

// 获取日记心情分析数据
export function getDiaryAnalysisData(params) {
  return request({
    url: '/diary/analysis/data',
    method: 'get',
    params
  })
}
