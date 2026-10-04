/**
 * 站点级常量 —— server routes（rss/sitemap 等）的唯一配置点。
 * 后端 API 地址：优先运行时环境变量 VITE_APP_BASE_URL（.env.dev/.env.prod 由 --dotenv 注入），
 * 客户端侧的权威定义在 src/utils/apiBase.ts（构建时 import.meta.env，机制不同故分开维护）。
 */
export const SITE_URL = 'https://pnkx.top'

const API_BASE_FALLBACK = 'https://admin.pnkx.top:8/prod-api'

export function apiBase(): string {
  return process.env.VITE_APP_BASE_URL || API_BASE_FALLBACK
}
