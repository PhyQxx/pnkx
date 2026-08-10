import type { PageQuery } from '@/types'
import type { ShareResource } from '@/types/share'
import type { HttpOption } from '@/composables/useHttp'

enum Api {
  shareList = '/client/share/list',
  shareClick = '/client/share/click'
}

export function getShareList(
  params: Partial<PageQuery> & Partial<ShareResource>,
  option?: HttpOption<ShareResource[]>
) {
  return useHttp.get<ShareResource[]>(Api.shareList, params, option)
}

export function recordShareClick(id: number) {
  return useHttp.post(`${Api.shareClick}/${id}`)
}
