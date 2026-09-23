import type { Wallpaper, WallpaperPageQuery } from '~/types/file'
import type { Result } from '~/types'
import { API_BASE_URL } from '~/utils/apiBase'

const baseURL = API_BASE_URL

enum Api {
  wallpaperList = '/client/wallpaper/list',
  wallpaperLike = '/client/wallpaper/like/',
}

// 获取博客管理「壁纸管理」中的公开壁纸（图库唯一数据源）
export function getWallpaperImg(params: WallpaperPageQuery) {
  return $fetch<Result<Wallpaper[]>>(Api.wallpaperList, {
    baseURL,
    params
  })
}

// 点赞/取消点赞壁纸（登录后可用；状态变更操作使用 POST）
export function likeWallpaper(id: number, authorization?: string) {
  return $fetch<Result<unknown> & { liked?: boolean }>(Api.wallpaperLike + id, {
    baseURL,
    method: 'POST',
    headers: authorization ? {Authorization: authorization} : undefined
  })
}
