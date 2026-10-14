import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'
import compression from 'vite-plugin-compression2'
import path from 'path'
import fs from 'fs'

// 图标名清单虚拟模块：IconSelect 只需要 svg 文件名（渲染走 svg sprite），
// 避免 eager glob 把 215 个 svg 全文（约 456KB）打进共享 chunk
const VIRTUAL_ICON_NAMES = 'virtual:svg-icon-names'
function svgIconNames() {
    return {
        name: 'svg-icon-names',
        resolveId(id) {
            if (id === VIRTUAL_ICON_NAMES) return '\0' + VIRTUAL_ICON_NAMES
            return null
        },
        load(id) {
            if (id !== '\0' + VIRTUAL_ICON_NAMES) return null
            const dir = path.resolve(process.cwd(), 'src/assets/icons/svg')
            const names = fs.readdirSync(dir).filter(f => f.endsWith('.svg')).map(f => f.replace(/\.svg$/, '')).sort()
            return `export default ${JSON.stringify(names)}`
        }
    }
}

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), 'VUE_APP_')
    return {
        envPrefix: 'VUE_APP_',
        plugins: [
            vue(),
            vueJsx(),
            svgIconNames(),
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
                    // chunk 名强制纯 ASCII：glob 导入的 svg 模块（如 验证码/闹钟.svg）
                    // 会让共享 chunk 带中文文件名，部分 CDN/对象存储对中文 key 不友好
                    sanitizeFileName(name) {
                        return name.replace(/[^\x20-\x7E]/g, c => '_' + c.codePointAt(0).toString(36))
                    },
                    manualChunks: {
                        'vue-vendor': ['vue', 'vue-router', 'vuex'],
                        'element-plus': ['element-plus'],
                        'element-icons': ['@element-plus/icons-vue'],
                        'echarts': ['echarts'],
                    }
                }
            }
        }
    }
})
