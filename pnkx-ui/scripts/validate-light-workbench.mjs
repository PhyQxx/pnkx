import { readFileSync } from 'node:fs'
import { strict as assert } from 'node:assert'

const read = path => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

const designTokens = read('src/assets/styles/design-tokens.scss')
const indexScss = read('src/assets/styles/index.scss')
const modernOverrides = read('src/assets/styles/modern-overrides.scss')
const sidebarScss = read('src/assets/styles/sidebar.scss')
const layout = read('src/layout/index.vue')
const navbar = read('src/layout/components/Navbar.vue')
const logo = read('src/layout/components/Sidebar/Logo.vue')
const tagsView = read('src/layout/components/TagsView/index.vue')
const appMain = read('src/layout/components/AppMain.vue')
const login = read('src/views/login.vue')
const dashboard = read('src/views/index.vue')

const requiredTokens = [
  '--pnkx-bg: #fbfdff',
  '--pnkx-bg-soft: #f7faff',
  '--pnkx-surface: #ffffff',
  '--pnkx-surface-muted: #fafcff',
  '--pnkx-border: #e8eef6',
  '--pnkx-border-soft: #f0f4f9',
  '--pnkx-text: #253041',
  '--pnkx-text-secondary: #708095',
  '--pnkx-text-muted: #a9b5c4',
  '--pnkx-primary: #5b8def',
  '--pnkx-primary-soft: #eef5ff',
  '--pnkx-success-soft: #effaf5',
  '--pnkx-warning-soft: #fff8ed',
  '--pnkx-info-soft: #eefaff',
  '--pnkx-danger-soft: #fff2f3'
]

for (const token of requiredTokens) {
  assert(designTokens.includes(token), `Missing light workbench token: ${token}`)
}

const requiredGlobalAnchors = [
  '.filter-panel',
  '.table-page',
  '.page-toolbar',
  '.batch-toolbar',
  '.p-card'
]

for (const anchor of requiredGlobalAnchors) {
  assert(indexScss.includes(anchor), `Global app styles must include ${anchor}`)
}

assert(indexScss.includes('background: var(--pnkx-bg)'), 'Global app background must use --pnkx-bg')
assert(!indexScss.includes('linear-gradient(135deg, #667eea 0%, #764ba2 100%)'), 'Global app styles must not use the old purple-blue gradient')
assert(modernOverrides.includes('.el-button--primary'), 'Primary button overrides must target .el-button--primary')
assert(modernOverrides.includes('background: var(--pnkx-primary);'), 'Primary button must use solid --pnkx-primary')
assert(!modernOverrides.includes('linear-gradient(135deg, var(--color-primary-500)'), 'Primary buttons must not use the old heavy blue gradient')
assert(!modernOverrides.includes('linear-gradient(135deg, #667eea 0%, #764ba2 100%)'), 'Primary buttons must not use the old purple-blue gradient')
assert(sidebarScss.includes('var(--pnkx-surface)') && sidebarScss.includes('var(--pnkx-primary-soft)'), 'Sidebar must use light workbench surfaces')
assert(!sidebarScss.includes('linear-gradient(180deg, #1f2d3d 0%, #304156 100%)'), 'Sidebar must not use the old heavy dark gradient')
assert(layout.includes('quick-action-dock'), 'Layout must expose the quick-action-dock anchor')
assert(navbar.includes('height: 52px') && navbar.includes('var(--pnkx-surface)'), 'Navbar must be the 52px light command bar')
assert(navbar.includes('navbar-action'), 'Navbar must expose navbar-action controls')
assert(navbar.includes('el-breadcrumb__inner'), 'Navbar breadcrumb must expose el-breadcrumb__inner styles')
assert(logo.includes('var(--pnkx-primary-soft)'), 'Sidebar logo must use the light primary soft surface')
assert(tagsView.includes('var(--pnkx-primary-soft)') && !tagsView.includes('activeStyle(tag)'), 'TagsView active styles must use light tokens, not inline theme color')
assert(appMain.includes('min-height: calc(100vh - 86px)'), 'AppMain must account for 52px navbar and 34px tags view')
assert(login.includes('login-workbench'), 'Login page must use the light workbench anchor')
assert(login.includes('login-panel'), 'Login page must expose the login-panel anchor')
assert(login.includes('visual-card'), 'Login page must expose the visual-card anchor')
assert(!login.includes('decoration-circle'), 'Login page must remove decorative circles')
assert(dashboard.includes('workbench-dashboard'), 'Dashboard must use the workbench-dashboard anchor')
assert(dashboard.includes('quick-stats'), 'Dashboard must expose quick-stats')
assert(dashboard.includes('urgent-list'), 'Dashboard must expose urgent-list')
assert(dashboard.includes('data-governance'), 'Dashboard must expose data-governance')
assert(dashboard.includes('var(--pnkx-bg)'), 'Dashboard must use --pnkx-bg')

console.log('Light workbench validation passed')
