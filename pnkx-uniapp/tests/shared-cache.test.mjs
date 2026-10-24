import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { spawnSync } from 'node:child_process'
import test from 'node:test'
const source=readFileSync(new URL('../utils/sharedCachePolicy.js',import.meta.url),'utf8')
const {purgeSharedCache}=await import('data:text/javascript;base64,'+Buffer.from(source).toString('base64'))
test('SQLite cleanup deletes foreign synced cache but preserves personal data and unsynced changes',async()=>{
 const sql=[]
 await purgeSharedCache({_escapeValue:v=>"'"+v.replaceAll("'","''")+"'",executeSql:async s=>sql.push(s)},'7')
 const run=spawnSync('python3',['-c',`
import sqlite3,json,sys
c=sqlite3.connect(':memory:')
for table in ['px_todo','px_shopping_list','px_shopping_item','px_meal_plan','px_commemoration_day']:
 c.execute('create table '+table+'(id integer,create_by text,_sync_status integer)')
 c.executemany('insert into '+table+' values(?,?,?)',[(1,'7',1),(2,'8',1),(3,'8',0),(4,None,0),(5,None,1),(6,'7',0)])
for sql in json.load(sys.stdin):c.execute(sql)
print(json.dumps([r[0] for r in c.execute('select id from px_todo order by id')]))
`],{input:JSON.stringify(sql),encoding:'utf8'})
 assert.equal(run.status,0,run.stderr)
 assert.deepEqual(JSON.parse(run.stdout),[1,3,4,6])
 assert.equal(sql.length,5)
})
test('missing owner never triggers broad deletion',async()=>{
 await purgeSharedCache({executeSql:()=>assert.fail('unexpected deletion')},null)
})
