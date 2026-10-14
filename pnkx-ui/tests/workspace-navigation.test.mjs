import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

// Execute the actual SFC methods with controlled network completion order.
async function component(file, api = {}, globals = {}) {
  const source = await readFile(new URL(file.startsWith('mobile:') ? '../../pnkx-uniapp/pages_life/workspace/' + file.slice(7) : '../src/views/px/workspace/' + file, import.meta.url), 'utf8')
  const names = []
  let script = source.match(/<script>([\s\S]*?)<\/script>/)[1]
  script = script.replace(/^import\s+(.+?)\s+from\s+['"].+?['"]\s*$/gm, (_, bindings) => {
    names.push(...bindings.replace(/[{}]/g, '').split(',').map(name => name.trim()))
    return ''
  })
  const options = new Function(...names, script.replace('export default', 'return'))(...names.map(name => api[name] || (() => { throw new Error('Unexpected API: ' + name) })))
  const vm = { $route: { query: {} }, $message: { success() {}, error() {} }, ...globals }
  Object.assign(vm, options.data.call(vm))
  for (const [name, method] of Object.entries(options.methods)) vm[name] = method.bind(vm)
  return { vm, options }
}
function deferred() { let resolve; const promise = new Promise(done => { resolve = done }); return { promise, resolve } }
const file = id => ({ id, parentId: 0, isFolder: false, mime: 'text/plain', displayName: id })
const listing = rows => ({ data: { rows, hasMore: false, nextAfter: 0, usedBytes: 0, quotaBytes: 100 } })

test('slow text decoding cannot replace a newer file preview', async () => {
  const text = deferred(), started = deferred()
  const { vm } = await component('files.vue', { fileContent: async id => ({ type: 'text/plain', text: () => id === 'A' ? (started.resolve(), text.promise) : Promise.resolve('body B') }) })
  const old = vm.preview(file('A')); await started.promise
  await vm.preview(file('B')); text.resolve('body A'); await old
  assert.equal(vm.current.id, 'B'); assert.equal(vm.previewText, 'body B')
})

test('closing or unmounting invalidates a pending text preview', async () => {
  for (const action of ['close', 'unmount']) {
    const text = deferred(), started = deferred()
    const { vm, options } = await component('files.vue', { fileContent: async () => ({ type: 'text/plain', text: () => (started.resolve(), text.promise) }) })
    const opening = vm.preview(file('A')); await started.promise
    if (action === 'close') vm.clearPreview(); else options.beforeUnmount.call(vm)
    text.resolve('private body'); await opening
    assert.equal(vm.current, null); assert.equal(vm.previewText, ''); assert.equal(vm.previewing, false)
  }
})

test('late route lookup cannot open the previous folder', async () => {
  const old = deferred(), started = deferred()
  const { vm } = await component('files.vue', { listFileFolders: async () => ({ data: [] }), getFile: id => id === 'A' ? (started.resolve(), old.promise) : Promise.resolve({ data: { id: 'B', isFolder: true } }), listFiles: async parent => listing([{ id: parent }]) }, { $route: { query: { id: 'A' } } })
  const first = vm.init(); await started.promise
  vm.$route.query.id = 'B'; await vm.init(); old.resolve({ data: { id: 'A', isFolder: true } }); await first
  assert.equal(vm.parent, 'B'); assert.equal(vm.rows[0].id, 'B')
})

test('history lists belong to the latest route even when responses arrive backwards', async () => {
  const old = deferred()
  const { vm } = await component('recovery.vue', { noteHistory: id => id === 'A' ? old.promise : Promise.resolve({ data: [{ title: 'B', revision: 2 }] }) }, { $route: { query: { id: 'A' } } })
  const first = vm.init(); vm.id = 'B'; await vm.init(); old.resolve({ data: [{ title: 'A', revision: 1 }] }); await first
  assert.equal(vm.history[0].title, 'B'); assert.equal(vm.loading, false)
})

test('switching versions immediately hides the old restore action', async () => {
  const old = deferred()
  const { vm } = await component('recovery.vue', { noteVersion: () => old.promise, getResource: async () => ({ data: { revision: 4 } }) })
  vm.id = 'A'; vm.snapshot = { revision: 1 }; vm.version = 2
  const loading = vm.compare(); assert.equal(vm.snapshot, null)
  old.resolve({ data: { revision: 2 } }); await loading; assert.equal(vm.snapshot.revision, 2)
})

test('confirmation for an old note cannot restore the new route', async () => {
  const confirmation = deferred(), writes = []
  const { vm } = await component('recovery.vue', { restoreVersion: async (...args) => writes.push(args) }, { $confirm: () => confirmation.promise })
  vm.id = 'A'; vm.snapshot = { revision: 1 }; vm.current = { revision: 3 }
  const restoring = vm.restore(); vm.id = 'B'; vm.snapshot = { revision: 2 }; vm.current = { revision: 8 }
  confirmation.resolve(); await restoring; assert.deepEqual(writes, [])
})

test('trash pagination ignores slower previous page responses', async () => {
  const old = deferred()
  const { vm } = await component('recovery.vue', { listTrash: ({ pageNum }) => pageNum === 1 ? old.promise : Promise.resolve({ data: { rows: [{ id: 2 }], total: 21 } }) })
  const first = vm.loadTrash(1); await vm.loadTrash(2); old.resolve({ data: { rows: [{ id: 1 }], total: 21 } }); await first
  assert.equal(vm.page, 2); assert.equal(vm.rows[0].id, 2)
})


test('mobile history cannot apply a confirmation after leaving the page', async () => {
  let confirm
  const writes = []
  const previous = globalThis.uni
  globalThis.uni = { showModal(options) { confirm = options.success } }
  try {
    const { vm, options } = await component('mobile:recovery.vue', { restoreVersion: async (...args) => writes.push(args) })
    vm.id = 'A'; vm.snapshot = { revision: 1 }; vm.current = { revision: 3 }
    vm.restore(); options.onUnload.call(vm); await confirm({ confirm: true })
    assert.deepEqual(writes, [])
  } finally { globalThis.uni = previous }
})

test('mobile history hides the previous snapshot while loading and ignores it after unload', async () => {
  const response = deferred()
  const { vm, options } = await component('mobile:recovery.vue', { noteVersion: () => response.promise, getResource: async () => ({ data: { revision: 3 } }) })
  vm.id = 'A'; vm.snapshot = { revision: 1 }
  const loading = vm.compare(2); assert.equal(vm.snapshot, null)
  options.onUnload.call(vm); response.resolve({ data: { revision: 2 } }); await loading
  assert.equal(vm.snapshot, null)
})


test('a failed file route lookup releases the loading indicator', async () => {
  const { vm } = await component('files.vue', { listFileFolders: async () => { throw new Error('network unavailable') } })
  vm.loading = true
  await assert.rejects(vm.init(), /network unavailable/)
  assert.equal(vm.loading, false)
})
