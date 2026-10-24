import store from '@/store'
import { purgeSharedCache } from '@/utils/sharedCachePolicy'
// #ifdef APP-PLUS
import sqliteDB from '@/utils/sqliteDB'
// #endif
export async function clearSharedCache() {
  // #ifdef APP-PLUS
  await purgeSharedCache(sqliteDB, store.state.user.id)
  // #endif
}
