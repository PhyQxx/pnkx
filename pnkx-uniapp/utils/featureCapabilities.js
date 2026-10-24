import request from '@/utils/request'
import { getToken } from '@/utils/auth'

let cachedToken, cached, expires = 0, pending
export async function getFeatureCapabilities() {
  const token = getToken()
  if (!token) return {}
  if (cachedToken !== token) { cachedToken = token; cached = null; expires = 0; pending = null }
  if (cached && Date.now() < expires) return cached
  if (pending) return pending
  const task = request({ url: '/life/features', method: 'get' }).then(response => {
    if (cachedToken !== token || getToken() !== token) return {}
    cached = response.data || {}; expires = Date.now() + 30000
    return cached
  }).finally(() => { if (pending === task) pending = null })
  pending = task
  return task
}
