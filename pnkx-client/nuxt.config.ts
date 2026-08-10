// https://nuxt.com/docs/api/configuration/nuxt-config

export default defineNuxtConfig({
  srcDir: 'src',
  app: {
    head: {
      charset: 'utf-8',
      viewport:
        'width=device-width,initial-scale=1,minimum-scale=1,maximum-scale=1,user-scalable=no,viewport-fit=cover',
      title: 'Pei你看雪',
      meta: [
        { name: 'keywords', content: 'Pei你看雪,博客,前端,后端,Vue,SpringBoot' },
        { name: 'description', content: 'PNKX，Pei你看雪情侣博客' }
      ],
      link: [
        {
          rel: 'stylesheet',
          href: 'https://ftp.pnkx.top:8/ftp/pnkx/static/css/pnkx.min.css'
        }
      ],
      script: [
        // 星空背景
        {
          src: 'https://ftp.pnkx.top:8/ftp/pnkx/static/js/starrySky.min.js',
          defer: true
        },
        // 聊天机器人
        {
          src: 'https://ai.pnkx.top:8/api/application/embed?protocol=https&host=ai.pnkx.top:8&token=4d93230497f38b58'
        }
      ]
    }
  },
  build: {
    // naive-ui 依赖 vueuc（CommonJS 包），现代 Nuxt 3 的 SSR 开发服务器基于 vite-node，
    // 必须在 dev 与 prod 都进行转译，否则 SSR 时 Node 原生 ESM 加载器会报
    // "Named export 'VResizeObserver' not found"。
    transpile: [
      'naive-ui',
      'vueuc',
      '@css-render/vue3-ssr',
      '@juggle/resize-observer',
      'markdown-it',
      'clipboard'
    ]
  },
  vite: {
    // build: {
    //   chunkSizeWarningLimit: 1500,
    //   rollupOptions: {
    //     output: {
    //       manualChunks(id) {
    //         if (id.includes('node_modules')) {
    //           return id.toString().split('node_modules/')[1].split('/')[0].toString()
    //         }
    //       }
    //     }
    //   }
    // },
    optimizeDeps: {
      // 这些包的 main 指向 UMD 产物且没有 exports 字段，浏览器端拿不到全局 Vue
      // 会导致 `e.Vue` 为 undefined，页面水合时崩溃白屏。
      // 通过预打包让 esbuild 解析 module 字段并将 vue 外置，得到纯净的 ESM。
      include:
        process.env.NODE_ENV === 'development'
          ? [
              'naive-ui',
              'vueuc',
              'vue3-danmaku',
              'vue-waterfall-plugin-next',
              'markdown-it/dist/markdown-it.js',
              'markdown-it-container',
              'markdown-it-for-inline',
              'highlight.js/lib/common'
            ]
          : []
    }
  },
  css: ['@/assets/css/animation.css', '@/assets/css/component.css', '@/assets/css/cyanosis.css'],
  // 代理
  nitro: {
    devProxy: {
      '/prod-api': {
        target: `https://pnkx.top/prod-api`,
        changeOrigin: true
      },
    }
  },
  modules: [
    '@nuxtjs/tailwindcss',
    '@pinia/nuxt',
    '@pinia-plugin-persistedstate/nuxt',
    '@vueuse/nuxt',
    '@nuxtjs/device',
    'nuxt-icon',
    '@nuxtjs/color-mode'
  ],
  pinia: {
    autoImports: ['defineStore', 'acceptHMRUpdate']
  },
  piniaPersistedstate: {
    storage: 'sessionStorage'
  },
  imports: {
    dirs: ['stores']
  },
  tailwindcss: {
    cssPath: '~/assets/css/tailwind.css',
    configPath: 'tailwind.config'
  },
  colorMode: {
    classSuffix: ''
  }
})
