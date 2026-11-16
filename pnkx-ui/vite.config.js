import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'
import compression from 'vite-plugin-compression2'
import Components from 'unplugin-vue-components/vite'
import { VantResolver } from '@vant/auto-import-resolver'
import { VitePWA } from 'vite-plugin-pwa'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), 'VUE_APP_')
    return {
        envPrefix: 'VUE_APP_',
        plugins: [
            vue(),
            vueJsx(),
            createSvgIconsPlugin({
                iconDirs: [path.resolve(process.cwd(), 'src/assets/icons/svg')],
                symbolId: 'icon-[name]'
            }),
            // Vant 按需引入：仅解析 van-* 标签（PC 端不使用，不影响现有组件注册习惯）
            Components({
                dirs: [],
                dts: false,
                resolvers: [VantResolver()]
            }),
            compression(),
            // PWA：让移动端(/m)可以"添加到主屏幕"装成 app（桌面图标 + 无浏览器外壳的独立窗口 + 离线外壳）。
            // - start_url 指向 /m：安装后点图标直接进移动端模块；PC 浏览器正常访问完全不受影响
            // - autoUpdate：新版本 Service Worker 安装完成后自动接管，已安装的 app 下次打开即为最新版
            // - 缓存策略同时服务于 PC 管理端：HTML 走 network-first（发版即时生效），
            //   业务 JS/CSS 依赖文件名 hash 由 precache 全权管理，接口 GET 仅做短时离线兜底
            VitePWA({
                registerType: 'autoUpdate',
                includeAssets: ['favicon.ico', 'apple-touch-icon.png', 'pwa-192.png', 'pwa-512.png'],
                manifest: {
                    name: 'Pei你看雪',
                    short_name: 'Pei你看雪',
                    description: 'Pei你看雪博客管理系统',
                    lang: 'zh-CN',
                    start_url: '/m',
                    scope: '/',
                    display: 'standalone',
                    orientation: 'portrait',
                    theme_color: '#5b8def',
                    background_color: '#fbfdff',
                    icons: [
                        { src: '/pwa-192.png', sizes: '192x192', type: 'image/png' },
                        { src: '/pwa-512.png', sizes: '512x512', type: 'image/png' }
                    ]
                },
                workbox: {
                    // 主入口 chunk 4MB+、vendor 手动分包后均超过 workbox 默认 2MiB 预缓存上限，
                    // 放宽到 8MB：漏掉入口 chunk 会导致已安装的 app 断网时无法启动
                    maximumFileSizeToCacheInBytes: 8 * 1024 * 1024,
                    // 断网时导航类请求统一回落到 SPA 外壳，由 vue-router 接管路由
                    navigateFallback: '/index.html',
                    // API / websocket 的导航类请求绝不回落到 index.html
                    navigateFallbackDenylist: [/^\/prod-api\//, /^\/websocket\//],
                    runtimeCaching: [
                        {
                            // 接口 GET：网络优先，断网时回落最近一次结果（短时兜底，不当长缓存用）
                            urlPattern: ({ url }) => url.pathname.startsWith('/prod-api/'),
                            handler: 'NetworkFirst',
                            options: {
                                cacheName: 'pnkx-api-cache',
                                networkTimeoutSeconds: 5,
                                expiration: { maxEntries: 50, maxAgeSeconds: 10 * 60 },
                                cacheableResponse: { statuses: [200] }
                            }
                        },
                        {
                            // 图片：仅同源（应用 UI 图/上传文件），缓存优先，离线时已看过的仍可显示
                            urlPattern: ({ request, url }) =>
                                request.destination === 'image' && url.origin === self.location.origin,
                            handler: 'CacheFirst',
                            options: {
                                cacheName: 'pnkx-image-cache',
                                expiration: { maxEntries: 200, maxAgeSeconds: 30 * 24 * 60 * 60 },
                                cacheableResponse: { statuses: [200] }
                            }
                        }
                    ]
                },
                // 本地 dev 与 e2e（均跑 vite dev server）不注册 SW，避免影响调试和测试
                devOptions: { enabled: false }
            })
        ],
        resolve: {
            extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue'],
            alias: [
                { find: '@', replacement: path.resolve(__dirname, './src') },
                { find: '@shikijs/core', replacement: path.resolve(__dirname, './src/shiki-shim.js') },
                // mermaid 的 mindmap 模块深引 cytoscape/dist/cytoscape.umd.js，
                // 而 cytoscape>=3.34 的 exports 只给该子路径开放 require 条件，
                // 构建时会报 "No known conditions for ./dist/cytoscape.umd.js"，重定向到 ESM 版本
                { find: /^cytoscape\/dist\/cytoscape\.umd\.js$/, replacement: 'cytoscape/dist/cytoscape.esm.mjs' }
            ]
        },
        css: {
            preprocessorOptions: {
                scss: {
                    additionalData: `@use "@/assets/styles/theme.scss" as *;`
                }
            }
        },
        // 依赖预构建必须显式声明全量清单。
        // vite 5.4 在运行时发现新依赖（如 cherry-markdown 动态加载 mermaid）触发重优化时，
        // 重扫结果可能被取消而退化成"仅 main.js 静态依赖"（3 个包），其余依赖从缓存集中丢失，
        // 页面上旧 hash 的依赖请求会 504——典型症状：AI 助手的 XMarkDown 异步组件加载失败，
        // 消息气泡全部渲染为空。include 里的依赖在每次优化批次中都会强制打包，可杜绝该问题。
        // 新增第三方依赖时需同步补充到此列表。
        optimizeDeps: {
            include: [
                '@element-plus/icons-vue',
                '@videojs-player/vue',
                'axios',
                'cherry-markdown',
                'clipboard',
                'dayjs',
                'dayjs/locale/zh-cn',
                'dompurify',
                'echarts/charts',
                'echarts/components',
                'echarts/core',
                'echarts/renderers',
                'echarts/theme/macarons',
                'element-plus',
                'element-plus/es/locale/lang/zh-cn',
                'file-saver',
                'fuse.js',
                'highlight.js/lib/core',
                'highlight.js/lib/languages/java',
                'highlight.js/lib/languages/javascript',
                'highlight.js/lib/languages/sql',
                'highlight.js/lib/languages/xml',
                'js-beautify',
                'js-cookie',
                'lodash',
                'marked',
                'mitt',
                'nprogress',
                'quill',
                'screenfull',
                'sortablejs',
                'vue',
                'vue-cropper',
                'vue-danmaku',
                'vue-element-plus-x',
                'vue-router',
                'vue3-count-to',
                'vue3-treeselect',
                'vuedraggable',
                'vant',
                'vuex',
                'x-markdown-vue'
            ]
        },
        server: {
            port: 80,
            host: '0.0.0.0',
            open: true,
            proxy: {
                [env.VUE_APP_BASE_API]: {
                    // E2E 并行时经 E2E_MOCK_PROXY 指向各自的 mock server（仅测试用途）
                    target: process.env.E2E_MOCK_PROXY || 'http://localhost:8068',
                    changeOrigin: true,
                    rewrite: (path) => path.replace(new RegExp('^' + env.VUE_APP_BASE_API), '')
                }
            }
        },
        build: {
            outDir: 'dist',
            assetsDir: 'static',
            sourcemap: false,
            chunkSizeWarningLimit: 500,
            rollupOptions: {
                output: {
                    manualChunks: {
                        'vue-vendor': ['vue', 'vue-router', 'vuex'],
                        'element-plus': ['element-plus', '@element-plus/icons-vue'],
                        'echarts': ['echarts'],
                        'vant': ['vant'],
                    }
                }
            }
        }
    }
})
