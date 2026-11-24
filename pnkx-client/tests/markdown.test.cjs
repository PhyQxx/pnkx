const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const compiled = { exports: {} }
new Function('exports', 'require', 'defineNuxtPlugin', ts.transpileModule(fs.readFileSync('src/plugins/markdown.ts', 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, esModuleInterop: true, target: ts.ScriptTarget.ES2020 }
}).outputText)(compiled.exports, require, fn => fn())
const render = compiled.exports.default.provide

test('reused parsers do not leak rendering state between calls', () => {
  const first = render.markdownIt('**hello**\n\n::: center\nworld\n:::')
  render.markdownIt('# another article\n\n- item')
  assert.equal(render.markdownIt('**hello**\n\n::: center\nworld\n:::'), first)
  assert.match(first, /<strong>hello<\/strong>/)
  assert.match(first, /text-align:center/)
})
test('all HTML output paths retain sanitization', () => {
  const malicious = '<img src=x onerror="alert(1)"><script>alert(1)</script>'
  for (const name of ['markdownIt', 'markdownItSearch', 'sanitizeHtml']) {
    assert.doesNotMatch(render[name](malicious), /onerror|<script/i)
  }
})
test('excerpt parser remains separate from search and article configuration', () => {
  assert.match(render.markdownItSearch('**hello**'), /<strong>hello<\/strong>/)
  assert.equal(render.markdownItContent('**hello**'), 'hello')
  assert.match(render.markdownIt('```js\nconst x = 1\n```'), /copy-btn/)
})
