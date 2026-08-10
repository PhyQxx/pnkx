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
            alias: {
                '@': path.resolve(__dirname, './src'),
                'vue': 'vue/dist/vue.esm-bundler.js',
                'cytoscape/dist/cytoscape.umd.js': 'cytoscape/dist/cytoscape.esm.mjs',
                'cytoscape': 'cytoscape/dist/cytoscape.esm.mjs',
                '@shikijs/core': path.resolve(__dirname, './src/shiki-shim.js')
            }
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
            chunkSizeWarningLimit: 1000,
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
