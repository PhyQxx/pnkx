import { defineConfig, devices } from '@playwright/test'

/**
 * E2E 测试配置
 *
 * 双 webServer：
 *   1. tests/mock-server/server.mjs  → :8068（vite dev proxy 的 /dev-api 天然指向它）
 *   2. vite dev                      → :4173（避开 config 里的 80 端口，CI 无需特权）
 *
 * 隔离策略：按 Authorization token 划分「档案」——不同 spec 文件用不同假 token，
 * Mock Server 按 token 维护 features 开关、路由覆写、请求日志，互不串扰；
 * 因此 workers>1 也安全。
 */
// 端口可环境变量化：多套 E2E 并行时各自独立（默认 4173/8068）
const WEB_PORT = Number(process.env.E2E_WEB_PORT || 4173)
const MOCK_PORT = Number(process.env.E2E_MOCK_PORT || 8068)

export default defineConfig({
    testDir: 'tests/e2e',
    timeout: 30_000,
    expect: { timeout: 6_000 },
    fullyParallel: false,
    workers: 2,
    retries: process.env.CI ? 1 : 0,
    // 报告与产物目录放在 node_modules/.cache 下：避免落在 vite 监听范围触发测试中的整页 reload
    outputDir: 'node_modules/.cache/pw-test-results',
    reporter: [['list'], ['html', { outputFolder: 'node_modules/.cache/pw-report', open: 'never' }]],
    use: {
        baseURL: `http://localhost:${WEB_PORT}`,
        trace: 'retain-on-failure',
        actionTimeout: 8_000,
        navigationTimeout: 15_000
    },
    projects: [
        {
            // 手机形态（chromium 内核 + iPhone 视口/触控，避免 devices['iPhone 13'] 默认 webkit）
            name: 'mobile',
            use: {
                ...devices['Desktop Chrome'],
                viewport: { width: 375, height: 812 },
                hasTouch: true,
                isMobile: true,
                deviceScaleFactor: 3,
                userAgent:
                    'Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1'
            }
        },
        {
            name: 'desktop',
            use: { viewport: { width: 1280, height: 800 } }
        }
    ],
    webServer: [
        {
            command: `MOCK_PORT=${MOCK_PORT} node tests/mock-server/server.mjs`,
            port: MOCK_PORT,
            reuseExistingServer: false,
            stdout: 'ignore',
            timeout: 15_000
        },
        {
            command: `E2E_MOCK_PROXY=http://localhost:${MOCK_PORT} npm run dev -- --port ${WEB_PORT} --strictPort`,
            url: `http://localhost:${WEB_PORT}`,
            reuseExistingServer: true,
            timeout: 60_000
        }
    ]
})
