import type { HttpOption } from '@/composables/useHttp'
import type { BlogInfo } from '@/types/home'

enum Api {
  blogInfo = '/client/getBlogInfo',
  report = '/client/visits'
}

/**
 * 获取博客信息
 * @returns 博客信息
 */
export function getBlogInfo(option?: HttpOption<BlogInfo>) {
  return useHttp.get<BlogInfo>(Api.blogInfo, {}, option)
}

/**
 * 上传访客信息
 */
export function report(option?: HttpOption<null>) {
  return useHttp.post<null>(Api.report, option)
}
