/**
 * sitemap.xml：静态页面 + 全部公开文章页
 * 供搜索引擎收录
 */
import { SITE_URL, apiBase } from '../utils/siteConfig'

import { collectSitemapArticles } from '../utils/sitemapArticles'

export default defineEventHandler(async (event) => {
  const staticPages = ['', '/archives', '/album', '/link', '/message', '/video', '/share', '/type']
  let urls = staticPages.map(p => `  <url><loc>${SITE_URL}${p}</loc><changefreq>daily</changefreq></url>`).join('\n')
  try {
    const rows = await collectSitemapArticles((pageNum, pageSize) =>
      $fetch(`${apiBase()}/client/article/listNotContent`, {
        params: { pageNum, pageSize, orderByColumn: 'id', isAsc: 'asc' },
        timeout: 10000,
        retry: 0
      })
    )
    urls += '\n' + rows.map(a => `  <url><loc>${SITE_URL}/post/${a.id}</loc>${
      a.createTime && /^\d{4}-\d{2}-\d{2}/.test(a.createTime) ? `<lastmod>${a.createTime.substring(0, 10)}</lastmod>` : ''
    }<changefreq>weekly</changefreq></url>`).join('\n')
  } catch (e) {
    // Do not advertise an incomplete sitemap as a successful response.
    setResponseHeader(event, 'retry-after', '60')
    throw createError({ statusCode: 503, statusMessage: 'Sitemap temporarily unavailable' })
  }

  setResponseHeader(event, 'content-type', 'application/xml; charset=utf-8')
  return `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
${urls}
</urlset>`
})
