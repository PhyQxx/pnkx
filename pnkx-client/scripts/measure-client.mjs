import manifest from '../.nuxt/dist/server/client.manifest.mjs'
import { readFileSync } from 'node:fs'
import { gzipSync } from 'node:zlib'
const entries = Object.entries(manifest)
const roots = entries.filter(([key, value]) => value.isEntry || key === 'pages/index.vue' || key === 'layouts/default.vue').map(([key]) => key)
const seen = new Set()
function visit(key) {
  if (seen.has(key)) return
  seen.add(key)
  for (const dependency of manifest[key]?.imports ?? []) visit(dependency)
}
roots.forEach(visit)
const files = [...new Set([...seen].map(key => manifest[key]?.file).filter(file => file?.endsWith('.js')))]
const chunks = files.map(file => {
  const data = readFileSync(new URL(`../.output/public/_nuxt/${file}`, import.meta.url))
  return { file, bytes: data.length, gzipBytes: gzipSync(data).length }
}).sort((a, b) => b.bytes - a.bytes)
console.log(JSON.stringify({note: 'Entry + homepage + default layout static import closure; excludes prefetch and runtime requests', bytes: chunks.reduce((n,c)=>n+c.bytes,0), gzipBytes: chunks.reduce((n,c)=>n+c.gzipBytes,0), chunks}, null, 2))
