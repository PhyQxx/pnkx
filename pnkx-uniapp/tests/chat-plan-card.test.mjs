import test from 'node:test';import assert from 'node:assert/strict';import {readFile} from 'node:fs/promises';
async function component(path,request){const source=await readFile(new URL(path,import.meta.url),'utf8');const script=source.match(/<script>([\s\S]*?)<\/script>/)[1].replace(/import[\s\S]*?;/g,'').replace('export default','return');const options=new Function('request','PlanDraftEditor','planLabels','toolLabels','planFields','planHint',script)(request,{}, {},{},()=>[],()=> '');const vm={...options.data(),disabled:false};for(const [key,fn] of Object.entries(options.methods))vm[key]=fn.bind(vm);Object.defineProperty(vm,'locked',{get:()=>options.computed.locked.call(vm)});return {vm,options};}
for(const path of ['../components/ChatPlanCard.vue','../../pnkx-ui/src/components/ChatPlanCard.vue']) {
 test(`${path}: edits bind execution to replacement and prevent duplicate click`,async()=>{
  const calls=[];let resolve;const {vm}=await component(path,args=>{calls.push(args);return new Promise(r=>resolve=r)});
  vm.revised({id:'new',status:'awaiting_confirmation'});const first=vm.run('confirm');await vm.run('confirm');assert.equal(calls.length,1);assert.equal(calls[0].url,'/operation-plans/new/confirm');resolve({data:{id:'new',status:'succeeded'}});await first;assert.equal(vm.plan.status,'succeeded');
 });
 test(`${path}: failure retains reviewed plan and editing blocks execution`,async()=>{
  const {vm}=await component(path,async()=>{throw new Error('offline')});vm.revised({id:'p',status:'awaiting_confirmation'});vm.editing=true;await vm.run('confirm');assert.equal(vm.error,'');vm.editing=false;await vm.run('confirm');assert.equal(vm.plan.id,'p');assert.equal(vm.error,'offline');assert.equal(vm.busy,false);
 });
}
