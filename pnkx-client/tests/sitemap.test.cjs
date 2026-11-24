const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const compiled = { exports: {} }
new Function('exports', ts.transpileModule(fs.readFileSync('src/server/utils/sitemapArticles.ts', 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
}).outputText)(compiled.exports)
const { collectSitemapArticles } = compiled.exports

test('fetches all 1201 articles in stable page order', async () => {
  const all = Array.from({length: 1201}, (_, i) => ({id: i + 1}))
  const calls = []
  const rows = await collectSitemapArticles(async (page, size) => {
    calls.push(page)
    return {code: 200, total: all.length, rows: all.slice((page - 1) * size, page * size)}
  })
  assert.deepEqual(rows, all)
  assert.deepEqual(calls, [1, 2, 3])
})
test('empty blog returns an empty article list', async () => {
  assert.deepEqual(await collectSitemapArticles(async () => ({code: 200, total: 0, rows: []})), [])
})
test('rejects repeated pages and business errors', async () => {
  await assert.rejects(collectSitemapArticles(async () => ({code: 200, total: 2, rows: [{id: 1}]})), /no progress/)
  await assert.rejects(collectSitemapArticles(async () => ({code: 500, total: 0, rows: []})), /Invalid/)
})
test('rejects partial backend failures and unsafe IDs', async () => {
  await assert.rejects(collectSitemapArticles(async page => {
    if (page === 2) throw Error('backend unavailable')
    return {code: 200, total: 2, rows: [{id: 1}]}
  }), /backend unavailable/)
  await assert.rejects(collectSitemapArticles(async () => ({code: 200, total: 1, rows: [{id: 'bad'}]})), /Invalid article ID/)
})
