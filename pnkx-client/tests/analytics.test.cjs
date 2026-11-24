const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), vm = require('node:vm'), ts = require('typescript'), crypto = require('node:crypto')
function compile(file) { return ts.transpileModule(fs.readFileSync(require('node:path').resolve(__dirname, '..', file), 'utf8').replace(/import\.meta\.hot/g, 'false'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText }
const utils = {}
vm.runInNewContext(compile('src/utils/analytics.ts'), { exports: utils })
test('route aggregation removes sensitive queries and unknown identifiers', () => {
 assert.equal(utils.analyticsPage('/post/123?token=secret').contentId, '123')
 assert.equal(utils.analyticsPage('/post/private-email').pageKey, 'other')
 assert.equal(utils.analyticsPage('/type/').pageKey, 'category')
})
function harness() {
 const hooks={},events={},timers=[],requests=[],route={value:{path:'/'}},storage=new Map(),exports={}
 const store={getItem:k=>storage.get(k),setItem:(k,v)=>storage.set(k,v)}
 let after,now=1000000000000,fail=false
 const document={hidden:false,referrer:'https://example.com/private?secret=yes',addEventListener:(n,f)=>{events[n]=f}}
 const window={innerHeight:500,localStorage:store,sessionStorage:store,addEventListener:(n,f)=>{events[n]=f}}
 vm.runInNewContext(compile('src/plugins/analytics.client.ts'),{exports,require:n=>n.includes('apiBase')?{API_BASE_URL:'/api'}:utils,
 defineNuxtPlugin:f=>f,useRuntimeConfig:()=>({public:{analyticsEnabled:true}}),
 useRouter:()=>({currentRoute:route,afterEach:f=>{after=f;return()=>{}}}),useToken:()=>({getToken:()=>null,tokenPrefix:'Bearer '}),
 crypto,URL,Date:{now:()=>now},window,document,setInterval:(fn,ms)=>{timers.push({fn,ms});return 1},clearInterval(){},
 fetch:async(_url,opts)=>{requests.push(JSON.parse(opts.body));if(fail)throw Error('offline');return{ok:true,json:async()=>({code:200})}}
 })
 const p=exports.default({hook:(n,f)=>{hooks[n]=f}})
 return{hooks,timers,requests,document,track:p.provide.analytics,advance(ms){now+=ms},offline(v){fail=v},go(p){route.value.path=p;after(null,null,null)}}
}
const drain=()=>new Promise(r=>setImmediate(r))
test('mount and navigation do not double count',async()=>{
 const h=harness();h.hooks['app:mounted']();await drain();h.go('/');h.go('/post/1');await drain()
 assert.equal(h.requests.flat().filter(e=>e.eventName==='page_view').length,2)
 assert.equal(h.requests[0][0].referrerHost,'example.com')
})
test('failed transmission retries identical IDs',async()=>{
 const h=harness();h.offline(true);h.hooks['app:mounted']();await drain();h.offline(false)
 h.timers.find(t=>t.ms===15000).fn();await drain();assert.equal(h.requests[0][0].eventId,h.requests[1][0].eventId)
})
test('hidden time excluded and async outcome retains original page',async()=>{
 const h=harness();h.hooks['app:mounted']();await drain();const operationId=crypto.randomUUID()
 h.track('ai_submit',{operationId});h.go('/post/2');await drain();h.track('ai_success',{operationId})
 h.document.hidden=true;h.advance(5000);h.timers.find(t=>t.ms===5000).fn();h.timers.find(t=>t.ms===15000).fn();await drain()
 assert.equal(h.requests.flat().find(e=>e.eventName==='ai_success').pageKey,'home')
 assert.equal(h.requests.flat().filter(e=>e.eventName==='page_duration').length,0)
})
test('reading thresholds handle short articles, hidden geometry and partial depth', () => {
 assert.deepEqual(Array.from(utils.readingThresholds(800, 1000, 700)), [])
 assert.deepEqual(Array.from(utils.readingThresholds(0, 0, 700)), [])
 assert.deepEqual(Array.from(utils.readingThresholds(0, 1000, 500)), [25,50])
 assert.deepEqual(Array.from(utils.readingThresholds(-500, 1000, 500)), [25,50,75,100])
})
test('playback position ignores unknown and infinite durations', () => {
 assert.deepEqual(Array.from(utils.playbackThresholds(10, Infinity)), [])
 assert.deepEqual(Array.from(utils.playbackThresholds(10, 0)), [])
 assert.deepEqual(Array.from(utils.playbackThresholds(50, 100)), [25,50])
})
test('article thresholds emit once per visit and reset on navigation',async()=>{
 const h=harness();h.document.querySelector=()=>({getBoundingClientRect:()=>({top:-500,height:1000})})
 // Harness viewport is supplied separately so short/long articles use real viewport geometry.
 h.hooks['app:mounted']();await drain();h.go('/post/1');await drain()
 h.document.hidden=true;h.timers.find(t=>t.ms===1000).fn()
 h.timers.find(t=>t.ms===15000).fn();await drain()
 assert.equal(h.requests.flat().filter(e=>e.eventName.startsWith('read_')).length,0)
 h.document.hidden=false;h.timers.find(t=>t.ms===1000).fn();h.timers.find(t=>t.ms===1000).fn()
 h.timers.find(t=>t.ms===15000).fn();await drain()
 assert.equal(h.requests.flat().filter(e=>e.eventName.startsWith('read_')).length,4)
 h.go('/post/2');await drain();h.timers.find(t=>t.ms===1000).fn();h.timers.find(t=>t.ms===15000).fn();await drain()
 assert.equal(h.requests.flat().filter(e=>e.eventName.startsWith('read_')).length,8)
})
test('video target ID is preserved without changing page identity',async()=>{
 const h=harness();h.hooks['app:mounted']();await drain();h.go('/video');await drain()
 h.track('video_start',{operationId:crypto.randomUUID(),targetId:'123'})
 h.timers.find(t=>t.ms===15000).fn();await drain()
 const e=h.requests.flat().find(e=>e.eventName==='video_start')
 assert.equal(e.targetId,'123');assert.equal(e.pageKey,'video');assert.equal(e.contentId,null)
})
test('login and reply results stay linked to their initiating operation',async()=>{
 for (const feature of ['login','reply']) {
  const h=harness();h.hooks['app:mounted']();await drain()
  const operationId=crypto.randomUUID()
  h.track(`${feature}_submit`,{operationId,targetId:feature==='reply'?'42':undefined})
  h.go('/user');await drain()
  h.track(`${feature}_success`,{operationId,targetId:feature==='reply'?'42':undefined})
  h.timers.find(t=>t.ms===15000).fn();await drain()
  const outcome=h.requests.flat().find(e=>e.eventName===`${feature}_success`)
  assert.equal(outcome.pageKey,'home');assert.equal(outcome.operationId,operationId)
  assert.equal('password' in outcome,false);assert.equal('content' in outcome,false)
 }
})
test('activation before app mounted gets page context without duplicate PV',async()=>{
 const h=harness(), operationId=crypto.randomUUID()
 h.track('activation_submit',{operationId});await drain()
 h.hooks['app:mounted']();h.track('activation_success',{operationId})
 h.timers.find(t=>t.ms===15000).fn();await drain()
 const all=h.requests.flat()
 assert.equal(all.filter(e=>e.eventName==='page_view').length,1)
 assert.equal(all.find(e=>e.eventName==='activation_submit').pageViewId,all.find(e=>e.eventName==='activation_success').pageViewId)
})
