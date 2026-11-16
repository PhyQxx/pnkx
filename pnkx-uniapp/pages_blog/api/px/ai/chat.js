import config from '@/config'
import { getToken } from '@/utils/auth'

const baseUrl = config.baseUrl

function createStreamRequest(url, data) {
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

export function chatStream(question, messages = []) {
  return createStreamRequest(baseUrl + '/ai/chat/stream', { question, messages })
}

export function confirmPendingAction() {
  return createStreamRequest(baseUrl + '/ai/pending/confirm', {})
}

export function cancelPendingAction() {
  return createStreamRequest(baseUrl + '/ai/pending/cancel', {})
}
