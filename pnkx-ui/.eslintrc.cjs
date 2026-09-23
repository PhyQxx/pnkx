module.exports = {
  root: true,
  env: {
    browser: true,
    es2021: true,
    node: true
  },
  // 规则分级原则：
  //   error = 真实缺陷/安全风险，新代码必须避免
  //   warn  = 存量迁移债（Vue2→3 遗留等），随日常迭代逐步消化，不阻塞
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-essential'
  ],
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module'
  },
  globals: {
    // vite 构建产物中由 CDN/其他脚本注入的全局
    $: 'readonly',
    window: 'readonly'
  },
  rules: {
    // ==== 错误级（真实缺陷/安全风险）====
    'no-undef': 'error',
    'no-dupe-keys': 'error',
    'no-eval': 'error',
    'no-implied-eval': 'error',
    'vue/no-side-effects-in-computed-properties': 'error',
    // ==== 警告级（存量迁移债，渐进消化）====
    // RuoYi 系页面组件命名惯例为单词（views/xxx/index.vue），不强制 multi-word
    'vue/multi-word-component-names': 'off',
    // Vue2→3 迁移遗留（v-on:native / 旧 slot 语法 / props 直接改写）
    'vue/no-deprecated-v-on-native-modifier': 'warn',
    'vue/no-deprecated-slot-attribute': 'warn',
    // TagsView 使用 router-link 的 tag prop（vue-router 4 下被忽略、降级渲染为 a，功能不受损）
    // 重构涉及核心布局，随 Vue2→3 迁移债一并处理，暂列 warn
    'vue/no-deprecated-router-link-tag-prop': 'warn',
    'vue/no-mutating-props': 'warn',
    'vue/no-reserved-component-names': 'warn',
    'vue/require-valid-default-prop': 'warn',
    'no-unused-vars': ['warn', { args: 'none' }],
    'no-useless-escape': 'warn',
    'no-extra-semi': 'warn',
    'no-constant-condition': 'warn',
    'no-console': 'warn',
    'no-debugger': 'warn',
    'vue/no-unused-components': 'warn',
    'vue/no-unused-vars': 'warn',
    'vue/require-v-for-key': 'warn',
    'vue/no-use-v-if-with-v-for': 'warn'
  }
}
