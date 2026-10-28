import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'
import compression from 'vite-plugin-compression2'
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
            compression()
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
                    target: 'http://localhost:8068',
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
                    }
                }
            }
        }
    }
})
