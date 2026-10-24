import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
const source = readFileSync(new URL('../utils/onlineActions.js', import.meta.url), 'utf8')
const { requiresOnline } = await import('data:text/javascript;base64,' + Buffer.from(source).toString('base64'))
test('financial, shared procurement and durable actions never enter the offline queue', () => {
  for (const url of ['/charge/instances/1/confirm', '/operation-plans/2/retry', '/shoppingItem/3/claim', '/shoppingItem/3/fulfillment', '/shoppingList/4/checkout', '/shopping-batches/5/record-expense', '/mealPlan/shopping-preview', '/mealPlan/transferToShopping', '/reminder/events/6/snooze', '/lifeReport/insights/7/accept', '/system/dataGroup/8/leave', '/bookkeeping/record', '/subscription', '/mealPlan', '/shoppingItem/clearChecked/4']) {
    assert.equal(requiresOnline({ url, method: 'post' }), true, url)
  }
})
test('server-only reads do not masquerade as empty cached success', () => {
  for (const url of ['/life/action-center?scope=group', '/shoppingList/4/batches', '/charge/instances?status=pending', '/operation-plans', '/system/dataGroup/list']) {
    assert.equal(requiresOnline({ url }), true, url)
  }
})
test('personal offline notes and ordinary cached lists stay available', () => {
  for (const config of [{url:'/note',method:'post'}, {url:'/admin/diary',method:'put'}, {url:'/shoppingItem/list'}, {url:'/bookkeeping/record/list'}]) {
    assert.equal(requiresOnline(config), false)
  }
})
