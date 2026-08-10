import type {HttpOption} from '@/composables/useHttp'
import type { Notice } from '@/types/notice'

enum Api {
  getNoticeList = '/client/getNoticeList',
  getNotice = '/client/getNotice/',
}

/**
 * 获取通知公告列表
 * @param option useFetch 配置选项
 * @returns 公告列表
 */
export function getNoticeList(option?: HttpOption<Notice[]>) {
  return useHttp.get<Notice[]>(Api.getNoticeList, option)
}

/**
 * 获取通知公告详情
 * @param id 通知公告id
 * @param option useFetch 配置选项
 * @returns 公告列表
 */
export function getNotice(id: number, option?: HttpOption<Notice>) {
  return useHttp.get<Notice>(Api.getNotice + id, option)
}
