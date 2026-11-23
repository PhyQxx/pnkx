import config from '@/config'
import { createStreamRequest } from '@/utils/streamRequest'

/**
 * AI 创意文案流式生成（工具箱·免登录）
 * 超时 90s：预留后端模型降级（60s 无可见输出切下一模型）+ 生成时间
 * @param {string} scene 场景：pickup=土味情话 | acrostic=藏头诗 | letter=三行情书
 * @param {string} keyword 昵称或藏头字（选填）
 */
export function creativeStream(scene, keyword = '') {
  return createStreamRequest(config.baseUrl + '/ai/creative/stream', { scene, keyword }, { timeout: 90000 })
}
