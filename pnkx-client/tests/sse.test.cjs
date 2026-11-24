const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const output = ts.transpileModule(fs.readFileSync('src/utils/sse.ts', 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS }
}).outputText
const compiled = { exports: {} }
new Function('exports', output)(compiled.exports)
const { createSseParser } = compiled.exports

for (const newline of ['\n', '\r\n', '\r']) {
  test(`SSE survives every chunk boundary (${JSON.stringify(newline)})`, () => {
    const input = [': heartbeat', 'data: 你好', 'data: world', '', 'data: [DONE]', '', ''].join(newline)
    for (let split = 0; split <= input.length; split++) {
      const result = []
      const parse = createSseParser(data => result.push(data))
      parse(input.slice(0, split))
      parse(input.slice(split))
      assert.deepEqual(result, ['你好\nworld', '[DONE]'])
    }
  })
}
test('preserves content spaces and ignores incomplete events', () => {
  const result = []
  const parse = createSseParser(data => result.push(data))
  for (const char of 'event: message\ndata:  leading\ndata\n\ndata: incomplete') parse(char)
  assert.deepEqual(result, [' leading\n'])
})
