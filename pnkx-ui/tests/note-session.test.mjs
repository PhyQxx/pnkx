import test from 'node:test'
import assert from 'node:assert/strict'
import { noteSession, saveSession } from '../src/utils/noteSession.js'
const note = { id: 1, title: 'A', content: 'old', revision: 1 }
test('serializes edits made while a save is in flight', async () => {
  const state = noteSession(note)
  state.draft.content = 'first'
  const sent = []
  let finish
  const send = snapshot => {
    sent.push(snapshot)
    if (sent.length === 1) return new Promise(resolve => { finish = () => resolve({ data: { ...snapshot, revision: 2 } }) })
    return Promise.resolve({ data: { ...snapshot, revision: 3 } })
  }
  const saving = saveSession(state, send)
  state.draft.content = 'second'
  assert.equal(saveSession(state, send), saving)
  finish()
  assert.equal(await saving, true)
  assert.deepEqual(sent.map(n => [n.content, n.revision]), [['first', 1], ['second', 2]])
  assert.equal(state.saved.content, 'second')
})
test('conflict preserves latest draft and pauses further writes', async () => {
  const state = noteSession(note)
  state.draft.content = 'local'
  const current = { ...note, content: 'remote', revision: 2 }
  let calls = 0
  const send = async () => { calls++; throw Object.assign(new Error('conflict'), { current }) }
  assert.equal(await saveSession(state, send), false)
  state.draft.content = 'more local edits'
  assert.equal(await saveSession(state, send), false)
  assert.equal(calls, 1)
  assert.equal(state.draft.content, 'more local edits')
  assert.equal(state.draft.revision, 1)
})
test('note sessions remain independent when user switches notes', async () => {
  const a = noteSession(note)
  const b = noteSession({ ...note, id: 2 })
  a.draft.content = 'edit A'
  b.draft.content = 'edit B'
  await saveSession(a, async snapshot => ({ data: { ...snapshot, revision: 2 } }))
  assert.equal(b.saved.content, 'old')
  assert.equal(b.draft.revision, 1)
})
test('restored stale draft requires explicit conflict resolution', () => {
  const state = noteSession({ ...note, revision: 2 }, { ...note, content: 'draft' })
  assert.equal(state.conflict.revision, 2)
  assert.equal(state.draft.content, 'draft')
})
