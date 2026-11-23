import { getToken } from '@/utils/auth'

/**
 * 流式 POST 请求封装（SSE 文本流）
 *
 * 返回 { onChunk(callback), completionPromise }：
 * - onChunk(cb)：注册文本片段回调，cb 收到增量文本字符串
 * - completionPromise：响应结束（resolve）/ 失败（reject）的 Promise
 *
 * 分端实现：
 * - H5 / App：XMLHttpRequest onprogress 增量读取
 * - 微信小程序：uni.request enableChunked + onChunkReceived（无 TextDecoder，内置 UTF-8 增量解码）
 * - 其他不支持 chunked 的端：自动降级为整包接收（onChunk 收到完整响应体）
 *
 * 统一使用 SSE 帧格式（data: 行 + 空行分隔，data:[DONE] 结束），
 * 解析逻辑见各调用方（如 pages_blog/ai/chat.vue readStreamResponse）。
 */

/**
 * 构建流式请求
 * @param {string} url 完整 URL
 * @param {Object} data 请求体（JSON）
 * @param {Object} options 可选项：{ timeout }
 * @returns {{onChunk: Function, completionPromise: Promise<void>}}
 */
export function createStreamRequest(url, data, options = {}) {
  const token = getToken()
  const timeout = options.timeout || 120000
  let _onChunk = null
  let _resolve, _reject

  const completionPromise = new Promise((resolve, reject) => {
    _resolve = resolve
    _reject = reject
  })

  const header = { 'Content-Type': 'application/json' }
  if (token) header.Authorization = 'Bearer ' + token

  // #ifdef H5 || APP-PLUS
  const xhr = new XMLHttpRequest()
  let lastIndex = 0

  xhr.open('POST', url)
  xhr.timeout = timeout
  xhr.onprogress = () => {
    const newData = xhr.responseText.substring(lastIndex)
    lastIndex = xhr.responseText.length
    if (_onChunk && newData) _onChunk(newData)
  }
  xhr.onload = () => _resolve()
  xhr.onerror = () => _reject(new Error('Network error'))
  xhr.ontimeout = () => _reject(new Error('Timeout'))
  xhr.onabort = () => _reject(new Error('Aborted'))
  xhr.send(JSON.stringify(data))
  // #endif

  // #ifndef H5 || APP-PLUS
  let receivedChunks = false
  const decoder = createUtf8Decoder()
  const requestTask = uni.request({
    url,
    method: 'POST',
    data,
    header,
    timeout,
    // 微信小程序分块接收；不支持的端会忽略该参数并整包返回
    enableChunked: true,
    success: (res) => {
      // chunked 未生效的端：整包兜底（正常字符串响应体一次性交付）
      if (!receivedChunks && res.data && typeof res.data === 'string') {
        if (_onChunk) _onChunk(res.data)
      }
      _resolve()
    },
    fail: () => _reject(new Error('Network error'))
  })
  if (requestTask && requestTask.onChunkReceived) {
    requestTask.onChunkReceived((res) => {
      receivedChunks = true
      const text = decoder.decode(res.data)
      if (_onChunk && text) _onChunk(text)
    })
  }
  // #endif

  return {
    onChunk(callback) { _onChunk = callback },
    completionPromise
  }
}

/**
 * UTF-8 增量解码器（替代小程序环境缺失的 TextDecoder）
 * 保留跨 chunk 的不完整多字节序列，待下一片段续接
 */
function createUtf8Decoder() {
  let pending = []
  return {
    /**
     * 解码一个二进制片段
     * @param {ArrayBuffer} buffer 原始字节
     * @returns {string}
     */
    decode(buffer) {
      if (!buffer) return ''
      const incoming = new Uint8Array(buffer)
      const all = pending.length > 0 ? pending.concat(Array.from(incoming)) : Array.from(incoming)
      pending = []
      const chars = []
      let i = 0
      while (i < all.length) {
        const b = all[i]
        let need = 0
        let cp = 0
        if (b < 0x80) {
          need = 0
          cp = b
        } else if ((b & 0xE0) === 0xC0) {
          need = 1
          cp = b & 0x1F
        } else if ((b & 0xF0) === 0xE0) {
          need = 2
          cp = b & 0x0F
        } else if ((b & 0xF8) === 0xF0) {
          need = 3
          cp = b & 0x07
        } else {
          // 非法首字节，跳过
          i += 1
          continue
        }
        if (i + need >= all.length) {
          // 末尾字符不完整：留给下一个片段
          pending = all.slice(i)
          break
        }
        let valid = true
        for (let k = 1; k <= need; k++) {
          const cb = all[i + k]
          if ((cb & 0xC0) !== 0x80) {
            valid = false
            break
          }
          cp = (cp << 6) | (cb & 0x3F)
        }
        if (!valid) {
          i += 1
          continue
        }
        i += need + 1
        if (cp > 0xFFFF) {
          // 4 字节序列 → 代理对
          cp -= 0x10000
          chars.push(String.fromCharCode(0xD800 + (cp >> 10)))
          chars.push(String.fromCharCode(0xDC00 + (cp & 0x3FF)))
        } else {
          chars.push(String.fromCharCode(cp))
        }
      }
      return chars.join('')
    }
  }
}
