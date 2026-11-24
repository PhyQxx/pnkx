export interface ArticleBrief {
  id: number
  createTime?: string
}
interface ArticlePage {
  code: number
  rows: ArticleBrief[]
  total: number
}

/** Fetch every page; reject incomplete/repeating responses instead of publishing a partial sitemap. */
export async function collectSitemapArticles(
  fetchPage: (pageNum: number, pageSize: number) => Promise<ArticlePage>
): Promise<ArticleBrief[]> {
  const articles = new Map<number, ArticleBrief>()
  const pageSize = 500
  for (let pageNum = 1; pageNum <= 100; pageNum++) {
    const page = await fetchPage(pageNum, pageSize)
    if (page.code !== 200 || !Array.isArray(page.rows) || !Number.isInteger(page.total) || page.total < 0) {
      throw new Error('Invalid article page')
    }
    // Leave room for static URLs within the sitemap protocol limit.
    if (page.total > 49000) throw new Error('Sitemap index required for more than 49000 articles')
    const previousSize = articles.size
    for (const article of page.rows) {
      if (!Number.isSafeInteger(article.id) || article.id <= 0) throw new Error('Invalid article ID')
      articles.set(article.id, article)
    }
    if (articles.size >= page.total) return [...articles.values()]
    if (articles.size === previousSize) throw new Error('Article pagination made no progress')
  }
  throw new Error('Article pagination limit exceeded')
}
