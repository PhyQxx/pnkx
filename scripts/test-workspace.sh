#!/usr/bin/env bash
# Local verification only; no application startup, production DB or paid AI calls.
set -euo pipefail
repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
with_mysql=false
if [[ "${1:-}" == "--mysql" && $# == 1 ]]; then
  with_mysql=true
elif [[ $# != 0 ]]; then
  echo "Usage: bash scripts/test-workspace.sh [--mysql]" >&2
  exit 2
fi
cd "$repo_dir"
for command in java mvn node npm sqlite3; do
  command -v "$command" >/dev/null || { echo "Missing prerequisite: $command" >&2; exit 1; }
done
if [[ ! -d pnkx-ui/node_modules ]]; then
  echo "Install pnkx-ui dependencies before running validation." >&2
  exit 1
fi
if [[ "$with_mysql" == true ]]; then
  bash scripts/test-note-isolation.sh
fi
mvn -f pnkx-admin/pom.xml -pl pnkx-admin -am test
node --test pnkx-ui/tests/*.test.mjs pnkx-uniapp/tests/note-offline.test.mjs pnkx-uniapp/tests/offline-sync-contract.test.mjs
node <<'JS'
const fs = require('fs'), path = require('path')
const { parse, compileTemplate } = require('./pnkx-ui/node_modules/@vue/compiler-sfc')
const babel = require('./pnkx-ui/node_modules/@babel/parser')
const dirs = ['pnkx-uniapp/pages_life/workspace', 'pnkx-uniapp/components/resource-links', 'pnkx-uniapp/components/workspace-summary']
const files = dirs.flatMap(dir => fs.readdirSync(dir).filter(file => file.endsWith('.vue')).map(file => path.join(dir, file)))
files.push('pnkx-uniapp/pages_life/note/detail.vue', 'pnkx-uniapp/pages_system/mine/setting/sync.vue', 'pnkx-uniapp/pages_life/search/index.vue', 'pnkx-uniapp/pages_life/todo/edit.vue')
for (const filename of files) {
  const { descriptor, errors } = parse(fs.readFileSync(filename, 'utf8'), { filename })
  if (errors.length) throw errors[0]
  if (descriptor.script) babel.parse(descriptor.script.content, { sourceType: 'module' })
  if (descriptor.template) {
    const result = compileTemplate({ source: descriptor.template.content, filename, id: filename,
      compilerOptions: { isCustomElement: tag => tag.startsWith('uni-') || ['view', 'text', 'scroll-view', 'picker', 'switch'].includes(tag) } })
    if (result.errors.length) throw result.errors[0]
  }
}
console.log(`uniapp scripts/templates validated: ${files.length}; real-device testing still required.`)
JS
(
  cd pnkx-ui
  npx --no-install eslint --ext .vue,.js src/views/px/workspace src/components/ResourceLinks src/components/WorkspaceSummary src/api/workspace.js src/utils/noteSession.js
  npm run build
)
git diff --check
