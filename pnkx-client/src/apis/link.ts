import type { UnwrapRef } from 'vue'
import type { Link } from '@/types/link'
import type { HttpOption } from '@/composables/useHttp'
import type { PageQuery } from '~/types'

enum Api {
  link = '/client/link/list',
  addLink = '/client/link/addLink',
}

export function getLinkList(params: PageQuery, option?: HttpOption<Link[]>) {
  return useHttp.get<Link[]>(Api.link, params, option)
}

export function addLink(link: Ref<UnwrapRef<Link>>, option?: HttpOption<Link[]>) {
  return useHttp.post<any>(Api.addLink, link, option)
}
