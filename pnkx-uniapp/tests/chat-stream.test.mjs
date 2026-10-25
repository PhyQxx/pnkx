import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
const src=await readFile(new URL('../utils/chatStreamParser.js',import.meta.url),'utf8');
const {createChatStreamParser}=await import(`data:text/javascript;base64,${Buffer.from(src).toString('base64')}`);
const id='12345678-1234-1234-1234-123456789012';
test('both clients use identical stream framing',async()=>assert.equal(src,await readFile(new URL('../../pnkx-ui/src/utils/chatStreamParser.js',import.meta.url),'utf8')));
test('every possible chunk split preserves text and structured plan metadata',()=>{
 const stream=`data:你好\r\ndata:第二行\r\n\r\nevent: operation-plan\ndata:{"planId":"${id}"}\n\ndata:[DONE]\n\n`;
 for(let i=0;i<=stream.length;i++){
  const texts=[],plans=[];const p=createChatStreamParser(v=>texts.push(v),v=>plans.push(v));p.push(stream.slice(0,i));p.push(stream.slice(i));p.finish();
  assert.deepEqual(texts,['你好\n第二行']);assert.deepEqual(plans,[id]);
 }
});
test('ordinary model JSON cannot become plan metadata and malformed events are ignored',()=>{
 const texts=[],plans=[];const p=createChatStreamParser(v=>texts.push(v),v=>plans.push(v));
 p.push(`data:{"planId":"${id}"}\n\nevent: operation-plan\ndata:bad\n\nevent: operation-plan\ndata:{"planId":"../../other"}\n\n`);p.finish();
 assert.equal(texts.length,1);assert.deepEqual(plans,[]);
});
test('partial final text and separate streams remain independent',()=>{
 const a=[],b=[];const one=createChatStreamParser(v=>a.push(v),()=>{}),two=createChatStreamParser(v=>b.push(v),()=>{});
 one.push('da');two.push('data:B\n\n');one.push('ta:A');one.finish();two.finish();assert.deepEqual(a,['A']);assert.deepEqual(b,['B']);
});
