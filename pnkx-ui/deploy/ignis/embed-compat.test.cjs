const { test } = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const source = fs.readFileSync(__dirname + '/embed-compat.js', 'utf8');
test('embedded renderer never reads the cross-origin parent require', () => {
  const foreign = new Proxy({}, { get() { throw new Error('cross-origin access'); } });
  const window = {};
  Object.defineProperty(window, 'parent', { configurable: true, get: () => foreign });
  window.require = () => 'local electron shim';
  vm.runInNewContext(source, { window, console });
  // Match Obsidian 1.12.7 startup traversal.
  let renderer = window;
  while (renderer !== renderer.parent && renderer.parent !== null) renderer = renderer.parent;
  assert.equal(renderer.require(), 'local electron shim');
});
test('standalone window remains unchanged', () => {
  const window = {}; window.parent = window;
  const before = Object.getOwnPropertyDescriptor(window, 'parent');
  vm.runInNewContext(source, { window, console });
  assert.deepEqual(Object.getOwnPropertyDescriptor(window, 'parent'), before);
});
test('unsupported window reports failure without throwing', () => {
  const window = {};
  Object.defineProperty(window, 'parent', { value: {}, configurable: false });
  const messages = [];
  vm.runInNewContext(source, { window, console: { error: m => messages.push(m) } });
  assert.equal(messages.length, 1);
});
