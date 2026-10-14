import storage from '@/utils/storage'
import constant from '@/utils/constant'
import { getToken } from '@/utils/auth'

// Never put tokens into database names or logs. Unknown legacy cache stays untouched.
export function getOfflineAccount() {
  const id = String(storage.get(constant.id) || '')
  return getToken() && /^[1-9][0-9]*$/.test(id) ? id : null
}
