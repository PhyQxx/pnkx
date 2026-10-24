// Never delete pending/conflicted local changes; they require explicit user resolution.
export const sharedCacheTables = ['px_todo', 'px_shopping_list', 'px_shopping_item', 'px_meal_plan', 'px_commemoration_day']
export async function purgeSharedCache(db, owner) {
  if (owner == null || String(owner) === '') return
  const escaped = db._escapeValue(String(owner))
  for (const table of sharedCacheTables) {
    await db.executeSql(`DELETE FROM ${table} WHERE _sync_status = 1 AND (create_by IS NULL OR CAST(create_by AS TEXT) <> ${escaped})`)
  }
}
