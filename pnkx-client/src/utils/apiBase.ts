/**
 * 后端 API 地址（唯一权威定义）
 * 优先取环境变量 VITE_APP_BASE_URL（.env.dev / .env.prod），未配置时回退生产地址。
 * 散落各处的 `|| 'https://...'` 兜底一律引用本常量，避免多处维护漂移。
 */
export const API_BASE_URL: string =
  (import.meta.env.VITE_APP_BASE_URL as string) || 'https://admin.pnkx.top:8/prod-api'
