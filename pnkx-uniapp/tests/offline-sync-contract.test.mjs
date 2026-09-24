import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = (path) => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

test('extended modules have local tables and incremental pull endpoints', () => {
  const db = read('utils/sqliteDB.js')
  const scheduler = read('utils/syncScheduler.js')
  const modules = [
    ['px_shopping_list', 'shoppingList'],
    ['px_shopping_item', 'shoppingItem'],
    ['px_recipe', 'recipe'],
    ['px_meal_plan', 'mealPlan'],
    ['px_subscription', 'subscription'],
    ['px_menstruation_record', 'menstruation'],
    ['px_bookkeeping_budget', 'budget'],
    ['px_bookkeeping_recurring', 'recurring'],
    ['px_book', 'book']
  ]

  for (const [table, module] of modules) {
    assert.match(db, new RegExp(`CREATE TABLE IF NOT EXISTS ${table.replaceAll('_', '\\_')}`))
    assert.match(scheduler, new RegExp(`/offline/sync/extended/${module}`))
  }
})

test('pagination advances by raw page size and applies server soft deletes', () => {
  const scheduler = read('utils/syncScheduler.js')
  assert.match(scheduler, /offset \+= res\.data\.items\.length/)
  assert.match(scheduler, /deletedServerIds\.push\(item\.id\)/)
  assert.ok(scheduler.includes('DELETE FROM ${mod.table} WHERE \\`_server_id\\` IN'))
})

test('core writes use idempotent batch and conflicts remain user-resolvable', () => {
  const scheduler = read('utils/syncScheduler.js')
  const queue = read('utils/offlineQueue.js')
  assert.match(scheduler, /IDEMPOTENT_BATCH_TABLES/)
  assert.match(scheduler, /url: '\/offline\/batch'/)
  assert.match(queue, /async retry\(taskId\)/)
  assert.match(queue, /async discard\(taskId, removeLocal = false\)/)
  assert.match(queue, /async replacePayload\(taskId, payload\)/)
})
