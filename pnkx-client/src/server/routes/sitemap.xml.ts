/**
 * sitemap.xml：静态页面 + 全部公开文章页
 * 供搜索引擎收录
 */
const SITE_URL = 'https://pnkx.top'
const API_BASE = process.env.VITE_APP_BASE_URL || 'https://admin.pnkx.top:8/prod-api'

interface ArticleBrief {
  id: number
  createTime?: string
}

export default defineEventHandler(async (event) => {
  const staticPages = ['', '/archives', '/album', '/link', '/message', '/videos', '/share']
  let urls = staticPages.map(p => `  <url><loc>${SITE_URL}${p}</loc><changefreq>daily</changefreq></url>`).join('\n')
  try {
    const res = await $fetch<any>(`${API_BASE}/client/article/listNotContent`, {
      params: { pageNum: 1, pageSize: 500 },
      timeout: 10000
    })
    const rows: ArticleBrief[] = res?.rows || []
    urls += '\n' + rows.map(a => `  <url><loc>${SITE_URL}/post/${a.id}</loc>${
      a.createTime ? `<lastmod>${a.createTime.substring(0, 10)}</lastmod>` : ''
    }<changefreq>weekly</changefreq></url>`).join('\n')
  } catch (e) {
    // 后端不可达时仅输出静态页
  }

  setResponseHeader(event, 'content-type', 'application/xml; charset=utf-8')
  return `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
${urls}
</urlset>`
})
