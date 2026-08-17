import request from '@/utils/request'

/**
 * AI聊天对话
 * @param {string} question - 用户问题
 */
export function chat(question) {
  return request({
    url: '/ai/chat',
    method: 'post',
    params: { question }
  })
}

/**
 * 获取默认AI模型配置
 */
export function getDefaultModel() {
  return request({
    url: '/aiModel/getDefault',
    method: 'get'
  })
}

/**
 * 获取AI模型列表
 */
export function listModels() {
  return request({
    url: '/aiModel/list',
    method: 'get'
  })
}

/**
 * AI流式对话
 * @param {string} question - 用户问题
 * @param {boolean} thinking - 是否开启思考模式
 * @returns {Promise<Response>} fetch Response with ReadableStream
 */
export function chatStream(question, messages = [], modelId = null, thinking = null) {
  const baseUrl = import.meta.env.VUE_APP_BASE_API
  const url = baseUrl + '/ai/chat/stream'
  const token = localStorage.getItem('Admin-Token') || localStorage.getItem('Token')
  const body = { question, messages }
  if (modelId) {
    body.modelId = modelId
  }
  if (thinking !== null) {
    body.thinking = thinking
  }
  return fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? 'Bearer ' + token : ''
    },
    body: JSON.stringify(body)
  })
}

function pendingActionStream(action) {
  const baseUrl = import.meta.env.VUE_APP_BASE_API
  const token = localStorage.getItem('Admin-Token') || localStorage.getItem('Token')
  return fetch(baseUrl + '/ai/pending/' + action, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? 'Bearer ' + token : ''
    }
  })
}

export function confirmPendingAction() {
  return pendingActionStream('confirm')
}

export function cancelPendingAction() {
  return pendingActionStream('cancel')
}
