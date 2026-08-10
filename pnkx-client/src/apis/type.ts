import type { HttpOption } from '@/composables/useHttp'
import type { Type } from '~/types/type'

enum Api {
  typeList = '/client/article/getArticleListGroupByType',
}

/**
 * 查看文章分类
 * @returns 文章分类
 */
export function getArticleListGroupByType(option?: HttpOption<Type[]>) {
  return useHttp.get<Type[]>(Api.typeList, {}, option)
}