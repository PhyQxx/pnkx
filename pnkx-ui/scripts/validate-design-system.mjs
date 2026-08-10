import { readFileSync } from 'node:fs'
import { strict as assert } from 'node:assert'

const read = path => {
  try {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')
  } catch (e) {
    return ''
  }
}

const designTokens = read('src/assets/styles/design-tokens.scss')
const modernOverrides = read('src/assets/styles/modern-overrides.scss')
const indexScss = read('src/assets/styles/index.scss')
const sidebarScss = read('src/assets/styles/sidebar.scss')
const layout = read('src/layout/index.vue')
const navbar = read('src/layout/components/Navbar.vue')
const tagsView = read('src/layout/components/TagsView/index.vue')
const login = read('src/views/login.vue')
const dashboard = read('src/views/index.vue')
const designMd = read('DESIGN.md')

const requiredTokens = [
  '--pnkx-bg: #f5f8fc',
  '--pnkx-bg-soft: #eef4fb',
  '--pnkx-surface: #ffffff',
  '--pnkx-surface-muted: #f8fafc',
  '--pnkx-border: #dfe7f0',
  '--pnkx-border-soft: #edf2f7',
  '--pnkx-text: #1f2937',
  '--pnkx-text-secondary: #667085',
  '--pnkx-primary: #2563eb',
  '--pnkx-primary-hover: #1d4ed8',
  '--pnkx-primary-soft: #dbeafe'
]

for (const token of requiredTokens) {
  assert(designTokens.includes(token), `缺少设计变量：${token}`)
}

assert(
  indexScss.includes('background: var(--pnkx-bg)') ||
    indexScss.includes('background-color: var(--pnkx-bg)'),
  '全局页面容器必须使用 --pnkx-bg'
)

assert(
  modernOverrides.includes('border-radius: var(--pnkx-radius-md)'),
  'Element Plus 覆盖样式必须使用 pnkx 8px 圆角变量'
)

assert(
  modernOverrides.includes('var(--pnkx-primary)'),
  'Element Plus 覆盖样式必须使用 --pnkx-primary'
)

assert(
  sidebarScss.includes('var(--pnkx-surface)') &&
    sidebarScss.includes('var(--pnkx-border)'),
  '侧边栏必须使用 pnkx 表面和边框变量'
)

assert(
  layout.includes('quick-action-dock') &&
    layout.includes('var(--pnkx-primary)'),
  '快捷操作入口必须改为新的 quick-action-dock 样式锚点'
)

assert(
  navbar.includes('height: 52px') &&
    navbar.includes('var(--pnkx-surface)'),
  '顶部导航必须使用 52px 轻暖工作台框架'
)

assert(
  tagsView.includes('var(--pnkx-primary-soft)') &&
    tagsView.includes('var(--pnkx-primary)'),
  '标签栏激活态必须使用 pnkx 主色变量'
)

assert(
  login.includes('login-workbench') &&
    login.includes('var(--pnkx-bg)'),
  '登录页必须使用 login-workbench 视觉锚点'
)

assert(
  dashboard.includes('workbench-dashboard') &&
    dashboard.includes('var(--pnkx-bg)'),
  '首页仪表盘必须使用 workbench-dashboard 视觉锚点'
)

assert(
  designMd.includes('轻暖生活工作台') &&
    designMd.includes('实现优先级'),
  'DESIGN.md 必须保持已确认的中文设计方向'
)

console.log('Design system validation passed')
