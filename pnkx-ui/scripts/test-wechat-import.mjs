import { reactive, effect, nextTick, stop } from 'vue'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
const source = await readFile(new URL('../src/views/px/life/bookkeeping/recordBatchMixin.js', import.meta.url), 'utf8')
let response = { data: { list: [{type: '12', account: '银行', accountObject: {id: '22'}}] } }
let calls = 0
const row = { imported: true, selected: true, money: '10.00', payTime: '2026-11-01 12:00:00', source: '商户', typeDifference: '1' }
const mixin = new Function('aiBatchParse', 'importWechat', source.replace(/^import .*\n/, '').replace('export default', 'return'))(
  async () => { calls++; if (response instanceof Error) throw response; return response },
  async () => ({data: [{...row}, {...row, selected: false}]})
)
const vm = reactive({ ...mixin.data(), typeList: [{id: 1, children: [{id: 12}]}], accountList: [{id: 2, children: [{id: 22}]}], $message: {success() {}}, $nextTick: nextTick })
for (const [name, method] of Object.entries(mixin.methods)) vm[name] = method.bind(vm)
await vm.importWechatFile({target: {files: [{}], value: 'file'}})
assert.equal(calls, 1)
assert.equal(vm.batchRecordList[0].type, 12)
assert.equal(vm.batchRecordList[0].account, 22)
assert.equal(vm.batchRecordList[0].money, '10.00')
assert.equal(vm.batchRecordList[0].payTime, row.payTime)
response = {data: {list: [{type: 999, account: 999}]}}
const unknown = {...row}
await vm.recognizeWechat([unknown])
assert.equal(unknown.type, '')
assert.equal(unknown.account, '')
assert.equal(unknown.importStatus, '部分未识别，请手动补齐')
const manual = {...row, type: 12, account: 22}
await vm.recognizeWechat([manual])
assert.equal(manual.account, 22)
response = new Error('AI unavailable')
await vm.recognizeWechat([unknown])
assert.equal(unknown.importStatus, '识别失败，请手动选择')
assert.equal(vm.aiParsing, false)
console.log('PASS: automatic recognition, IDs, unknown IDs, manual edits, failure recovery')

// 第二条请求保持未完成时，第一条必须已经触发响应式视图更新。
let releaseFirst, releaseSecond
let requested = 0
const delayedMixin = new Function('aiBatchParse', 'importWechat', source.replace(/^import .*\n/, '').replace('export default', 'return'))(
  () => new Promise(resolve => { if (++requested === 1) releaseFirst = resolve; else releaseSecond = resolve }),
  async () => ({data: [{...row}, {...row}]})
)
const live = reactive({...delayedMixin.data(), typeList: vm.typeList, accountList: vm.accountList, $message: {success() {}}, $nextTick: nextTick})
for (const [name, method] of Object.entries(delayedMixin.methods)) live[name] = method.bind(live)
let displayedAccount
const stopEffect = effect(() => { displayedAccount = live.batchRecordList[0]?.account })
const importing = live.importWechatFile({target: {files: [{}], value: 'file'}})
await new Promise(resolve => setTimeout(resolve, 0))
assert.equal(live.wechatLoading, false)
releaseFirst({data: {list: [{type: 12, accountObject: {id: 22}}]}})
await new Promise(resolve => setTimeout(resolve, 0))
assert.equal(requested, 2)
assert.equal(live.aiParsing, true)
assert.equal(displayedAccount, 22, 'first row renders before second request resolves')
assert.equal(live.wechatProgress.completed, 1)
releaseSecond({data: {list: []}})
await importing
assert.equal(live.wechatProgress.completed, 2)
assert.equal(live.aiParsing, false)
stop(stopEffect)
console.log('PASS: reactive first-row update while second AI request is pending')
