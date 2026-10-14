import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync, mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { execFileSync } from 'node:child_process'
const read = name => readFileSync(new URL('../utils/' + name, import.meta.url), 'utf8')
const moduleUrl = 'data:text/javascript;base64,' + Buffer.from(read('noteOffline.js')).toString('base64')
const { writeOfflineNote, withNoteWriteLock } = await import(moduleUrl)
function load(name, deps) {
  const source = read(name).replace(/\/\/ #ifndef APP-PLUS[\s\S]*?\/\/ #endif/g, '')
    .replace(/^import .*$/gm, '').replace(/export default (\w+)/, 'return $1')
  return new Function(...Object.keys(deps), source)(...Object.values(deps))
}
async function fixture(t) {
  const dir = mkdtempSync(join(tmpdir(), 'pnkx-note-sqlite-'))
  t.after(() => rmSync(dir, { recursive: true, force: true }))
  const file = join(dir, 'test.db')
  const db = load('sqliteDB.js', { withNoteWriteLock })
  db.executeSql = async sql => { execFileSync('sqlite3', [file, sql]) }
  db.selectSql = async sql => JSON.parse(execFileSync('sqlite3', ['-json', file, sql], { encoding: 'utf8' }) || '[]')
  await db.initTables()
  const queue = load('offlineQueue.js', { sqliteDB: db })
  let next = 0
  const write = data => writeOfflineNote({ url: '/note', method: 'put', data }, db, queue, () => 'op-' + ++next)
  return { db, queue, write, uuid: () => 'op-' + ++next }
}
test('additive revision migration retains dirty rows and pending operations', async t => {
  const { db, queue } = await fixture(t)
  await db.executeSql('ALTER TABLE px_note DROP COLUMN revision')
  await db.insert('px_note', { title: 'unsaved', content: 'draft', _sync_status: 0 })
  await queue.enqueue({ id: 'pending', tableName: 'px_note', method: 'put', payload: { content: 'draft' } })
  await db.initTables()
  assert.equal((await db.selectSql('SELECT * FROM px_note'))[0].content, 'draft')
  assert.equal((await db.selectSql('SELECT * FROM px_note'))[0].revision, null)
  assert.equal((await queue.getPendingTasks()).length, 1)
})
test('consecutive offline edits keep one operation, original revision and server identity', async t => {
  const { db, queue, write } = await fixture(t)
  await db.insert('px_note', { title: 'A', content: 'old', revision: 4, _server_id: 99, _sync_status: 1, client_uuid: 'original' })
  await write({ id: 99, title: 'A', content: 'first', revision: 4 })
  await write({ id: 99, title: 'A', content: 'second', revision: 4 })
  const tasks = await queue.getPendingTasks()
  assert.equal(tasks.length, 1)
  assert.deepEqual([JSON.parse(tasks[0].payload).revision, JSON.parse(tasks[0].payload).content], [4, 'second'])
  const local = (await db.selectSql('SELECT * FROM px_note'))[0]
  assert.equal(local._server_id, 99)
  assert.equal(local.client_uuid, 'original')
  await db.upsertBatch('px_note', [{ id: 99, content: 'remote', revision: 5 }])
  assert.equal((await db.getById('px_note', local.id)).content, 'second')
})
test('unsynced creation can be edited without becoming an update of a server ID', async t => {
  const { db, queue, write, uuid } = await fixture(t)
  const result = await writeOfflineNote({ url: '/note', method: 'post', data: { title: 'new', content: 'first' } }, db, queue, uuid)
  await write({ ...result.data, content: 'second' })
  const tasks = await queue.getPendingTasks()
  assert.equal(tasks.length, 1)
  assert.equal(tasks[0].method, 'post')
  assert.equal(JSON.parse(tasks[0].payload).id, undefined)
  assert.equal(JSON.parse(tasks[0].payload).content, 'second')
})
test('a retry or conflict cannot be changed under an idempotent operation ID', async t => {
  const { db, queue, write } = await fixture(t)
  await db.insert('px_note', { title: 'A', revision: 1, _server_id: 99, _sync_status: 1 })
  await write({ id: 99, content: 'first', revision: 1 })
  const [task] = await queue.getPendingTasks()
  await queue.incrementRetry(task.id)
  await assert.rejects(write({ id: 99, content: 'later', revision: 1 }), /待处理/)
  assert.equal(JSON.parse((await queue.getPendingTasks())[0].payload).content, 'first')
})
test('upload conflict immediately preserves both versions and stops retries', async t => {
  const { db, queue, write } = await fixture(t)
  await db.insert('px_note', { title: 'A', revision: 1, _server_id: 99, _sync_status: 1 })
  await write({ id: 99, content: 'draft', revision: 1 })
  let calls = 0
  const scheduler = load('syncScheduler.js', { getOfflineAccount: () => '101', withNoteWriteLock, sqliteDB: db, offlineQueue: queue,
    isOnline: { value: true }, setOnNetworkRestore() {}, request: async () => {
      calls++
      return { code: 200, data: { results: [{ status: 'conflict', errorCode: 'NOTE_REVISION_CONFLICT',
        errorMsg: 'conflict', current: { id: 99, content: 'remote', revision: 2 } }] } }
    } })
  await scheduler.syncPendingTasks()
  await scheduler.syncPendingTasks()
  assert.equal(calls, 1)
  const [conflict] = await queue.getConflictTasks()
  assert.equal(conflict.retry_count, 0)
  assert.equal(JSON.parse(conflict.payload).content, 'draft')
  assert.equal(JSON.parse(conflict.error_msg).current.content, 'remote')
})
test('successful upload persists new revision and merged content into local cache', async t => {
  const { db, queue, write } = await fixture(t)
  await db.insert('px_note', { title: 'A', content: 'old', revision: 1, _server_id: 99, _sync_status: 1 })
  await write({ id: 99, content: 'draft', revision: 1 })
  const [task] = await queue.getPendingTasks()
  await queue.replacePayload(task.id, { ...JSON.parse(task.payload), content: 'merged', revision: 2 })
  const scheduler = load('syncScheduler.js', { getOfflineAccount: () => '101', withNoteWriteLock, sqliteDB: db, offlineQueue: queue,
    isOnline: { value: true }, setOnNetworkRestore() {}, request: async () => (
      { code: 200, data: { results: [{ status: 'success', id: 99, revision: 3 }] } }) })
  await scheduler.syncPendingTasks()
  const [row] = await db.selectSql('SELECT * FROM px_note')
  assert.deepEqual([row.content, row.revision, row._sync_status], ['merged', 3, 1])
  await db.upsertBatch('px_note', [{ id: 99, content: 'stale cache', revision: 2 }])
  assert.equal((await db.getById('px_note', row.id)).content, 'merged')
})
test('server tombstones preserve local dirty notes during incremental pull', async t => {
  const { db, queue } = await fixture(t)
  await db.insert('px_note', { title: 'dirty', content: 'keep', revision: 1, _server_id: 99, _sync_status: 0 })
  await db.insert('px_note', { title: 'clean', revision: 1, _server_id: 100, _sync_status: 1 })
  const scheduler = load('syncScheduler.js', { getOfflineAccount: () => '101', withNoteWriteLock, sqliteDB: db, offlineQueue: queue,
    isOnline: { value: true }, setOnNetworkRestore() {}, request: async () => (
      { code: 200, data: { items: [{ id: 99, delFlag: 1, revision: 2 }, { id: 100, delFlag: 1, revision: 2 }], hasMore: false,
        nextCursor: 'n2:101:2' } }) })
  await scheduler._pullModule({ table: 'px_note', syncUrl: '/offline/sync/note' })
  const rows = await db.selectSql('SELECT * FROM px_note')
  assert.equal(rows.length, 1)
  assert.equal(rows[0].content, 'keep')
})

test('failed note page never advances the committed cursor', async t => {
  const { db, queue } = await fixture(t)
  await db.executeSql("INSERT INTO _sync_cursor(table_name,last_cursor) VALUES ('px_note','n2:101:1')")
  const scheduler = load('syncScheduler.js', { getOfflineAccount: () => '101', withNoteWriteLock, sqliteDB: db, offlineQueue: queue,
    isOnline: { value: true }, setOnNetworkRestore() {}, request: async () => ({ code: 200, data: {
      items: [{ id: 99, delFlag: 1, revision: 2 }], nextCursor: 'n2:101:2', hasMore: false } }) })
  const execute = db.executeSql
  db.executeSql = async sql => { if (sql.startsWith('DELETE FROM px_note')) throw new Error('simulated disk error'); return execute(sql) }
  await scheduler._pullNotes()
  assert.equal((await db.selectSql("SELECT last_cursor FROM _sync_cursor WHERE table_name='px_note'"))[0].last_cursor,'n2:101:1')
})

test('native SQLite uses separate account files and rejects anonymous access', async () => {
  let account = '101'
  const opened = []
  const db = load('sqliteDB.js', { withNoteWriteLock, getOfflineAccount: () => account, plus: { sqlite: {
    openDatabase(args) { opened.push({ name: args.name, path: args.path }); args.success() },
    executeSql(args) { args.success() }
  } } })
  await db.executeSql('SELECT 1')
  account = '202'
  await db.executeSql('SELECT 1')
  assert.deepEqual(opened.map(x => x.path), ['_doc/pnkx_offline_101.db','_doc/pnkx_offline_202.db'])
  account = null
  await assert.rejects(() => db.executeSql('SELECT 1'))
})

test('late note pull from another account is never written to the new cache', async t => {
  const { db, queue } = await fixture(t)
  let account = '101'
  const scheduler = load('syncScheduler.js', { getOfflineAccount: () => account, withNoteWriteLock, sqliteDB: db, offlineQueue: queue,
    isOnline: { value: true }, setOnNetworkRestore() {}, request: async () => {
      account = '202'
      return { code: 200, data: { items: [{ id: 99, title: 'private', revision: 1 }], nextCursor: 'n2:101:1', hasMore: false } }
    } })
  await scheduler._pullNotes()
  assert.equal((await db.selectSql('SELECT * FROM px_note')).length,0)
  assert.equal((await db.selectSql('SELECT * FROM _sync_cursor')).length,0)
})
