import type { Article } from '@/types/article'
import type { HttpOption } from '@/composables/useHttp'
import type { PageQuery } from '@/types'

enum Api {
  articleList = '/client/article/listOrdinaryContent',
  articleListNotContent = '/client/article/listNotContent',
  article = '/client/article/',
  recommend = '/client/article/getHotArticle'
}

/**
 * 查看文章列表
 * @param params 查询条件
 * @param option useFetch 配置选项
 * @returns 文章列表
 */
export function getArticleList(params: PageQuery, option?: HttpOption<Article[]>) {
  return useHttp.get<Article[]>(Api.articleList, params, option)
}

/**
 * 查看文章列表
 * @param params 查询条件
 * @param option useFetch 配置选项
 * @returns 文章列表
 */
export function listNotContent(params: PageQuery, option?: HttpOption<Article[]>) {
  return useHttp.get<Article[]>(Api.articleListNotContent, params, option)
}

/**
 * 查看文章
 * @param articleId 文章id
 * @param option
 */
export function getArticle(articleId: number, option?: HttpOption<Article>) {
  return useHttp.get<Article>(Api.article + articleId, {}, option)
}

/**
 * 查看推荐文章
 * @returns 推荐文章
 */
export function getHotArticle(option?: HttpOption<Article[]>) {
  return useHttp.get<Article[]>(Api.recommend, {}, option)
}

/**
 * 点赞文章
 * @param articleId 文章id
 */
export function likeArticle(articleId: number) {
  return useHttp.post<null>(`/client/article/like/${articleId}`)
}
