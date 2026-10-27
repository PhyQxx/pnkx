import test from 'node:test';import assert from 'node:assert/strict';import {readFile} from 'node:fs/promises';
const source=await readFile(new URL('../utils/shoppingGroups.js',import.meta.url),'utf8');const m=await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`);
test('both clients share one shopping grouping contract',async()=>assert.equal(source,await readFile(new URL('../../pnkx-ui/src/utils/shoppingGroups.js',import.meta.url),'utf8')));
test('items are grouped by fulfillment state with settled taking precedence',()=>{const groups=m.groupItems([
  {id:1,name:'鸡蛋',fulfillmentState:'needed'},
  {id:2,name:'牛奶',fulfillmentState:'available'},
  {id:3,name:'面包',fulfillmentState:'purchased'},
  {id:4,name:'已结算的面包',fulfillmentState:'purchased',batchId:9},
  {id:5,name:'旧勾选数据',checked:true},
  {id:6,name:'旧未勾选数据'}
]);assert.deepEqual(groups.needed.map(i=>i.id),[1,6]);assert.deepEqual(groups.available.map(i=>i.id),[2]);assert.deepEqual(groups.purchased.map(i=>i.id),[3,5]);assert.deepEqual(groups.settled.map(i=>i.id),[4]);assert.equal(m.groupMeta.settled.label,'已结算');assert.deepEqual(m.groupItems(null).needed,[])});
test('page preselection opens the checkout with only still-eligible items',async()=>{const composableSource=await readFile(new URL('../utils/useShoppingCheckout.js',import.meta.url),'utf8');assert.equal(composableSource,await readFile(new URL('../../pnkx-ui/src/utils/useShoppingCheckout.js',import.meta.url),'utf8'));
// 项目 .js 按 CJS 加载,搭建临时 ESM 目录(含 node_modules 链接)加载组合式函数
const{mkdirSync,writeFileSync,symlinkSync,rmSync,realpathSync}=await import('node:fs');
const tmp=new URL('./.tmp-esm/',import.meta.url);
rmSync(tmp,{recursive:true,force:true});mkdirSync(tmp);
try{
writeFileSync(new URL('package.json',tmp),JSON.stringify({type:'module'}));
writeFileSync(new URL('checkoutModel.mjs',tmp),await readFile(new URL('../utils/checkoutModel.js',import.meta.url),'utf8'));
writeFileSync(new URL('useShoppingCheckout.mjs',tmp),composableSource.replace("'./checkoutModel'","'./checkoutModel.mjs'"));
mkdirSync(new URL('node_modules',tmp));
symlinkSync(realpathSync(new URL('../node_modules/vue',import.meta.url)),new URL('node_modules/vue',tmp).pathname);
const{useShoppingCheckout}=await import(new URL('useShoppingCheckout.mjs',tmp).href);
const items=[
  {id:11,name:'苹果',quantity:'3个',fulfillmentState:'purchased'},
  {id:12,name:'香蕉',quantity:'2根',fulfillmentState:'purchased'},
  {id:13,name:'橙子',quantity:'1个',fulfillmentState:'needed'},
  {id:14,name:'已结算梨',quantity:'1个',fulfillmentState:'purchased',batchId:7}
];
const api={request:async()=>({data:[]}),listAccount:async()=>({rows:[]}),listClassification:async()=>({rows:[]})};
const state=useShoppingCheckout({items,listId:1},{},api);
state.open(null,[12,13,14,999]);
assert.deepEqual(state.form.value.purchasedItemIds,[12]);
state.open(null,[]);
assert.equal(state.form.value.purchasedItemIds.length,2);
state.open(null,[11,12]);
assert.deepEqual(state.form.value.purchasedItemIds,[11,12]);
}finally{rmSync(tmp,{recursive:true,force:true})}});
