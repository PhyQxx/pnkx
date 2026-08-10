/**
 * 运行nuxt打包后的文件
 * 安装pm2
 * 把nuxt打包文件.output和ecosystem.config.js放相同目录下
 * 执行pm2 start ecosystem.config.js
 * pm2 list 查看运行服务
 */
module.exports = {
  apps: [
    {
      name: 'Pei你看雪博客',
      port: '3000',
      exec_mode: 'cluster',
      instances: 'max',
      script: './.output/server/index.mjs'
    }
  ]
}
