import fs from 'node:fs'
import path from 'node:path'

const root = path.join(process.cwd(), 'src')
const issues = []

function walk(dir, files = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      walk(fullPath, files)
    } else if (entry.isFile() && fullPath.endsWith('.vue')) {
      files.push(fullPath)
    }
  }
  return files
}

for (const file of walk(root)) {
  const content = fs.readFileSync(file, 'utf8')
  const tags = content.match(/<pagination[\s\S]*?(?:\/>|<\/pagination>)/gi) || []

  for (const tag of tags) {
    if (/v-modelv-model:/.test(tag)) {
      issues.push(`${path.relative(process.cwd(), file)} -> malformed v-model attribute: ${tag.replace(/\s+/g, ' ').trim()}`)
    }

    const hasModelPage = /v-model:page\s*=/.test(tag)
    const hasModelLimit = /v-model:limit\s*=/.test(tag)
    const hasPageProp = /:page\s*=/.test(tag)
    const hasLimitProp = /:limit\s*=/.test(tag)

    if ((hasPageProp || hasLimitProp) && !(hasModelPage && hasModelLimit)) {
      issues.push(`${path.relative(process.cwd(), file)} -> ${tag.replace(/\s+/g, ' ').trim()}`)
    }
  }
}

const paginationComponent = fs.readFileSync(path.join(root, 'components', 'Pagination', 'index.vue'), 'utf8')
if (!/\$emit\(['"]update:page['"]/.test(paginationComponent)) {
  issues.push('src/components/Pagination/index.vue missing update:page emit')
}
if (!/\$emit\(['"]update:limit['"]/.test(paginationComponent)) {
  issues.push('src/components/Pagination/index.vue missing update:limit emit')
}

if (issues.length) {
  console.error(`Pagination sync check failed: ${issues.length} issue(s)`)
  for (const issue of issues) {
    console.error(`- ${issue}`)
  }
  process.exit(1)
}

console.log('Pagination sync check passed')
