import test from 'node:test';import assert from 'node:assert/strict';import {readFile} from 'node:fs/promises';
const source=await readFile(new URL('../utils/ruleChangeFlow.js',import.meta.url),'utf8');const m=await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`);
test('both clients share one rule change flow contract',async()=>assert.equal(source,await readFile(new URL('../../pnkx-ui/src/utils/ruleChangeFlow.js',import.meta.url),'utf8')));
test('field diffs render with labels and readable values',()=>{const diffs=m.formatDiffs([
  {field:'name',before:'视频会员',after:'视频会员Pro'},
  {field:'postingMode',before:'manual_confirm',after:'auto_record'},
  {field:'amount',before:30,after:35},
  {field:'trialEndDate',before:null,after:'2026-12-01'}
]);assert.equal(diffs[0].label,'名称');assert.equal(diffs[0].after,'视频会员Pro');assert.equal(diffs[1].before,'到期确认');assert.equal(diffs[1].after,'自动记账');assert.equal(diffs[2].label,'金额');assert.equal(diffs[3].before,'未设置');assert.deepEqual(m.formatDiffs([]),[]);assert.equal(m.formatDiffs(undefined).length,0)});
test('presenter registration drives the flow and revokes safely',async()=>{
  assert.equal(await m.presentRuleFlow({}),null);
  let seen=null;
  const revoke=m.setRuleFlowPresenter(async(task)=>{seen=task;return{status:'committed',result:{ok:1}}});
  const out=await m.presentRuleFlow({kind:'change'});
  assert.deepEqual(seen,{kind:'change'});assert.deepEqual(out,{status:'committed',result:{ok:1}});
  // 后注册者覆盖,先注销者不能误清后注册者
  const revoke2=m.setRuleFlowPresenter(async()=>({status:'cancelled'}));
  revoke();
  assert.equal((await m.presentRuleFlow({})).status,'cancelled');
  revoke2();
  assert.equal(await m.presentRuleFlow({}),null);
});
