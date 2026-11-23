/**
 * 流式请求工具单测：UTF-8 增量解码器跨 chunk 边界正确性
 * 运行：node tests/tools/streamRequest.test.js
 */
const fs = require('fs')
const path = require('path')
const assert = require('assert')

// streamRequest.js 含 import 语句与平台条件编译，测试环境只取纯函数部分执行
const src = fs.readFileSync(path.resolve(__dirname, '../..', 'utils/streamRequest.js'), 'utf8')
const body = src.replace(/^import .*$/gm, '').replace(/export\s+/g, '')
const { createUtf8Decoder } = new Function(`${body}\nreturn { createUtf8Decoder }`)()

function test(name, fn) {
  try {
    fn()
    console.log(`ok - ${name}`)
  } catch (error) {
    console.error(`not ok - ${name}`)
    console.error(error.message)
    process.exitCode = 1
  }
}

/**
 * 文本转精确大小的 ArrayBuffer（Buffer.from().buffer 是池化内存，比内容大，不能直接用）
 */
function toBuffer(text) {
  const bytes = Buffer.from(text, 'utf8')
  const arr = new Uint8Array(bytes.length)
  arr.set(bytes)
  return arr.buffer
}

/**
 * 按字节流模拟分片解码：bytes 按 splitAt 位置切成两片，依次 decode 后拼接
 */
function decodeSplit(text, splitAt) {
  const bytes = Array.from(Buffer.from(text, 'utf8'))
  const decoder = createUtf8Decoder()
  const part1 = decoder.decode(Uint8Array.from(bytes.slice(0, splitAt)).buffer)
  const part2 = decoder.decode(Uint8Array.from(bytes.slice(splitAt)).buffer)
  return part1 + part2
}

test('ASCII 文本单 chunk 解码', () => {
  const decoder = createUtf8Decoder()
  const out = decoder.decode(toBuffer('data:hello\n\n'))
  assert.strictEqual(out, 'data:hello\n\n')
})

test('中文完整 chunk 解码', () => {
  const decoder = createUtf8Decoder()
  const out = decoder.decode(toBuffer('今晚就吃「火锅」！'))
  assert.strictEqual(out, '今晚就吃「火锅」！')
})

test('emoji（4 字节 UTF-8）解码', () => {
  const decoder = createUtf8Decoder()
  const out = decoder.decode(toBuffer('🍜🥟✨'))
  assert.strictEqual(out, '🍜🥟✨')
})

test('中英混排 + 跨 chunk 边界全位置扫描（我爱你ABC123🍜）', () => {
  const text = '我爱你ABC123🍜'
  const bytes = Buffer.from(text, 'utf8')
  // 在每一个字节边界处切分，逐一验证跨 chunk 续接
  for (let split = 1; split < bytes.length; split++) {
    const out = decodeSplit(text, split)
    assert.strictEqual(out, text, `切分点 ${split} 处解码失败：${JSON.stringify(out)}`)
  }
})

test('中文整句每个字节边界扫描', () => {
  const text = '命运安排，今晚就吃麻辣烫'
  const bytes = Buffer.from(text, 'utf8')
  for (let split = 1; split < bytes.length; split++) {
    const out = decodeSplit(text, split)
    assert.strictEqual(out, text, `切分点 ${split} 处解码失败：${JSON.stringify(out)}`)
  }
})

test('三连片切分解码（多级续接）', () => {
  const text = '土味情话测试🎉ok'
  const bytes = Array.from(Buffer.from(text, 'utf8'))
  const decoder = createUtf8Decoder()
  const third = Math.floor(bytes.length / 3)
  const out =
    decoder.decode(Uint8Array.from(bytes.slice(0, third)).buffer) +
    decoder.decode(Uint8Array.from(bytes.slice(third, third * 2)).buffer) +
    decoder.decode(Uint8Array.from(bytes.slice(third * 2)).buffer)
  assert.strictEqual(out, text)
})

test('空 buffer 解码返回空串', () => {
  const decoder = createUtf8Decoder()
  assert.strictEqual(decoder.decode(new ArrayBuffer(0)), '')
  assert.strictEqual(decoder.decode(null), '')
})

console.log('')
