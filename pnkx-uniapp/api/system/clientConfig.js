import request from '@/utils/request'

// 根据参数键名查询参数值（客户端免登录）
export function getClientConfigKey(configKey) {
  return request({
    url: '/client/config/configKey/' + configKey,
    method: 'get'
  })
}
