import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import path from 'node:path'
const root = fileURLToPath(new URL('..', import.meta.url))
const result = spawnSync(process.execPath, [path.join(root, 'node_modules/@dcloudio/vite-plugin-uni/bin/uni.js'), 'build', '-p', 'h5'], {
  cwd: root,
  env: { ...process.env, UNI_PLATFORM: 'h5', UNI_INPUT_DIR: root, UNI_OUTPUT_DIR: path.join(root, 'unpackage/dist/build/h5') },
  stdio: 'inherit'
})
if (result.error) throw result.error
process.exit(result.status ?? 1)
