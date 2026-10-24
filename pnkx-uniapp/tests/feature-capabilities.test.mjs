import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
const read = p => readFileSync(new URL(p, import.meta.url), 'utf8')
const source = read('../utils/featureCapabilities.js')
function loader(request, getToken) {
  return new Function('request', 'getToken', source.replace(/^import .*$/gm, '').replace('export async', 'async') + '\nreturn getFeatureCapabilities')(request, getToken)
}
test('both clients use the same capability isolation policy', () => {
  assert.equal(source, read('../../pnkx-ui/src/utils/featureCapabilities.js'))
})
test('simultaneous mounts share one request and use the same snapshot', async () => {
  let count = 0
  const load = loader(async () => { count++; return {data:{chargeInstanceV2:true}} }, () => 'a')
  const values = await Promise.all([load(),load(),load()])
  assert.equal(count,1)
  assert.equal(values[0].chargeInstanceV2,true)
  await load(); assert.equal(count,1)
})
test('late response from previous account cannot enable the next account', async () => {
  let token = 'a', finish
  const load = loader(() => token === 'a' ? new Promise(r => {finish=r}) : Promise.resolve({data:{chargeInstanceV2:false}}), () => token)
  const previous = load()
  token = 'b'
  assert.equal((await load()).chargeInstanceV2,false)
  finish({data:{chargeInstanceV2:true}})
  assert.deepEqual(await previous,{})
  assert.equal((await load()).chargeInstanceV2,false)
  token = ''; assert.deepEqual(await load(),{})
})
test('network failure does not poison future capability retries', async () => {
  let calls=0
  const load=loader(async()=>{if(++calls===1)throw Error('offline');return {data:{}}},()=> 'a')
  await assert.rejects(load())
  assert.deepEqual(await load(),{})
  assert.equal(calls,2)
})
