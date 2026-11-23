import config from '@/config'
import { createStreamRequest } from '@/utils/streamRequest'

const baseUrl = config.baseUrl

export function chatStream(question, messages = []) {
  return createStreamRequest(baseUrl + '/ai/chat/stream', { question, messages })
}

export function confirmPendingAction() {
  return createStreamRequest(baseUrl + '/ai/pending/confirm', {})
}

export function cancelPendingAction() {
  return createStreamRequest(baseUrl + '/ai/pending/cancel', {})
}
