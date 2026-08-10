module.exports = {
  plugins: ['prettier-plugin-tailwindcss'],
  singleQuote: true, // 字串使用单引号，而不是双引号
  printWidth: 100, // 每行代码长度（默认80）
  tabWidth: 2, // 每个tab相当于多少个空格（默认2
  trailingComma: 'all', // 多行使用拖尾逗号（默认node）
  arrowParens: 'avoid' // 箭头函数参数括号 默认avoid 可选 avoid | always。avoid 能省略括号的就省略 例如 x => x; always总是有括号
}
