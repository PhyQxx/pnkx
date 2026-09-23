import config from '@/config'
import { getToken } from '@/utils/auth'

const baseUrl = config.baseUrl

/**
 * AI 流式对话请求（跨端）：
 * - H5：XMLHttpRequest 流式读取（onprogress 增量）
 * - App / 小程序：uni.request + enableChunked + onChunkReceived（需 App 3.7.2+ / 小程序基础库 2.20.1+）
 * 对外接口统一为 { onChunk(cb), completionPromise }，cb 收到 UTF-8 增量文本。
 */

// ==================== H5 实现 ====================
function createStreamRequestH5(url, data) {
  const token = getToken()
  const xhr = new XMLHttpRequest()
  let _resolve, _reject, _onChunk
  let lastIndex = 0

  const completionPromise = new Promise((resolve, reject) => {
    _resolve = resolve
    _reject = reject
  })

  xhr.open('POST', url)
  xhr.setRequestHeader('Content-Type', 'application/json')
  if (token) xhr.setRequestHeader('Authorization', 'Bearer ' + token)
  xhr.timeout = 120000

  xhr.onprogress = () => {
    const newData = xhr.responseText.substring(lastIndex)
    lastIndex = xhr.responseText.length
    if (_onChunk && newData) _onChunk(newData)
  }

  xhr.onload = () => _resolve()
  xhr.onerror = () => _reject(new Error('Network error'))
  xhr.ontimeout = () => _reject(new Error('Timeout'))

  xhr.send(JSON.stringify(data))

  return {
    onChunk(callback) { _onChunk = callback },
    completionPromise
  }
}

// ==================== App / 小程序实现 ====================
// 无 TextDecoder 环境的手写 UTF-8 解码（带跨 chunk 多字节前缀缓冲）
function makeChunkDecoder() {
  let leftover = []
  return function decode(buffer) {
    if (typeof TextDecoder !== 'undefined') {
      return new TextDecoder('utf-8').decode(buffer, { stream: true })
    }
    const out = []
    const bytes = new Uint8Array(leftover.length + buffer.byteLength)
    bytes.set(leftover)
    bytes.set(new Uint8Array(buffer), leftover.length)
    let i = 0
    for (; i < bytes.length; i++) {
      const b = bytes[i]
      let len, cp
      if (b < 0x80) { len = 1; cp = b }
      else if (b < 0xE0) { len = 2; cp = b & 0x1F }
      else if (b < 0xF0) { len = 3; cp = b & 0x0F }
      else { len = 4; cp = b & 0x07 }
      if (i + len > bytes.length) break
      for (let j = 1; j < len; j++) cp = (cp << 6) | (bytes[i + j] & 0x3F)
      if (cp > 0xFFFF) {
        const offset = cp - 0x10000
        out.push(String.fromCharCode(0xD800 + (offset >> 10), 0xDC00 + (offset & 0x3FF)))
      } else {
        out.push(String.fromCharCode(cp))
      }
      i += len - 1
    }
    leftover = Array.from(bytes.slice(i))
    return out.join('')
  }
}

function createStreamRequestUni(url, data) {
  const token = getToken()
  let _resolve, _reject, _onChunk
  const completionPromise = new Promise((resolve, reject) => {
    _resolve = resolve
    _reject = reject
  })
  const decode = makeChunkDecoder()

  const task = uni.request({
    url,
    method: 'POST',
    data,
    timeout: 120000,
    enableChunked: true,
    header: Object.assign(
      { 'Content-Type': 'application/json' },
      token ? { 'Authorization': 'Bearer ' + token } : {}
    ),
    success: () => _resolve(),
    fail: (err) => _reject(new Error((err && err.errMsg) || 'request fail'))
  })

  if (task && typeof task.onChunkReceived === 'function') {
    task.onChunkReceived((res) => {
      if (_onChunk && res && res.data) {
        const text = decode(res.data)
        if (text) _onChunk(text)
      }
    })
  } else {
    // 运行环境不支持分块传输（低版本基础库），明确报错由调用方降级处理
    _reject(new Error('当前环境不支持流式响应'))
  }

  return {
    onChunk(callback) { _onChunk = callback },
    completionPromise
  }
}

// ==================== 按平台选择实现 ====================
// #ifdef H5
const createStreamRequest = createStreamRequestH5
// #endif
// #ifndef H5
const createStreamRequest = createStreamRequestUni
// #endif

export function chatStream(question, messages = []) {
  return createStreamRequest(baseUrl + '/ai/chat/stream', { question, messages })
}

export function confirmPendingAction() {
  return createStreamRequest(baseUrl + '/ai/pending/confirm', {})
}

export function cancelPendingAction() {
  return createStreamRequest(baseUrl + '/ai/pending/cancel', {})
}
