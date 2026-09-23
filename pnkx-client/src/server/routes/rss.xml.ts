/**
 * RSS 2.0 输出：最新 20 篇公开文章
 * 供 RSS 阅读器（Feedly/Inoreader 等）订阅
 */
const SITE_URL = 'https://pnkx.top'
const API_BASE = process.env.VITE_APP_BASE_URL || 'https://admin.pnkx.top:8/prod-api'

interface ArticleBrief {
  id: number
  title: string
  createTime?: string
}

function escapeXml(str: string): string {
  return (str || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
}

export default defineEventHandler(async (event) => {
  let items = ''
  try {
    const res = await $fetch<any>(`${API_BASE}/client/article/listNotContent`, {
      params: { pageNum: 1, pageSize: 20 },
      timeout: 10000
    })
    const rows: ArticleBrief[] = res?.rows || []
    const now = new Date().toUTCString()
    items = rows.map(a => `    <item>
      <title>${escapeXml(a.title)}</title>
      <link>${SITE_URL}/post/${a.id}</link>
      <guid isPermaLink="true">${SITE_URL}/post/${a.id}</guid>
      <pubDate>${a.createTime ? new Date(a.createTime).toUTCString() : now}</pubDate>
    </item>`).join('\n')
  } catch (e) {
    // 后端不可达时输出空 channel，避免订阅器报错
  }

  setResponseHeader(event, 'content-type', 'application/rss+xml; charset=utf-8')
  return `<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0">
  <channel>
    <title>Pei你看雪</title>
    <link>${SITE_URL}</link>
    <description>Pnkx · Pei你看雪 个人博客</description>
    <language>zh-CN</language>
${items}
  </channel>
</rss>`
})
