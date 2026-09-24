import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function importSource(relativePath) {
  const source = await readFile(new URL('../' + relativePath, import.meta.url), 'utf8')
  return import('data:text/javascript;base64,' + Buffer.from(source).toString('base64'))
}

test('SSE parser preserves split lines, multiline events and DONE boundary', async () => {
  const { createSseParser } = await importSource('utils/sseParser.js')
  const events = []
  const parser = createSseParser(data => events.push(data))
  parser.push('da')
  parser.push('ta: 第一段\r\ndata: 第二')
  parser.push('行\r\n\r\ndata: [DO')
  parser.push('NE]\n\n')
  parser.push('data: 不应出现\n\n')
  parser.finish()
  assert.deepEqual(events, ['第一段\n第二行'])
  assert.equal(parser.isDone(), true)
})

test('push payload parsing and deep links cover every reminder source', async () => {
  const { parsePushPayload, resolvePushRoute } = await importSource('utils/pushRouter.js')
  const parsed = parsePushPayload({ data: { payload: JSON.stringify({
    type: 'life_reminder', sourceType: 'todo', sourceId: 42
  }) } })
  assert.equal(resolvePushRoute(parsed), '/pages_life/todo/edit?id=42&fromNotification=1')
  assert.equal(resolvePushRoute({ type: 'life_reminder', sourceType: 'commemoration', sourceId: 8 }),
    '/pages_life/commemorationDay/add?id=8&fromNotification=1')
  assert.equal(resolvePushRoute({ type: 'life_reminder', sourceType: 'subscription', sourceId: 5 }),
    '/pages_life/subscription/index?highlight=5')
  assert.equal(resolvePushRoute({ type: 'life_reminder', sourceType: 'menstruation' }),
    '/pages_life/menstruationAssistant/index')
  assert.equal(resolvePushRoute({ type: 'budget_alert', sourceType: 'budget' }),
    '/pages_life/bookkeeping/budget/index')
  assert.equal(resolvePushRoute({ type: 'life_reminder', sourceType: 'unknown' }), '/pages/index/index')
  assert.equal(parsePushPayload({ data: { payload: '{bad json' } }), null)
})

test('monthly export keeps authenticated download and system share menu', async () => {
  const api = await readFile(new URL('../api/px/life/bookkeeping/record.js', import.meta.url), 'utf8')
  const page = await readFile(new URL('../pages_life/bookkeeping/record/index.vue', import.meta.url), 'utf8')
  assert.match(api, /uni\.downloadFile\s*\(/)
  assert.match(api, /Authorization:\s*'Bearer '\s*\+\s*getToken\(\)/)
  assert.match(api, /statusCode\s*===\s*200/)
  assert.match(page, /uni\.openDocument\s*\(/)
  assert.match(page, /showMenu:\s*true/)
})

